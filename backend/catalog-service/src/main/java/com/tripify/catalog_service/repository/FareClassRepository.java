package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.FareClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareClassRepository extends JpaRepository<FareClass, Long> {

    List<FareClass> findByFlightId(Long flightId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fc FROM FareClass fc WHERE fc.id = :id")
    Optional<FareClass> findByIdForUpdate(@Param("id") Long id);
}
