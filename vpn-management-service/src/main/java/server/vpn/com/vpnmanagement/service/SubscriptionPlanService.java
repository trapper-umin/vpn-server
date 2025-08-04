package server.vpn.com.vpnmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.vpnmanagement.client.UserManagementClient;
import server.vpn.com.vpnmanagement.dto.CreateSubscriptionPlanRequest;
import server.vpn.com.vpnmanagement.dto.SubscriptionPlanResponse;
import server.vpn.com.vpnmanagement.dto.UserProfileDto;
import server.vpn.com.vpnmanagement.entity.*;
import server.vpn.com.vpnmanagement.mapper.SubscriptionPlanMapper;
import server.vpn.com.vpnmanagement.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerRepository vpnServerRepository;
    private final VpnSubscriptionRepository vpnSubscriptionRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final UserManagementClient userManagementClient;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    /**
     * Получение планов подписки продавца
     */
    public List<SubscriptionPlanResponse> getSellerPlans(UUID sellerId) {
        log.info("Получение планов подписки для продавца: {}", sellerId);
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findBySellerIdOrderByServerNameAndSortOrder(sellerId);
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(plans);
    }

    /**
     * Получение планов подписки по серверу
     */
    public List<SubscriptionPlanResponse> getServerPlans(UUID sellerId, UUID serverId) {
        log.info("Получение планов для сервера {} продавца: {}", serverId, sellerId);
        
        // Проверяем, что сервер принадлежит продавцу
        vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден"));
        
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findByServer_IdOrderBySortOrderAscCreatedAtAsc(serverId);
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(plans);
    }

    /**
     * Создание нового плана подписки
     */
    @Transactional
    public SubscriptionPlanResponse createPlan(UUID sellerId, UUID serverId, CreateSubscriptionPlanRequest request) {
        log.info("Создание плана подписки для сервера {} продавца: {}", serverId, sellerId);

        // Проверяем, что сервер принадлежит продавцу
        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Сервер не найден"));

        // Проверяем уникальность имени плана для сервера
        if (subscriptionPlanRepository.existsByServer_IdAndNameIgnoreCase(serverId, request.getName())) {
            throw new IllegalArgumentException("План с таким названием уже существует для этого сервера");
        }

        SubscriptionPlan plan = subscriptionPlanMapper.toEntity(request);
        plan.setServer(server);

        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedPlan);
    }

    /**
     * Обновление плана подписки
     */
    @Transactional
    public SubscriptionPlanResponse updatePlan(UUID sellerId, UUID planId, CreateSubscriptionPlanRequest request) {
        log.info("Обновление плана {} для продавца: {}", planId, sellerId);

        SubscriptionPlan plan = subscriptionPlanRepository.findByIdAndSellerId(planId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("План не найден"));

        // Проверяем уникальность имени (если имя изменилось)
        if (!plan.getName().equalsIgnoreCase(request.getName()) && 
            subscriptionPlanRepository.existsByServer_IdAndNameIgnoreCase(plan.getServer().getId(), request.getName())) {
            throw new IllegalArgumentException("План с таким названием уже существует для этого сервера");
        }

        // Обновляем поля
        plan.setName(request.getName());
        plan.setType(SubscriptionPlan.PlanType.valueOf(request.getType().toUpperCase()));
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setYearlyPrice(request.getYearlyPrice());
        plan.setMaxConnections(request.getMaxConnections());
        plan.setBandwidthLimit(request.getBandwidthLimit());
        plan.setSpeedLimit(request.getSpeedLimit());
        plan.setIsPopular(request.getIsPopular());
        plan.setSortOrder(request.getSortOrder());
        plan.setFeatures(request.getFeatures());
        plan.setDescription(request.getDescription());

        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedPlan);
    }

    /**
     * Переключение статуса плана
     */
    @Transactional
    public SubscriptionPlanResponse togglePlanStatus(UUID sellerId, UUID planId) {
        log.info("Переключение статуса плана {} для продавца: {}", planId, sellerId);

        SubscriptionPlan plan = subscriptionPlanRepository.findByIdAndSellerId(planId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("План не найден"));

        plan.setIsActive(!plan.getIsActive());

        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedPlan);
    }

    /**
     * Удаление плана подписки
     */
    @Transactional
    public void deletePlan(UUID sellerId, UUID planId) {
        log.info("Удаление плана {} для продавца: {}", planId, sellerId);

        SubscriptionPlan plan = subscriptionPlanRepository.findByIdAndSellerId(planId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("План не найден"));

        // Проверяем, что у плана нет активных подписок
        if (plan.getActiveSubscribers() > 0) {
            throw new IllegalArgumentException("Нельзя удалить план с активными подписками");
        }

        subscriptionPlanRepository.delete(plan);
    }

    /**
     * Получение планов для маркетплейса
     */
    public List<SubscriptionPlanResponse> getMarketplacePlans() {
        log.info("Получение планов для маркетплейса");
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findActiveMarketplacePlans();
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(plans);
    }

    /**
     * Получение популярных планов
     */
    public List<SubscriptionPlanResponse> getPopularPlans() {
        log.info("Получение популярных планов");
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findByIsPopularTrueAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc();
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(plans);
    }

    /**
     * Покупка плана подписки
     */
    @Transactional
    public void purchasePlan(UUID userId, UUID planId, String billingCycle, String authHeader) {
        log.info("Покупка плана {} пользователем {} на период {}", planId, userId, billingCycle);
        
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("План не найден"));
        
        // Проверки доступности плана
        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("План недоступен для покупки");
        }
        
        // Проверяем, что план не превысил максимальное количество подписчиков (используем max_connections)
        if (plan.getActiveSubscribers() >= plan.getMaxConnections()) {
            throw new IllegalArgumentException("Достигнуто максимальное количество подписчиков для этого плана");
        }
        
        // Проверяем, что пользователь еще не подписан на этот план
        List<VpnSubscription> existingSubscriptions = vpnSubscriptionRepository
                .findByUserIdAndPlan_IdAndIsActiveTrue(userId, planId);
        if (!existingSubscriptions.isEmpty()) {
            throw new IllegalArgumentException("У вас уже есть активная подписка на этот план");
        }
        
        // Определяем цену и период подписки
        VpnSubscription.BillingCycle cycle = VpnSubscription.BillingCycle.valueOf(billingCycle.toUpperCase());
        BigDecimal price = cycle == VpnSubscription.BillingCycle.MONTHLY ? 
                plan.getMonthlyPrice() : plan.getYearlyPrice();
        
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = cycle == VpnSubscription.BillingCycle.MONTHLY ?
                startDate.plusMonths(1) : startDate.plusYears(1);
        
        // Получаем данные пользователя из user-management-service
        UserProfileDto userProfile = userManagementClient.getUserProfile(authHeader);
        if (userProfile == null || !userProfile.getIsActive()) {
            throw new IllegalArgumentException("Пользователь не найден или неактивен");
        }
        
        String userEmail = userProfile.getEmail();
        
        // Создаем подписку
        VpnSubscription subscription = VpnSubscription.builder()
                .userId(userId)
                .userEmail(userEmail)
                .plan(plan)
                .billingCycle(cycle)
                .startDate(startDate)
                .endDate(endDate)
                .totalPaid(price)
                .isActive(true)
                .build();
        
        vpnSubscriptionRepository.save(subscription);
        
        // Обновляем статистику плана
        plan.setTotalSubscribers(plan.getTotalSubscribers() + 1);
        plan.setActiveSubscribers(plan.getActiveSubscribers() + 1);
        plan.setTotalRevenue(plan.getTotalRevenue().add(price));
        if (cycle == VpnSubscription.BillingCycle.MONTHLY) {
            plan.setMonthlyRevenue(plan.getMonthlyRevenue().add(price));
        }
        subscriptionPlanRepository.save(plan);
        
        // Обновляем статистику сервера
        VpnServer server = plan.getServer();
        server.setTotalSubscribers(server.getTotalSubscribers() + 1);
        server.setActiveSubscribers(server.getActiveSubscribers() + 1);
        server.setTotalRevenue(server.getTotalRevenue().add(price));
        if (cycle == VpnSubscription.BillingCycle.MONTHLY) {
            server.setMonthlyRevenue(server.getMonthlyRevenue().add(price));
        }
        vpnServerRepository.save(server);
        
        // Создаем или обновляем запись в sales_records
        LocalDate today = LocalDate.now();
        UUID sellerId = server.getSellerId();
        
        SalesRecord salesRecord = salesRecordRepository.findBySellerIdAndSaleDate(sellerId, today)
                .orElse(SalesRecord.builder()
                        .sellerId(sellerId)
                        .saleDate(today)
                        .revenue(BigDecimal.ZERO)
                        .newSubscribers(0)
                        .refunds(0)
                        .build());
        
        salesRecord.setRevenue(salesRecord.getRevenue().add(price));
        salesRecord.setNewSubscribers(salesRecord.getNewSubscribers() + 1);
        salesRecordRepository.save(salesRecord);
        
        log.info("План {} успешно приобретен пользователем {} за {}", planId, userId, price);
    }
}