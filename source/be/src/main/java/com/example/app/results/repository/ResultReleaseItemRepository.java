package com.example.app.results.repository;

import com.example.app.results.entity.ResultReleaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResultReleaseItemRepository extends JpaRepository<ResultReleaseItem, Long> {
    List<ResultReleaseItem> findByRelease_IdOrderByRowOrderAsc(Long releaseId);

    long countByRelease_Id(Long releaseId);

    @Modifying
    @Query("delete from ResultReleaseItem i where i.release.id = :releaseId")
    void deleteByReleaseId(@Param("releaseId") Long releaseId);
}
