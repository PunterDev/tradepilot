package com.tradepilot.datatrust;

import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_reconciliations")
public class ImportReconciliation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private TradingAccount account;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "import_batch_id", nullable = false, unique = true) private ImportBatch importBatch;
    @Column(length = 30) private String source;
    @Column(name = "trade_count", nullable = false) private int tradeCount;
    @Column(name = "net_pnl", precision = 18, scale = 2) private BigDecimal netPnl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ImportReconciliationStatus status = ImportReconciliationStatus.NOT_REVIEWED;
    @Column(name = "mismatch_reasons", length = 500) private String mismatchReasons;
    @Column(columnDefinition = "text") private String comment;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public TradingAccount getAccount(){return account;} public void setAccount(TradingAccount v){account=v;} public ImportBatch getImportBatch(){return importBatch;} public void setImportBatch(ImportBatch v){importBatch=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public int getTradeCount(){return tradeCount;} public void setTradeCount(int v){tradeCount=v;} public BigDecimal getNetPnl(){return netPnl;} public void setNetPnl(BigDecimal v){netPnl=v;} public ImportReconciliationStatus getStatus(){return status;} public void setStatus(ImportReconciliationStatus v){status=v;} public String getMismatchReasons(){return mismatchReasons;} public void setMismatchReasons(String v){mismatchReasons=v;} public String getComment(){return comment;} public void setComment(String v){comment=v;} public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
