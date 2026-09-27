package com.alvinskylers.tablesync.dto.table;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TableResponse(
        UUID id,
        int tableNumber,
        int seatCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
