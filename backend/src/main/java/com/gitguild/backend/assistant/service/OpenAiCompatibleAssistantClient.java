package com.gitguild.backend.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiCompatibleAssistantClient implements AssistantAiClient {

    private static final int MAX_ANSWER_LENGTH = 1000;
    private static final int MAX_SUGGESTION_LENGTH = 80;
    private static final int MAX_SUGGESTIONS = 3;

    private final AssistantAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleAssistantClient(AssistantAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    OpenAiCompatibleAssistantClient(
            AssistantAiProperties properties,
            ObjectMapper objectMapper,
            RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public Optional<AssistantAnswerResult> tryAnswer(AssistantChatContext context) {
        if (!properties.isReady()) {
            return Optional.empty();
        }

        try {
            String body = restClient.post()
                    .uri(chatCompletionsEndpoint())
                    .body(requestBody(context))
                    .retrieve()
                    .body(String.class);
            return parseResponse(body);
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static RestClient createRestClient(AssistantAiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeoutMs());
        requestFactory.setReadTimeout(properties.timeoutMs());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + nullToEmpty(properties.apiKey()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Map<String, Object> requestBody(AssistantChatContext context) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(context))));
        request.put("temperature", 0.2);
        request.put("max_tokens", 700);
        request.put("response_format", Map.of("type", "json_object"));
        return request;
    }

    private String systemPrompt() {
        return """
                你是艾丽丝，英文名 Alice，是代码公会 GitGuild 的 AI 向导，面向来访者与公会成员回答平台使用问题。公会成员是统一账号，既可以接取委托完成开发，也可以发布委托并审核成果。

                角色与边界：
                1. 你的自称只能是“艾丽丝”或“GitGuild AI 向导”；不要自称“看板娘”。
                2. 你的性格热情、善良、耐心、可靠。回答要让用户感到被欢迎和被帮助，但不要过度卖萌、夸张或使用表情符号。
                3. 只回答 GitGuild 的平台使用、委托流程、成果提交、提交审核、PR 合并、仓库接入、Git/Gitea 基础操作、成长系统相关问题。
                4. 不处理管理员后台问题，不索要或输出密码、Token、密钥等敏感信息。
                5. 不承诺已经替用户提交、审核、发布、接取委托、合并 PR 或跳转；页面跳转由前端按钮处理，PR 合并由用户在审核页面自主操作。
                6. 不得基于未提供的个人画像或状态编造用户偏好、委托状态、提交状态、审核意见或 PR 信息。
                7. 如果问题超出范围，简短说明你能回答的范围，并给出可继续追问的方向。

                回答策略：
                1. 先判断用户意图：平台介绍、发布委托、接取委托、接取后的工作、提交成果、审核成果、状态查询、委托推荐、成长记录、异常排查、超出范围。
                2. 先给一句明确结论，再给 1-3 个可执行步骤。简单问题用 2-4 句话，流程问题控制在 3 步以内。
                3. 用户问“下一步”“怎么办”“哪里看”时，必须说明去哪个页面、看什么、做什么，不要只重复页面名称。
                4. 用户问题模糊时，先回答最可能的场景，再用一句话澄清。
                5. 推荐新委托时，只能从“可推荐的委托板候选”中选择，并说明 questId、标题和推荐理由；没有候选时明确说明暂无可推荐委托。
                6. GitGuild 已提供单独的合并 PR 按钮。委托人审核成果时，可以根据验收结果和 PR 状态自主选择是否通过该按钮合并 PR；不要引导用户必须回到 Gitea 手动合并，也不能承诺已经替用户合并 PR。
                7. 用简洁中文回答，必要时可以保留 Git、PR、Issue、clone、push 等英文技术术语。

                身份权限策略：
                1. 公会成员账号已统一：登录成员同时拥有“接取委托、进入工作台开发、提交成果”和“发布委托、仓库接入、审核成果、合并 PR”的全部能力。对登录成员的问题，直接给出可执行步骤，不要说“你不能执行该操作”，也不要让其切换账号。
                2. 接任务相关功能（接取、开发、提交）与发委托相关功能（发布、审核、合并 PR）都是同一个成员账号可做的事，按用户问到的场景给出对应步骤即可。
                3. 来访者（未登录）询问需要登录的功能时，先说明需要登录或注册成为成员，再给简短流程概览。
                4. 管理员后台不在你的服务范围。

                状态查询策略：
                1. 用户询问“我的委托/提交/PR/审核为什么还没通过、现在到哪一步”时，先查看可见状态数据。
                2. 如果可见状态中有相关记录，就根据状态解释含义并给出下一步。
                3. 如果没有相关记录，就自然说明暂时没有看到记录，并引导用户去提交柜台、工作台或审核台查看。
                4. 不要暴露“上下文”“系统数据”“模型没有数据”等实现表述。

                输出格式：
                只输出严格 JSON，不要 Markdown，不要代码块，格式为：
                {"answer":"回答文本","suggestedQuestions":["后续问题1","后续问题2"]}

                示例：
                当前用户身份：委托人
                用户：我怎么发布委托？
                输出：{"answer":"作为公会成员你可以直接发布委托。先到仓库接入页导入或同步 Gitea 仓库，让 GitGuild 读取仓库里的 Issue；再从 Issue 生成委托草稿，补充标题、验收标准、难度、技术栈和奖励 XP；提交后等待管理员审核，通过后委托就会出现在悬赏任务板。","suggestedQuestions":["如何接入仓库？","为什么我发布的委托还没出现在任务板？"]}

                当前用户身份：委托人
                用户：我能审核别人的提交吗？
                输出：{"answer":"可以。作为公会成员，你可以在提交审核台查看自己发布的委托收到的成果：先阅读成果说明，再打开关联 PR 检查实现范围是否满足验收标准，然后选择通过、退回修改或驳回。若 PR 可合并，你还能在审核页用合并 PR 按钮自主合并。","suggestedQuestions":["退回修改意见怎么写？","什么时候需要合并 PR？"]}

                当前用户身份：来访者
                用户：我怎么发布委托？
                输出：{"answer":"发布委托需要先登录或注册成为公会成员。成为成员后，你可以在仓库接入页导入仓库、从 Issue 生成委托草稿，补充验收标准与奖励后提交管理员审核，通过即可上架。","suggestedQuestions":["如何登录或注册？","成员都能做什么？"]}

                用户：委托人怎么发布委托？
                输出：{"answer":"委托人发布委托通常从仓库接入开始。你需要先导入或同步 Gitea 仓库，让 GitGuild 读取仓库里的 Issue；然后从 Issue 生成委托草稿，补充标题、说明、验收标准、难度、技术栈和奖励 XP；提交后等待管理员审核，通过后委托才会出现在悬赏任务板。","suggestedQuestions":["如何接入仓库？","为什么我发布的委托还没出现在任务板？"]}

                用户：冒险家怎么接取委托？
                输出：{"answer":"冒险家可以先进入悬赏任务板，按难度、技术栈或关键词筛选适合自己的委托。打开委托详情后，重点确认背景说明、验收标准、奖励 XP 和技术要求；确认适合后再点击接取委托。接取成功后，这个委托会进入你的冒险家工作台。","suggestedQuestions":["接取后下一步做什么？","如何筛选适合我的委托？"]}

                用户：我接了委托之后该怎么做？
                输出：{"answer":"接取委托后，下一步是进入冒险家工作台查看任务分支和仓库信息。你可以按照页面给出的 Git 步骤 clone 仓库、切换任务分支、完成代码修改并 push；开发完成后，再回到 GitGuild 的提交柜台登记成果和 PR 信息。","suggestedQuestions":["任务分支是什么？","如何提交成果？"]}

                用户：我怎么提交成果？
                输出：{"answer":"提交成果需要在提交柜台完成。你需要选择对应的已接取委托，确认或填写 PR 信息，补充成果说明和必要证据，然后提交给委托人审核。提交后可以回到提交柜台查看审核状态，如果被退回，就根据审核意见修改后重新提交。","suggestedQuestions":["PR 链接在哪里填写？","被退回后怎么重新提交？"]}

                用户：委托人怎么审核冒险家的提交？
                输出：{"answer":"委托人可以进入提交审核台查看待审核成果。审核时建议先阅读成果说明，再查看关联 PR，确认实现内容是否满足委托的验收标准；符合要求可以通过，不符合可以退回修改并写清楚原因。如果平台检测到可合并的 PR，你还可以在审核时通过单独的合并 PR 按钮自主选择是否合并，不需要回到 Gitea 操作。","suggestedQuestions":["退回修改意见怎么写？","什么时候需要合并 PR？"]}

                用户：个人成长记录有什么用？
                输出：{"answer":"个人成长记录用于展示你在 GitGuild 中的贡献表现，包括等级、XP、完成委托数量、贡献记录和徽章等信息。冒险家完成并通过审核的委托会增加成长记录；这些数据也可以帮助艾丽丝在推荐委托时更贴近你的技术栈和难度偏好。","suggestedQuestions":["XP 怎么增加？","在哪里查看排行榜？"]}

                用户：我的提交为什么还没通过？
                输出：{"answer":"我暂时没有看到你的提交记录，所以不能直接判断原因。你可以先到提交柜台查看状态：如果是待审核，说明委托人还没有处理；如果是退回修改，就需要根据审核意见修改后重新提交。","suggestedQuestions":["在哪里看提交状态？","被退回后怎么处理？"]}

                用户：推荐一个适合我的委托。
                输出：{"answer":"我会优先根据你的等级、XP、完成记录、常用技术栈和当前悬赏任务板中的可接取委托来推荐。如果有可推荐候选，我会说明委托编号、标题和推荐理由；如果暂时没有候选，我会建议你到悬赏任务板按技术栈和难度筛选。","suggestedQuestions":["如何提升推荐匹配度？","去悬赏任务板看看"]}
                """;
    }

    private String userPrompt(AssistantChatContext context) {
        String prompt = """
                当前页面上下文：%s
                当前页面可做事项：%s
                当前用户身份：%s
                当前用户成长画像：%s
                可推荐的委托板候选：%s
                已授权可见的委托状态：%s
                已授权可见的提交状态：%s
                用户问题：
                %s
                """.formatted(
                blankToUnknown(context.page()),
                pageCapabilityText(context.page()),
                context.roleLabel(),
                userProfileText(context),
                questRecommendationText(context),
                questStatusText(context),
                submissionStatusText(context),
                nullToEmpty(context.message()));
        return truncate(prompt, properties.maxPromptChars());
    }

    private String pageCapabilityText(String page) {
        return switch (blankToUnknown(page)) {
            case "front-desk" -> "AI 前台页，可提问、查看本地历史记录，并通过回答下方按钮跳转到相关页面";
            case "hall" -> "公会大厅，可进入悬赏任务板、工作台、提交柜台、成长档案、排行榜和 AI 前台";
            case "quest-board" -> "悬赏任务板，可浏览、筛选、查看并接取已发布委托";
            case "adventurer-workbench" -> "冒险家工作台，可查看已接取委托、任务分支、仓库信息和 Git 操作步骤";
            case "maintainer-workbench" -> "委托人工作台，可管理仓库、发布委托并进入提交审核流程";
            case "maintainer-publish" -> "委托发布页，可从仓库 Issue 生成委托草稿并提交审核";
            case "maintainer-review" -> "提交审核台，可查看待审核成果、关联 PR、通过、退回、驳回，并可通过单独的合并 PR 按钮自主合并 PR";
            case "submission-counter" -> "提交柜台，可选择已接取委托、登记成果说明和 PR 信息，并查看提交状态";
            case "repository-sync" -> "仓库接入页，可导入或同步 Gitea 仓库，并读取 Issue 作为委托来源";
            case "profile" -> "个人成长档案页，可查看等级、XP、完成委托、贡献记录和徽章";
            case "leaderboard" -> "排行榜页，可查看用户 XP 和成长表现排名";
            case "help" -> "帮助说明页，可查看 GitGuild 角色流程和基础使用说明";
            default -> "未知页面，仅根据用户问题和可见状态给出通用引导";
        };
    }

    private String userProfileText(AssistantChatContext context) {
        if (context.userProfile() == null) {
            return "无";
        }
        AssistantChatContext.UserProfileSnapshot profile = context.userProfile();
        return "level=%d,totalXp=%d,completedQuestCount=%d,preferredTechStacks=%s,difficultyComfort=%s"
                .formatted(
                        profile.level(),
                        profile.totalXp(),
                        profile.completedQuestCount(),
                        profile.preferredTechStacks(),
                        profile.difficultyComfort());
    }

    private String questRecommendationText(AssistantChatContext context) {
        if (context.questBoardCandidates().isEmpty()) {
            return "无";
        }
        return context.questBoardCandidates().stream()
                .limit(8)
                .map(candidate -> "questId=%s,title=%s,difficulty=%s,techStack=%s,rewardXp=%d,estimatedHours=%d,category=%s,tags=%s,status=%s,matchScore=%.2f,reasons=%s"
                        .formatted(
                                candidate.questId(),
                                clean(candidate.title()),
                                clean(candidate.difficulty()),
                                candidate.techStack(),
                                candidate.rewardXp(),
                                candidate.estimatedHours(),
                                clean(candidate.categoryName()),
                                candidate.tagNames(),
                                clean(candidate.status()),
                                candidate.matchScore(),
                                candidate.reasons()))
                .toList()
                .toString();
    }

    private String questStatusText(AssistantChatContext context) {
        if (context.questStatuses().isEmpty()) {
            return "无";
        }
        return context.questStatuses().stream()
                .limit(5)
                .map(status -> "questId=%s,title=%s,questStatus=%s,assignmentId=%s,assignmentStatus=%s,taskBranch=%s,repository=%s,defaultBranch=%s,nextAction=%s"
                        .formatted(
                                status.questId(),
                                clean(status.title()),
                                clean(status.questStatus()),
                                status.assignmentId(),
                                clean(status.assignmentStatus()),
                                clean(status.taskBranch()),
                                clean(status.repositoryName()),
                                clean(status.repositoryDefaultBranch()),
                                clean(status.nextAction())))
                .toList()
                .toString();
    }

    private String submissionStatusText(AssistantChatContext context) {
        if (context.submissionStatuses().isEmpty()) {
            return "无";
        }
        return context.submissionStatuses().stream()
                .limit(5)
                .map(status -> "submissionId=%s,questId=%s,questTitle=%s,submissionStatus=%s,submittedAt=%s,pullRequestId=%s,externalPrId=%s,prTitle=%s,prStatus=%s,sourceBranch=%s,targetBranch=%s,merged=%s,latestReviewDecision=%s,latestReviewSummary=%s,latestReviewedAt=%s,nextAction=%s"
                        .formatted(
                                status.submissionId(),
                                status.questId(),
                                clean(status.questTitle()),
                                clean(status.submissionStatus()),
                                clean(status.submittedAt()),
                                status.pullRequestId(),
                                clean(status.externalPrId()),
                                clean(status.prTitle()),
                                clean(status.prStatus()),
                                clean(status.sourceBranch()),
                                clean(status.targetBranch()),
                                status.merged(),
                                clean(status.latestReviewDecision()),
                                clean(status.latestReviewSummary()),
                                clean(status.latestReviewedAt()),
                                clean(status.nextAction())))
                .toList()
                .toString();
    }

    private Optional<AssistantAnswerResult> parseResponse(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual()) {
            return Optional.empty();
        }

        return parseAssistantContent(contentNode.asText());
    }

    private Optional<AssistantAnswerResult> parseAssistantContent(String content) throws JsonProcessingException {
        String json = extractJsonObject(content);
        JsonNode root = objectMapper.readTree(json);
        String answer = truncate(clean(root.path("answer").asText("")), MAX_ANSWER_LENGTH);
        if (answer.isBlank()) {
            return Optional.empty();
        }

        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionNodes = root.path("suggestedQuestions");
        if (suggestionNodes.isArray()) {
            for (JsonNode node : suggestionNodes) {
                String suggestion = truncate(clean(node.asText("")), MAX_SUGGESTION_LENGTH);
                if (!suggestion.isBlank()) {
                    suggestions.add(suggestion);
                }
                if (suggestions.size() == MAX_SUGGESTIONS) {
                    break;
                }
            }
        }

        return Optional.of(new AssistantAnswerResult(answer, AssistantAnswerSource.AI, List.copyOf(suggestions)));
    }

    private String chatCompletionsEndpoint() {
        return trimTrailingSlash(properties.baseUrl()) + "/chat/completions";
    }

    private String extractJsonObject(String content) {
        String trimmed = nullToEmpty(content).trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Assistant response does not contain a JSON object.");
        }
        return trimmed.substring(start, end + 1);
    }

    private String clean(String value) {
        return nullToEmpty(value).replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxChars) {
        String text = nullToEmpty(value);
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private static String trimTrailingSlash(String value) {
        String text = nullToEmpty(value).trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
