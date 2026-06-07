package com.tradepilot.service;

import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final ResendEmailClient resendEmailClient;
    private final ObservabilityService observabilityService;

    public void sendVerificationEmail(String email, String link) {
        sendOrLog(
                email,
                "Verify your TradePilot account",
                buildVerificationEmail(link),
                link
        );
    }

    public void sendPasswordResetEmail(String email, String link) {
        sendOrLog(
                email,
                "Reset your TradePilot password",
                buildPasswordResetEmail(link),
                link
        );
    }

    public EmailSendResult sendContactEmail(String to, String name, String fromEmail, String subject, String contactMessage) {
        return sendOrLogContact(
                to,
                "[TradePilot Contact] " + subject,
                buildContactEmail(name, fromEmail, contactMessage),
                name,
                fromEmail,
                subject,
                contactMessage
        );
    }

    public EmailSendResult sendContactReply(String to, String subject, String replyBody) {
        return sendOrLogHtmlEmail(
                to,
                subject,
                buildReplyEmail(replyBody),
                "CONTACT_REPLY"
        );
    }

    private void sendOrLog(String to, String subject, String htmlBody, String link) {
        long start = System.currentTimeMillis();

        if (!resendEmailClient.isConfigured()) {
            log.warn("EMAIL MODE: RESEND_NOT_CONFIGURED. Email link generated but not logged. template={}, recipient={}",
                    subject, LogSanitizer.maskEmail(to));
            observabilityService.recordEmail(to, subject, true, System.currentTimeMillis() - start, null);
            return;
        }

        try {
            String emailId = resendEmailClient.sendHtml(to, subject, htmlBody);
            log.info("EMAIL MODE: RESEND_ENABLED. Email sent. template={}, recipient={}, emailId={}",
                    subject, LogSanitizer.maskEmail(to), emailId);
            observabilityService.recordEmail(to, subject, true, System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            log.warn("Resend email send failed. Link not logged. template={}, recipient={}, error={}",
                    subject, LogSanitizer.maskEmail(to), LogSanitizer.summarizeError(e.getMessage()));
            observabilityService.recordEmail(to, subject, false, System.currentTimeMillis() - start, LogSanitizer.summarizeError(e.getMessage()));
        }
    }

    private EmailSendResult sendOrLogHtmlEmail(String to, String subject, String htmlBody, String template) {
        long start = System.currentTimeMillis();

        if (!resendEmailClient.isConfigured()) {
            log.warn("EMAIL MODE: RESEND_NOT_CONFIGURED. Email not sent. template={}, recipient={}",
                    template, LogSanitizer.maskEmail(to));
            observabilityService.recordEmail(to, subject, true, System.currentTimeMillis() - start, "RESEND_NOT_CONFIGURED");
            return new EmailSendResult(false, "Resend is not configured; message was not sent.");
        }

        try {
            String emailId = resendEmailClient.sendHtml(to, subject, htmlBody);
            log.info("EMAIL MODE: RESEND_ENABLED. Email sent. template={}, recipient={}, emailId={}",
                    template, LogSanitizer.maskEmail(to), emailId);
            observabilityService.recordEmail(to, subject, true, System.currentTimeMillis() - start, null);
            return new EmailSendResult(true, null);
        } catch (Exception e) {
            log.warn("Resend email send failed. template={}, recipient={}, error={}",
                    template, LogSanitizer.maskEmail(to), LogSanitizer.summarizeError(e.getMessage()));
            String error = LogSanitizer.summarizeError(e.getMessage());
            observabilityService.recordEmail(to, subject, false, System.currentTimeMillis() - start, error);
            return new EmailSendResult(false, error);
        }
    }

    private EmailSendResult sendOrLogContact(
            String to,
            String subject,
            String htmlBody,
            String name,
            String fromEmail,
            String contactSubject,
            String contactMessage
    ) {
        long start = System.currentTimeMillis();

        if (!resendEmailClient.isConfigured()) {
            log.warn("EMAIL MODE: RESEND_NOT_CONFIGURED. Contact message not emailed. recipient={}, from={}, subjectLength={}, messageLength={}",
                    LogSanitizer.maskEmail(to),
                    LogSanitizer.maskEmail(fromEmail),
                    contactSubject == null ? 0 : contactSubject.length(),
                    contactMessage == null ? 0 : contactMessage.length());
            observabilityService.recordEmail(to, "CONTACT", true, System.currentTimeMillis() - start, "RESEND_NOT_CONFIGURED");
            return new EmailSendResult(false, "Resend is not configured; message was not sent.");
        }

        try {
            String emailId = resendEmailClient.sendHtml(to, subject, htmlBody, fromEmail);
            log.info("EMAIL MODE: RESEND_ENABLED. Contact email sent. recipient={}, from={}, emailId={}",
                    LogSanitizer.maskEmail(to), LogSanitizer.maskEmail(fromEmail), emailId);
            observabilityService.recordEmail(to, "CONTACT", true, System.currentTimeMillis() - start, null);
            return new EmailSendResult(true, null);
        } catch (Exception e) {
            log.warn("Resend contact email send failed. Message body not logged. recipient={}, from={}, error={}",
                    LogSanitizer.maskEmail(to), LogSanitizer.maskEmail(fromEmail), LogSanitizer.summarizeError(e.getMessage()));
            String error = LogSanitizer.summarizeError(e.getMessage());
            observabilityService.recordEmail(to, "CONTACT", false, System.currentTimeMillis() - start, error);
            return new EmailSendResult(false, error);
        }
    }

    private String buildVerificationEmail(String link) {
        return baseEmail(
                "Verify your email",
                "Welcome to TradePilot.",
                "Verify your email address to activate your TradePilot account and start importing trades.",
                "Verify email",
                link,
                "This verification link expires in 24 hours."
        );
    }

    private String buildPasswordResetEmail(String link) {
        return baseEmail(
                "Reset your password",
                "Password reset requested.",
                "Use the button below to choose a new password for your TradePilot account.",
                "Reset password",
                link,
                "This reset link expires in 30 minutes. If you did not request this, you can safely ignore this email."
        );
    }

    private String buildContactEmail(String name, String email, String message) {
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#020617;font-family:Inter,Arial,sans-serif;color:#e5e7eb;">
                    <div style="padding:32px 18px;background:linear-gradient(135deg,#03120c,#0f172a 45%%,#07111f);">
                      <div style="max-width:680px;margin:0 auto;border:1px solid rgba(148,163,184,.24);border-radius:12px;background:#0f172a;overflow:hidden;">
                        <div style="padding:28px 30px;border-bottom:1px solid rgba(148,163,184,.16);">
                          <div style="color:#6ee7b7;font-size:13px;font-weight:900;letter-spacing:.12em;text-transform:uppercase;">TradePilot Contact</div>
                          <h1 style="margin:12px 0 0;font-size:28px;line-height:1.15;color:#f8fafc;">New contact form message</h1>
                        </div>
                        <div style="padding:30px;color:#cbd5e1;line-height:1.65;">
                          <p style="margin:0 0 10px;"><strong style="color:#f8fafc;">Name:</strong><br>%s</p>
                          <p style="margin:0 0 10px;"><strong style="color:#f8fafc;">Email:</strong><br>%s</p>
                          <p style="margin:0;"><strong style="color:#f8fafc;">Message:</strong></p>
                          <p style="white-space:pre-wrap;margin:8px 0 0;">%s</p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(escapeHtml(name), escapeHtml(email), escapeHtml(message));
    }

    private String buildReplyEmail(String message) {
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#020617;font-family:Inter,Arial,sans-serif;color:#e5e7eb;">
                    <div style="padding:32px 18px;background:linear-gradient(135deg,#03120c,#0f172a 45%%,#07111f);">
                      <div style="max-width:680px;margin:0 auto;border:1px solid rgba(148,163,184,.24);border-radius:12px;background:#0f172a;overflow:hidden;">
                        <div style="padding:28px 30px;border-bottom:1px solid rgba(148,163,184,.16);">
                          <div style="color:#6ee7b7;font-size:13px;font-weight:900;letter-spacing:.12em;text-transform:uppercase;">TradePilot Support</div>
                          <h1 style="margin:12px 0 0;font-size:28px;line-height:1.15;color:#f8fafc;">Thanks for reaching out</h1>
                        </div>
                        <div style="padding:30px;color:#cbd5e1;line-height:1.65;">
                          <p style="white-space:pre-wrap;margin:0;">%s</p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(escapeHtml(message));
    }

    private String baseEmail(
            String title,
            String eyebrow,
            String message,
            String buttonText,
            String link,
            String footer
    ) {
        String safeLink = escapeHtml(link);
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#020617;font-family:Inter,Arial,sans-serif;color:#e5e7eb;">
                    <div style="padding:32px 18px;background:linear-gradient(135deg,#03120c,#0f172a 45%%,#14130b);">
                      <div style="max-width:620px;margin:0 auto;border:1px solid rgba(148,163,184,.24);border-radius:12px;background:#0f172a;overflow:hidden;">
                        <div style="padding:28px 30px;border-bottom:1px solid rgba(148,163,184,.16);">
                          <div style="color:#6ee7b7;font-size:13px;font-weight:900;letter-spacing:.12em;text-transform:uppercase;">TradePilot AI</div>
                          <h1 style="margin:12px 0 0;font-size:28px;line-height:1.15;color:#f8fafc;">%s</h1>
                        </div>
                        <div style="padding:30px;">
                          <p style="margin:0 0 8px;color:#bbf7d0;font-weight:800;">%s</p>
                          <p style="margin:0 0 24px;color:#cbd5e1;line-height:1.65;">%s</p>
                          <a href="%s" style="display:inline-block;padding:13px 18px;border-radius:8px;background:linear-gradient(135deg,#6ee7b7,#22c55e);color:#052e16;text-decoration:none;font-weight:900;">%s</a>
                          <p style="margin:24px 0 8px;color:#94a3b8;font-size:13px;line-height:1.6;">If the button does not work, paste this link into your browser:</p>
                          <p style="word-break:break-all;margin:0;color:#6ee7b7;font-size:13px;line-height:1.6;">%s</p>
                          <p style="margin:24px 0 0;color:#94a3b8;font-size:13px;line-height:1.6;">%s</p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(eyebrow),
                escapeHtml(message),
                safeLink,
                escapeHtml(buttonText),
                safeLink,
                escapeHtml(footer)
        );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record EmailSendResult(boolean sent, String error) {
    }
}
