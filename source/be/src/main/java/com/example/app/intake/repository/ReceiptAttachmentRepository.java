package com.example.app.intake.repository;

import com.example.app.intake.entity.ReceiptAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptAttachmentRepository extends JpaRepository<ReceiptAttachment, Long> {
    Optional<ReceiptAttachment> findByIdAndReceipt_Id(Long id, Long receiptId);
    List<ReceiptAttachment> findByReceipt_IdOrderByIdAsc(Long receiptId);
}
