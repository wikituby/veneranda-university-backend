package com.ispautomation.modules.auth.service;


import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.UnauthorizedException;
import com.ispautomation.modules.audit.repository.AuditLogRepository;
import com.ispautomation.modules.auth.dto.ChangePasswordRequest;
import com.ispautomation.modules.auth.dto.LoginRequest;
import com.ispautomation.modules.auth.dto.RegisterRequest;
import com.ispautomation.modules.auth.dto.RefreshTokenRequest;
import com.ispautomation.modules.auth.dto.TokenResponse;
import com.ispautomation.modules.auth.dto.UpdateProfileRequest;
import com.ispautomation.modules.auth.service.GoogleIdTokenVerifier.GoogleIdentity;
import com.ispautomation.modules.rbac.entity.Permission;
import com.ispautomation.modules.rbac.entity.RefreshToken;
import com.ispautomation.modules.rbac.entity.Role;
import com.ispautomation.modules.rbac.entity.Tenant;
import com.ispautomation.modules.rbac.entity.User;
import com.ispautomation.modules.rbac.repository.RefreshTokenRepository;
import com.ispautomation.modules.rbac.repository.RoleRepository;
import com.ispautomation.modules.rbac.repository.TenantRepository;
import com.ispautomation.modules.rbac.repository.UserRepository;
import com.ispautomation.security.JwtTokenService;
import com.ispautomation.security.PasswordEncoder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;



@ApplicationScoped
public class AuthService {


    private static final Logger LOG =
            Logger.getLogger(AuthService.class);



    @Inject
    UserRepository userRepository;


    @Inject
    RefreshTokenRepository refreshTokenRepository;


    @Inject
    AuditLogRepository auditLogRepository;


    @Inject
    PasswordEncoder passwordEncoder;


    @Inject
    JwtTokenService jwtTokenService;


    @Inject
    GoogleIdTokenVerifier googleIdTokenVerifier;


    @Inject
    TenantRepository tenantRepository;


    @Inject
    RoleRepository roleRepository;


    @ConfigProperty(name = "app.google.default-role", defaultValue = "STUDENT")
    String googleDefaultRole;




    @Transactional
    public TokenResponse login(
            LoginRequest request,
            String ipAddress,
            String userAgent) {



        User user =
                userRepository.findByEmail(request.getUsername())
                .or(() -> userRepository
                        .find("username", request.getUsername())
                        .firstResultOptional())
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid username or password"));



        if(Boolean.FALSE.equals(user.getIsActive())){

            throw new UnauthorizedException(
                    "Account inactive");

        }



        if(user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())){


            throw new UnauthorizedException(
                    "Invalid username or password");

        }



        return issueTokens(user, ipAddress, userAgent);

    }



    @Transactional
    public TokenResponse loginWithGoogle(
            String idToken,
            String ipAddress,
            String userAgent) {

        GoogleIdentity identity = googleIdTokenVerifier.verify(idToken);
        User user = findOrCreateGoogleUser(identity);

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account inactive");
        }

        return issueTokens(user, ipAddress, userAgent);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(409, "An account with this email already exists. Sign in instead.");
        }

        Tenant tenant = tenantRepository.findByCode("DEFAULT")
                .orElseThrow(() -> new UnauthorizedException("Default tenant is not configured"));

        String[] nameParts = request.getFullName().trim().split("\\s+");
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[nameParts.length - 1] : "Student";

        User user = new User();
        user.setTenant(tenant);
        user.setUsername(uniqueUsername(tenant.getId(), email));
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuthProvider("local");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setIsSystem(false);
        user.setFailedLoginCount(0);
        user.setTwoFactorEnabled(false);
        user.setStatus("ACTIVE");

        Role studentRole = roleRepository.findByTenantAndCode(tenant.getId(), googleDefaultRole)
                .orElseThrow(() -> new UnauthorizedException(
                        "Default student role is not configured: " + googleDefaultRole));
        Set<Role> roles = new HashSet<>();
        roles.add(studentRole);
        user.setRoles(roles);
        userRepository.persist(user);
        LOG.infof("Registered student %s (%s)", user.getEmail(), user.getUsername());
        return issueTokens(user, ipAddress, userAgent);
    }

    private User findOrCreateGoogleUser(GoogleIdentity identity) {
        Optional<User> bySub = userRepository.findByGoogleSub(identity.sub());
        if (bySub.isPresent()) {
            User existing = bySub.get();
            linkGoogleProvider(existing, identity);
            return existing;
        }

        Optional<User> byEmail = userRepository.findByEmail(identity.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleSub(identity.sub());
            linkGoogleProvider(existing, identity);
            if (identity.pictureUrl() != null && !identity.pictureUrl().isBlank()) {
                existing.setAvatarUrl(identity.pictureUrl());
            }
            userRepository.persist(existing);
            return existing;
        }

        Tenant tenant = tenantRepository.findByCode("DEFAULT")
                .orElseThrow(() -> new UnauthorizedException("Default tenant is not configured"));

        User user = new User();
        user.setTenant(tenant);
        user.setUsername(uniqueUsername(tenant.getId(), identity.email()));
        user.setEmail(identity.email());
        user.setFirstName(resolveFirstName(identity));
        user.setLastName(resolveLastName(identity));
        user.setAvatarUrl(identity.pictureUrl());
        user.setGoogleSub(identity.sub());
        user.setAuthProvider("google");
        user.setPasswordHash(null);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setIsSystem(false);
        user.setFailedLoginCount(0);
        user.setTwoFactorEnabled(false);
        user.setStatus("ACTIVE");

        Role studentRole = roleRepository.findByTenantAndCode(tenant.getId(), googleDefaultRole)
                .orElseThrow(() -> new UnauthorizedException(
                        "Default Google role is not configured: " + googleDefaultRole));
        Set<Role> roles = new HashSet<>();
        roles.add(studentRole);
        user.setRoles(roles);

        userRepository.persist(user);
        LOG.infof("Created Google user %s (%s)", user.getEmail(), user.getUsername());
        return user;
    }

    private void linkGoogleProvider(User user, GoogleIdentity identity) {
        if (user.getGoogleSub() == null) {
            user.setGoogleSub(identity.sub());
        }
        String provider = user.getAuthProvider();
        if (provider == null || "local".equalsIgnoreCase(provider)) {
            user.setAuthProvider(user.getPasswordHash() == null ? "google" : "both");
        } else if ("google".equalsIgnoreCase(provider) && user.getPasswordHash() != null) {
            user.setAuthProvider("both");
        }
    }

    private String uniqueUsername(Long tenantId, String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = local.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[^a-z0-9]+", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int attempt = 0;
        while (userRepository.existsByUsername(tenantId, candidate)) {
            attempt++;
            String suffix = String.valueOf(attempt);
            int maxBase = Math.max(1, 50 - suffix.length());
            candidate = (base.length() > maxBase ? base.substring(0, maxBase) : base) + suffix;
        }
        return candidate;
    }

    private static String resolveFirstName(GoogleIdentity identity) {
        if (identity.givenName() != null && !identity.givenName().isBlank()) {
            return identity.givenName().trim();
        }
        if (identity.fullName() != null && !identity.fullName().isBlank()) {
            String[] parts = identity.fullName().trim().split("\\s+");
            return parts[0];
        }
        return identity.email().contains("@")
                ? identity.email().substring(0, identity.email().indexOf('@'))
                : "Student";
    }

    private static String resolveLastName(GoogleIdentity identity) {
        if (identity.familyName() != null && !identity.familyName().isBlank()) {
            return identity.familyName().trim();
        }
        if (identity.fullName() != null && !identity.fullName().isBlank()) {
            String[] parts = identity.fullName().trim().split("\\s+");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return "User";
    }

    private TokenResponse issueTokens(User user, String ipAddress, String userAgent) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);

        Set<String> roles = getRoles(user);
        Set<String> permissions = getPermissions(user);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getBranch() != null ? String.valueOf(user.getBranch().getId()) : null,
                roles,
                permissions);

        String refreshToken = jwtTokenService.generateRefreshToken(
                user.getId(),
                user.getUsername());

        persistRefreshToken(user, refreshToken, ipAddress, userAgent);
        return createResponse(accessToken, refreshToken, user, roles, permissions);
    }




    @Transactional
    public TokenResponse refresh(
            RefreshTokenRequest request,
            String ipAddress,
            String userAgent) {



        String rawToken =
                request.getRefreshToken();



        Long userId =
                jwtTokenService.extractUserId(
                        rawToken);



        if(userId == null){

            throw new UnauthorizedException(
                    "Invalid refresh token");

        }



        User user =
                userRepository.findByIdOptional(userId)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User not found"));



        String hash =
                sha256(rawToken);



        RefreshToken oldToken =
                refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Refresh token not found"));



        if(Boolean.TRUE.equals(
                oldToken.getIsRevoked())){


            throw new UnauthorizedException(
                    "Refresh token revoked");

        }



        if(oldToken.getExpiresAt()
                .isBefore(LocalDateTime.now())){


            throw new UnauthorizedException(
                    "Refresh token expired");

        }



        // rotate old token
        oldToken.setIsRevoked(true);

        oldToken.setRevokedAt(
                LocalDateTime.now());



        Set<String> roles =
                getRoles(user);



        Set<String> permissions =
                getPermissions(user);




        String access =
                jwtTokenService.generateAccessToken(
                        user.getId(),
                        user.getUsername(),
                        user.getTenant()!=null
                                ? user.getTenant().getId()
                                : null,
                        user.getBranch()!=null
                                ? String.valueOf(
                                  user.getBranch().getId())
                                : null,
                        roles,
                        permissions);




        String newRefresh =
                jwtTokenService.generateRefreshToken(
                        user.getId(),
                        user.getUsername());




        persistRefreshToken(
                user,
                newRefresh,
                ipAddress,
                userAgent);




        return createResponse(
                access,
                newRefresh,
                user,
                roles,
                permissions);

    }




    public TokenResponse.UserInfo getCurrentUserInfo(
            Long userId){



        User user =
                userRepository.findByIdOptional(userId)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User not found"));


        return toUserInfo(user);

    }

    @Transactional
    public TokenResponse.UserInfo updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new BusinessException(409, "That email is already in use.");
        }

        String[] nameParts = request.getFullName().trim().split("\\s+");
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts.length > 1 ? nameParts[nameParts.length - 1] : user.getLastName());
        user.setEmail(email);
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        user.setPhone(phone == null || phone.isBlank() ? null : phone);
        user.setUpdatedBy(userId);
        userRepository.persist(user);
        return toUserInfo(user);
    }

    @Transactional
    public TokenResponse.UserInfo changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        if (hasPassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException(400, "Current password is incorrect.");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        String provider = user.getAuthProvider();
        if (provider == null || "local".equalsIgnoreCase(provider)) {
            user.setAuthProvider("local");
        } else if ("google".equalsIgnoreCase(provider)) {
            user.setAuthProvider("both");
        }
        user.setUpdatedBy(userId);
        userRepository.persist(user);
        return toUserInfo(user);
    }

    private TokenResponse.UserInfo toUserInfo(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        return new TokenResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                hasPassword,
                getRoles(user),
                getPermissions(user));
    }




    private void persistRefreshToken(
            User user,
            String rawToken,
            String ip,
            String agent){



        String hash =
                sha256(rawToken);



        RefreshToken token =
                new RefreshToken();



        token.setUuid(
                UUID.randomUUID());


        token.setUser(user);


        token.setTokenHash(hash);


        token.setIpAddress(ip);


        token.setDeviceInfo(agent);


        token.setExpiresAt(
                LocalDateTime.now()
                .plusSeconds(
                        jwtTokenService
                        .getRefreshTokenTtl()
                        .getSeconds()));


        token.setIsRevoked(false);



        refreshTokenRepository.persist(token);

    }




    private Set<String> getRoles(User user){

        return user.getRoles()
                .stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

    }




    private Set<String> getPermissions(User user){

        return user.getRoles()
                .stream()
                .flatMap(
                        r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

    }




    private TokenResponse createResponse(
            String access,
            String refresh,
            User user,
            Set<String> roles,
            Set<String> permissions){



        TokenResponse.UserInfo info = toUserInfo(user);



        return new TokenResponse(
                access,
                refresh,
                jwtTokenService
                        .getAccessTokenTtl()
                        .getSeconds(),
                info);

    }




    private String sha256(String input){


        try{


            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");


            return HexFormat.of()
                    .formatHex(
                            digest.digest(
                                    input.getBytes(
                                            java.nio.charset.StandardCharsets.UTF_8)));



        }catch(Exception e){


            throw new RuntimeException(
                    "Failed to hash token",
                    e);

        }

    }




    @Transactional
    public void logout(
            Long userId,
            String ip,
            String agent){


        refreshTokenRepository
                .revokeAllForUser(userId);

    }

}
