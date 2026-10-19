package com.alvinskylers.tablesync;

import com.alvinskylers.tablesync.dto.reservation.CreateReservationRequest;
import com.alvinskylers.tablesync.entity.Reservation;
import com.alvinskylers.tablesync.exception.InvalidReservationTimeException;
import com.alvinskylers.tablesync.exception.TableReservedException;
import com.alvinskylers.tablesync.repository.ReservationRepository;
import com.alvinskylers.tablesync.validator.ReservationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationValidationTest {

    @Mock ReservationRepository reservationRepository;
    @InjectMocks ReservationValidator validator;

    @Test
    void creation_endBeforeStart_throwsInvalidTime() {
        var request = new CreateReservationRequest(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 10, 10, 21, 0),
                LocalDateTime.of(2026, 10, 10, 19, 0));

        assertThrows(InvalidReservationTimeException.class,
                () -> validator.validateReservationCreation(request));
    }

    @Test
    void creation_equalToStart_throwsInvalidTime() {
        var request = new CreateReservationRequest(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 10, 10, 21, 0),
                LocalDateTime.of(2026, 10, 10, 21, 0));

        assertThrows(InvalidReservationTimeException.class,
                () -> validator.validateReservationCreation(request));
    }

    @Test
    void creation_overlappingExists_throwsTableReserved() {
        UUID tableId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 10, 10, 21, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 10, 22, 0);

        when(reservationRepository.findOverlappingReservations(
                eq(tableId), anyList(), eq(start), eq(end)))
                .thenReturn(List.of(Reservation.builder().build()));
        var request = new CreateReservationRequest(tableId, start, end);

        assertThrows(TableReservedException.class,
                () -> validator.validateReservationCreation(request));
    }


    @Test
    void creation_noOverlap_doesNotThrow() {
        UUID tableId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 10, 10, 21, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 10, 22, 0);

        when(reservationRepository.findOverlappingReservations(
                eq(tableId), anyList(), eq(start), eq(end)
        )).thenReturn(List.of());

        var request = new CreateReservationRequest(tableId, start, end);

        assertDoesNotThrow(() -> validator.validateReservationCreation(request));
        verify(reservationRepository).findOverlappingReservations(
                eq(tableId), anyList(), eq(start), eq(end));
    }

}
