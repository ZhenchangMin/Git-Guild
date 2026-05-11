# API 规范：推荐匹配

## 1. 通用约定

基础路径：`/api/v1`

认证规则：需要登录或可选登录的接口通过 `Authorization` 请求头传递 JWT。

认证方式：

```http
Authorization: Bearer <JWT_TOKEN>
```

各接口是否必须携带该请求头，以接口内"请求头"表格为准。

统一成功响应：

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {},
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0001"
}
```

统一失败响应：

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "limit 不能超过 20",
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0002"
}
```

字段说明：

| 字段名         | 说明                                                                     |
| ----------- | ---------------------------------------------------------------------- |
| `score`     | 综合推荐分数，取值范围 `0.0 - 1.0`，由所选策略加权计算，不直接暴露各策略权重                          |
| `reasons`   | 推荐理由列表，按贡献度排序；每条由后端策略生成，对应一种 `RecommendationStrategy`                  |
| `strategy`  | 单条理由所属策略标识：`tech-stack`、`growth-stage`，未来可扩展                            |
| `strongMatch` | 是否为强匹配，对应 `RecommendationResult.isStrongMatch()`，前端可据此突出展示              |

## 2. 业务码/错误码

| 业务码       | HTTP 状态码    | 说明   |
| --------- | ----------- | ---- |
| `SUCCESS` | `200 / 201` | 请求成功 |

| 错误码                          | HTTP 状态码 | 说明                          |
| ---------------------------- | -------- | --------------------------- |
| `VALIDATION_FAILED`          | `400`    | 参数缺失或格式错误                   |
| `STRATEGY_NOT_AVAILABLE`     | `400`    | 指定的推荐策略不存在或未启用              |
| `FEEDBACK_SIGNAL_INVALID`    | `400`    | 推荐反馈信号不在允许的取值范围             |
| `UNAUTHORIZED`               | `401`    | 未登录或令牌无效                    |
| `FORBIDDEN`                  | `403`    | 当前用户无权访问此推荐结果               |
| `USER_NOT_FOUND`             | `404`    | 用户不存在                       |
| `QUEST_NOT_FOUND`            | `404`    | 任务不存在                       |
| `RECOMMENDATION_NOT_FOUND`   | `404`    | 没有对应的推荐结果，无法解释              |
| `RECOMMENDATION_UNAVAILABLE` | `409`    | 用户成长档案或技术栈信息不足，暂时无法生成推荐     |
| `INTERNAL_ERROR`             | `500`    | 服务器内部错误                     |

## 3. 获取个人推荐任务

接口：`GET /api/v1/recommendations/quests`

功能：为登录用户生成与其技术栈、成长阶段相匹配的任务推荐列表。结果由 `RecommendationService.recommendQuests` 组合策略生成。

权限：登录用户。结果始终基于令牌识别的当前用户，不接受 `userId` 参数。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                                                       |
| --------------- | -------- | --- | -------------------------------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`；被推荐用户由后端根据该令牌确定 |

Query 参数：

| 参数名                | 类型        | 必填  | 说明                                                                          |
| ------------------ | --------- | --- | --------------------------------------------------------------------------- |
| `strategy`         | `String`  | 否   | 指定单一策略：`tech-stack`、`growth-stage`；省略时使用组合策略                 |
| `beginnerFriendly` | `Boolean` | 否   | 是否只推荐新手友好任务，默认跟随用户成长阶段                                              |
| `excludeAccepted`  | `Boolean` | 否   | 是否排除当前用户已接取或已完成的任务，默认 `true`                                      |
| `limit`            | `Integer` | 否   | 返回数量，默认 `5`，最大 `20`                                                          |

请求示例：

```http
GET /api/v1/recommendations/quests?strategy=tech-stack&limit=5
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "user": {
      "userId": 3001,
      "username": "adventurer01"
    },
    "strategy": "tech-stack",
    "generatedAt": "2026-05-10T20:00:00+08:00",
    "items": [
      {
        "resultId": 6001,
        "quest": {
          "questId": 5001,
          "title": "实现 Issue 同步状态页面",
          "difficulty": "C",
          "techStack": ["Vue", "Spring Boot"],
          "rewardXp": 180,
          "estimatedHours": 6,
          "beginnerFriendly": true,
          "category": {
            "categoryId": 2,
            "name": "前端开发"
          },
          "repository": {
            "repositoryId": 1001,
            "name": "git-guild"
          }
        },
        "score": 0.86,
        "strongMatch": true,
        "reasons": [
          {
            "strategy": "tech-stack",
            "weight": 0.7,
            "text": "技术栈匹配 Vue、Spring Boot"
          },
          {
            "strategy": "growth-stage",
            "weight": 0.3,
            "text": "难度 C 与你当前成长阶段相符"
          }
        ]
      }
    ]
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0301"
}
```

### 失败响应

HTTP 状态码：`409 Conflict`

错误码：`RECOMMENDATION_UNAVAILABLE`

```json
{
  "code": "RECOMMENDATION_UNAVAILABLE",
  "message": "暂时无法生成推荐",
  "details": "用户尚未填写技术栈，且成长档案为空",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0302"
}
```

HTTP 状态码：`400 Bad Request`

错误码：`STRATEGY_NOT_AVAILABLE`

```json
{
  "code": "STRATEGY_NOT_AVAILABLE",
  "message": "指定的推荐策略不存在",
  "details": "strategy=collaborative-filter",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0303"
}
```

## 4. 为任务推荐贡献者

接口：`GET /api/v1/recommendations/quests/{questId}/contributors`

功能：维护者查看某任务的潜在贡献者推荐列表。结果由 `RecommendationService.recommendContributors` 生成，按综合分数降序返回。

权限：登录用户，且必须是该任务的发布者（`MAINTAINER`）或平台 `ADMIN`。

### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                              |
| --------------- | -------- | --- | ------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`   |

Path 参数：

| 参数名        | 类型     | 必填  | 说明      |
| ---------- | ------ | --- | ------- |
| `questId`  | `Long` | 是   | 任务 ID   |

Query 参数：

| 参数名         | 类型        | 必填  | 说明                                          |
| ----------- | --------- | --- | ------------------------------------------- |
| `strategy`  | `String`  | 否   | 指定单一策略：`tech-stack`、`growth-stage`；省略时使用组合策略 |
| `limit`     | `Integer` | 否   | 返回数量，默认 `5`，最大 `20`                          |
| `excludeAssigned` | `Boolean` | 否 | 是否排除已经接取过该任务的用户，默认 `true`                     |

请求示例：

```http
GET /api/v1/recommendations/quests/5001/contributors?limit=5
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "quest": {
      "questId": 5001,
      "title": "实现 Issue 同步状态页面"
    },
    "strategy": "combined",
    "generatedAt": "2026-05-10T20:00:00+08:00",
    "items": [
      {
        "resultId": 6101,
        "user": {
          "userId": 3001,
          "username": "adventurer01",
          "level": 5,
          "techStack": ["Vue", "TypeScript", "Spring Boot"]
        },
        "score": 0.82,
        "strongMatch": true,
        "reasons": [
          {
            "strategy": "tech-stack",
            "weight": 0.6,
            "text": "用户掌握 Vue、Spring Boot，覆盖任务全部技术栈"
          },
          {
            "strategy": "growth-stage",
            "weight": 0.4,
            "text": "用户当前等级与任务难度 C 匹配"
          }
        ]
      }
    ]
  },
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0304"
}
```

### 失败响应

HTTP 状态码：`403 Forbidden`

错误码：`FORBIDDEN`

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无权查看该任务的贡献者推荐",
  "details": "questId=5001",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0305"
}
```

HTTP 状态码：`404 Not Found`

错误码：`QUEST_NOT_FOUND`

```json
{
  "code": "QUEST_NOT_FOUND",
  "message": "任务不存在",
  "details": "questId=5001",
  "timestamp": "2026-05-10T20:00:00+08:00",
  "traceId": "req-0306"
}
```

## 5. 查询推荐理由

接口：`GET /api/v1/recommendations/reasons`

功能：查询某用户与某任务之间的推荐理由，对应 `RecommendationService.explain`。常用于推荐卡片的"为什么推荐我？"详情页或维护者查看候选人的匹配依据。

权限：登录用户。


### 请求参数

请求头：

| 参数名             | 类型       | 必填  | 说明                              |
| --------------- | -------- | --- | ------------------------------- |
| `Authorization` | `String` | 是   | 登录令牌，格式为 `Bearer <JWT_TOKEN>`   |

Query 参数：

| 参数名       | 类型     | 必填  | 说明                                                    |
| --------- | ------ | --- | ----------------------------------------------------- |
| `questId` | `Long` | 是   | 任务 ID                                                 |
| `userId`  | `Long` | 否   | 被推荐用户 ID；省略时默认当前登录用户                     |

请求示例：

```http
GET /api/v1/recommendations/explanations?questId=5001&userId=3001
Authorization: Bearer <JWT_TOKEN>
```

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "user": {
      "userId": 3001,
      "username": "adventurer01"
    },
    "quest": {
      "questId": 5001,
      "title": "实现 Issue 同步状态页面"
    },
    "score": 0.82,
    "strongMatch": true,
    "reasons": [
      {
        "strategy": "tech-stack",
        "weight": 0.6,
        "text": "用户掌握 Vue、Spring Boot，覆盖任务全部技术栈"
      },
      {
        "strategy": "growth-stage",
        "weight": 0.4,
        "text": "用户当前等级与任务难度 C 匹配"
      }
    ],
    "generatedAt": "2026-05-10T20:00:00+08:00"
  },
  "timestamp": "2026-05-10T20:05:00+08:00",
  "traceId": "req-0307"
}
```

### 失败响应

HTTP 状态码：`404 Not Found`

错误码：`RECOMMENDATION_NOT_FOUND`

```json
{
  "code": "RECOMMENDATION_NOT_FOUND",
  "message": "没有对应的推荐结果",
  "details": "userId=3001, questId=5001",
  "timestamp": "2026-05-10T20:05:00+08:00",
  "traceId": "req-0308"
}
```

HTTP 状态码：`403 Forbidden`

错误码：`FORBIDDEN`

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无权查看他人推荐理由",
  "details": "questId=5001, userId=3001",
  "timestamp": "2026-05-10T20:05:00+08:00",
  "traceId": "req-0309"
}
```