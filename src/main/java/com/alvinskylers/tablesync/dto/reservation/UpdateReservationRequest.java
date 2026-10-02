package com.alvinskylers.tablesync.dto.reservation;

import com.alvinskylers.tablesync.entity.enums.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationRequest(

        @NotNull
        Status status
) {
}
