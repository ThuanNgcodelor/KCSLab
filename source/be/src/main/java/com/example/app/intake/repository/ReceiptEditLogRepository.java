package com.example.app.intake.repository;

import com.example.app.intake.entity.ReceiptEditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptEditLogRepository extends JpaRepository<ReceiptEditLog, Long> {
    List<ReceiptEditLog> findByReceipt_IdOrderByIdDesc(Long receiptId);
}
