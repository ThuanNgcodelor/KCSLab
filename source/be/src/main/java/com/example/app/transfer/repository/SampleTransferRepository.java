package com.example.app.transfer.repository;

import com.example.app.transfer.entity.SampleTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleTransferRepository extends JpaRepository<SampleTransfer, Long> {
    List<SampleTransfer> findTop50ByOrderByCreatedAtDesc();
}
