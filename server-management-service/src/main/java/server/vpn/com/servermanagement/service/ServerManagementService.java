package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.servermanagement.dto.builder.ServerFieldsChanger;
import server.vpn.com.servermanagement.dto.mapper.VpnServerMapper;
import server.vpn.com.servermanagement.dto.request.CreateServerRequest;
import server.vpn.com.servermanagement.dto.response.SellerServerResponse;
import server.vpn.com.servermanagement.entity.SubscriptionPlan;
import server.vpn.com.servermanagement.entity.VpnServer;
import server.vpn.com.servermanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.servermanagement.repository.VpnServerRepository;
import server.vpn.com.servermanagement.util.JwtUtil;

import java.util.List;
import java.util.UUID;

import static server.vpn.com.servermanagement.util.enums.ServerManagementConstants.*;

@Service
@RequiredArgsConstructor
public class ServerManagementService {

    private final JwtUtil jwtUtil;
    private final VpnServerRepository vpnServerRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerMapper vpnServerMapper;


    @Transactional
    public SellerServerResponse createServer(Authentication authentication, CreateServerRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        if (vpnServerRepository.existsBySellerIdAndNameIgnoreCase(sellerId, request.getName())) {
            throw new IllegalArgumentException(SERVER_NAME_ALREADY_EXISTS);
        }

        VpnServer server = vpnServerMapper.toEntity(request);
        server.setSellerId(sellerId);

        server.setIpAddress("0.0.0.0");
        server.setPort(51820);
        server.setProtocols(List.of("WireGuard", "OpenVPN"));
        server.setStatus(VpnServer.ServerStatus.SETUP);

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }

    @Transactional
    public SellerServerResponse toggleServerStatus(Authentication authentication, UUID serverId) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SERVER_NOT_FOUND));

        server.setIsActive(!server.getIsActive());
        server.setIsOnline(server.getIsActive());
        server.setStatus(server.getIsActive() ? VpnServer.ServerStatus.ACTIVE : VpnServer.ServerStatus.INACTIVE);

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }

    @Transactional
    public void deleteServer(Authentication authentication, UUID serverId) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SERVER_NOT_FOUND));

        long activePlansWithSubscriptions = subscriptionPlanRepository.findByServer_IdAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc(serverId)
                .stream()
                .mapToLong(SubscriptionPlan::getActiveSubscribers)
                .sum();
        if (activePlansWithSubscriptions > 0) {
            throw new IllegalArgumentException(THERE_ARE_ACTIVE_SUBSCRIPTIONS);
        }

        vpnServerRepository.delete(server);
    }

    @Transactional
    public SellerServerResponse updateServer(Authentication authentication, UUID serverId, CreateServerRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SERVER_NOT_FOUND));

        if (!server.getName().equalsIgnoreCase(request.getName()) &&
                vpnServerRepository.existsBySellerIdAndNameIgnoreCase(sellerId, request.getName())) {
            throw new IllegalArgumentException(SERVER_NAME_ALREADY_EXISTS);
        }

        ServerFieldsChanger.changeServerFields(server, request);

        VpnServer savedServer = vpnServerRepository.save(server);
        return vpnServerMapper.toSellerServerResponse(savedServer);
    }
}
