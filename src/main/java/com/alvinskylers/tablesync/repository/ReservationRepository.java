package com.alvinskylers.tablesync.repository;

import com.alvinskylers.tablesync.entity.Reservation;
import com.alvinskylers.tablesync.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Reservation> findByTableId(UUID tableId, Pageable pageable);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.table.id = :tableId " +
            "AND r.status IN :statuses " +
            "AND r.reservationStart < :end " +
            "AND r.reservationEnd > :start")
    List<Reservation> findReservationsForTableInRange(
            @Param("tableId") UUID tableId,
            @Param("statuses") List<Status> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.table.id = :tableId " +
            "AND r.status IN :blockingStatuses " +
            "AND r.reservationStart < :newEnd " +
            "AND r.reservationEnd > :newStart")
    List<Reservation> findOverlappingReservations(
            @Param("tableId") UUID tableId,
            @Param("blockingStatuses") List<Status> blockingStatuses,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd);

}
