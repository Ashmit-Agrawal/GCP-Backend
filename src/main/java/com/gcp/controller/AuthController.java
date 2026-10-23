package com.gcp.controller;

import com.gcp.dto.AuthRequest;
import com.gcp.dto.AuthResponse;
import com.gcp.dto.AuthResponse.OfficerInfo;
import com.gcp.exception.BadRequestException;
import com.gcp.model.Company;
import com.gcp.enums.Role;
import com.gcp.model.User;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.UserRepository;
import com.gcp.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public AuthController(AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username().trim(), req.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(req.username().trim())
                    .orElseThrow(() -> new BadRequestException("User not found"));

            String token = tokenProvider.generateToken(
                    user.getUsername(), user.getRole(), user.getCompanyId(), user.getFullName());

            Company company = null;
            if (user.getCompanyId() != null) {
                company = companyRepository.findById(user.getCompanyId()).orElse(null);
            }

            OfficerInfo officer = null;
            if (user.getRole() == Role.ROLE_ADMIN) {
                officer = new OfficerInfo(
                        user.getFullName() != null ? user.getFullName() : "Dr. Arvind Sharma",
                        "Regional Environmental Officer",
                        "Central India Zone",
                        "IFS-2014-0473",
                        "Ministry of Environment, Forest & Climate Change");
            }

            return new AuthResponse(
                    token,
                    "Bearer",
                    user.getUsername(),
                    user.getRole(),
                    user.getFullName(),
                    user.getCompanyId(),
                    company,
                    officer);

        } catch (DisabledException e) {
            throw new BadRequestException(
                    "Your account is pending verification and approval by the administrative officer.");
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid username or password.");
        }
    }

    @GetMapping("/me")
    public AuthResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadRequestException("Not authenticated");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Company company = null;
        if (user.getCompanyId() != null) {
            company = companyRepository.findById(user.getCompanyId()).orElse(null);
        }

        OfficerInfo officer = null;
        if (user.getRole() == Role.ROLE_ADMIN) {
            officer = new OfficerInfo(
                    user.getFullName() != null ? user.getFullName() : "Dr. Arvind Sharma",
                    "Regional Environmental Officer",
                    "Central India Zone",
                    "IFS-2014-0473",
                    "Ministry of Environment, Forest & Climate Change");
        }

        return new AuthResponse(
                null,
                "Bearer",
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                user.getCompanyId(),
                company,
                officer);
    }
}
