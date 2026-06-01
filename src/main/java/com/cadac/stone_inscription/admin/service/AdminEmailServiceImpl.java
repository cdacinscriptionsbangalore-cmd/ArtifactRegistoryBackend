package com.cadac.stone_inscription.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminEmailServiceImpl implements AdminEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${ADMIN_APPROVAL_INTERNAL_EMAIL:}")
    private String internalApprovalEmail;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendApprovalRequest(String adminEmail, String adminName, String approvalLink) {
        if (internalApprovalEmail.isBlank()) {
            throw new IllegalStateException("admin.approval.internal-email is not configured");
        }
        JavaMailSender mailSender = getMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(internalApprovalEmail);
        if (!fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setSubject("Admin approval request");
        message.setText(
                "A new admin access request was submitted.\n\n"
                        + "Name: " + adminName + "\n"
                        + "Email: " + adminEmail + "\n\n"
                        + "Approve access using this link:\n"
                        + approvalLink);
        mailSender.send(message);
    }

    @Override
    public void sendApprovalConfirmed(String adminEmail, String adminName) {
        JavaMailSender mailSender = getMailSender();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        if (!fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setSubject("Admin access approved");
        message.setText(
                "Hello " + adminName + ",\n\n"
                        + "Your admin access request has been approved. "
                        + "You can now use Admin Login with your existing OAuth provider.");
        mailSender.send(message);
    }

    private JavaMailSender getMailSender() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Mail sender is not configured");
        }
        return mailSender;
    }
}
