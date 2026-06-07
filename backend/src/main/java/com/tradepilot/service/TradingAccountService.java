package com.tradepilot.service;

import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingAccountService {

    private final TradingAccountRepository tradingAccountRepository;
    private final ImportBatchRepository importBatchRepository;
    private final UsageLimitService usageLimitService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;


    private final TradeRepository tradeRepository;

    public TradingAccountResponse create(User user, TradingAccountRequest request) {
        requireUser(user);
        usageLimitService.enforceAccountCreation(user);

        TradingAccount account = TradingAccount.builder()
                .user(user)
                .name(request.name())
                .propFirm(request.propFirm())
                .accountNumber(request.accountNumber())
                .accountType(request.accountType())
                .startingBalance(request.startingBalance())
                .status(request.status() != null ? request.status() : TradingAccountStatus.ACTIVE)
                .build();

        TradingAccount saved = tradingAccountRepository.save(account);
        cacheInvalidationService.invalidateBillingUsage(user.getId());
        log.info("Trading account created. userId={}, accountId={}, type={}",
                user.getId(), saved.getId(), saved.getAccountType());
        return mapToResponse(saved);
    }

    public List<TradingAccountResponse> getAccounts(User user) {
        requireUser(user);

        List<TradingAccount> accounts = tradingAccountRepository.findByUserOrderByCreatedAtDesc(user);
        log.trace("Trading accounts loaded. userId={}, count={}", user.getId(), accounts.size());

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TradingAccount getAccountForImport(User user, Long accountId) {
        requireUser(user);

        return tradingAccountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Trading account not found"));
    }

    public TradingAccountResponse mapToResponse(TradingAccount account) {
        return TradingAccountResponse.of(
                account.getId(),
                account.getName(),
                account.getPropFirm(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStartingBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    @Transactional
    public void deleteAccount(User user, Long accountId) {
        requireUser(user);

        TradingAccount account = tradingAccountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        importBatchRepository.deleteByAccountIdAndUser(accountId, user);
        tradeRepository.deleteByAccountIdAndUser(accountId, user);
        tradingAccountRepository.delete(account);
        cacheInvalidationService.invalidateUserTradeDerivedCaches(user.getId());
        log.info("Trading account deleted. userId={}, accountId={}", user.getId(), accountId);
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
    }
}
