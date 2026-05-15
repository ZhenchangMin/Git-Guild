# API 规范：新手引导

## 1. 通用约定

基础路径：`/api/v1`

认证规则：需要登录或可选登录的接口通过 `Authorization` 请求头传递 JWT。

认证方式：

```http
Authorization: Bearer <JWT_TOKEN>
```

各接口是否必须携带该请求头，以接口内“请求头”表格为准。

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
  "details": "repositoryId 必须为正整数",
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0002"
}
```

---

## 2. 业务码 / 错误码

| 业务码 | HTTP 状态码 | 说明 |
|---|---|---|
| `SUCCESS` | `200` | 请求成功 |

| 错误码 | HTTP 状态码 | 说明 |
|---|---|---|
| `VALIDATION_FAILED` | `400` | 参数缺失或格式错误 |
| `UNAUTHORIZED` | `401` | 未登录或令牌无效 |
| `FORBIDDEN` | `403` | 当前用户无权限查看该仓库引导 |
| `REPOSITORY_NOT_FOUND` | `404` | 仓库不存在 |
| `PROJECT_GUIDE_NOT_FOUND` | `404` | 该仓库尚未配置项目指引 |
| `REPOSITORY_UNAVAILABLE` | `409` | 仓库当前不可用，例如同步失败或已被禁用 |
| `INTERNAL_ERROR` | `500` | 服务器内部错误 |

---

## 3. 获取项目结构与贡献指引

接口：`GET /api/v1/repositories/{repositoryId}/guide`

功能：获取指定仓库的新手引导内容，包括项目结构说明、运行说明、贡献流程、推荐阅读顺序和常见问题，帮助新手快速理解项目核心能力并完成首次贡献。

权限：公开仓库可公开访问。私有仓库或受限仓库需要登录，并且当前用户必须具有访问权限。

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `Authorization` | `String` | 否 | 登录令牌，格式为 `Bearer <JWT_TOKEN>`。公开仓库可不传；私有或受限仓库必须传 |

Path 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `repositoryId` | `Long` | 是 | 仓库 ID |

Query 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `includeFaq` | `Boolean` | 否 | 是否返回常见问题，默认 `true` |
| `includeRecommendedQuests` | `Boolean` | 否 | 是否返回适合新手的推荐任务，默认 `false` |
| `language` | `String` | 否 | 指引语言，支持 `zh-CN`、`en-US`，默认 `zh-CN` |

请求示例：

```http
GET /api/v1/repositories/3001/guide?includeFaq=true&includeRecommendedQuests=true&language=zh-CN
```

请求示例：访问私有仓库指引

```http
GET /api/v1/repositories/3001/guide?includeFaq=true
Authorization: Bearer <JWT_TOKEN>
```

---

### 成功响应

HTTP 状态码：`200 OK`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "guideId": 801,
    "repositoryId": 3001,
    "repositoryName": "gitguild-backend",
    "projectOverview": "GitGuild 后端服务，负责用户认证、任务管理、提交审核、成长激励和通知等核心能力。",
    "techStack": [
      "Java",
      "Spring Boot",
      "MySQL",
      "Redis"
    ],
    "projectStructure": {
      "root": "gitguild-backend",
      "items": [
        {
          "path": "src/main/java/com/gitguild/auth",
          "description": "用户认证与权限相关代码"
        },
        {
          "path": "src/main/java/com/gitguild/quest",
          "description": "任务创建、发布、接取与查询相关代码"
        },
        {
          "path": "src/main/java/com/gitguild/review",
          "description": "成果提交、维护者审核和管理员审核相关代码"
        },
        {
          "path": "src/main/resources/application.yml",
          "description": "项目运行配置文件"
        }
      ]
    },
    "runInstructions": {
      "environment": [
        "JDK 17 或以上版本",
        "MySQL 8.0 或以上版本",
        "Redis 6.0 或以上版本",
        "Maven 3.8 或以上版本"
      ],
      "steps": [
        {
          "order": 1,
          "title": "克隆项目",
          "command": "git clone https://github.com/example/gitguild-backend.git"
        },
        {
          "order": 2,
          "title": "初始化数据库",
          "command": "mysql -u root -p gitguild < docs/sql/init.sql"
        },
        {
          "order": 3,
          "title": "修改配置文件",
          "command": "vim src/main/resources/application.yml"
        },
        {
          "order": 4,
          "title": "启动项目",
          "command": "mvn spring-boot:run"
        }
      ]
    },
    "contributionSteps": [
      {
        "order": 1,
        "title": "选择新手任务",
        "description": "在任务列表中筛选 difficulty 为 A 或 B 的任务，并阅读任务完成标准。"
      },
      {
        "order": 2,
        "title": "创建开发分支",
        "description": "从主分支拉取最新代码，并基于任务 ID 创建独立分支。"
      },
      {
        "order": 3,
        "title": "完成代码修改",
        "description": "按照任务描述完成代码实现，并补充必要的测试。"
      },
      {
        "order": 4,
        "title": "提交 Pull Request",
        "description": "提交 PR 后，在 GitGuild 中关联对应任务并提交成果。"
      },
      {
        "order": 5,
        "title": "根据审核意见修改",
        "description": "如果维护者要求修改，需要根据 ReviewItem 逐项处理后重新提交。"
      }
    ],
    "faq": [
      {
        "question": "第一次贡献应该选择什么任务？",
        "answer": "建议优先选择难度为 A、标签包含“新手友好”或“首次贡献”的任务。"
      },
      {
        "question": "提交成果时必须关联 Pull Request 吗？",
        "answer": "代码类任务通常需要关联 Pull Request；文档类任务可根据任务要求决定。"
      }
    ],
    "recommendedQuests": [
      {
        "questId": 5001,
        "title": "补充用户登录接口参数校验",
        "difficulty": "A",
        "rewardXp": 40
      },
      {
        "questId": 5002,
        "title": "完善任务详情页接口文档",
        "difficulty": "A",
        "rewardXp": 30
      }
    ],
    "updatedAt": "2026-05-10T22:00:00+08:00"
  },
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0401"
}
```

---

### 成功响应示例：不返回推荐任务

当 `includeRecommendedQuests=false` 时，响应中可以不包含 `recommendedQuests` 字段。

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "guideId": 801,
    "repositoryId": 3001,
    "repositoryName": "gitguild-backend",
    "projectOverview": "GitGuild 后端服务，负责用户认证、任务管理、提交审核、成长激励和通知等核心能力。",
    "techStack": [
      "Java",
      "Spring Boot",
      "MySQL",
      "Redis"
    ],
    "projectStructure": {
      "root": "gitguild-backend",
      "items": [
        {
          "path": "src/main/java/com/gitguild/quest",
          "description": "任务核心模块"
        },
        {
          "path": "src/main/java/com/gitguild/review",
          "description": "提交与审核模块"
        }
      ]
    },
    "runInstructions": {
      "environment": [
        "JDK 17 或以上版本",
        "MySQL 8.0 或以上版本"
      ],
      "steps": [
        {
          "order": 1,
          "title": "启动项目",
          "command": "mvn spring-boot:run"
        }
      ]
    },
    "contributionSteps": [
      {
        "order": 1,
        "title": "阅读任务说明",
        "description": "理解任务背景、完成标准和提交要求。"
      }
    ],
    "updatedAt": "2026-05-10T22:00:00+08:00"
  },
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0402"
}
```

---

## 4. 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "language 只能为 zh-CN 或 en-US",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0403"
}
```

HTTP 状态码：`401 Unauthorized`

错误码：`UNAUTHORIZED`

```json
{
  "code": "UNAUTHORIZED",
  "message": "未登录或令牌无效",
  "details": "访问私有仓库指引需要登录",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0404"
}
```

HTTP 状态码：`403 Forbidden`

错误码：`FORBIDDEN`

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无权限查看该仓库引导",
  "details": "repositoryId=3001",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0405"
}
```

HTTP 状态码：`404 Not Found`

错误码：`REPOSITORY_NOT_FOUND`

```json
{
  "code": "REPOSITORY_NOT_FOUND",
  "message": "仓库不存在",
  "details": "repositoryId=3001",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0406"
}
```

HTTP 状态码：`404 Not Found`

错误码：`PROJECT_GUIDE_NOT_FOUND`

```json
{
  "code": "PROJECT_GUIDE_NOT_FOUND",
  "message": "该仓库尚未配置项目指引",
  "details": "repositoryId=3001",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0407"
}
```

HTTP 状态码：`409 Conflict`

错误码：`REPOSITORY_UNAVAILABLE`

```json
{
  "code": "REPOSITORY_UNAVAILABLE",
  "message": "仓库当前不可用，无法获取项目指引",
  "details": "repositoryId=3001, syncStatus=FAILED",
  "timestamp": "2026-05-10T22:00:00+08:00",
  "traceId": "req-0408"
}
```

---

## 5. 业务规则说明

1. 该接口用于新手快速理解某个仓库的项目结构、运行方式和贡献流程。
2. 公开仓库的项目指引可以不登录访问；私有仓库或受限仓库必须登录后访问。
3. 当 `includeFaq=true` 时，响应中返回 `faq` 字段。
4. 当 `includeRecommendedQuests=true` 时，响应中返回适合新手的推荐任务列表。
5. 如果仓库同步失败、被禁用或不可访问，应返回 `REPOSITORY_UNAVAILABLE`。
6. 如果仓库存在但未维护项目指引，应返回 `PROJECT_GUIDE_NOT_FOUND`。
7. 项目指引内容应尽量面向初学者，避免只给出复杂命令而缺少解释。
8. 项目结构、运行说明和贡献步骤应由维护者定期更新，确保与仓库最新状态一致。
