package com.careerfit.identity.account.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccountEntity> findByEmail(String email);
}
