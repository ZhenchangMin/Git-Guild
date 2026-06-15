package com.gitguild.backend.assistant.service;

import com.gitguild.backend.assistant.dto.AssistantChatResponse.AssistantAction;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AssistantActionResolver {

    private static final int MAX_ACTIONS = 2;

    public List<AssistantAction> resolveActions(String message, String page) {
        return resolveActions(AssistantChatContext.anonymous(message, page));
    }

    public List<AssistantAction> resolveActions(AssistantChatContext context) {
        String normalizedMessage = normalize(context.message());
        if (normalizedMessage.isBlank()) {
            return List.of();
        }

        Set<AssistantAction> actions = new LinkedHashSet<>();
        addIfMatched(actions, normalizedMessage, "打开委托发布页", "maintainer-publish",
                List.of("发布委托", "新建委托", "新建任务", "发任务", "从 issue 发布", "issue 发布", "发悬赏"));
        addIfMatched(actions, normalizedMessage, "打开提交审核台", "maintainer-review",
                List.of("审核提交", "批阅", "待审核", "审核台", "查看提交", "提交审核"));
        addReviewIntentIfMatched(actions, normalizedMessage);
        addIfMatched(actions, normalizedMessage, "打开悬赏任务板", "quest-board",
                List.of(
                        "委托板",
                        "悬赏板",
                        "任务板",
                        "接任务",
                        "接取",
                        "找任务",
                        "可接",
                        "找 issue",
                        "quest board",
                        "推荐任务",
                        "推荐委托",
                        "推荐一个任务",
                        "适合我的任务",
                        "适合我的委托",
                        "推荐悬赏"));
        addIfMatched(actions, normalizedMessage, "打开提交柜台", "submission-counter",
                List.of("提交柜台", "提交成果", "登记成果", "交作业", "交成果", "提交单", "提交委托", "提交任务"));
        addWorkbenchIfMatched(actions, normalizedMessage, context);
        addIfMatched(actions, normalizedMessage, "打开成长档案", "profile",
                List.of("成长", "等级", "xp", "徽章", "档案", "个人档案"));
        addIfMatched(actions, normalizedMessage, "打开排行榜", "leaderboard",
                List.of("排行榜", "排名", "榜单", "leaderboard"));
        addIfMatched(actions, normalizedMessage, "打开仓库接入", "repository-sync",
                List.of("仓库接入", "同步仓库", "导入仓库", "接入仓库", "repository sync"));
        addIfMatched(actions, normalizedMessage, "打开帮助说明", "help",
                List.of("帮助", "教程", "使用说明", "guide"));

        return List.copyOf(actions);
    }

    private void addIfMatched(
            Set<AssistantAction> actions,
            String normalizedMessage,
            String label,
            String routeName,
            List<String> keywords) {
        if (actions.size() >= MAX_ACTIONS || !containsAny(normalizedMessage, keywords)) {
            return;
        }
        actions.add(new AssistantAction(label, routeName));
    }

    private void addReviewIntentIfMatched(Set<AssistantAction> actions, String normalizedMessage) {
        if (actions.size() >= MAX_ACTIONS) {
            return;
        }
        boolean hasReviewVerb = containsAny(normalizedMessage, List.of("审阅", "审查", "检查"));
        boolean hasReviewObject = containsAny(normalizedMessage, List.of("委托", "任务", "成果"));
        if (hasReviewVerb && hasReviewObject) {
            actions.add(new AssistantAction("打开提交审核台", "maintainer-review"));
        }
    }

    private void addWorkbenchIfMatched(
            Set<AssistantAction> actions,
            String normalizedMessage,
            AssistantChatContext context) {
        if (actions.size() >= MAX_ACTIONS || !containsAny(normalizedMessage,
                List.of("工作台", "clone", "克隆", "分支", "checkout", "push", "commit", "提交代码"))) {
            return;
        }

        if (!context.authenticated()) {
            actions.add(new AssistantAction("前往登录后打开工作台", "login"));
            return;
        }
        if (context.hasRole("ROLE_MAINTAINER")) {
            actions.add(new AssistantAction("打开委托人工作台", "maintainer-workbench"));
            return;
        }
        actions.add(new AssistantAction("打开冒险家工作台", "adventurer-workbench"));
    }

    private boolean containsAny(String normalizedMessage, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedMessage::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
