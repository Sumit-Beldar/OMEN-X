# OMEN-X

OMEN-X is a modular Open Source Intelligence (OSINT) and threat analysis desktop application built in Java and JavaFX. It provides security researchers and intelligence analysts with multi-source reconnaissance scanners, media and file forensics tools, session investigation history tracking, and a process supervisor subsystem for running the external OSIRIS 3D geospatial surveillance engine locally.

- **Command Center & Tool Launcher**: Centralized operations dashboard with live metrics (total scans, threat findings, active modules), a 9-module categorized tool grid, search filtering across capabilities, and recent activity logs.
- **Username Recognition (`UsernameScanner`)**: Concurrent HTTP client scanner querying account availability across 6 social and developer networks (GitHub, Reddit, Instagram, Pinterest, Twitch, X).
- **Email Exposure & Breach Detection (`EmailScanner`)**: Email syntax validation and data breach identification via the XposedOrNot REST API.
- **Domain & DNS Intelligence (`DomainScanner`)**: JNDI-based DNS record enumeration (A, MX, NS, TXT, CNAME), HTTP/HTTPS reachability probing, SSL/TLS certificate chain inspection, active subdomain resolution across 20 common prefixes, and direct port 43 WHOIS queries.
- **IP Geolocation & Threat Scoring (`IPScanner`)**: Geographic location and ASN routing extraction via IP-API, paired with reputation confidence scoring via the AbuseIPDB v2 REST API.
- **Telecom & Phone Intel (`PhoneScanner`)**: Phone number parsing, E.164 and international formatting, carrier resolution, geographic description, and timezone mapping via Google `libphonenumber`, with optional carrier verification via Abstract API.
- **EXIF Metadata & Forensic Extraction (`ImageMetadataScanner`)**: File signature verification (JPEG, PNG, GIF, WEBP, TIFF), optical metadata extraction (camera make, model, lens settings, exposure, ISO, timestamps), GPS coordinate parsing via Drew Noakes `metadata-extractor`, and Google Maps coordinate redirection.
- **Reverse Image Search (`ReverseImageScanner`)**: Multipart image upload to `imgdb.io` temporary storage combined with QuanticData Google Lens reverse visual search matching, plus direct search redirects to Google Images, Yandex Visual, Bing Visual, and TinEye.
- **Malware Threat Analysis (`MalwareScanner`)**: Local cryptographic hash generation (MD5, SHA-1, SHA-256, SHA-512), local MIME/file type detection, VirusTotal v3 REST API hash reputation analysis, threat classification labels, and explicit multipart file upload to VirusTotal.
- **OSIRIS 3D Geospatial Reconnaissance Subsystem (`com.omenx.backend.osiris`)**: Process manager and automated installer that verifies local runtimes (Node.js, npm, Git/embedded JGit), clones the OSIRIS 3D surveillance repository, runs dependency installation and Next.js production builds, manages local port binding, monitors process health, and launches the browser interface.
- **Operator Authentication & Profile Management (`UserDatabase`, `LoginUI`)**: SQLite-backed user database with BCrypt password hashing, 6-digit email verification dispatch via Jakarta Mail SMTP, session authentication, and pre-seeded operator demo accounts.
- **Session Investigation History (`ScanDatabase`, `ReportService`, `ScanService`)**: In-memory session database recording scan types, targets, finding counts, and timestamps across active investigations.

## Stack

| Layer | Technology | Where It Lives |
| :--- | :--- | :--- |
| Language & Runtime | Java 25 | `pom.xml`, `src/main/java` |
| GUI Framework | JavaFX 25 (`javafx-controls`, `javafx-fxml`) | `src/main/java/com/omenx/frontend` |
| Build System | Apache Maven & `javafx-maven-plugin` (0.0.8) | `pom.xml` |
| Database Layer | SQLite JDBC (3.48.0.0) | `src/main/java/com/omenx/database` |
| Cryptography & Passwords | jBCrypt (0.4) | `src/main/java/com/omenx/backend/service/PasswordUtil.java` |
| Email & SMTP | Jakarta Mail / Angus Mail (2.0.3) | `src/main/java/com/omenx/backend/service/EmailService.java` |
| JSON Processing | Jackson Databind (2.20.0) | `src/main/java/com/omenx/backend/osint` |
| Image Metadata | Drew Noakes `metadata-extractor` (2.19.0) | `src/main/java/com/omenx/backend/osint/ImageMetadataScanner.java` |
| Phone Parsing | Google `libphonenumber` (9.0.38), `carrier`, `geocoder`, `prefixmapper` | `src/main/java/com/omenx/backend/osint/PhoneScanner.java` |
| Git Subsystem | Eclipse JGit (7.1.0) & System Git | `src/main/java/com/omenx/backend/osiris/OsirisInstaller.java` |
| Logging | SLF4J Simple (2.0.16) | `pom.xml` |
| UI Styling | JavaFX CSS | `src/main/resources/styles/app.css` |
| External Subsystem | Node.js / Next.js OSIRIS Platform | `src/main/java/com/omenx/backend/osiris` |
| Testing | JUnit Jupiter (5.11.4) | `src/test/java/com/omenx/database/UserAuthTest.java` |

## Architecture

### High-Level Flow
```
[Desktop User / JavaFX UI]
        │
        ├── Authentication Flow ──► UserDatabase (SQLite `omenx.db`) ──► EmailService (SMTP)
        │
        ├── Investigation Scanners ──► OSINT Modules ──► External APIs / DNS / WHOIS / JNDI / HTTP
        │                                  │
        │                                  └──► ScanService ──► ScanDatabase (In-Memory Session Store)
        │
        └── OSIRIS Subsystem ──► OsirisProcessManager ──► OsirisInstaller (Git/JGit + npm) ──► Next.js Server (Port 3000)
```

1. **User Interaction**: The operator interacts with JavaFX views (`LoginUI`, `DashboardUI`, dedicated scanner views, or `OsirisLauncherUI`).
2. **Authentication Gate**: `LoginUI` verifies credentials against SQLite (`omenx.db`) using BCrypt hash comparison. Unverified accounts require a 6-digit numeric verification code issued by `EmailService`.
3. **Execution Layer**: Scanners execute asynchronously on background executor threads (`Executors.newSingleThreadExecutor()` / `newCachedThreadPool()`) to keep the JavaFX Application Thread responsive.
4. **Data Persistence**: Active scan records are collected in `ScanDatabase` via `ScanService`. User accounts and verification states persist in SQLite (`omenx.db`).
5. **OSIRIS Subsystem Supervision**: `OsirisRuntimeManager` detects Node.js and npm on the host. `OsirisInstaller` clones the external repository to `~/.omenx/tools/osiris/app`, runs `npm install` and `npm run build`, and `OsirisProcessManager` manages the server lifecycle with health-check polling and browser automation.

### Folder Map

```
.
├── pom.xml                                      # Maven build definition and dependency declarations
├── README.md                                    # Project documentation
├── .gitignore                                   # Version control exclusions
├── omenx.db                                     # Local SQLite database file
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── omenx
│   │   │           ├── backend
│   │   │           │   ├── Main.java            # Application entrypoint, shell chrome, and unified scan view
│   │   │           │   ├── osint                # Standalone OSINT scanning and forensic extraction modules
│   │   │           │   │   ├── DomainScanner.java        # DNS, WHOIS, SSL, and subdomain discovery
│   │   │           │   │   ├── EmailScanner.java         # Email regex validation and breach intelligence
│   │   │           │   │   ├── ImageMetadataScanner.java # EXIF, IPTC, and GPS metadata parser
│   │   │           │   │   ├── IPScanner.java            # IP geolocation and AbuseIPDB scoring
│   │   │           │   │   ├── MalwareScanner.java       # Local hashing and VirusTotal v3 scanner
│   │   │           │   │   ├── PhoneScanner.java         # libphonenumber carrier/area resolution
│   │   │           │   │   ├── ReverseImageScanner.java  # imgdb.io upload and QuanticData Google Lens search
│   │   │           │   │   └── UsernameScanner.java      # Social platform HTTP username check
│   │   │           │   ├── osiris               # OSIRIS 3D subsystem lifecycle, installer, and runtime supervision
│   │   │           │   │   ├── OsirisConfig.java         # File paths, ports, and external API credential persistence
│   │   │           │   │   ├── OsirisInstaller.java      # 5-stage setup: JGit/Git clone, npm install, build, cache
│   │   │           │   │   ├── OsirisProcessManager.java # Process spawning, port resolution, health-check, termination
│   │   │           │   │   ├── OsirisProgressListener.java # Lifecycle stages, status enums, and progress listener interface
│   │   │           │   │   └── OsirisRuntimeManager.java # OS-specific binary discovery for Node.js, npm, and Git
│   │   │           │   ├── service              # Application services (email, reporting, scan history, passwords)
│   │   │           │   │   ├── EmailService.java     # Jakarta Mail SMTP delivery for verification codes
│   │   │           │   │   ├── PasswordUtil.java     # BCrypt hashing, verification, and password validation
│   │   │           │   │   ├── ReportService.java    # Investigation metrics and record aggregation
│   │   │           │   │   └── ScanService.java      # Scan record recording and history retrieval
│   │   │           │   └── tools
│   │   │           │       └── TestScanner.java      # CLI testing harness for UsernameScanner
│   │   │           ├── database                 # Data persistence and database models
│   │   │           │   ├── model
│   │   │           │   │   ├── ScanRecord.java       # In-memory scan record domain model
│   │   │           │   │   └── UserRecord.java       # SQLite user account record model
│   │   │           │   ├── README.md                 # Database package overview
│   │   │           │   ├── ScanDatabase.java         # Thread-safe in-memory scan history store
│   │   │           │   └── UserDatabase.java         # SQLite JDBC DAO for user authentication and verification
│   │   │           └── frontend                 # JavaFX UI views, components, and layout builders
│   │   │               ├── AppHeader.java            # Standardized panel header and action bar component
│   │   │               ├── DashboardUI.java          # Command center, metric cards, 9-tool launcher grid, activity log
│   │   │               ├── ImageMetadataUI.java      # EXIF metadata extraction and drag-and-drop file view
│   │   │               ├── IPAddressUI.java          # IP geolocation and reputation intelligence view
│   │   │               ├── LoginUI.java              # Particle radar canvas, operator login, enrollment, verification
│   │   │               ├── MalwareAnalysisUI.java    # File hash analysis and VirusTotal scan workspace
│   │   │               ├── OsirisLauncherUI.java     # OSIRIS 3D subsystem launcher, telemetry console, settings modal
│   │   │               └── ReverseImageUI.java       # Reverse image search visual match workspace
│   │   └── resources
│   │       ├── config.properties.example        # Configuration template for external API credentials and SMTP
│   │       ├── config.properties                # Local active configuration (ignored by version control)
│   │       └── styles
│   │           └── app.css                      # Dark glassmorphic design system stylesheets
│   └── test
│       └── java
│           └── com
│               └── omenx
│                   └── database
│                       └── UserAuthTest.java    # JUnit 5 test suite for SQLite auth, seeding, and verification
```

### Auth Model
- **Identity Storage**: User records reside in SQLite (`omenx.db`) under table `users`.
- **Password Protection**: Passwords are never stored in plaintext; they are hashed using BCrypt with 12 salt rounds (`PasswordUtil.hashPassword()`).
- **Enrollment & Verification**:
  - New operator signup creates an account with `is_verified = 0`.
  - A random 6-digit numeric verification code with a 15-minute expiration timestamp is generated and saved to the database.
  - The verification code is dispatched via SMTP HTML email using `EmailService` (or printed to stderr/stdout if SMTP is unconfigured).
  - An operator must submit the valid 6-digit code before account access is granted.
- **Session State**: Successful authentication transitions `Main.java` from `LoginUI` to `DashboardUI`, injecting top status indicators, the module navigation sidebar rail, and the bottom status bar. Disconnecting returns the user to the login screen.

### Background Execution & Realtime Subsystems
- **Reconnaissance Execution**: Scanner queries execute on dedicated background threads via Java `ExecutorService` and publish updates to the JavaFX Application Thread via `Platform.runLater()`.
- **Telemetry & Process Watchdog**: `OsirisProcessManager` maintains a scheduled background watchdog (`Executors.newSingleThreadScheduledExecutor`) polling process liveness every 3 seconds, captures stdout/stderr streams into an in-memory ring buffer (up to 1,500 lines), and coordinates graceful process tree termination via PID tracking and OS task killers upon application exit.

## Database

### Engine
- **SQLite 3** accessed via `sqlite-jdbc` (`org.xerial:sqlite-jdbc:3.48.0.0`).
- Database file: `omenx.db` (created in the working directory).

### Schema & Migrations
Database tables and indexes are initialized automatically on startup by `UserDatabase.initializeDatabase()`.

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    is_verified INTEGER NOT NULL DEFAULT 0,
    verification_code TEXT,
    verification_code_expires_at TEXT,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username COLLATE NOCASE);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email COLLATE NOCASE);
```

### Table Summary

| Table | Purpose | Important Columns | Constraints & Indexes |
| :--- | :--- | :--- | :--- |
| `users` | Stores operator accounts, BCrypt password hashes, and email verification states | `id`, `username`, `email`, `password_hash`, `is_verified`, `verification_code`, `verification_code_expires_at`, `created_at` | `id` (PK, Autoincrement), `username` (UNIQUE, NOCASE, Index), `email` (UNIQUE, NOCASE, Index) |

### Seeding & Demo Accounts
`UserDatabase.seedDemoAccounts()` automatically checks for and seeds 5 pre-verified operator accounts on database creation:

| Username | Supported Alias | Email | Default Security Access Key | Verified Status |
| :--- | :--- | :--- | :--- | :--- |
| `Omen-X(Member1)` | `member1` | `member1@omenx.local` | `SkibidiSahur@67` | Pre-verified (`1`) |
| `Omen-X(Member2)` | `member2` | `member2@omenx.local` | `mPhg@69` | Pre-verified (`1`) |
| `Omen-X(Member3)` | `member3` | `member3@omenx.local` | `Usersayshello` | Pre-verified (`1`) |
| `Omen-X(Member4)` | `member4` | `member4@omenx.local` | `iwannabehackertoo` | Pre-verified (`1`) |
| `Omen-X(Member5)` | `member5` | `member5@omenx.local` | `tspmofr@911` | Pre-verified (`1`) |

### Invariants
- Usernames and email addresses are matched case-insensitively (`COLLATE NOCASE`).
- Passwords require a minimum length of 8 characters and are stored strictly as BCrypt hashes.
- Verification codes are 6-digit numeric strings valid for 15 minutes (`ISO_LOCAL_DATE_TIME` format).
- In-memory scan logs (`ScanRecord`) track module name, target string, hit count, and scan time (`HH:mm`).

## Setup

1. **Prerequisites**:
   - **Java Development Kit (JDK)**: JDK 25 or later.
   - **Apache Maven**: Version 3.8 or later.
   - **Node.js & npm** *(Optional, required only for the OSIRIS 3D subsystem)*: Node.js 18+ with npm.
   - **Git** *(Optional, for OSIRIS repository management; pure Java JGit engine is bundled as fallback)*.

2. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd "Omen-X Main"
   ```

3. **Configure Environment and API Credentials**:
   Copy the configuration template:
   ```bash
   cp src/main/resources/config.properties.example src/main/resources/config.properties
   ```
   Fill in credentials in `src/main/resources/config.properties`:

   | Variable | Description | Requirement | Default |
   | :--- | :--- | :--- | :--- |
   | `QUANTICDATA_API_KEY` | QuanticData API Bearer token for Google Lens reverse visual search | Optional | Empty |
   | `abuseipdb_api_key` | AbuseIPDB v2 REST API key for IP abuse confidence scoring | Optional | Empty |
   | `ABSTRACT_PHONE_API_KEY` | Abstract API key for extended carrier validation | Optional | Empty |
   | `virustotal_api_key` | VirusTotal v3 REST API key for file hash scanning | Optional | Empty |
   | `smtp.host` | SMTP server host for dispatching verification emails | Optional | Empty |
   | `smtp.port` | SMTP server port (e.g. 587 for STARTTLS, 465 for SSL) | Optional | `587` |
   | `smtp.username` | SMTP authentication username | Optional | Empty |
   | `smtp.password` | SMTP authentication password | Optional | Empty |
   | `smtp.from.email` | From address on verification emails | Optional | `noreply@omenx.local` |

4. **Initialize Database**:
   The SQLite database (`omenx.db`) and demo accounts are created automatically on the first run of the application or test suite. No manual migration command is required.

5. **Build and Run**:
   Compile the Java codebase:
   ```bash
   mvn clean compile
   ```
   Launch the JavaFX desktop application:
   ```bash
   mvn javafx:run
   ```

6. **Log In**:
   Use any seeded demo account to sign in:
   - **Operator Identifier**: `Omen-X(Member1)` (or click the quick-select chip **M1**)
   - **Security Access Key**: `SkibidiSahur@67`

## Scripts

| Command | Purpose | When to Use |
| :--- | :--- | :--- |
| `mvn clean compile` | Compiles Java sources and copies resources into `target/classes` | After modifying source files or configuration templates |
| `mvn javafx:run` | Launches the JavaFX application entrypoint (`com.omenx.backend.Main`) | For standard local application development and execution |
| `mvn test` | Runs the JUnit Jupiter test suite via Maven Surefire | Before pushing code or testing database authentication logic |
| `java -cp target/classes;... com.omenx.backend.Main` | Runs compiled application directly from classes | For running without Maven wrapper or plugin |
| `java -cp target/classes;... com.omenx.backend.tools.TestScanner` | Runs CLI diagnostic tool for `UsernameScanner` | To quickly test username recon against platforms from the terminal |

## App Map

### UI Screens & Navigation

| Screen | Controller / View | Description | Access |
| :--- | :--- | :--- | :--- |
| **Operator Login** | `LoginUI.java` | Particle background canvas, operator identifier input, profile chips (M1–M5), security key entry | Public |
| **Operator Enrollment** | `LoginUI.java` (`showSignupCard`) | Account registration (callsign, email, access key validation) | Public |
| **Email Verification** | `LoginUI.java` (`showVerifyCard`) | 6-digit numeric verification code entry with countdown and resend button | Public |
| **Command Center** | `DashboardUI.java` | Main dashboard with metrics, 9-tool categorized launcher grid, capability search, and activity log | Authenticated |
| **Username Recognition** | `Main.java` (`createScanView`) | Query username across 6 platforms with live status, hit counts, and target profile links | Authenticated |
| **Email Exposure** | `Main.java` (`createScanView`) | Validate email and query XposedOrNot breach records with breach tags | Authenticated |
| **Domain & DNS Intel** | `Main.java` (`createScanView`) | DNS records (A, MX, NS, TXT, CNAME), HTTP/HTTPS, SSL certificate inspection, WHOIS | Authenticated |
| **IP Geolocation** | `IPAddressUI.java` | IP geolocation lookup, ASN routing, AbuseIPDB confidence score, OpenStreetMap link | Authenticated |
| **Phone Intel** | `Main.java` (`createScanView`) | Number parsing, E.164 formatting, carrier resolution, geographic description | Authenticated |
| **EXIF Metadata** | `ImageMetadataUI.java` | Drag-and-drop image metadata extraction, optical profile, GPS coordinates, Google Maps link | Authenticated |
| **Reverse Image Search** | `ReverseImageUI.java` | Image upload, QuanticData Google Lens visual matches, quick search engine redirects | Authenticated |
| **Malware Threat Analysis** | `MalwareAnalysisUI.java` | File hashes (MD5, SHA-1, SHA-256, SHA-512), VirusTotal reputation, engine breakdowns | Authenticated |
| **OSIRIS Platform Launcher** | `OsirisLauncherUI.java` | OSIRIS 3D installer, telemetry log stream, server launch/stop, and credentials modal | Authenticated |

### Key User Flows

1. **Authentication Flow**:
   - Operator enters identifier (`Omen-X(Member1)` or email) and security access key.
   - If verified, session initializes and opens `DashboardUI`.
   - If unverified, redirects to 6-digit verification code screen.
2. **Operator Enrollment Flow**:
   - Operator fills callsign, email, and password on signup card.
   - System checks uniqueness and password policy, creates unverified user in SQLite.
   - System generates 6-digit code (15-min expiry) and dispatches email via SMTP.
   - Operator enters code on verification card; system marks `is_verified = 1`.
3. **Investigation Workflow**:
   - Operator selects a tool card from the Dashboard launcher or sidebar rail.
   - Operator inputs target (username, email, domain, IP, phone, image, or binary).
   - Scanner executes asynchronously on background threads; UI updates stats and results.
   - Investigation is automatically appended to session history in `ScanDatabase`.
4. **OSIRIS 3D Platform Launch Flow**:
   - Operator clicks `OSIRIS Platform` in sidebar or launcher grid.
   - Operator clicks `Launch OSIRIS`.
   - System validates runtime binaries -> clones repo via Git/JGit -> executes `npm install` -> executes `npm run build` -> binds available port -> polls health check -> launches default browser to `http://localhost:3000`.

### Internal Service Layer

| Service / Scanner | Method | Authorization | Operations |
| :--- | :--- | :--- | :--- |
| `UserDatabase` | `authenticate(identifier, pass)` | Public | Reads `users` table, verifies BCrypt hash, checks `is_verified` |
| `UserDatabase` | `createUser(username, email, pass, code, expiry)` | Public | Inserts row into `users` table with BCrypt hash |
| `UserDatabase` | `verifyCode(identifier, code)` | Public | Validates 6-digit code against expiration, updates `is_verified = 1` |
| `EmailService` | `sendVerificationCode(email, user, code)` | Internal | Sends HTML verification template via Jakarta Mail SMTP |
| `UsernameScanner` | `scan(username)` | Authenticated | Sends HTTP GET requests to 6 platforms |
| `EmailScanner` | `scan(email)` | Authenticated | Queries XposedOrNot REST API for breach data |
| `DomainScanner` | `scan(domain)` | Authenticated | JNDI DNS query, HTTPS SSL handshake, port 43 TCP WHOIS |
| `IPScanner` | `scan(ip)` | Authenticated | Queries IP-API JSON and AbuseIPDB v2 REST API |
| `PhoneScanner` | `scan(number, defaultRegion)` | Authenticated | Google `libphonenumber` parsing + Abstract API lookup |
| `ImageMetadataScanner` | `scan(imageFile)` | Authenticated | Reads EXIF, IPTC, and GPS directory headers |
| `ReverseImageScanner` | `scan(imageFile)` | Authenticated | Uploads to `imgdb.io` and queries QuanticData Google Lens API |
| `MalwareScanner` | `analyzeFile(filePath)` | Authenticated | Computes MD5/SHA hashes, queries VirusTotal v3 files endpoint |
| `ScanService` | `saveScan(type, target, found, time)` | Authenticated | Appends record to `ScanDatabase` |
| `OsirisProcessManager` | `launch(listener)` | Authenticated | Coordinates setup, port resolution, process spawning, health polling |

## Configuration

Configuration values are resolved first from System Environment Variables, falling back to `src/main/resources/config.properties`.

### Environment Variables & Properties

| Property Name | Environment Variable | Purpose | Default |
| :--- | :--- | :--- | :--- |
| `QUANTICDATA_API_KEY` | `QUANTICDATA_API_KEY` | Bearer token for QuanticData Google Lens API | None |
| `abuseipdb_api_key` | `ABUSEIPDB_API_KEY` | API Key for AbuseIPDB IP reputation queries | None |
| `ABSTRACT_PHONE_API_KEY` | `ABSTRACT_PHONE_API_KEY` | API Key for Abstract Phone Validation API | None |
| `virustotal_api_key` | `VIRUSTOTAL_API_KEY` | API Key for VirusTotal v3 REST API | None |
| `smtp.host` | `SMTP_HOST` | SMTP server host address | None |
| `smtp.port` | `SMTP_PORT` | SMTP server port | `587` |
| `smtp.username` | `SMTP_USERNAME` | SMTP account username | None |
| `smtp.password` | `SMTP_PASSWORD` | SMTP account password | None |
| `smtp.from.email` | `SMTP_FROM_EMAIL` | Sender address on verification emails | `noreply@omenx.local` |

### OSIRIS Subsystem Configuration
Stored in `~/.omenx/config/osiris.properties` and mirrored to `~/.omenx/tools/osiris/app/.env.local`:
- `port`: Web server port (Default: `3000`).
- `opensky_client_id` / `opensky_client_secret`: OpenSky Network live flight tracking credentials.
- `n2yo_api_key`: N2YO satellite orbit and pass tracking API key.
- `firms_api_key`: NASA FIRMS global thermal anomalies API key.
- `ais_api_key`: AIS vessel tracking API key.
- `custom_node_path`: Custom path to Node.js executable if not on system PATH.
- `custom_git_path`: Custom path to Git executable if not on system PATH.

## Testing

The test suite is built on JUnit Jupiter 5 (`org.junit.jupiter:junit-jupiter:5.11.4`).

### Running Tests
Execute the test suite using Maven:
```bash
mvn test
```

### Test Coverage
Located in `src/test/java/com/omenx/database/UserAuthTest.java`:
- **Demo Account Seeding**: Verifies 5 demo accounts (`Member1` through `Member5`) are auto-created, pre-verified, and password hashes use BCrypt formatting.
- **Authentication**: Verifies login with exact callsigns, case-insensitive usernames, shorthand aliases (`member1`), and incorrect password rejection.
- **Password Utility**: Tests minimum 8-character length validation, empty password rejection, and BCrypt verification.
- **Registration & Verification Lifecycle**: Tests user creation, unverified login restriction, invalid 6-digit code rejection, code expiration handling, code renewal, and successful verification unlock.
- **Constraint Enforcement**: Tests duplicate username and duplicate email rejection via SQLite unique constraints.
- **Email Service Handling**: Tests empty and null recipient validation in `EmailService`.

### Test Database Isolation
Tests instantiate `UserDatabase` using an isolated temporary SQLite database file created via `File.createTempFile("omenx_test_", ".db")` in the `@BeforeEach` fixture and wiped in `@AfterEach`.

## Deployment

OMEN-X is a cross-platform desktop application designed to run locally on Windows, macOS, and Linux workstations.

### Building the Project
To compile classes and bundle resources:
```bash
mvn clean package -DskipTests
```

### Running in Production
Run the application using the Maven JavaFX plugin:
```bash
mvn javafx:run
```

Or execute directly with a Java 25 runtime:
```bash
java -cp "target/classes;target/dependency/*" com.omenx.backend.Main
```

### Host Requirements
- **OS**: Windows 10/11, macOS 12+, or modern Linux distribution.
- **Java**: JDK 25.
- **Node.js (for OSIRIS)**: Node.js 18+ and npm installed and accessible in the user PATH (or configured via the OSIRIS Settings dialog).
- **Outbound Network Ports**:
  - `80/tcp`, `443/tcp`: HTTP/HTTPS API intelligence queries (IP-API, AbuseIPDB, VirusTotal, XposedOrNot, imgdb.io, QuanticData).
  - `53/udp`, `53/tcp`: DNS resolution.
  - `43/tcp`: Direct WHOIS queries against registry servers.
  - `587/tcp` or `465/tcp`: Outbound SMTP for account verification delivery.
  - `3000/tcp` (or next free port): Local loopback binding for the OSIRIS Next.js subsystem.

## Conventions

- **Code Organization**:
  - `com.omenx.backend.osint`: Standalone scanner classes with pure Java logic and HTTP clients.
  - `com.omenx.backend.osiris`: Subsystem supervisor, runtime discovery, and process lifecycle managers.
  - `com.omenx.backend.service`: Business services (authentication helpers, reporting, scan logging).
  - `com.omenx.database`: SQLite JDBC DAO layer and model records.
  - `com.omenx.frontend`: JavaFX UI views, controls, and components.
- **Threading Model**: All network calls, file hashing, and database operations must run on background threads (`ExecutorService` / `Thread`). UI mutations must always be routed through `Platform.runLater()`.
- **Styling**: Component styles and glassmorphic aesthetics are defined in `src/main/resources/styles/app.css` using standard CSS classes (`glass-panel`, `btn-primary`, `btn-ghost`, `badge`, `cyber-input`).

## Troubleshooting

| Issue | Root Cause | Resolution |
| :--- | :--- | :--- |
| **`Node.js runtime not found` in OSIRIS Launcher** | Node.js is not installed or not present in system `PATH`. | Install Node.js 18+ from [nodejs.org](https://nodejs.org/), or open **⚙ API Keys & Port** in the OSIRIS launcher and configure the exact path to `node.exe`. |
| **`npm not found after searching all known paths`** | npm binary is missing from PATH or installed in a non-standard location. | Ensure Node.js was installed with npm included, or verify that `npm.cmd` exists alongside `node.exe`. |
| **`Destination locked by background process` during OSIRIS clone** | A previously running Node.js or Git process holds open file handles in `~/.omenx/tools/osiris/app`. | Click **↻ Reinstall / Reset** in the OSIRIS launcher, which automatically terminates orphan child processes (`taskkill /F /IM node.exe`) and cleans file locks before re-cloning. |
| **`API key not configured` during IP, Malware, or Image Scans** | Required third-party API keys are missing from `config.properties`. | Add keys to `src/main/resources/config.properties` (`abuseipdb_api_key`, `virustotal_api_key`, `QUANTICDATA_API_KEY`, `ABSTRACT_PHONE_API_KEY`). |
| **Verification email not received on operator signup** | SMTP credentials are not configured in `config.properties` or environment variables. | Either configure `smtp.host`, `smtp.port`, `smtp.username`, and `smtp.password` in `config.properties`, or inspect standard output in the terminal where the 6-digit verification code is logged during development. |
| **`Invalid operator identifier or security key` on login** | Incorrect credentials or account not verified. | Use a seeded demo profile like `Omen-X(Member1)` with password `SkibidiSahur@67`, or complete the 6-digit email verification step for newly created accounts. |
| **`Address already in use` error when launching OSIRIS** | Port 3000 is occupied by another local service. | `OsirisProcessManager` automatically searches ports 3001–3030 if port 3000 is busy. Alternatively, set a custom port in the OSIRIS Settings modal. |
