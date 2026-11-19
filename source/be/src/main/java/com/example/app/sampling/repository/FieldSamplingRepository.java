package com.example.app.sampling.repository;
import com.example.app.sampling.entity.FieldSampling;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FieldSamplingRepository extends JpaRepository<FieldSampling,Long>{Optional<FieldSampling> findByTestRequest_Id(Long id);boolean existsByFormNumberIgnoreCase(String number);}
