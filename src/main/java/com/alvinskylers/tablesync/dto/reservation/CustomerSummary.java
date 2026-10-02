package com.alvinskylers.tablesync.dto.reservation;

import java.util.UUID;

public record CustomerSummary(
        UUID id,
        String email
) {
}
