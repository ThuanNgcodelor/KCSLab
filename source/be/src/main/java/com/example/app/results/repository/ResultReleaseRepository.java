package com.example.app.results.repository;

import com.example.app.results.entity.ResultRelease;
import com.example.app.results.entity.ResultReleaseStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResultReleaseRepository extends JpaRepository<ResultRelease, Long>, JpaSpecificationExecutor<ResultRelease> {
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

    /** Reviewed testing records that have no result dossier yet (ready to start one). */
    @Query("""
            select t from TestingRecord t
            where t.status = com.example.app.testing.entity.TestRecordStatus.REVIEWED
              and not exists (select 1 from ResultRelease x where x.testingRecord = t)
            """)
    Page<com.example.app.testing.entity.TestingRecord> findReadyRecords(Pageable pageable);
}
