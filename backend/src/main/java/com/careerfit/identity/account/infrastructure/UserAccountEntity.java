package com.careerfit.identity.account.infrastructure;

import com.careerfit.identity.account.domain.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_account")
class UserAccountEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccountEntity() {}

    UserAccountEntity(
            UUID id,
            String email,
            String passwordHash,
            AccountStatus status,
            Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    String email() {
        return email;
    }

    String passwordHash() {
        return passwordHash;
    }

    AccountStatus status() {
        return status;
    }

    Instant createdAt() {
        return createdAt;
    }
}
