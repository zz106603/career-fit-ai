package com.careerfit.identity.account.infrastructure;

import com.careerfit.identity.UserId;
import com.careerfit.identity.account.application.UserAccountRepository;
import com.careerfit.identity.account.domain.UserAccount;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {

    private final SpringDataUserAccountRepository repository;

    public JpaUserAccountRepository(SpringDataUserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity(
                account.id().value(),
                account.email(),
                account.passwordHash(),
                account.status(),
                account.createdAt());
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UserId userId) {
        return repository.findById(userId.value()).map(this::toDomain);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                new UserId(entity.id()),
                entity.email(),
                entity.passwordHash(),
                entity.status(),
                entity.createdAt());
    }
}
