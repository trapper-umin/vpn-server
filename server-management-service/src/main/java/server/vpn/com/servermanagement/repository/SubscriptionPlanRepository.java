package server.vpn.com.servermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.servermanagement.entity.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    /**
     * Поиск планов по ID сервера
     */
    List<SubscriptionPlan> findByServer_IdOrderBySortOrderAscCreatedAtAsc(UUID serverId);

    /**
     * Поиск активных планов по ID сервера
     */
    List<SubscriptionPlan> findByServer_IdAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc(UUID serverId);

    /**
     * Поиск планов по ID продавца через сервер
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId ORDER BY p.server.name, p.sortOrder, p.createdAt")
    List<SubscriptionPlan> findBySellerIdOrderByServerNameAndSortOrder(@Param("sellerId") UUID sellerId);

    /**
     * Поиск активных планов по ID продавца через сервер
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId AND p.isActive = true ORDER BY p.server.name, p.sortOrder, p.createdAt")
    List<SubscriptionPlan> findBySellerIdAndIsActiveTrueOrderByServerNameAndSortOrder(@Param("sellerId") UUID sellerId);

    /**
     * Поиск плана по ID и ID продавца
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.id = :planId AND p.server.sellerId = :sellerId")
    Optional<SubscriptionPlan> findByIdAndSellerId(@Param("planId") UUID planId, @Param("sellerId") UUID sellerId);

    /**
     * Подсчет планов по ID сервера
     */
    long countByServer_Id(UUID serverId);

    /**
     * Подсчет активных планов по ID сервера
     */
    long countByServer_IdAndIsActiveTrue(UUID serverId);

    /**
     * Суммарный доход по планам продавца
     */
    @Query("SELECT COALESCE(SUM(p.totalRevenue), 0) FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId AND p.isActive = true")
    BigDecimal getTotalRevenueBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Месячный доход по планам продавца
     */
    @Query("SELECT COALESCE(SUM(p.monthlyRevenue), 0) FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId AND p.isActive = true")
    BigDecimal getMonthlyRevenueBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Общее количество подписчиков по планам продавца
     */
    @Query("SELECT COALESCE(SUM(p.totalSubscribers), 0) FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId AND p.isActive = true")
    Integer getTotalSubscribersBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Активные подписчики по планам продавца
     */
    @Query("SELECT COALESCE(SUM(p.activeSubscribers), 0) FROM SubscriptionPlan p WHERE p.server.sellerId = :sellerId AND p.isActive = true")
    Integer getActiveSubscribersBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Проверка существования плана с таким именем у сервера
     */
    boolean existsByServer_IdAndNameIgnoreCase(UUID serverId, String name);

    /**
     * Поиск популярных планов
     */
    List<SubscriptionPlan> findByIsPopularTrueAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc();

    /**
     * Поиск планов для маркетплейса (активные планы с активными серверами)
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.isActive = true AND p.server.isActive = true AND p.server.isOnline = true ORDER BY p.isPopular DESC, p.sortOrder ASC, p.createdAt ASC")
    List<SubscriptionPlan> findActiveMarketplacePlans();
}