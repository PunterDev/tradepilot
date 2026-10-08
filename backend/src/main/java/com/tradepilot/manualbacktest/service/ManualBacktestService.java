package com.tradepilot.manualbacktest.service;

import com.tradepilot.backtest.model.BacktestBar;
import com.tradepilot.entity.User;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.manualbacktest.dto.ManualBacktestDtos.*;
import com.tradepilot.manualbacktest.entity.*;
import com.tradepilot.manualbacktest.repository.*;
import com.tradepilot.marketdata.entity.MarketBar;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.marketdata.repository.MarketBarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class ManualBacktestService {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final String TIMEZONE = "America/New_York";
    private final ManualBacktestSessionRepository sessions; private final ManualBacktestTradeRepository trades; private final ManualBacktestTradeEventRepository events;
    private final MarketBarRepository marketBars; private final FeatureEntitlementService entitlements;

    public ManualBacktestService(ManualBacktestSessionRepository sessions, ManualBacktestTradeRepository trades, ManualBacktestTradeEventRepository events, MarketBarRepository marketBars, FeatureEntitlementService entitlements) {
        this.sessions=sessions; this.trades=trades; this.events=events; this.marketBars=marketBars; this.entitlements=entitlements;
    }

    public Access access(User user) { return new Access(entitlements.hasEntitlement(user, FeatureEntitlementKey.MANUAL_BACKTEST_BETA), "MANUAL_BACKTEST_BETA", "Manual Backtest", "Private Research"); }
    public List<Session> list(User user) { require(user); return sessions.findTop30ByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::session).toList(); }
    public Session get(User user, Long id) { require(user); return session(owned(user, id)); }

    @Transactional
    public Session create(User user, CreateSession request) {
        require(user);
        if (request == null || request.symbol() == null || request.tradingDate() == null) throw new IllegalArgumentException("Symbol and trading date are required.");
        LocalTime startLocal = request.replayStart() == null ? LocalTime.of(9,30) : request.replayStart();
        LocalTime endLocal = request.replayEnd() == null ? LocalTime.of(11,0) : request.replayEnd();
        if (!endLocal.isAfter(startLocal)) throw new IllegalArgumentException("Replay end must be after replay start.");
        Instant start = ZonedDateTime.of(request.tradingDate(), startLocal, NEW_YORK).toInstant();
        Instant end = ZonedDateTime.of(request.tradingDate(), endLocal, NEW_YORK).toInstant();
        List<MarketBar> coverage = marketBars.findBySymbolAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(request.symbol(), "1m", start, end.plus(Duration.ofMinutes(1)));
        if (coverage.stream().noneMatch(bar -> !bar.getTsEvent().isBefore(start) && !bar.getTsEvent().isAfter(end))) throw new IllegalArgumentException("Historical replay data is not available for this session.");
        Instant initialCursor = coverage.stream().filter(bar -> bar.getTsEvent().isBefore(start)).map(MarketBar::getTsEvent).max(Instant::compareTo).orElse(start.minus(Duration.ofMinutes(1)));
        ManualBacktestSession saved = new ManualBacktestSession();
        saved.setUserId(user.getId()); saved.setSymbol(request.symbol()); saved.setContinuousSymbol(request.symbol().continuousSymbol()); saved.setTradingDate(request.tradingDate()); saved.setTimezone(TIMEZONE);
        saved.setStartTime(start); saved.setEndTime(end); saved.setCursorTs(initialCursor); saved.setStatus(ManualBacktestSession.Status.ACTIVE); saved.setRiskProfile(request.riskProfile() == null ? ManualBacktestSession.RiskProfile.UNIFORM_1R : request.riskProfile()); saved.setNotes(trim(request.notes())); saved.setCreatedAt(Instant.now()); saved.setUpdatedAt(Instant.now());
        return session(sessions.save(saved));
    }

    @Transactional
    public Session advance(User user, Long id, Advance request) {
        require(user); ManualBacktestSession session = locked(user, id); active(session);
        int count = request == null ? 1 : request.count(); if (count < 1 || count > 5) throw new IllegalArgumentException("Replay advance must be between 1 and 5 bars.");
        if (request.expectedCursorTs() == null || !request.expectedCursorTs().equals(session.getCursorTs())) throw new IllegalStateException("Replay cursor is stale. Reload the session and try again.");
        List<MarketBar> available = afterCursor(session);
        if (available.isEmpty()) { closeAtSessionEnd(session, null); return session(sessions.save(session)); }
        for (MarketBar bar : available.subList(0, Math.min(count, available.size()))) {
            processBar(session, bar); session.setCursorTs(bar.getTsEvent());
            if (!bar.getTsEvent().isBefore(session.getEndTime())) { closeAtSessionEnd(session, bar); break; }
        }
        session.setUpdatedAt(Instant.now()); return session(sessions.save(session));
    }

    public Chart chart(User user, Long id, String timeframe) {
        require(user); ManualBacktestSession session = owned(user, id); String normalized = timeframe == null ? "1m" : timeframe.toLowerCase(Locale.ROOT);
        Duration duration = switch (normalized) { case "1m" -> Duration.ofMinutes(1); case "5m" -> Duration.ofMinutes(5); case "1h" -> Duration.ofHours(1); case "4h" -> Duration.ofHours(4); default -> throw new IllegalArgumentException("Chart timeframe must be 1m, 5m, 1h, or 4h."); };
        Instant contextStart = contextStart(session.getCursorTs(), normalized);
        List<BacktestBar> revealed = marketBars.findBySymbolAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(session.getSymbol(), "1m", contextStart, session.getCursorTs().plus(Duration.ofMinutes(1))).stream()
                .filter(bar -> !bar.getTsEvent().isAfter(session.getCursorTs())).map(this::bar).toList();
        List<Candle> display = "1m".equals(normalized)
                ? revealed.stream().map(value -> candle(value, true)).toList()
                : new ReplayAggregationService().aggregate(revealed, duration).stream().map(value -> candle(value.bar(), value.complete())).toList();
        return new Chart(session.getSymbol().name(), session.getContinuousSymbol(), TIMEZONE, normalized, session.getCursorTs(), display, activeTrade(session), latestClosedTrade(session));
    }

    @Transactional
    public Trade placeOrder(User user, Long id, Order request) {
        require(user); ManualBacktestSession session = locked(user, id); active(session);
        if (request == null || request.direction() == null || request.orderType() == null || request.stopPrice() == null || request.targetPrice() == null) throw new IllegalArgumentException("Direction, order type, stop, and target are required.");
        if (activeTradeEntity(session).isPresent()) throw new IllegalStateException("Only one pending or open position is allowed.");
        BigDecimal stop = executable(request.stopPrice(), session.getSymbol()); BigDecimal target = executable(request.targetPrice(), session.getSymbol()); BigDecimal reference = latestRevealedClose(session);
        BigDecimal configuredEntry = request.orderType() == ManualBacktestTrade.EntryOrderType.MARKET ? null : executable(requiredEntry(request.configuredEntryPrice()), session.getSymbol());
        BigDecimal validationEntry = configuredEntry == null ? reference : configuredEntry;
        valid(request.direction(), validationEntry, stop, target); validateOrderSide(request.direction(), request.orderType(), configuredEntry, reference);
        ManualBacktestTrade trade = new ManualBacktestTrade(); trade.setSessionId(session.getId()); trade.setUserId(user.getId()); trade.setSymbol(session.getSymbol()); trade.setDirection(request.direction()); trade.setOrderPlacedTs(session.getCursorTs()); trade.setEntryOrderType(request.orderType()); trade.setConfiguredEntryPrice(configuredEntry); trade.setInitialStopPrice(stop); trade.setCurrentStopPrice(stop); trade.setTargetPrice(target); trade.setRiskMultiplier(multiplier(session)); trade.setStatus(ManualBacktestTrade.Status.PENDING); trade.setPendingAction("ENTRY"); trade.setNotes(trim(request.notes())); trade.setCreatedAt(Instant.now()); trade.setUpdatedAt(Instant.now());
        trade = trades.save(trade); event(trade, session, "ORDER_PLACED", null, configuredEntry); return trade(trade);
    }

    @Transactional
    public Trade updatePosition(User user, Long id, PositionChange request) {
        require(user); ManualBacktestSession session=locked(user,id); active(session); ManualBacktestTrade trade=activeTradeEntity(session).orElseThrow(()->new IllegalStateException("No active order or position."));
        if (request == null || request.stopPrice() == null || request.targetPrice() == null) throw new IllegalArgumentException("Stop and target are required.");
        BigDecimal stop=executable(request.stopPrice(),session.getSymbol()), target=executable(request.targetPrice(),session.getSymbol()); BigDecimal entry=trade.getStatus()==ManualBacktestTrade.Status.PENDING ? executable(requiredEntry(request.entryPrice()),session.getSymbol()) : trade.getEntryPrice(); valid(trade.getDirection(), entry, stop, target);
        if (trade.getStatus()==ManualBacktestTrade.Status.PENDING) { BigDecimal reference=latestRevealedClose(session); validateOrderSide(trade.getDirection(),trade.getEntryOrderType(),entry,reference); BigDecimal oldEntry=trade.getConfiguredEntryPrice(); trade.setConfiguredEntryPrice(entry); event(trade,session,"ENTRY_CHANGED",oldEntry,entry); }
        BigDecimal oldStop=trade.getCurrentStopPrice(),oldTarget=trade.getTargetPrice(); trade.setCurrentStopPrice(stop); trade.setTargetPrice(target); if (request.notes()!=null) trade.setNotes(trim(request.notes())); trade.setUpdatedAt(Instant.now()); trade=trades.save(trade); event(trade,session,"STOP_CHANGED",oldStop,stop); event(trade,session,"TARGET_CHANGED",oldTarget,target); return trade(trade);
    }
    @Transactional
    public Trade cancelPendingOrder(User user, Long id) { require(user); ManualBacktestSession session=locked(user,id); active(session); ManualBacktestTrade trade=activeTradeEntity(session).filter(value->value.getStatus()==ManualBacktestTrade.Status.PENDING).orElseThrow(()->new IllegalStateException("No pending order.")); trade.setStatus(ManualBacktestTrade.Status.CANCELLED); trade.setPendingAction(null); trade.setUpdatedAt(Instant.now()); trade=trades.save(trade); event(trade,session,"ORDER_CANCELLED",null,null); return trade(trade); }

    @Transactional
    public Trade breakEven(User user, Long id) { require(user); ManualBacktestSession session=locked(user,id); active(session); ManualBacktestTrade trade=openTrade(session); BigDecimal old=trade.getCurrentStopPrice(); trade.setCurrentStopPrice(trade.getEntryPrice()); trade.setUpdatedAt(Instant.now()); trade=trades.save(trade); event(trade,session,"STOP_MOVED_TO_BE",old,trade.getEntryPrice()); return trade(trade); }
    @Transactional
    public Trade flatten(User user, Long id) { require(user); ManualBacktestSession session=locked(user,id); active(session); ManualBacktestTrade trade=openTrade(session); if ("FLATTEN".equals(trade.getPendingAction())) return trade(trade); trade.setPendingAction("FLATTEN"); trade.setUpdatedAt(Instant.now()); trade=trades.save(trade); event(trade,session,"FLATTEN_REQUESTED",null,null); return trade(trade); }
    @Transactional
    public Session complete(User user, Long id) { require(user); ManualBacktestSession session=locked(user,id); active(session); if (session.getCursorTs().isBefore(session.getEndTime())) throw new IllegalStateException("Replay must reach its configured end before completion."); closeAtSessionEnd(session, null); return session(sessions.save(session)); }

    private void processBar(ManualBacktestSession session, MarketBar bar) {
        activeTradeEntity(session).ifPresent(trade -> {
            if (trade.getStatus() == ManualBacktestTrade.Status.PENDING) fillPending(trade, session, bar);
            else if ("FLATTEN".equals(trade.getPendingAction())) close(trade, session, bar.getTsEvent(), bar.getOpen(), ManualBacktestTrade.ExitReason.MANUAL);
            else evaluateOpen(trade, session, bar);
        });
    }
    private void fillPending(ManualBacktestTrade trade, ManualBacktestSession session, MarketBar bar) {
        BigDecimal entry=entryFill(trade, bar);
        if (entry==null) return;
        try { valid(trade.getDirection(), entry, trade.getCurrentStopPrice(), trade.getTargetPrice()); }
        catch (IllegalArgumentException exception) { trade.setStatus(ManualBacktestTrade.Status.CANCELLED); trade.setPendingAction(null); trade.setUpdatedAt(Instant.now()); trades.save(trade); event(trade,session,"MARKET_ORDER_CANCELLED",null,null); return; }
        trade.setEntryTs(bar.getTsEvent()); trade.setEntryPrice(entry); trade.setInstrumentId(bar.getInstrumentId()); trade.setInitialRiskPoints(entry.subtract(trade.getInitialStopPrice()).abs()); trade.setStatus(ManualBacktestTrade.Status.OPEN); trade.setPendingAction(null); trade.setUpdatedAt(Instant.now()); trades.save(trade); event(trade,session,"ORDER_FILLED",trade.getConfiguredEntryPrice(),entry); evaluateAfterFill(trade,session,bar);
    }
    private BigDecimal entryFill(ManualBacktestTrade trade, MarketBar bar) {
        return ManualOrderExecution.entryFill(trade.getDirection(), trade.getEntryOrderType(), trade.getConfiguredEntryPrice(), bar.getOpen(), bar.getHigh(), bar.getLow());
    }
    private void evaluateAfterFill(ManualBacktestTrade trade, ManualBacktestSession session, MarketBar bar) {
        boolean intrabarStopEntry=(trade.getEntryOrderType()==ManualBacktestTrade.EntryOrderType.STOP && ((trade.getDirection()==ManualBacktestTrade.Direction.LONG&&bar.getOpen().compareTo(trade.getEntryPrice())<0)||(trade.getDirection()==ManualBacktestTrade.Direction.SHORT&&bar.getOpen().compareTo(trade.getEntryPrice())>0)));
        boolean intrabarLimitEntry=(trade.getEntryOrderType()==ManualBacktestTrade.EntryOrderType.LIMIT && ((trade.getDirection()==ManualBacktestTrade.Direction.LONG&&bar.getOpen().compareTo(trade.getEntryPrice())>0)||(trade.getDirection()==ManualBacktestTrade.Direction.SHORT&&bar.getOpen().compareTo(trade.getEntryPrice())<0)));
        if (intrabarStopEntry || intrabarLimitEntry) { // Pre-entry movement is unknowable; only retain the directionally reachable post-trigger extreme.
            boolean target=trade.getDirection()==ManualBacktestTrade.Direction.LONG ? bar.getHigh().compareTo(trade.getTargetPrice())>=0 : bar.getLow().compareTo(trade.getTargetPrice())<=0;
            boolean stop=trade.getDirection()==ManualBacktestTrade.Direction.LONG ? bar.getLow().compareTo(trade.getCurrentStopPrice())<=0 : bar.getHigh().compareTo(trade.getCurrentStopPrice())>=0;
            if (intrabarLimitEntry) { if(stop) close(trade,session,bar.getTsEvent(),trade.getCurrentStopPrice(),ManualBacktestTrade.ExitReason.STOP); return; }
            if(target) close(trade,session,bar.getTsEvent(),trade.getTargetPrice(),ManualBacktestTrade.ExitReason.TARGET); return;
        }
        evaluateOpen(trade,session,bar);
    }
    private void evaluateOpen(ManualBacktestTrade trade, ManualBacktestSession session, MarketBar bar) {
        boolean stop=trade.getDirection()==ManualBacktestTrade.Direction.LONG ? bar.getLow().compareTo(trade.getCurrentStopPrice())<=0 : bar.getHigh().compareTo(trade.getCurrentStopPrice())>=0;
        boolean target=trade.getDirection()==ManualBacktestTrade.Direction.LONG ? bar.getHigh().compareTo(trade.getTargetPrice())>=0 : bar.getLow().compareTo(trade.getTargetPrice())<=0;
        if (stop) close(trade,session,bar.getTsEvent(),trade.getCurrentStopPrice(),ManualBacktestTrade.ExitReason.STOP);
        else if (target) close(trade,session,bar.getTsEvent(),trade.getTargetPrice(),ManualBacktestTrade.ExitReason.TARGET);
    }
    private void close(ManualBacktestTrade trade, ManualBacktestSession session, Instant time, BigDecimal price, ManualBacktestTrade.ExitReason reason) {
        BigDecimal risk=trade.getInitialRiskPoints(); BigDecimal raw=(trade.getDirection()==ManualBacktestTrade.Direction.LONG ? price.subtract(trade.getEntryPrice()) : trade.getEntryPrice().subtract(price)).divide(risk,6,RoundingMode.HALF_UP);
        trade.setExitTs(time); trade.setExitPrice(price); trade.setExitReason(reason); trade.setRawR(raw); trade.setWeightedR(raw.multiply(trade.getRiskMultiplier()).setScale(6,RoundingMode.HALF_UP)); trade.setStatus(ManualBacktestTrade.Status.CLOSED); trade.setPendingAction(null); trade.setUpdatedAt(Instant.now()); trades.save(trade); event(trade,session,"TRADE_CLOSED",null,price);
    }
    private void closeAtSessionEnd(ManualBacktestSession session, MarketBar finalBar) {
        activeTradeEntity(session).ifPresent(trade -> { if(trade.getStatus()==ManualBacktestTrade.Status.OPEN) { MarketBar bar=finalBar==null?latestBar(session):finalBar; if(bar!=null)close(trade,session,bar.getTsEvent(),bar.getClose(),ManualBacktestTrade.ExitReason.SESSION_END); } else { trade.setStatus(ManualBacktestTrade.Status.CANCELLED);trade.setPendingAction(null);trades.save(trade);event(trade,session,"ORDER_CANCELLED",null,null); } });
        session.setStatus(ManualBacktestSession.Status.COMPLETED); session.setCompletedAt(Instant.now()); session.setUpdatedAt(Instant.now());
    }
    private List<MarketBar> afterCursor(ManualBacktestSession session) { return marketBars.findBySymbolAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(session.getSymbol(),"1m",session.getCursorTs().plusMillis(1),session.getEndTime().plus(Duration.ofMinutes(1))).stream().filter(bar -> !bar.getTsEvent().isAfter(session.getEndTime())).toList(); }
    private MarketBar latestBar(ManualBacktestSession session) { return marketBars.findBySymbolAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(session.getSymbol(),"1m",session.getCursorTs(),session.getCursorTs().plus(Duration.ofMinutes(1))).stream().findFirst().orElse(null); }
    private BigDecimal latestRevealedClose(ManualBacktestSession session) { MarketBar latest=latestBar(session); if(latest==null) throw new IllegalStateException("Advance one bar before placing a market order."); return latest.getClose(); }
    private Optional<ManualBacktestTrade> activeTradeEntity(ManualBacktestSession session) { return trades.findFirstBySessionIdAndUserIdAndStatusInOrderByCreatedAtDesc(session.getId(),session.getUserId(),List.of(ManualBacktestTrade.Status.PENDING,ManualBacktestTrade.Status.OPEN)); }
    private ManualBacktestTrade openTrade(ManualBacktestSession session) { return activeTradeEntity(session).filter(trade -> trade.getStatus()==ManualBacktestTrade.Status.OPEN).orElseThrow(() -> new IllegalStateException("No open position.")); }
    private Trade activeTrade(ManualBacktestSession session) { return activeTradeEntity(session).map(this::trade).orElse(null); }
    private Trade latestClosedTrade(ManualBacktestSession session) { return trades.findBySessionIdAndUserIdOrderByCreatedAtAsc(session.getId(),session.getUserId()).stream().filter(value -> value.getStatus()==ManualBacktestTrade.Status.CLOSED).reduce((first,second)->second).map(this::trade).orElse(null); }
    private void event(ManualBacktestTrade trade, ManualBacktestSession session, String type, BigDecimal oldValue, BigDecimal newValue) { ManualBacktestTradeEvent event=new ManualBacktestTradeEvent(); event.record(trade.getId(),session.getId(),session.getUserId(),session.getCursorTs(),type,oldValue,newValue); events.save(event); }
    private void valid(ManualBacktestTrade.Direction direction, BigDecimal entry, BigDecimal stop, BigDecimal target) { if(entry==null || stop==null || target==null || (direction==ManualBacktestTrade.Direction.LONG && (stop.compareTo(entry)>=0||target.compareTo(entry)<=0)) || (direction==ManualBacktestTrade.Direction.SHORT && (stop.compareTo(entry)<=0||target.compareTo(entry)>=0))) throw new IllegalArgumentException("Invalid " + direction.name().toLowerCase(Locale.ROOT) + " position: stop and target must bracket entry."); }
    private BigDecimal requiredEntry(BigDecimal entry) { if(entry==null) throw new IllegalArgumentException("Entry price is required for limit and stop orders."); return entry; }
    private void validateOrderSide(ManualBacktestTrade.Direction direction, ManualBacktestTrade.EntryOrderType type, BigDecimal entry, BigDecimal reference) {
        if (type==ManualBacktestTrade.EntryOrderType.MARKET) return;
        boolean wrongLimit=(type==ManualBacktestTrade.EntryOrderType.LIMIT)&&((direction==ManualBacktestTrade.Direction.LONG&&entry.compareTo(reference)>0)||(direction==ManualBacktestTrade.Direction.SHORT&&entry.compareTo(reference)<0));
        boolean wrongStop=(type==ManualBacktestTrade.EntryOrderType.STOP)&&((direction==ManualBacktestTrade.Direction.LONG&&entry.compareTo(reference)<=0)||(direction==ManualBacktestTrade.Direction.SHORT&&entry.compareTo(reference)>=0));
        if(wrongLimit) throw new IllegalArgumentException(direction==ManualBacktestTrade.Direction.LONG?"Use a Long Stop order for an entry above current price.":"Use a Short Stop order for an entry below current price.");
        if(wrongStop) throw new IllegalArgumentException(direction==ManualBacktestTrade.Direction.LONG?"Long Stop entry must be above current price.":"Short Stop entry must be below current price.");
    }
    private BigDecimal executable(BigDecimal price, MarketInstrument instrument) { if(price==null || price.remainder(instrument.tickSize()).stripTrailingZeros().signum()!=0) throw new IllegalArgumentException("Prices must align to the " + instrument.tickSize().stripTrailingZeros().toPlainString() + " tick."); return price.setScale(6, RoundingMode.UNNECESSARY); }
    private BigDecimal multiplier(ManualBacktestSession session) { if(session.getRiskProfile()==ManualBacktestSession.RiskProfile.UNIFORM_1R)return BigDecimal.ONE; return switch(session.getTradingDate().getDayOfWeek()){case MONDAY,FRIDAY->new BigDecimal("0.5"); default->BigDecimal.ONE;}; }
    private Instant contextStart(Instant cursor, String timeframe) { return switch(timeframe){case "1m"->cursor.minus(Duration.ofMinutes(45));case "5m"->cursor.minus(Duration.ofHours(3));case "1h"->tradingDayStart(cursor,5);case "4h"->tradingDayStart(cursor,15);default->cursor;}; }
    private Instant tradingDayStart(Instant cursor,int days){LocalDate day=cursor.atZone(NEW_YORK).toLocalDate();int found=0;while(found<days){day=day.minusDays(1);if(day.getDayOfWeek()!=DayOfWeek.SATURDAY&&day.getDayOfWeek()!=DayOfWeek.SUNDAY)found++;}return day.atStartOfDay(NEW_YORK).toInstant();}
    private BacktestBar bar(MarketBar value){return new BacktestBar(value.getTsEvent(),value.getInstrumentId(),value.getOpen(),value.getHigh(),value.getLow(),value.getClose(),value.getVolume());}
    private Candle candle(BacktestBar value,boolean complete){return new Candle(value.time(),value.open(),value.high(),value.low(),value.close(),value.volume(),complete);}
    private Session session(ManualBacktestSession value){return new Session(value.getId(),value.getSymbol().name(),value.getContinuousSymbol(),value.getTradingDate(),value.getTimezone(),value.getStartTime(),value.getEndTime(),value.getCursorTs(),value.getStatus().name(),value.getRiskProfile().name(),value.getNotes(),value.getCreatedAt(),value.getUpdatedAt(),value.getCompletedAt(),summary(value));}
    private Trade trade(ManualBacktestTrade value){return new Trade(value.getId(),value.getDirection().name(),value.getEntryOrderType().name(),value.getConfiguredEntryPrice(),value.getInstrumentId(),value.getOrderPlacedTs(),value.getEntryTs(),value.getEntryPrice(),value.getInitialStopPrice(),value.getCurrentStopPrice(),value.getTargetPrice(),value.getExitTs(),value.getExitPrice(),value.getExitReason()==null?null:value.getExitReason().name(),value.getInitialRiskPoints(),value.getRawR(),value.getRiskMultiplier(),value.getWeightedR(),value.getStatus().name(),value.getPendingAction(),value.getNotes());}
    private Summary summary(ManualBacktestSession session){List<ManualBacktestTrade> all=trades.findBySessionIdAndUserIdOrderByCreatedAtAsc(session.getId(),session.getUserId()), filled=all.stream().filter(t->t.getEntryPrice()!=null).toList(),closed=filled.stream().filter(t->t.getStatus()==ManualBacktestTrade.Status.CLOSED).toList();int wins=(int)closed.stream().filter(t->t.getRawR().signum()>0).count(), losses=(int)closed.stream().filter(t->t.getRawR().signum()<0).count(), be=closed.size()-wins-losses,pending=(int)all.stream().filter(t->t.getStatus()==ManualBacktestTrade.Status.PENDING).count();BigDecimal raw=sum(closed,false),weighted=sum(closed,true),positive=closed.stream().map(ManualBacktestTrade::getRawR).filter(r->r.signum()>0).reduce(BigDecimal.ZERO,BigDecimal::add),negative=closed.stream().map(ManualBacktestTrade::getRawR).filter(r->r.signum()<0).map(BigDecimal::abs).reduce(BigDecimal.ZERO,BigDecimal::add),cumulative=BigDecimal.ZERO,peak=BigDecimal.ZERO,drawdown=BigDecimal.ZERO;int streak=0,maxStreak=0;for(ManualBacktestTrade t:closed){cumulative=cumulative.add(t.getWeightedR());peak=peak.max(cumulative);drawdown=drawdown.max(peak.subtract(cumulative));if(t.getRawR().signum()<0)streak++;else streak=0;maxStreak=Math.max(maxStreak,streak);}return new Summary(filled.size(),pending,wins,losses,be,divide(BigDecimal.valueOf(wins),closed.size()),raw,weighted,divide(raw,closed.size()),divide(positive,wins),divide(negative.negate(),losses),negative.signum()==0?null:positive.divide(negative,6,RoundingMode.HALF_UP),drawdown,maxStreak);}
    private BigDecimal sum(List<ManualBacktestTrade> values,boolean weighted){return values.stream().map(weighted?ManualBacktestTrade::getWeightedR:ManualBacktestTrade::getRawR).reduce(BigDecimal.ZERO,BigDecimal::add);} private BigDecimal divide(BigDecimal value,int divisor){return divisor==0?BigDecimal.ZERO:value.divide(BigDecimal.valueOf(divisor),6,RoundingMode.HALF_UP);}
    private ManualBacktestSession owned(User user,Long id){return sessions.findByIdAndUserId(id,user.getId()).orElseThrow(()->new ResourceNotFoundException("Manual backtest session not found"));} private ManualBacktestSession locked(User user,Long id){return sessions.lockByIdAndUserId(id,user.getId()).orElseThrow(()->new ResourceNotFoundException("Manual backtest session not found"));} private void require(User user){entitlements.requireEntitlement(user,FeatureEntitlementKey.MANUAL_BACKTEST_BETA);} private void active(ManualBacktestSession session){if(session.getStatus()!=ManualBacktestSession.Status.ACTIVE)throw new IllegalStateException("Manual backtest session is completed and read-only.");} private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
