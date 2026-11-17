package com.example.app.results.repository;

import com.example.app.results.entity.ResultRelease;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultReleaseRepository extends JpaRepository<ResultRelease, Long> {
    boolean existsByTestingRecord_IdAndVersion(Long testingRecordId, int version);
}
