package com.tradepilot.repository;

import com.tradepilot.entity.ContactMessage;
import com.tradepilot.enums.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessageStatus status);

    List<ContactMessage> findByEmailSentFalseOrderByCreatedAtDesc();

    Page<ContactMessage> findAllBy(Pageable pageable);

    Page<ContactMessage> findByStatus(ContactMessageStatus status, Pageable pageable);

    Page<ContactMessage> findByEmailSentFalse(Pageable pageable);

    List<ContactMessage> findTop6ByOrderByCreatedAtDesc();

    List<ContactMessage> findTop5ByOrderByCreatedAtDesc();

    List<ContactMessage> findTop10ByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    long countByStatus(ContactMessageStatus status);

    long countByEmailSentFalse();

    @Query("""
            select count(c),
                   sum(case when c.status = com.tradepilot.enums.ContactMessageStatus.NEW then 1 else 0 end),
                   sum(case when c.status = com.tradepilot.enums.ContactMessageStatus.OPEN then 1 else 0 end),
                   sum(case when c.status = com.tradepilot.enums.ContactMessageStatus.REPLIED then 1 else 0 end),
                   sum(case when c.emailSent = false then 1 else 0 end)
            from ContactMessage c
            """)
    Object adminContactCounts();
}
