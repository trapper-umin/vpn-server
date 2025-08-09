package server.vpn.com.vpnmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.vpn.com.vpnmanagement.dto.response.SalesDataResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerServerResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerStatsResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerSubscriberResponse;
import server.vpn.com.vpnmanagement.service.SellerDashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    /**
     * Получение статистики продавца
     */
    @GetMapping("/stats") //
    public ResponseEntity<SellerStatsResponse> getSellerStats(Authentication authentication) {

        SellerStatsResponse stats = sellerDashboardService.getSellerStats(authentication);

        return ResponseEntity.ok(stats);
    }

    /**
     * Получение списка серверов продавца
     */
    @GetMapping("/servers") //
    public ResponseEntity<List<SellerServerResponse>> getSellerServers(Authentication authentication) {

        List<SellerServerResponse> servers = sellerDashboardService.getSellerServers(authentication);

        return ResponseEntity.ok(servers);
    }

    /**
     * Получение подписчиков продавца
     */
    @GetMapping("/subscribers") //
    public ResponseEntity<List<SellerSubscriberResponse>> getSellerSubscribers(Authentication authentication) {

        List<SellerSubscriberResponse> subscribers = sellerDashboardService.getSellerSubscribers(authentication);

        return ResponseEntity.ok(subscribers);
    }

    /**
     * Получение данных о продажах за период
     */
    @GetMapping("/sales") //
    public ResponseEntity<List<SalesDataResponse>> getSalesData(@RequestParam(defaultValue = "30") int days,
                                                                Authentication authentication) {

        List<SalesDataResponse> salesData = sellerDashboardService.getSalesData(authentication, days);

        return ResponseEntity.ok(salesData);
    }
}
