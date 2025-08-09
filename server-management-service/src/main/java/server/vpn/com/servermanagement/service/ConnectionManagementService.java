package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.servermanagement.entity.SubscriptionPlan;
import server.vpn.com.servermanagement.entity.VpnServer;
import server.vpn.com.servermanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.servermanagement.repository.VpnServerRepository;

import java.util.List;
import java.util.UUID;

import static server.vpn.com.servermanagement.util.enums.ServerManagementConstants.*;

@Service
@RequiredArgsConstructor
public class ConnectionManagementService {

    private final VpnServerRepository vpnServerRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * Валидация максимального количества подписчиков плана относительно сервера
     * 
     * @param server VPN сервер
     * @param maxConnections максимальное количество подписчиков для плана
     * @throws IllegalArgumentException если лимит превышен
     */
    public void validatePlanMaxConnections(VpnServer server, Integer maxConnections) {
        if (maxConnections > server.getMaxConnections()) {
            throw new IllegalArgumentException(MAX_CONNECTIONS_EXCEEDS_SERVER_LIMIT);
        }
    }

    /**
     * Валидация общей вместимости сервера при создании/обновлении плана
     * 
     * @param server VPN сервер
     * @param planMaxConnections максимальное количество подписчиков для плана
     * @param excludePlanId ID плана, который исключается из расчета (при обновлении)
     * @throws IllegalArgumentException если вместимость превышена
     */
    public void validateServerCapacity(VpnServer server, Integer planMaxConnections, 
                                     UUID excludePlanId) {
        List<SubscriptionPlan> serverPlans = subscriptionPlanRepository
                .findByServer_IdAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc(server.getId());

        int totalMaxSubscribers = 0;
        
        for (SubscriptionPlan plan : serverPlans) {
            if (plan.getId().equals(excludePlanId)) {
                continue;
            }

            totalMaxSubscribers += plan.getMaxConnections();
        }

        totalMaxSubscribers += planMaxConnections;
        
        if (totalMaxSubscribers > server.getMaxConnections()) {
            throw new IllegalArgumentException(SERVER_CONNECTION_CAPACITY_EXCEEDED + 
                    " (требуется: " + totalMaxSubscribers + ", доступно: " + server.getMaxConnections() + ")");
        }
    }

    /**
     * Инкремент current_connections сервера при добавлении одного подписчика
     * 
     * @param server VPN сервер
     */
    @Transactional
    public void incrementServerConnections(VpnServer server) {
        server.setCurrentConnections(server.getCurrentConnections() + 1);
        vpnServerRepository.save(server);
    }

    /**
     * Декремент current_connections сервера при удалении одного подписчика
     * 
     * @param server VPN сервер
     */
    @Transactional
    public void decrementServerConnections(VpnServer server) {
        int newConnections = Math.max(0, server.getCurrentConnections() - 1);
        server.setCurrentConnections(newConnections);
        vpnServerRepository.save(server);
    }
}
