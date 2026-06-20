package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaProperties;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.RepositorySourceUrls;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.AdminReviewRecordRepository;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仓库级联删除器：按外键安全顺序清空一个仓库牵连的全部业务数据，再删除平台 Gitea 副本。
 *
 * <p><b>为何手写顺序而非依赖 JPA cascade：</b>{@code Quest} 等实体对其下游（submissions /
 * reviews / XP 流水 / 贡献记录）<b>没有</b> {@code cascade=REMOVE} 映射——它们由子表侧持有外键，
 * 父实体上没有集合映射。因此必须显式按「孙→子→父」顺序删除，并在每个依赖层之间 {@code flush}，
 * 强制 Hibernate 按此顺序发 DELETE，否则单次 flush 内的删除不保证外键安全。
 *
 * <p>删除顺序（与线上人工清理脚本一致）：
 * <pre>
 *   review_items → review_records → submissions
 *     → quest_assignments / admin_review_records / xp_transactions / contribution_records(by quest)
 *     → quests (顺带清空 quest_tag_relations 关联行，并释放 quest→issue 外键)
 *     → issues / pull_requests / contribution_records(by repo)
 *     → repository
 *     → Gitea 副本
 * </pre>
 *
 * <p>Gitea 副本删除放在事务末尾：若 Gitea 调用失败（非 404），异常上抛触发整个事务回滚，
 * 保证「平台数据」与「Gitea 副本」要么都删、要么都留，不出现半删状态。
 */
@Component
public class RepositoryCascadeDeleter {

    private final QuestRepository questRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final QuestAssignmentRepository questAssignmentRepository;
    private final AdminReviewRecordRepository adminReviewRecordRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final CodeIssueRepository codeIssueRepository;
    private final CodePullRequestRepository codePullRequestRepository;
    private final CodeRepositoryRepository codeRepositoryRepository;
    private final GiteaAdapter giteaAdapter;
    private final GiteaProperties giteaProperties;

    public RepositoryCascadeDeleter(
            QuestRepository questRepository,
            SubmissionRepository submissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            QuestAssignmentRepository questAssignmentRepository,
            AdminReviewRecordRepository adminReviewRecordRepository,
            XpTransactionRepository xpTransactionRepository,
            ContributionRecordRepository contributionRecordRepository,
            CodeIssueRepository codeIssueRepository,
            CodePullRequestRepository codePullRequestRepository,
            CodeRepositoryRepository codeRepositoryRepository,
            GiteaAdapter giteaAdapter,
            GiteaProperties giteaProperties) {
        this.questRepository = questRepository;
        this.submissionRepository = submissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.questAssignmentRepository = questAssignmentRepository;
        this.adminReviewRecordRepository = adminReviewRecordRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.contributionRecordRepository = contributionRecordRepository;
        this.codeIssueRepository = codeIssueRepository;
        this.codePullRequestRepository = codePullRequestRepository;
        this.codeRepositoryRepository = codeRepositoryRepository;
        this.giteaAdapter = giteaAdapter;
        this.giteaProperties = giteaProperties;
    }

    /**
     * 级联删除指定仓库的全部数据及其 Gitea 副本。调用方须保证已完成权限校验。
     */
    @Transactional
    public void deleteCascade(CodeRepository repository) {
        Long repoId = repository.getRepositoryId();

        List<Quest> quests = questRepository.findByRepositoryRepositoryId(repoId);
        List<Long> questIds = quests.stream().map(Quest::getQuestId).toList();

        if (!questIds.isEmpty()) {
            // 1) review_items（经 review_records 级联）→ review_records
            List<Submission> submissions = submissionRepository.findByQuestQuestIdIn(questIds);
            List<Long> submissionIds = submissions.stream().map(Submission::getSubmissionId).toList();
            if (!submissionIds.isEmpty()) {
                reviewRecordRepository.deleteAll(
                        reviewRecordRepository.findBySubmissionSubmissionIdIn(submissionIds));
                reviewRecordRepository.flush();
            }

            // 2) submissions（释放 pull_request 外键引用）
            submissionRepository.deleteAll(submissions);
            submissionRepository.flush();

            // 3) 其余直接挂在 quest 上的子表（彼此无外键依赖，一次 flush 即可）
            questAssignmentRepository.deleteAll(questAssignmentRepository.findByQuestQuestIdIn(questIds));
            adminReviewRecordRepository.deleteAll(adminReviewRecordRepository.findByQuestQuestIdIn(questIds));
            xpTransactionRepository.deleteAll(xpTransactionRepository.findByQuestQuestIdIn(questIds));
            contributionRecordRepository.deleteAll(contributionRecordRepository.findByQuestQuestIdIn(questIds));
            contributionRecordRepository.flush();

            // 4) quests（顺带删 quest_tag_relations 关联行，释放 quest→issue 外键）
            questRepository.deleteAll(quests);
            questRepository.flush();
        }

        // 5) 仓库级子表：issues / pull_requests / 兜底 contribution_records(by repo)
        codeIssueRepository.deleteAll(codeIssueRepository.findByRepositoryRepositoryId(repoId));
        codePullRequestRepository.deleteAll(codePullRequestRepository.findByRepositoryRepositoryId(repoId));
        contributionRecordRepository.deleteAll(contributionRecordRepository.findByRepositoryRepositoryId(repoId));
        codeIssueRepository.flush();
        codePullRequestRepository.flush();

        // 6) 仓库本体
        codeRepositoryRepository.delete(repository);
        codeRepositoryRepository.flush();

        // 7) 平台 Gitea 副本（失败则回滚以上全部删除）
        deleteGiteaCopy(repository);
    }

    /**
     * 删除平台 Gitea 上的仓库副本。仅当 {@code sourceUrl} 指向平台自己的 Gitea（host 与平台
     * base-url 一致）时才执行，避免误删登记为外链的第三方仓库。无法解析 owner/repo 时跳过。
     */
    private void deleteGiteaCopy(CodeRepository repository) {
        String source = repository.getSourceUrl();
        if (source == null || source.isBlank()) {
            return;
        }
        String sourceHost = RepositorySourceUrls.host(source);
        String platformHost = RepositorySourceUrls.host(giteaProperties.baseUrl());
        if (sourceHost.isBlank() || platformHost.isBlank() || !sourceHost.equals(platformHost)) {
            return;
        }
        GiteaRepoCoordinates coords;
        try {
            coords = GiteaRepoCoordinates.parse(source);
        } catch (BusinessException e) {
            return;
        }
        giteaAdapter.deleteRepository(coords.owner(), coords.repo());
    }
}
