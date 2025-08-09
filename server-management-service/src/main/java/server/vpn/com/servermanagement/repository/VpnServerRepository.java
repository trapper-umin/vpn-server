package server.vpn.com.servermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.servermanagement.entity.VpnServer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VpnServerRepository extends JpaRepository<VpnServer, UUID> {

    /**
     * Поиск серверов по ID продавца
     */
    List<VpnServer> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    /**
     * Поиск активных серверов продавца
     */
    List<VpnServer> findBySellerIdAndIsActiveTrueOrderByCreatedAtDesc(UUID sellerId);

    /**
     * Поиск сервера по ID и ID продавца
     */
    Optional<VpnServer> findByIdAndSellerId(UUID serverId, UUID sellerId);

    /**
     * Подсчет общего количества серверов продавца
     */
    long countBySellerIdAndIsActiveTrue(UUID sellerId);

    /**
     * Подсчет активных серверов продавца
     */
    long countBySellerIdAndIsActiveTrueAndIsOnlineTrue(UUID sellerId);

    /**
     * Суммарный доход продавца
     */
    @Query("SELECT COALESCE(SUM(s.totalRevenue), 0) FROM VpnServer s WHERE s.sellerId = :sellerId AND s.isActive = true")
    BigDecimal getTotalRevenueBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Месячный доход продавца
     */
    @Query("SELECT COALESCE(SUM(s.monthlyRevenue), 0) FROM VpnServer s WHERE s.sellerId = :sellerId AND s.isActive = true")
    BigDecimal getMonthlyRevenueBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Общее количество подписчиков продавца
     */
    @Query("SELECT COALESCE(SUM(s.totalSubscribers), 0) FROM VpnServer s WHERE s.sellerId = :sellerId AND s.isActive = true")
    Integer getTotalSubscribersBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Активные подписчики продавца
     */
    @Query("SELECT COALESCE(SUM(s.activeSubscribers), 0) FROM VpnServer s WHERE s.sellerId = :sellerId AND s.isActive = true")
    Integer getActiveSubscribersBySellerIdAndIsActiveTrue(@Param("sellerId") UUID sellerId);

    /**
     * Проверка существования сервера с таким именем у продавца
     */
    boolean existsBySellerIdAndNameIgnoreCase(UUID sellerId, String name);
}