package com.example.app.transfer.repository;

import com.example.app.transfer.entity.SampleTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SampleTransferRepository extends JpaRepository<SampleTransfer, Long>, JpaSpecificationExecutor<SampleTransfer> {
    List<SampleTransfer> findTop50ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SampleTransfer t where t.id = :id")
    Optional<SampleTransfer> findByIdForDecision(@Param("id") Long id);
}
