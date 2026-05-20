package com.cadac.stone_inscription.report.dto;

import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportTargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateReportRequest", description = "Request used by authenticated users to report a post, comment, or user.")
public class CreateReportRequest {

    @Schema(description = "Type of resource being reported.", example = "POST", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ReportTargetType targetType;

    @Schema(description = "MongoDB ObjectId or canonical identifier of the reported resource.", example = "665f1df013ad4e18f6a11244", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String targetId;

    @Schema(description = "Reason selected by the reporter.", example = "MISINFORMATION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private ReportReason reason;

    @Schema(description = "Reporter-provided context for moderators. Maximum 1000 characters.", example = "The inscription description contains misleading historical attribution.", maxLength = 1000, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 1000)
    private String details;
}
