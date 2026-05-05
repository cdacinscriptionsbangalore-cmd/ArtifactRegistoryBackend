package com.cadac.stone_inscription.report.dto;

import com.cadac.stone_inscription.report.enums.ModerationAction;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModerateReportRequest {

    private ModerationAction action;

    @Size(max = 1000)
    private String note;
}
