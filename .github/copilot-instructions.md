# AI Coding Agent Instructions

Actionable guidance for the Pharmacy Management System mono-repo (Spring Boot backend + React frontend). Focus on existing patterns—do not introduce new architectural styles without discussion.

---

## 1. Repository Overview

**Two main projects:**
- `Phamarcy-Management-Backend/`: Spring Boot 3.5.5, PostgreSQL, JWT auth, comprehensive audit trail
- `pharmacy-management/`: React 18 + React Router 6, Ant Design components, communicates via REST

**Data flow:** React → `api.js`/`auditApi.js` → Backend `/api/*` → Service → Repository → PostgreSQL (`pharmacy` schema)

**Branch workflow (frontend):**
- Feature branches from `release` → PR to `developer` for review
- After QA testing in `developer` and bug fixes logged in `qa_test/` → PR to `release`
- Backend follows similar pattern

---

## 2. Backend Architecture

### Layer Structure
Domain-driven package layout: `model` → `repository` → `service` → `controller` → `audit/security/config`

**Controllers** (thin REST layer):
- Delegate to services, return entities/DTOs directly
- Pagination via `Pageable` method params (e.g., `AuditController.getAllAuditLogs(Pageable pageable)`)
- Example: `com/rdp/controller/AuditController.java`

**Services** (business logic):
- Stateless Spring `@Service` components
- Constructor injection via Lombok `@RequiredArgsConstructor`
- Validation & orchestration—no direct repository exposure to controllers
- Example: `com/rdp/service/CategoryService.java`

**Repositories** (data access):
- Spring Data JPA interfaces extending `JpaRepository`
- Use derived query methods (e.g., `findByProductCode`) over custom `@Query` when possible
- Example: `com/rdp/repository/ProductRepository.java`

**Entities** (domain models):
- All mutable entities extend `BaseAuditableEntity` (auto-populates `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`)
- Table names prefixed `rdp_` (e.g., `rdp_products`)
- Primary keys: `<entity>Id` (e.g., `productId`)
- Define indexes at entity level: `@Index(name = "rdp_idx_product_code", columnList = "product_code")`
- Jakarta validation annotations on fields (e.g., `@NotBlank`, `@DecimalMin`, `@DecimalMax`)
- Example: `src/main/java/com/rdp/model/Product.java`

### Security
- **JWT filter:** `JwtAuthFilter` parses `Authorization: Bearer <token>`, extracts username + roles
- **Role mapping:** Roles from token get `ROLE_` prefix (e.g., `ADMIN` → `ROLE_ADMIN`)
- **Authorization:** `@PreAuthorize("hasRole('ADMIN')")` on controllers/methods
- **Security context:** Auditing relies on `SecurityContextHolder` for `performedBy` field

### Auditing (3 layers)
1. **JPA entity auditing:** `BaseAuditableEntity` + `@EntityListeners(AuditingEntityListener.class)` (automatic timestamps/user)
2. **Action-level auditing:** `AuditAspect` intercepts controller methods, logs to `AuditLog` via `AuditService`
3. **Database triggers:** Failsafe audit columns at DB level (see `audit-schema.sql`)

**Audit endpoints:** `/api/audit` (search, export, entity history)
- CSV export uses inline `escapeField()` logic—preserve when modifying
- Date parsing supports `YYYY-MM-DD` or ISO formats—maintain backward compatibility
- Sensitive fields excluded via `audit.exclude-fields` property

---

## 3. Frontend Architecture (React)

### Routing & Layout
- `App.js`: Single `<AuthProvider>` wrapping all routes, conditionally renders `Sidebar`/`Header`/`Footer`
- Routes defined in `<Routes>`, protected via `<PrivateRoute>` wrapper (checks `token` in `AuthContext`)
- Example routes: `/products`, `/billing`, `/audit-trail`, `/reports/sales`

### State Management
- **AuthContext** (`components/AuthContext.js`): Global state for `token`, `roles`, `username`
  - `login(token, rolesArr, user)`: Sets state + `localStorage`
  - `logout()`: Clears state + `localStorage`
  - `hasRole(role)`: Case-insensitive role check
- **Local state:** Pages use `useState` for forms, tables, modals; `useEffect` for data fetching
- Example: `pages/AuditTrail.js` (filters + pagination state)

### API Communication
- **Base wrapper:** `src/utill/api.js` exports `api(path, { method, body, token, headers })`
  - Automatically attaches JWT header, base URL from `REACT_APP_API_BASE` or proxy
  - Returns parsed JSON or text; throws error with server message if not `res.ok`
- **Audit-specific:** `src/utill/auditApi.js` exports `getAuditLogs(filters, token)`, `exportAuditLogs(filters, token)`
  - Handles date formatting (YYYY-MM-DD → YYYY-MM-DDTHH:MM:SS) for backend compatibility

### Component Patterns
- Functional components with hooks (`useState`, `useEffect`, `useContext`)
- Modal usage: Ant Design `Modal` or custom `components/Modal.js`
- Table rendering: Often client-side filtering/sorting after backend pagination fetch
- Example audit UI: `pages/AuditTrail.js` (search filters → `getAuditLogs` → table display)

---

## 4. Configuration & Environment

### Backend (`application.properties`)
- **Database:** PostgreSQL at `localhost:5432/postgres?currentSchema=pharmacy`
- **Hibernate DDL:** `spring.jpa.hibernate.ddl-auto=update` (auto-schema evolution in dev/qa)
- **Logging:** Log4j2 (excludes default logback), audit logging at `DEBUG` level
- **Profiles:** `application-{qa|stg|prod}.properties` for environment-specific config

### Frontend (`package.json`)
- **Proxy:** `"proxy": "http://localhost:8080"` routes `/api/*` calls to backend in dev
- **Dependencies:** `react-router-dom@6`, `antd@5`, `xlsx` for Excel exports
- **Scripts:** `npm start` (dev server on `:3000`), `npm run build` (production build)

### Build Commands
- **Backend:** `./gradlew clean build` → jar in `build/libs/`, `./gradlew bootRun` for dev
- **Frontend:** `npm install`, `npm start` (dev), `npm run build` (prod → `build/` folder)

---

## 5. Naming & Code Conventions

### Backend
- **Tables:** `rdp_<plural>` (e.g., `rdp_products`, `rdp_audit_logs`)
- **Indexes:** `rdp_idx_<field>` (e.g., `rdp_idx_product_code`)
- **Primary keys:** `<entity>Id` (e.g., `productId`, `customerId`)
- **Unique constraints:** Defined at entity level with `unique = true` or `@Table(uniqueConstraints = ...)`
- **Lombok:** Use `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder` for entities/services

### Frontend
- **File naming:** PascalCase for components/pages (`AuditTrail.js`, `AuthContext.js`)
- **Folder structure:** `pages/` for routed views, `components/` for reusable UI, `utill/` for helpers/API
- **API utilities:** Centralized in `utill/` (do not scatter fetch logic across pages)

---

## 6. Development Workflows

### Adding a New Entity (Backend)
1. Create `@Entity` class extending `BaseAuditableEntity` in `com/rdp/model/`
2. Define `@Table(name = "rdp_<name>s")`, add indexes if needed
3. Add validation annotations (`@NotBlank`, `@Min`, etc.)
4. Create repository interface in `com/rdp/repository/` extending `JpaRepository`
5. Create service class in `com/rdp/service/` with `@Service` + `@RequiredArgsConstructor`
6. Create controller in `com/rdp/controller/` with `@RestController` + `@RequestMapping("/api/<resource>")`
7. Apply `@PreAuthorize` for role-based access control
8. Audit logging handled automatically by `AuditAspect`—no extra code unless custom actions needed

### Adding a New Frontend Page
1. Create component file in `pages/` (e.g., `NewFeature.js`)
2. Add route in `App.js`: `<Route path="/new-feature" element={<PrivateRoute><NewFeature /></PrivateRoute>} />`
3. Add navigation link in `Sidebar.js`
4. Use `useContext(AuthContext)` for `token`/`roles`; call backend via `api()` from `utill/api.js`
5. Manage local state with `useState`, fetch data in `useEffect`

### Testing Changes
- **Backend:** Write JUnit 5 tests in `src/test/`, mock dependencies with Mockito, run `./gradlew test`
- **Frontend:** Manual testing in dev mode (`npm start`), log bugs in `qa_test/` Excel sheet
- **QA workflow:** Feature branch → `developer` → testing → bug fixes → `release`

### Release Process
- **Backend:** Tag release (`git tag vX.Y.Z`), `./gradlew clean build`, deploy jar from `build/libs/`
- **Frontend:** `npm run build` → deploy `build/` folder to static hosting or app server

---

## 7. Common Pitfalls & How to Avoid

❌ **Bypassing service layer** → Always go Controller → Service → Repository  
❌ **Returning unpaginated lists** → Use `Pageable` in controller methods  
❌ **Ignoring optimistic locking** → Handle `ObjectOptimisticLockingFailureException` on concurrent updates  
❌ **Storing sensitive data in audit** → Mask or exclude fields (password, token, secret) before logging  
❌ **Breaking audit trail** → Do not modify `BaseAuditableEntity` or `AuditAspect` without discussion  
❌ **Hardcoding roles** → Use `@PreAuthorize` with role constants, verify `ROLE_` prefix in `JwtAuthFilter`  
❌ **Scattering API calls** → Keep fetch logic in `utill/api.js` or `auditApi.js`, not inline in components  

---

## 8. Key File Reference Examples

| Pattern | Example File |
|---------|--------------|
| Entity with audit + validation | `src/main/java/com/rdp/model/Product.java` |
| Thin REST controller | `src/main/java/com/rdp/controller/AuditController.java` |
| Service with constructor injection | `src/main/java/com/rdp/service/CategoryService.java` |
| JWT security filter | `src/main/java/com/rdp/security/JwtAuthFilter.java` |
| Base auditable entity | `src/main/java/com/rdp/audit/BaseAuditableEntity.java` |
| React auth context | `pharmacy-management/src/components/AuthContext.js` |
| API wrapper utility | `pharmacy-management/src/utill/api.js` |
| Audit-specific API | `pharmacy-management/src/utill/auditApi.js` |
| Protected route wrapper | `pharmacy-management/src/components/PrivateRoute.js` |
| Audit trail page | `pharmacy-management/src/pages/AuditTrail.js` |
| App routing structure | `pharmacy-management/src/App.js` |

---

## 9. Agent Guidelines (DO / DON'T)

✅ **DO:**
- Follow existing layer structure (controller → service → repository)
- Extend `BaseAuditableEntity` for new entities
- Use `@PreAuthorize` for endpoint security
- Centralize API calls in `utill/` on frontend
- Add Jakarta validation annotations on entity fields
- Write focused service methods, keep controllers thin
- Use `Pageable` for list endpoints
- Test with security context when auditing is involved

🚫 **DON'T:**
- Introduce new frameworks (e.g., MapStruct, Redux) without explicit request
- Modify audit core (`BaseAuditableEntity`, `AuditAspect`) without discussion
- Expose repositories directly to controllers
- Log or store sensitive fields (password, token, secret) in audit
- Return large unpaginated results from REST endpoints
- Hardcode URLs or tokens in components—use env vars or context

---

## 10. Quick Navigation

**Backend packages:**
- Audit: `com/rdp/audit/*`
- Security: `com/rdp/security/*`
- Models: `com/rdp/model/*`
- Services: `com/rdp/service/*`
- Controllers: `com/rdp/controller/*`
- Repositories: `com/rdp/repository/*`

**Frontend folders:**
- Pages: `pharmacy-management/src/pages/`
- Components: `pharmacy-management/src/components/`
- API utilities: `pharmacy-management/src/utill/`
- Routing: `pharmacy-management/src/App.js`

---

**Feedback welcome:** Clarify any missing workflow, test strategy, performance practice, or domain-specific logic before making deeper changes.
