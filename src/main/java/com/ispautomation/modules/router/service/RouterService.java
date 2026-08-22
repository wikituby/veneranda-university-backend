package com.ispautomation.modules.router.service;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.ConflictException;
import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.router.communication.DeviceCommunicationService;
import com.ispautomation.modules.router.dto.CreateRouterRequest;
import com.ispautomation.modules.router.dto.RouterDto;
import com.ispautomation.modules.router.dto.UpdateRouterRequest;
import com.ispautomation.modules.router.entity.Router;
import com.ispautomation.modules.router.repository.RouterRepository;
import com.ispautomation.modules.rbac.entity.Branch;
import com.ispautomation.modules.rbac.entity.Tenant;
import com.ispautomation.security.PasswordEncoder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for router management CRUD operations.
 *
 * Encrypts credentials using the existing BCrypt password encoder.
 * Provides extension points for device communication through the
 * {@link DeviceCommunicationService} strategy interface.
 */
@ApplicationScoped
public class RouterService {

    @Inject
    RouterRepository routerRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    /**
     * All registered device communication strategy implementations.
     * Currently empty; implementations will be plugged in when device
     * communication modules are built (MikroTik API, SSH, SNMP, etc.).
     */
    @Inject
    Instance<DeviceCommunicationService> communicationServices;

    @Transactional
    public RouterDto createRouter(CreateRouterRequest request, Long tenantId, Long createdBy) {
        // Validate vendor
        Router.Vendor vendor;
        try {
            vendor = Router.Vendor.valueOf(request.getVendor().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Unsupported vendor: " + request.getVendor()
                    + ". Supported: MIKROTIK, UBIQUITI, TP_LINK, D_LINK, CISCO, HUAWEI, GENERIC");
        }

        // Validate IP uniqueness within tenant
        if (routerRepository.existsByTenantAndIpAddress(tenantId, request.getIpAddress())) {
            throw new ConflictException("A router with IP address " + request.getIpAddress()
                    + " already exists in this tenant");
        }

        Router router = new Router();
        router.setName(request.getName());
        router.setVendor(vendor);
        router.setModel(request.getModel());
        router.setIpAddress(request.getIpAddress());
        router.setApiPort(request.getApiPort() != null ? request.getApiPort() : 8728);
        router.setUsername(request.getUsername());
        router.setPasswordEncrypted(passwordEncoder.encode(request.getPassword()));
        router.setLocation(request.getLocation());
        router.setFirmware(request.getFirmware());
        router.setRouterVersion(request.getRouterVersion());
        router.setSerialNumber(request.getSerialNumber());
        router.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true);
        router.setIsOnline(false);
        router.setNotes(request.getNotes());
        router.setStatus("ACTIVE");
        router.setCreatedBy(createdBy);

        // Set tenant
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        router.setTenant(tenant);

        // Set branch if provided
        if (request.getBranchId() != null) {
            Branch branch = new Branch();
            branch.setId(request.getBranchId());
            router.setBranch(branch);
        }

        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    @Transactional
    public RouterDto updateRouter(Long id, UpdateRouterRequest request, Long updatedBy) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));

        if (request.getName() != null) router.setName(request.getName());

        if (request.getVendor() != null) {
            try {
                router.setVendor(Router.Vendor.valueOf(request.getVendor().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(400, "Unsupported vendor: " + request.getVendor()
                        + ". Supported: MIKROTIK, UBIQUITI, TP_LINK, D_LINK, CISCO, HUAWEI, GENERIC");
            }
        }

        if (request.getModel() != null) router.setModel(request.getModel());

        if (request.getIpAddress() != null
                && !request.getIpAddress().equalsIgnoreCase(router.getIpAddress())) {
            if (routerRepository.existsByTenantAndIpAddress(
                    router.getTenant().getId(), request.getIpAddress())) {
                throw new ConflictException("A router with IP address " + request.getIpAddress()
                        + " already exists in this tenant");
            }
            router.setIpAddress(request.getIpAddress());
        }

        if (request.getApiPort() != null) router.setApiPort(request.getApiPort());
        if (request.getUsername() != null) router.setUsername(request.getUsername());

        // Re-encrypt password if provided
        if (request.getPassword() != null) {
            router.setPasswordEncrypted(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getLocation() != null) router.setLocation(request.getLocation());
        if (request.getFirmware() != null) router.setFirmware(request.getFirmware());
        if (request.getRouterVersion() != null) router.setRouterVersion(request.getRouterVersion());
        if (request.getSerialNumber() != null) router.setSerialNumber(request.getSerialNumber());
        if (request.getIsEnabled() != null) router.setIsEnabled(request.getIsEnabled());
        if (request.getStatus() != null) router.setStatus(request.getStatus());
        if (request.getNotes() != null) router.setNotes(request.getNotes());

        if (request.getBranchId() != null) {
            Branch branch = new Branch();
            branch.setId(request.getBranchId());
            router.setBranch(branch);
        }

        router.setUpdatedBy(updatedBy);
        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    public RouterDto getRouterById(Long id) {
        return routerRepository.findByIdOptional(id)
                .map(RouterDto::fromEntity)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));
    }

    public PageResponse<RouterDto> listRouters(PageRequest pageRequest, Long tenantId) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (tenantId != null) {
            query.append(" and tenant.id = :tenantId");
            params.put("tenantId", tenantId);
        }
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            query.append(" and (lower(name) like :search"
                    + " or lower(ipAddress) like :search"
                    + " or lower(location) like :search"
                    + " or lower(model) like :search"
                    + " or lower(serialNumber) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        long total = routerRepository.find(query.toString(), params).count();
        List<Router> routers = routerRepository.find(query.toString(), params)
                .page(io.quarkus.panache.common.Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list();

        List<RouterDto> dtos = routers.stream()
                .map(RouterDto::fromEntity)
                .collect(Collectors.toList());
        return new PageResponse<>(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Transactional
    public void deleteRouter(Long id) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));
        routerRepository.delete(router);
    }

    @Transactional
    public RouterDto enableRouter(Long id, Long updatedBy) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));
        router.setIsEnabled(true);
        router.setStatus("ACTIVE");
        router.setUpdatedBy(updatedBy);
        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    @Transactional
    public RouterDto disableRouter(Long id, Long updatedBy) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));
        router.setIsEnabled(false);
        router.setStatus("MAINTENANCE");
        router.setUpdatedBy(updatedBy);
        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    /**
     * Test connectivity to a router.
     *
     * Currently returns a stub success. Real implementation will use
     * the registered {@link DeviceCommunicationService} implementations
     * based on the router's vendor.
     */
    @Transactional
    public RouterDto testConnection(Long id, Long updatedBy) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));

        // Try registered communication services first
        boolean reachable = false;
        for (DeviceCommunicationService service : communicationServices) {
            if (service.supportsVendor(router.getVendor().name())) {
                reachable = service.testConnection(router);
                break;
            }
        }

        // Stub: mark as online if enabled (real implementation will ping/connect)
        if (!reachable) {
            reachable = Boolean.TRUE.equals(router.getIsEnabled());
        }

        router.setIsOnline(reachable);
        router.setLastSeenAt(LocalDateTime.now());
        if (reachable) {
            router.setStatus("ACTIVE");
        } else {
            router.setStatus("OFFLINE");
        }
        router.setUpdatedBy(updatedBy);
        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    /**
     * Synchronize router information.
     *
     * Currently a stub. Real implementation will use
     * {@link DeviceCommunicationService#synchronize(Router)}.
     */
    @Transactional
    public RouterDto synchronizeRouter(Long id, Long updatedBy) {
        Router router = routerRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Router not found: " + id));

        boolean synced = false;
        for (DeviceCommunicationService service : communicationServices) {
            if (service.supportsVendor(router.getVendor().name())) {
                Router updated = service.synchronize(router);
                updated.setLastSyncAt(LocalDateTime.now());
                updated.setUpdatedBy(updatedBy);
                routerRepository.persist(updated);
                return RouterDto.fromEntity(updated);
            }
        }

        // Stub: just update last sync timestamp
        router.setLastSyncAt(LocalDateTime.now());
        router.setUpdatedBy(updatedBy);
        routerRepository.persist(router);
        return RouterDto.fromEntity(router);
    }

    /**
     * Get dashboard statistics for routers.
     */
    public Map<String, Object> getDashboardStats(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRouters", routerRepository.countByTenant(tenantId));
        stats.put("onlineRouters", routerRepository.countOnlineByTenant(tenantId));
        stats.put("mikrotikCount", routerRepository.countByTenantAndVendor(tenantId, Router.Vendor.MIKROTIK));
        stats.put("ubiquitiCount", routerRepository.countByTenantAndVendor(tenantId, Router.Vendor.UBIQUITI));
        stats.put("ciscoCount", routerRepository.countByTenantAndVendor(tenantId, Router.Vendor.CISCO));
        stats.put("huaweiCount", routerRepository.countByTenantAndVendor(tenantId, Router.Vendor.HUAWEI));
        return stats;
    }
}