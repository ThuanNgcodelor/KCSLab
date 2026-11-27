package com.example.app.transfer.repository;

import com.example.app.transfer.entity.TransferAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TransferAssigneeRepository extends JpaRepository<TransferAssignee, Long> {
    List<TransferAssignee> findByTransfer_IdInOrderByIdAsc(Collection<Long> transferIds);
    List<TransferAssignee> findByTransfer_IdOrderByIdAsc(Long transferId);
}
