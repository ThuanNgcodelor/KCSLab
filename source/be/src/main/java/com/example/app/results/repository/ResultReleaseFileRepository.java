package com.example.app.results.repository;

import com.example.app.results.entity.ResultReleaseFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultReleaseFileRepository extends JpaRepository<ResultReleaseFile, Long> {
    List<ResultReleaseFile> findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(Long releaseId);

    long countByRelease_IdAndRemovedAtIsNull(Long releaseId);

    Optional<ResultReleaseFile> findByIdAndRelease_Id(Long id, Long releaseId);
}
