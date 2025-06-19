package server.vpn.com.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username", unique = true)
    private String username;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @Column(name = "changed_date", nullable = false)
    private ZonedDateTime changedDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "changed_by")
    private String changedBy;
} 