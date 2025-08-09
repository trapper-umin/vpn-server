package server.vpn.com.servermanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vpn_servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnServer {

    @Id
    @GeneratedValue
    @Column(name = "server_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private Integer port;

    @Column(name = "max_connections", nullable = false)
    private Integer maxConnections;

    @Column(name = "current_connections", nullable = false)
    @Builder.Default
    private Integer currentConnections = 0;

    @Column(nullable = false, length = 50)
    private String bandwidth;

    @Column(nullable = false, length = 50)
    private String speed;

    @Column(nullable = false)
    @Builder.Default
    private Integer ping = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal uptime = BigDecimal.valueOf(100.0);

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private Boolean isOnline = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "total_subscribers", nullable = false)
    @Builder.Default
    private Integer totalSubscribers = 0;

    @Column(name = "active_subscribers", nullable = false)
    @Builder.Default
    private Integer activeSubscribers = 0;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "monthly_revenue", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyRevenue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServerStatus status = ServerStatus.SETUP;

    @ElementCollection
    @CollectionTable(name = "vpn_server_features", joinColumns = @JoinColumn(name = "server_id"))
    @Column(name = "feature")
    private List<String> features;

    @ElementCollection
    @CollectionTable(name = "vpn_server_protocols", joinColumns = @JoinColumn(name = "server_id"))
    @Column(name = "protocol")
    private List<String> protocols;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ServerStatus {
        SETUP, ACTIVE, INACTIVE, MAINTENANCE
    }
}