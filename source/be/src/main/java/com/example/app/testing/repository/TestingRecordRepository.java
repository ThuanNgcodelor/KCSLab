package com.example.app.testing.repository;

import com.example.app.testing.entity.TestingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestingRecordRepository extends JpaRepository<TestingRecord, Long> {
    boolean existsByTransferItem_Id(Long transferItemId);
    List<TestingRecord> findTop50ByOrderByCreatedAtDesc();
}
