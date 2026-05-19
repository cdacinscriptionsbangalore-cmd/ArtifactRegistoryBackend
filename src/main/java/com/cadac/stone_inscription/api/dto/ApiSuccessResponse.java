package com.cadac.stone_inscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(name = "ApiSuccessResponse", description = "Standard success envelope returned by most JSON endpoints.")
public class ApiSuccessResponse<T> {

    @Schema(description = "Operation result message.", example = "Profile fetched successfully")
    private String message;

    @Schema(description = "HTTP status reason used by the response envelope.", example = "OK")
    @JsonProperty("http-status")
    private Object httpStatus;

    @Schema(description = "Endpoint-specific payload.")
    private T data;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Object httpStatus) {
        this.httpStatus = httpStatus;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
