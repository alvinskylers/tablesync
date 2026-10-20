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

    // Creation Tests
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

    // Cancellation Tests
    @Test
    void cancellation_customerCancelOwnReservation_allowed() {
        var owner = userWithRole(Role.CUSTOMER);
        var reservation = reservationOf(owner, Status.CONFIRMED);
        givenReservationExists(reservation);
        loginAs(owner);

        assertDoesNotThrow(() -> validator.validateReservationCancellation(reservation.getId()));
    }

    @Test
    void cancellation_customerCancelOthersReservation_throwsAuthorizationDenied() {
        var owner = userWithRole(Role.CUSTOMER);
        var otherCustomer = userWithRole(Role.CUSTOMER);
        var reservation = reservationOf(owner, Status.CONFIRMED);
        givenReservationExists(reservation);
        loginAs(otherCustomer);

        assertThrows(AuthorizationDeniedException.class,
                () -> validator.validateReservationCancellation(reservation.getId()));
    }

    @Test
    void cancellation_staffCancelAnyReservation() {
        var owner = userWithRole(Role.CUSTOMER);
        var staff = userWithRole(Role.WAITER);
        var reservation = reservationOf(owner, Status.CONFIRMED);
        givenReservationExists(reservation);
        loginAs(staff);

        assertDoesNotThrow(() -> validator.validateReservationCancellation(reservation.getId()) );
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names ={"CANCELLED", "COMPLETED", "REJECTED"})
    void cancellation_terminalStatus_throwsStatusConflict(Status status) {
        var customer = userWithRole(Role.CUSTOMER);
        var reservation = reservationOf(customer, status);
        givenReservationExists(reservation);
        loginAs(customer);

        assertThrows(ReservationUpdateStatusException.class,
                () -> validator.validateReservationCancellation(reservation.getId()));
    }

    @Test
    void cancellation_reservationNotFound_throwsNotFound() {
        var id = UUID.randomUUID();
        when(reservationRepository.findById(id))
                .thenReturn(Optional.empty());
        loginAs(userWithRole(Role.CUSTOMER));
        assertThrows(ReservationNotFoundException.class,
                () -> validator.validateReservationUpdate(id));
    }

    // Update Tests
    @Test
    void update_customer_throwsAuthorizationDenied() {
        var customer = userWithRole(Role.CUSTOMER);
        var reservation = reservationOf(customer , Status.CONFIRMED);
        givenReservationExists(reservation);
        loginAs(customer);

        assertThrows(AuthorizationDeniedException.class,
                () -> validator.validateReservationUpdate(reservation.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"COMPLETED", "CANCELLED", "REJECTED"})
    void update_staffTerminalStatus_throwsStatusConflict(Status status) {
        var reservation = reservationOf(userWithRole(Role.CUSTOMER), status);
        givenReservationExists(reservation);
        loginAs(userWithRole(Role.WAITER));

        assertThrows(ReservationUpdateStatusException.class,
                () -> validator.validateReservationCancellation(reservation.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"PENDING", "CONFIRMED"})
    void update_staffOnActiveStatus_allowed(Status status) {
        var reservation = reservationOf(userWithRole(Role.CUSTOMER), status);
        givenReservationExists(reservation);
        loginAs(userWithRole(Role.WAITER));

        assertDoesNotThrow(() -> validator.validateReservationUpdate(reservation.getId()));
    }

    @Test
    void update_reservationNotFound_throwsNotFound() {
        var id = UUID.randomUUID();
        when(reservationRepository.findById(id))
                .thenReturn(Optional.empty());
        loginAs(userWithRole(Role.WAITER));
        assertThrows(ReservationNotFoundException.class,
                () -> validator.validateReservationUpdate(id));
    }

    // Helper Methods
    private void givenReservationExists(Reservation reservation) {
        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
    }

    private Reservation reservationOf(User customer, Status status) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .status(status)
                .build();
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
