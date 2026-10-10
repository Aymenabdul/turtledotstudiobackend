package com.turtledot.studio.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeClientAccessRepository extends JpaRepository<EmployeeClientAccess, Long> {
    List<EmployeeClientAccess> findByEmployeeId(Long employeeId);
    List<EmployeeClientAccess> findByClientId(Long clientId);
    List<EmployeeClientAccess> findByEventId(Long eventId);
    List<EmployeeClientAccess> findByEmployeeIdAndEventIdIsNull(Long employeeId);
    Optional<EmployeeClientAccess> findByEmployeeIdAndClientId(Long employeeId, Long clientId);
    Optional<EmployeeClientAccess> findByEmployeeIdAndEventId(Long employeeId, Long eventId);

    @Modifying
    @Query("DELETE FROM EmployeeClientAccess a WHERE a.employeeId = :employeeId AND a.clientId = :clientId")
    void deleteByEmployeeIdAndClientId(Long employeeId, Long clientId);

    @Modifying
    @Query("DELETE FROM EmployeeClientAccess a WHERE a.eventId = :eventId")
    void deleteByEventId(Long eventId);

    @Modifying
    @Query("DELETE FROM EmployeeClientAccess a WHERE a.employeeId = :employeeId AND a.eventId = :eventId")
    void deleteByEmployeeIdAndEventId(Long employeeId, Long eventId);

    @Modifying
    @Query("DELETE FROM EmployeeClientAccess a WHERE a.employeeId = :employeeId AND a.eventId IS NULL")
    void deleteByEmployeeIdAndEventIdIsNull(Long employeeId);
}
