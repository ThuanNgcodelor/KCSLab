package com.example.app.intake.repository;

import com.example.app.intake.entity.SampleCodeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleCodeEventRepository extends JpaRepository<SampleCodeEvent, Long> {
    List<SampleCodeEvent> findBySample_IdOrderByIdAsc(Long sampleId);
}
