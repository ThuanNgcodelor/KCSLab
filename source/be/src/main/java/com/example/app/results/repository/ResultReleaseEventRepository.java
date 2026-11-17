package com.example.app.results.repository;

import com.example.app.results.entity.ResultReleaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultReleaseEventRepository extends JpaRepository<ResultReleaseEvent, Long> {
    List<ResultReleaseEvent> findByRelease_IdOrderByIdAsc(Long releaseId);
}
