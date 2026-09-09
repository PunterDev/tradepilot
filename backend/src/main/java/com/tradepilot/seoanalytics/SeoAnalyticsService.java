package com.tradepilot.seoanalytics;

import com.tradepilot.entity.User;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.seoanalytics.dto.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class SeoAnalyticsService {
    private static final ZoneId DATABASE_ZONE = ZoneOffset.UTC;

    private static final Map<String, String> TOOL_PATHS = Map.of(
            "/tools/trading-expectancy-calculator", "trading_expectancy",
            "/tools/overtrading-calculator", "overtrading",
            "/tools/revenge-trading-calculator", "revenge_trading"
    );

    private final SeoAcquisitionEventRepository eventRepository;
    private final UserAcquisitionAttributionRepository attributionRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SeoAnalyticsService(
            SeoAcquisitionEventRepository eventRepository,
            UserAcquisitionAttributionRepository attributionRepository,
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.eventRepository = eventRepository;
        this.attributionRepository = attributionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void recordEvents(List<SeoAcquisitionEventRequest> requests) {
        if (requests == null || requests.isEmpty()) return;
        Set<String> seenEventIds = new HashSet<>();
        List<SeoAcquisitionEvent> events = requests.stream()
                .limit(25)
                .map(this::toEvent)
                .filter(Objects::nonNull)
                .filter(event -> {
                    if (!StringUtils.hasText(event.getEventId())) return true;
                    return seenEventIds.add(event.getEventId());
                })
                .toList();
        if (!events.isEmpty()) {
            eventRepository.saveAll(events);
        }
    }

    @Transactional
    public void recordSignup(User user, SeoAttributionRequest request) {
        if (user == null || request == null || !StringUtils.hasText(request.getLandingPage())) return;
        if (attributionRepository.findByUserId(user.getId()).isPresent()) return;
        UserAcquisitionAttribution attribution = new UserAcquisitionAttribution();
        attribution.setUserId(user.getId());
        attribution.setAnonymousId(cleanPlain(firstNonBlank(request.getAnonymousVisitorId(), request.getAnonymousId()), 100, "anonymousVisitorId"));
        attribution.setLandingPage(validatePath(request.getLandingPage(), "landingPage"));
        attribution.setInitialReferrer(validateReferrer(request.getInitialReferrer()));
        attribution.setUtmSource(cleanPlain(request.getUtmSource(), 100, "utmSource"));
        attribution.setUtmMedium(cleanPlain(request.getUtmMedium(), 100, "utmMedium"));
        attribution.setUtmCampaign(cleanPlain(request.getUtmCampaign(), 150, "utmCampaign"));
        attribution.setUtmContent(cleanPlain(request.getUtmContent(), 150, "utmContent"));
        attribution.setUtmTerm(cleanPlain(request.getUtmTerm(), 150, "utmTerm"));
        attribution.setFirstSeenAt(parseClientTime(request.getCapturedAt(), LocalDateTime.now()));
        attribution.setSignupCompletedAt(LocalDateTime.now());
        attributionRepository.save(attribution);
    }

    @Transactional
    public void markFirstImportCompleted(User user, LocalDateTime occurredAt) {
        mark(user, occurredAt, "import");
    }

    @Transactional
    public void markFirstInsightGenerated(User user, LocalDateTime occurredAt) {
        mark(user, occurredAt, "insight");
    }

    @Transactional
    public void markProConverted(User user, LocalDateTime occurredAt) {
        if (user == null || user.getPlan() != PlanType.PRO || !hasProEntitlement(user.getSubscriptionStatus())) return;
        mark(user, occurredAt, "pro");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public SeoAnalyticsResponse dashboard(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStart = startDate == null ? resolvedEnd.minusDays(29) : startDate;
        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        if (Duration.between(resolvedStart.atStartOfDay(), resolvedEnd.plusDays(1).atStartOfDay()).toDays() > 366) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range cannot exceed 366 days");
        }
        LocalDateTime start = resolvedStart.atStartOfDay();
        LocalDateTime end = resolvedEnd.plusDays(1).atStartOfDay();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", toDatabaseTimestamp(start))
                .addValue("end", toDatabaseTimestamp(end));

        Map<String, EventCounts> eventCountsByLanding = eventCounts("landing_page", "landing_page", params);
        Map<String, EventCounts> eventCountsByTool = eventCounts("coalesce(tool, '')", "tool_key", params);
        List<UserMilestone> users = userMilestones(params);

        long views = eventCountsByLanding.values().stream().mapToLong(EventCounts::views).sum();
        long ctaClicks = eventCountsByLanding.values().stream().mapToLong(EventCounts::ctaClicks).sum();
        long signupStarted = eventCountsByLanding.values().stream().mapToLong(EventCounts::signupStarted).sum();
        long signups = users.stream().filter(user -> inRange(user.signupCompletedAt(), start, end)).count();
        long firstImports = users.stream().filter(user -> inRange(user.firstImportAt(), start, end)).count();
        long firstInsights = users.stream().filter(user -> inRange(user.firstInsightAt(), start, end)).count();
        long proConversions = users.stream().filter(user -> inRange(user.proConvertedAt(), start, end)).count();

        List<SeoAnalyticsResponse.Kpi> kpis = List.of(
                new SeoAnalyticsResponse.Kpi("views", "Organic landing views", views, 0, "SEO page and tool views"),
                new SeoAnalyticsResponse.Kpi("ctaClicks", "SEO CTA clicks", ctaClicks, rate(ctaClicks, views), "Register and upgrade intent"),
                new SeoAnalyticsResponse.Kpi("signups", "SEO signups", signups, rate(signups, views), "First-touch attributed registrations"),
                new SeoAnalyticsResponse.Kpi("activated", "SEO activated users", firstInsights, rate(firstInsights, signups), "First personalized insight"),
                new SeoAnalyticsResponse.Kpi("pro", "SEO Pro conversions", proConversions, rate(proConversions, signups), "Attributed Pro upgrades")
        );

        List<SeoAnalyticsResponse.FunnelStep> funnel = funnel(views, ctaClicks, signupStarted, signups, firstImports, firstInsights, proConversions);
        List<SeoAnalyticsResponse.LandingPageRow> landingPages = landingRows(eventCountsByLanding, users, start, end);
        List<SeoAnalyticsResponse.ToolRow> tools = toolRows(eventCountsByTool, users, start, end);
        List<SeoAnalyticsResponse.SourceRow> sources = sourceRows(users, eventSourceCounts(params), start, end);
        List<SeoAnalyticsResponse.CampaignRow> campaigns = campaignRows(users, eventCampaignCounts(params), start, end);
        List<SeoAnalyticsResponse.TrendPoint> trend = trend(resolvedStart, resolvedEnd, users);
        SeoAnalyticsResponse.LatencyMetrics latency = latency(users, start, end);
        List<String> notes = List.of(
                "Acquisition data starts when seo_acquisition_events and user_acquisition_attribution are deployed; earlier users cannot be first-touch attributed retroactively.",
                "Anonymous analytics use a random local browser id only. No IP address, fingerprint, calculator inputs, trade P&L, strategy, or journal content is stored.",
                "Pro conversions are counted after TradePilot records a Stripe checkout/admin upgrade timestamp for an attributed SEO user."
        );
        return new SeoAnalyticsResponse(resolvedStart, resolvedEnd, LocalDateTime.now(), kpis, funnel, landingPages, tools, sources, campaigns, trend, latency, notes);
    }

    private SeoAcquisitionEvent toEvent(SeoAcquisitionEventRequest request) {
        if (request == null) return null;
        String eventName = cleanPlain(request.getEventName(), 64, "eventName");
        if (!SeoAcquisitionEventName.isAllowed(eventName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown SEO analytics event");
        }
        String landingPage = validatePath(request.getLandingPage(), "landingPage");
        String eventId = cleanPlain(request.getEventId(), 36, "eventId");
        if (StringUtils.hasText(eventId) && eventRepository.existsByEventId(eventId)) return null;
        SeoAcquisitionEvent event = new SeoAcquisitionEvent();
        event.setEventId(eventId);
        event.setEventName(eventName);
        event.setAnonymousId(cleanPlain(firstNonBlank(request.getAnonymousVisitorId(), request.getAnonymousId()), 100, "anonymousVisitorId"));
        event.setLandingPage(landingPage);
        event.setTool(cleanPlain(request.getTool(), 100, "tool"));
        event.setCtaLabel(cleanPlain(request.getCtaLabel(), 120, "ctaLabel"));
        event.setCtaDestination(validateOptionalPath(request.getCtaDestination(), "ctaDestination"));
        event.setReferrer(validateReferrer(request.getReferrer()));
        event.setUtmSource(cleanPlain(request.getUtmSource(), 100, "utmSource"));
        event.setUtmMedium(cleanPlain(request.getUtmMedium(), 100, "utmMedium"));
        event.setUtmCampaign(cleanPlain(request.getUtmCampaign(), 150, "utmCampaign"));
        event.setUtmContent(cleanPlain(request.getUtmContent(), 150, "utmContent"));
        event.setUtmTerm(cleanPlain(request.getUtmTerm(), 150, "utmTerm"));
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }

    private void mark(User user, LocalDateTime occurredAt, String field) {
        if (user == null) return;
        attributionRepository.findByUserId(user.getId()).ifPresent(attribution -> {
            LocalDateTime value = occurredAt == null ? LocalDateTime.now() : occurredAt;
            if ("import".equals(field) && attribution.getFirstImportCompletedAt() == null) {
                attribution.setFirstImportCompletedAt(value);
            } else if ("insight".equals(field) && attribution.getFirstInsightGeneratedAt() == null) {
                attribution.setFirstInsightGeneratedAt(value);
            } else if ("pro".equals(field) && attribution.getProConvertedAt() == null) {
                attribution.setProConvertedAt(value);
            }
            attributionRepository.save(attribution);
        });
    }

    private Map<String, EventCounts> eventCounts(String selectExpr, String alias, MapSqlParameterSource params) {
        String sql = """
                select %s as %s,
                       sum(case when event_name in ('seo_landing_page_view','tool_page_view') then 1 else 0 end) as views,
                       sum(case when event_name = 'seo_cta_click' or event_name like '%%_cta_click' then 1 else 0 end) as cta_clicks,
                       sum(case when event_name = 'signup_started_from_seo' then 1 else 0 end) as signup_started,
                       sum(case when event_name like '%%_started' then 1 else 0 end) as started,
                       sum(case when event_name like '%%_completed' then 1 else 0 end) as completed
                from seo_acquisition_events
                where occurred_at >= :start and occurred_at < :end
                group by %s
                """.formatted(selectExpr, alias, selectExpr);
        return jdbcTemplate.query(sql, params, rs -> {
            Map<String, EventCounts> rows = new LinkedHashMap<>();
            while (rs.next()) {
                String key = Optional.ofNullable(rs.getString(alias)).filter(StringUtils::hasText).orElse("unknown");
                rows.put(key, new EventCounts(rs.getLong("views"), rs.getLong("cta_clicks"), rs.getLong("signup_started"), rs.getLong("started"), rs.getLong("completed")));
            }
            return rows;
        });
    }

    private List<UserMilestone> userMilestones(MapSqlParameterSource params) {
        String sql = """
                select uaa.user_id, uaa.landing_page, uaa.utm_source, uaa.utm_medium, uaa.utm_campaign,
                       uaa.signup_completed_at,
                       coalesce(uaa.first_import_completed_at, (
                           select min(coalesce(e.occurred_at, e.created_at)) from feature_usage_events e
                           where e.user_id = uaa.user_id and e.action = 'FIRST_IMPORT_COMPLETED'
                       )) as first_import_at,
                       coalesce(uaa.first_insight_generated_at, (
                           select min(coalesce(e.occurred_at, e.created_at)) from feature_usage_events e
                           where e.user_id = uaa.user_id and e.action = 'FIRST_INSIGHT_GENERATED'
                       )) as first_insight_at,
                       coalesce(uaa.pro_converted_at, (
                           select min(coalesce(e.occurred_at, e.created_at)) from feature_usage_events e
                           where e.user_id = uaa.user_id and e.action = 'PRO_CONVERSION'
                       )) as pro_converted_at
                from user_acquisition_attribution uaa
                where (uaa.signup_completed_at >= :start and uaa.signup_completed_at < :end)
                   or (uaa.first_import_completed_at >= :start and uaa.first_import_completed_at < :end)
                   or (uaa.first_insight_generated_at >= :start and uaa.first_insight_generated_at < :end)
                   or (uaa.pro_converted_at >= :start and uaa.pro_converted_at < :end)
                """;
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new UserMilestone(
                rs.getLong("user_id"),
                rs.getString("landing_page"),
                sourceLabel(rs.getString("utm_source")),
                mediumLabel(rs.getString("utm_medium")),
                campaignLabel(rs.getString("utm_campaign")),
                fromDatabaseTimestamp(rs.getTimestamp("signup_completed_at")),
                fromDatabaseTimestamp(rs.getTimestamp("first_import_at")),
                fromDatabaseTimestamp(rs.getTimestamp("first_insight_at")),
                fromDatabaseTimestamp(rs.getTimestamp("pro_converted_at"))
        ));
    }

    private Timestamp toDatabaseTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value.atZone(ZoneId.systemDefault()).withZoneSameInstant(DATABASE_ZONE).toLocalDateTime());
    }

    private LocalDateTime fromDatabaseTimestamp(Timestamp value) {
        if (value == null) return null;
        return value.toLocalDateTime().atZone(DATABASE_ZONE).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Map<SourceKey, EventCounts> eventSourceCounts(MapSqlParameterSource params) {
        String sql = """
                select coalesce(nullif(utm_source,''),'direct/unknown') as source,
                       coalesce(nullif(utm_medium,''),'unknown') as medium,
                       sum(case when event_name in ('seo_landing_page_view','tool_page_view') then 1 else 0 end) as views,
                       sum(case when event_name = 'seo_cta_click' or event_name like '%%_cta_click' then 1 else 0 end) as cta_clicks
                from seo_acquisition_events
                where occurred_at >= :start and occurred_at < :end
                group by coalesce(nullif(utm_source,''),'direct/unknown'), coalesce(nullif(utm_medium,''),'unknown')
                """;
        return jdbcTemplate.query(sql, params, rs -> {
            Map<SourceKey, EventCounts> rows = new LinkedHashMap<>();
            while (rs.next()) rows.put(new SourceKey(rs.getString("source"), rs.getString("medium")), new EventCounts(rs.getLong("views"), rs.getLong("cta_clicks"), 0, 0, 0));
            return rows;
        });
    }

    private Map<String, EventCounts> eventCampaignCounts(MapSqlParameterSource params) {
        String sql = """
                select coalesce(nullif(utm_campaign,''),'uncategorized') as campaign,
                       sum(case when event_name in ('seo_landing_page_view','tool_page_view') then 1 else 0 end) as views
                from seo_acquisition_events
                where occurred_at >= :start and occurred_at < :end
                group by coalesce(nullif(utm_campaign,''),'uncategorized')
                """;
        return jdbcTemplate.query(sql, params, rs -> {
            Map<String, EventCounts> rows = new LinkedHashMap<>();
            while (rs.next()) rows.put(rs.getString("campaign"), new EventCounts(rs.getLong("views"), 0, 0, 0, 0));
            return rows;
        });
    }

    private List<SeoAnalyticsResponse.FunnelStep> funnel(long views, long ctaClicks, long signupStarted, long signups, long firstImports, long firstInsights, long proConversions) {
        long[] counts = { views, ctaClicks, signupStarted, signups, firstImports, firstInsights, proConversions };
        String[] keys = { "landing_view", "cta_click", "signup_started", "signup_completed", "first_import", "first_insight", "pro_upgrade" };
        String[] labels = { "Landing Page View", "CTA Click", "Signup Started", "Signup Completed", "First Import", "First Insight", "Pro Upgrade" };
        List<SeoAnalyticsResponse.FunnelStep> steps = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            steps.add(new SeoAnalyticsResponse.FunnelStep(keys[i], labels[i], counts[i], i == 0 ? 100 : rate(counts[i], counts[i - 1]), rate(counts[i], counts[0])));
        }
        return steps;
    }

    private List<SeoAnalyticsResponse.LandingPageRow> landingRows(Map<String, EventCounts> eventCounts, List<UserMilestone> users, LocalDateTime start, LocalDateTime end) {
        Set<String> pages = new TreeSet<>(eventCounts.keySet());
        users.forEach(user -> pages.add(user.landingPage()));
        return pages.stream().map(page -> {
            EventCounts events = eventCounts.getOrDefault(page, EventCounts.empty());
            List<UserMilestone> pageUsers = users.stream().filter(user -> Objects.equals(user.landingPage(), page)).toList();
            long signups = count(pageUsers, UserMilestone::signupCompletedAt, start, end);
            long imports = count(pageUsers, UserMilestone::firstImportAt, start, end);
            long insights = count(pageUsers, UserMilestone::firstInsightAt, start, end);
            long pro = count(pageUsers, UserMilestone::proConvertedAt, start, end);
            return new SeoAnalyticsResponse.LandingPageRow(page, events.views(), events.ctaClicks(), signups, imports, insights, pro, rate(signups, events.views()), rate(insights, signups), rate(pro, signups));
        }).sorted(Comparator.comparingLong(SeoAnalyticsResponse.LandingPageRow::firstInsights).reversed()).toList();
    }

    private List<SeoAnalyticsResponse.ToolRow> toolRows(Map<String, EventCounts> eventCounts, List<UserMilestone> users, LocalDateTime start, LocalDateTime end) {
        Set<String> tools = new TreeSet<>(TOOL_PATHS.values());
        tools.addAll(eventCounts.keySet());
        tools.remove("unknown");
        return tools.stream().map(tool -> {
            EventCounts events = eventCounts.getOrDefault(tool, EventCounts.empty());
            List<UserMilestone> toolUsers = users.stream().filter(user -> Objects.equals(toolFromLanding(user.landingPage()), tool)).toList();
            long signups = count(toolUsers, UserMilestone::signupCompletedAt, start, end);
            long imports = count(toolUsers, UserMilestone::firstImportAt, start, end);
            long insights = count(toolUsers, UserMilestone::firstInsightAt, start, end);
            long pro = count(toolUsers, UserMilestone::proConvertedAt, start, end);
            return new SeoAnalyticsResponse.ToolRow(tool, events.views(), events.started(), events.completed(), events.ctaClicks(), signups, imports, insights, pro, rate(events.completed(), events.started()), rate(signups, events.views()), rate(insights, signups), rate(pro, signups));
        }).toList();
    }

    private List<SeoAnalyticsResponse.SourceRow> sourceRows(List<UserMilestone> users, Map<SourceKey, EventCounts> eventCounts, LocalDateTime start, LocalDateTime end) {
        Set<SourceKey> keys = new TreeSet<>(Comparator.comparing(SourceKey::source).thenComparing(SourceKey::medium));
        keys.addAll(eventCounts.keySet());
        users.forEach(user -> keys.add(new SourceKey(user.source(), user.medium())));
        return keys.stream().map(key -> {
            EventCounts events = eventCounts.getOrDefault(key, EventCounts.empty());
            List<UserMilestone> sourceUsers = users.stream().filter(user -> Objects.equals(user.source(), key.source()) && Objects.equals(user.medium(), key.medium())).toList();
            long signups = count(sourceUsers, UserMilestone::signupCompletedAt, start, end);
            long insights = count(sourceUsers, UserMilestone::firstInsightAt, start, end);
            long pro = count(sourceUsers, UserMilestone::proConvertedAt, start, end);
            return new SeoAnalyticsResponse.SourceRow(key.source(), key.medium(), events.views(), events.ctaClicks(), signups, insights, pro, rate(signups, events.views()), rate(insights, signups), rate(pro, signups));
        }).sorted(Comparator.comparingLong(SeoAnalyticsResponse.SourceRow::firstInsights).reversed()).toList();
    }

    private List<SeoAnalyticsResponse.CampaignRow> campaignRows(List<UserMilestone> users, Map<String, EventCounts> eventCounts, LocalDateTime start, LocalDateTime end) {
        Set<String> campaigns = new TreeSet<>(eventCounts.keySet());
        users.forEach(user -> campaigns.add(user.campaign()));
        return campaigns.stream().map(campaign -> {
            EventCounts events = eventCounts.getOrDefault(campaign, EventCounts.empty());
            List<UserMilestone> campaignUsers = users.stream().filter(user -> Objects.equals(user.campaign(), campaign)).toList();
            long signups = count(campaignUsers, UserMilestone::signupCompletedAt, start, end);
            long insights = count(campaignUsers, UserMilestone::firstInsightAt, start, end);
            long pro = count(campaignUsers, UserMilestone::proConvertedAt, start, end);
            return new SeoAnalyticsResponse.CampaignRow(campaign, events.views(), signups, insights, pro, rate(signups, events.views()), rate(insights, signups), rate(pro, signups));
        }).sorted(Comparator.comparingLong(SeoAnalyticsResponse.CampaignRow::firstInsights).reversed()).toList();
    }

    private List<SeoAnalyticsResponse.TrendPoint> trend(LocalDate start, LocalDate end, List<UserMilestone> users) {
        List<SeoAnalyticsResponse.TrendPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            LocalDate current = day;
            long signups = users.stream().filter(user -> user.signupCompletedAt() != null && user.signupCompletedAt().toLocalDate().equals(current)).count();
            long insights = users.stream().filter(user -> user.firstInsightAt() != null && user.firstInsightAt().toLocalDate().equals(current)).count();
            points.add(new SeoAnalyticsResponse.TrendPoint(current, signups, insights));
        }
        return points;
    }

    private SeoAnalyticsResponse.LatencyMetrics latency(List<UserMilestone> users, LocalDateTime start, LocalDateTime end) {
        List<Double> importMinutes = users.stream()
                .filter(user -> inRange(user.signupCompletedAt(), start, end) && user.firstImportAt() != null)
                .map(user -> (double) Duration.between(user.signupCompletedAt(), user.firstImportAt()).toMinutes())
                .filter(value -> value >= 0)
                .sorted()
                .toList();
        List<Double> insightMinutes = users.stream()
                .filter(user -> inRange(user.signupCompletedAt(), start, end) && user.firstInsightAt() != null)
                .map(user -> (double) Duration.between(user.signupCompletedAt(), user.firstInsightAt()).toMinutes())
                .filter(value -> value >= 0)
                .sorted()
                .toList();
        return new SeoAnalyticsResponse.LatencyMetrics(median(importMinutes), median(insightMinutes));
    }

    private long count(List<UserMilestone> users, java.util.function.Function<UserMilestone, LocalDateTime> getter, LocalDateTime start, LocalDateTime end) {
        return users.stream().filter(user -> inRange(getter.apply(user), start, end)).count();
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return Math.round((double) numerator / denominator * 1000.0) / 10.0;
    }

    private Double median(List<Double> values) {
        if (values.isEmpty()) return null;
        int mid = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(mid) : (values.get(mid - 1) + values.get(mid)) / 2.0;
    }

    private String toolFromLanding(String landingPage) {
        return TOOL_PATHS.getOrDefault(landingPage, null);
    }

    private String sourceLabel(String source) {
        return StringUtils.hasText(source) ? source : "direct/unknown";
    }

    private String mediumLabel(String medium) {
        return StringUtils.hasText(medium) ? medium : "unknown";
    }

    private String campaignLabel(String campaign) {
        return StringUtils.hasText(campaign) ? campaign : "uncategorized";
    }

    private String cleanPlain(String value, int max, String fieldName) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max || containsControlCharacter(trimmed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is invalid");
        }
        return trimmed;
    }

    private String validatePath(String value, String fieldName) {
        String trimmed = cleanPlain(value, 256, fieldName);
        if (!StringUtils.hasText(trimmed)
                || !trimmed.startsWith("/")
                || trimmed.startsWith("//")
                || trimmed.contains("?")
                || trimmed.contains("#")
                || trimmed.toLowerCase(Locale.ROOT).contains("javascript:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a safe path");
        }
        return trimmed;
    }

    private String validateOptionalPath(String value, String fieldName) {
        if (!StringUtils.hasText(value)) return null;
        return validatePath(value, fieldName);
    }

    private String validateReferrer(String value) {
        String trimmed = cleanPlain(value, 500, "referrer");
        if (!StringUtils.hasText(trimmed)) return null;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referrer is invalid");
        }
        return trimmed;
    }

    private boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(ch -> ch < 32 || ch == 127);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private boolean hasProEntitlement(SubscriptionStatus status) {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING || status == SubscriptionStatus.PAST_DUE;
    }

    private LocalDateTime parseClientTime(String value, LocalDateTime fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        try {
            return Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignored) {
                return fallback;
            }
        }
    }

    private record EventCounts(long views, long ctaClicks, long signupStarted, long started, long completed) {
        static EventCounts empty() { return new EventCounts(0, 0, 0, 0, 0); }
    }

    private record UserMilestone(Long userId, String landingPage, String source, String medium, String campaign, LocalDateTime signupCompletedAt, LocalDateTime firstImportAt, LocalDateTime firstInsightAt, LocalDateTime proConvertedAt) {}
    private record SourceKey(String source, String medium) {}
}
