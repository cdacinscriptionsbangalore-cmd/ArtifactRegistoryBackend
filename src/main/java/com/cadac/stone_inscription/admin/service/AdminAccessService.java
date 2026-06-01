package com.cadac.stone_inscription.admin.service;

import com.cadac.stone_inscription.admin.entity.AdminRequest;
import com.cadac.stone_inscription.entity.UserAuth;

public interface AdminAccessService {

    AdminRequest createOrRefreshPendingRequest(UserAuth userAuth, String name, String provider);

    boolean isApprovedAdmin(String email);

    void approveRequest(String rawToken);

    String getApprovalResultRedirectUrl(String status);
}
