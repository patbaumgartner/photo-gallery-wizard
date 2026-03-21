[![CI](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/ci.yml/badge.svg)](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/ci.yml) [![Release](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/release.yml/badge.svg)](https://github.com/patbaumgartner/photo-gallery-wizard/actions/workflows/release.yml) [![Java](https://img.shields.io/badge/Java-25-blue?logo=openjdk)](https://openjdk.org/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Photo Gallery Wizard

A Spring Boot wizard application that guides you through setting up and managing photo galleries — step by step.

---

## Table of Contents

* [Features](#features)
* [Prerequisites](#prerequisites)
* [Getting Started](#getting-started)
  * [Download a release](#download-a-release)
  * [Build from source](#build-from-source)
* [Usage](#usage)
* [Configuration](#configuration)
* [Contributing](#contributing)
* [Code of Conduct](#code-of-conduct)
* [License](#license)

---

## Features

* 🧙 **Step-by-step wizard** — guided multi-step setup process for photo gallery configuration
* ⚙️ **Flexible configuration** — all settings configurable via properties file, environment variables, or command-line flags
* 🚀 **Native image support** — can be compiled to a GraalVM native binary for instant startup and no JVM dependency
* ✅ **Architecture testing** — enforced architectural rules via [Taikai](https://github.com/enofex/taikai)
* 📊 **Code coverage** — JaCoCo coverage reports generated on every CI build

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 25+ |
| Maven | 3.9+ |

> **Tip:** A Maven wrapper (`./mvnw` / `mvnw.cmd`) is included, so you only need Java installed.

---

## Getting Started

### Download a release

Download the pre-built JAR or native binary for your platform from the [Releases](https://github.com/patbaumgartner/photo-gallery-wizard/releases) page.

Each release contains:

* `photo-gallery-wizard-<version>.jar` — runnable fat JAR (requires Java 25)
* `photo-gallery-wizard-<version>-linux.zip` — native binary for Linux
* `photo-gallery-wizard-<version>-windows.zip` — native binary for Windows

### Build from source

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

To build a **GraalVM native image** (requires GraalVM with `native-image` installed):

```bash
./mvnw clean package -Pnative -DskipTests
```

The native binary will be at `target/photo-gallery-wizard`.

---

## Usage

Start the application:

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Using the JAR
java -jar target/photo-gallery-wizard-*.jar
```

Open your browser at [http://localhost:8080](http://localhost:8080) and follow the wizard steps.

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

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to open issues, suggest improvements, and submit pull requests.

---

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating you agree to uphold it.

---

## License

This project is licensed under the [MIT License](LICENSE).
