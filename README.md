# 🕵️ OMEN-X

## Open Source Intelligence & Security Investigation Platform

OMEN-X is a fully developed desktop-based **Open-Source Intelligence (OSINT) and security investigation platform** designed to bring multiple reconnaissance, information-gathering, and analysis capabilities together in a single application.

It provides a centralized dashboard through which users can perform different types of investigations, analyze information, inspect images, and maintain a structured investigation workflow.

OMEN-X is designed for **cybersecurity students, security researchers, investigators, and OSINT learners** working with publicly available information.

---

## 🚀 Features

OMEN-X provides **8 integrated investigation modules** accessible through the main dashboard.

### 1. 👤 Username Investigation

The Username Investigation module allows users to investigate usernames across supported online platforms.

**How it works**
1. Enter the username to investigate.
2. OMEN-X performs the supported username searches.
3. Potentially associated profiles are identified.
4. Results are displayed in the investigation interface.

**Capabilities**
- Username searching
- Profile discovery
- Platform-based investigation
- Result presentation
- Investigation status

### 2. 📧 Email Investigation

The Email Investigation module performs OSINT checks related to an email address.

**How it works**
1. Enter an email address.
2. OMEN-X performs the supported email-related checks.
3. Available information is processed.
4. Results are displayed in a structured format.

**Capabilities**
- Email analysis
- Basic email OSINT checks
- Investigation results
- Structured information display

### 3. 🌐 Domain Investigation

The Domain Investigation module provides technical and OSINT information about a domain.

**Information investigated**
- DNS information
- HTTP status
- HTTPS status
- SSL certificate information
- WHOIS information
- Common subdomain discovery
- Domain-related technical information

**How it works**
1. Enter the target domain.
2. OMEN-X performs the supported domain checks.
3. The collected information is processed.
4. Results are displayed through the domain investigation interface.

### 4. 🌍 IP Address Investigation

The IP Investigation module analyzes an IP address and retrieves available network-related information.

**Information may include**
- IP address information
- Network information
- ISP / organization information
- Available geolocation information
- Additional investigation data

**How it works**
1. Enter an IP address.
2. OMEN-X performs the supported IP investigation.
3. Available information is collected.
4. Results are presented in a structured interface.

### 5. 📱 Phone Number Investigation

The Phone Number Investigation module performs OSINT checks related to phone numbers.

**How it works**
1. Enter the phone number.
2. OMEN-X performs the supported phone-number checks.
3. Available information is processed.
4. Results are displayed through the investigation interface.

**Capabilities**
- Phone number analysis
- Basic OSINT checks
- Carrier-related information where available
- Structured result presentation

### 6. 🖼️ Image Metadata Investigation

The Image Metadata module allows users to select an image and extract available metadata from the file.

The module uses metadata extraction capabilities to inspect information embedded within supported image files.

**Metadata**

OMEN-X can extract available information such as:

- File name
- File extension
- File size
- MIME type
- File signature
- Image dimensions
- Camera manufacturer
- Camera model
- Software used
- Date taken
- Original date
- Exposure
- F-number
- ISO
- GPS latitude
- GPS longitude

**How it works**
1. Select an image.
2. OMEN-X reads the image file.
3. Available metadata is extracted.
4. The information is displayed in the Image Metadata interface.

**🔍 GPS Analysis**

If GPS metadata exists inside the image, OMEN-X can extract:

```text
Latitude
Longitude
```

The availability of GPS information depends on whether the original image contains GPS metadata.

### 7. 🖼️ Reverse Image Investigation

The Reverse Image Investigation module provides an interface for image-based OSINT investigation.

**How it works**
1. Select or provide an image.
2. OMEN-X processes the image.
3. Supported reverse-image investigation methods are performed.
4. Results are presented through the Reverse Image interface.

The module is designed to assist with investigating potential image sources and related information.

### 8. 🔍 Investigation & Scan Management

The Investigation and Scan Management module provides the foundation for organizing investigation activity across OMEN-X.

It allows the platform to maintain structured information about investigations and scans performed through the application.

**Capabilities**
- Investigation records
- Scan records
- Search activity
- Finding tracking
- Investigation status
- Structured investigation information

The scan management system also supports the dashboard statistics used to provide an overview of investigation activity.

---

## 📊 Dashboard

The OMEN-X Dashboard is the central interface of the application.

It provides access to all investigation modules from a single location.

**Dashboard features**
- Centralized module access
- Investigation module cards
- Module status
- Investigation navigation
- Scan statistics
- Findings statistics
- Investigation activity
- Clean graphical interface
- Modular navigation

The dashboard provides a single workspace for accessing and managing the different investigation capabilities of OMEN-X.

---

## 🔐 Login System

OMEN-X includes a dedicated login interface that acts as the entry point to the application.

The login page provides:
- User identification
- Password input
- Login functionality
- Application access control
- Clean and dedicated authentication interface

After successful login, the user is taken to the OMEN-X investigation dashboard.

---

## 📈 Scan & Finding Statistics

OMEN-X maintains investigation statistics through its scan management system.

The dashboard provides information such as:
- Total Scans
- Total Findings
- Investigation Activity

These statistics provide users with an overview of investigation activity performed within the application.

---

## 🧩 Modular Architecture

OMEN-X uses a modular Java architecture where investigation functionality and UI components are separated into dedicated classes.

This makes the application easier to:
- Maintain
- Modify
- Debug
- Extend
- Test
- Develop collaboratively

A simplified project structure is:

```text
OMEN-X
│
├── pom.xml
├── README.md
│
└── src
    └── main
        ├── java
        │   └── com
        │       └── omenx
        │           │
        │           ├── Main.java
        │           ├── DomainScanner.java
        │           ├── EmailScanner.java
        │           ├── IPScanner.java
        │           ├── PhoneScanner.java
        │           ├── MalwareScanner.java
        │           │
        │           ├── database/
        │           ├── model/
        │           ├── osint/
        │           ├── service/
        │           └── ui/
        │               ├── DashboardUI.java
        │               ├── ImageMetadataUI.java
        │               ├── ReverseImageUI.java
        │               ├── InvestigationUI.java
        │               └── ...
        │
        └── resources
            └── css
                ├── omenx.css
                └── hud.css
```

The project structure may contain additional classes and components depending on the current version.

---

## 🛠️ Technology Stack

OMEN-X is built using:

- **Java** (JDK 25)
- **JavaFX**
- **JavaFX CSS**
- **Apache Maven**
- **Metadata Extractor** (com.drewnoakes)
- **Jackson** (JSON processing)
- **Google libphonenumber** (+ carrier & geocoder)
- **Git** / **GitHub**
- **Visual Studio Code** (recommended)

The application uses a modular architecture that allows additional technologies and integrations to be incorporated when required.

---

## 💻 System Requirements

**Required**
- Windows 10 or Windows 11
- Java JDK 25 or compatible version
- Apache Maven 3.9+
- Git

**Recommended**
- Visual Studio Code
- Java Extension Pack for VS Code
- Stable internet connection

---

## 📥 Installation

### 1. Install Java
Install a compatible Java JDK.

Verify the installation:
```bash
java -version
javac -version
```

### 2. Install Apache Maven
Install Apache Maven 3.9 or later.

Verify:
```bash
mvn -version
```

### 3. Install Git
```bash
git --version
```

### 4. Install Visual Studio Code (Recommended)
Install the **Extension Pack for Java** from the VS Code Extensions marketplace.

---

## 📦 Download OMEN-X

Clone the GitHub repository:

```bash
git clone https://github.com/Sumit-Beldar/OMEN-X.git
cd OMEN-X
```

---

## 🔨 Build OMEN-X

```bash
mvn clean compile
```

Maven will automatically download the dependencies defined in the project and compile the source code.

A successful build will end with:
BUILD SUCCESS


---

## ▶️ Run OMEN-X

**Recommended way (JavaFX Maven plugin):**

```bash
mvn javafx:run
```

**Alternatively**, open the project in Visual Studio Code and run:
- `src/main/java/com/omenx/Main.java`

The main application class is:
com.omenx.Main

---

## 💻 Running Through Visual Studio Code

1. Open Visual Studio Code.
2. Select **File → Open Folder**.
3. Select the OMEN-X project directory.
4. Make sure the Java Extension Pack is installed.
5. Allow VS Code to load the Maven project.
6. Open `Main.java`.
7. Select **Run**.

You can also compile first:
```bash
mvn clean compile
```

---

## 🧹 Clean and Rebuild

If you encounter build problems:

```bash
mvn clean
mvn clean compile
```

After a successful compilation, run the application again.

---

## ⚠️ Troubleshooting

**`'java' is not recognized`**
- Make sure Java JDK is installed and configured in the system PATH.
- Verify with `java -version`.

**`'mvn' is not recognized`**
- Make sure Apache Maven is installed and its `bin` directory has been added to the system PATH.
- Verify with `mvn -version`.

**JavaFX does not start**
- Ensure Java JDK is correctly installed.
- Ensure Maven is installed.
- Confirm the project compiled successfully.
- Confirm Maven dependencies were downloaded.
- Verify the JavaFX configuration in `pom.xml`.
- Confirm the correct main class is being executed.

Try:
```bash
mvn clean compile
mvn javafx:run
```

---

## 🔒 Responsible Use

OMEN-X is intended for:
- Cybersecurity education
- OSINT learning
- Security research
- Authorized investigations
- Analysis of publicly available information

Users are responsible for ensuring that their use of OMEN-X complies with applicable laws, regulations, platform policies, and organizational rules.

**OMEN-X should not be used for:**
- Unauthorized access
- Bypassing security controls
- Harassment
- Unauthorized surveillance
- Abuse of third-party services
- Unlawful collection or use of information

Only investigate systems, accounts, domains, IP addresses, phone numbers, images, or other targets when there is a legitimate and authorized purpose.

---

## 🚀 Future Development

Although OMEN-X is fully developed in its current version, the modular architecture allows the project to continue evolving.

Future versions can introduce:
- Additional OSINT modules
- Additional data sources
- API integrations
- Improved investigation analysis
- Additional visualization capabilities
- Enhanced investigation management
- Additional security and usability improvements

The architecture is designed so that new functionality can be integrated without requiring the entire application to be rewritten.

---

## 📄 License

This project is open source. Please check the repository for the specific license details.

---

**Made for cybersecurity education and legitimate OSINT research.**
