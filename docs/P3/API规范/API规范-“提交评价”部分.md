# API 规范：提交与审核

## 1. 通用约定

基础路径：`/api/v1`

认证规则：需要登录的接口通过 `Authorization` 请求头传递 JWT。

认证方式：

```http
Authorization: Bearer <JWT_TOKEN>
```

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
  "details": "summary 不能为空",
  "timestamp": "yyyy-mm-ddThh:mm:ss+08:00",
  "traceId": "req-0002"
}
```

---

## 2. 业务码 / 错误码

| 业务码 | HTTP 状态码 | 说明 |
|---|---|---|
| `SUCCESS` | `200 / 201` | 请求成功 |

| 错误码 | HTTP 状态码 | 说明 |
|---|---|---|
| `VALIDATION_FAILED` | `400` | 参数缺失或格式错误 |
| `UNAUTHORIZED` | `401` | 未登录或令牌无效 |
| `FORBIDDEN` | `403` | 当前用户无权限审核该提交 |
| `SUBMISSION_NOT_FOUND` | `404` | 提交记录不存在 |
| `QUEST_NOT_FOUND` | `404` | 提交对应的任务不存在 |
| `SUBMISSION_ALREADY_REVIEWED` | `409` | 该提交已经被审核，不能重复审核 |
| `SUBMISSION_NOT_REVIEWABLE` | `409` | 当前提交状态不允许审核 |
| `PR_NOT_MERGED` | `409` | 关联 PR 尚未合并，不能通过审核 |
| `INTERNAL_ERROR` | `500` | 服务器内部错误 |

---

## 3. 维护者审核提交并给出反馈

接口：`POST /api/v1/submissions/{submissionId}/reviews`

功能：维护者对用户提交的任务成果进行审核，给出审核结论和逐项反馈。该接口对应提交与审核模块中的 `ReviewService.reviewSubmission(command)`，审核后会生成 `ReviewRecord` 和若干 `ReviewItem`。审核通过后，可进一步触发成长激励模块记录 XP 和贡献记录。

权限：登录用户，角色必须为 `MAINTAINER` 或 `ADMIN`。普通初学者不能审核提交。

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `Authorization` | `String` | 是 | 登录令牌，格式为 `Bearer <JWT_TOKEN>` |
| `Content-Type` | `String` | 是 | 固定为 `application/json` |

Path 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `submissionId` | `Long` | 是 | 被审核的提交记录 ID |

请求体：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `decision` | `String` | 是 | 审核结论，支持 `APPROVED`、`CHANGES_REQUESTED`、`REJECTED` |
| `summary` | `String` | 是 | 审核总结，长度 1-500 |
| `items` | `Array` | 否 | 逐项审核意见列表 |
| `items[].checkpoint` | `String` | 是 | 审核检查项，例如代码规范、功能完整性、测试情况 |
| `items[].comment` | `String` | 否 | 对该检查项的具体反馈，长度 0-300 |
| `items[].passed` | `Boolean` | 是 | 该检查项是否通过 |

请求示例：

```http
POST /api/v1/submissions/1001/reviews
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：

```json
{
  "decision": "CHANGES_REQUESTED",
  "summary": "整体实现方向正确，但接口参数校验和异常处理还需要完善。",
  "items": [
    {
      "checkpoint": "功能完整性",
      "comment": "核心功能已经实现，但边界情况处理不足。",
      "passed": false
    },
    {
      "checkpoint": "代码规范",
      "comment": "命名清晰，代码结构较合理。",
      "passed": true
    },
    {
      "checkpoint": "测试情况",
      "comment": "缺少对异常输入的测试用例。",
      "passed": false
    }
  ]
}
```

---

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "提交审核已完成",
  "data": {
    "reviewId": 501,
    "submissionId": 1001,
    "reviewerId": 12,
    "decision": "CHANGES_REQUESTED",
    "summary": "整体实现方向正确，但接口参数校验和异常处理还需要完善。",
    "requiresChanges": true,
    "items": [
      {
        "itemId": 9001,
        "checkpoint": "功能完整性",
        "comment": "核心功能已经实现，但边界情况处理不足。",
        "passed": false
      },
      {
        "itemId": 9002,
        "checkpoint": "代码规范",
        "comment": "命名清晰，代码结构较合理。",
        "passed": true
      },
      {
        "itemId": 9003,
        "checkpoint": "测试情况",
        "comment": "缺少对异常输入的测试用例。",
        "passed": false
      }
    ],
    "submissionStatus": "CHANGES_REQUESTED",
    "reviewedAt": "2026-05-10T20:30:00+08:00"
  },
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0201"
}
```

---

### 成功响应示例：审核通过

如果维护者审核通过，`decision` 可以传入 `APPROVED`。

请求体示例：

```json
{
  "decision": "APPROVED",
  "summary": "功能实现完整，代码质量良好，已通过审核。",
  "items": [
    {
      "checkpoint": "功能完整性",
      "comment": "任务要求的功能均已完成。",
      "passed": true
    },
    {
      "checkpoint": "代码规范",
      "comment": "代码结构清晰，命名规范。",
      "passed": true
    },
    {
      "checkpoint": "测试情况",
      "comment": "已提供基本测试用例。",
      "passed": true
    }
  ]
}
```

响应示例：

```json
{
  "code": "SUCCESS",
  "message": "提交审核已通过",
  "data": {
    "reviewId": 502,
    "submissionId": 1001,
    "reviewerId": 12,
    "decision": "APPROVED",
    "summary": "功能实现完整，代码质量良好，已通过审核。",
    "requiresChanges": false,
    "submissionStatus": "APPROVED",
    "questStatus": "COMPLETED",
    "rewardXp": 80,
    "reviewedAt": "2026-05-10T20:40:00+08:00"
  },
  "timestamp": "2026-05-10T20:40:00+08:00",
  "traceId": "req-0202"
}
```

---

### 失败响应

HTTP 状态码：`400 Bad Request`

错误码：`VALIDATION_FAILED`

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数不合法",
  "details": "decision 只能为 APPROVED、CHANGES_REQUESTED 或 REJECTED",
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0203"
}
```

HTTP 状态码：`401 Unauthorized`

错误码：`UNAUTHORIZED`

```json
{
  "code": "UNAUTHORIZED",
  "message": "未登录或令牌无效",
  "details": "Authorization header is missing or invalid",
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0204"
}
```

HTTP 状态码：`403 Forbidden`

错误码：`FORBIDDEN`

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无权限审核该提交",
  "details": "只有任务发布者、仓库维护者或管理员可以审核提交",
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0205"
}
```

HTTP 状态码：`404 Not Found`

错误码：`SUBMISSION_NOT_FOUND`

```json
{
  "code": "SUBMISSION_NOT_FOUND",
  "message": "提交记录不存在",
  "details": "submissionId=1001",
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0206"
}
```

HTTP 状态码：`409 Conflict`

错误码：`SUBMISSION_ALREADY_REVIEWED`

```json
{
  "code": "SUBMISSION_ALREADY_REVIEWED",
  "message": "该提交已经被审核，不能重复审核",
  "details": "submissionId=1001, currentStatus=APPROVED",
  "timestamp": "2026-05-10T20:30:00+08:00",
  "traceId": "req-0207"
}
```

---

## 4. 业务规则说明

1. 只有状态为 `PENDING_REVIEW` 的提交可以被审核。
2. 审核结论为 `APPROVED` 时，提交状态更新为 `APPROVED`。
3. 审核结论为 `CHANGES_REQUESTED` 时，提交状态更新为 `CHANGES_REQUESTED`，用户需要根据反馈重新修改并提交。
4. 审核结论为 `REJECTED` 时，提交状态更新为 `REJECTED`，表示该提交无效或不符合任务要求。
5. 审核通过后，系统可触发成长激励流程，例如增加 XP、更新成长档案、生成贡献记录。
6. 审核完成后，系统应向提交人发送审核结果通知。
