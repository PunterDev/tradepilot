package com.tradepilot.importing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CsvTable {
    private final List<String> headers;
    private final List<Map<String, String>> rows;

    private CsvTable(List<String> headers, List<Map<String, String>> rows) {
        this.headers = headers;
        this.rows = rows;
    }

    static CsvTable parse(String csv) {
        List<List<String>> records = parseRecords(csv == null ? "" : csv);
        if (records.isEmpty()) {
            return new CsvTable(List.of(), List.of());
        }
        List<String> headers = records.get(0).stream().map(String::trim).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.stream().allMatch(value -> value == null || value.isBlank())) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int h = 0; h < headers.size(); h++) {
                row.put(headers.get(h), h < record.size() ? record.get(h).trim() : "");
            }
            rows.add(row);
        }
        return new CsvTable(headers, rows);
    }

    boolean hasHeaders(Set<String> requiredHeaders) {
        Set<String> normalizedHeaders = headers.stream()
                .map(CsvTable::normalizeHeader)
                .collect(Collectors.toSet());
        return requiredHeaders.stream().map(CsvTable::normalizeHeader).allMatch(normalizedHeaders::contains);
    }

    List<String> headers() {
        return headers;
    }

    List<Map<String, String>> rows() {
        return rows;
    }

    static String get(Map<String, String> row, String header) {
        String normalized = normalizeHeader(header);
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (normalizeHeader(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static List<List<String>> parseRecords(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                current.add(cell.toString());
                cell.setLength(0);
            } else if ((ch == '\n' || ch == '\r') && !inQuotes) {
                if (ch == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
                current.add(cell.toString());
                cell.setLength(0);
                records.add(current);
                current = new ArrayList<>();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !current.isEmpty()) {
            current.add(cell.toString());
            records.add(current);
        }
        return records;
    }
}
