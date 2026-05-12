# Reward Selection & Balance API Contract (Child)

**Endpoint Base**: `/api/child/rewards`  
**Authentication**: Bearer token (child user must have role == CHILD)  
**Content-Type**: application/json

---

## 1. Get Available Rewards for Selection

**Endpoint**: `GET /api/child/rewards/available`

**Query Parameters**: (none)

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "childName": "Alice",
  "availableRewards": [
    {
      "rewardTemplateId": "uuid-family-1",
      "rewardType": "MONEY",
      "amount": 0.50,
      "unit": "PER_CHAPTER",
      "description": "$0.50 per chapter",
      "cannedTemplateId": "MONEY_050_PER_CHAPTER",
      "scope": "FAMILY"
    },
    {
      "rewardTemplateId": "uuid-family-2",
      "rewardType": "TIME",
      "amount": 5,
      "unit": "PER_CHAPTER",
      "description": "5 min screentime per chapter",
      "cannedTemplateId": "TIME_5MIN_PER_CHAPTER",
      "scope": "FAMILY"
    },
    {
      "rewardTemplateId": "uuid-child-override-1",
      "rewardType": "MONEY",
      "amount": 1.00,
      "unit": "PER_CHAPTER",
      "description": "$1.00 per chapter (just for you)",
      "cannedTemplateId": null,
      "scope": "PER_CHILD"
    }
  ],
  "defaultRewardId": "uuid-family-1"
}
```

**Notes**:
- Returns both family-level rewards and per-child overrides
- `defaultRewardId` is set to system default ($1/chapter) if no explicit default configured
- Excludes deleted rewards

---

## 2. Select Reward for Book

**Endpoint**: `POST /api/child/rewards/select/{bookReadId}`

**Path Parameters**:
- `bookReadId` (UUID): The book being added/started

**Request Body**:

```json
{
  "rewardTemplateId": "uuid-family-1"
}
```

**Response** (201 Created):

```json
{
  "selectionId": "uuid-sel-1",
  "bookReadId": "uuid-book-1",
  "rewardTemplateId": "uuid-family-1",
  "rewardType": "MONEY",
  "description": "$0.50 per chapter",
  "lockedAmount": 0.50,
  "lockedUnit": "PER_CHAPTER",
  "selectedAt": "2026-05-11T13:00:00Z"
}
```

**Effect**:
- Creates RewardSelection with locked-in amount/unit
- Creates ProgressTracking if needed (based on reward unit)
- Child can now track progress and earn this reward

**Error** (404 Not Found):
```json
{
  "error": "Reward template not found or not available for this child"
}
```

**Error** (409 Conflict - reward already selected for this book):
```json
{
  "error": "Reward already selected for this book"
}
```

---

## 3. Change Reward Selection (Mid-Book)

**Endpoint**: `PUT /api/child/rewards/select/{bookReadId}`

**Path Parameters**:
- `bookReadId` (UUID): The book to change reward for

**Request Body**:

```json
{
  "rewardTemplateId": "uuid-child-override-1"
}
```

**Response** (200 OK):

```json
{
  "selectionId": "uuid-sel-1",
  "bookReadId": "uuid-book-1",
  "rewardTemplateId": "uuid-child-override-1",
  "rewardType": "MONEY",
  "description": "$1.00 per chapter (just for you)",
  "lockedAmount": 1.00,
  "lockedUnit": "PER_CHAPTER",
  "selectedAt": "2026-05-11T13:00:00Z",
  "changedAt": "2026-05-11T14:00:00Z",
  "note": "New reward applies to chapters starting from Chapter 5"
}
```

**Effect**:
- Updates RewardSelection with new locked-in amount
- Existing chapters (1-4) tracked with old rate ($0.50 × 4 = $2.00)
- Future chapters (5+) tracked with new rate ($1.00 × chapters)
- Child sees clear messaging about what changed

---

## 4. Get Current Reward for Book

**Endpoint**: `GET /api/child/rewards/select/{bookReadId}`

**Path Parameters**:
- `bookReadId` (UUID): The book

**Response** (200 OK):

```json
{
  "selectionId": "uuid-sel-1",
  "bookReadId": "uuid-book-1",
  "rewardTemplateId": "uuid-family-1",
  "rewardType": "MONEY",
  "description": "$0.50 per chapter",
  "lockedAmount": 0.50,
  "lockedUnit": "PER_CHAPTER",
  "selectedAt": "2026-05-11T13:00:00Z",
  "isActive": true
}
```

---

## 5. Update Progress Tracking (Chapters)

**Endpoint**: `PUT /api/child/rewards/progress/{bookReadId}/chapters`

**Path Parameters**:
- `bookReadId` (UUID): The book

**Request Body**:

```json
{
  "totalChapters": 15,
  "currentChapter": 8,
  "chaptersRead": [0, 1, 2, 3, 4, 5, 6, 7]
}
```

**Response** (200 OK):

```json
{
  "trackingId": "uuid-track-1",
  "bookReadId": "uuid-book-1",
  "trackingType": "CHAPTERS",
  "totalChapters": 15,
  "currentChapter": 8,
  "chaptersRead": [0, 1, 2, 3, 4, 5, 6, 7],
  "progressPercentage": 53.3,
  "projectedReward": 4.00,
  "updatedAt": "2026-05-11T14:15:00Z"
}
```

**Notes**:
- `chaptersRead` is array of chapter indices read (allows non-linear)
- `projectedReward` = length(chaptersRead) × lockedAmount (for preview)
- Mandatory for PER_CHAPTER rewards

---

## 6. Update Progress Tracking (Pages)

**Endpoint**: `PUT /api/child/rewards/progress/{bookReadId}/pages`

**Path Parameters**:
- `bookReadId` (UUID): The book

**Request Body**:

```json
{
  "totalPages": 320,
  "currentPage": 150
}
```

**Response** (200 OK):

```json
{
  "trackingId": "uuid-track-1",
  "bookReadId": "uuid-book-1",
  "trackingType": "PAGES",
  "totalPages": 320,
  "currentPage": 150,
  "progressPercentage": 46.9,
  "equivalentChapters": 7.5,
  "updatedAt": "2026-05-11T14:20:00Z"
}
```

**Notes**:
- Optional for all rewards; primary tracking option for PER_BOOK rewards
- `equivalentChapters` is calculated as currentPage / (totalPages / estimatedChapters)
- OpenLibrary API may have provided totalPages; user can override

---

## 7. Get Current Rewards Balance

**Endpoint**: `GET /api/child/rewards/balance`

**Query Parameters**: (none)

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "childName": "Alice",
  "balance": {
    "totalEarned": 45.50,
    "totalPaid": 20.00,
    "availableBalance": 25.50,
    "pendingPayout": {
      "amount": 5.50,
      "reminderSentAt": "2026-05-11T09:00:00Z"
    }
  },
  "byRewardType": [
    {
      "rewardType": "MONEY",
      "description": "Cash Rewards",
      "totalEarned": 45.50,
      "totalPaid": 20.00,
      "availableBalance": 25.50,
      "currency": "USD"
    }
  ]
}
```

**Notes**:
- "Total Earned" = sum of all EARNED + PAID accumulations
- "Total Paid" = sum of PAID accumulations
- "Available Balance" = Total Earned - Total Paid
- "Pending Payout" = child has sent reminder and parent hasn't confirmed yet

---

## 8. Get Reward History / Accumulation Log

**Endpoint**: `GET /api/child/rewards/history`

**Query Parameters**:
- `status` (optional): Filter by EARNED, PENDING_PAYOUT, PAID (default: all)
- `limit` (optional, default 50): Pagination limit
- `offset` (optional, default 0): Pagination offset

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "accumulations": [
    {
      "accumulationId": "uuid-acc-1",
      "bookTitle": "The Hobbit",
      "bookReadId": "uuid-book-1",
      "rewardType": "MONEY",
      "description": "$0.50 per chapter",
      "amountEarned": 5.00,
      "unitCount": 10,
      "calculationNote": "10 chapters completed",
      "status": "PAID",
      "payoutDate": "2026-05-10T18:00:00Z",
      "createdAt": "2026-05-09T15:30:00Z"
    },
    {
      "accumulationId": "uuid-acc-2",
      "bookTitle": "Harry Potter",
      "bookReadId": "uuid-book-2",
      "rewardType": "MONEY",
      "description": "$0.50 per chapter",
      "amountEarned": 10.50,
      "unitCount": 21,
      "calculationNote": "21 chapters completed",
      "status": "PENDING_PAYOUT",
      "payoutDate": null,
      "createdAt": "2026-05-11T10:00:00Z"
    }
  ]
}
```

---

## 9. Send Payout Reminder to Parent

**Endpoint**: `POST /api/child/rewards/payout-reminder`

**Request Body**:

```json
{
  "message": "I've finished more chapters and would like my payout!"
}
```

**Response** (201 Created):

```json
{
  "messageId": "uuid-msg-4",
  "senderId": "uuid-child-1",
  "recipientId": "uuid-parent-1",
  "messageType": "PAYOUT_REMINDER",
  "messageText": "I've finished more chapters and would like my payout!",
  "relatedAmount": 10.50,
  "createdAt": "2026-05-11T15:00:00Z"
}
```

**Effect**:
- Creates Message record with type PAYOUT_REMINDER
- Parent receives in-app notification
- Parent may receive email if they have `notify_email_payouts=true` (rate limited: max 1/day per child)
- Related accumulations linked to this message

**Error** (429 Too Many Requests):
```json
{
  "error": "Too many reminders. Please wait before sending another."
}
```

---

## 10. Get Messages from Parent

**Endpoint**: `GET /api/child/rewards/messages`

**Query Parameters**:
- `type` (optional): Filter by ENCOURAGEMENT, PAYOUT_CONFIRMATION (default: all)
- `limit` (optional, default 50): Pagination limit
- `offset` (optional, default 0): Pagination offset

**Response** (200 OK):

```json
{
  "childId": "uuid-child-1",
  "messages": [
    {
      "messageId": "uuid-msg-3",
      "senderId": "uuid-parent-1",
      "senderName": "Mom",
      "messageType": "ENCOURAGEMENT",
      "messageText": "Great job on finishing that chapter! Keep it up!",
      "isRead": true,
      "createdAt": "2026-05-11T12:00:00Z",
      "readAt": "2026-05-11T12:05:00Z"
    },
    {
      "messageId": "uuid-msg-2",
      "senderId": "uuid-parent-1",
      "senderName": "Mom",
      "messageType": "PAYOUT_CONFIRMATION",
      "messageText": "Your payout of $10.50 has been confirmed!",
      "relatedAmount": 10.50,
      "isRead": true,
      "createdAt": "2026-05-11T11:30:00Z",
      "readAt": "2026-05-11T11:35:00Z"
    }
  ]
}
```

---

## 11. Mark Message as Read

**Endpoint**: `PUT /api/child/rewards/messages/{messageId}/read`

**Path Parameters**:
- `messageId` (UUID): The message to mark read

**Request Body**: (empty)

**Response** (200 OK):

```json
{
  "messageId": "uuid-msg-3",
  "isRead": true,
  "readAt": "2026-05-11T16:00:00Z"
}
```

---

## 12. Complete Book & Trigger Reward Calculation

**Endpoint**: `POST /api/child/rewards/complete/{bookReadId}`

**Path Parameters**:
- `bookReadId` (UUID): The book to mark complete

**Request Body**: (empty)

**Response** (200 OK):

```json
{
  "bookReadId": "uuid-book-1",
  "rewardEarned": {
    "accumulationId": "uuid-acc-1",
    "rewardType": "MONEY",
    "amountEarned": 5.00,
    "unitCount": 10,
    "calculationNote": "10 chapters @ $0.50",
    "status": "EARNED"
  },
  "newBalance": {
    "totalEarned": 50.50,
    "totalPaid": 20.00,
    "availableBalance": 30.50
  }
}
```

**Effect**:
- Validates reward selection and progress tracking
- If PER_CHAPTER reward: verifies chapter count entered; calculates amountEarned
- If PER_BOOK reward with optional tracking: accepts completion with or without tracking
- Creates RewardAccumulation with status EARNED
- Updates BookRead.endDate
- No email sent; child sees in-app notification of new earning

**Error** (400 Bad Request - missing required tracking):
```json
{
  "error": "This book has a per-chapter reward. You must enter chapter count before completing."
}
```

---

## Error Codes

| Code | Meaning |
|------|---------|
| 400 | Invalid request (validation failed) |
| 403 | Not authorized (not child, reward not available) |
| 404 | Resource not found (book, reward, etc.) |
| 409 | Conflict (reward already selected, reward template deleted, etc.) |
| 429 | Too many requests (rate limit exceeded) |
| 500 | Server error |

---

## Rate Limiting & Pagination

- **List endpoints**: Paginate with `limit` (max 100) and `offset`
- **Payout reminder**: Max 1 per day per child
- **Message endpoints**: No rate limit on reading; 100 requests per hour per child

---

## Security Notes

- All endpoints require Bearer token authentication
- Child can only access their own data
- Child cannot see sibling's rewards or balances
- Progress and reward selection are scoped to the child's own books
