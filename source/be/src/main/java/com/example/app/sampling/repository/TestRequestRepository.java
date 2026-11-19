package com.example.app.sampling.repository;
import com.example.app.sampling.entity.TestRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
public interface TestRequestRepository extends JpaRepository<TestRequest,Long>,JpaSpecificationExecutor<TestRequest>{ Optional<TestRequest> findByRequestNumberIgnoreCase(String number); boolean existsByRequestNumberIgnoreCase(String number); }
