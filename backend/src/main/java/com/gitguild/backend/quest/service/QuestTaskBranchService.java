package com.gitguild.backend.quest.service;

import com.gitguild.backend.quest.domain.QuestAssignment;

/**
 * 为 Quest 接取记录在本地 Gitea 创建 task branch（Issue #12）。
 *
 * <p>task branch 是后续工作台 commit（#13）与创建 PR（#14）的基础。
 */
public interface QuestTaskBranchService {

    /**
     * 确保 active Assignment 拥有一个 task branch（幂等）。
     *
     * <p>若 assignment 已记录 {@code taskBranch} 则直接返回；否则按
     * {@code task/quest-{questId}-assignment-{assignmentId}-{username}} 在 Gitea 创建分支，
     * 持久化分支名后返回。Gitea 侧分支已存在（409）视为成功。
     *
     * @param assignment 处于 ACTIVE 状态的接取记录
     * @return task branch 名称
     */
    String ensureTaskBranch(QuestAssignment assignment);
}
