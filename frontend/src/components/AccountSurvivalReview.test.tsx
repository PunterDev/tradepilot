import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AccountSurvivalReview from "./AccountSurvivalReview";
import type { CommitmentEvaluation, CommitmentEvaluationHistory, CommitmentRuleResult, ProtectionCommitment, RiskProfile, SurvivalAssessment } from "../api/accountSurvivalApi";
import * as survivalApi from "../api/accountSurvivalApi";

vi.mock("../api/accountSurvivalApi", async importOriginal => {
  const actual = await importOriginal<typeof import("../api/accountSurvivalApi")>();
  return {
    ...actual,
    getSurvivalAssessment: vi.fn(),
    getRiskProfile: vi.fn(),
    getProtectionCommitment: vi.fn(),
    getCommitmentEvaluation: vi.fn(),
    getCommitmentEvaluationHistory: vi.fn(),
    saveRiskProfile: vi.fn(),
    saveProtectionCommitment: vi.fn(),
    acceptProtectionCommitment: vi.fn(),
    evaluateProtectionCommitment: vi.fn(),
  };
});

const assessment: SurvivalAssessment = {
  accountId: 7,
  sessionDate: "2026-04-07",
  state: "STABLE",
  currentBalance: "50000.00",
  failureThreshold: "48000.00",
  remainingBuffer: "2000.50",
  plannedRiskPerTrade: "150.25",
  typicalLoss: "100.00",
  typicalLossSampleSize: 5,
  plannedRiskBufferPercent: "7.51",
  typicalLossBufferPercent: "5.00",
  normalLossCapacity: "20.0",
  firmMaxContracts: 5,
  personalMaxContracts: 3,
  sustainableMaxContracts: 2,
  primaryThreat: "Configured risk remains within the observed buffer.",
  evidenceStrength: "MODERATE",
  missingDataReasons: [],
  turningPoint: { detected: false, observableFacts: [], evidenceStrength: "MODERATE", limitations: [] },
  calculatedAt: "2026-04-07T16:00:00",
  calculationVersion: "account-survival-v1",
};

const commitment: ProtectionCommitment = {
  id: 11,
  accountId: 7,
  effectiveSessionDate: "2026-04-06",
  maxNormalizedExposure: "20.00",
  maxAttempts: 3,
  maxSessionLoss: "200.00",
  postLossPauseMinutes: 20,
  allowedWindowStart: "09:30",
  allowedWindowEnd: "16:00",
  requireFreshSetupConfirmation: true,
  source: "SYSTEM_RECOMMENDED",
  status: "ACCEPTED",
  acceptedAt: "2026-04-06T16:05:00",
};

const profile: RiskProfile = { accountId: 7, currentBalance: "50000.00", failureThreshold: "48000.00", plannedRiskPerTrade: "150.25" };

const rules: CommitmentRuleResult[] = [
  { rule: "MAX_ATTEMPTS", status: "FOLLOWED", configuredValue: "3", observedValue: "2", message: "Two completed attempts were observed." },
  { rule: "MAX_NORMALIZED_EXPOSURE", status: "FOLLOWED", configuredValue: "20", observedValue: "10", message: "Maximum normalized exposure remained within the commitment." },
  { rule: "MAX_SESSION_LOSS", status: "FOLLOWED", configuredValue: "200.00", observedValue: "-24.50", message: "Realized session loss remained within the configured maximum." },
  { rule: "MIN_POST_LOSS_PAUSE", status: "FOLLOWED", configuredValue: "20", observedValue: "26", message: "The shortest post-loss pause was 26 minutes." },
  { rule: "TRADING_WINDOW", status: "FOLLOWED", configuredValue: "09:30–16:00", observedValue: "09:30", message: "All completed attempts opened inside the configured window." },
  { rule: "FRESH_CONFIRMATION", status: "NOT_EVALUABLE", configuredValue: "Enabled", observedValue: null, message: "Completed-trade data does not prove contemporaneous confirmation." },
];

const completedEvaluation: CommitmentEvaluation = {
  available: true,
  evaluatedSessionDate: "2026-04-07",
  overallStatus: "COMPLETED",
  evaluationVersion: "account-survival-v1",
  rules,
};
const evaluationHistory: CommitmentEvaluationHistory = { evaluations: [completedEvaluation], evaluatedSessions: 1, followedSessions: 1, brokenSessions: 0, partiallyObservedSessions: 0, notEvaluableSessions: 0 };

function mockLoad(evaluation: CommitmentEvaluation | Error = completedEvaluation) {
  vi.mocked(survivalApi.getSurvivalAssessment).mockResolvedValue(assessment);
  vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
  vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
  if (evaluation instanceof Error) {
    vi.mocked(survivalApi.getCommitmentEvaluation).mockRejectedValue(evaluation);
  } else {
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(evaluation);
  }
  vi.mocked(survivalApi.getCommitmentEvaluationHistory).mockResolvedValue(evaluationHistory);
}

async function renderLoaded(evaluation: CommitmentEvaluation | Error = completedEvaluation) {
  mockLoad(evaluation);
  render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
  await screen.findByRole("heading", { name: "Stable" });
}

beforeEach(() => vi.clearAllMocks());

describe("AccountSurvivalReview commitment evaluation", () => {
  it("announces loading without showing stale evaluation results", () => {
    let resolveAssessment: (value: SurvivalAssessment) => void = () => undefined;
    vi.mocked(survivalApi.getSurvivalAssessment).mockReturnValue(new Promise(resolve => { resolveAssessment = resolve; }));
    vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
    vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(completedEvaluation);
    vi.mocked(survivalApi.getCommitmentEvaluationHistory).mockResolvedValue(evaluationHistory);

    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading Account Survival review...");
    expect(screen.queryByRole("region", { name: "Commitment evaluation" })).not.toBeInTheDocument();
    resolveAssessment(assessment);
  });

  it("renders a pending evaluation without fabricated rule results", async () => {
    await renderLoaded({ available: false, overallStatus: "ACCEPTED", rules: [], limitation: "No later eligible imported session exists yet." });

    expect(screen.getByText("No later eligible imported session exists yet.")).toBeVisible();
    expect(screen.queryByText(/Max Attempts/)).not.toBeInTheDocument();
    expect(screen.queryByText("Completed")).not.toBeInTheDocument();
  });

  it("renders completed status, typed rule facts, and reusable production formatting", async () => {
    await renderLoaded();

    expect(screen.getAllByText("Completed").length).toBeGreaterThan(0);
    expect(screen.getByText("Trading date: Apr 7, 2026 · account-survival-v1")).toBeVisible();
    expect(screen.getByText("Max Attempts — Followed")).toBeVisible();
    expect(screen.getByText("Max Normalized Exposure — Followed")).toBeVisible();
    expect(screen.getByText("Min Post Loss Pause — Followed")).toBeVisible();
    expect(screen.getByText("Configured: $200.00 · Observed: -$24.50")).toBeVisible();
    expect(screen.getByText("Configured: 20 contracts · Observed: 10 contracts")).toBeVisible();
    expect(screen.getByText("Configured: 20 min · Observed: 26 min")).toBeVisible();
    expect(screen.getByText("Configured: Enabled · Observed: —")).toBeVisible();
    expect(screen.getByText("Remaining buffer").parentElement).toHaveTextContent("$2,000.50");
    expect(screen.getByRole("region", { name: "Commitment evaluation history" })).toHaveTextContent("Adherence history");
  });

  it("renders broken rules as measurable violations while preserving followed rules", async () => {
    await renderLoaded({
      ...completedEvaluation,
      overallStatus: "BROKEN",
      rules: [
        { ...rules[0], status: "FOLLOWED" },
        { ...rules[2], status: "BROKEN", observedValue: "-250.75", message: "Realized session loss exceeded the configured maximum by $50.75." },
        { ...rules[3], rule: "POST_LOSS_PAUSE", status: "BROKEN", observedValue: "4", message: "The next completed attempt opened four minutes after a realized loss." },
      ],
    });

    expect(screen.getAllByText("Broken").length).toBeGreaterThan(0);
    expect(screen.getByText("Max Attempts — Followed")).toBeVisible();
    expect(screen.getByText("Max Session Loss — Broken")).toBeVisible();
    expect(screen.getByText("Configured: $200.00 · Observed: -$250.75")).toBeVisible();
    expect(screen.queryByText(/revenge trading/i)).not.toBeInTheDocument();
  });

  it("keeps partially observed and not evaluable evidence distinct from completion or failure", async () => {
    await renderLoaded({ ...completedEvaluation, overallStatus: "PARTIALLY_OBSERVED", rules: [rules[0], rules[5]] });

    expect(screen.getAllByText("Partially Observed").length).toBeGreaterThan(0);
    expect(screen.getByText("Max Attempts — Followed")).toBeVisible();
    expect(screen.getByText("Fresh Confirmation — Not Evaluable")).toBeVisible();
    expect(screen.queryByText(/— Broken$/)).not.toBeInTheDocument();

  });

  it("renders a neutral not-evaluable evaluation with its evidence limitation", async () => {
    await renderLoaded({ ...completedEvaluation, overallStatus: "NOT_EVALUABLE", rules: [rules[5]] });

    expect(screen.getAllByText("Not Evaluable").length).toBeGreaterThan(0);
    const evidenceButton = screen.getByRole("button", { name: "Show evidence" });
    await userEvent.setup().click(evidenceButton);
    expect(screen.getByText("Completed-trade data does not prove contemporaneous confirmation.")).toBeVisible();
    expect(screen.queryByText(/— Broken$/)).not.toBeInTheDocument();
  });

  it("keeps rendered profile and commitment data usable when evaluation loading fails", async () => {
    await renderLoaded(new Error("evaluation unavailable"));

    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load the commitment evaluation.");
    expect(screen.getByText("Remaining buffer")).toBeVisible();
    expect(screen.queryByRole("button", { name: "Edit draft commitment" })).not.toBeInTheDocument();
    expect(screen.getByText(/Accepted protection rules are locked/)).toBeVisible();
    expect(screen.getByText("No evaluation has been recorded yet.")).toBeVisible();
  });

  it("expands and collapses rule evidence with mouse and keyboard", async () => {
    const user = userEvent.setup();
    await renderLoaded();
    const evaluationRegion = screen.getByRole("region", { name: "Commitment evaluation" });
    const evidenceButtons = within(evaluationRegion).getAllByRole("button", { name: "Show evidence" });
    const firstRuleButton = evidenceButtons[0];

    expect(firstRuleButton).toHaveAttribute("aria-expanded", "false");
    await user.click(firstRuleButton);
    expect(firstRuleButton).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("Two completed attempts were observed.")).toBeVisible();

    firstRuleButton.focus();
    await user.keyboard("{Enter}");
    expect(firstRuleButton).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByText("Two completed attempts were observed.")).not.toBeInTheDocument();
  });
});

describe("AccountSurvivalReview cross-account turning point", () => {
  it("renders compact, evidence-safe cross-account continuation facts", async () => {
    const crossAccountAssessment: SurvivalAssessment = {
      ...assessment,
      turningPoint: {
        detected: true,
        type: "CROSS_ACCOUNT_CONTINUATION",
        timestamp: "2026-04-07T10:12:05",
        observableFacts: ["Cross-account continuation detected."],
        previousNormalizedExposure: "20",
        newNormalizedExposure: "40",
        pnlBefore: "-240",
        finalSessionPnl: "-780",
        stopBeforeActionPnl: "-240",
        lossesAvoided: "540",
        winnersForgone: "90",
        netImpact: "450",
        evidenceStrength: "MODERATE",
        limitations: ["Observed sequence only; intent is not inferred."],
        crossAccountEvidence: {
          sourceAccounts: ["Topstep 50K #1"], laterAccounts: ["Apex 50K #2", "Apex 50K #3"],
          sourceSymbols: ["NQ"], laterSymbols: ["MNQ"], instrumentFamily: "NASDAQ", direction: "SHORT",
          elapsedSeconds: 480, previousRealizedLoss: "-240", previousNormalizedExposure: "20", newNormalizedExposure: "40",
          exposureDifference: "20", exposureMultiplier: "2", timestampPrecision: "SECOND", confidence: "MODERATE",
          limitations: ["Near-synchronous account activity is grouped as one synchronized action; no copier relationship is inferred."], engineVersion: "account-survival-v1",
        },
      },
    };
    vi.mocked(survivalApi.getSurvivalAssessment).mockResolvedValue(crossAccountAssessment);
    vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
    vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(completedEvaluation);
    vi.mocked(survivalApi.getCommitmentEvaluationHistory).mockResolvedValue(evaluationHistory);
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await screen.findByRole("heading", { name: "Cross-account continuation" });

    const evidenceButton = screen.getByRole("button", { name: "Cross-account evidence" });
    expect(evidenceButton).toHaveAttribute("aria-expanded", "false");
    await userEvent.setup().click(evidenceButton);
    expect(evidenceButton).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("Topstep 50K #1")).toBeVisible();
    expect(screen.getByText("Apex 50K #2, Apex 50K #3 (2 accounts)")).toBeVisible();
    expect(screen.getByText("8 min 0s")).toBeVisible();
    expect(screen.getByText("NASDAQ SHORT")).toBeVisible();
    expect(screen.getByText("20.00 units")).toBeVisible();
    expect(screen.getByText("40.00 units")).toBeVisible();
    expect(screen.getByText(/Data limitations:/)).toHaveTextContent("no copier relationship is inferred");
    expect(screen.getByText(/Winners forgone:/)).toHaveTextContent("$90.00");
    expect(screen.queryByText(/revenge|emotional account switching/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/tradeId|accountId|\b123\b/i)).not.toBeInTheDocument();
  });

  it("keeps a generic turning point rendering when cross-account evidence is absent", async () => {
    const genericAssessment: SurvivalAssessment = {
      ...assessment,
      turningPoint: { detected: true, type: "RAPID_SAME_FAMILY_REENTRY", timestamp: "2026-04-07T10:12:05", observableFacts: ["A realized loss was followed by a same-family entry."], evidenceStrength: "MODERATE", limitations: [] },
    };
    vi.mocked(survivalApi.getSurvivalAssessment).mockResolvedValue(genericAssessment);
    vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
    vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(completedEvaluation);
    vi.mocked(survivalApi.getCommitmentEvaluationHistory).mockResolvedValue(evaluationHistory);
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await screen.findByText("A realized loss was followed by a same-family entry.");
    expect(screen.queryByText("Cross-account evidence")).not.toBeInTheDocument();
  });
});
