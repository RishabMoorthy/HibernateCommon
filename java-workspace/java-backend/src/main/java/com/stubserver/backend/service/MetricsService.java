package com.stubserver.backend.service;

import com.stubserver.backend.exception.BadRequestException;
import org.common.db.repository.MetricsRepository;
import com.stubserver.backend.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MetricsRepository metricsRepo;

    public List<Map<String, Object>> getLifetimeHits() {
        List<Map<String, Object>> rows = metricsRepo.findLifetimeHits();
        return rows.stream().map(r -> Map.<String, Object>of(
                "serviceName", r.get("VSNAME"),
                "counts", Map.of(
                        "total", r.get("TOTAL_COUNT"),
                        "qa", r.get("TOTAL_QA_COUNT"),
                        "uat", r.get("TOTAL_UAT_COUNT")
                )
        )).toList();
    }

    public Map<String, Object> getMonthlyHits(String fromMonth, String toMonth) {
        if (fromMonth == null || toMonth == null) {
            throw new BadRequestException("fromMonth and toMonth are required in MON-YYYY format");
        }
        List<Map<String, Object>> rows = metricsRepo.findMonthlyHits(fromMonth, toMonth);
        List<Map<String, Object>> data = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serviceName", r.get("serviceName"));
            m.put("month", r.get("month"));
            m.put("year", r.get("year"));
            m.put("totalCount", r.get("totalCount"));
            m.put("totalQACount", r.get("totalQACount"));
            m.put("totalUATCount", r.get("totalUATCount"));
            return m;
        }).toList();
        return Map.of("data", data);
    }

    public List<Map<String, Object>> getCustomReport(String fromDate, String toDate) {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }
        List<Map<String, Object>> rows = metricsRepo.findCustomReport(fromDate, toDate);
        return rows.stream().map(r -> Map.<String, Object>of(
                "serviceName", r.get("SERVICENAME"),
                "transDate", r.get("TRANSDATE"),
                "counts", Map.of(
                        "total", r.get("TOTALCOUNT"),
                        "qa", r.get("TOTALQACOUNT"),
                        "uat", r.get("TOTALUATCOUNT")
                )
        )).toList();
    }

    public Map<String, Object> getDormantServiceLists(String serverIP) {
        if (serverIP == null || serverIP.isEmpty()) {
            throw new BadRequestException("Missing serverIP in request body");
        }
        List<Map<String, Object>> rows = metricsRepo.findDormantServices(serverIP);

        Map<String, Object> resp = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> last3 = new LinkedHashMap<>();
        last3.put("count_0", new ArrayList<>());
        last3.put("count_1_50", new ArrayList<>());
        last3.put("count_51_100", new ArrayList<>());
        Map<String, List<Map<String, Object>>> last6 = new LinkedHashMap<>();
        last6.put("count_0", new ArrayList<>());
        last6.put("count_1_50", new ArrayList<>());
        last6.put("count_51_100", new ArrayList<>());

        for (Map<String, Object> row : rows) {
            String vsname = String.valueOf(row.get("VSNAME"));
            Object hits3 = row.get("HITS_3M");
            Object hits6 = row.get("HITS_6M");
            String cat3 = (String) row.get("COUNT_CATEGORY_3M");
            String cat6 = (String) row.get("COUNT_CATEGORY_6M");
            if (cat3 != null && last3.containsKey(cat3)) {
                last3.get(cat3).add(Map.of("VSNAME", vsname, "COUNT", hits3));
            }
            if (cat6 != null && last6.containsKey(cat6)) {
                last6.get(cat6).add(Map.of("VSNAME", vsname, "COUNT", hits6));
            }
        }
        resp.put("last_3_months", last3);
        resp.put("last_6_months", last6);
        return resp;
    }

    public Map<String, Object> getResponseTime(String serviceName, String serverIP,
                                                String fromDateTime, String toDateTime) {
        if (serviceName == null || serverIP == null) {
            throw new BadRequestException("serviceName and serverIP are required.");
        }
        if (fromDateTime == null || toDateTime == null) {
            throw new BadRequestException("fromDateTime and toDateTime are required (ISO: YYYY-MM-DDTHH:mm).");
        }
        if (!fromDateTime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}") ||
                !toDateTime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
            throw new BadRequestException("Invalid datetime format. Use ISO like 2025-12-01T10:25.");
        }

        String fromUtc = DateTimeUtil.istToUtcMinuteString(fromDateTime);
        String toUtc = DateTimeUtil.istToUtcMinuteString(toDateTime);

        List<Map<String, Object>> rows = metricsRepo.findResponseTime(serviceName, serverIP, fromUtc, toUtc);
        return Map.of("total", rows.size(), "data", rows);
    }
}
