package server.vpn.com.vpnmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.vpnmanagement.dto.*;
import server.vpn.com.vpnmanagement.entity.SalesRecord;
import server.vpn.com.vpnmanagement.entity.VpnServer;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;
import server.vpn.com.vpnmanagement.mapper.SalesRecordMapper;
import server.vpn.com.vpnmanagement.mapper.VpnServerMapper;
import server.vpn.com.vpnmanagement.mapper.VpnSubscriptionMapper;
import server.vpn.com.vpnmanagement.repository.SalesRecordRepository;
import server.vpn.com.vpnmanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.vpnmanagement.repository.VpnServerRepository;
import server.vpn.com.vpnmanagement.repository.VpnSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpnManagementService {

    private final VpnServerRepository vpnServerRepository;
    private final VpnSubscriptionRepository vpnSubscriptionRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerMapper vpnServerMapper;
    private final VpnSubscriptionMapper vpnSubscriptionMapper;
    private final SalesRecordMapper salesRecordMapper;

    /**
     * Получение статистики продавца
     */
    public SellerStatsResponse getSellerStats(UUID sellerId) {
        log.info("Получение статистики для продавца: {}", sellerId);

        // Получаем данные из репозиториев
        long totalServers = vpnServerRepository.countBySellerIdAndIsActiveTrue(sellerId);
        long activeServers = vpnServerRepository.countBySellerIdAndIsActiveTrueAndIsOnlineTrue(sellerId);
        BigDecimal totalRevenue = subscriptionPlanRepository.getTotalRevenueBySellerIdAndIsActiveTrue(sellerId);
        BigDecimal monthlyRevenue = subscriptionPlanRepository.getMonthlyRevenueBySellerIdAndIsActiveTrue(sellerId);
        Integer totalSubscribers = subscriptionPlanRepository.getTotalSubscribersBySellerIdAndIsActiveTrue(sellerId);
        Integer activeSubscribers = subscriptionPlanRepository.getActiveSubscribersBySellerIdAndIsActiveTrue(sellerId);

        // Заглушки для рейтинга и конверсии (потом можно добавить реальные данные)
        BigDecimal averageRating = BigDecimal.valueOf(4.7);
        Integer totalReviews = 127;
        BigDecimal conversionRate = BigDecimal.valueOf(23.5);
        BigDecimal churnRate = BigDecimal.valueOf(8.2);

        return SellerStatsResponse.builder()
                .totalServers((int) totalServers)
                .activeServers((int) activeServers)
                .totalSubscribers(totalSubscribers != null ? totalSubscribers : 0)
                .activeSubscribers(activeSubscribers != null ? activeSubscribers : 0)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .conversionRate(conversionRate)
                .churnRate(churnRate)
                .build();
    }

    /**
     * Получение списка серверов продавца
     */
    public List<SellerServerResponse> getSellerServers(UUID sellerId) {
        log.info("Получение серверов для продавца: {}", sellerId);
        List<VpnServer> servers = vpnServerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return vpnServerMapper.toSellerServerResponseList(servers);
    }

    /**
     * Получение подписчиков продавца
     */
    public List<SellerSubscriberResponse> getSellerSubscribers(UUID sellerId) {
        log.info("Получение подписчиков для продавца: {}", sellerId);
        List<VpnSubscription> subscriptions = vpnSubscriptionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return vpnSubscriptionMapper.toSellerSubscriberResponseList(subscriptions);
    }

    /**
     * Получение данных о продажах за период
     */
    public List<SalesDataResponse> getSalesData(UUID sellerId, int days) {
        log.info("Получение данных о продажах для продавца: {} за {} дней", sellerId, days);
        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        List<SalesRecord> salesRecords = salesRecordRepository.findBySellerIdAndSaleDateAfterOrderBySaleDateAsc(sellerId, startDate);
        return salesRecordMapper.toSalesDataResponseList(salesRecords);
    }

    /**
     * Создание нового сервера
     */
    @Transactional
    public SellerServerResponse createServer(UUID sellerId, CreateServerRequest request) {
        log.info("Создание сервера для продавца: {}", sellerId);

        // Проверка уникальности имени сервера
        if (vpnServerRepository.existsBySellerIdAndNameIgnoreCase(sellerId, request.getName())) {
            throw new IllegalArgumentException("Сервер с таким названием уже существует");
        }

        VpnServer server = vpnServerMapper.toEntity(request);
        server.setSellerId(sellerId);
        
        // Устанавливаем дефолтные значения
        server.setIpAddress("0.0.0.0"); // Будет установлен при настройке
        server.setPort(51820); // WireGuard default port
        server.setProtocols(List.of("WireGuard", "OpenVPN"));
        server.setStatus(VpnServer.ServerStatus.SETUP);

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }

    /**
     * Переключение статуса сервера
     */
    @Transactional
    public SellerServerResponse toggleServerStatus(UUID sellerId, UUID serverId) {
        log.info("Переключение статуса сервера {} для продавца: {}", serverId, sellerId);

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден"));

        server.setIsActive(!server.getIsActive());
        server.setIsOnline(server.getIsActive());
        server.setStatus(server.getIsActive() ? VpnServer.ServerStatus.ACTIVE : VpnServer.ServerStatus.INACTIVE);

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }

    /**
     * Удаление сервера
     */
    @Transactional
    public void deleteServer(UUID sellerId, UUID serverId) {
        log.info("Удаление сервера {} для продавца: {}", serverId, sellerId);

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден"));

        // Проверка, что у сервера нет активных планов с подписками
        long activePlansWithSubscriptions = subscriptionPlanRepository.findByServer_IdAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc(serverId)
                .stream()
                .mapToLong(plan -> plan.getActiveSubscribers())
                .sum();
        if (activePlansWithSubscriptions > 0) {
            throw new IllegalArgumentException("Нельзя удалить сервер с активными подписками");
        }

        vpnServerRepository.delete(server);
    }

    /**
     * Обновление сервера
     */
    @Transactional
    public SellerServerResponse updateServer(UUID sellerId, UUID serverId, CreateServerRequest request) {
        log.info("Обновление сервера {} для продавца: {}", serverId, sellerId);

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден"));

        // Проверка уникальности имени (если имя изменилось)
        if (!server.getName().equalsIgnoreCase(request.getName()) && 
            vpnServerRepository.existsBySellerIdAndNameIgnoreCase(sellerId, request.getName())) {
            throw new IllegalArgumentException("Сервер с таким названием уже существует");
        }

        // Обновляем поля
        server.setName(request.getName());
        server.setCountry(request.getCountry());
        server.setCountryCode(request.getCountryCode());
        server.setCity(request.getCity());
        server.setMaxConnections(request.getMaxConnections());
        server.setBandwidth(request.getBandwidth());
        server.setSpeed(request.getSpeed());
        server.setDescription(request.getDescription());
        server.setFeatures(request.getFeatures());

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }

    /**
     * Проверка подключения к серверу
     */
    public ServerConnectionResponse testServerConnection(UUID sellerId, ServerConnectionRequest request) {
        log.info("Тестирование подключения к серверу {} для продавца: {}", request.getIp(), sellerId);
        
        // Здесь должна быть реальная логика подключения к серверу
        // Пока возвращаем мок данные
        try {
            // Симуляция проверки подключения
            Thread.sleep(2000); // Имитация задержки
            
            ServerConnectionResponse.ServerInfo serverInfo = ServerConnectionResponse.ServerInfo.builder()
                    .ip(request.getIp())
                    .os("Ubuntu 22.04 LTS")
                    .region("Unknown")
                    .provider("Generic VPS")
                    .build();
            
            return ServerConnectionResponse.builder()
                    .success(true)
                    .serverInfo(serverInfo)
                    .build();
                    
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ServerConnectionResponse.builder()
                    .success(false)
                    .error("Прервано тестирование подключения")
                    .build();
        } catch (Exception e) {
            log.error("Ошибка при тестировании подключения к серверу: {}", e.getMessage());
            return ServerConnectionResponse.builder()
                    .success(false)
                    .error("Не удалось подключиться к серверу: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Развертывание WireGuard на сервере
     */
    public WireGuardDeploymentResponse deployWireGuard(UUID sellerId, String serverIp) {
        log.info("Развертывание WireGuard на сервере {} для продавца: {}", serverIp, sellerId);
        
        // Здесь должна быть реальная логика развертывания WireGuard
        // Пока возвращаем мок данные
        try {
            List<WireGuardDeploymentResponse.DeploymentStep> steps = List.of(
                WireGuardDeploymentResponse.DeploymentStep.builder()
                        .id("system_update")
                        .name("Обновление системы")
                        .status("completed")
                        .details("Система успешно обновлена")
                        .logs(List.of("apt update", "apt upgrade -y"))
                        .build(),
                WireGuardDeploymentResponse.DeploymentStep.builder()
                        .id("wireguard_install")
                        .name("Установка WireGuard")
                        .status("completed")
                        .details("WireGuard успешно установлен")
                        .logs(List.of("apt install wireguard -y"))
                        .build(),
                WireGuardDeploymentResponse.DeploymentStep.builder()
                        .id("config_setup")
                        .name("Настройка конфигурации")
                        .status("completed")
                        .details("Конфигурация настроена")
                        .logs(List.of("Generated server keys", "Created wg0.conf"))
                        .build()
            );

            return WireGuardDeploymentResponse.builder()
                    .success(true)
                    .steps(steps)
                    .build();
                    
        } catch (Exception e) {
            log.error("Ошибка при развертывании WireGuard: {}", e.getMessage());
            return WireGuardDeploymentResponse.builder()
                    .success(false)
                    .error("Ошибка развертывания: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Тестирование готовности сервера
     */
    public ServerTestingResponse testServerReadiness(UUID sellerId, String serverIp) {
        log.info("Тестирование готовности сервера {} для продавца: {}", serverIp, sellerId);
        
        // Здесь должна быть реальная логика тестирования сервера
        // Пока возвращаем мок данные
        try {
            List<ServerTestingResponse.TestResult> tests = List.of(
                ServerTestingResponse.TestResult.builder()
                        .id("connectivity")
                        .name("Проверка подключения")
                        .status("passed")
                        .details("Сервер доступен")
                        .logs(List.of("ping successful", "ssh connection ok"))
                        .build(),
                ServerTestingResponse.TestResult.builder()
                        .id("wireguard_status")
                        .name("Статус WireGuard")
                        .status("passed")
                        .details("WireGuard запущен и работает")
                        .logs(List.of("systemctl status wg-quick@wg0", "interface wg0 is up"))
                        .build(),
                ServerTestingResponse.TestResult.builder()
                        .id("port_check")
                        .name("Проверка портов")
                        .status("passed")
                        .details("Порт 51820 доступен")
                        .logs(List.of("netstat -an | grep 51820", "port is listening"))
                        .build()
            );

            return ServerTestingResponse.builder()
                    .success(true)
                    .tests(tests)
                    .overallStatus("passed")
                    .build();
                    
        } catch (Exception e) {
            log.error("Ошибка при тестировании сервера: {}", e.getMessage());
            return ServerTestingResponse.builder()
                    .success(false)
                    .overallStatus("failed")
                    .tests(List.of())
                    .build();
        }
    }
}