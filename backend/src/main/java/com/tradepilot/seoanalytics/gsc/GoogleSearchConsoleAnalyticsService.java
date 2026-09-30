package com.tradepilot.seoanalytics.gsc;

import com.tradepilot.seoanalytics.gsc.dto.SearchConsoleAnalyticsResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleSearchConsoleAnalyticsService {
    private final GoogleSearchConsoleProperties properties;
    private final GoogleSearchConsoleClient client;
    private final SearchConsolePageNormalizer pageNormalizer;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public GoogleSearchConsoleAnalyticsService(
            GoogleSearchConsoleProperties properties,
            GoogleSearchConsoleClient client,
            SearchConsolePageNormalizer pageNormalizer,
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.properties = properties;
        this.client = client;
        this.pageNormalizer = pageNormalizer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public SearchConsoleAnalyticsResponse dashboard(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStart = startDate == null ? resolvedEnd.minusDays(29) : startDate;
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        if (Duration.between(resolvedStart.atStartOfDay(), resolvedEnd.plusDays(1).atStartOfDay()).toDays() > 366) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Date range cannot exceed 366 days");
        }
        if (!properties.isEnabled()) {
            return disabled(resolvedStart, resolvedEnd);
        }
        if (!StringUtils.hasText(properties.getProperty()) || !properties.hasCredentials()) {
            return unavailable(resolvedStart, resolvedEnd, "Search Console is not configured.");
        }

        CacheKey key = new CacheKey(properties.getProperty(), resolvedStart, resolvedEnd);
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh(properties.cacheTtl())) {
            return cached.response();
        }

        try {
            SearchConsoleAnalyticsResponse response = build(resolvedStart, resolvedEnd);
            cache.put(key, new CacheEntry(response, LocalDateTime.now()));
            return response;
        } catch (SearchConsoleUnavailableException ex) {
            return unavailable(resolvedStart, resolvedEnd, "Search Console data is temporarily unavailable.");
        } catch (RuntimeException ex) {
            return unavailable(resolvedStart, resolvedEnd, "Search Console data is temporarily unavailable.");
        }
    }

    private SearchConsoleAnalyticsResponse build(LocalDate startDate, LocalDate endDate) {
        List<SearchConsoleMetricRow> pageRows = client.query(SearchConsoleQueryType.PAGE, startDate, endDate);
        List<SearchConsoleMetricRow> queryRows = client.query(SearchConsoleQueryType.QUERY, startDate, endDate);
        List<SearchConsoleMetricRow> queryPageRows = client.query(SearchConsoleQueryType.QUERY_PAGE, startDate, endDate);
        List<SearchConsoleMetricRow> dateRows = client.query(SearchConsoleQueryType.DATE, startDate, endDate);
        Map<String, ConversionCounts> conversions = conversionCounts(startDate, endDate);

        SearchConsoleAnalyticsResponse.Summary summary = summary(queryRows);
        List<SearchConsoleAnalyticsResponse.PageRow> pages = pageRows.stream()
                .map(row -> pageNormalizer.normalize(row.page()).map(path -> toPageRow(path, row, conversions.getOrDefault(path, ConversionCounts.empty()))).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(SearchConsoleAnalyticsResponse.PageRow::clicks).reversed())
                .toList();
        List<SearchConsoleAnalyticsResponse.QueryPageRow> queries = queryPageRows.stream()
                .map(row -> pageNormalizer.normalize(row.page()).map(path -> toQueryPageRow(path, row, conversions.getOrDefault(path, ConversionCounts.empty()))).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(SearchConsoleAnalyticsResponse.QueryPageRow::clicks).reversed()
                        .thenComparingLong(SearchConsoleAnalyticsResponse.QueryPageRow::impressions).reversed())
                .toList();
        List<SearchConsoleAnalyticsResponse.TrendPoint> trend = dateRows.stream()
                .map(this::toTrendPoint)
                .sorted(Comparator.comparing(SearchConsoleAnalyticsResponse.TrendPoint::date))
                .toList();
        return new SearchConsoleAnalyticsResponse(
                true,
                true,
                "Connected",
                properties.getProperty(),
                startDate,
                endDate,
                Instant.now(),
                null,
                summary,
                pages,
                queries,
                trend,
                opportunities(queries),
                notes()
        );
    }

    private SearchConsoleAnalyticsResponse.PageRow toPageRow(String path, SearchConsoleMetricRow row, ConversionCounts conversions) {
        return new SearchConsoleAnalyticsResponse.PageRow(
                path,
                row.impressions(),
                row.clicks(),
                percent(row.ctr()),
                round(row.position()),
                conversions.signups(),
                conversions.firstImports(),
                conversions.firstInsights(),
                conversions.proConversions(),
                percentRate(conversions.signups(), row.clicks()),
                percentRate(conversions.firstInsights(), row.clicks()),
                percentRate(conversions.proConversions(), row.clicks()),
                per100(conversions.firstInsights(), row.clicks()),
                per100(conversions.proConversions(), row.clicks())
        );
    }

    private SearchConsoleAnalyticsResponse.QueryPageRow toQueryPageRow(String path, SearchConsoleMetricRow row, ConversionCounts conversions) {
        return new SearchConsoleAnalyticsResponse.QueryPageRow(
                row.query(),
                path,
                row.impressions(),
                row.clicks(),
                percent(row.ctr()),
                round(row.position()),
                isBranded(row.query()) ? "Branded" : "Non-Branded",
                conversions.signups(),
                conversions.firstInsights(),
                conversions.proConversions(),
                per100(conversions.firstInsights(), row.clicks()),
                per100(conversions.proConversions(), row.clicks())
        );
    }

    private SearchConsoleAnalyticsResponse.TrendPoint toTrendPoint(SearchConsoleMetricRow row) {
        return new SearchConsoleAnalyticsResponse.TrendPoint(LocalDate.parse(row.date()), row.impressions(), row.clicks(), percent(row.ctr()), round(row.position()));
    }

    private SearchConsoleAnalyticsResponse.Summary summary(List<SearchConsoleMetricRow> queryRows) {
        long impressions = queryRows.stream().mapToLong(SearchConsoleMetricRow::impressions).sum();
        long clicks = queryRows.stream().mapToLong(SearchConsoleMetricRow::clicks).sum();
        long brandedImpressions = queryRows.stream().filter(row -> isBranded(row.query())).mapToLong(SearchConsoleMetricRow::impressions).sum();
        long brandedClicks = queryRows.stream().filter(row -> isBranded(row.query())).mapToLong(SearchConsoleMetricRow::clicks).sum();
        return new SearchConsoleAnalyticsResponse.Summary(
                impressions,
                clicks,
                percentRate(clicks, impressions),
                weightedPosition(queryRows),
                brandedImpressions,
                brandedClicks,
                impressions - brandedImpressions,
                clicks - brandedClicks
        );
    }

    private Map<String, ConversionCounts> conversionCounts(LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", startDate.atStartOfDay())
                .addValue("end", endDate.plusDays(1).atStartOfDay());
        String sql = """
                select landing_page,
                       sum(case when signup_completed_at >= :start and signup_completed_at < :end then 1 else 0 end) as signups,
                       sum(case when first_import_completed_at >= :start and first_import_completed_at < :end then 1 else 0 end) as first_imports,
                       sum(case when first_insight_generated_at >= :start and first_insight_generated_at < :end then 1 else 0 end) as first_insights,
                       sum(case when pro_converted_at >= :start and pro_converted_at < :end then 1 else 0 end) as pro_conversions
                from user_acquisition_attribution uaa
                join users u on u.id = uaa.user_id
                where u.role <> 'ADMIN'
                  and coalesce(u.analytics_excluded, false) = false
                  and ((signup_completed_at >= :start and signup_completed_at < :end)
                   or (first_import_completed_at >= :start and first_import_completed_at < :end)
                   or (first_insight_generated_at >= :start and first_insight_generated_at < :end)
                   or (pro_converted_at >= :start and pro_converted_at < :end))
                group by landing_page
                """;
        return jdbcTemplate.query(sql, params, rs -> {
            Map<String, ConversionCounts> result = new HashMap<>();
            while (rs.next()) {
                String landingPage = rs.getString("landing_page");
                if (StringUtils.hasText(landingPage)) {
                    result.put(landingPage, new ConversionCounts(
                            rs.getLong("signups"),
                            rs.getLong("first_imports"),
                            rs.getLong("first_insights"),
                            rs.getLong("pro_conversions")
                    ));
                }
            }
            return result;
        });
    }

    private List<SearchConsoleAnalyticsResponse.OpportunityRow> opportunities(List<SearchConsoleAnalyticsResponse.QueryPageRow> rows) {
        List<SearchConsoleAnalyticsResponse.OpportunityRow> opportunities = new ArrayList<>();
        double averageCtr = rows.stream().filter(row -> row.impressions() > 0).mapToDouble(SearchConsoleAnalyticsResponse.QueryPageRow::ctr).average().orElse(0);
        for (SearchConsoleAnalyticsResponse.QueryPageRow row : rows) {
            if (row.impressions() >= 100 && row.averagePosition() >= 5 && row.averagePosition() <= 20) {
                opportunities.add(opportunity("Striking Distance", row, "Average position is between 5 and 20 with meaningful impressions."));
            } else if (row.impressions() >= 100 && row.ctr() < averageCtr * 0.6) {
                opportunities.add(opportunity("Low CTR Opportunity", row, "CTR is materially below this dashboard range average."));
            } else if (row.clicks() >= 20 && row.activatedPer100Clicks() >= 5) {
                opportunities.add(opportunity("High Business Value", row, "Landing page has strong first-insight activation per 100 Search Console clicks."));
            } else if (row.clicks() >= 50 && row.activatedPer100Clicks() == 0) {
                opportunities.add(opportunity("Traffic Without Activation", row, "Search clicks are present but this landing page produced no first insights in range."));
            }
            if (opportunities.size() >= 25) break;
        }
        return opportunities;
    }

    private SearchConsoleAnalyticsResponse.OpportunityRow opportunity(String category, SearchConsoleAnalyticsResponse.QueryPageRow row, String reason) {
        return new SearchConsoleAnalyticsResponse.OpportunityRow(category, row.query(), row.landingPage(), row.impressions(), row.clicks(), row.ctr(), row.averagePosition(), row.landingPageFirstInsights(), row.activatedPer100Clicks(), reason);
    }

    private SearchConsoleAnalyticsResponse disabled(LocalDate startDate, LocalDate endDate) {
        return new SearchConsoleAnalyticsResponse(false, false, "Not configured", null, startDate, endDate, null, "Google Search Console is not connected.", emptySummary(), List.of(), List.of(), List.of(), List.of(), notes());
    }

    private SearchConsoleAnalyticsResponse unavailable(LocalDate startDate, LocalDate endDate, String message) {
        return new SearchConsoleAnalyticsResponse(true, false, "Temporarily unavailable", safeProperty(), startDate, endDate, null, message, emptySummary(), List.of(), List.of(), List.of(), List.of(), notes());
    }

    private SearchConsoleAnalyticsResponse.Summary emptySummary() {
        return new SearchConsoleAnalyticsResponse.Summary(0, 0, 0, 0, 0, 0, 0, 0);
    }

    private List<String> notes() {
        return List.of(
                "Search Console data is aggregated and may be delayed; it can differ from real-time TradePilot analytics.",
                "Conversions are joined by normalized landing page only. They are not individually traced Google clicks.",
                "Search Console may omit anonymized or low-volume queries and is queried for web search only."
        );
    }

    private String safeProperty() {
        return StringUtils.hasText(properties.getProperty()) ? properties.getProperty() : null;
    }

    private boolean isBranded(String query) {
        if (!StringUtils.hasText(query)) return false;
        String lower = query.toLowerCase(Locale.ROOT);
        return properties.brandTermList().stream().anyMatch(lower::contains);
    }

    private double weightedPosition(List<SearchConsoleMetricRow> rows) {
        long impressions = rows.stream().mapToLong(SearchConsoleMetricRow::impressions).sum();
        if (impressions <= 0) return 0;
        double weighted = rows.stream().mapToDouble(row -> row.position() * row.impressions()).sum();
        return round(weighted / impressions);
    }

    private double percent(double ratio) {
        return round(ratio * 100);
    }

    private double percentRate(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return round((double) numerator / denominator * 100);
    }

    private double per100(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return round((double) numerator / denominator * 100);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ConversionCounts(long signups, long firstImports, long firstInsights, long proConversions) {
        static ConversionCounts empty() { return new ConversionCounts(0, 0, 0, 0); }
    }

    private record CacheKey(String property, LocalDate startDate, LocalDate endDate) {}

    private record CacheEntry(SearchConsoleAnalyticsResponse response, LocalDateTime cachedAt) {
        boolean isFresh(Duration ttl) {
            return cachedAt.plus(ttl).isAfter(LocalDateTime.now());
        }
    }
}
