package com.careerfit.identity.account.application;

import com.careerfit.identity.UserId;
import com.careerfit.identity.account.domain.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UserId userId);
}
