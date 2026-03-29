[![CI](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/ci.yml/badge.svg)](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/ci.yml) [![Release](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/release.yml/badge.svg)](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/release.yml) [![Java](https://img.shields.io/badge/Java-25-blue?logo=openjdk)](https://openjdk.org/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Photo Gallery Wizard

A Spring Boot terminal application for school-photography workflows. The interactive TUI drives the full Schulfotos pipeline — from gallery-code generation through QR-code PDF rendering, folder creation, watermarking, and PicPeak upload.

## What It Does

- Generates unique gallery codes and secure passwords
- Writes a CSV with gallery URLs and optional PicPeak event IDs
- Uploads the generated CSV to the gallery server
- Renders a duplex-ready QR-code PDF with numbered codes and password back-pages
- Creates an event folder tree for class photos and portraits
- Resizes and watermarks event photos into separate output folders
- Uploads watermarked photos to PicPeak galleries

## Requirements

| Requirement | Version |
|---|---|
| Java | 25+ |
| Maven | 3.9+ |

The Maven wrapper is included, so Java is the only local prerequisite.

## Build

```bash
./mvnw clean verify
```

The runnable JAR is written to `target/photo-gallery-wizard-<version>.jar`.

To build a GraalVM native binary:

```bash
./mvnw clean package -Pnative -DskipTests
```

The binary is written to `target/photo-gallery-wizard`.

### Releasing

Tag a commit to trigger the release workflow:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow builds the JAR and native binaries for Linux, macOS, and Windows, packages each with `logo.png` and the `configuration/` folder, and publishes a GitHub Release with SHA-256 checksums.

## Running

```bash
./mvnw spring-boot:run
```

Or from the JAR:

```bash
java -jar target/photo-gallery-wizard-*.jar
```

This is a terminal application. It does not start a web server.

## TUI Workflow

The TUI drives a seven-step Schulfotos pipeline. Each step has a title shown in the progress bar.

| Step | Title | What happens |
|---|---|---|
| 1 | **Schulfotos** | Enter class name, event code (4 alphanumeric chars), shooting date (`dd.MM.yyyy`), code count, and PicPeak toggle. |
| 2 | **Review** | Inspect the derived configuration (URLs, grid, logo, output paths). Press Enter to execute. |
| 3 | **Results** | View CSV and PDF generation results. The CSV is automatically uploaded to the gallery server when credentials are configured. Press Enter to continue. |
| 4 | **Folders** | Select the generated CSV from `schulfotos/` and create the event folder tree under `<class-name>-<event-code>/` (`klassenfoto/`, `portrait-1/` … `portrait-N/`). |
| 5 | **Watermark** | Select the created event folder and batch-resize and watermark all photos into sibling `-watermarked` output folders. |
| 6 | **Upload** | Select the same event folder and upload watermarked photos to PicPeak galleries. |
| 7 | **Done** | Final summary. Use F4/F5/F6 to jump back to earlier steps if needed. |

### Default Preset

The default preset ships with these values:

| Setting | Value |
|---|---|
| Base URL | `https://example.com/schulfotos` |
| Gallery URL | `https://example.com/schulfotos/?code=` |
| QR size | `200` px |
| Grid layout | 3 columns × 4 rows |
| Cutting lines | enabled |
| Code label | `GALERIE CODE` |
| Password label | `GALERIE PASSWORT` |
| Logo | `logo.png` |

### Keyboard Shortcuts

| Key | Action |
|---|---|
| `Tab` | Move focus between form fields |
| `Enter` | Advance or execute the current step |
| `F2` | Go back one step |
| `F3` | Reset and start a new workflow |
| `F4` | Jump to Folders (step 4) |
| `F5` | Jump to Watermark (step 5) |
| `F6` | Jump to Upload (step 6) |
| `Up` / `Down` | Select CSV files (step 4) or event folders (steps 5 and 6) |
| `Ctrl+C` | Exit |

## Generated Files

Step 3 writes these files into `schulfotos/`:

- `<class-name>-<event-code>-codes.csv`
- `<class-name>-<event-code>-qr-codes.pdf`

### CSV Columns

| Column | Description |
|---|---|
| `Number` | Sequential row number |
| `Code` | Gallery code (`XXXX-XXXX-XXXX`) |
| `Password` | 9-character secure password |
| `Class Name` | Class name from generation |
| `URL` | Full gallery URL |
| `PicPeak Event ID` | PicPeak event identifier (blank when PicPeak is disabled) |

### Folder Structure

The Folders step creates this tree below `schulfotos/`:

```text
schulfotos/
  <class-name>-<event-code>/
    klassenfoto/
    portrait-1/
    portrait-2/
    …
```

After the Watermark step, output folders appear beside the sources:

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

### Filename Strip Postfix

During watermarking, the application strips a configurable postfix from filenames before writing them to the output folder. This is useful when source images have been renamed by an editor (e.g., `MEL_6175_NEU.jpg`). With the default `app.image.filename-strip-postfix=_NEU`, the output file becomes `MEL_6175.jpg`.

Set `app.image.filename-strip-postfix=` (empty) to disable stripping and keep original filenames.

### Upload Behavior

- The generated CSV is automatically uploaded to `{base-url}/upload.php` after generation (step 3) when PicPeak credentials are configured.
- Each gallery receives all files from `klassenfoto-watermarked/`.
- Each gallery also receives its own `portrait-N-watermarked/` files.
- Rows without a PicPeak event ID are skipped.

## Configuration

Spring Boot relaxed binding applies, so properties can be set in `application.properties`, environment variables, or command-line flags.

### App Properties

| Property | Default | Description |
|---|---|---|
| `app.event-code` | _(blank)_ | 4-char alphanumeric event code (pre-fills TUI form field) |
| `app.event-name` | _(blank)_ | Class name (pre-fills TUI form field) |

### Image Properties

All properties use the `app.image` prefix.

| Property | Default | Description |
|---|---|---|
| `app.image.watermark-path` | `configuration/watermark.png` | Watermark image for the resize step |
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
| `portrait-prefix` | `portrait-` | Prefix for portrait folders (`portrait-1`, `portrait-2`, …) |
| `watermarked-suffix` | `-watermarked` | Suffix appended to output folder names |
| `password-length` | `9` | Generated password length |

### PicPeak Properties

PicPeak credentials are loaded from an optional `configuration/picpeak-credentials.properties` file (imported via `spring.config.import`). Copy `configuration/picpeak-credentials.properties.example` to get started.

All properties use the `app.picpeak` prefix.

| Property | Default |
|---|---|
| `enabled` | `false` |
| `create-events` | `true` |
| `api-url` | _(blank)_ |
| `username` | _(blank)_ |
| `password` | _(blank)_ |
| `event-type` | `schulfotos` |
| `event-date` | _(blank)_ |
| `customer-email` | _(blank)_ |
| `admin-email` | _(blank)_ |
| `require-password` | `true` |
| `welcome-message` | _(blank)_ |
| `expiration-days` | `42` |
| `allow-user-uploads` | `false` |
| `feedback-enabled` | `true` |
| `allow-ratings` | `true` |
| `allow-likes` | `true` |
| `allow-comments` | `false` |
| `allow-favorites` | `false` |
| `allow-downloads` | `false` |
| `disable-right-click` | `true` |
| `enable-devtools-protection` | `true` |
| `use-canvas-rendering` | `true` |
| `watermark-downloads` | `false` |
| `hero-logo-visible` | `false` |
| `require-name-email` | `false` |
| `moderate-comments` | `false` |
| `show-feedback-to-guests` | `false` |
| `header-style` | `standard` |
| `hero-divider-style` | `wave` |
| `css-template-id` | `1` |
| `color-theme` | `default` |
| `protection-level` | `standard` |
| `source-mode` | `managed` |
| `hero-image-anchor` | `center` |
| `hero-logo-size` | `medium` |
| `hero-logo-position` | `top` |
| `upload-category-id` | _(unset)_ |
| `klassenfoto-category-id` | _(unset)_ |
| `portrait-category-id` | _(unset)_ |
| `external-path` | _(unset)_ |
| `hero-photo-id` | _(unset)_ |
| `max-password-retries` | `3` |

Minimum required for PicPeak:

```properties
app.picpeak.enabled=true
app.picpeak.api-url=https://your-picpeak-instance
app.picpeak.username=you@example.com
app.picpeak.password=secret
app.picpeak.customer-email=you@example.com
```

## Typical Local Flow

1. Start the TUI.
2. Complete steps 1–3 (Schulfotos → Review → Results) to generate the CSV and PDF in `schulfotos/`.
3. Complete step 4 to create the `<class-name>-<event-code>/` folder tree.
4. Copy source photos into the generated `klassenfoto/` and `portrait-N/` folders.
5. Run step 5 (Watermark) to produce the `-watermarked` output folders.
6. Run step 6 (Upload) to push watermarked photos to PicPeak.

## Development

```bash
# Run all tests
./mvnw test

# Validate code formatting
./mvnw spring-javaformat:validate

# Apply code formatting
./mvnw spring-javaformat:apply
```

The build also runs JaCoCo code coverage and Taikai architecture verification.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Code of Conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE)