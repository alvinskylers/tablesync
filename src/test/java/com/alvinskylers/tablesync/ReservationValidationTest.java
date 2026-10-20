package com.alvinskylers.tablesync;

import com.alvinskylers.tablesync.dto.reservation.CreateReservationRequest;
import com.alvinskylers.tablesync.entity.Reservation;
import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.entity.enums.Role;
import com.alvinskylers.tablesync.entity.enums.Status;
import com.alvinskylers.tablesync.exception.*;
import com.alvinskylers.tablesync.repository.ReservationRepository;
import com.alvinskylers.tablesync.security.UserPrincipal;
import com.alvinskylers.tablesync.validator.ReservationValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    private Reservation reservationOf(User customer, Status status) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .status(status)
                .build();
    }

    private void givenReservationExists(Reservation reservation) {
        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
    }

    private User userWithRole(Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(role)
                .build();
    }

    private void loginAs(User user) {
        var principal = new UserPrincipal(user);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

}
