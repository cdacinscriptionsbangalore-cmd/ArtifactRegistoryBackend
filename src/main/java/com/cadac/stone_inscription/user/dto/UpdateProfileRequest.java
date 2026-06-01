package com.cadac.stone_inscription.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateProfileRequest", description = "Editable profile fields for the authenticated user.")
public class UpdateProfileRequest {

    @JsonProperty("username")
    @Schema(description = "Unique username. Submit only when changing it.", example = "inscription_scholar", minLength = 3, maxLength = 25)
    @Size(min = 3, max = 25, message = "Username must be between 3 and 25 characters")
    private String username;

    @JsonProperty("bio")
    @Schema(description = "Short profile bio. Letters, numbers, and spaces only.", example = "Epigraphy researcher", minLength = 3, maxLength = 150, pattern = "^(?=.*[A-Za-z0-9])[A-Za-z0-9 ]+$")
    @Size(min = 3, max = 150, message = "Bio must be between 3 and 150 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9 ]+$", message = "Bio can only contain letters, numbers, and spaces")
    private String bio;
}
