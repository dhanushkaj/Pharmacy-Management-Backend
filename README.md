# Introduction

Concise, actionable guidance for working in this Pharmacy Management mono-repo (Spring Boot backend + React frontend). Focus on existing patterns—do not introduce new architectural styles without discussion.

## 1. Big Picture
- Two projects: `Phamarcy-Management-Backend` (Spring Boot 3.5.5) and `pharmacy-management` (React). Backend exposes REST under `/api/*`, frontend consumes via `src/utill/api.js` & `auditApi.js`.
- Domain-driven package layout: `model` (entities) → `repository` (Spring Data JPA) → `service` (business logic) → `controller` (REST) → `audit/security/config` (cross-cutting concerns).
- All mutable domain entities extend `BaseAuditableEntity` for JPA auditing (createdAt/updatedAt/user/version). Additional action-level auditing via `AuditAspect` + `AuditService` writing `AuditLog` records.

## 2. Core Architectural Patterns
- Controllers: Thin, delegate to Service, return DTO or entity directly, pagination via `Pageable` method params (e.g. AuditController.getAllAuditLogs()).
- Services: Stateless spring components performing validation & orchestration. Prefer constructor injection (Lombok `@RequiredArgsConstructor`).
- Repositories: Spring Data interfaces (e.g. `ProductRepository`)—use derived query methods; add custom queries only when necessary.
- Security: JWT parsed in `JwtAuthFilter`; roles mapped to `ROLE_*`. Restrict endpoints with `@PreAuthorize` (see `AuditController` admin-only).
- Auditing: AOP intercepts CRUD operations; manual logging via `auditService.logAction(...)` when needed for non-standard operations.

## 3. Audit Trail Specifics
- Endpoints live under `/api/audit` (search, export, summaries). CSV export implemented inline in controller—preserve escaping logic (`escapeField`).
- Date parsing handles flexible formats (YYYY-MM-DD or ISO). If refining, keep backward compatibility & robust null handling.
- Exclude sensitive fields via properties (`audit.exclude-fields`). Respect field length limits when adding new audited attributes.

## 4. Conventions & Naming
- Table names prefixed `rdp_` (e.g. `rdp_products`); entity primary keys use `<name>Id` (e.g. `productId`).
- Unique indexes defined at entity level (see `Product` for `product_code`, `barcode`). Maintain index naming pattern `rdp_idx_<field>`.
- Validation via Jakarta annotations directly on entity (e.g. `@NotBlank`, `@DecimalMin`). Preserve existing constraints when modifying fields.

## 5. Build & Run Workflow (Backend)
- Build: `./gradlew clean build` → jar in `build/libs/`.
- Dev run: `./gradlew bootRun` (requires PostgreSQL and proper `application*.properties`).
- Release tagging: create git tag `vX.Y.Z` then build. Keep snapshot vs release version consistent with `build.gradle`.
- Tests: JUnit 5 + Spring Security Test + Mockito. When adding service tests, cover security context population for auditing where relevant.

## 6. Frontend Integration Touchpoints
- Audit UI templates already provided (`AUDIT_TRAIL_COMPONENT.txt`, examples). Route `/audit-trail` guarded by role (admin/manager). Keep API calls centralized in `auditApi.js`.
- History modals should call `/api/audit/entity/{entityType}/{entityId}`; paginate client-side only after server pagination retrieval.

## 7. Safe Extension Guidelines
- When adding a new entity: extend `BaseAuditableEntity`, create repository + service + controller following existing naming and layering; add indexes if frequent lookup fields.
- For new audit actions: prefer using existing `AuditAction` enum; if adding values, ensure export & summary endpoints still function (update any mapping logic if present).
- Avoid embedding business logic in controllers—keep transformations in service layer.

## 8. Common Pitfalls to Avoid
- Bypassing service layer (breaks audit & validation). Always go: Controller → Service → Repository.
- Returning large unpaged lists; use `Pageable` to maintain consistency & performance.
- Ignoring optimistic locking (`version` field). Handle `ObjectOptimisticLockingFailureException` gracefully in updates.
- Storing sensitive data in audit logs—filter or mask manually if needed before calling `auditService.logAction`.

## 9. Quick Reference Examples
- Entity definition pattern: see `Product` (indexes, relations, validation, auditing inheritance).
- Audit export pattern: AuditController.exportAuditLogs() (collect list, build CSV).
- JWT role mapping: `JwtAuthFilter` (`ROLE_` prefix) — replicate when introducing custom authorities.

## 10. Agent DO / DON’T
- DO: Reuse existing layers, follow naming, keep auditing intact, write small focused service methods, add tests for new business rules.
- DO: Validate inputs with Jakarta annotations & (if complex) service-level checks.
- DON’T: Introduce new frameworks (e.g., mapstruct) without explicit request; rewrite auditing core; expose repositories directly to controllers.
- DON’T: Log sensitive fields (password, token, secret) even manually.

## 11. Fast Search Tips
- Audit classes: `com/rdp/audit/*`
- Security: `com/rdp/security/*`
- Domain models: `com/rdp/model/*`
- Services: `com/rdp/service/*`
- Controllers: `com/rdp/controller/*`

Feedback welcome: Clarify any missing workflow, test strategy, or performance practice you need before deeper changes.



# Pharmacy-Management-Backend

## Release Process

1. Ensure all code changes are committed and pushed to the main branch.
2. Tag the release in git:
	```sh
	git tag vX.Y.Z
	git push origin vX.Y.Z
	```
3. Build the backend JAR:
	```sh
	./gradlew clean build
	```
4. The release artifact will be available in `build/libs/`.

## Running the Backend

1. Make sure PostgreSQL (or your configured DB) is running and accessible.
2. Update `src/main/resources/application.properties` with your DB credentials if needed.
3. Start the backend service:
	```sh
	./gradlew bootRun
	```
	or run the JAR directly:
	```sh
	java -jar build/libs/Phamarcy-Management-Backend-0.0.1-SNAPSHOT.jar
	```
4. The backend will be available at `http://localhost:8080`.

## Branches

- `main`: Production and release branch.
- `dev`: Development branch for new features and bug fixes.
- `feature/*`: Feature-specific branches (e.g., `feature/auth`, `feature/grn`).

## Frontend

See the frontend project in `pharmacy-management/` for ReactJS setup and instructions.




