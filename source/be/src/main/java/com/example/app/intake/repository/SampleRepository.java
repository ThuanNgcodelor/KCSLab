package com.example.app.intake.repository;

import com.example.app.intake.entity.Sample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SampleRepository extends JpaRepository<Sample, Long> {
    long countByReceivedDate(LocalDate receivedDate);
}
