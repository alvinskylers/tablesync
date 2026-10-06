package com.alvinskylers.tablesync.validator;

import com.alvinskylers.tablesync.entity.RestaurantTable;
import com.alvinskylers.tablesync.entity.enums.Status;
import com.alvinskylers.tablesync.exception.InvalidReservationTimeException;
import com.alvinskylers.tablesync.exception.TableNotFoundException;
import com.alvinskylers.tablesync.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TableValidator {

    private final TableRepository tableRepository;
    public static final List<Status> STATUSES = List.of(Status.CONFIRMED, Status.PENDING);

    public void validateScheduleRequest(UUID tableId, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidReservationTimeException("Reservation end time must be after start time. ");
        }

        if (end.isAfter(start.plusWeeks(1))) {
            throw new InvalidReservationTimeException("Reservation time range must not exceed 1 week.");
        }
        findTableById(tableId);
    }

    private RestaurantTable findTableById(UUID tableId) {
        return tableRepository.findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("table not found with id: " + tableId));
    }
 }
