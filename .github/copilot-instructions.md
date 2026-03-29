# Copilot Instructions — Photo Gallery Wizard

## Project Overview

Spring Boot 4.0.5 terminal application (no web server) for school-photography workflows. The interactive TUI drives a 7-step Schulfotos pipeline: gallery-code generation → QR-code PDF → folder creation → watermarking → PicPeak upload.

**Java 25 · Maven 3.9+ · Spring Boot 4.0.5 · TamboUI · ZXing · PDFBox · Apache Commons CSV**

## Architecture

```
config/          — @ConfigurationProperties records (AppProperties, SchulfotosProperties, PicPeakProperties)
model/           — Immutable records (GalleryCode, WizardRequest, PdfOptions, CsvReadResult, WizardExecutionResult)
service/         — Stateless @Service classes with constructor injection
tui/             — TamboUI-based terminal UI (state machine, controller, step views, palette)
```

### Key Conventions

- **All configuration classes are records** annotated with `@ConfigurationProperties`. They validate in the compact constructor and provide sensible defaults for null/zero/blank values.
- **All model classes are records** with compact constructors that coerce nulls to safe defaults.
- **Services are stateless**, use constructor injection (never `@Autowired` on fields), and are annotated with `@Service`. Use `@Autowired` on a constructor only when the class has multiple constructors (e.g., for testability).
- **No `*Impl` class names.** Services end with `Service`, properties end with `Properties`.
- **No `java.util.Date` or `java.util.Calendar`.** Use `java.time` APIs.
- **No `System.out` / `System.err`.** Use SLF4J `LOGGER` (private static final).
- **Fields must not be public.** Constants follow `UPPER_SNAKE_CASE`.
- **Interfaces must not have an `I` prefix.**
- **No wildcard imports.** Use explicit imports only.
- These rules are enforced by Taikai architecture tests.

### Class Member Ordering

Follow standard Java class member ordering:

1. Static constants (`private static final`)
2. Instance fields (`private final`)
3. Constructors
4. Public methods
5. Package-private methods
6. Private methods
7. Inner classes, interfaces, records, and enums

## Code Style

- **Spring Java Format** is enforced. Always run `./mvnw spring-javaformat:apply` before committing. CI validates formatting.
- Use tabs for indentation (Spring Java Format default).
- Imports: no wildcards, no cycles. Remove unused imports.
- Never silently swallow exceptions — at minimum log at `debug` level.

## Testing

- JUnit 5 + AssertJ for assertions.
- Test classes are **package-private** (no `public`), named `*Test`.
- Test methods are **package-private** (no `public`).
- Every test method must contain assertions or verifications.
- `@Disabled` is not allowed on test classes or methods.
- Test fixtures use `@TempDir` for file system tests and create images programmatically (no checked-in test resources).

## Build Commands

```bash
./mvnw clean verify                    # Full build with tests + coverage + format validation
./mvnw test                            # Run tests only
./mvnw spring-javaformat:apply         # Auto-format all Java files
./mvnw spring-javaformat:validate      # Check formatting without modifying
./mvnw clean package -Pnative -DskipTests  # GraalVM native image
```

## Configuration Properties

Properties are organized under three prefixes:

| Prefix | Record | Purpose |
|--------|--------|---------|
| `app.*` | `AppProperties` | Event code/name, watermark (opacity 0.5, scale 0.6), resize max 1200px, JPEG quality 0.9, filename postfix, logo timeouts |
| `app.schulfotos.*` | `SchulfotosProperties` | Gallery URLs, QR/grid/PDF layout, folder naming, password length |
| `app.picpeak.*` | `PicPeakProperties` | PicPeak API integration (URL, credentials, gallery settings) |

PicPeak credentials are in `configuration/picpeak-credentials.properties` (git-ignored), imported via `spring.config.import`.

## Key Domain Concepts

- **Gallery code** — Format `XXXX-YYYY-ZZZZ`, first 4 chars are the event code.
- **Event folder** — `schulfotos/<event-name>/` with `klassenfoto/` and `portrait-1/` through `portrait-N/`.
- **Watermarked suffix** — `-watermarked` appended to folder names (configurable via `app.schulfotos.watermarked-suffix`).
- **Filename strip postfix** — Removes a configurable postfix (default `_NEU`) from filenames during watermarking (e.g., `MEL_6175_NEU.jpg` → `MEL_6175.jpg`). Configured via `app.filename-strip-postfix`.
- **PicPeak** — Optional cloud gallery platform. Each gallery gets all class photos + its own portrait folder.

## Commit Convention

Use **Conventional Commits**: `<type>(<scope>): <short description>`

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`

## When Adding New Properties

1. Add the field to the appropriate `*Properties` record.
2. Add default/validation logic in the compact constructor.
3. Add the default value to `application.properties`.
4. Update **all** test files that construct the record (add the new parameter).
5. Add property tests in the corresponding `*PropertiesTest`.
6. Document the property in [README.md](../README.md) and [INSTRUCTIONS.md](../INSTRUCTIONS.md).

## When Adding New Services

1. Create in the `service` package, annotated with `@Service`.
2. Use constructor injection for dependencies.
3. Keep stateless — store config in `final` fields set from properties in the constructor.
4. Create a matching `*ServiceTest` in the test `service` package (package-private class and methods).

## When Modifying Existing Code

1. Follow the existing class member ordering (constants → fields → constructors → methods → inner types).
2. Remove unused imports after refactoring.
3. Run `./mvnw spring-javaformat:apply` before committing.
4. Verify with `./mvnw clean verify` — all 345+ tests must pass.
