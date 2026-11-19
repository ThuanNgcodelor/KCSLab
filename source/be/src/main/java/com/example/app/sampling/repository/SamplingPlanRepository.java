package com.example.app.sampling.repository;
import com.example.app.sampling.entity.SamplingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SamplingPlanRepository extends JpaRepository<SamplingPlan,Long>{Optional<SamplingPlan> findByTestRequest_Id(Long id);boolean existsByFormNumberIgnoreCase(String number);}
