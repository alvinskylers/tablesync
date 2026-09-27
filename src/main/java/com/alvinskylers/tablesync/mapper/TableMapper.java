package com.alvinskylers.tablesync.mapper;

import com.alvinskylers.tablesync.dto.table.TableResponse;
import com.alvinskylers.tablesync.entity.RestaurantTable;
import org.springframework.stereotype.Component;

@Component
public class TableMapper {

    public TableResponse mapTableToResponse(RestaurantTable table) {
        return TableResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .seatCount(table.getSeatCount())
                .createdAt(table.getCreatedAt())
                .build();
    }


}
