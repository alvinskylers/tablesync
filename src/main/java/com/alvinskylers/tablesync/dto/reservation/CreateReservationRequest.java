package com.alvinskylers.tablesync.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;


import java.time.LocalDateTime;
import java.util.UUID;

public record CreateReservationRequest(

        @NotNull
        UUID tableId,

        @NotNull
        @Future
        LocalDateTime reservationStart,

        @NotNull
        @Future
        LocalDateTime reservationEnd
) {
}
