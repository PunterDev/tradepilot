package com.tradepilot.performancetimeline.dto;

public class PerformanceCategoryScores {
    private Integer discipline;
    private Integer riskManagement;
    private Integer execution;
    private Integer consistency;
    private Integer psychology;

    public PerformanceCategoryScores() {
    }

    public PerformanceCategoryScores(Integer discipline, Integer riskManagement, Integer execution, Integer consistency, Integer psychology) {
        this.discipline = discipline;
        this.riskManagement = riskManagement;
        this.execution = execution;
        this.consistency = consistency;
        this.psychology = psychology;
    }

    public Integer getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Integer discipline) {
        this.discipline = discipline;
    }

    public Integer getRiskManagement() {
        return riskManagement;
    }

    public void setRiskManagement(Integer riskManagement) {
        this.riskManagement = riskManagement;
    }

    public Integer getExecution() {
        return execution;
    }

    public void setExecution(Integer execution) {
        this.execution = execution;
    }

    public Integer getConsistency() {
        return consistency;
    }

    public void setConsistency(Integer consistency) {
        this.consistency = consistency;
    }

    public Integer getPsychology() {
        return psychology;
    }

    public void setPsychology(Integer psychology) {
        this.psychology = psychology;
    }
}
