package com.cadac.stone_inscription.auth;

public enum OAuthFlowType {
    USER_LOGIN("user_login"),
    ADMIN_REGISTER("admin_register"),
    ADMIN_LOGIN("admin_login");

    private final String value;

    OAuthFlowType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OAuthFlowType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return USER_LOGIN;
        }

        for (OAuthFlowType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        return USER_LOGIN;
    }
}
