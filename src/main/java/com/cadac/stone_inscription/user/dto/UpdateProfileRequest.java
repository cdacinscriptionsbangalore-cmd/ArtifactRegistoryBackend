package com.cadac.stone_inscription.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @JsonProperty("username")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @JsonProperty("bio")
    @Size(min = 3, max = 150, message = "Bio must be between 3 and 150 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9 ]+$", message = "Bio can only contain letters, numbers, and spaces")
    private String bio;
}
