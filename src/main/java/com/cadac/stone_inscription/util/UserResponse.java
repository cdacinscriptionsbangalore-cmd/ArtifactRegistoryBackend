package com.cadac.stone_inscription.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class UserResponse {
    public static <T> ResponseEntity<?> responseHandler(String message, HttpStatus status, T data) {

        Map<Object, Object> resp = new HashMap<>();
        resp.put("message", message);
        resp.put("http-status", status);
        resp.put("data", data);

        return new ResponseEntity<>(resp, status);
    }

}
