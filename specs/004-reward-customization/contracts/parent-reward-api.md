# Reward Management API Contract (Parent)

**Endpoint Base**: `/api/parent/rewards`  
**Authentication**: Bearer token (parent user must have role == PARENT)  
**Content-Type**: application/json

---

## 1. List Family & Per-Child Rewards

**Endpoint**: `GET /api/parent/rewards`

**Query Parameters**:
- `includeDeleted` (optional, boolean): If true, include archived rewards (default: false)

**Response** (200 OK):

```json
{
  "familyRewards": [
    {
      "rewardTemplateId": "uuid-1",
      "rewardType": "MONEY",
      "amount": 0.50,
      "unit": "PER_CHAPTER",
      "description": "$0.50 per chapter",
      "cannedTemplateId": "MONEY_050_PER_CHAPTER",
      "isDeleted": false,
      "createdAt": "2026-05-11T10:00:00Z"
    },
    {
      "rewardTemplateId": "uuid-2",
      "rewardType": "TIME",
      "amount": 5,
      "unit": "PER_CHAPTER",
      "description": "5 min screentime per chapter",
      "cannedTemplateId": "TIME_5MIN_PER_CHAPTER",
      "isDeleted": false,
      "createdAt": "2026-05-11T10:00:00Z"
    }
  ],
  "perChildRewards": [
    {
      "childId": "uuid-child-1",
      "childName": "Alice",
      "rewards": [
        {
          "rewardTemplateId": "uuid-3",
          "rewardType": "MONEY",
          "amount": 1.00,
          "unit": "PER_CHAPTER",
          "description": "Custom: $1.00 per chapter (Alice only)",
          "cannedTemplateId": null,
          "isDeleted": false,
          "createdAt": "2026-05-11T10:15:00Z"
        }
      ]
    }
  ]
}
```

**Error** (403 Forbidden):
```json
{
  "error": "Not authorized to view rewards"
}
```

---

## 2. Create Family-Level Reward

**Endpoint**: `POST /api/parent/rewards`

**Request Body**:

```json
{
  "rewardType": "MONEY",
  "amount": 0.75,
  "unit": "PER_CHAPTER",
  "description": "Custom family reward",
  "cannedTemplateId": null
}
```

**Response** (201 Created):

```json
{
  "rewardTemplateId": "uuid-new",
  "rewardType": "MONEY",
  "amount": 0.75,
  "unit": "PER_CHAPTER",
  "description": "Custom family reward",
  "cannedTemplateId": null,
  "isDeleted": false,
  "createdAt": "2026-05-11T10:30:00Z"
}
```

**Error** (400 Bad Request):
```json
{
  "error": "amount must be >= 0"
}
```

---

## 3. Create Per-Child Reward Override

**Endpoint**: `POST /api/parent/rewards/child/{childId}`

**Path Parameters**:
- `childId` (UUID): The child ID to create reward for

**Request Body**:

```json
{
  "rewardType": "TIME",
  "amount": 30,
  "unit": "PER_BOOK",
  "description": "30 min screentime per book (override for Alice)"
}
```

**Response** (201 Created):

```json
{
  "rewardTemplateId": "uuid-new-child",
  "childId": "uuid-child-1",
  "rewardType": "TIME",
  "amount": 30,
  "unit": "PER_BOOK",
  "description": "30 min screentime per book (override for Alice)",
  "cannedTemplateId": null,
  "isDeleted": false,
  "createdAt": "2026-05-11T10:45:00Z"
}
```

**Error** (403 Forbidden - if child doesn't belong to parent):
```json
{
  "error": "Child not found in family"
}
```

---

## 4. Update Reward Template

**Endpoint**: `PUT /api/parent/rewards/{rewardTemplateId}`

**Path Parameters**:
- `rewardTemplateId` (UUID): The reward template to update

**Request Body** (partial update):

```json
{
  "amount": 1.00,
  "description": "Updated: $1.00 per chapter"
}
```

**Response** (200 OK):

```json
{
  "rewardTemplateId": "uuid-1",
  "rewardType": "MONEY",
  "amount": 1.00,
  "unit": "PER_CHAPTER",
  "description": "Updated: $1.00 per chapter",
  "cannedTemplateId": null,
  "isDeleted": false,
  "updatedAt": "2026-05-11T11:00:00Z"
}
```

---

## 5. Archive (Soft-Delete) Reward

**Endpoint**: `DELETE /api/parent/rewards/{rewardTemplateId}`

**Path Parameters**:
- `rewardTemplateId` (UUID): The reward to archive

**Request Body**: (empty)

**Response** (200 OK):

```json
{
  "rewardTemplateId": "uuid-1",
  "isDeleted": true,
  "archivedAt": "2026-05-11T11:15:00Z"
}
```

**Effect**:
- Sets `is_deleted = true` on RewardTemplate
- Existing RewardSelection entries using this template remain active for in-progress books
- New RewardSelection calls will exclude this template from available options
- Parent receives confirmation in-app

---

## 6. Get Child Reward History & Accumulation

**Endpoint**: `GET /api/parent/rewards/child/{childId}/accumulation`

**Path Parameters**:
- `childId` (UUID): The child to view rewards for

**Query Parameters**:
- `status` (optional): Filter by EARNED, PENDING_PAYOUT, or PAID
- `limit` (optional, default 50): Pagination limit
- `offset` (optional, default 0): Pagination offset

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "childName": "Alice",
  "summary": {
    "totalEarned": 25.50,
    "totalPaid": 15.00,
    "availableBalance": 10.50
  },
  "accumulations": [
    {
      "accumulationId": "uuid-acc-1",
      "bookReadId": "uuid-book-1",
      "rewardType": "MONEY",
      "amountEarned": 5.00,
      "unitCount": 10,
      "calculationNote": "10 chapters @ $0.50",
      "status": "PAID",
      "payoutDate": "2026-05-10T18:00:00Z",
      "createdAt": "2026-05-09T15:30:00Z"
    },
    {
      "accumulationId": "uuid-acc-2",
      "bookReadId": "uuid-book-2",
      "rewardType": "MONEY",
      "amountEarned": 10.50,
      "unitCount": 21,
      "calculationNote": "21 chapters @ $0.50",
      "status": "PENDING_PAYOUT",
      "payoutDate": null,
      "createdAt": "2026-05-11T10:00:00Z",
      "relatedMessageId": "uuid-msg-1"
    }
  ]
}
```

---

## 7. Confirm Payout for Child

**Endpoint**: `POST /api/parent/rewards/child/{childId}/payout-confirm`

**Path Parameters**:
- `childId` (UUID): The child being paid out

**Request Body**:

```json
{
  "accumulationIds": ["uuid-acc-2"],
  "payoutMethod": "Cash (optional note for parent's own records)"
}
```

**Response** (200 OK):

```json
{
  "payoutId": "uuid-payout-1",
  "childId": "uuid-child-1",
  "amount": 10.50,
  "accumulationIds": ["uuid-acc-2"],
  "status": "PAID",
  "confirmedAt": "2026-05-11T11:30:00Z",
  "message": {
    "messageId": "uuid-msg-2",
    "messageType": "PAYOUT_CONFIRMATION",
    "messageText": "Your payout of $10.50 has been confirmed!"
  }
}
```

**Effect**:
- Updates all `accumulations[*].status` to PAID
- Creates a PAYOUT_CONFIRMATION message sent to child
- Child receives in-app notification

---

## 8. Get Pending Payouts for All Children

**Endpoint**: `GET /api/parent/rewards/payouts-pending`

**Query Parameters**: (none)

**Response** (200 OK):

```json
{
  "totalPending": 25.50,
  "children": [
    {
      "childId": "uuid-child-1",
      "childName": "Alice",
      "pendingAmount": 10.50,
      "pendingAccumulations": 2,
      "lastReminderAt": "2026-05-11T09:00:00Z",
      "reminderCount": 1
    },
    {
      "childId": "uuid-child-2",
      "childName": "Bob",
      "pendingAmount": 15.00,
      "pendingAccumulations": 3,
      "lastReminderAt": "2026-05-10T14:00:00Z",
      "reminderCount": 2
    }
  ]
}
```

---

## 9. Send Encouragement Message to Child

**Endpoint**: `POST /api/parent/rewards/message/encouragement`

**Request Body**:

```json
{
  "childId": "uuid-child-1",
  "messageText": "Great job on finishing that chapter! Keep it up!"
}
```

**Response** (201 Created):

```json
{
  "messageId": "uuid-msg-3",
  "senderId": "uuid-parent-1",
  "recipientId": "uuid-child-1",
  "messageType": "ENCOURAGEMENT",
  "messageText": "Great job on finishing that chapter! Keep it up!",
  "isRead": false,
  "createdAt": "2026-05-11T12:00:00Z"
}
```

**Effect**:
- Message stored in database
- Child receives in-app notification
- No email sent to child (per FR-013)

---

## 10. Get Messages Sent to Child

**Endpoint**: `GET /api/parent/rewards/message/sent/{childId}`

**Path Parameters**:
- `childId` (UUID): The child to view sent messages for

**Query Parameters**:
- `limit` (optional, default 50): Pagination limit
- `offset` (optional, default 0): Pagination offset

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "messages": [
    {
      "messageId": "uuid-msg-3",
      "messageType": "ENCOURAGEMENT",
      "messageText": "Great job on finishing that chapter!",
      "isRead": true,
      "createdAt": "2026-05-11T12:00:00Z",
      "readAt": "2026-05-11T12:05:00Z"
    }
  ]
}
```

---

## Error Codes

| Code | Meaning |
|------|---------|
| 400 | Invalid request (validation failed) |
| 403 | Not authorized (not parent, child not in family) |
| 404 | Resource not found (reward template, child, etc.) |
| 409 | Conflict (e.g., attempting to use deleted reward) |
| 500 | Server error |

---

## Rate Limiting & Pagination

- **List endpoints**: Paginate with `limit` (max 100) and `offset`
- **Message endpoints**: Rate limit 100 messages per hour per parent
- **Payout endpoints**: Rate limit 10 payouts per hour per parent

---

## Security Notes

- All endpoints require Bearer token authentication
- Parent can only access their own data and their children's data
- Child data access validated server-side by checking `User.parentId`
