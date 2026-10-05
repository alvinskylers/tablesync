    package com.alvinskylers.tablesync.validator;

    import com.alvinskylers.tablesync.dto.reservation.CreateReservationRequest;
    import com.alvinskylers.tablesync.entity.Reservation;
    import com.alvinskylers.tablesync.entity.User;
    import com.alvinskylers.tablesync.entity.enums.Role;
    import com.alvinskylers.tablesync.entity.enums.Status;
    import com.alvinskylers.tablesync.exception.*;
    import com.alvinskylers.tablesync.repository.ReservationRepository;
    import com.alvinskylers.tablesync.security.UserPrincipal;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.authorization.AuthorizationDeniedException;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Component;

    import java.util.List;
    import java.util.UUID;


    @Component
    @RequiredArgsConstructor
    public class ReservationValidator {

        private final ReservationRepository reservationRepository;
        private static final List<Status> BLOCKED_STATUSES = List.of(Status.PENDING, Status.CONFIRMED);

        public void validateReservationCreation(CreateReservationRequest request) {
            if (!request.reservationEnd().isAfter(request.reservationStart())) {
                throw new InvalidReservationTimeException("Reservation end time must be after start time. ");
            }
            var list = reservationRepository.findOverlappingReservations(
                    request.tableId(),
                    BLOCKED_STATUSES,
                    request.reservationStart(),
                    request.reservationEnd());
            if (!list.isEmpty()) {
                throw new TableReservedException("this table is currently reserved for the requested time.");
            }
        }

          public void validateReservationCancellation(UUID reservationId) {
            var reservation = findReservationById(reservationId);
            var user = getCurrentUser();

            if (user.getRole() == Role.CUSTOMER && !reservation.getCustomer().getId().equals(user.getId())) {
                throw new AuthorizationDeniedException("Current customer did not make this reservation.");
            }

            switch (reservation.getStatus()) {
                case Status.CANCELLED -> throw new ReservationUpdateStatusException("Cannot cancel a cancelled reservation");
                case Status.COMPLETED -> throw new ReservationUpdateStatusException("Cannot cancel a completed reservation");
                case Status.REJECTED -> throw new ReservationUpdateStatusException("Cannot cancel a rejected reservation");
            }
        }

        public void validateReservationUpdate(UUID reservationId) {
            var user = getCurrentUser();
            var reservation = findReservationById(reservationId);
            if (user.getRole() == Role.CUSTOMER) {
                throw new AuthorizationDeniedException("Customers cannot update reservation statuses.");
            }
            switch (reservation.getStatus()) {
                case Status.CANCELLED -> throw new ReservationUpdateStatusException("Cannot update a cancelled reservation");
                case Status.COMPLETED -> throw new ReservationUpdateStatusException("Cannot update a completed reservation");
                case Status.REJECTED -> throw new ReservationUpdateStatusException("Cannot update a rejected reservation");
                default -> {}
            }
        }

        private User getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUser();
        }

        private Reservation findReservationById(UUID id) {
            return reservationRepository.findById(id)
                    .orElseThrow(() -> new ReservationNotFoundException("reservation not found with id: " + id));
        }
    }
