package com.omenx.database.model;

import java.time.LocalDateTime;

public class UserRecord {

    private final int id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean isVerified;
    private final String verificationCode;
    private final LocalDateTime verificationCodeExpiresAt;
    private final LocalDateTime createdAt;

    public UserRecord(
            int id,
            String username,
            String email,
            String passwordHash,
            boolean isVerified,
            String verificationCode,
            LocalDateTime verificationCodeExpiresAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isVerified = isVerified;
        this.verificationCode = verificationCode;
        this.verificationCodeExpiresAt = verificationCodeExpiresAt;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
