package com.example.app.transfer.repository;

import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.transfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SampleTransferItemRepository extends JpaRepository<SampleTransferItem, Long> {
    List<SampleTransferItem> findBySample_Id(Long sampleId);
    boolean existsBySample_IdAndTransfer_StatusNot(Long sampleId, TransferStatus status);
    boolean existsBySample_IdAndTransfer_StatusIn(Long sampleId, Collection<TransferStatus> statuses);
}
