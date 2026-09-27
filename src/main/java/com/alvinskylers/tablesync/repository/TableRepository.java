package com.alvinskylers.tablesync.repository;

import com.alvinskylers.tablesync.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TableRepository extends JpaRepository<RestaurantTable, UUID> {

    boolean existsByTableNumber(int tableNumber);

}
