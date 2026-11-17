package com.example.app.results.repository;

import com.example.app.results.entity.ResultRelease;
import com.example.app.results.entity.ResultReleaseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResultReleaseRepository extends JpaRepository<ResultRelease, Long> {
    boolean existsByTestingRecord_IdAndVersion(Long testingRecordId, int version);

    boolean existsByTestingRecord_IdAndStatus(Long testingRecordId, ResultReleaseStatus status);

    boolean existsByLookupCode(String lookupCode);

    boolean existsByResultNumber(String resultNumber);

    @Query("select coalesce(max(r.version), 0) from ResultRelease r where r.testingRecord.id = :recordId")
    int maxVersion(@Param("recordId") Long recordId);

    @Query("select r from ResultRelease r where r.testingRecord.id = :recordId and r.version = "
            + "(select max(x.version) from ResultRelease x where x.testingRecord.id = :recordId)")
    Optional<ResultRelease> findLatestForRecord(@Param("recordId") Long recordId);

    Optional<ResultRelease> findByLookupCode(String lookupCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResultRelease r where r.id = :id")
    Optional<ResultRelease> findByIdForUpdate(@Param("id") Long id);

    @Query("select r from ResultRelease r where (:status is null or r.status = :status)")
    Page<ResultRelease> search(@Param("status") ResultReleaseStatus status, Pageable pageable);
}
