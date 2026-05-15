# API 规范：管理员审核任务发布与下架

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
  "details": "decision 不能为空",
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
| `FORBIDDEN` | `403` | 当前用户无管理员权限 |
| `QUEST_NOT_FOUND` | `404` | 任务不存在 |
| `QUEST_NOT_REVIEWABLE` | `409` | 当前任务状态不允许管理员审核 |
| `QUEST_ALREADY_PUBLISHED` | `409` | 任务已经发布，不能重复发布 |
| `QUEST_ALREADY_CLOSED` | `409` | 任务已经下架或关闭 |
| `ADMIN_REVIEW_DUPLICATED` | `409` | 管理员审核记录已存在，不能重复审核 |
| `INTERNAL_ERROR` | `500` | 服务器内部错误 |

---

## 3. 管理员审核任务发布 / 下架

接口：`POST /api/v1/quests/{questId}/admin-reviews`

功能：管理员对任务进行审核，决定是否允许任务发布，或者对已经发布的任务执行下架处理。该接口用于补充项目中的核心管理流程，保证任务在正式展示给初学者之前经过平台管理员审核。

权限：登录用户，角色必须为 `ADMIN`。

### 请求参数

请求头：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `Authorization` | `String` | 是 | 登录令牌，格式为 `Bearer <JWT_TOKEN>` |
| `Content-Type` | `String` | 是 | 固定为 `application/json` |

Path 参数：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `questId` | `Long` | 是 | 被管理员审核的任务 ID |

请求体：

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `decision` | `String` | 是 | 管理员审核结论，支持 `APPROVE_PUBLISH`、`REJECT_PUBLISH`、`TAKE_DOWN` |
| `reason` | `String` | 是 | 审核原因或处理说明，长度 1-500 |
| `visibleToPublisher` | `Boolean` | 否 | 是否将审核原因展示给任务发布者，默认 `true` |

decision 取值说明：

| decision | 说明 |
|---|---|
| `APPROVE_PUBLISH` | 审核通过，允许任务正式发布 |
| `REJECT_PUBLISH` | 审核不通过，退回任务发布申请 |
| `TAKE_DOWN` | 对已发布任务执行下架处理 |

请求示例：

```http
POST /api/v1/quests/2001/admin-reviews
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

请求体示例：审核通过并发布任务

```json
{
  "decision": "APPROVE_PUBLISH",
  "reason": "任务描述清晰，完成标准明确，适合作为新手贡献任务发布。",
  "visibleToPublisher": true
}
```

---

### 成功响应

HTTP 状态码：`201 Created`

业务码：`SUCCESS`

```json
{
  "code": "SUCCESS",
  "message": "任务审核已通过并发布",
  "data": {
    "adminReviewId": 701,
    "questId": 2001,
    "adminId": 1,
    "decision": "APPROVE_PUBLISH",
    "reason": "任务描述清晰，完成标准明确，适合作为新手贡献任务发布。",
    "questStatus": "PUBLISHED",
    "reviewedAt": "2026-05-10T21:00:00+08:00"
  },
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0301"
}
```

---

### 成功响应示例：审核不通过

如果管理员认为任务描述不完整、完成标准不清晰，或者不适合发布，可以退回任务发布申请。

请求体示例：

```json
{
  "decision": "REJECT_PUBLISH",
  "reason": "任务缺少明确的完成标准，请补充验收条件和预期提交内容。",
  "visibleToPublisher": true
}
```

响应示例：

```json
{
  "code": "SUCCESS",
  "message": "任务发布申请已退回",
  "data": {
    "adminReviewId": 702,
    "questId": 2001,
    "adminId": 1,
    "decision": "REJECT_PUBLISH",
    "reason": "任务缺少明确的完成标准，请补充验收条件和预期提交内容。",
    "questStatus": "DRAFT",
    "reviewedAt": "2026-05-10T21:10:00+08:00"
  },
  "timestamp": "2026-05-10T21:10:00+08:00",
  "traceId": "req-0302"
}
```

---

### 成功响应示例：管理员下架任务

如果任务已经发布，但后续发现内容不合规、描述有误或不再适合展示，管理员可以执行下架操作。

请求体示例：

```json
{
  "decision": "TAKE_DOWN",
  "reason": "该任务关联的 Issue 已关闭，任务不再适合继续展示。",
  "visibleToPublisher": true
}
```

响应示例：

```json
{
  "code": "SUCCESS",
  "message": "任务已下架",
  "data": {
    "adminReviewId": 703,
    "questId": 2001,
    "adminId": 1,
    "decision": "TAKE_DOWN",
    "reason": "该任务关联的 Issue 已关闭，任务不再适合继续展示。",
    "questStatus": "CLOSED",
    "reviewedAt": "2026-05-10T21:20:00+08:00"
  },
  "timestamp": "2026-05-10T21:20:00+08:00",
  "traceId": "req-0303"
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
  "details": "decision 只能为 APPROVE_PUBLISH、REJECT_PUBLISH 或 TAKE_DOWN",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0304"
}
```

HTTP 状态码：`401 Unauthorized`

错误码：`UNAUTHORIZED`

```json
{
  "code": "UNAUTHORIZED",
  "message": "未登录或令牌无效",
  "details": "Authorization header is missing or invalid",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0305"
}
```

HTTP 状态码：`403 Forbidden`

错误码：`FORBIDDEN`

```json
{
  "code": "FORBIDDEN",
  "message": "当前用户无管理员权限",
  "details": "只有 ADMIN 角色可以审核任务发布或执行下架操作",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0306"
}
```

HTTP 状态码：`404 Not Found`

错误码：`QUEST_NOT_FOUND`

```json
{
  "code": "QUEST_NOT_FOUND",
  "message": "任务不存在",
  "details": "questId=2001",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0307"
}
```

HTTP 状态码：`409 Conflict`

错误码：`QUEST_NOT_REVIEWABLE`

```json
{
  "code": "QUEST_NOT_REVIEWABLE",
  "message": "当前任务状态不允许管理员审核",
  "details": "questId=2001, currentStatus=COMPLETED",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0308"
}
```

HTTP 状态码：`409 Conflict`

错误码：`QUEST_ALREADY_PUBLISHED`

```json
{
  "code": "QUEST_ALREADY_PUBLISHED",
  "message": "任务已经发布，不能重复发布",
  "details": "questId=2001, currentStatus=PUBLISHED",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0309"
}
```

HTTP 状态码：`409 Conflict`

错误码：`QUEST_ALREADY_CLOSED`

```json
{
  "code": "QUEST_ALREADY_CLOSED",
  "message": "任务已经下架或关闭",
  "details": "questId=2001, currentStatus=CLOSED",
  "timestamp": "2026-05-10T21:00:00+08:00",
  "traceId": "req-0310"
}
```

---

## 5. 业务规则说明

1. 只有 `ADMIN` 角色可以调用该接口。
2. 当 `decision` 为 `APPROVE_PUBLISH` 时，任务状态必须为 `PENDING_ADMIN_REVIEW`。
3. 管理员审核通过后，任务状态变更为 `PUBLISHED`。
4. 当 `decision` 为 `REJECT_PUBLISH` 时，任务状态从 `PENDING_ADMIN_REVIEW` 回退为 `DRAFT`，发布者需要修改后重新提交审核。
5. 当 `decision` 为 `TAKE_DOWN` 时，任务状态通常从 `PUBLISHED` 变更为 `CLOSED`。
6. 每次管理员审核都会生成一条 `AdminReviewRecord`，用于记录管理员、审核结论、审核原因和审核时间。
7. 管理员审核完成后，系统应向任务发布者发送通知，告知任务发布申请通过、退回或已下架。
8. 已完成的任务原则上不允许被管理员重新审核发布；如需处理，应由后台管理流程单独处理。
