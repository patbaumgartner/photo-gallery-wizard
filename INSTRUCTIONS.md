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
| `app.event-name` | _(blank)_ | Class name (pre-fills the TUI form) |

### Image Properties

All properties use the `app.image` prefix.

| Property | Default | Description |
|---|---|---|
| `app.image.watermark-path` | `configuration/watermark.png` | Watermark image used during the resize step |
| `app.image.resize-max-edge` | `1200` | Maximum pixel dimension after resize |
| `app.image.watermark-opacity` | `0.5` | Watermark transparency (0.0–1.0) |
| `app.image.watermark-scale` | `0.6` | Watermark size as fraction of image width (0.0–1.0) |
| `app.image.jpeg-quality` | `0.9` | JPEG compression quality (0.0–1.0) |
| `app.image.filename-strip-postfix` | `_NEU` | Postfix to strip from filenames during watermarking (e.g., `MEL_6175_NEU.jpg` → `MEL_6175.jpg`). Set to empty to disable. |
| `app.image.logo-connect-timeout-ms` | `5000` | HTTP connect timeout for logo download (ms) |
| `app.image.logo-read-timeout-ms` | `10000` | HTTP read timeout for logo download (ms) |

### Schulfotos Properties

All properties use the `app.schulfotos` prefix.

| Property | Default | Description |
|---|---|---|
| `output-dir` | `schulfotos` | Root directory for event folders |
| `base-url` | `https://example.com/schulfotos` | Base URL for gallery pages |
| `gallery-url` | `https://example.com/schulfotos/?code=` | URL template for gallery codes |
| `default-code-count` | `17` | Default number of gallery codes to generate |
| `code-charset` | `A–Z0–9` | Character set for code generation |
| `qr-size` | `200` | QR code image size in pixels |
| `grid-columns` | `3` | Number of columns on the PDF card grid |
| `grid-rows` | `4` | Number of rows on the PDF card grid |
| `show-cutting-lines` | `true` | Whether to render cutting guides on the PDF |
| `gallery-code-label` | `GALERIE CODE` | Label printed above the gallery code |
| `gallery-password-label` | `GALERIE PASSWORT` | Label printed above the password |
| `logo-path` | `configuration/logo.png` | Logo image path (HTTP URL or local file) |
| `klassenfoto-folder` | `klassenfoto` | Folder name for class photos |
| `portrait-prefix` | `portrait-` | Prefix for portrait folders |
| `watermarked-suffix` | `-watermarked` | Suffix appended to output folder names |
| `password-length` | `9` | Generated password length |

### PicPeak Properties

PicPeak credentials are loaded from an optional external file:

```
configuration/picpeak-credentials.properties
```

Copy `configuration/picpeak-credentials.properties.example` and fill in your values. The file is git-ignored by default.

All PicPeak properties use the `app.picpeak` prefix — see the [README](README.md#picpeak-properties) for the full list.

## TUI Workflow

The terminal wizard runs in seven steps:

1. `Schulfotos`: enter class name, event code, shooting date, code count, and PicPeak toggle.
2. `Review`: check the derived URLs, paths, and PDF layout before execution.
3. `Results`: review the generated CSV and PDF paths. The CSV is uploaded to the gallery server automatically.
4. `Folders`: create the `<class-name>-<event-code>/` folder tree from the generated CSV.
5. `Watermark`: resize and watermark all photos into sibling `-watermarked` folders.
6. `Upload`: upload `klassenfoto-watermarked/` and the matching portrait folder to each PicPeak gallery.
7. `Done`: review the final summary, then press `F3` to start again if needed.

### Generated Files And Folders

Step 3 writes these files into `schulfotos/`:

- `<class-name>-<event-code>-codes.csv`
- `<class-name>-<event-code>-qr-codes.pdf`

Step 4 then creates this folder structure:

```text
schulfotos/
   <class-name>-<event-code>/
      klassenfoto/
      portrait-1/
      portrait-2/
      …
```

After step 5, the output folders appear beside the sources:

```text
schulfotos/
   <class-name>-<event-code>/
      klassenfoto/
      klassenfoto-watermarked/
      portrait-1/
      portrait-1-watermarked/
      portrait-2/
      portrait-2-watermarked/
```

### Keyboard Shortcuts

| Key | Action |
|---|---|
| `Tab` | Move focus between form fields |
| `Enter` | Advance or execute the current step |
| `F2` | Go back one step |
| `F3` | Reset and start a new workflow |
| `F4` | Jump to Folders |
| `F5` | Jump to Watermark |
| `F6` | Jump to Upload |
| `Up` / `Down` | Select CSV files or event folders |
| `Ctrl+C` | Exit |

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
   git tag v1.0.1
   git push origin v1.0.1
   ```
2. The [Release workflow](.github/workflows/release.yml) runs automatically and:
   - Builds and tests the JAR
   - Builds native binaries for Linux, macOS, and Windows
   - Packages each artifact with `logo.png` and the `configuration/` folder
   - Creates platform-specific ZIP archives
   - Generates SHA-256 checksums
   - Publishes a GitHub Release with auto-generated release notes

The release tag includes the `v` prefix (for example `v1.0.1`), but archive names use the plain semantic version (`1.0.1`).

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
