package com.omenx.database;

import com.omenx.backend.service.PasswordUtil;
import com.omenx.database.model.UserRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class UserDatabase {

    private static final String DEFAULT_DB_PATH = "omenx.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final String dbUrl;

    public enum VerificationStatus {
        SUCCESS,
        INVALID_CODE,
        EXPIRED,
        USER_NOT_FOUND,
        ALREADY_VERIFIED
    }

    public enum AuthStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        UNVERIFIED,
        USER_NOT_FOUND
    }

    public record AuthResult(AuthStatus status, UserRecord user, String message) {}
    public record VerificationResult(VerificationStatus status, UserRecord user, String message) {}

    public UserDatabase() {
        this(DEFAULT_DB_PATH);
    }

    public UserDatabase(String dbFilePath) {
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
        initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initializeDatabase() {
        String createTableSql = """
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
        """;
        String createUsernameIdx = "CREATE INDEX IF NOT EXISTS idx_users_username ON users (username COLLATE NOCASE);";
        String createEmailIdx = "CREATE INDEX IF NOT EXISTS idx_users_email ON users (email COLLATE NOCASE);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createUsernameIdx);
            stmt.execute(createEmailIdx);
            seedDemoAccounts(conn);
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error initializing database: " + ex.getMessage());
        }
    }

    private void seedDemoAccounts(Connection conn) {
        String[][] demoUsers = {
            {"Omen-X(Member1)", "member1@omenx.local", "SkibidiSahur@67"},
            {"Omen-X(Member2)", "member2@omenx.local", "mPhg@69"},
            {"Omen-X(Member3)", "member3@omenx.local", "Usersayshello"},
            {"Omen-X(Member4)", "member4@omenx.local", "iwannabehackertoo"},
            {"Omen-X(Member5)", "member5@omenx.local", "tspmofr@911"}
        };

        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ? COLLATE NOCASE";
        String insertSql = """
            INSERT INTO users (username, email, password_hash, is_verified, verification_code, verification_code_expires_at, created_at)
            VALUES (?, ?, ?, 1, NULL, NULL, ?)
        """;

        for (String[] demo : demoUsers) {
            String username = demo[0];
            String email = demo[1];
            String plainPass = demo[2];

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String hashed = PasswordUtil.hashPassword(plainPass);
                        String now = LocalDateTime.now().format(DATE_FORMATTER);

                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, username);
                            insertStmt.setString(2, email);
                            insertStmt.setString(3, hashed);
                            insertStmt.setString(4, now);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("[UserDatabase] Error seeding account " + username + ": " + ex.getMessage());
            }
        }
    }

    public synchronized UserRecord createUser(
            String username,
            String email,
            String plainPassword,
            String verificationCode,
            LocalDateTime expiresAt
    ) throws SQLException {
        String passwordHash = PasswordUtil.hashPassword(plainPassword);
        String createdAt = LocalDateTime.now().format(DATE_FORMATTER);
        String expiresAtStr = expiresAt != null ? expiresAt.format(DATE_FORMATTER) : null;

        String insertSql = """
            INSERT INTO users (username, email, password_hash, is_verified, verification_code, verification_code_expires_at, created_at)
            VALUES (?, ?, ?, 0, ?, ?, ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username.trim());
            stmt.setString(2, email.trim().toLowerCase());
            stmt.setString(3, passwordHash);
            stmt.setString(4, verificationCode != null ? verificationCode.trim() : null);
            stmt.setString(5, expiresAtStr);
            stmt.setString(6, createdAt);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            int generatedId = 0;
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                }
            }

            return new UserRecord(
                    generatedId,
                    username.trim(),
                    email.trim().toLowerCase(),
                    passwordHash,
                    false,
                    verificationCode,
                    expiresAt,
                    LocalDateTime.now()
            );
        }
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error checking username: " + ex.getMessage());
            return false;
        }
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? COLLATE NOCASE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error checking email: " + ex.getMessage());
            return false;
        }
    }

    public Optional<UserRecord> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error finding user by username: " + ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<UserRecord> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return Optional.empty();
        String sql = "SELECT * FROM users WHERE email = ? COLLATE NOCASE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error finding user by email: " + ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<UserRecord> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return Optional.empty();
        String clean = identifier.trim();

        if (clean.matches("(?i)member[1-5]")) {
            String memberNum = clean.replaceAll("[^0-9]", "");
            clean = "Omen-X(Member" + memberNum + ")";
        }

        String sql = "SELECT * FROM users WHERE username = ? COLLATE NOCASE OR email = ? COLLATE NOCASE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, clean);
            stmt.setString(2, clean.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error finding user by identifier: " + ex.getMessage());
        }
        return Optional.empty();
    }

    public synchronized boolean updateVerificationCode(String emailOrUsername, String newCode, LocalDateTime newExpiry) {
        Optional<UserRecord> userOpt = findByIdentifier(emailOrUsername);
        if (userOpt.isEmpty()) return false;

        String expiryStr = newExpiry != null ? newExpiry.format(DATE_FORMATTER) : null;
        String sql = "UPDATE users SET verification_code = ?, verification_code_expires_at = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newCode.trim());
            stmt.setString(2, expiryStr);
            stmt.setInt(3, userOpt.get().getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error updating verification code: " + ex.getMessage());
            return false;
        }
    }

    public synchronized VerificationResult verifyCode(String identifier, String code) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return new VerificationResult(VerificationStatus.USER_NOT_FOUND, null, "Operator identifier is required.");
        }
        if (code == null || code.trim().isEmpty()) {
            return new VerificationResult(VerificationStatus.INVALID_CODE, null, "Verification code is required.");
        }

        Optional<UserRecord> userOpt = findByIdentifier(identifier);
        if (userOpt.isEmpty()) {
            return new VerificationResult(VerificationStatus.USER_NOT_FOUND, null, "Operator account not found.");
        }

        UserRecord user = userOpt.get();
        if (user.isVerified()) {
            return new VerificationResult(VerificationStatus.ALREADY_VERIFIED, user, "Account is already verified.");
        }

        String storedCode = user.getVerificationCode();
        if (storedCode == null || !storedCode.trim().equalsIgnoreCase(code.trim())) {
            return new VerificationResult(VerificationStatus.INVALID_CODE, user, "Invalid verification code. Please check and retry.");
        }

        LocalDateTime expiresAt = user.getVerificationCodeExpiresAt();
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return new VerificationResult(VerificationStatus.EXPIRED, user, "Verification code has expired. Please request a new code.");
        }

        String sql = "UPDATE users SET is_verified = 1, verification_code = NULL, verification_code_expires_at = NULL WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.executeUpdate();

            UserRecord verifiedUser = new UserRecord(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    true,
                    null,
                    null,
                    user.getCreatedAt()
            );
            return new VerificationResult(VerificationStatus.SUCCESS, verifiedUser, "Account verified successfully! You can now log in.");
        } catch (SQLException ex) {
            System.err.println("[UserDatabase] Error verifying user: " + ex.getMessage());
            return new VerificationResult(VerificationStatus.INVALID_CODE, user, "Database error verifying account.");
        }
    }

    public AuthResult authenticate(String identifier, String plainPassword) {
        if (identifier == null || identifier.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null, "Operator ID & Security Key required.");
        }

        Optional<UserRecord> userOpt = findByIdentifier(identifier);
        if (userOpt.isEmpty()) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null, "ACCESS DENIED: Invalid operator identifier or security key.");
        }

        UserRecord user = userOpt.get();
        boolean passwordMatches = PasswordUtil.checkPassword(plainPassword, user.getPasswordHash());

        if (!passwordMatches) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null, "ACCESS DENIED: Invalid operator identifier or security key.");
        }

        if (!user.isVerified()) {
            return new AuthResult(AuthStatus.UNVERIFIED, user, "ACCESS RESTRICTED: Account email is unverified. Please verify your 6-digit code first.");
        }

        return new AuthResult(AuthStatus.SUCCESS, user, "AUTHENTICATION GRANTED");
    }

    private UserRecord mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        boolean isVerified = rs.getInt("is_verified") == 1;
        String verificationCode = rs.getString("verification_code");
        String expiresAtStr = rs.getString("verification_code_expires_at");
        String createdAtStr = rs.getString("created_at");

        LocalDateTime expiresAt = null;
        if (expiresAtStr != null && !expiresAtStr.isBlank()) {
            try {
                expiresAt = LocalDateTime.parse(expiresAtStr, DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {}
        }

        LocalDateTime createdAt = LocalDateTime.now();
        if (createdAtStr != null && !createdAtStr.isBlank()) {
            try {
                createdAt = LocalDateTime.parse(createdAtStr, DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {}
        }

        return new UserRecord(
                id,
                username,
                email,
                passwordHash,
                isVerified,
                verificationCode,
                expiresAt,
                createdAt
        );
    }
}
