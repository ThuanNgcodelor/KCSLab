package com.example.app.transfer.repository;

import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.transfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleTransferItemRepository extends JpaRepository<SampleTransferItem, Long> {
    boolean existsBySample_IdAndTransfer_StatusNot(Long sampleId, TransferStatus status);
}
