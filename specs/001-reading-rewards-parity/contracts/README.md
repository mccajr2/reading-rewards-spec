# Contracts

This directory holds the current contract summary for the parity rebuild.

Implemented endpoints summary:

- Authentication
	- `POST /api/auth/signup`
	- `GET /api/auth/verify-email?token=...`
	- `POST /api/auth/login`
	- `POST /api/auth/logout`
- Parent management
	- `GET /api/parent/kids`
	- `POST /api/parent/kids`
	- `POST /api/parent/reset-child-password`
	- `GET /api/parent/kids/summary`
	- `GET /api/parent/{childId}/child-detail`
	- `POST /api/parent/{childId}/chapter-reads/{chapterReadId}/reverse`
- Books and reading progress
	- `GET /api/search`
	- `GET /api/books`
	- `POST /api/books`
	- `POST /api/books/{googleBookId}/finish`
	- `POST /api/books/{googleBookId}/reread`
	- `DELETE /api/bookreads/{bookReadId}`
	- `GET /api/books/{googleBookId}/chapters`
	- `POST /api/books/{googleBookId}/chapters`
	- `PUT /api/chapters/{id}`
	- `POST /api/bookreads/{bookReadId}/chapters/{chapterId}/read`
	- `DELETE /api/books/{googleBookId}/chapters/{chapterId}/read`
	- `GET /api/bookreads/{bookReadId}/chapterreads`
	- `GET /api/bookreads/in-progress`
	- `GET /api/history`
- Rewards and credits
	- `GET /api/credits`
	- `GET /api/rewards/summary`
	- `GET /api/rewards`
	- `POST /api/rewards/spend`
	- `POST /api/rewards/payout`

Notes:

- Protected endpoints require Bearer JWT except configured public auth routes.
- Parent login is blocked when account status is `UNVERIFIED`.
- `GET /api/credits` is a legacy-parity alias that returns `{ cents, dollars }` derived from the same reward balance as `GET /api/rewards/summary`. It was retained to match the legacy app's route surface and is the primary balance indicator used by the child UI.

US1 contract details (parent lifecycle + dashboard):

- `POST /api/auth/signup`
	- Request JSON example:
		- `{ "email": "parent@example.com", "password": "Password1!", "firstName": "Pat", "lastName": "Parent" }`
	- `200 OK`: verification email triggered and message returned.
	- `400 Bad Request`: missing required fields or duplicate email.

- `GET /api/auth/verify-email?token=...`
	- `200 OK`: token consumed and parent status promoted to `VERIFIED`.
	- `400 Bad Request`: invalid/expired token.

- `POST /api/auth/login`
	- Request JSON example:
		- `{ "username": "parent@example.com", "password": "Password1!" }`
		- child login uses kid username in the same `username` field.
	- `200 OK`: `{ token, user }` where `user.role` is `PARENT` or `CHILD`.
	- `403 Forbidden`: unverified parent login attempt.
	- `401 Unauthorized`: invalid credentials.

- `POST /api/parent/kids`
	- Auth: parent JWT required.
	- Request JSON example: `{ "username": "kid1", "firstName": "Jamie", "password": "KidPass1!" }`
	- `200 OK`: child account created.
	- `400 Bad Request`: duplicate username or missing required fields.

- `GET /api/parent/kids`
	- Auth: parent JWT required.
	- Response example:
		- `[ { "id": "...", "firstName": "Jamie", "username": "kid1" } ]`

- `GET /api/parent/kids/summary`
	- Auth: parent JWT required.
	- Response example:
		- `{ "kids": [ { "id": "...", "firstName": "Jamie", "username": "kid1", "booksRead": 2, "chaptersRead": 9, "totalEarned": 9.0, "currentBalance": 7.5 } ] }`

- `POST /api/parent/reset-child-password`
	- Auth: parent JWT required.
	- Request JSON example: `{ "childUsername": "kid1", "newPassword": "NewKidPass1!" }`
	- `200 OK`: password reset complete.
	- `404 Not Found`: child not owned by parent.

US2 contract details (reading + rewards):

- `GET /api/search`
	- Query params: any of `title`, `author`, `isbn`.
	- Uses Open Library search API mapping (`key` -> `googleBookId` compatibility field), normalized to slash-free IDs (example: `/works/OL82563W` becomes `OL82563W`).
	- Returns an empty list when Open Library is unavailable.

- `POST /api/books`
	- Auth: child or parent JWT.
	- Request JSON example:
		- `{ "googleBookId": "OL82563W", "title": "Charlotte's Web", "authors": ["E.B. White"], "description": "...", "thumbnailUrl": "..." }`
	- Creates or reuses the shared `book` record and creates a user-scoped `book_read` row.

- `POST /api/books/{googleBookId}/chapters`
	- Seeds shared chapters on first add for that book.
	- Reuses existing seeded chapters on subsequent adds (no overwrite).

- `POST /api/bookreads/{bookReadId}/chapters`
	- Same seed/reuse rule as above, but addressed by `bookReadId`.
	- Intended UI behavior: the app opens an in-app chapter-count dialog when a newly added book has no shared chapters yet.

- `PUT /api/chapters/{id}`
	- Request JSON example: `{ "name": "Renamed Chapter" }`
	- Applies explicit rename semantics for existing chapter records.

- `POST /api/bookreads/{bookReadId}/chapters/{chapterId}/read`
	- Idempotent chapter read marker.
	- Side effect: creates one `EARN` reward entry for first completion only.

- `DELETE /api/books/{googleBookId}/chapters/{chapterId}/read`
	- Reverses a chapter read and deletes reward entries linked to the deleted chapter-read row.

- `POST /api/books/{googleBookId}/finish` and `POST /api/books/{googleBookId}/reread`
	- `finish`: closes the in-progress row (`endDate` set).
	- `reread`: creates a new in-progress row while preserving shared chapter definitions.

- `GET /api/rewards`
	- Query params: `page`, `pageSize`.
	- Response shape: `{ rewards: [...], totalCount: number }`.

- `POST /api/rewards/spend?amount=...&note=...` and `POST /api/rewards/payout?amount=...`
	- Record spend/payout debits; summary balance is `earned - paidOut - spent`.

Open Library notes:

- Provider endpoint: `https://openlibrary.org/search.json`.
- Cover thumbnails are derived from `cover_i` via `https://covers.openlibrary.org/b/id/{cover_i}-M.jpg`.
- Compatibility field name remains `googleBookId` in DTOs/contracts to preserve legacy UI and API expectations.

US3 contract details (child detail + reversal):

- `GET /api/parent/{childId}/child-detail`
	- Auth: parent JWT required.
	- `200 OK` when `{childId}` belongs to authenticated parent.
	- `403 Forbidden` when caller is not a parent.
	- `404 Not Found` when child does not belong to authenticated parent.
	- Response body fields:
		- `child`: `{ id, firstName, username }`
		- `books[]`: each includes `{ bookReadId, googleBookId, title, authors, thumbnailUrl, startDate, endDate, inProgress, chapters[] }`
		- `chapters[]`: each includes `{ id, index, name, isRead, chapterReadId?, earnedReward? }`
		- `rewards[]`: each includes `{ id, type, amount, chapterReadId, note, createdAt }`
		- rollup fields: `{ totalEarned, currentBalance }`

- `POST /api/parent/{childId}/chapter-reads/{chapterReadId}/reverse`
	- Auth: parent JWT required.
	- `200 OK` when parent owns child and chapter-read belongs to that child.
	- `403 Forbidden` when caller is not a parent.
	- `404 Not Found` when child not owned by parent, or chapter-read is not the child’s.
	- Side effects:
		- deletes the targeted `chapter_reads` row.
		- deletes reward rows linked by `reward.chapterReadId == {chapterReadId}` for the same child.