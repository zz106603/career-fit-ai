package com.careerfit.identity.security;

import com.careerfit.identity.account.application.UserAccountRepository;
import com.careerfit.identity.account.domain.AccountStatus;
import com.careerfit.identity.account.domain.UserAccount;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public AccountUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = repository
                .findByEmail(UserAccount.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("인증 정보를 확인할 수 없습니다."));
        return new AuthenticatedUserPrincipal(
                account.id(),
                account.email(),
                account.passwordHash(),
                account.status() == AccountStatus.ACTIVE);
    }
}
