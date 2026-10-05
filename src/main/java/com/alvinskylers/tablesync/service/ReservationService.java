package com.alvinskylers.tablesync.service;

import com.alvinskylers.tablesync.dto.reservation.CreateReservationRequest;
import com.alvinskylers.tablesync.dto.reservation.ReservationResponse;
import com.alvinskylers.tablesync.dto.reservation.UpdateReservationRequest;
import com.alvinskylers.tablesync.entity.Reservation;
import com.alvinskylers.tablesync.entity.RestaurantTable;
import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.entity.enums.Role;
import com.alvinskylers.tablesync.entity.enums.Status;
import com.alvinskylers.tablesync.exception.*;
import com.alvinskylers.tablesync.mapper.ReservationMapper;
import com.alvinskylers.tablesync.repository.ReservationRepository;
import com.alvinskylers.tablesync.repository.TableRepository;
import com.alvinskylers.tablesync.security.UserPrincipal;
import com.alvinskylers.tablesync.validator.ReservationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationValidator reservationValidator;
    private final ReservationMapper reservationMapper;
    private final TableRepository tableRepository;

    public Page<ReservationResponse> getAllReservations(Pageable pageable) {
        User user = getCurrentUser();
        if (user.getRole() == Role.CUSTOMER) {
            return reservationRepository.findByCustomerId(getCurrentUser().getId(), pageable)
                    .map(r -> reservationMapper.mapReservationToResponse(r));
        }
         return reservationRepository.findAll(pageable)
                .map(r -> reservationMapper.mapReservationToResponse(r));
    }

    public ReservationResponse createReservation(CreateReservationRequest request) {
        reservationValidator.validateReservationCreation(request);
        User customer = getCurrentUser();
        RestaurantTable table = findTableById(request.tableId());
        Reservation reservation = Reservation.builder()
                .customer(customer)
                .table(table)
                .reservationStart(request.reservationStart())
                .reservationEnd(request.reservationEnd())
                .status(Status.PENDING)
                .build();
        reservationRepository.save(reservation);
        return reservationMapper.mapReservationToResponse(reservation);
    }

    public ReservationResponse cancelReservation(UUID reservationId) {
        reservationValidator.validateReservationCancellation(reservationId);
        var reservation = findReservationById(reservationId);
        reservation.setStatus(Status.CANCELLED);
        reservationRepository.save(reservation);
        return reservationMapper.mapReservationToResponse(reservation);
    }

    public ReservationResponse updateReservation(UUID reservationId, UpdateReservationRequest request) {
        reservationValidator.validateReservationUpdate(reservationId);
        var reservation = findReservationById(reservationId);
        reservation.setStatus(request.status());
        reservationRepository.save(reservation);
        return reservationMapper.mapReservationToResponse(reservation);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getUser();
    }

    private RestaurantTable findTableById(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new TableNotFoundException("table not found with id: " + id));
    }

    private Reservation findReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("reservation not found with id: " + id));
    }

}
