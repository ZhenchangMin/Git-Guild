package com.gitguild.backend.assistant.service;

import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AssistantFallbackService implements AssistantAnswerService {

    @Override
    public AssistantAnswerResult answer(AssistantChatContext context) {
        String normalizedMessage = normalize(context.message());

        Optional<AssistantAnswerResult> restrictedAnswer = answerRestrictedMaintainerIntent(context);
        if (restrictedAnswer.isPresent()) {
            return restrictedAnswer.get();
        }

        if (isAcceptedTaskNextStepQuestion(normalizedMessage)) {
            return faq(
                    "接取委托后，下一步通常是在冒险家工作台准备任务分支，按页面给出的 Git 步骤 clone 仓库、切换分支、提交修改并 push。完成开发后，再到提交柜台登记成果和 PR 信息，交给委托人审核。",
                    List.of("任务分支是什么？", "如何提交成果？"));
        }

        if (isQuestRecommendationQuestion(normalizedMessage)) {
            return faq(
                    "我可以结合委托板中的公开可接委托，以及你自己的等级、XP、完成记录和技术栈偏好来推荐新委托。推荐结果只作为参考，不会自动接取任务；你可以通过回答下方按钮前往悬赏任务板查看详情。",
                    List.of("推荐一个适合我的委托", "如何提升推荐匹配度？"));
        }

        if (containsAny(normalizedMessage, List.of("接任务", "接取", "找任务", "委托板", "悬赏板", "任务板"))) {
            return faq(
                    "你可以先进入悬赏任务板，按分类、技术栈、难度或关键词筛选委托。打开委托详情后，确认背景、验收标准和奖励 XP，再点击接取委托。接取成功后，下一步通常是进入工作台准备任务分支。",
                    List.of("接取委托后下一步做什么？", "如何筛选适合我的委托？"));
        }

        if (containsAny(normalizedMessage, List.of("提交柜台", "提交成果", "登记成果", "交作业", "交成果", "提交任务", "提交委托"))) {
            return faq(
                    "成果提交在提交柜台完成。你需要选择关联委托，确认 PR 信息，填写成果说明和必要证据，然后提交给委托人审核。系统不会在聊天中替你提交，只会提供跳转按钮。",
                    List.of("如何查看 PR 状态？", "被退回后怎么重新提交？"));
        }

        if (isPublishQuestion(normalizedMessage)) {
            return faq(
                    "委托人发布任务时，先在仓库接入页导入或同步仓库，再从 Issue 生成委托草稿，补充标题、说明、验收标准、难度、技术栈和奖励 XP。提交后需要等待管理员审核通过，委托才会进入悬赏任务板。",
                    List.of("委托人如何接入仓库？", "为什么我发布的委托还没出现在任务板？"));
        }

        if (isReviewQuestion(normalizedMessage)) {
            return faq(
                    "委托人可以在提交审核台查看冒险家提交的成果。审核时建议先阅读成果说明，再打开关联 PR 检查实现范围和验收标准，然后选择通过、退回修改或驳回。如果平台检测到可合并的 PR，可以在审核页使用单独的合并 PR 按钮自主合并，不需要回到 Gitea 操作。",
                    List.of("退回修改意见怎么写？", "什么时候需要合并 PR？"));
        }

        if (isMergePullRequestQuestion(normalizedMessage)) {
            return faq(
                    "委托人可以在提交审核台处理 PR 合并。建议先确认成果满足验收标准、PR 状态可合并，再通过审核页的合并 PR 按钮自主合并；GitGuild 不会在聊天中替你合并 PR。",
                    List.of("审核成果怎么判断？", "退回修改意见怎么写？"));
        }

        if (containsAny(normalizedMessage, List.of("工作台", "clone", "克隆", "分支", "checkout", "push", "commit", "提交代码"))) {
            return faq(
                    "工作台用于处理已接委托的开发步骤。冒险家通常需要准备任务分支、clone 仓库、切到任务分支、提交修改并 push，然后回到提交柜台登记成果。委托人的工作台主要用于仓库、委托和审核入口管理。",
                    List.of("如何提交成果？", "任务分支是什么？"));
        }

        if (isRepositorySyncQuestion(normalizedMessage)) {
            return faq(
                    "仓库接入用于把 Gitea 仓库同步到 Git Guild。委托人可以导入仓库、读取 Issue，并基于 Issue 发布委托。同步失败时，优先检查仓库地址、访问凭据和 Gitea 服务状态。",
                    List.of("委托人如何发布任务？", "同步仓库失败怎么办？"));
        }

        if (containsAny(normalizedMessage, List.of("成长", "等级", "xp", "徽章", "档案", "个人档案"))) {
            return faq(
                    "个人档案展示你的等级、XP、完成委托、贡献记录和徽章。冒险家完成并通过审核的委托会产生成长记录；委托人也可以查看自己的平台身份和相关记录。",
                    List.of("XP 如何增长？", "在哪里看排行榜？"));
        }

        if (containsAny(normalizedMessage, List.of("排行榜", "排名", "榜单", "leaderboard"))) {
            return faq(
                    "排行榜展示用户 XP 和成长表现，适合查看当前贡献排名。它主要用于展示激励，不会影响委托审核结果。",
                    List.of("个人档案在哪里？", "XP 如何增长？"));
        }

        if (containsAny(normalizedMessage, List.of("帮助", "教程", "使用说明", "guide"))) {
            return faq(
                    "帮助说明页会按角色解释 Git Guild 的使用流程。你也可以直接问我：如何接取委托、如何提交成果、委托人如何发布任务、委托人如何审核提交。",
                    List.of("如何接取委托？", "委托人如何审核提交？"));
        }

        if (containsAny(normalizedMessage, List.of("演示", "demo", "账号", "密码"))) {
            return faq(
                    "本地演示账号通常包括冒险家 advent、委托人 guild 和管理员 admin，默认密码为 admin123。艾丽丝作为 GitGuild AI 向导，主要服务来访者、冒险家和委托人；管理员后台不纳入本功能范围。",
                    List.of("推荐演示流程是什么？", "我是冒险家，下一步做什么？"));
        }

        AssistantAnswerResult pageAnswer = answerByPage(normalizedMessage, context.page());
        if (pageAnswer != null) {
            return pageAnswer;
        }

        return new AssistantAnswerResult(
                "我目前主要回答 Git Guild 的平台使用、委托流程、提交审核、仓库接入和 Git/Gitea 基础操作问题。你可以换一种说法，例如“如何接取委托”“如何提交成果”或“委托人如何审核提交”。",
                AssistantAnswerSource.FALLBACK,
                List.of("如何接取委托？", "如何提交成果？", "委托人如何审核提交？"));
    }

    public AssistantAnswerResult answer(String message, String page) {
        return answer(AssistantChatContext.anonymous(message, page));
    }

    public Optional<AssistantAnswerResult> answerRestrictedMaintainerIntent(AssistantChatContext context) {
        if (context.hasRole("ROLE_MAINTAINER")) {
            return Optional.empty();
        }

        // 账号合并后，所有登录成员都可发布/审核/接入/合并；此处只会命中「未登录来访者」，
        // 引导其登录即可，而不再说「你不是委托人不能做」。
        String normalizedMessage = normalize(context.message());
        if (isPublishQuestion(normalizedMessage)) {
            return Optional.of(faq(
                    "发布委托需要登录成员身份。登录后，你可以在仓库接入页导入或同步仓库，再从 Issue 生成委托草稿，补充标题、验收标准、难度、技术栈和奖励 XP，提交管理员审核通过即可上架。",
                    List.of("如何登录或注册？", "委托人如何接入仓库？")));
        }

        if (isReviewQuestion(normalizedMessage)) {
            return Optional.of(faq(
                    "审核成果需要登录成员身份。登录后，你可以在提交审核台查看冒险家提交的成果：先读成果说明，再打开关联 PR 检查实现范围与验收标准，然后选择通过、退回修改或驳回。",
                    List.of("如何登录或注册？", "委托人如何审核提交？")));
        }

        if (isRepositorySyncQuestion(normalizedMessage)) {
            return Optional.of(faq(
                    "接入或同步仓库需要登录成员身份。登录后，你可以在仓库接入页导入 Gitea 仓库、读取 Issue，并基于 Issue 发布委托。",
                    List.of("如何登录或注册？", "同步仓库失败怎么办？")));
        }

        if (isMergePullRequestQuestion(normalizedMessage)) {
            return Optional.of(faq(
                    "合并 PR 需要登录成员身份。登录后，你可以在提交审核台根据验收结果和 PR 状态，使用合并 PR 按钮自主合并，无需回到 Gitea 操作。",
                    List.of("如何登录或注册？", "委托人如何审核提交？")));
        }

        return Optional.empty();
    }

    private AssistantAnswerResult answerByPage(String normalizedMessage, String page) {
        if (!containsAny(normalizedMessage, List.of("这个页面", "当前页面", "这里", "怎么用"))) {
            return null;
        }

        return switch (normalize(page)) {
            case "hall" -> faq(
                    "公会大厅是主要导航入口。你可以从大厅进入悬赏任务板、工作台、提交柜台、成长档案和排行榜。",
                    List.of("我应该先去哪里？", "如何接取委托？"));
            case "quest-board" -> faq(
                    "悬赏任务板用于浏览可接取委托。建议先用筛选条件缩小范围，再打开详情确认验收标准，最后接取委托。",
                    List.of("如何筛选适合我的委托？", "接取后下一步做什么？"));
            case "adventurer-workbench" -> faq(
                    "冒险家工作台用于推进已接委托。你可以查看任务状态、准备任务分支、复制 Git 操作步骤，并在完成后前往提交柜台。",
                    List.of("如何提交成果？", "任务分支是什么？"));
            case "maintainer-workbench" -> faq(
                    "委托人工作台用于管理仓库、发布委托和进入提交审核台。你可以从这里查看待处理事项并进入对应流程。",
                    List.of("如何发布委托？", "如何审核提交？"));
            case "submission-counter" -> faq(
                    "提交柜台用于登记冒险家的成果。请确认关联委托和 PR，填写成果说明后提交给委托人审核。",
                    List.of("如何查看 PR 状态？", "被退回后怎么办？"));
            case "repository-sync" -> faq(
                    "仓库接入页用于导入或同步 Gitea 仓库，并读取 Issue 作为委托来源。",
                    List.of("如何发布委托？", "同步仓库失败怎么办？"));
            case "profile" -> faq(
                    "个人档案展示等级、XP、徽章和贡献记录。它用于观察成长，不直接改变任务状态。",
                    List.of("XP 如何增长？", "排行榜在哪里？"));
            case "leaderboard" -> faq(
                    "排行榜展示用户 XP 和成长排名，用于比较贡献进度。",
                    List.of("个人档案在哪里？", "XP 如何增长？"));
            case "help" -> faq(
                    "帮助页用于查看 Git Guild 的角色流程和基础说明。你也可以继续直接向我提问。",
                    List.of("如何接取委托？", "委托人如何审核提交？"));
            default -> null;
        };
    }

    private AssistantAnswerResult faq(String answer, List<String> suggestedQuestions) {
        return new AssistantAnswerResult(answer, AssistantAnswerSource.FAQ, suggestedQuestions);
    }

    private boolean isReviewQuestion(String normalizedMessage) {
        if (containsAny(normalizedMessage, List.of("审核提交", "审核成果", "审核委托", "审核任务", "批阅", "待审核", "审核台", "查看提交", "提交审核"))) {
            return true;
        }
        boolean hasReviewVerb = containsAny(normalizedMessage, List.of("审核", "审阅", "审查", "检查"));
        boolean hasReviewObject = containsAny(normalizedMessage, List.of("委托", "任务", "成果"));
        return hasReviewVerb && hasReviewObject;
    }

    private boolean isPublishQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage, List.of("发布委托", "新建委托", "新建任务", "发任务", "发悬赏", "issue 发布"));
    }

    private boolean isRepositorySyncQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage, List.of("仓库接入", "同步仓库", "导入仓库", "接入仓库", "repository sync"));
    }

    private boolean isMergePullRequestQuestion(String normalizedMessage) {
        boolean asksMerge = containsAny(normalizedMessage, List.of("合并", "merge"));
        boolean asksPullRequest = containsAny(normalizedMessage, List.of("pr", "pull request", "拉取请求"));
        return asksMerge && asksPullRequest;
    }

    private boolean isAcceptedTaskNextStepQuestion(String normalizedMessage) {
        boolean hasAcceptedTask = containsAny(normalizedMessage, List.of("接了任务", "接到任务", "已接任务", "已接委托", "接取后"));
        boolean asksNextStep = containsAny(normalizedMessage, List.of("下一步", "然后", "之后", "怎么做", "做什么"));
        return hasAcceptedTask && asksNextStep;
    }

    private boolean isQuestRecommendationQuestion(String normalizedMessage) {
        boolean asksRecommendation = containsAny(normalizedMessage, List.of("推荐", "适合我", "匹配我"));
        boolean asksQuest = containsAny(normalizedMessage, List.of("任务", "委托", "悬赏", "quest"));
        return asksRecommendation && asksQuest;
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
