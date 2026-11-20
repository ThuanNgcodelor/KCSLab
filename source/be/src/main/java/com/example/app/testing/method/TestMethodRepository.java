package com.example.app.testing.method;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestMethodRepository extends JpaRepository<TestMethod, Long>, JpaSpecificationExecutor<TestMethod> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
