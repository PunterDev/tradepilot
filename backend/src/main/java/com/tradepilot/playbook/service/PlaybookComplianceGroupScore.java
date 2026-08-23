package com.tradepilot.playbook.service;

final class PlaybookComplianceGroupScore {
    private final int availableWeight;
    private final int metWeight;
    private final int notMetCount;
    private final int unknownCount;

    PlaybookComplianceGroupScore(int availableWeight, int metWeight, int notMetCount, int unknownCount) {
        this.availableWeight = availableWeight;
        this.metWeight = metWeight;
        this.notMetCount = notMetCount;
        this.unknownCount = unknownCount;
    }

    int availableWeight() { return availableWeight; }
    int metWeight() { return metWeight; }
    int notMetCount() { return notMetCount; }
    int unknownCount() { return unknownCount; }
}
