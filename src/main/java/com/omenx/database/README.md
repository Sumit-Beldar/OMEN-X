# Database Package (com.omenx.database)

This package contains data persistence structures and session history management for OMEN-X.

## Current Implementation
- **ScanDatabase.java**: In-memory session database storing scan history records for active investigations.
- **model/ScanRecord.java**: Domain data entity capturing scan type, target, hits found, and timestamp.

## Future Persistence Extensions
This package is reserved for future database and persistent storage integrations, such as:
- Local relational storage (e.g. SQLite via JDBC)
- Target repository DAO patterns and entity mappings
- Investigation case management and schema migration scripts
