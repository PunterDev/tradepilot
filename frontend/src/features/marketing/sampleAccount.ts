export const sampleAccount = {
  totalTrades: 42,
  postLossTrades: 14,
  qualifyingOccurrences: 9,
  finding: {
    title: "Revenge Trading After Losses",
    pattern: "Post-loss overtrading",
    evidenceStrength: "WEAK",
    evidenceLabel: "Weak evidence",
    baselineAverageR: -0.11,
    triggeredAverageR: -0.62,
    deteriorationR: 0.51,
    coachRule: "Pause 20 minutes after two consecutive losses.",
  },
} as const;
