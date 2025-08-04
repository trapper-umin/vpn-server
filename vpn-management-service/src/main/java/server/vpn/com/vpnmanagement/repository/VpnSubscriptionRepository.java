package server.vpn.com.vpnmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;

import java.util.List;
import java.util.UUID;

@Repository
public interface VpnSubscriptionRepository extends JpaRepository<VpnSubscription, UUID> {

    /**
     * Поиск подписок по ID плана
     */
    List<VpnSubscription> findByPlan_IdOrderByCreatedAtDesc(UUID planId);

    /**
     * Поиск активных подписок по ID плана
     */
    List<VpnSubscription> findByPlan_IdAndIsActiveTrueOrderByCreatedAtDesc(UUID planId);

    /**
     * Поиск подписок по ID продавца через план и сервер
     */
    @Query("SELECT s FROM VpnSubscription s WHERE s.plan.server.sellerId = :sellerId ORDER BY s.createdAt DESC")
    List<VpnSubscription> findBySellerIdOrderByCreatedAtDesc(@Param("sellerId") UUID sellerId);

    /**
     * Поиск активных подписок по ID продавца через план и сервер
     */
    @Query("SELECT s FROM VpnSubscription s WHERE s.plan.server.sellerId = :sellerId AND s.isActive = true ORDER BY s.createdAt DESC")
    List<VpnSubscription> findBySellerIdAndIsActiveTrueOrderByCreatedAtDesc(@Param("sellerId") UUID sellerId);

    /**
     * Подсчет подписок по ID плана
     */
    long countByPlan_Id(UUID planId);

    /**
     * Подсчет активных подписок по ID плана
     */
    long countByPlan_IdAndIsActiveTrue(UUID planId);

    /**
     * Поиск подписки по пользователю и плану
     */
    List<VpnSubscription> findByUserIdAndPlan_Id(UUID userId, UUID planId);

    /**
     * Поиск активной подписки по пользователю и плану
     */
    List<VpnSubscription> findByUserIdAndPlan_IdAndIsActiveTrue(UUID userId, UUID planId);
}