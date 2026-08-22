package com.ispautomation.modules.rbac.service;

import com.ispautomation.common.exception.ConflictException;
import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.rbac.dto.CreateUserRequest;
import com.ispautomation.modules.rbac.dto.PermissionDto;
import com.ispautomation.modules.rbac.dto.RoleDto;
import com.ispautomation.modules.rbac.dto.UpdateUserRequest;
import com.ispautomation.modules.rbac.dto.UserDto;
import com.ispautomation.modules.rbac.entity.Branch;
import com.ispautomation.modules.rbac.entity.Permission;
import com.ispautomation.modules.rbac.entity.Role;
import com.ispautomation.modules.rbac.entity.Tenant;
import com.ispautomation.modules.rbac.entity.User;
import com.ispautomation.modules.rbac.repository.PermissionRepository;
import com.ispautomation.modules.rbac.repository.RoleRepository;
import com.ispautomation.modules.rbac.repository.UserRepository;
import com.ispautomation.security.PasswordEncoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for RBAC operations: users, roles, permissions CRUD.
 */
@ApplicationScoped
public class RbacService {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    // ===== Users =====

    @Transactional
    public UserDto createUser(CreateUserRequest request, Long tenantId, Long createdBy) {
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(tenantId, request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setJobTitle(request.getJobTitle());
        user.setEmployeeNumber(request.getEmployeeNumber());
        user.setIsActive(request.getIsActive());
        user.setIsLocked(false);
        user.setIsSystem(false);
        user.setFailedLoginCount(0);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setTwoFactorEnabled(false);
        user.setStatus("ACTIVE");
        user.setCreatedBy(createdBy);

        // Set tenant
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        user.setTenant(tenant);

        // Set branch if provided
        if (request.getBranchId() != null) {
            Branch branch = new Branch();
            branch.setId(request.getBranchId());
            user.setBranch(branch);
        }

        // Assign roles
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findByIdOptional(roleId)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        userRepository.persist(user);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, Long updatedBy) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getJobTitle() != null) user.setJobTitle(request.getJobTitle());
        if (request.getEmployeeNumber() != null) user.setEmployeeNumber(request.getEmployeeNumber());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getIsLocked() != null) user.setIsLocked(request.getIsLocked());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        if (request.getBranchId() != null) {
            Branch branch = new Branch();
            branch.setId(request.getBranchId());
            user.setBranch(branch);
        }

        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findByIdOptional(roleId)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        user.setUpdatedBy(updatedBy);
        userRepository.persist(user);
        return UserDto.fromEntity(user);
    }

    public UserDto getUserById(Long id) {
        return userRepository.findByIdOptional(id)
                .map(UserDto::fromEntity)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public PageResponse<UserDto> listUsers(PageRequest pageRequest, Long tenantId) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (tenantId != null) {
            query.append(" and tenant.id = :tenantId");
            params.put("tenantId", tenantId);
        }
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            query.append(" and (lower(username) like :search or lower(email) like :search or lower(firstName) like :search or lower(lastName) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        long total = userRepository.find(query.toString(), params).count();
        List<User> users = userRepository.find(query.toString(), params)
                .page(io.quarkus.panache.common.Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list();

        List<UserDto> dtos = users.stream().map(UserDto::fromEntity).collect(Collectors.toList());
        return new PageResponse<>(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (Boolean.TRUE.equals(user.getIsSystem())) {
            throw new ConflictException("System users cannot be deleted");
        }
        userRepository.delete(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        user.setIsLocked(false);
        userRepository.persist(user);
    }

    // ===== Roles =====

    @Transactional
    public RoleDto createRole(String name, String code, String description, Long tenantId,
                              Set<Long> permissionIds, Long createdBy) {
        if (roleRepository.existsByTenantAndCode(tenantId, code)) {
            throw new ConflictException("Role code already exists: " + code);
        }

        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role.setIsSystem(false);
        role.setIsActive(true);
        role.setStatus("ACTIVE");
        role.setCreatedBy(createdBy);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        role.setTenant(tenant);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (Long permId : permissionIds) {
                Permission perm = permissionRepository.findByIdOptional(permId)
                        .orElseThrow(() -> new NotFoundException("Permission not found: " + permId));
                permissions.add(perm);
            }
            role.setPermissions(permissions);
        }

        roleRepository.persist(role);
        return RoleDto.fromEntity(role);
    }

    @Transactional
    public RoleDto updateRole(Long id, String name, String description,
                              Boolean isActive, Set<Long> permissionIds, Long updatedBy) {
        Role role = roleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Role not found: " + id));

        if (name != null) role.setName(name);
        if (description != null) role.setDescription(description);
        if (isActive != null) role.setIsActive(isActive);

        if (permissionIds != null) {
            Set<Permission> permissions = new HashSet<>();
            for (Long permId : permissionIds) {
                Permission perm = permissionRepository.findByIdOptional(permId)
                        .orElseThrow(() -> new NotFoundException("Permission not found: " + permId));
                permissions.add(perm);
            }
            role.setPermissions(permissions);
        }

        role.setUpdatedBy(updatedBy);
        roleRepository.persist(role);
        return RoleDto.fromEntity(role);
    }

    public RoleDto getRoleById(Long id) {
        return roleRepository.findByIdOptional(id)
                .map(RoleDto::fromEntity)
                .orElseThrow(() -> new NotFoundException("Role not found: " + id));
    }

    public PageResponse<RoleDto> listRoles(PageRequest pageRequest, Long tenantId) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (tenantId != null) {
            query.append(" and tenant.id = :tenantId");
            params.put("tenantId", tenantId);
        }
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            query.append(" and (lower(name) like :search or lower(code) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        long total = roleRepository.find(query.toString(), params).count();
        List<Role> roles = roleRepository.find(query.toString(), params)
                .page(io.quarkus.panache.common.Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list();

        List<RoleDto> dtos = roles.stream().map(RoleDto::fromEntity).collect(Collectors.toList());
        return new PageResponse<>(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Role not found: " + id));
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new ConflictException("System roles cannot be deleted");
        }
        roleRepository.delete(role);
    }

    // ===== Permissions =====

    public PermissionDto getPermissionById(Long id) {
        return permissionRepository.findByIdOptional(id)
                .map(PermissionDto::fromEntity)
                .orElseThrow(() -> new NotFoundException("Permission not found: " + id));
    }

    public List<PermissionDto> listAllPermissions() {
        return permissionRepository.listAll().stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PermissionDto> listPermissionsByModule(String module) {
        return permissionRepository.findByModule(module).stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toList());
    }
}