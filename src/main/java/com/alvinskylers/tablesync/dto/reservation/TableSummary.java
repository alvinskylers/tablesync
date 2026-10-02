package com.alvinskylers.tablesync.dto.reservation;

import java.util.UUID;

public record TableSummary(
        UUID id,
        int tableNumber
) {
}
