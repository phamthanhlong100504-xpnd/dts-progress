package com.dts.progress.dto.response;

import java.time.LocalDate;

public record StreakResponse(
        int currentStreak,
        int longestStreak,
        LocalDate lastStudyDate,
        boolean studiedToday
) {}
