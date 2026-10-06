package com.alvinskylers.tablesync.dto.table;

import java.time.LocalDateTime;

public record BookedSlot(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
