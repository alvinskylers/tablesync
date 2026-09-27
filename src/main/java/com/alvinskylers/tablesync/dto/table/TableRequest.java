package com.alvinskylers.tablesync.dto.table;

import jakarta.validation.constraints.Min;

public record TableRequest(

        @Min(1)
        int tableNumber,

        @Min(1)
        int seatCount
) {
}
