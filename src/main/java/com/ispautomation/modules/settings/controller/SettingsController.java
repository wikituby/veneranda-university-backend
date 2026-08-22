package com.ispautomation.modules.settings.controller;

import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.settings.entity.Setting;
import com.ispautomation.modules.settings.repository.SettingRepository;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/v1/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Settings", description = "System settings management")
public class SettingsController {

    @Inject
    SettingRepository settingRepository;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List all settings for current tenant")
    public Response listSettings() {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("setting:manage");

        List<Setting> settings = settingRepository.findByTenantId(securityContext.getTenantId());
        List<Map<String, Object>> dtos = settings.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("category", s.getCategory());
            m.put("key", s.getKey());
            m.put("value", s.getIsEncrypted() ? "••••••" : s.getValue());
            m.put("valueType", s.getValueType());
            m.put("description", s.getDescription());
            m.put("isPublic", s.getIsPublic());
            m.put("isEncrypted", s.getIsEncrypted());
            m.put("status", s.getStatus());
            m.put("createdAt", s.getCreatedAt());
            m.put("updatedAt", s.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update a setting value")
    public Response updateSetting(@PathParam("id") Long id, @Valid UpdateSettingRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("setting:manage");

        Setting setting = settingRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Setting not found: " + id));
        if (request.getValue() != null) setting.setValue(request.getValue());
        setting.setUpdatedBy(securityContext.getUserId());
        settingRepository.persist(setting);

        Map<String, Object> m = new HashMap<>();
        m.put("id", setting.getId());
        m.put("key", setting.getKey());
        m.put("value", setting.getIsEncrypted() ? "••••••" : setting.getValue());
        m.put("category", setting.getCategory());
        m.put("updatedAt", setting.getUpdatedAt());
        return Response.ok(m).build();
    }

    @GET
    @Path("/public")
    @Operation(summary = "Get public settings (no auth required)")
    public Response getPublicSettings() {
        List<Setting> settings = settingRepository.findPublicSettings();
        Map<String, Object> result = new HashMap<>();
        for (Setting s : settings) result.put(s.getKey(), s.getValue());
        return Response.ok(result).build();
    }

    public static class UpdateSettingRequest {
        @jakarta.validation.constraints.NotBlank(message = "Value is required")
        private String value;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}