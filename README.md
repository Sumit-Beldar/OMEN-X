# 🕵️ OMEN-X — Tactical OSINT & Threat Intelligence Platform

[![Java](https://img.shields.io/badge/Java-25%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-FF3B30?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Status](https://img.shields.io/badge/Status-Operational-22C55E?style=for-the-badge)](https://github.com/)

**OMEN-X** is a military-grade, desktop-based Open Source Intelligence (OSINT) and static security investigation platform. Built on JavaFX with a tactical cyber dark theme, OMEN-X unifies eight intelligence engines into a single, high-performance command center for security researchers, digital forensics investigators, and cybersecurity analysts.

---

## ⚡ Key Highlights & Architecture

- **Unified Tactical Design System:** Driven by a centralized `app.css` design system with a dark slate palette (`#090C12` / `#111520`) and high-visibility tactical crimson accents (`#DC2626`).
- **Interactive Command Center:** Real-time reconnaissance metrics, active module grids with responsive column scaling, and instant activity logs.
- **Cybersecurity Authentication Gateway:** Terminal-grade login screen featuring an animated tactical canvas with pulsing radar grids, sweeping scanlines, floating ambient particles, and HUD telemetry.
- **Persistent Operations Sidebar:** Fixed navigation shell with military bracket icons (`//>`, `@>`, `#> `, `::>`, `[]> `, `{}> `, `<>`, `>>`, `!!>`) and an unclippable operator status widget with hover tooltips and session management.
- **Standardized `AppHeader` Architecture:** Reusable header component maintaining consistent title, subtitle, status pills (`● READY`, `◈ SCANNING`, `✖ ERROR`), and reliable `"← Dashboard"` back navigation across all modules.

---

## 🛡️ Intelligence Modules (The 8 Operations)

### 1. 👤 `@>` Username Recognition
- Queries 500+ public platforms and social networks simultaneously.
- Identifies profile availability, direct URLs, and digital footprint traces.
- Real-time statistics: Found, Not Found, and Unknown counts with instant clipboard export.

### 2. 📧 `#> ` Email Exposure
- Analyzes target email addresses for breach records and domain deliverability.
- Inspects MX routing, domain validity, and associated online identity exposure.

### 3. 🌐 `::>` Domain & DNS Intelligence
- Full WHOIS registration lookups: Registrar, Organization, Creation & Expiry dates, and WHOIS server.
- Deep DNS record enumeration: A, MX, NS, TXT, and CNAME records.
- Web presence verification: HTTP/HTTPS status, SSL certificate validity, and issuer details.
- Subdomain discovery and domain reputation/blacklist auditing.

### 4. 🌍 `[]>` IP Geolocation
- Resolves IP location metadata: City, Region, Country, Postal Code, Continent, and Coordinates.
- Network infrastructure analysis: ASN, ISP/Carrier, Public IP, Reverse Hostname, and Domain.
- Abuse & threat reputation: Abuse confidence scores, report counts, and Tor exit node detection.
- Interactive OpenStreetMap integration with direct coordinate plotting.

### 5. 📱 `{}> ` Phone Intel
- Powered by Google `libphonenumber` metadata engines.
- Number validation: Possible, Valid, and Country/Region match verification.
- Carrier and network identification: Original network operator and line type (Mobile, Fixed Line, VOIP).
- Geographic metadata and timezone mapping.

### 6. 📷 `<>` EXIF Forensics
- Deep metadata extraction supporting JPG, PNG, GIF, TIFF, and WEBP formats.
- Drag-and-drop image dropzone with real-time thumbnail preview.
- Hardware signatures: Camera make, camera model, lens/optics, software, and exposure settings (F-number, ISO).
- Geolocation forensics: Extracts GPS Latitude/Longitude with one-click Google Maps coordinate navigation.

### 7. 🔍 `>>` Reverse Image Search
- Upload and analyze images across public web search indices.
- Discovers visually identical images, cropped variants, and original source domains.
- Displays ranked results with thumbnail previews, titles, source domains, and direct web links.

### 8. ☣ `!!>` Malware Threat Analysis
- Static file inspection: Computes MD5, SHA-1, SHA-256, and SHA-512 cryptographic hashes.
- Multi-engine detection: VirusTotal threat engine integration for file reputation and detection ratios.
- Analysis breakdown: Malicious, Suspicious, Harmless, and Undetected engine counts.

---

## 📂 Project Structure

```
OMEN-X Main/
├── pom.xml                               # Maven project dependencies & JavaFX plugin
├── README.md                             # Platform documentation
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── omenx/
    │   │           ├── Main.java         # Application entry point & persistent sidebar shell
    │   │           ├── database/
    │   │           │   └── ScanDatabase.java       # Session scan record persistence
    │   │           ├── model/
    │   │           │   └── ScanRecord.java         # Scan activity data model
    │   │           ├── osint/
    │   │           │   ├── DomainScanner.java      # WHOIS, DNS, SSL & Subdomain discovery
    │   │           │   ├── EmailScanner.java       # Breach intelligence & exposure scanner
    │   │           │   ├── ImageMetadataScanner.java # EXIF, GPS & camera hardware extraction
    │   │           │   ├── IPScanner.java          # IP-API & AbuseIPDB network geolocation
    │   │           │   ├── MalwareScanner.java     # Hash calculation & VirusTotal integration
    │   │           │   ├── PhoneScanner.java       # libphonenumber carrier & geo validator
    │   │           │   ├── ReverseImageScanner.java# Multi-engine image lookup
    │   │           │   ├── UsernameOSINT.java      # Platform definitions & headers
    │   │           │   └── UsernameScanner.java    # Multithreaded social & platform scanner
    │   │           ├── service/
    │   │           │   ├── ReportService.java      # Investigation reporting & export service
    │   │           │   └── ScanService.java        # Core scan broker & history management
    │   │           └── ui/
    │   │               ├── AppHeader.java          # Standardized modular header component
    │   │               ├── DashboardUI.java        # Command Center with responsive module grid
    │   │               ├── IPAddressUI.java        # IP Geolocation workspace with stat strip
    │   │               ├── ImageMetadataUI.java    # EXIF Forensics drop-zone workspace
    │   │               ├── InvestigationUI.java    # Multi-source intelligence workspace
    │   │               ├── LoginUI.java            # Tactical animated cyber login gateway
    │   │               ├── MalwareAnalysisUI.java  # Threat analysis & engine detection UI
    │   │               └── ReverseImageUI.java     # Reverse image search workspace
    │   └── resources/
    │       ├── config.properties                   # API keys & platform configurations
    │       └── styles/
    │           └── app.css                         # Unified tactical crimson design system
    └── test/
        └── java/
            └── com/
                └── omenx/
                    └── TestScanner.java            # Standalone scanner verification test
```

---

## 🔐 Operator Authentication

Access to OMEN-X requires operator authentication. Authorized credentials:

| Operator Identifier | Access Key | Clearance Level |
| :--- | :--- | :--- |
| `Omen-X(Member1)` / `member1` | `SkibidiSahur@67` | Alpha-1 |
| `Omen-X(Member2)` / `member2` | `mPhg@69` | Alpha-2 |
| `Omen-X(Member3)` / `member3` | `Usersayshello` | Alpha-3 |
| `Omen-X(Member4)` / `member4` | `iwannabehackertoo` | Alpha-4 |
| `Omen-X(Member5)` / `member5` | `tspmofr@911` | Alpha-5 |

---

## 🛠️ Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 25 or higher
- **Apache Maven:** Version 3.8+

### Installation & Run

1. **Clone or navigate to the repository:**
   ```bash
   git clone https://github.com/Sumit-Beldar/OMEN-X.git
   cd "OMEN-X Main"
   ```

2. **Compile the project:**
   ```bash
   mvn clean compile
   ```

3. **Launch OMEN-X:**
   ```bash
   mvn javafx:run
   ```

---

## ⚖️ Legal & Ethical Notice

> [!WARNING]
> **OMEN-X** is designed exclusively for authorized cybersecurity investigations, educational purposes, and defensive security research. The platform strictly queries publicly available databases and open-source intelligence feeds. Users are solely responsible for complying with all applicable local, national, and international laws when conducting investigations.
