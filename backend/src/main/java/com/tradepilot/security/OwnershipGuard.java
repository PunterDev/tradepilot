package com.tradepilot.security;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.observability.RequestCorrelationFilter;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnershipGuard {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OwnershipGuard.class);
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public TradingAccount requireAccountOwner(User user, Long accountId) {
        Long userId = requireCurrentUserId(user);
        return tradingAccountRepository.findByIdAndUser_Id(accountId, userId).orElseThrow(() -> {
            logDenied(userId, "trading_account", accountId);
            return new ResourceNotFoundException("Trading account not found");
        });
    }

    @Transactional(readOnly = true)
    public Trade requireTradeOwner(User user, Long tradeId) {
        Long userId = requireCurrentUserId(user);
        return tradeRepository.findByIdAndUser_Id(tradeId, userId).orElseThrow(() -> {
            logDenied(userId, "trade", tradeId);
            return new ResourceNotFoundException("Trade not found");
        });
    }

    @Transactional(readOnly = true)
    public Trade requireTradeOwnerForAccount(User user, Long tradeId, Long accountId) {
        Trade trade = requireTradeOwner(user, tradeId);
        Long actualAccountId = trade.getAccount() == null ? null : trade.getAccount().getId();
        if (accountId != null && !accountId.equals(actualAccountId)) {
            logDenied(user.getId(), "trade_account_scope", tradeId);
            throw new ResourceNotFoundException("Trade not found");
        }
        return trade;
    }

    public Long requireCurrentUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user.getId();
    }

    private void logDenied(Long userId, String resourceType, Long resourceId) {
        log.warn("Ownership access denied. currentUserId={}, resourceType={}, resourceId={}, requestId={}",
                userId,
                resourceType,
                resourceId,
                MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @java.lang.SuppressWarnings("all")
    public OwnershipGuard(final TradingAccountRepository tradingAccountRepository, final TradeRepository tradeRepository) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.tradeRepository = tradeRepository;
    }
}
