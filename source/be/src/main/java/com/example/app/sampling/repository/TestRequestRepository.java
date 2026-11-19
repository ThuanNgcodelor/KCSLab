package com.example.app.sampling.repository;
import com.example.app.sampling.entity.TestRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TestRequestRepository extends JpaRepository<TestRequest,Long>{ Optional<TestRequest> findByRequestNumberIgnoreCase(String number); boolean existsByRequestNumberIgnoreCase(String number); }
