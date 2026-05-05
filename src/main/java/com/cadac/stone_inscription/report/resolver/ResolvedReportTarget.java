package com.cadac.stone_inscription.report.resolver;

import com.cadac.stone_inscription.report.enums.ReportTargetType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolvedReportTarget {

    private final String id;
    private final String authorId;
    private final ReportTargetType type;
    private final String content;
    private final Object entity;
}
