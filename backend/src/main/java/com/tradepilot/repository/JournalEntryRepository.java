package com.tradepilot.repository;

import com.tradepilot.entity.JournalEntry;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByUserAndAccountAndJournalDate(
            User user,
            TradingAccount account,
            LocalDate journalDate
    );

    Optional<JournalEntry> findByUserAndAccountIsNullAndJournalDate(
            User user,
            LocalDate journalDate
    );

    List<JournalEntry> findByUserOrderByJournalDateDesc(User user);
}