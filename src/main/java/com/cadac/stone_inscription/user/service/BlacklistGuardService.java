package com.cadac.stone_inscription.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;

@Service
public class BlacklistGuardService {

    public void ensureCanCreateOrModifyContent(User user) {
        ensureNotBlacklisted(user, "Blacklisted users cannot create or modify content.");
    }

    public void ensureCanReport(User user) {
        ensureNotBlacklisted(user, "Blacklisted users cannot file reports.");
    }

    private void ensureNotBlacklisted(User user, String message) {
        if (user != null && Boolean.TRUE.equals(user.getBlackListed())) {
            throw new StoneInscriptionException(message, HttpStatus.FORBIDDEN);
        }
    }
}
