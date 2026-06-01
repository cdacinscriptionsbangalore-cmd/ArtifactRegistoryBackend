package com.cadac.stone_inscription.admin.service;

public interface AdminEmailService {

    void sendApprovalRequest(String adminEmail, String adminName, String approvalLink);

    void sendApprovalConfirmed(String adminEmail, String adminName);
}
