# Contributing to Photo Gallery Wizard

Thank you for your interest in contributing! Contributions of any kind — bug reports, feature suggestions, documentation improvements, and code — are warmly welcome.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Suggest a Feature](#how-to-suggest-a-feature)
- [Development Setup](#development-setup)
- [Making a Pull Request](#making-a-pull-request)
- [Coding Standards](#coding-standards)

## Code of Conduct

This project follows a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold it.

## How to Report a Bug

1. **Search existing issues** to avoid duplicates.
2. Open a new issue with the **Bug report** template.
3. Include:
   - A clear title and description
   - Steps to reproduce the problem
   - Expected vs. actual behaviour
   - Java version, OS, and whether you use the JAR or native binary

## How to Suggest a Feature

1. **Search existing issues** to see if the idea has already been discussed.
2. Open a new issue with the **Feature request** template.
3. Describe the use case and why it would be valuable.

## Development Setup

### Prerequisites

- **Java 25** (Temurin distribution recommended)
- **Maven** (or use the included `./mvnw` wrapper)

### Build

```bash
# Run all tests with coverage
./mvnw verify

# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Run the application locally
./mvnw spring-boot:run
```

### Code Formatting

The project uses [Spring Java Format](https://github.com/spring-io/spring-javaformat) to enforce consistent formatting. Apply the formatter before committing:

```bash
./mvnw spring-javaformat:apply
```

The CI pipeline will fail if the code is not formatted correctly. You can validate locally with:

```bash
./mvnw spring-javaformat:validate
```

## Commit Messages

This project uses **[Conventional Commits](https://www.conventionalcommits.org/)**.

Format: `<type>(<scope>): <short description>`

| Type | When to use |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `docs` | Documentation only |
| `test` | Adding or correcting tests |
| `chore` | Build tooling, dependency updates, CI |
| `perf` | Performance improvement |

**Rules:**
- Use the **imperative mood** in the short description ("add" not "added")
- Keep the subject line ≤ 72 characters
- Reference issues in the footer: `Closes #42`
- **Make small, atomic commits** — one logical change per commit

**Examples:**
```
feat(tui): add keyboard shortcut to jump to upload step
fix(watermark): correct resize calculation for portrait images
refactor(tui): extract color palette into helper class
docs(readme): update configuration property table
chore(ci): add format validation to CI pipeline
```

## Making a Pull Request

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feat/my-new-feature
   ```
2. Make your changes in **small, atomic commits** following the commit conventions above.
3. **Add tests** where appropriate.
4. Run the full build:
   ```bash
   ./mvnw verify
   ```
5. Push your branch and open a pull request against `main`.
6. Fill in the pull request template and describe *what* and *why*.

PRs are reviewed promptly. Please be patient if feedback takes a day or two.

## Coding Standards

- Follow standard Java conventions and the existing code style.
- Keep services stateless and use constructor injection (never `@Autowired` on fields).
- All configuration and model classes are **records** with compact constructors that validate inputs and provide defaults.
- No `*Impl` class names. Services end with `Service`, properties end with `Properties`.
- No `java.util.Date` or `java.util.Calendar` — use `java.time` APIs.
- No `System.out` / `System.err` — use SLF4J `LOGGER` (`private static final`).
- Fields must not be `public`. Constants follow `UPPER_SNAKE_CASE`.
- Interfaces must not have an `I` prefix.
- Architecture rules are enforced by [Taikai](https://github.com/enofex/taikai) tests; make sure they pass.

### Adding New Properties

1. Add the field to the appropriate `*Properties` record.
2. Add default/validation logic in the compact constructor.
3. Add the default value to `application.properties`.
4. Update **all** test files that construct the record (add the new parameter).
5. Add property tests in the corresponding `*PropertiesTest`.
6. Document the property in `README.md` and `INSTRUCTIONS.md`.

### Adding New Services

1. Create in the `service` package, annotated with `@Service`.
2. Use constructor injection for dependencies.
3. Keep stateless — store config in `final` fields set from properties in the constructor.
4. Create a matching `*ServiceTest` in the test `service` package (package-private class and methods).
