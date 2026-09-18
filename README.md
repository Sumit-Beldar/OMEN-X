# OMEN-X • Global OSINT & Threat Intelligence Platform

**OMEN-X** is a modular Open Source Intelligence (OSINT) and threat analysis dashboard built in Java and JavaFX. It integrates multi-source reconnaissance scanners, forensic file analysis, and session activity tracking within a dark glassmorphic desktop interface, alongside an automated launcher subsystem for the 3D OSIRIS geospatial surveillance platform.

---

## Features & Modules

### 1. Username Recognition (`UsernameScanner`)
- High-concurrency HTTP scanner querying over 500 public platforms, developer portals, and social networks (e.g., GitHub, Reddit, Instagram, X, Pinterest, Twitch, Steam, Spotify).
- Real-time status detection (200 OK vs. 404/indeterminate) and automated profile URL generation.

### 2. Email Exposure & Deliverability (`EmailScanner`)
- Email format and RFC regex validation.
- Domain extraction and DNS MX record verification via Java Naming and Directory Interface (`InitialDirContext`).
- Disposable/temporary email domain filtering against known provider lists.

### 3. Domain & DNS Intelligence (`DomainScanner`)
- Comprehensive DNS record enumeration (A, AAAA, MX, NS, TXT, CNAME, SOA).
- Subdomain enumeration across common host prefixes (`www`, `mail`, `api`, `dev`, `portal`, `vpn`, etc.).
- Active SSL/TLS certificate inspection over HTTPS (issuer, subject, validity dates, SANs).

### 4. IP Geolocation & Threat Scoring (`IPScanner`)
- Network routing and geolocation lookup via IP-API (country, city, coordinates, ISP, AS number, reverse DNS).
- Security reputation analysis and abuse confidence scoring via the AbuseIPDB v2 REST API.

### 5. Telecom & Phone Intelligence (`PhoneScanner`)
- Phone number validation and E.164 parsing via Google `libphonenumber`.
- Carrier identification, geographic area resolution, and timezone mapping.
- Optional extended validation via Abstract API Phone Validation.

### 6. EXIF Metadata & Forensic Extraction (`ImageMetadataScanner`)
- Deep EXIF and IPTC metadata extraction from image files using Drew Noakes `metadata-extractor`.
- Parses camera make, model, lens settings, exposure, aperture, ISO, creation timestamps, and software.
- Extracts GPS decimal coordinates (latitude, longitude, altitude) for geolocation analysis.

### 7. Reverse Image Intelligence (`ReverseImageScanner`)
- Uploads target images to `imgdb.io` via multipart HTTP requests.
- Generates targeted reverse search queries across Google Lens, Yandex Visual Search, Bing Visual Search, and TinEye.
- Optional image similarity matching via QuanticData.

### 8. Malware & Threat Analysis (`MalwareScanner`)
- Computes cryptographic file checksums (SHA-256, SHA-1, MD5).
- Queries the VirusTotal v3 REST API for security vendor detections, reputation scores, threat categories, and scan verdicts.

### 9. OSIRIS 3D Geospatial Reconnaissance Subsystem (`com.omenx.backend.osiris`)
- Embedded runtime and process supervisor for launching the third-party OSIRIS 3D surveillance engine.
- Automated 5-stage setup: runtime verification (Node.js, npm, Git), repository cloning via Eclipse JGit, npm dependency installation, Next.js production compilation, and background server lifecycle management with health-check probing and automatic browser launch.

### 10. Session History & Persistence (`com.omenx.database`)
- Thread-safe session database (`ScanDatabase`) tracking all executed investigations and finding counts.
- Exportable investigation reporting via `ReportService`.

---

## Project Structure

```
omen-x
+-- .gitignore
+-- pom.xml
+-- README.md
+-- src
    +-- main
        +-- java
        |   +-- com
        |       +-- omenx
        |           +-- backend
        |           |   +-- Main.java
        |           |   +-- osint
        |           |   |   +-- DomainScanner.java
        |           |   |   +-- EmailScanner.java
        |           |   |   +-- ImageMetadataScanner.java
        |           |   |   +-- IPScanner.java
        |           |   |   +-- MalwareScanner.java
        |           |   |   +-- PhoneScanner.java
        |           |   |   +-- ReverseImageScanner.java
        |           |   |   +-- UsernameScanner.java
        |           |   +-- osiris
        |           |   |   +-- OsirisConfig.java
        |           |   |   +-- OsirisInstaller.java
        |           |   |   +-- OsirisProcessManager.java
        |           |   |   +-- OsirisProgressListener.java
        |           |   |   +-- OsirisRuntimeManager.java
        |           |   +-- service
        |           |   |   +-- ReportService.java
        |           |   |   +-- ScanService.java
        |           |   +-- tools
        |           |       +-- TestScanner.java
        |           +-- database
        |           |   +-- model
        |           |   |   +-- ScanRecord.java
        |           |   +-- README.md
        |           |   +-- ScanDatabase.java
        |           +-- frontend
        |               +-- AppHeader.java
        |               +-- DashboardUI.java
        |               +-- ImageMetadataUI.java
        |               +-- IPAddressUI.java
        |               +-- LoginUI.java
        |               +-- MalwareAnalysisUI.java
        |               +-- OsirisLauncherUI.java
        |               +-- ReverseImageUI.java
        +-- resources
            +-- styles
            |   +-- app.css
            +-- config.properties
```

---

## Tech Stack & Dependencies

- **Language & Runtime**: Java 25
- **GUI Framework**: JavaFX 25 (`javafx-controls`, `javafx-fxml`)
- **Build System**: Apache Maven & `javafx-maven-plugin` (0.0.8)
- **JSON Processing**: Jackson Databind (2.20.0)
- **Image Metadata**: Drew Noakes `metadata-extractor` (2.19.0)
- **Phone Number Parsing**: Google `libphonenumber` (9.0.38), `carrier` (2.38), `geocoder` (3.37), `prefixmapper` (3.37)
- **Git Engine**: Eclipse JGit (7.1.0)
- **Logging**: SLF4J Simple (2.0.16)

---

## Setup & Installation

### Prerequisites
- **Java Development Kit (JDK)**: JDK 25 or later
- **Apache Maven**: Version 3.8+
- **Node.js & npm** *(Optional, required only for the OSIRIS 3D subsystem)*: Node.js 18+ and npm
- **Git** *(Optional, for OSIRIS repository management)*

### Installation Steps

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd omen-x
   ```

2. **Compile the project**:
   ```bash
   mvn clean compile
   ```

3. **Run the application**:
   ```bash
   mvn javafx:run
   ```

*(Alternatively, run the compiled main class directly:)*
```bash
java -cp target/classes com.omenx.backend.Main
```

---

## Configuration & API Keys

External API integrations are optional. Core reconnaissance modules function without keys, but full intelligence feeds require API keys configured in `src/main/resources/config.properties`.

Create or update `src/main/resources/config.properties` with placeholder keys:

```properties
# AbuseIPDB API Key (IP Reputation)
abuseipdb_api_key=YOUR_ABUSEIPDB_API_KEY

# VirusTotal API v3 Key (Malware & File Hash Scanning)
virustotal_api_key=YOUR_VIRUSTOTAL_API_KEY

# Abstract API Key (Extended Phone Validation)
ABSTRACT_PHONE_API_KEY=YOUR_ABSTRACT_PHONE_API_KEY

# QuanticData API Key (Extended Reverse Image Matching)
QUANTICDATA_API_KEY=YOUR_QUANTICDATA_API_KEY
```

> [!NOTE]
> `config.properties` is ignored by version control to prevent exposing sensitive credentials.

For the **OSIRIS Subsystem**, API keys for OpenSky Network, N2YO Satellite tracking, and NASA FIRMS can be configured directly inside the application via the OSIRIS Settings modal or via `.env.local` inside the OSIRIS workspace directory.

---

## Third-Party Attribution

The OSIRIS 3D global reconnaissance engine integrated into this platform is a third-party project by simplifaisoul:
- **Project**: [OSIRIS](https://github.com/simplifaisoul/osiris)
- **License**: MIT License
- **Usage**: Managed as an external subsystem via automated runtime cloning and process supervision under the terms of the MIT License.

---

## Responsible Use & Legal Notice

OMEN-X is developed solely for authorized security research, educational purposes, threat hunting, and defensive intelligence gathering. 

- Users are responsible for ensuring that all queries and scans comply with applicable local, national, and international laws.
- Do not use this tool against targets without explicit authorization or in violation of target platform terms of service.
- The authors assume no liability for misuse, unauthorized access, or damages resulting from the use of this software.

---

## License

This project is licensed under the [MIT License](LICENSE).
