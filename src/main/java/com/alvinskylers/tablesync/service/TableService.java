package com.alvinskylers.tablesync.service;

import com.alvinskylers.tablesync.dto.table.TableRequest;
import com.alvinskylers.tablesync.dto.table.TableResponse;
import com.alvinskylers.tablesync.entity.RestaurantTable;
import com.alvinskylers.tablesync.exception.TableNotFoundException;
import com.alvinskylers.tablesync.exception.TableNumberExistsException;
import com.alvinskylers.tablesync.mapper.TableMapper;
import com.alvinskylers.tablesync.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableMapper tableMapper;
    private final TableRepository tableRepository;


    public Page<TableResponse> getTables(Pageable pageable) {
        return  tableRepository.findAll(pageable)
                .map(tableMapper::mapTableToResponse);
    }

    public TableResponse createTable(TableRequest request) {
        if (tableRepository.existsByTableNumber(request.tableNumber())) {
            throw new TableNumberExistsException("table number exists.");
        }

        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(request.tableNumber())
                .seatCount(request.seatCount())
                .build();

        tableRepository.save(table);
        return tableMapper.mapTableToResponse(table);
    }

    public TableResponse viewTable(UUID id) {
        RestaurantTable table = findTableById(id);
        return tableMapper.mapTableToResponse(table);
    }

    public TableResponse updateTable(UUID id, TableRequest request) {
        RestaurantTable table = findTableById(id);

        if (request.tableNumber() != table.getTableNumber()
                && tableRepository.existsByTableNumber(request.tableNumber())) {
            throw new TableNumberExistsException("table with number exists, number: " + request.tableNumber());
        }

        table.setTableNumber(request.tableNumber());
        table.setSeatCount(request.seatCount());

        tableRepository.save(table);
        return tableMapper.mapTableToResponse(table);
    }

    public void deleteTable(UUID id) {
        RestaurantTable table = findTableById(id);
        tableRepository.delete(table);
    }

    private RestaurantTable findTableById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new TableNotFoundException("table does not exists, id" + id));
    }

}
