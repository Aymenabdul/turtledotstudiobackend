package com.turtledot.studio.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeePermissionRepository extends JpaRepository<EmployeePermission, Long> {
    List<EmployeePermission> findByEmployeeId(Long employeeId);

    @Modifying
    @Query("DELETE FROM EmployeePermission p WHERE p.employeeId = :employeeId")
    void deleteAllByEmployeeId(Long employeeId);
}
