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
    onUsageChanged,
    onLimitClick,
}: Props) {
    const ai = parseAi(snapshot?.aiResponseJson);
    const isRunning =
        snapshot?.status === "PENDING" || snapshot?.status === "PROCESSING";

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
                        if (remaining === 0) {
                            onLimitClick?.();
                            return;
                        }
                        await onRegenerate();
                        onUsageChanged?.();
                    }}
                    disabled={generating || isRunning}
                    className={coachButtonClass}
                    style={coachButtonStyle}
                >
                    {generating || isRunning ? "Generating..." : remaining === 0 ? "Limit reached" : "Regenerate"}
                </button>
            </div>

            {remaining === 0 && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    You have reached this month’s Daily Coach generation limit. Cached snapshots remain viewable.
                </p>
            )}

            {loading && <p className={`mt-4 ${coachMutedClass}`}>Loading coach...</p>}

            {error && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachErrorStyle}>
                    {error}
                </p>
            )}

            {snapshot?.status === "FAILED" && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachErrorStyle}>
                    {snapshot.errorMessage ?? "Coach generation failed."}
                </p>
            )}

            {isRunning && (
                <p className="mt-4 rounded-xl border p-3 text-sm" style={coachNoticeStyle}>
                    AI Coach is analyzing your trades. This page will update automatically.
                </p>
            )}

            {!loading && !snapshot && !error && !isRunning && (
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

            {snapshot && (
                <div className="mt-5 grid gap-4 md:grid-cols-4">
                    <CoachInsightCard title="Trades" value={snapshot.totalTrades} />
                    <CoachInsightCard title="Net P&L" value={`$${snapshot.netPnl ?? 0}`} />
                    <CoachInsightCard title="Win Rate" value={`${snapshot.winRate ?? 0}%`} />
                    <CoachInsightCard title="Profit Factor" value={snapshot.profitFactor} />
                </div>
            )}

            {snapshot && (
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <CoachInsightCard title="Best Setup" value={snapshot.bestSetup} />
                    <CoachInsightCard title="Biggest Mistake" value={snapshot.biggestMistake} />
                </div>
            )}

            {ai && (
                <div className={`mt-5 ${coachInnerClass}`} style={coachInnerStyle}>
                    <h3 className={coachSectionTitleClass}>{ai.headline}</h3>

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
