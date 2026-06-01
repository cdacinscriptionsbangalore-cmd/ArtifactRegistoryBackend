package com.cadac.stone_inscription.report.dto;

import com.cadac.stone_inscription.report.enums.ModerationAction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "ModerateReportRequest", description = "Optional instruction supplied by a moderator when processing a report.")
public class ModerateReportRequest {

    @Schema(description = "Moderation action to apply. If omitted, the moderation chain decides the next transition.", example = "REMOVE_CONTENT")
    private ModerationAction action;

    @Schema(description = "Moderator note saved in the report audit trail. Maximum 1000 characters.", example = "Removed duplicate inscription image after review.", maxLength = 1000)
    @Size(max = 1000)
    private String note;
}
