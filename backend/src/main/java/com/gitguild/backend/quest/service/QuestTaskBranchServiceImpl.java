package com.gitguild.backend.quest.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@link QuestTaskBranchService} 的实现。
 *
 * <p>本类<b>不</b>标注 {@code @Transactional}：它在调用方（如 {@code acceptQuest}）的事务内运行，
 * 既能持久化 {@code taskBranch}，又能在 Gitea 不可达时把异常抛回调用方做 best-effort 处理，
 * 而不会越过事务代理边界把当前事务标记为 rollback-only。
 *
 * <p><b>幂等保证：</b>
 * <ul>
 *   <li>Assignment 已记录 taskBranch → 直接返回，不再调用 Gitea；</li>
 *   <li>Gitea 侧分支已存在（{@code CODE_HOST_RESOURCE_CONFLICT} / HTTP 409）→ 视为成功，
 *       回填同一确定性分支名。</li>
 * </ul>
 */
@Service
public class QuestTaskBranchServiceImpl implements QuestTaskBranchService {

    private static final Logger log = LoggerFactory.getLogger(QuestTaskBranchServiceImpl.class);

    private final GiteaAdapter giteaAdapter;
    private final QuestAssignmentRepository assignmentRepository;

    public QuestTaskBranchServiceImpl(
            GiteaAdapter giteaAdapter,
            QuestAssignmentRepository assignmentRepository) {
        this.giteaAdapter = giteaAdapter;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public String ensureTaskBranch(QuestAssignment assignment) {
        if (assignment.getTaskBranch() != null && !assignment.getTaskBranch().isBlank()) {
            return assignment.getTaskBranch();
        }

        Quest quest = assignment.getQuest();
        CodeRepository repository = quest.getRepository();
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        String branchName = buildBranchName(assignment);
        String baseBranch = repository.getDefaultBranch();

        try {
            giteaAdapter.createBranch(coords.owner(), coords.repo(), branchName, baseBranch);
        } catch (BusinessException ex) {
            // 分支已存在（幂等）：Gitea 返回 409，视为创建成功，回填确定性分支名
            if (!"CODE_HOST_RESOURCE_CONFLICT".equals(ex.getCode())) {
                throw ex;
            }
            log.info("task branch 已存在，按幂等处理 assignmentId={}, branch={}",
                    assignment.getAssignmentId(), branchName);
        }

        assignment.setTaskBranch(branchName);
        assignmentRepository.save(assignment);
        return branchName;
    }

    /**
     * 生成可追踪的 task branch 名：{@code task/quest-{questId}-assignment-{assignmentId}-{username}}。
     * 同时包含 Quest、Assignment、Adventurer 三要素；username 经 git refname 合法化。
     */
    private String buildBranchName(QuestAssignment assignment) {
        Long questId = assignment.getQuest().getQuestId();
        Long assignmentId = assignment.getAssignmentId();
        String username = sanitize(assignment.getAssignee().getUsername());
        return "task/quest-" + questId + "-assignment-" + assignmentId + "-" + username;
    }

    /**
     * 将用户名合法化为 git refname 片段：非 {@code [a-zA-Z0-9._-]} 字符替换为 {@code -}，
     * 去重连续 {@code -} 并修剪首尾；为空时回退为 {@code user}。
     */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "user";
        }
        String cleaned = raw.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "user" : cleaned;
    }
}
