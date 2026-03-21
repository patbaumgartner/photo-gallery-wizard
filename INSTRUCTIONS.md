# Photo Gallery Wizard — Instructions

This document provides step-by-step instructions for running, configuring, and building the Photo Gallery Wizard application.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
- [Configuration](#configuration)
- [Building from Source](#building-from-source)
- [Building a Native Image](#building-a-native-image)
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

### Using the Maven wrapper

```bash
./mvnw spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080) to access the wizard.

### Using the JAR

```bash
java -jar target/photo-gallery-wizard-*.jar
```

---

## Configuration

All options can be set via `application.properties`, environment variables, or command-line flags (Spring Boot relaxed-binding applies).

**Example `application.properties`:**

```properties
app.gallery-name=My Photo Gallery
app.gallery-description=A beautiful collection of memories
app.output-path=gallery-output
```

**Available properties:**

| Property | Default | Description |
|---|---|---|
| `app.gallery-name` | `My Photo Gallery` | Default gallery name shown in the wizard |
| `app.gallery-description` | *(empty)* | Default gallery description |
| `app.output-path` | `gallery-output` | Default output directory for generated gallery files |

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/patbaumgartner/photo-gallery-wizard.git
cd photo-gallery-wizard

# Build (skipping tests for speed)
./mvnw clean package -DskipTests

# Build including tests
./mvnw clean verify
```

The fat JAR is produced at `target/photo-gallery-wizard-*.jar`.

---

## Building a Native Image

To build a GraalVM native image (requires GraalVM with `native-image` installed):

```bash
./mvnw clean package -Pnative -DskipTests
```

The native binary will be produced at `target/photo-gallery-wizard`.

---

## Downloading a Release

Download the pre-built JAR or native binary for your platform from the [Releases](https://github.com/patbaumgartner/photo-gallery-wizard/releases) page.

Each release contains:

* `photo-gallery-wizard-<version>.jar` — runnable fat JAR (requires Java 25)
* `photo-gallery-wizard-<version>-linux.zip` — native binary for Linux
* `photo-gallery-wizard-<version>-windows.zip` — native binary for Windows
