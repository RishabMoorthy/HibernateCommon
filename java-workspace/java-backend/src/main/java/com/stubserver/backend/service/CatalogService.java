package com.stubserver.backend.service;

import org.common.db.entity.VsCatalog;
import com.stubserver.backend.exception.BadRequestException;
import org.common.db.repository.MasterCatalogRepository;
import org.common.db.repository.VsCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final VsCatalogRepository vsCatalogRepo;
    private final MasterCatalogRepository masterCatalogRepo;

    public Map<String, Object> getServiceGroupTagsList() {
        List<VsCatalog> all = vsCatalogRepo.findAll();
        List<Map<String, Object>> services = all.stream().map(v -> {
            String name = v.getVsName() != null ? v.getVsName().trim() : "";
            String group = v.getGroup() != null ? v.getGroup().trim() : "";
            String tagsRaw = v.getTags() != null ? v.getTags() : "";
            List<String> tags = tagsRaw.isEmpty() ? List.of() :
                    Arrays.stream(tagsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            return Map.<String, Object>of("servicename", name, "group", group, "tags", tags);
        }).toList();
        return Map.of("totalService", services.size(), "services", services);
    }

    public Map<String, Object> checkMasterCatalog(String serviceName, String port) {
        boolean nameEmpty = serviceName == null || serviceName.isEmpty();
        boolean portEmpty = port == null || port.isEmpty();
        if (nameEmpty && portEmpty) {
            throw new BadRequestException("Provide at least one of: serviceName or port");
        }

        boolean nameMatch = !nameEmpty && masterCatalogRepo.existsByVsName(serviceName);
        Integer portInt = null;
        try { if (!portEmpty) portInt = Integer.parseInt(port); } catch (NumberFormatException ignored) {}
        boolean portMatch = portInt != null && masterCatalogRepo.existsByPort(portInt);
        boolean exists = nameMatch || portMatch;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("serviceName", nameEmpty ? null : serviceName);
        resp.put("port", portEmpty ? null : port);
        resp.put("exists", exists);
        resp.put("nameMatch", nameMatch);
        resp.put("portMatch", portMatch);
        return resp;
    }
}
