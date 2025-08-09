package server.vpn.com.servermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.servermanagement.entity.SalesRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesRecordRepository extends JpaRepository<SalesRecord, UUID> {

    /**
     * Поиск записей продаж по ID продавца за период
     */
    List<SalesRecord> findBySellerIdAndSaleDateBetweenOrderBySaleDateAsc(
            UUID sellerId, 
            LocalDate startDate, 
            LocalDate endDate
    );

    /**
     * Поиск записей продаж по ID продавца за последние дни
     */
    @Query("SELECT s FROM SalesRecord s WHERE s.sellerId = :sellerId AND s.saleDate >= :startDate ORDER BY s.saleDate ASC")
    List<SalesRecord> findBySellerIdAndSaleDateAfterOrderBySaleDateAsc(
            @Param("sellerId") UUID sellerId, 
            @Param("startDate") LocalDate startDate
    );

    /**
     * Поиск записи продаж по ID продавца и дате
     */
    Optional<SalesRecord> findBySellerIdAndSaleDate(UUID sellerId, LocalDate saleDate);

    /**
     * Поиск последних записей продаж по ID продавца
     */
    List<SalesRecord> findTop30BySellerIdOrderBySaleDateDesc(UUID sellerId);

    /**
     * Проверка существования записи за определенную дату для продавца
     */
    boolean existsBySellerIdAndSaleDate(UUID sellerId, LocalDate saleDate);
}