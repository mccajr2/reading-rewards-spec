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

Notes:

- Protected endpoints require Bearer JWT except configured public auth routes.
- Parent login is blocked when account status is `UNVERIFIED`.