package com.gcp.security;

import com.gcp.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Authentication> getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof UserPrincipal)
                .map(auth -> (UserPrincipal) auth.getPrincipal());
    }

    public static Optional<String> getCurrentUsername() {
        return getAuthentication().map(Authentication::getName);
    }

    public static Long getCurrentCompanyId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getCompanyId).orElse(null);
    }

    public static boolean isAdmin() {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> Role.ROLE_ADMIN.name().equals(a.getAuthority())))
                .orElse(false);
    }
}
