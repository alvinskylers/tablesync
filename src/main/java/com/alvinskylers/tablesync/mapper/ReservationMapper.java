package com.alvinskylers.tablesync.mapper;

import com.alvinskylers.tablesync.dto.reservation.CustomerSummary;
import com.alvinskylers.tablesync.dto.reservation.ReservationResponse;
import com.alvinskylers.tablesync.dto.reservation.TableSummary;
import com.alvinskylers.tablesync.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse mapReservationToResponse(Reservation reservation) {
        CustomerSummary customer = new CustomerSummary(
                reservation.getCustomer().getId(),
                reservation.getCustomer().getEmail());
        TableSummary table = new TableSummary(
                reservation.getTable().getId(),
                reservation.getTable().getTableNumber());
        return ReservationResponse.builder()
                .id(reservation.getId())
                .customer(customer)
                .table(table)
                .reservationStart(reservation.getReservationStart())
                .reservationEnd(reservation.getReservationEnd())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
