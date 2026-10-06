package com.omenx.backend.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    private String smtpHost;
    private int smtpPort = 587;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpFromEmail;

    public record SendResult(boolean success, String errorMessage) {}

    public EmailService() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        smtpHost = System.getenv("SMTP_HOST");
        String portEnv = System.getenv("SMTP_PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                smtpPort = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }
        smtpUsername = System.getenv("SMTP_USERNAME");
        smtpPassword = System.getenv("SMTP_PASSWORD");
        smtpFromEmail = System.getenv("SMTP_FROM_EMAIL");

        try (InputStream is = EmailService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);

                if (smtpHost == null || smtpHost.isBlank()) {
                    smtpHost = props.getProperty("smtp.host");
                }
                if (props.containsKey("smtp.port")) {
                    try {
                        String p = props.getProperty("smtp.port");
                        if (p != null && !p.isBlank()) {
                            smtpPort = Integer.parseInt(p.trim());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (smtpUsername == null || smtpUsername.isBlank()) {
                    smtpUsername = props.getProperty("smtp.username");
                }
                if (smtpPassword == null || smtpPassword.isBlank()) {
                    smtpPassword = props.getProperty("smtp.password");
                }
                if (smtpFromEmail == null || smtpFromEmail.isBlank()) {
                    smtpFromEmail = props.getProperty("smtp.from.email");
                }
            }
        } catch (Exception ex) {
            System.err.println("[EmailService] Note: Unable to load config.properties: " + ex.getMessage());
        }

        if (smtpHost != null) smtpHost = smtpHost.trim();
        if (smtpUsername != null) smtpUsername = smtpUsername.trim();
        if (smtpPassword != null) smtpPassword = smtpPassword.trim();
        if (smtpFromEmail != null) smtpFromEmail = smtpFromEmail.trim();

        if (smtpFromEmail == null || smtpFromEmail.isBlank()) {
            smtpFromEmail = (smtpUsername != null && !smtpUsername.isBlank()) ? smtpUsername : "noreply@omenx.local";
        }
    }

    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isBlank() && !smtpHost.equalsIgnoreCase("smtp.example.com")
                && !smtpHost.equals("YOUR_KEY_HERE") && !smtpHost.startsWith("YOUR_");
    }

    public SendResult sendVerificationCode(String recipientEmail, String username, String code) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new SendResult(false, "Recipient email is required.");
        }

        if (!isConfigured()) {
            String notice = "SMTP is unconfigured (code generated: " + code + "). Configure SMTP in config.properties for automated delivery.";
            return new SendResult(false, notice);
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.connectiontimeout", "7000");
            props.put("mail.smtp.timeout", "7000");

            boolean useAuth = smtpUsername != null && !smtpUsername.isBlank();
            props.put("mail.smtp.auth", String.valueOf(useAuth));

            if (smtpPort == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session;
            if (useAuth) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUsername, smtpPassword);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpFromEmail, "OMEN-X Security Gateway", StandardCharsets.UTF_8.name()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            message.setSubject("[OMEN-X] Operator Verification Code", StandardCharsets.UTF_8.name());

            String htmlBody = buildHtmlEmail(username, code);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            return new SendResult(true, null);

        } catch (Exception ex) {
            String errDetail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.err.println("[EmailService] Error dispatching email to " + recipientEmail + ": " + errDetail);
            return new SendResult(false, "Failed to send verification email (" + errDetail + "). Please verify SMTP settings.");
        }
    }

    private String buildHtmlEmail(String username, String code) {
        String safeUser = (username != null && !username.isBlank()) ? username : "Operator";
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body {
                  margin: 0;
                  padding: 24px;
                  background-color: #04060A;
                  font-family: 'Consolas', 'JetBrains Mono', 'Segoe UI', monospace, sans-serif;
                  color: #F1F5F9;
                }
                .container {
                  max-width: 520px;
                  margin: 0 auto;
                  background: #0B1323;
                  border: 1px solid #22D3EE;
                  border-radius: 12px;
                  padding: 32px;
                  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
                }
                .brand {
                  text-align: center;
                  margin-bottom: 24px;
                }
                .badge {
                  display: inline-block;
                  font-size: 10px;
                  font-weight: bold;
                  letter-spacing: 2px;
                  color: #22D3EE;
                  background: rgba(34, 211, 238, 0.12);
                  border: 1px solid rgba(34, 211, 238, 0.35);
                  border-radius: 4px;
                  padding: 4px 10px;
                  margin-bottom: 8px;
                }
                .logo {
                  font-size: 28px;
                  font-weight: 800;
                  color: #22D3EE;
                  letter-spacing: 4px;
                  margin: 0;
                }
                .subtitle {
                  font-size: 10px;
                  color: #64748B;
                  letter-spacing: 1.5px;
                  margin-top: 4px;
                }
                .divider {
                  height: 1px;
                  background: linear-gradient(to right, transparent, rgba(34, 211, 238, 0.35), transparent);
                  margin: 20px 0;
                }
                .greeting {
                  font-size: 14px;
                  color: #94A3B8;
                  margin-bottom: 16px;
                }
                .content {
                  font-size: 13px;
                  line-height: 1.6;
                  color: #CBD5E1;
                }
                .code-box {
                  text-align: center;
                  background: rgba(4, 6, 10, 0.85);
                  border: 1px dashed #22D3EE;
                  border-radius: 8px;
                  padding: 18px;
                  margin: 24px 0;
                }
                .code-label {
                  font-size: 10px;
                  color: #64748B;
                  letter-spacing: 2px;
                  margin-bottom: 8px;
                }
                .code {
                  font-size: 34px;
                  font-weight: 800;
                  letter-spacing: 10px;
                  color: #22D3EE;
                }
                .notice {
                  font-size: 11px;
                  color: #94A3B8;
                  border-left: 2px solid #F59E0B;
                  padding-left: 10px;
                  margin: 16px 0;
                }
                .footer {
                  text-align: center;
                  font-size: 9px;
                  color: #475569;
                  margin-top: 24px;
                  letter-spacing: 1px;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="brand">
                  <div class="badge">// SECURE ACCESS VERIFICATION //</div>
                  <div class="logo">OMEN-X</div>
                  <div class="subtitle">GLOBAL OSINT & INTELLIGENCE COMMAND WORKSTATION</div>
                </div>
                <div class="divider"></div>
                <div class="greeting">Operator <strong>{{USERNAME}}</strong>,</div>
                <div class="content">
                  A verification request has been initiated for your OMEN-X intelligence terminal access profile. Enter the 6-digit numeric verification code below into your desktop application to complete authentication:
                </div>
                <div class="code-box">
                  <div class="code-label">VERIFICATION ACCESS CODE</div>
                  <div class="code">{{CODE}}</div>
                </div>
                <div class="notice">
                  &#9672; This verification code will expire in <strong>15 minutes</strong>.<br>
                  &#9672; If you did not initiate this request, no action is required.
                </div>
                <div class="divider"></div>
                <div class="footer">
                  OMEN-X INTELLIGENCE SYSTEM &bull; RESTRICTED ACCESS
                </div>
              </div>
            </body>
            </html>
            """;
        return template
                .replace("{{USERNAME}}", safeUser)
                .replace("{{CODE}}", code != null ? code : "000000");
    }
}
