package com.tradepilot.playbook.entity;

import com.tradepilot.playbook.enums.ChecklistType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playbook_checklist_item", indexes = {
        @Index(name = "idx_playbook_checklist_setup_id", columnList = "setup_id"),
        @Index(name = "idx_playbook_checklist_type", columnList = "checklist_type")
})
public class PlaybookChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setup_id", nullable = false)
    private PlaybookSetup setup;
    @Enumerated(EnumType.STRING)
    @Column(name = "checklist_type", nullable = false, length = 32)
    private ChecklistType checklistType;
    @Column(nullable = false, length = 180)
    private String label;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "is_required", nullable = false)
    private Boolean required;
    @Column(nullable = false)
    private Integer weight;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(nullable = false)
    private Boolean active;
    @Column(name = "expected_value_type", length = 32)
    private String expectedValueType;
    @Column(name = "expected_value_json", columnDefinition = "text")
    private String expectedValueJson;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (required == null) required = true;
        if (weight == null || weight <= 0) weight = 1;
        if (displayOrder == null) displayOrder = 0;
        if (active == null) active = true;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlaybookSetup getSetup() { return setup; }
    public void setSetup(PlaybookSetup setup) { this.setup = setup; }
    public ChecklistType getChecklistType() { return checklistType; }
    public void setChecklistType(ChecklistType checklistType) { this.checklistType = checklistType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getExpectedValueType() { return expectedValueType; }
    public void setExpectedValueType(String expectedValueType) { this.expectedValueType = expectedValueType; }
    public String getExpectedValueJson() { return expectedValueJson; }
    public void setExpectedValueJson(String expectedValueJson) { this.expectedValueJson = expectedValueJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
