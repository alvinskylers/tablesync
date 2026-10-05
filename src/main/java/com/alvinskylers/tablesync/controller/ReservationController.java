package com.alvinskylers.tablesync.controller;

import com.alvinskylers.tablesync.dto.reservation.CreateReservationRequest;
import com.alvinskylers.tablesync.dto.reservation.ReservationResponse;
import com.alvinskylers.tablesync.dto.reservation.UpdateReservationRequest;
import com.alvinskylers.tablesync.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReservationResponse> reservations = reservationService.getAllReservations(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reservations);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse reservation = reservationService.createReservation(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservation);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID id) {
        ReservationResponse reservation = reservationService.cancelReservation(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reservation);
    }

    @PreAuthorize("hasAnyRole('ADMIN','HOST')")
    @PatchMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(@PathVariable UUID id, @Valid @RequestBody UpdateReservationRequest request) {
        ReservationResponse reservation = reservationService.updateReservation(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reservation);
    }

}
