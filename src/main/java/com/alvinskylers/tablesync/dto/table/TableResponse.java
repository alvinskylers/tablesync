package com.alvinskylers.tablesync.dto.table;

import java.time.LocalDateTime;
import java.util.UUID;

public record TableResponse(
        UUID id,
        int tableNumber,
        int seatCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
