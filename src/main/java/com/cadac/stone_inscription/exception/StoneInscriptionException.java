package com.cadac.stone_inscription.exception;

import org.springframework.http.HttpStatus;

public class StoneInscriptionException extends RuntimeException {

    private HttpStatus httpStatus;

   public StoneInscriptionException(String message, HttpStatus status) {
        super(message);
        httpStatus = status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
