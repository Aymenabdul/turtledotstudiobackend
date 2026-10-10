package com.turtledot.studio.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

        Optional<Client> findByIdAndStudioId(Long id, Long studioId);

        Optional<Client> findByClientCodeAndStudioId(String clientCode, Long studioId);

        boolean existsByClientCode(String clientCode);

        @Query("SELECT c FROM Client c WHERE c.studioId = :studioId AND c.status != 'DELETED' " +
                        "AND (:status IS NULL OR c.status = :status) " +
                        "AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR c.phone LIKE CONCAT('%', :search, '%'))")
        Page<Client> searchClients(
                        @Param("studioId") Long studioId,
                        @Param("status") ClientStatus status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT c FROM Client c WHERE c.studioId = :studioId AND c.status != 'DELETED' " +
                        "AND c.id IN (SELECT a.clientId FROM EmployeeClientAccess a WHERE a.employeeId = :employeeId) "
                        +
                        "AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Client> findAssignedClientsForEmployee(
                        @Param("studioId") Long studioId,
                        @Param("employeeId") Long employeeId,
                        @Param("search") String search,
                        Pageable pageable);

        List<Client> findByStudioId(Long studioId);

        long countByStudioIdAndStatus(Long studioId, ClientStatus status);

        @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Client c WHERE c.studioId = :studioId AND c.status != 'DELETED'")
        java.math.BigDecimal sumTotalAmountByStudioId(@Param("studioId") Long studioId);
}
