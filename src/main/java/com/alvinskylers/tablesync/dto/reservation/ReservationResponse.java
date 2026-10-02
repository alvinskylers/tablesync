package com.alvinskylers.tablesync.dto.reservation;

import com.alvinskylers.tablesync.entity.enums.Status;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReservationResponse(
        UUID id,
        CustomerSummary customer,
        TableSummary table,
        LocalDateTime reservationStart,
        LocalDateTime reservationEnd,
        Status status,
        LocalDateTime createdAt
) {
}
