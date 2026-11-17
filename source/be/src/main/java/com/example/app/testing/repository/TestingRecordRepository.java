package com.example.app.testing.repository;

import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.entity.TestingRecord;
import com.example.app.transfer.entity.SampleTransferItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TestingRecordRepository extends JpaRepository<TestingRecord, Long>, JpaSpecificationExecutor<TestingRecord> {
    boolean existsByTransferItem_Id(Long transferItemId);
    List<TestingRecord> findTop50ByOrderByCreatedAtDesc();

    /** Samples the receiving lab accepted that still have no testing record. */
    @Query("""
            select i from SampleTransferItem i
            where i.transfer.status = com.example.app.transfer.entity.TransferStatus.ACCEPTED
              and i.sample.status = com.example.app.intake.entity.ReceiptStatus.TRANSFERRED
              and i.sample.codingStatus = com.example.app.intake.entity.SampleCodingStatus.VERIFIED
              and not exists (select 1 from TestingRecord r where r.transferItem = i)
            """)
    Page<SampleTransferItem> findCandidates(Pageable pageable);
}
