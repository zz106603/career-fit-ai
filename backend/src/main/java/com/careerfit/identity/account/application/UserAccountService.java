package com.careerfit.identity.account.application;

import com.careerfit.identity.UserId;
import com.careerfit.identity.account.domain.AccountStatus;
import com.careerfit.identity.account.domain.UserAccount;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAccountService(
            UserAccountRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserAccount signup(String email, String rawPassword) {
        String normalizedEmail = UserAccount.normalizeEmail(email);
        validatePassword(rawPassword);
        if (repository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        UserAccount account = new UserAccount(
                new UserId(UUID.randomUUID()),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                AccountStatus.ACTIVE,
                clock.instant());
        try {
            return repository.save(account);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public UserAccount find(UserId userId) {
        return repository
                .findById(userId)
                .orElseThrow(() -> new IllegalStateException("인증 사용자의 계정을 찾을 수 없습니다."));
    }

    private static void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 72) {
            throw new IllegalArgumentException("비밀번호는 8자 이상 72자 이하여야 합니다.");
        }
    }
}
