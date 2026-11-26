import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { startTransition, StrictMode, Suspense, useState } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AccountSurvivalReview from "./AccountSurvivalReview";
import type { AccountSurvivalAccess, CommitmentEvaluation, CommitmentRuleResult, ProtectionCommitment, RiskProfile, SurvivalAssessment } from "../api/accountSurvivalApi";
import * as survivalApi from "../api/accountSurvivalApi";

vi.mock("../api/accountSurvivalApi", async importOriginal => {
  const actual = await importOriginal<typeof import("../api/accountSurvivalApi")>();
  return {
    ...actual,
    getAccountSurvivalAccess: vi.fn(),
    getSurvivalAssessment: vi.fn(),
    getRiskProfile: vi.fn(),
    getProtectionCommitment: vi.fn(),
    getCommitmentEvaluation: vi.fn(),
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

const proAccess: AccountSurvivalAccess = { rolloutMode: "GENERAL_AVAILABILITY", rolloutAvailable: true, premiumAccess: true, readOnly: false, upgradeRequired: false, message: "Account Survival premium actions are available." };
const freeAccess: AccountSurvivalAccess = { rolloutMode: "GENERAL_AVAILABILITY", rolloutAvailable: true, premiumAccess: false, readOnly: true, upgradeRequired: true, message: "Upgrade to Pro to create commitments and evaluations. Existing records remain read-only." };

function mockLoad(evaluation: CommitmentEvaluation | Error = completedEvaluation) {
  vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValue(proAccess);
  vi.mocked(survivalApi.getSurvivalAssessment).mockResolvedValue(assessment);
  vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
  vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
  if (evaluation instanceof Error) {
    vi.mocked(survivalApi.getCommitmentEvaluation).mockRejectedValue(evaluation);
  } else {
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(evaluation);
  }
}

async function renderLoaded(evaluation: CommitmentEvaluation | Error = completedEvaluation) {
  mockLoad(evaluation);
  render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
  await screen.findByRole("heading", { name: "Stable" });
}

beforeEach(() => vi.clearAllMocks());

function deferred<T>() { let resolve!: (value: T) => void; let reject!: (reason?: unknown) => void; const promise = new Promise<T>((next, fail) => { resolve = next; reject = fail; }); return { promise, resolve, reject }; }
function assessmentFor(accountId: number, date = "2026-04-06"): SurvivalAssessment { return { ...assessment, accountId, sessionDate: date, primaryThreat: `Account ${accountId} assessment` }; }
function commitmentFor(accountId: number, status = "ACCEPTED"): ProtectionCommitment { return { ...commitment, id: accountId * 10 + 1, accountId, maxAttempts: accountId, status, acceptedAt: status === "ACCEPTED" ? commitment.acceptedAt : null }; }
function mockAccountAwareLoads(status = "ACCEPTED") {
  vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValue(proAccess);
  vi.mocked(survivalApi.getSurvivalAssessment).mockImplementation(async (accountId, date) => assessmentFor(accountId, date));
  vi.mocked(survivalApi.getRiskProfile).mockImplementation(async accountId => ({ ...profile, accountId }));
  vi.mocked(survivalApi.getProtectionCommitment).mockImplementation(async accountId => commitmentFor(accountId, status));
  vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue({ available: false, overallStatus: "ACCEPTED", rules: [], limitation: "Pending." });
}

type ReviewSelection = { accountId: number; date: string };
function concurrentReviewHarness(blockedSelection: ReviewSelection) {
  const suspendedRenderReached = deferred<void>();
  const releaseSuspendedRender = deferred<void>();
  let blocked = true;
  let select!: (selection: ReviewSelection) => void;
  function SuspendAfterReview({ selection }: { selection: ReviewSelection }) {
    if (blocked && selection.accountId === blockedSelection.accountId && selection.date === blockedSelection.date) {
      suspendedRenderReached.resolve();
      throw releaseSuspendedRender.promise;
    }
    return null;
  }
  function Harness() {
    const [selection, setSelection] = useState<ReviewSelection>({ accountId: 7, date: "2026-04-06" });
    select = next => startTransition(() => setSelection(next));
    return <Suspense fallback={<p>Pending selection</p>}>
      <AccountSurvivalReview accountId={selection.accountId} date={selection.date} />
      <SuspendAfterReview selection={selection} />
    </Suspense>;
  }
  return {
    Harness,
    select: (selection: ReviewSelection) => select(selection),
    suspendedRenderReached,
    release: () => { blocked = false; releaseSuspendedRender.resolve(); },
  };
}

describe("AccountSurvivalReview entitlement states", () => {
  it("renders a free preview without premium mutation actions", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValue(freeAccess);
    vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(null);
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);

    await screen.findByText("Free preview");
    expect(screen.getByText(/Upgrade to Pro/)).toBeVisible();
    expect(screen.queryByRole("button", { name: "Create commitment" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Show evidence and limitations" })).toBeEnabled();
  });

  it("keeps preserved commitment and evaluation data read-only after downgrade", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValue(freeAccess);
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);

    await screen.findByText("Read-only access");
    expect(screen.getByText("Max Attempts — Followed")).toBeVisible();
    expect(screen.queryByRole("button", { name: "Edit commitment" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Evaluate commitment" })).not.toBeInTheDocument();
  });

  it("distinguishes rollout unavailability from an upgrade requirement", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValue({ ...freeAccess, rolloutMode: "DISABLED", rolloutAvailable: false, upgradeRequired: false, message: "Account Survival premium actions are not available in this release. Existing records remain read-only." });
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);

    await screen.findByText("Rollout unavailable");
    expect(screen.queryByText(/Upgrade to Pro/)).not.toBeInTheDocument();
  });

  it("fails closed when the entitlement request fails", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockRejectedValue(new Error("offline"));
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to verify Account Survival access");
    expect(screen.queryByRole("button", { name: "Evaluate commitment" })).not.toBeInTheDocument();
  });

  it("handles a server-side entitlement change without retaining premium actions", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess)
      .mockResolvedValueOnce(proAccess)
      .mockResolvedValue(freeAccess);
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockRejectedValue({ response: { status: 403, data: { errorCode: "SUBSCRIPTION_REQUIRED", message: "Upgrade to Pro to access Account Survival." } } });
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Evaluate commitment" }));

    await screen.findByText("Read-only access");
    expect(screen.getByRole("alert")).toHaveTextContent("Upgrade to Pro to access Account Survival.");
    expect(screen.queryByRole("button", { name: "Evaluate commitment" })).not.toBeInTheDocument();
  });

  it("clears prior premium data while switching accounts", async () => {
    mockLoad();
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await screen.findByText("Max Attempts — Followed");
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockReturnValue(new Promise(() => undefined));

    view.rerender(<AccountSurvivalReview accountId={8} date="2026-04-06" />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading Account Survival review");
    expect(screen.queryByText("Max Attempts — Followed")).not.toBeInTheDocument();
  });
});

describe("AccountSurvivalReview asynchronous selection isolation", () => {
  it("keeps a visible account A evaluation bound to A while account B has rendered and suspended", async () => {
    mockAccountAwareLoads();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockResolvedValue(completedEvaluation);
    const harness = concurrentReviewHarness({ accountId: 8, date: "2026-04-07" });
    render(<harness.Harness />);
    const user = userEvent.setup();
    await screen.findByText("Account 7 assessment");

    act(() => harness.select({ accountId: 8, date: "2026-04-07" }));
    await harness.suspendedRenderReached.promise;
    expect(screen.getByText("Account 7 assessment")).toBeVisible();
    expect(screen.queryByText("Pending selection")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Evaluate commitment" }));

    expect(survivalApi.evaluateProtectionCommitment).toHaveBeenCalledWith(7, "2026-04-06");
    expect(vi.mocked(survivalApi.evaluateProtectionCommitment).mock.calls.some(([accountId]) => accountId === 8)).toBe(false);
  });

  it("binds all editable account A values to A while account B is suspended", async () => {
    mockAccountAwareLoads("DRAFT");
    vi.mocked(survivalApi.getSurvivalAssessment).mockImplementation(async (accountId, date) => accountId === 7
      ? { ...assessmentFor(accountId, date), state: "INSUFFICIENT_DATA", missingDataReasons: ["Profile required"] }
      : assessmentFor(accountId, date));
    vi.mocked(survivalApi.saveRiskProfile).mockReturnValue(new Promise(() => undefined));
    vi.mocked(survivalApi.saveProtectionCommitment).mockReturnValue(new Promise(() => undefined));
    const harness = concurrentReviewHarness({ accountId: 8, date: "2026-04-07" });
    render(<harness.Harness />);
    const user = userEvent.setup();
    await screen.findByText("Account 7 assessment");
    await user.clear(screen.getByRole("spinbutton", { name: "Current account balance" }));
    await user.type(screen.getByRole("spinbutton", { name: "Current account balance" }), "51001.25");
    await user.click(screen.getByRole("button", { name: "Edit commitment" }));
    await user.clear(screen.getByRole("spinbutton", { name: "Maximum attempts" }));
    await user.type(screen.getByRole("spinbutton", { name: "Maximum attempts" }), "4");

    act(() => harness.select({ accountId: 8, date: "2026-04-07" }));
    await harness.suspendedRenderReached.promise;
    await user.click(screen.getByRole("button", { name: "Save risk profile" }));
    expect(vi.mocked(survivalApi.saveRiskProfile).mock.calls[0]).toEqual([7, {
      ...profile,
      accountId: 7,
      currentBalance: 51001.25,
    }]);

    // Resolve duplicate-mutation protection by remounting the still-committed A view,
    // then verify the independent commitment payload under the same suspended B render.
    const secondHarness = concurrentReviewHarness({ accountId: 8, date: "2026-04-07" });
    render(<secondHarness.Harness />);
    await screen.findAllByText("Account 7 assessment");
    const reviews = screen.getAllByText("Account 7 assessment");
    const secondReview = reviews[reviews.length - 1].closest("section")!;
    await user.click(within(secondReview).getByRole("button", { name: "Edit commitment" }));
    const attempts = within(secondReview).getByRole("spinbutton", { name: "Maximum attempts" });
    await user.clear(attempts); await user.type(attempts, "4");
    act(() => secondHarness.select({ accountId: 8, date: "2026-04-07" }));
    await secondHarness.suspendedRenderReached.promise;
    await user.click(within(secondReview).getByRole("button", { name: "Save commitment" }));
    expect(vi.mocked(survivalApi.saveProtectionCommitment).mock.calls[0]).toEqual([7, {
      ...commitmentFor(7, "DRAFT"),
      maxAttempts: 4,
      effectiveSessionDate: "2026-04-06",
    }]);
  });

  it("keeps account A acceptance bound to A after a suspended B render is abandoned", async () => {
    mockAccountAwareLoads("DRAFT");
    vi.mocked(survivalApi.acceptProtectionCommitment).mockReturnValue(new Promise(() => undefined));
    const harness = concurrentReviewHarness({ accountId: 8, date: "2026-04-07" });
    render(<harness.Harness />);
    const user = userEvent.setup();
    await screen.findByText("Account 7 assessment");
    act(() => harness.select({ accountId: 8, date: "2026-04-07" }));
    await harness.suspendedRenderReached.promise;
    act(() => harness.select({ accountId: 7, date: "2026-04-06" }));

    await user.click(screen.getByRole("button", { name: "Accept commitment" }));

    expect(vi.mocked(survivalApi.acceptProtectionCommitment).mock.calls).toEqual([[7, "2026-04-06"]]);
  });

  it("commits a suspended B selection and ignores a pending A completion", async () => {
    mockAccountAwareLoads();
    const pendingA = deferred<CommitmentEvaluation>();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(pendingA.promise);
    const harness = concurrentReviewHarness({ accountId: 8, date: "2026-04-07" });
    render(<harness.Harness />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Evaluate commitment" }));
    expect(vi.mocked(survivalApi.evaluateProtectionCommitment).mock.calls).toEqual([[7, "2026-04-06"]]);

    act(() => harness.select({ accountId: 8, date: "2026-04-07" }));
    await harness.suspendedRenderReached.promise;
    act(() => harness.release());
    await screen.findByText("Account 8 assessment");
    const account8Reads = vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.filter(([id]) => id === 8).length;
    pendingA.resolve(completedEvaluation);
    await pendingA.promise; await Promise.resolve();

    expect(screen.getByText("Account 8 assessment")).toBeVisible();
    expect(vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.filter(([id]) => id === 8)).toHaveLength(account8Reads);
    expect(screen.queryByText("Completed")).not.toBeInTheDocument();
  });

  it("keeps a suspended session-date change from retargeting the committed date", async () => {
    mockAccountAwareLoads();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(new Promise(() => undefined));
    const harness = concurrentReviewHarness({ accountId: 7, date: "2026-04-07" });
    render(<harness.Harness />);
    const user = userEvent.setup();
    await screen.findByText("Account 7 assessment");
    act(() => harness.select({ accountId: 7, date: "2026-04-07" }));
    await harness.suspendedRenderReached.promise;

    await user.click(screen.getByRole("button", { name: "Evaluate commitment" }));

    expect(vi.mocked(survivalApi.evaluateProtectionCommitment).mock.calls).toEqual([[7, "2026-04-06"]]);
  });

  it("invalidates the first Strict Mode effect lifecycle before enabling mutations", async () => {
    mockAccountAwareLoads();
    const obsoleteAccess = deferred<AccountSurvivalAccess>();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockReturnValueOnce(obsoleteAccess.promise).mockResolvedValue(proAccess);
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(new Promise(() => undefined));
    render(<StrictMode><AccountSurvivalReview accountId={7} date="2026-04-06" /></StrictMode>);
    const user = userEvent.setup();
    await screen.findByText("Account 7 assessment");

    obsoleteAccess.resolve(freeAccess);
    await obsoleteAccess.promise; await Promise.resolve();
    await user.click(screen.getByRole("button", { name: "Evaluate commitment" }));

    expect(screen.queryByText("Read-only access")).not.toBeInTheDocument();
    expect(vi.mocked(survivalApi.evaluateProtectionCommitment).mock.calls).toEqual([[7, "2026-04-06"]]);
  });

  it("ignores a delayed evaluation across A to B to A and never starts a stale refresh", async () => {
    mockAccountAwareLoads();
    const pending = deferred<CommitmentEvaluation>();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(pending.promise);
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Evaluate commitment" }));
    expect(survivalApi.evaluateProtectionCommitment).toHaveBeenCalledTimes(1);

    view.rerender(<AccountSurvivalReview accountId={8} date="2026-04-06" />);
    await screen.findByText("Account 8 assessment");
    view.rerender(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await screen.findByText("Account 7 assessment");
    pending.resolve(completedEvaluation);

    await waitFor(() => expect(screen.getByText("Max attempts").parentElement).toHaveTextContent("7"));
    expect(vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.filter(([id]) => id === 7)).toHaveLength(2);
    expect(screen.queryByText("Completed")).not.toBeInTheDocument();
  });

  it("isolates delayed save and accept completions from a newly selected account", async () => {
    mockAccountAwareLoads("DRAFT");
    const save = deferred<ProtectionCommitment>(); const accept = deferred<ProtectionCommitment>();
    vi.mocked(survivalApi.saveProtectionCommitment).mockReturnValue(save.promise);
    vi.mocked(survivalApi.acceptProtectionCommitment).mockReturnValue(accept.promise);
    const user = userEvent.setup();
    const saveView = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await user.click(await screen.findByRole("button", { name: "Edit commitment" }));
    await user.click(screen.getByRole("button", { name: "Save commitment" }));
    saveView.rerender(<AccountSurvivalReview accountId={8} date="2026-04-06" />);
    await screen.findByText("Account 8 assessment");
    save.resolve(commitmentFor(7, "DRAFT"));
    await waitFor(() => expect(screen.getByText("Max attempts").parentElement).toHaveTextContent("8"));
    saveView.unmount();

    const acceptView = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await user.click(await screen.findByRole("button", { name: "Accept commitment" }));
    acceptView.rerender(<AccountSurvivalReview accountId={8} date="2026-04-06" />);
    await screen.findByText("Account 8 assessment");
    accept.resolve(commitmentFor(7, "ACCEPTED"));
    await waitFor(() => expect(screen.getByText("Max attempts").parentElement).toHaveTextContent("8"));
    expect(screen.queryByText("Accepted")).not.toBeInTheDocument();
  });

  it("isolates profile completion and obsolete failures from the active account", async () => {
    mockAccountAwareLoads();
    vi.mocked(survivalApi.getSurvivalAssessment).mockImplementation(async (accountId, date) => accountId === 7
      ? { ...assessmentFor(accountId, date), state: "INSUFFICIENT_DATA", missingDataReasons: ["Profile required"] }
      : assessmentFor(accountId, date));
    const save = deferred<RiskProfile>();
    vi.mocked(survivalApi.saveRiskProfile).mockReturnValue(save.promise);
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Save risk profile" }));
    view.rerender(<AccountSurvivalReview accountId={8} date="2026-04-06" />);
    await screen.findByText("Account 8 assessment");
    save.reject(new Error("obsolete failure"));
    await waitFor(() => expect(screen.getByText("Account 8 assessment")).toBeVisible());
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Evaluate commitment" })).toBeEnabled();
  });

  it("refreshes current structured entitlement denials and fails closed if refresh fails", async () => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValueOnce(proAccess).mockRejectedValueOnce(new Error("offline"));
    vi.mocked(survivalApi.saveProtectionCommitment).mockRejectedValue({ response: { status: 403, data: { errorCode: "SUBSCRIPTION_REQUIRED", message: "Upgrade required." } } });
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Edit commitment" }));
    await user.click(screen.getByRole("button", { name: "Save commitment" }));

    await screen.findByText("Read-only access");
    expect(screen.getByRole("alert")).toHaveTextContent("premium actions remain disabled");
    expect(screen.queryByRole("button", { name: "Edit commitment" })).not.toBeInTheDocument();
  });

  it.each([
    ["SUBSCRIPTION_REQUIRED", 403, freeAccess, "Read-only access"],
    ["FEATURE_DISABLED", 404, { ...freeAccess, rolloutMode: "DISABLED" as const, rolloutAvailable: false, upgradeRequired: false, message: "Account Survival premium actions are not available in this release. Existing records remain read-only." }, "Rollout unavailable"],
  ])("refreshes authoritative access after save denial %s", async (errorCode, status, refreshedAccess, expectedNotice) => {
    mockLoad();
    vi.mocked(survivalApi.getAccountSurvivalAccess).mockResolvedValueOnce(proAccess).mockResolvedValueOnce(refreshedAccess);
    vi.mocked(survivalApi.saveProtectionCommitment).mockRejectedValue({ response: { status, data: { errorCode, message: "Premium action denied." } } });
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Edit commitment" }));
    await user.click(screen.getByRole("button", { name: "Save commitment" }));

    await screen.findByText(expectedNotice);
    expect(screen.getByRole("alert")).toHaveTextContent("Premium action denied.");
    expect(survivalApi.getAccountSurvivalAccess).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("button", { name: "Edit commitment" })).not.toBeInTheDocument();
  });

  it("does not reinterpret unrelated save failures as entitlement changes", async () => {
    mockLoad();
    vi.mocked(survivalApi.saveProtectionCommitment).mockRejectedValue({ response: { status: 500, data: { errorCode: "INTERNAL_ERROR", message: "Temporary failure." } } });
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Edit commitment" }));
    await user.click(screen.getByRole("button", { name: "Save commitment" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Temporary failure.");
    expect(survivalApi.getAccountSurvivalAccess).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Save commitment" })).toBeEnabled();
  });

  it("ignores an old-account denial and prevents duplicate pending mutations", async () => {
    mockAccountAwareLoads();
    const pending = deferred<CommitmentEvaluation>();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(pending.promise);
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup(); const button = await screen.findByRole("button", { name: "Evaluate commitment" });
    await user.click(button); await user.click(button);
    expect(survivalApi.evaluateProtectionCommitment).toHaveBeenCalledTimes(1);
    view.rerender(<AccountSurvivalReview accountId={8} date="2026-04-07" />);
    await screen.findByText("Account 8 assessment");
    pending.reject({ response: { status: 404, data: { errorCode: "FEATURE_DISABLED", message: "Unavailable." } } });
    await waitFor(() => expect(screen.getByText("Account 8 assessment")).toBeVisible());
    expect(screen.getByRole("button", { name: "Evaluate commitment" })).toBeEnabled();
    expect(screen.queryByText("Rollout unavailable")).not.toBeInTheDocument();
  });

  it("invalidates a pending mutation when only the selected session date changes", async () => {
    mockAccountAwareLoads();
    const pending = deferred<CommitmentEvaluation>();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(pending.promise);
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Evaluate commitment" }));

    view.rerender(<AccountSurvivalReview accountId={7} date="2026-04-07" />);
    await screen.findByText("Account 7 assessment");
    pending.resolve(completedEvaluation);

    await waitFor(() => expect(screen.getByText("Account 7 assessment")).toBeVisible());
    expect(screen.queryByText("Completed")).not.toBeInTheDocument();
    expect(vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.filter(([, date]) => date === "2026-04-06")).toHaveLength(1);
    expect(vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.filter(([, date]) => date === "2026-04-07")).toHaveLength(1);
  });

  it("does not refresh or update state after unmounting with a mutation pending", async () => {
    mockAccountAwareLoads();
    const pending = deferred<CommitmentEvaluation>();
    vi.mocked(survivalApi.evaluateProtectionCommitment).mockReturnValue(pending.promise);
    const view = render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Evaluate commitment" }));
    const readsBeforeUnmount = vi.mocked(survivalApi.getSurvivalAssessment).mock.calls.length;

    view.unmount();
    pending.resolve(completedEvaluation);
    await pending.promise;
    await Promise.resolve();

    expect(survivalApi.getSurvivalAssessment).toHaveBeenCalledTimes(readsBeforeUnmount);
    expect(survivalApi.getCommitmentEvaluation).toHaveBeenCalledTimes(1);
  });
});

describe("AccountSurvivalReview commitment evaluation", () => {
  it("announces loading without showing stale evaluation results", () => {
    let resolveAssessment: (value: SurvivalAssessment) => void = () => undefined;
    vi.mocked(survivalApi.getSurvivalAssessment).mockReturnValue(new Promise(resolve => { resolveAssessment = resolve; }));
    vi.mocked(survivalApi.getRiskProfile).mockResolvedValue(profile);
    vi.mocked(survivalApi.getProtectionCommitment).mockResolvedValue(commitment);
    vi.mocked(survivalApi.getCommitmentEvaluation).mockResolvedValue(completedEvaluation);

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
    expect(screen.queryByText("Broken")).not.toBeInTheDocument();

  });

  it("renders a neutral not-evaluable evaluation with its evidence limitation", async () => {
    await renderLoaded({ ...completedEvaluation, overallStatus: "NOT_EVALUABLE", rules: [rules[5]] });

    expect(screen.getAllByText("Not Evaluable").length).toBeGreaterThan(0);
    const evidenceButton = screen.getByRole("button", { name: "Show evidence" });
    await userEvent.setup().click(evidenceButton);
    expect(screen.getByText("Completed-trade data does not prove contemporaneous confirmation.")).toBeVisible();
    expect(screen.queryByText("Broken")).not.toBeInTheDocument();
  });

  it("keeps rendered profile and commitment data usable when evaluation loading fails", async () => {
    await renderLoaded(new Error("evaluation unavailable"));

    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load the commitment evaluation.");
    expect(screen.getByText("Remaining buffer")).toBeVisible();
    expect(screen.getByRole("button", { name: "Edit commitment" })).toBeEnabled();
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
    render(<AccountSurvivalReview accountId={7} date="2026-04-06" />);
    await screen.findByText("A realized loss was followed by a same-family entry.");
    expect(screen.queryByText("Cross-account evidence")).not.toBeInTheDocument();
  });
});
