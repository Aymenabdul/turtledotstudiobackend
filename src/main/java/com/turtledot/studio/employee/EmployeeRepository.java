package com.turtledot.studio.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserId(Long userId);
    Optional<Employee> findByIdAndStudioId(Long id, Long studioId);
    Page<Employee> findByStudioId(Long studioId, Pageable pageable);
    boolean existsByEmployeeCode(String employeeCode);
    long countByStudioId(Long studioId);
}
