package com.tradepilot.service;

import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingAccountService {

    private final TradingAccountRepository tradingAccountRepository;
    private final ImportBatchRepository importBatchRepository;


    private final TradeRepository tradeRepository;

    public TradingAccountResponse create(User user, TradingAccountRequest request) {
        TradingAccount account = TradingAccount.builder()
                .user(user)
                .name(request.name())
                .propFirm(request.propFirm())
                .accountNumber(request.accountNumber())
                .startingBalance(request.startingBalance())
                .status(request.status() != null ? request.status() : TradingAccountStatus.ACTIVE)
                .build();

        return mapToResponse(tradingAccountRepository.save(account));
    }

    public List<TradingAccountResponse> getAccounts(User user) {
        List<TradingAccount> accounts = user == null
                ? tradingAccountRepository.findByUserIsNullOrderByCreatedAtDesc()
                : tradingAccountRepository.findByUserOrderByCreatedAtDesc(user);

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TradingAccount getAccountForImport(User user, Long accountId) {
        if (user == null) {
            return tradingAccountRepository.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Trading account not found"));
        }

        return tradingAccountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new RuntimeException("Trading account not found"));
    }

    private TradingAccountResponse mapToResponse(TradingAccount account) {
        return TradingAccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .propFirm(account.getPropFirm())
                .accountNumber(account.getAccountNumber())
                .startingBalance(account.getStartingBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteAccount(User user, Long accountId) {
        if (user == null) {
            throw new RuntimeException("User is required");
        }

        TradingAccount account = tradingAccountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 🔥 DELETE IN CORRECT ORDER
        importBatchRepository.deleteByAccountId(accountId);
        tradeRepository.deleteByAccountId(accountId);
        tradingAccountRepository.delete(account);
    }
}