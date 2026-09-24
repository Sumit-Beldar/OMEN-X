package com.omenx.database;

import com.omenx.backend.service.EmailService;
import com.omenx.backend.service.PasswordUtil;
import com.omenx.database.model.UserRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserAuthTest {

    private File tempDbFile;
    private UserDatabase userDatabase;
    private EmailService emailService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("omenx_test_", ".db");
        tempDbFile.deleteOnExit();
        userDatabase = new UserDatabase(tempDbFile.getAbsolutePath());
        emailService = new EmailService();
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testDemoAccountsSeededAndPreVerified() {
        for (int i = 1; i <= 5; i++) {
            String username = "Omen-X(Member" + i + ")";
            Optional<UserRecord> userOpt = userDatabase.findByUsername(username);
            assertTrue(userOpt.isPresent(), "Demo user " + username + " should be seeded");
            UserRecord user = userOpt.get();
            assertTrue(user.isVerified(), "Demo user should be pre-verified");
            assertNotNull(user.getPasswordHash(), "Password hash must not be null");
            assertFalse(user.getPasswordHash().startsWith("Skibidi"), "Password must be BCrypt hashed, not plaintext");
        }
    }

    @Test
    public void testDemoAccountAuthentication() {
        UserDatabase.AuthResult res1 = userDatabase.authenticate("Omen-X(Member1)", "SkibidiSahur@67");
        assertEquals(UserDatabase.AuthStatus.SUCCESS, res1.status());
        assertNotNull(res1.user());
        assertEquals("Omen-X(Member1)", res1.user().getUsername());

        UserDatabase.AuthResult aliasRes = userDatabase.authenticate("member1", "SkibidiSahur@67");
        assertEquals(UserDatabase.AuthStatus.SUCCESS, aliasRes.status());

        UserDatabase.AuthResult badPass = userDatabase.authenticate("Omen-X(Member1)", "WrongPassword123");
        assertEquals(UserDatabase.AuthStatus.INVALID_CREDENTIALS, badPass.status());
    }

    @Test
    public void testPasswordValidationAndHashing() {
        assertNotNull(PasswordUtil.validatePassword("short"), "Password under 8 chars must be invalid");
        assertNotNull(PasswordUtil.validatePassword(""), "Empty password must be invalid");
        assertNull(PasswordUtil.validatePassword("SecurePass123!"), "Valid password should return null error");

        String plain = "MySecretPass@2026";
        String hashed = PasswordUtil.hashPassword(plain);
        assertNotEquals(plain, hashed);
        assertTrue(PasswordUtil.checkPassword(plain, hashed));
        assertFalse(PasswordUtil.checkPassword("WrongPass", hashed));
    }

    @Test
    public void testSignupAndVerificationFlow() throws SQLException {
        String username = "InvestigatorAlpha";
        String email = "alpha@recon.agency";
        String password = "TacticalPassword@99";
        String code = "584920";
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        UserRecord created = userDatabase.createUser(username, email, password, code, expiry);
        assertNotNull(created);
        assertFalse(created.isVerified(), "Newly registered user must be unverified");
        assertEquals(username, created.getUsername());
        assertEquals(email, created.getEmail());

        assertTrue(userDatabase.existsByUsername(username));
        assertTrue(userDatabase.existsByEmail(email));

        UserDatabase.AuthResult unverifiedAuth = userDatabase.authenticate(username, password);
        assertEquals(UserDatabase.AuthStatus.UNVERIFIED, unverifiedAuth.status(), "Unverified login must be blocked");

        UserDatabase.VerificationResult wrongCodeRes = userDatabase.verifyCode(email, "000000");
        assertEquals(UserDatabase.VerificationStatus.INVALID_CODE, wrongCodeRes.status());

        userDatabase.updateVerificationCode(email, "999999", LocalDateTime.now().minusMinutes(5));
        UserDatabase.VerificationResult expiredRes = userDatabase.verifyCode(email, "999999");
        assertEquals(UserDatabase.VerificationStatus.EXPIRED, expiredRes.status());

        String newCode = "729143";
        boolean resendOk = userDatabase.updateVerificationCode(email, newCode, LocalDateTime.now().plusMinutes(15));
        assertTrue(resendOk);

        UserDatabase.VerificationResult successRes = userDatabase.verifyCode(email, newCode);
        assertEquals(UserDatabase.VerificationStatus.SUCCESS, successRes.status());
        assertTrue(successRes.user().isVerified());

        UserDatabase.AuthResult verifiedAuth = userDatabase.authenticate(username, password);
        assertEquals(UserDatabase.AuthStatus.SUCCESS, verifiedAuth.status());
        assertTrue(verifiedAuth.user().isVerified());
    }

    @Test
    public void testDuplicateUsernamesAndEmails() throws SQLException {
        userDatabase.createUser("UniqueUserA", "unique_a@agency.local", "Password123!", "111111", LocalDateTime.now().plusMinutes(10));
        assertThrows(SQLException.class, () -> {
            userDatabase.createUser("UniqueUserA", "different_email@agency.local", "Password123!", "222222", LocalDateTime.now().plusMinutes(10));
        });
        assertThrows(SQLException.class, () -> {
            userDatabase.createUser("DifferentUser", "unique_a@agency.local", "Password123!", "333333", LocalDateTime.now().plusMinutes(10));
        });
    }

    @Test
    public void testEmailServiceValidationAndHandling() {
        EmailService.SendResult emptyEmailRes = emailService.sendVerificationCode("", "TestUser", "123456");
        assertNotNull(emptyEmailRes);
        assertFalse(emptyEmailRes.success());
        assertNotNull(emptyEmailRes.errorMessage());

        EmailService.SendResult nullEmailRes = emailService.sendVerificationCode(null, "TestUser", "123456");
        assertNotNull(nullEmailRes);
        assertFalse(nullEmailRes.success());
        assertNotNull(nullEmailRes.errorMessage());
    }
}
