export type SampleTimelineTrade = {
  label: string;
  time: string;
  result: string;
  triggered: boolean;
};

export const publicSampleAccount = {
  account: {
    label: "Fictional futures sample account",
    reviewWindow: "Illustrative NQ review",
    dateRange: "May 6-17, 2026",
    instruments: "NQ and MNQ",
    activeSession: "9:30-11:00 ET",
    winRate: "43.8%",
    disclosure: "Static sample data only. It is not customer performance, a trade signal, financial advice, or a promise of future results.",
  },
  dataTrust: {
    completedTrades: 64,
    netPnl: "-$483.75",
    importSources: "Illustrative Tradovate or Topstep export",
    confirmationPrompt: "Does this match your trading platform?",
  },
  primaryFinding: {
    type: "POST_LOSS_OVERTRADING",
    title: "Post-loss overtrading",
    trigger: "Two consecutive losses",
    occurrenceCount: 14,
    evidenceStrength: "MODERATE",
    baseline: { label: "Other comparable trades", result: "+0.18R", sampleSize: 50 },
    triggered: { label: "Trades after two losses", result: "-0.42R", sampleSize: 14 },
    rUnitDisclosure: "R values in this fictional sample use a predefined $100 risk unit. Broker CSV imports do not by themselves establish intended per-trade risk.",
    counterfactual: {
      lossesAvoided: "$612.50",
      winnersForgone: "$184.25",
      netHistoricalImpact: "$428.25",
    },
    timeline: [
      { label: "Trade 21", time: "9:42 ET", result: "-0.70R", triggered: false },
      { label: "Trade 22", time: "9:54 ET", result: "-0.85R", triggered: false },
      { label: "Trade 23", time: "10:03 ET", result: "-0.60R", triggered: true },
      { label: "Trade 24", time: "10:11 ET", result: "-0.45R", triggered: true },
      { label: "Trade 25", time: "10:19 ET", result: "+0.20R", triggered: true },
    ] as readonly SampleTimelineTrade[],
  },
  supportingFinding: {
    type: "SIZE_ESCALATION_AFTER_LOSS",
    title: "Size escalation after loss",
    evidenceStrength: "WEAK",
    description: "A separate sample finding highlights larger size after a loss, so the public examples do not imply every account has the same leak.",
    nextRule: "Keep size unchanged after a losing trade until the next planned setup.",
  },
  secondaryFinding: {
    type: "OVERTRADING",
    title: "Overtrading",
    description: "A separate sample surface shows how a normal session trade limit can become an evidence-backed review point.",
    nextRule: "Stop adding trades once you reach your normal session trade limit.",
  },
  coach: {
    rule: "After two consecutive losses, pause trading for 20 minutes.",
    focus: "Reset the plan, identify the next valid setup, and decide whether another trade still meets your rules.",
  },
  ctas: {
    registrationPath: "/register?source=sample-demo",
    registrationLabel: "Try TradePilot Free",
  },
} as const;

export const sampleEvidenceMethod = "TradePilot detects the pattern from your trading data first. AI helps explain it—it does not invent the finding.";
