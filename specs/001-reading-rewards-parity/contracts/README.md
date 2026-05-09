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