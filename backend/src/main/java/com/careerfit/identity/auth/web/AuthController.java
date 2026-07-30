package com.careerfit.identity.auth.web;

import com.careerfit.identity.account.application.UserAccountService;
import com.careerfit.identity.account.domain.UserAccount;
import com.careerfit.identity.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthController(
            UserAccountService accountService,
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticatedUserResponse> signup(
            @Valid @RequestBody AuthCredentialsRequest request) {
        UserAccount account = accountService.signup(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthenticatedUserResponse.from(account));
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(), request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        sessionAuthenticationStrategy.onAuthentication(
                authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return AuthenticatedUserResponse.from(
                (AuthenticatedUserPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(Authentication authentication) {
        return AuthenticatedUserResponse.from(
                (AuthenticatedUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(
            HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken());
    }
}
