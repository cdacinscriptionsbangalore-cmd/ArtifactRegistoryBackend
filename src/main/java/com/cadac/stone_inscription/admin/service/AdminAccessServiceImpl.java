package com.cadac.stone_inscription.admin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.admin.entity.AdminRequest;
import com.cadac.stone_inscription.admin.entity.AdminRequestStatus;
import com.cadac.stone_inscription.admin.repository.AdminRequestRepository;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.repository.UserAuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAccessServiceImpl implements AdminAccessService {

    private final AdminRequestRepository adminRequestRepository;
    private final AdminEmailService adminEmailService;
    private final UserAuthRepository userAuthRepository;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.frontend.admin.approval-result-url:https://inscriptions.cdacb.in/admin/approval-result}")
    private String approvalResultUrl;

    @Value("${admin.approval.token-validity-ms:86400000}")
    private long approvalTokenValidityMs;

    @Override
    public AdminRequest createOrRefreshPendingRequest(UserAuth userAuth, String name, String provider) {
        if (userAuth == null || userAuth.getEmail() == null) {
            throw new StoneInscriptionException("User account is required for admin registration", HttpStatus.BAD_REQUEST);
        }

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Date expiresAt = new Date(System.currentTimeMillis() + approvalTokenValidityMs);

        AdminRequest request = adminRequestRepository.findByEmail(userAuth.getEmail())
                .map(existing -> updateExistingRequest(existing, userAuth, name, provider, tokenHash, expiresAt))
                .orElseGet(() -> buildNewRequest(userAuth, name, provider, tokenHash, expiresAt));

        AdminRequest savedRequest = adminRequestRepository.save(request);
        adminEmailService.sendApprovalRequest(savedRequest.getEmail(), savedRequest.getName(), buildApprovalLink(rawToken));
        return savedRequest;
    }

    @Override
    public boolean isApprovedAdmin(String email) {
        return adminRequestRepository.findByEmail(email)
                .map(request -> request.getStatus() == AdminRequestStatus.APPROVED)
                .orElse(false);
    }

    @Override
    public void approveRequest(String rawToken) {
        String tokenHash = hashToken(rawToken);
        AdminRequest request = adminRequestRepository.findByApprovalTokenHash(tokenHash)
                .orElseThrow(() -> new StoneInscriptionException("Invalid admin approval token", HttpStatus.BAD_REQUEST));

        if (request.getApprovalTokenExpiresAt() == null || request.getApprovalTokenExpiresAt().before(new Date())) {
            throw new StoneInscriptionException("Admin approval token expired", HttpStatus.BAD_REQUEST);
        }

        request.setStatus(AdminRequestStatus.APPROVED);
        request.setApprovedAt(new Date());
        request.setApprovedBy("email-link");
        request.setApprovalTokenHash(null);
        request.setApprovalTokenExpiresAt(null);

        adminRequestRepository.save(request);
        grantAdminRole(request.getEmail());
        adminEmailService.sendApprovalConfirmed(request.getEmail(), request.getName());
    }

    public String getApprovalResultRedirectUrl(String status) {
        return approvalResultUrl + "?status=" + status;
    }

    private AdminRequest buildNewRequest(
            UserAuth userAuth,
            String name,
            String provider,
            String tokenHash,
            Date expiresAt) {

        return AdminRequest.builder()
                .userAuthId(userAuth.getId())
                .email(userAuth.getEmail())
                .name(name)
                .provider(provider)
                .status(AdminRequestStatus.PENDING)
                .approvalTokenHash(tokenHash)
                .approvalTokenExpiresAt(expiresAt)
                .build();
    }

    private AdminRequest updateExistingRequest(
            AdminRequest request,
            UserAuth userAuth,
            String name,
            String provider,
            String tokenHash,
            Date expiresAt) {

        request.setUserAuthId(userAuth.getId());
        request.setEmail(userAuth.getEmail());
        request.setName(name);
        request.setProvider(provider);

        if (request.getStatus() != AdminRequestStatus.APPROVED) {
            request.setStatus(AdminRequestStatus.PENDING);
        }

        request.setApprovalTokenHash(tokenHash);
        request.setApprovalTokenExpiresAt(expiresAt);
        return request;
    }

    private String buildApprovalLink(String rawToken) {
        return trimTrailingSlash(backendUrl) + "/oauth2/admin/approve?token=" + rawToken;
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private void grantAdminRole(String email) {
        UserAuth userAuth = userAuthRepository.findByEmail(email);
        if (userAuth == null) {
            throw new StoneInscriptionException("Approved admin user not found", HttpStatus.NOT_FOUND);
        }

        List<String> roles = userAuth.getRoles() == null ? new ArrayList<>() : new ArrayList<>(userAuth.getRoles());
        boolean hasAdminRole = roles.stream().anyMatch(role -> "admin".equalsIgnoreCase(role));
        if (!hasAdminRole) {
            roles.add("admin");
            userAuth.setRoles(roles);
            userAuthRepository.save(userAuth);
        }
    }
}
