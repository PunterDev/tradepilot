package com.tradepilot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reflections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiReflection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "prompt_text", columnDefinition = "text")
    private String promptText;
    @Column(name = "response_text", nullable = false, columnDefinition = "text")
    private String responseText;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist public void onCreate(){ createdAt = LocalDateTime.now(); }
}
