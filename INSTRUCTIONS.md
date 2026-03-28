# Photo Gallery Wizard — Instructions

Step-by-step guide for running, configuring, and building the Photo Gallery Wizard.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)
- [Building from Source](#building-from-source)
- [Building a Native Image](#building-a-native-image)
- [Creating a Release](#creating-a-release)
- [Downloading a Release](#downloading-a-release)

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 25+ |
| Maven | 3.9+ |

> **Tip:** A Maven wrapper (`./mvnw` / `mvnw.cmd`) is included, so you only need Java installed.

---

## Running the Application

This is a **terminal application** — it renders an interactive TUI directly in your terminal. It does not start a web server.

### Using the Maven wrapper

```bash
./mvnw spring-boot:run
```

### Using the JAR

```bash
java -jar target/photo-gallery-wizard-*.jar
```

### Using the native binary

```bash
./photo-gallery-wizard
```

---

## Configuration

All options can be set in `application.properties`, environment variables, or command-line flags (Spring Boot relaxed-binding applies).

### App Properties

| Property | Default | Description |
|---|---|---|
| `app.event-code` | _(blank)_ | 4-char alphanumeric event code (pre-fills the TUI form) |
| `app.event-name` | _(blank)_ | Event name (pre-fills the TUI form) |
| `app.watermark-path` | `configuration/watermark.png` | Watermark image used during the resize step |
| `app.resize-max-edge` | `1200` | Maximum pixel dimension after resize |

### PicPeak Properties

PicPeak credentials are loaded from an optional external file:

```
configuration/picpeak-credentials.properties
```

Copy `configuration/picpeak-credentials.properties.example` and fill in your values. The file is git-ignored by default.

All PicPeak properties use the `app.picpeak` prefix — see the [README](README.md#picpeak-properties) for the full list.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/patbaumgartner/photo-gallery-wizard.git
cd photo-gallery-wizard

# Build and run tests with coverage
./mvnw clean verify

# Build JAR only (skip tests)
./mvnw clean package -DskipTests
```

The fat JAR is produced at `target/photo-gallery-wizard-<version>.jar`.

### Code Formatting

The project enforces [Spring Java Format](https://github.com/spring-io/spring-javaformat). Apply formatting before committing:

```bash
./mvnw spring-javaformat:apply
```

The CI pipeline validates formatting automatically.

---

## Building a Native Image

Requires GraalVM with `native-image`:

```bash
./mvnw clean package -Pnative -DskipTests
```

The native binary is produced at `target/photo-gallery-wizard`.

---

## Creating a Release

Releases are fully automated via GitHub Actions. To create a release:

1. Tag the commit:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. The [Release workflow](.github/workflows/release.yml) runs automatically and:
   - Builds and tests the JAR
   - Builds native binaries for Linux, macOS, and Windows
   - Packages each artifact with `logo.png` and the `configuration/` folder
   - Creates platform-specific ZIP archives
   - Generates SHA-256 checksums
   - Publishes a GitHub Release with auto-generated release notes

---

## Downloading a Release

Download pre-built artifacts from the [Releases](https://github.com/patbaumgartner/photo-gallery-wizard/releases) page.

Each release contains:

| Artifact | Description |
|---|---|
| `photo-gallery-wizard-<version>-jar.zip` | Runnable fat JAR (requires Java 25) |
| `photo-gallery-wizard-<version>-linux.zip` | Native binary for Linux |
| `photo-gallery-wizard-<version>-macos.zip` | Native binary for macOS |
| `photo-gallery-wizard-<version>-windows.zip` | Native binary for Windows |
| `checksums-sha256.txt` | SHA-256 checksums for all artifacts |

Every ZIP includes the binary/JAR, `logo.png`, and a `configuration/` folder with `picpeak-credentials.properties.example` and `watermark.png`.
