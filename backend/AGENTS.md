# AGENTS.md

## Project shape
- Spring Boot 3.2 / Java 21 backend for a Discord-like app.
- Main entrypoint: `src/main/java/com/hubble/HubbleApplication.java`.
- Packages are organized by concern: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `security`, `configuration`, `exception`, `validator`.

## How requests flow
- REST controllers are thin and return `ApiResponse<T>` wrappers; see `controller/AuthController.java`, `controller/MessageController.java`, `controller/MediaController.java`.
- Business logic lives in services and throws `AppException` with `ErrorCode` values; do not leak raw exceptions to controllers.
- Validation keys in request DTOs are enum-style strings like `USERNAME_INVALID` / `EMAIL_INVALID`; `GlobalExceptionHandler` maps them to user-facing messages and replaces `{min}` placeholders.

## Important runtime boundaries
- Auth is stateless JWT-based: `SecurityConfig` permits only `/api/auth/**`, `/api/media/**`, and `/ws/**`; everything else requires authentication.
- `UserPrincipal` stores the authenticated user UUID in `getUsername()`; controllers commonly read the user via `@AuthenticationPrincipal` or `Authentication`.
- WebSocket/STOMP is configured in `configuration/WebSocketConfig.java` with endpoint `/ws`, broker prefix `/topic`, and app prefix `/app`.
- Realtime chat writes to DB first, then broadcasts `MessageResponse` to `/topic/channels/{channelId}` from `MessageService`.

## Storage and integrations
- File uploads go through `MediaService` + `StorageService`, then persist an `Attachment` row before being linked to a message later.
- `StorageProperties` is bound from `app.storage.*`; `MinioConfig` is conditional on `app.storage.provider=minio`.
- `application.yml` also defines Postgres, mail, JWT, Supabase, and Google OAuth settings; prefer env overrides for environment-specific values.

## Code conventions to follow
- Use Lombok patterns already in the repo: `@RequiredArgsConstructor`, `@FieldDefaults`, `@Builder`, `@Data`.
- Use MapStruct mappers for entity/DTO conversion; e.g. `mapper/MessageMapper.java`.
- Keep controller methods small: validate input, call service, wrap result in `ApiResponse`.
- Preserve the existing message style: much of the domain/error text is in Vietnamese.

## Testing and verification
- Unit tests live under `src/test/java/com/hubble/...`; existing tests use JUnit 5 + Mockito, e.g. `service/SessionServiceTest.java`.
- Run `mvn test` before finishing non-trivial changes.
- For packaging, use `mvn clean package`; Docker builds with the repo `Dockerfile` already run `mvn clean package -Dmaven.test.skip=true`.

## Helpful docs
- Realtime chat flow is documented in `docs/websocket-flow.md`.
- There is no existing repo-specific agent guidance file in the workspace; this file is the source of truth for AI coding agents.
