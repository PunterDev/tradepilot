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

export function WeeklyCoachCard({
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
    const isNoTradeWeek = Boolean(snapshot?.noTradeWeek);
    const isLimitReached = !isNoTradeWeek && (remaining === 0 || limitReached);
    const canShowCachedSnapshot = limitReached ? hasCachedSnapshot : Boolean(snapshot);
    const displaySnapshot = snapshot && (!limitReached || hasCachedSnapshot);

    return (
        <section className={coachCardClass} style={coachCardStyle}>
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-xl font-bold text-[#f8fafc]">Weekly Coach</h2>
                    <p className={coachMutedClass}>
                        Detect your strongest days, weakest habits, and recurring patterns.
                    </p>
                </div>

                <button
                    onClick={async () => {
                        if (isNoTradeWeek) {
                            return;
                        }
                        if (isLimitReached) {
                            onLimitClick?.();
                            return;
                        }
                        await onRegenerate();
                        onUsageChanged?.();
                    }}
                    disabled={generating || isRunning || isLimitReached || isNoTradeWeek}
                    className={coachButtonClass}
                    style={coachButtonStyle}
                >
                    {generating || isRunning ? "Generating..." : isNoTradeWeek ? "No trades to review" : isLimitReached ? "Limit reached" : "Regenerate"}
                </button>
            </div>

            {isNoTradeWeek && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    No trades for this week. Import trades before regenerating weekly coach feedback.
                </p>
            )}

            {isLimitReached && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    {limitMessage
                        ?? (canShowCachedSnapshot
                            ? "Limit reached. Showing cached Weekly Coach snapshot."
                            : "You have reached this month’s Weekly Coach generation limit. No cached snapshot is available for this week.")}
                </p>
            )}

            {loading && <p className={`mt-4 ${coachMutedClass}`}>Loading coach...</p>}

            {error && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachErrorStyle}>
                    {error}
                </p>
            )}

            {displaySnapshot && isRunning && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    Weekly Coach is being generated. This page will update automatically.
                </p>
            )}

            {!loading && !snapshot && !error && !isRunning && !isLimitReached && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>
                        No AI coach snapshots yet
                    </h3>
                    <p className={`mt-2 leading-6 ${coachMutedClass}`}>
                        Import trades for this account, then generate a Weekly Coach snapshot
                        to review recurring patterns, best setups, and next-week focus.
                    </p>
                </div>
            )}

            {!loading && isLimitReached && !hasCachedSnapshot && !displaySnapshot && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>Weekly Coach limit reached</h3>
                    <p className={`mt-2 leading-6 ${coachMutedClass}`}>
                        No cached coach snapshot is available for this week. Try another week with an existing snapshot or wait until your limit resets.
                    </p>
                </div>
            )}

            {displaySnapshot && (
                <div className="mt-5 grid gap-4 md:grid-cols-4">
                    <CoachInsightCard title="Trades" value={snapshot.totalTrades} />
                    <CoachInsightCard title="Net P&L" value={`$${snapshot.netPnl ?? 0}`} />
                    <CoachInsightCard title="Win Rate" value={`${snapshot.winRate ?? 0}%`} />
                    <CoachInsightCard title="Profit Factor" value={snapshot.profitFactor} />
                </div>
            )}

            {displaySnapshot && (
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <CoachInsightCard title="Best Session" value={snapshot.bestSession} />
                    <CoachInsightCard title="Worst Session" value={snapshot.worstSession} />
                    <CoachInsightCard title="Best Setup" value={snapshot.bestSetup} />
                    <CoachInsightCard title="Worst Setup" value={snapshot.worstSetup} />
                </div>
            )}

            {displaySnapshot && isNoTradeWeek && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>No weekly trade data yet</h3>
                    <div className="mt-4 grid gap-4 md:grid-cols-2">
                        <CoachInsightCard
                            title="Weekly Summary"
                            value={snapshot.selectedDaySummary || "No trades found for this week."}
                            eyebrow="No trades"
                        />
                        <CoachInsightCard
                            title="Setup Review"
                            value={snapshot.setupDirectionReview || "No setup review available because there were no trades this week."}
                            eyebrow="No setup data"
                        />
                        <CoachInsightCard
                            title="Behavior Review"
                            value={snapshot.dataQualityReason || "No emotional or behavioral pattern can be assessed without trades."}
                            eyebrow={snapshot.dataQuality || "No trades"}
                        />
                        <CoachInsightCard
                            title="Next Step"
                            value={snapshot.nextSessionRule || "Import trades for this week to generate AI coaching."}
                            eyebrow="Action"
                        />
                    </div>
                </div>
            )}

            {displaySnapshot && !isNoTradeWeek && ai && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>{ai.headline}</h3>

                    <div className="mt-4 grid gap-4 md:grid-cols-2">
                        <CoachInsightCard title="Recurring Pattern" value={ai.emotionalPattern} />
                        <CoachInsightCard title="Best Behavior" value={ai.bestBehavior} />
                        <CoachInsightCard title="Risk Note" value={ai.riskNote} />
                        <CoachInsightCard title="Discipline Note" value={ai.disciplineNote} />
                    </div>

                    <div className="mt-5 rounded-2xl border border-emerald-300/20 bg-emerald-400/10 p-4 text-[#dcfce7]">
                        <p className="text-sm text-[#86efac]">Next week focus</p>
                        <p className="mt-1 text-lg font-semibold">{ai.nextSessionFocus}</p>
                    </div>
                </div>
            )}
        </section>
    );
}
