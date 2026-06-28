package com.tradepilot.service;

import com.tradepilot.dto.journal.JournalRequest;
import com.tradepilot.dto.journal.JournalResponse;
import com.tradepilot.entity.JournalEntry;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JournalService {
    private final JournalEntryRepository journalRepository;
    private final TradingAccountService tradingAccountService;

    public JournalResponse save(User user, JournalRequest request) {
        final TradingAccount account = request.accountId() != null ? tradingAccountService.getAccountForImport(user, request.accountId()) : null;
        Optional<JournalEntry> existing = account == null ? journalRepository.findByUserAndAccountIsNullAndJournalDate(user, request.journalDate()) : journalRepository.findByUserAndAccountAndJournalDate(user, account, request.journalDate());
        JournalEntry entry = existing.orElseGet(() -> JournalEntry.builder().user(user).account(account).journalDate(request.journalDate()).build());
        entry.setMood(request.mood());
        entry.setSetupQuality(request.setupQuality());
        entry.setMistakes(request.mistakes());
        entry.setLessonLearned(request.lessonLearned());
        entry.setNotes(request.notes());
        return map(journalRepository.save(entry));
    }

    public List<JournalResponse> getAll(User user) {
        return journalRepository.findByUserOrderByJournalDateDesc(user).stream().map(this::map).toList();
    }

    public JournalResponse getByDate(User user, Long accountId, LocalDate date) {
        TradingAccount account = null;
        if (accountId != null) {
            account = tradingAccountService.getAccountForImport(user, accountId);
        }
        return (account == null ? journalRepository.findByUserAndAccountIsNullAndJournalDate(user, date) : journalRepository.findByUserAndAccountAndJournalDate(user, account, date)).map(this::map).orElse(null);
    }

    private JournalResponse map(JournalEntry entry) {
        return JournalResponse.builder().id(entry.getId()).accountId(entry.getAccount() == null ? null : entry.getAccount().getId()).journalDate(entry.getJournalDate()).mood(entry.getMood()).setupQuality(entry.getSetupQuality()).mistakes(entry.getMistakes()).lessonLearned(entry.getLessonLearned()).notes(entry.getNotes()).createdAt(entry.getCreatedAt()).updatedAt(entry.getUpdatedAt()).build();
    }

    @java.lang.SuppressWarnings("all")
    public JournalService(final JournalEntryRepository journalRepository, final TradingAccountService tradingAccountService) {
        this.journalRepository = journalRepository;
        this.tradingAccountService = tradingAccountService;
    }
}
