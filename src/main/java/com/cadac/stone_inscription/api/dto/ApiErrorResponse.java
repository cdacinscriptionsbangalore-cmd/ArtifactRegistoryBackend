package com.cadac.stone_inscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(name = "ApiErrorResponse", description = "Standard error body returned by API exception handlers.")
public class ApiErrorResponse {

    @Schema(description = "Human-readable error message.", example = "Invalid or missing authorization token")
    @JsonProperty("error_message")
    private String errorMessage;

    @Schema(description = "HTTP status reason or numeric status used by the handler.", example = "UNAUTHORIZED")
    @JsonProperty("http_status")
    private Object httpStatus;

    @Schema(description = "HTTP status code.", example = "401")
    @JsonProperty("http_status_code")
    private Integer httpStatusCode;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Object httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
}
