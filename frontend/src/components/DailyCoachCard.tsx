import type { CoachAiResponse, CoachSnapshot } from "../types/coachTypes";
import { CoachInsightCard } from "./CoachInsightCard";
import {
    coachButtonClass,
    coachButtonStyle,
    coachCardClass,
    coachCardStyle,
    coachErrorStyle,
    coachInnerClass,
    coachInnerStyle,
    coachMutedClass,
    coachNoticeStyle,
    coachSectionTitleClass,
} from "./coachStyles";

type Props = {
    snapshot: CoachSnapshot | null;
    loading: boolean;
    generating: boolean;
    error?: string | null;
    onRegenerate: () => void;
    remaining?: number;
    limitReached?: boolean;
    limitMessage?: string;
    hasCachedSnapshot?: boolean;
    onUsageChanged?: () => void;
    onLimitClick?: () => void;
};

function parseAi(json?: string): CoachAiResponse | null {
    if (!json) return null;

    try {
        return JSON.parse(json) as CoachAiResponse;
    } catch {
        return null;
    }
}

export function DailyCoachCard({
    snapshot,
    loading,
    generating,
    error,
    onRegenerate,
    remaining = 1,
    limitReached = false,
    limitMessage,
    hasCachedSnapshot = false,
    onUsageChanged,
    onLimitClick,
}: Props) {
    const ai = parseAi(snapshot?.aiResponseJson);
    const isRunning =
        snapshot?.status === "PENDING" || snapshot?.status === "PROCESSING";
    const topLeak = snapshot?.topLeakToWatch;
    const todaySummary = snapshot?.selectedDaySummary || ai?.headline || readinessLabel(snapshot?.netPnl ?? 0, snapshot?.profitFactor ?? 0);
    const mainIssueToday = snapshot?.primaryIssueToday || ai?.avoidNextSession || snapshot?.biggestMistake || "Wait for clean confirmation before adding risk.";
    const directionReview = snapshot?.setupDirectionReview || ai?.nextSessionFocus || "Review whether direction, setup, and execution quality aligned.";
    const historicalPattern = snapshot?.historicalPatternToWatch
        || (topLeak
            ? `Account-level pattern: ${topLeak.summary || topLeak.title}.`
            : "No cached account-level leak result yet.");
    const nextRule = snapshot?.nextSessionRule || ai?.oneRuleForNextSession || "Trade the plan, cap losses, and pause after any rule break.";
    const dataQuality = [
        snapshot?.dataQuality || "Data Quality",
        snapshot?.dataQualityReason,
    ].filter(Boolean).join(": ");
    const isNoTradeDay = Boolean(snapshot?.noTradeDay);
    const isLimitReached = !isNoTradeDay && (remaining === 0 || limitReached);
    const canShowCachedSnapshot = limitReached ? hasCachedSnapshot : Boolean(snapshot);
    const displaySnapshot = snapshot && (!limitReached || hasCachedSnapshot);

    return (
        <section className={coachCardClass} style={coachCardStyle}>
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-xl font-bold text-[#f8fafc]">Daily Coach</h2>
                    <p className={coachMutedClass}>
                        Mentor-style review for today’s trading behavior.
                    </p>
                </div>

                <button
                    onClick={async () => {
                        if (isNoTradeDay) {
                            return;
                        }
                        if (isLimitReached) {
                            onLimitClick?.();
                            return;
                        }
                        await onRegenerate();
                        onUsageChanged?.();
                    }}
                    disabled={generating || isRunning || isLimitReached || isNoTradeDay}
                    className={coachButtonClass}
                    style={coachButtonStyle}
                >
                    {generating || isRunning ? "Generating..." : isNoTradeDay ? "No trades to review" : isLimitReached ? "Limit reached" : "Regenerate"}
                </button>
            </div>

            {isNoTradeDay && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    No trades for this date. Import trades before regenerating coach feedback.
                </p>
            )}

            {isLimitReached && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    {limitMessage
                        ?? (canShowCachedSnapshot
                            ? "Limit reached. Showing cached Daily Coach snapshot."
                            : "You have reached this month’s Daily Coach generation limit. No cached snapshot is available for this date.")}
                </p>
            )}

            {loading && <p className={`mt-4 ${coachMutedClass}`}>Loading coach...</p>}

            {error && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachErrorStyle}>
                    {error}
                </p>
            )}

            {displaySnapshot && snapshot?.status === "FAILED" && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachErrorStyle}>
                    {snapshot.errorMessage ?? "Coach generation failed."}
                </p>
            )}

            {displaySnapshot && isRunning && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    AI Coach is analyzing your trades. This page will update automatically.
                </p>
            )}

            {!loading && !snapshot && !error && !isRunning && !isLimitReached && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>
                        No AI coach snapshots yet
                    </h3>
                    <p className={`mt-2 leading-6 ${coachMutedClass}`}>
                        Import trades for this account, then generate a Daily Coach snapshot
                        to review behavior, mistakes, and next-session focus.
                    </p>
                </div>
            )}

            {!loading && isLimitReached && !hasCachedSnapshot && !displaySnapshot && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>Daily Coach limit reached</h3>
                    <p className={`mt-2 leading-6 ${coachMutedClass}`}>
                        No cached coach snapshot is available for this date. Try another date with an existing snapshot or wait until your limit resets.
                    </p>
                </div>
            )}

            {displaySnapshot && (
                <div className="mt-5 grid gap-4 md:grid-cols-3">
                    <CoachInsightCard title="Today’s Session" value={todaySummary} eyebrow="Today" />
                    <CoachInsightCard title="Main Issue Today" value={mainIssueToday} eyebrow="Today" />
                    <CoachInsightCard title="Setup / Direction Review" value={directionReview} eyebrow="Today" />
                </div>
            )}

            {displaySnapshot && (
                <div className="mt-4 grid gap-4 md:grid-cols-3">
                    <CoachInsightCard
                        title="Account-Level Pattern"
                        value={historicalPattern}
                        eyebrow={snapshot.historicalPatternAppliesToday === false ? "Not today's issue" : "Account-level"}
                    />
                    <CoachInsightCard title="Rule for Next Session" value={nextRule} eyebrow="Action rule" />
                    <CoachInsightCard title="Data Quality" value={dataQuality} eyebrow={snapshot?.dataQuality || "Data"} />
                </div>
            )}

            {displaySnapshot && ai && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>Coach Details</h3>

                    <div className="mt-4 grid gap-4 md:grid-cols-2">
                        <CoachInsightCard title="Emotional Pattern" value={ai.emotionalPattern} />
                        <CoachInsightCard title="Best Behavior" value={ai.bestBehavior} />
                        <CoachInsightCard title="Avoid Next Session" value={ai.avoidNextSession} />
                        <CoachInsightCard title="Next Focus" value={ai.nextSessionFocus} />
                    </div>

                    <div className="mt-5 rounded-2xl border border-emerald-300/20 bg-emerald-400/10 p-4 text-[#dcfce7]">
                        <p className="text-sm text-[#86efac]">One rule for next session</p>
                        <p className="mt-1 text-lg font-semibold">
                            {ai.oneRuleForNextSession}
                        </p>
                    </div>
                </div>
            )}
        </section>
    );
}

function readinessLabel(netPnl: number, profitFactor: number) {
    if (profitFactor >= 1.5 && netPnl >= 0) return "Strong: protect the plan";
    if (profitFactor >= 1 || netPnl >= 0) return "Neutral: stay selective";
    return "Cautious: reduce risk";
}
