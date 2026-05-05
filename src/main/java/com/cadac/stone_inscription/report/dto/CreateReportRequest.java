package com.cadac.stone_inscription.report.dto;

import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotNull
    private ReportTargetType targetType;

    @NotBlank
    private String targetId;

    @NotNull
    private ReportReason reason;

    @NotBlank
    @Size(max = 1000)
    private String details;
}
