# Feature Specification: Render Deployment with GitHub Actions CI/CD

**Feature Branch**: `002-deployment-cicd`  
**Created**: 2026-05-10  
**Prerequisites**: Requires spec 001-reading-rewards-parity implemented and deployed first  
**Status**: Ready for Development  
**Input**: Set up continuous deployment and CI/CD for reading-rewards-spec to Render using GitHub Actions with professional portfolio-quality implementation

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer opens pull request with code changes (Priority: P1)

A developer completes work on a feature, commits changes, and pushes to a feature branch to create a pull request. GitHub Actions automatically runs tests and builds to validate the changes without requiring manual steps.

**Why this priority**: Pull request checks are the first gate for code quality, preventing broken code from reaching main. This is foundational for a robust CI/CD pipeline and enables safe collaboration.

**Independent Test**: Can be fully tested by creating a feature branch, making code changes, opening a PR, and verifying that GitHub Actions workflows trigger automatically to run tests and provide clear pass/fail feedback to the developer.

**Acceptance Scenarios**:

1. **Given** a developer pushes changes to a feature branch, **When** a pull request is opened against main, **Then** GitHub Actions workflows trigger automatically and run backend and/or frontend tests based on what changed
2. **Given** tests are running on a PR, **When** backend tests pass, **Then** the backend Docker image builds successfully and is ready to push
3. **Given** tests are running on a PR, **When** frontend tests pass, **Then** the frontend Docker image builds successfully and is ready to push
4. **Given** tests fail on a PR, **When** developers view the PR status checks, **Then** GitHub displays which tests failed with clear error messages and log links
5. **Given** a PR has failing checks, **When** a developer clicks the "Details" link on a check, **Then** they can see test output and error details in GitHub Actions
6. **Given** all PR checks pass, **When** a maintainer reviews and approves the PR, **Then** the merge button is enabled and PR is ready to merge
7. **Given** all PR checks pass, **When** a developer attempts to merge before fixes are made, **Then** GitHub prevents the merge if checks are not all passing

---

### User Story 2 - Developer merges pull request to main branch (Priority: P1)

After code review and approval, a developer merges a feature branch into main. This triggers automated build, push, and deployment workflows that deploy the application to Render without manual intervention.

**Why this priority**: Main branch merges are the trigger for production deployments. Automated deployment without manual steps ensures consistency, reduces error, and accelerates time-to-user-value.

**Independent Test**: Can be fully tested by merging a PR to main, verifying GitHub Actions workflows complete successfully, and confirming all three services (backend, frontend, database) are deployed and healthy on Render within 10 minutes.

**Acceptance Scenarios**:

1. **Given** a PR is merged to main branch, **When** the merge is complete, **Then** GitHub Actions deploy workflow(s) trigger automatically
2. **Given** the deploy workflow starts, **When** backend changes are detected, **Then** backend image is built, tagged with git commit SHA, and pushed to container registry
3. **Given** the deploy workflow starts, **When** frontend changes are detected, **Then** frontend image is built, tagged with git commit SHA, and pushed to container registry
4. **Given** images are pushed to the registry, **When** Render deployment process begins, **Then** Render pulls the new images and starts the services
5. **Given** backend service is deploying, **When** the service reaches running state, **Then** Render runs health check by calling GET /actuator/health
6. **Given** health check is called, **When** the endpoint responds with status 200 and healthy status, **Then** the deployment is marked as successful
7. **Given** backend and frontend are both deployed, **When** all services report healthy, **Then** developers can access the application at its Render URL
8. **Given** deployment completes successfully, **When** the deployment is finished, **Then** frontend is able to reach backend using configured VITE_API_URL and API calls work end-to-end

---

### User Story 3 - Separate workflows trigger for backend-only or frontend-only changes (Priority: P2)

When a developer commits changes only to frontend code, only the frontend workflow triggers and deploys. Similarly, backend-only changes trigger only backend workflow. This optimizes CI/CD efficiency and reduces unnecessary rebuilds.

**Why this priority**: Path-triggered workflows improve deployment speed and developer experience, but depend on core P1 workflows being in place. Allows targeted deployments when only one service changes.

**Independent Test**: Can be fully tested by committing frontend-only changes to main, verifying that frontend workflow triggers but backend workflow does not, and vice versa for backend changes.

**Acceptance Scenarios**:

1. **Given** changes are made only to frontend files (frontend/src/*, frontend/package.json, frontend/Dockerfile), **When** commit is pushed to main, **Then** frontend GitHub Actions workflow triggers but backend workflow does not start
2. **Given** changes are made only to backend files (backend/src/*, backend/pom.xml, backend/Dockerfile), **When** commit is pushed to main, **Then** backend GitHub Actions workflow triggers but frontend workflow does not start
3. **Given** changes are made to both backend and frontend files, **When** commit is pushed to main, **Then** both workflows trigger and run in parallel
4. **Given** changes are made to configuration or root files affecting both services, **When** commit is pushed to main, **Then** both workflows trigger to rebuild both services

---

### User Story 4 - Deployment fails and system reports clear error messages (Priority: P2)

When a deployment fails (e.g., service won't start, health check fails, database migration fails), the system detects the failure, stops the deployment, and provides detailed error messages so developers can diagnose and fix the issue.

**Why this priority**: Clear error reporting is essential for operational reliability. Depends on core P1 deployment working, but critical for supporting developers in troubleshooting production issues.

**Independent Test**: Can be fully tested by intentionally triggering a deployment failure (e.g., misconfigured environment variable), verifying the deployment stops, and checking that error logs clearly indicate the root cause in GitHub Actions and Render dashboards.

**Acceptance Scenarios**:

1. **Given** a deployment is in progress and backend service fails to start, **When** health checks fail multiple times in succession, **Then** Render stops the deployment and marks it as failed
2. **Given** a deployment failure occurs, **When** developers check the Render deployment logs, **Then** error messages clearly indicate why the service failed (e.g., "Failed to connect to database", "Missing environment variable VITE_API_URL")
3. **Given** a deployment failure occurs, **When** developers check GitHub Actions workflow logs, **Then** error details and build output help identify if the problem is in the build, push, or Render deploy step
4. **Given** health checks fail during deployment, **When** the deployment fails, **Then** the previously healthy version continues serving traffic (implicit rollback by Render)

---

### Edge Cases

- What happens when a Docker image fails to build during the PR check phase? (Build should fail fast with clear error in GitHub Actions checks)
- What if PostgreSQL connection fails after deployment? (Health check should fail and prevent marking deployment as successful)
- What if VITE_API_URL is misconfigured and frontend cannot reach backend? (Developers can detect this via error logs or manual testing immediately after deployment)
- What if only one service (e.g., backend) is healthy but the other (e.g., frontend) fails to start? (Deployment partially succeeds; developers must investigate and fix frontend issue before next merge)
- What happens if GitHub Actions workflow times out during testing? (Workflow should fail and prevent merge; developers must fix or retry)
- How are sensitive values (database passwords, API keys) protected in CI/CD? (Should be stored as GitHub repository secrets, not in code; accessed as environment variables at runtime)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: GitHub Actions MUST have a workflow that runs on pull requests against main branch and executes backend tests (Maven: `mvn test`)
- **FR-002**: GitHub Actions MUST have a workflow that runs on pull requests against main branch and executes frontend tests (Vite: `npm test` or equivalent)
- **FR-003**: GitHub Actions backend workflow MUST only trigger when files matching backend paths (backend/**, .github/workflows/backend*.yml) are changed OR on all PRs to main
- **FR-004**: GitHub Actions frontend workflow MUST only trigger when files matching frontend paths (frontend/**, .github/workflows/frontend*.yml) are changed OR on all PRs to main
- **FR-005**: GitHub Actions MUST build Docker images for backend and frontend services after tests pass on a commit to main branch
- **FR-006**: GitHub Actions MUST push built Docker images to a container registry (Docker Hub or GitHub Container Registry) with tags including git commit SHA and latest tag
- **FR-007**: GitHub Actions MUST configure container registry credentials securely using GitHub Secrets (not hardcoded)
- **FR-008**: GitHub Actions deploy workflow MUST only run when code is pushed to main branch (not on PRs)
- **FR-009**: Render deployment MUST pull Docker images from the registry and start backend service and frontend service; the PostgreSQL database is hosted on Neon (external managed service, not Render Postgres)
- **FR-010**: Render deployment MUST configure backend service with environment variables: SPRING_PROFILES_ACTIVE=prod, DATABASE_URL=[neon-connection-string], DATABASE_USER, DATABASE_PASSWORD, FRONTEND_URL
- **FR-011**: Render deployment MUST configure frontend service with environment variable: VITE_API_URL=[backend-url] pointing to the deployed backend service
- **FR-012**: Backend service MUST run health checks by exposing GET /actuator/health endpoint, returning HTTP 200 with healthy status
- **FR-013**: Render MUST run automated health checks after deploying backend service and require healthy response before marking deployment successful
- **FR-014**: Render MUST run automated health checks after deploying frontend service (GET / returning HTTP 200) and require healthy response before marking deployment successful
- **FR-015**: GitHub Actions MUST prevent merging to main branch if any required checks (tests, builds) fail
- **FR-016**: PostgreSQL database MUST be hosted on Neon free tier (not Render Postgres, which expires after 30 days); backend connects via Neon connection string supplied as DATABASE_URL environment variable
- **FR-016b**: Neon database MUST have the application schema applied via Flyway migrations on backend startup; schema is compatible with the existing reading-rewards schema
- **FR-017**: Frontend MUST be able to make API calls to backend using the configured VITE_API_URL without CORS or network issues
- **FR-018**: Docker build process for both services MUST complete successfully with no build errors or warnings related to missing dependencies
- **FR-019**: GitHub Actions workflows MUST provide clear error messages and logs when tests fail, builds fail, or deployments fail

### Key Entities

- **GitHub Actions Workflow**: YAML configuration file in `.github/workflows/` that defines automated steps triggered by repository events (PR, push to main)
- **Docker Image**: Containerized backend or frontend service, built from Dockerfile and tagged with git commit SHA and version information
- **Container Registry**: Storage repository (Docker Hub, GitHub Container Registry) where Docker images are stored and retrieved for deployment
- **GitHub Secrets**: Securely stored credential values (registry passwords, API tokens) accessed by GitHub Actions workflows without exposing in logs
- **Render Service**: Deployed application instance on Render platform with resource allocation, environment configuration, and health check endpoints
- **Health Check Endpoint**: HTTP endpoint that Render calls to verify a service is running and responsive (e.g., /actuator/health, GET /)
- **Neon Database**: Serverless PostgreSQL hosting platform with a permanent free tier; connection is made via a standard PostgreSQL JDBC URL supplied as DATABASE_URL environment variable to the backend
- **Database Migration**: Process that applies Flyway migration scripts to the Neon database on backend startup to establish or update the schema
- **Git Path Filter**: GitHub Actions configuration that determines whether a workflow triggers based on which files changed in a commit

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: GitHub Actions workflows automatically run all tests on every PR and prevent merge if any tests fail
- **SC-002**: GitHub Actions workflows run PR checks within 10 minutes of PR creation, providing immediate feedback to developers
- **SC-003**: GitHub Actions workflows automatically build and push Docker images on every main branch commit, completing within 15 minutes
- **SC-004**: Backend health check endpoint returns HTTP 200 with healthy status within 30 seconds of service start
- **SC-005**: Frontend service loads and serves initial page load within 3 seconds from Render URL after successful deployment
- **SC-006**: Main branch deployment completes end-to-end (code merge → test → build → push → deploy → health checks pass) within 15 minutes
- **SC-007**: Frontend can successfully make API calls to backend and receive valid responses within 2 seconds latency
- **SC-008**: Neon PostgreSQL database is accessible from the Render backend service; schema migrations run successfully on startup and the database is queryable
- **SC-009**: Path-triggered workflows correctly identify backend vs. frontend changes and trigger appropriate workflows only (backend changes don't trigger frontend workflow and vice versa)
- **SC-010**: Zero manual deployment steps required after GitHub Actions and Render are initially configured
- **SC-011**: All failed deployments clearly display error messages and logs within GitHub Actions dashboard and Render dashboard for developer diagnosis
- **SC-012**: Sensitive credentials (registry passwords, database credentials) are protected via GitHub Secrets and never logged or exposed in workflow output

## Assumptions

- **GitHub Actions Environment**: GitHub repository has GitHub Actions enabled and can execute workflows without additional cost (public repo or sufficient private minutes)
- **Container Registry Access**: A container registry (Docker Hub, GitHub Container Registry) is configured and accessible from GitHub Actions and Render
- **GitHub Secrets Configuration**: GitHub repository secrets are configured for container registry credentials and Render deployment tokens (not committed to repo)
- **Render Account**: A Render account exists with sufficient free tier resources to deploy two services (backend, frontend); PostgreSQL is NOT hosted on Render (Render Postgres free tier expires after 30 days)
- **Neon Account**: A Neon account exists (or is created) with a free-tier PostgreSQL database provisioned; Neon free tier does not expire
- **Dockerfile Validity**: Backend Dockerfile and frontend Dockerfile are present and valid; projects can be containerized successfully
- **Test Framework Setup**: Backend tests are executable via Maven (mvn test) and frontend tests are executable via configured test runner (Playwright, Jest, or similar)
- **Spring Boot Actuator**: Backend application has Spring Boot Actuator dependency configured with /actuator/health endpoint enabled and secured appropriately
- **Environment Variable Access**: Services can access environment variables set in Render at runtime without additional configuration
- **Database Migration Scripts**: Flyway or similar database migration tool is configured in backend; migration scripts exist and can run at startup
- **Neon Connection String**: Neon provides a standard PostgreSQL connection string (postgresql://user:password@host/dbname); this is stored as a GitHub Secret and set as DATABASE_URL on the Render backend service
- **Render Service Naming**: Backend and frontend services are deployed as separate Render services (not in one composite service) to enable independent scaling and deployment
- **Git History Accessibility**: GitHub Actions has access to git history to determine which files changed for path-based filtering
- **API Communication**: Frontend can reach backend via internal Render network or public URL depending on Render's networking model
- **Free Tier Compatibility**: Both Render services (backend, frontend) fit within Render's free tier resource limits; Neon free tier supports up to 0.5 GB storage and is sufficient for a personal project
- **Existing render.yaml**: The reading-rewards/render.yaml provides a valid starting point; project can adapt it for reading-rewards-spec or create new Render configuration
- **Docker Hub (or Registry) Account**: If using Docker Hub, a free account exists or is created; registry is publicly accessible or Render has pull credentials
- **Domain/URL Stability**: Render services receive stable URLs that can be configured in frontend as VITE_API_URL without frequent changes during iteration

