package com.ispautomation.modules.audit.controller;

import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.audit.entity.AuditLog;
import com.ispautomation.modules.audit.repository.AuditLogRepository;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/v1/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Audit Logs", description = "View system audit trail")
public class AuditLogController {

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List audit logs (paginated, searchable)")
    public Response listAuditLogs(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortDir") @DefaultValue("desc") String sortDir,
            @QueryParam("search") String search) {

        securityContext.requireAuthenticated();
        securityContext.requirePermission("audit:read");

        PageRequest pageRequest = PageRequest.of(page, size, sortBy, sortDir, search);
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (securityContext.getTenantId() != null) {
            query.append(" and tenantId = :tenantId");
            params.put("tenantId", securityContext.getTenantId());
        }
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            query.append(" and (lower(module) like :search or lower(action) like :search or lower(username) like :search or lower(description) like :search)");
            params.put("search", "%" + pageRequest.getSearch().toLowerCase() + "%");
        }

        long total = auditLogRepository.find(query.toString(), params).count();
        List<AuditLog> logs = auditLogRepository.find(query.toString(), params)
                .page(io.quarkus.panache.common.Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list();

        List<Map<String, Object>> dtos = logs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("tenantId", log.getTenantId());
            m.put("userId", log.getUserId());
            m.put("username", log.getUsername());
            m.put("module", log.getModule());
            m.put("action", log.getAction());
            m.put("entityType", log.getEntityType());
            m.put("entityId", log.getEntityId());
            m.put("description", log.getDescription());
            m.put("ipAddress", log.getIpAddress());
            m.put("requestPath", log.getRequestPath());
            m.put("httpMethod", log.getHttpMethod());
            m.put("statusCode", log.getStatusCode());
            m.put("durationMs", log.getDurationMs());
            m.put("createdAt", log.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        PageResponse<Map<String, Object>> response = new PageResponse<>(dtos, total, pageRequest.getPage(), pageRequest.getSize());
        return Response.ok(response).build();
    }
}