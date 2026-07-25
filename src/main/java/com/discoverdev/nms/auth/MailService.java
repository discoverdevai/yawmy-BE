package com.discoverdev.nms.auth;

import com.discoverdev.nms.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails via the configured SMTP relay (Brevo).
 * All methods are best-effort: if mail is not configured or sending fails,
 * we log and return rather than throwing, so the caller's flow (e.g. creating
 * an invite) is never broken by an email problem.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties props;

    public MailService(JavaMailSender mailSender, MailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendAdminInvite(String toEmail, String fullName, String inviteLink) {
        String greeting = (fullName != null && !fullName.isBlank()) ? fullName : "there";
        String subject = "You're invited to Yawmy Care";
        String body = """
            Hi %s,

            You have been invited to set up your nursery on Yawmy Care.
            Tap the link below to set your password and get started:

            %s

            This invitation expires in 48 hours.

            If you did not expect this, you can ignore this email.
            """.formatted(greeting, inviteLink);
        send(toEmail, subject, body);
    }

    /**
     * Tell whoever can re-issue an invite that an invitee's link lapsed.
     * {@code roleLabel} is "teacher" or "admin"; {@code nurseryName} may be null.
     */
    public void sendReactivationNotice(
        String toEmail,
        String requesterEmail,
        String roleLabel,
        String nurseryName,
        String message
    ) {
        String subject = "Invite reactivation requested";
        String where = (nurseryName != null && !nurseryName.isBlank())
            ? "\nNursery: " + nurseryName
            : "";
        String extra = (message != null && !message.isBlank())
            ? "\n\nMessage from the requester:\n" + message
            : "";
        String how = "teacher".equals(roleLabel)
            ? "To reactivate, open the staff list and use \"Resend invite\" for this teacher."
            : "To reactivate, issue a fresh invite for this account.";
        String body = """
            An invited %s reports their invitation has expired and asks to be re-invited.

            Requester email: %s%s%s

            %s
            """.formatted(roleLabel, requesterEmail, where, extra, how);
        send(toEmail, subject, body);
    }

    public void sendStaffInvite(String toEmail, String fullName, String inviteLink) {
        String greeting = (fullName != null && !fullName.isBlank()) ? fullName : "there";
        String subject = "You're invited to join Yawmy Care";
        String body = """
            Hi %s,

            You have been invited to join your nursery's team on Yawmy Care.
            Tap the link below to set your password and get started:

            %s

            This invitation expires in 48 hours.

            If you did not expect this, you can ignore this email.
            """.formatted(greeting, inviteLink);
        send(toEmail, subject, body);
    }

    public void sendParentInvite(String toEmail, String fullName, String inviteLink) {
        String greeting = (fullName != null && !fullName.isBlank()) ? fullName : "there";
        String subject = "You're invited to Yawmy Care";
        String body = """
            Hi %s,

            You have been invited to follow your child's day on Yawmy Care.
            Tap the link below to set your password and get started:

            %s

            This invitation expires in 48 hours.

            If you did not expect this, you can ignore this email.
            """.formatted(greeting, inviteLink);
        send(toEmail, subject, body);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        String subject = "Your Yawmy Care password reset code";
        String body = """
            You requested to reset your Yawmy Care password.

            Your verification code is: %s

            This code expires in 30 seconds. If it expires, tap "Resend code" in the app.
            If you did not request this, you can ignore this email.
            """.formatted(otp);
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        if (props.from() == null || props.from().isBlank()) {
            log.warn("Mail not sent to {}: app.mail.from (MAIL_FROM) is not configured", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(props.from(), props.fromName() != null ? props.fromName() : props.from());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Invite email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}