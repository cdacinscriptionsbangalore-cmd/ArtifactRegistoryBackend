package com.cadac.stone_inscription.exception;

import org.springframework.validation.BindException;

import java.nio.file.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {

    // Custom Exception
    @ExceptionHandler(StoneInscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleStoneInscriptionException(StoneInscriptionException exception) {
        Map<String, Object> errorResp = new HashMap<String, Object>();
        errorResp.put("error_message", exception.getMessage());
        errorResp.put("http_status", exception.getHttpStatus());
        errorResp.put("http_status_code", exception.getHttpStatus().value());
        return new ResponseEntity<Map<String, Object>>(errorResp, exception.getHttpStatus());
    }

    // Request Body
    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exeption) {

        Map<String, Object> errorResp = new HashMap<String, Object>();

        exeption.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            // errorResp.put(fieldName, message);
            errorResp.put("error_message", fieldName + " --> " + message);
        });

        errorResp.put("http_status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        return new ResponseEntity<Map<String, Object>>(errorResp, HttpStatus.UNPROCESSABLE_ENTITY);

    }

    // Model Attribute
    @ExceptionHandler(BindException.class)

    public ResponseEntity<Map<String, Object>> handleBindException(
            BindException exeption) {

        Map<String, Object> errorResp = new HashMap<String, Object>();
        errorResp.put("http_status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        exeption.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorResp.put("error_message", fieldName + " : " + message);
        });

        return new ResponseEntity<Map<String, Object>>(errorResp, HttpStatus.UNPROCESSABLE_ENTITY);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {

        Map<String, Object> errorResp = new HashMap<String, Object>();
        errorResp.put("error_message", exception.getMessage());
        errorResp.put("http_status", HttpStatus.BAD_REQUEST);
        errorResp.put("http_status_code", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<Map<String, Object>>(errorResp, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {

        Map<String, Object> errorResp = new HashMap<String, Object>();
        errorResp.put("error_message", "Upload size exceeded the allowed limit. Each image must be 75 MB or less and a post can contain at most 16 images.");
        errorResp.put("http_status", HttpStatus.BAD_REQUEST);
        errorResp.put("http_status_code", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<Map<String, Object>>(errorResp, HttpStatus.BAD_REQUEST);
    }

    // // 🔹 500 Internal Server Error (catch all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> errorResp = new HashMap<String, Object>();
        errorResp.put("error_message", ex.getMessage());
        errorResp.put("http_status", HttpStatus.BAD_REQUEST);
        errorResp.put("http_status_code", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<Map<String, Object>>(errorResp, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    // // Any kind of Exception
    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<String> handleUnknownException(Exception ex) {

    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    // .body("An unknown error occurred. Please try again later.");
    // }

}
