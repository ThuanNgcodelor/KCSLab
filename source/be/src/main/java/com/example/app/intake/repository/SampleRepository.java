package com.example.app.intake.repository;

import com.example.app.intake.entity.Sample;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SampleRepository extends JpaRepository<Sample, Long>, JpaSpecificationExecutor<Sample> {
    long countByReceivedDate(LocalDate receivedDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sample s where s.id = :id")
    Optional<Sample> findByIdForUpdate(@Param("id") Long id);
}
