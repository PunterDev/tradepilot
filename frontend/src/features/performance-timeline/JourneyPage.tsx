import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { TradingAccount } from "../../api/accountApi";
import { getPerformanceTimeline, regeneratePerformanceTimeline } from "../../api/performanceTimelineApi";
import PageHeader from "../../layout/PageHeader";
import { button } from "../../styles/dashboardStyles";
import type { SelectedAccountId } from "../../types/dashboard";
import type {
  PerformanceCategoryScores,
  PerformanceMilestone,
  PerformanceTimelineMonth,
  PerformanceTimelineResponse,
} from "../../types/performanceTimelineTypes";
import {
  categoryFromScores,
  categoryInsights,
  cleanSentence,
  developmentRoadmap,
  developmentStage,
  groupInactiveMonths,
  groupMilestones,
  initialStrength,
  isFirstScoredMonth,
  journeySummary,
  latestReady,
  monthLabel,
  monthlyTheme,
  nextDevelopmentMilestone,
  previousReady,
  primaryWeakness,
  scoreDescriptor,
  secondaryObservation,
  shortNextFocus,
  suppressDuplicate,
  usefulNextFocus,
  type CategoryInsight,
  type JourneyTimelineItem,
  type MilestoneGroup,
  type RoadmapStage,
} from "./journeyLogic";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  onSelectAccount: (accountId: SelectedAccountId) => void;
  onUsageChanged: () => void;
};

export default function JourneyPage({ accounts, selectedAccountId, onSelectAccount, onUsageChanged }: Props) {
  const initialAccountId = useMemo(() => {
    if (selectedAccountId === "ALL" || selectedAccountId === "") return accounts[0]?.id;
    return Number(selectedAccountId);
  }, [accounts, selectedAccountId]);
  const [accountId, setAccountId] = useState<number | undefined>(initialAccountId);
  const [data, setData] = useState<PerformanceTimelineResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [error, setError] = useState("");
  const [scoreInfoOpen, setScoreInfoOpen] = useState(false);

  useEffect(() => {
    setAccountId(initialAccountId);
  }, [initialAccountId]);

  useEffect(() => {
    if (!accountId) return;
    setLoading(true);
    setError("");
    getPerformanceTimeline(accountId)
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [accountId]);

  async function regenerate() {
    if (!accountId) return;
    setRegenerating(true);
    setError("");
    try {
      const next = await regeneratePerformanceTimeline(accountId);
      setData(next);
      onUsageChanged();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setRegenerating(false);
    }
  }

  const hasEligibleMonths = Boolean(data?.months?.some((month) => month.status === "READY"));

  return (
    <>
      <PageHeader
        label="Journey • Pro"
        title="AI Performance Timeline"
        description="See how your discipline, execution, risk management, and trading behavior are evolving over time."
      />
      <section style={shell}>
        <div style={toolbar}>
          <div style={controlGroup}>
            <label style={label}>Account</label>
            <select
              value={accountId ?? ""}
              onChange={(event) => {
                const next = Number(event.target.value);
                setAccountId(next);
                onSelectAccount(next);
              }}
              style={select}
            >
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.name}</option>
              ))}
            </select>
          </div>
          <button
            type="button"
            onClick={regenerate}
            disabled={!accountId || regenerating || !hasEligibleMonths}
            style={{ ...button, width: 260, borderRadius: 8, opacity: !hasEligibleMonths ? 0.6 : 1 }}
            title={!hasEligibleMonths ? "At least one month needs five trades before regeneration uses AI credits." : "Regenerate the full journey"}
          >
            {regenerating ? "Regenerating..." : "Regenerate AI Coaching · 3 credits"}
          </button>
          <p style={toolbarHelp}>Performance metrics update automatically. Regeneration refreshes the written coaching analysis.</p>
        </div>

        {loading && <div style={notice}>Building your performance journey...</div>}
        {error && <div style={errorBox}>{error}</div>}

        {!loading && data && (
          <>
            <HeroSummary data={data} onExplainScore={() => setScoreInfoOpen(true)} />
            <DevelopmentRoadmap months={data.months} />
            <NextMilestone months={data.months} />
            <ScoreTrend months={data.months} />
            <CategoryCards data={data} />
            <Timeline months={data.months} />
            <Milestones data={data} />
          </>
        )}
        {scoreInfoOpen && <ScoreInfoModal onClose={() => setScoreInfoOpen(false)} />}
      </section>
    </>
  );
}

function HeroSummary({ data, onExplainScore }: { data: PerformanceTimelineResponse; onExplainScore: () => void }) {
  const current = latestReady(data.months);
  const previous = previousReady(data.months);
  const firstScored = isFirstScoredMonth(data.months);
  const strongest = categoryFromScores(current?.categoryScores, true) ?? data.strongestCategory;
  const weakest = categoryFromScores(current?.categoryScores, false) ?? data.weakestCategory;
  const stage = developmentStage(current?.categoryScores, current?.scoreConfidence);
  const summary = journeySummary(current, firstScored);
  const strength = firstScored
    ? initialStrength(current?.categoryScores)
    : suppressDuplicate(current?.narrative?.biggestImprovement, [summary]) ?? improvementFallback(strongest);
  const weakness = primaryWeakness(current?.categoryScores, current?.metrics);
  const nextFocus = shortNextFocus(weakest, current?.metrics);

  return (
    <section style={heroCard}>
      <div style={heroMain}>
        <div style={heroTopline}>
          <span style={eyebrow}>Performance Journey Report</span>
          <span style={pill}>{data.cached ? "Cached" : "Updated"}</span>
          <span style={pill}>Generated {formatDate(data.generatedAt)}</span>
          <span style={confidencePill(current?.scoreConfidence)}>{current?.scoreConfidence ?? "LOW"} confidence</span>
        </div>
        <h2 style={heroTitle}>{headlineFor(data)}</h2>
        <p style={heroText}>{summary}</p>
      </div>
      <div style={heroScorePanel}>
        <Metric
          label="Current Evolution Score"
          value={formatScore(data.currentEvolutionScore)}
          tone={scoreTone(data.currentEvolutionScore)}
          supportingText={scoreDescriptor(data.currentEvolutionScore)}
        />
      </div>
      <div style={heroInsightGrid}>
        <HeroInsight label="Current stage" value={stage} />
        <HeroInsight label={firstScored ? "Initial strength" : "Biggest improvement"} value={strength} />
        <HeroInsight label="Primary weakness" value={weakness} />
        <HeroInsight label="Next focus" value={nextFocus} />
      </div>
      <div style={heroMetricGrid}>
        <Metric
          label={previous ? "Previous Score" : "Comparison Status"}
          value={previous ? formatScore(data.previousEvolutionScore) : "First scored month"}
          tone={neutralTone}
          supportingText={previous ? formatChange(data.scoreChange) : "Month-over-month comparisons begin after the next eligible month."}
        />
        <Metric label="Highest Score" value={formatScore(data.highestEvolutionScore)} tone={neutralTone} />
        <Metric label="Strongest Category" value={strongest ?? "—"} tone={positiveTone} />
        <Metric label="Weakest Category" value={weakest ?? "—"} tone={warningTone} />
      </div>
      <button type="button" style={scoreInfoButton} onClick={onExplainScore}>How the score works</button>
    </section>
  );
}

function DevelopmentRoadmap({ months }: { months: PerformanceTimelineMonth[] }) {
  const stages = developmentRoadmap(months);
  return (
    <section style={roadmapCard}>
      <div style={sectionHeader}>
        <div>
          <span style={eyebrow}>Development Roadmap</span>
          <p style={muted}>Where your process is now, and what the journey is building toward.</p>
        </div>
      </div>
      <div style={roadmapGrid}>
        {stages.map((stage) => <RoadmapStep key={stage.title} stage={stage} />)}
      </div>
    </section>
  );
}

function RoadmapStep({ stage }: { stage: RoadmapStage }) {
  const isCurrent = stage.status === "current";
  const isComplete = stage.status === "complete";
  return (
    <div style={roadmapStep(stage.status)} aria-current={isCurrent ? "step" : undefined}>
      <span style={roadmapDot(stage.status)}>{isComplete ? "✓" : isCurrent ? "•" : ""}</span>
      <div style={roadmapCopy}>
        <strong>{stage.title}</strong>
        <span>{stage.description}</span>
      </div>
    </div>
  );
}

function NextMilestone({ months }: { months: PerformanceTimelineMonth[] }) {
  const milestone = nextDevelopmentMilestone(months);
  if (!milestone) return null;
  return (
    <section style={nextMilestoneCard}>
      <div>
        <span style={eyebrow}>Next Development Milestone</span>
        <h3 style={nextMilestoneTitle}>{milestone.title}</h3>
        <p style={muted}>{milestone.action}</p>
      </div>
      <div style={nextMilestoneStats}>
        <MiniMetric label="Current" value={milestone.currentValue} />
        <MiniMetric label="Target" value={milestone.targetValue} />
      </div>
    </section>
  );
}

function HeroInsight({ label, value }: { label: string; value: string }) {
  return (
    <div style={heroInsight}>
      <span style={mutedLabel}>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function CategoryCards({ data }: { data: PerformanceTimelineResponse }) {
  const current = latestReady(data.months);
  const previous = previousReady(data.months);
  const insights = categoryInsights(current?.categoryScores, previous?.categoryScores);
  return (
    <section style={categorySection}>
      <div style={sectionHeader}>
        <div>
          <span style={eyebrow}>Process Categories</span>
          <p style={muted}>Scores measure process quality and behavioral consistency, not future profitability.</p>
        </div>
      </div>
      <div style={categoryGrid}>
        {insights.map((insight) => (
          <CategoryCard key={insight.key} insight={insight} />
        ))}
      </div>
    </section>
  );
}

function CategoryCard({ insight }: { insight: CategoryInsight }) {
  return (
    <div style={categoryCard}>
      <div style={categoryTopline}>
        <span style={categoryLabelRow}>
          <CategoryIcon category={insight.key} />
          <span style={mutedLabel}>{insight.label}</span>
        </span>
        <span style={categoryRating(insight.score)}>{insight.rating}</span>
      </div>
      <div style={categoryScoreRow}>
        <strong style={categoryValue}>{formatScore(insight.score)}</strong>
        <span style={changePill(insight.change)}>{formatChange(insight.change)}</span>
      </div>
      <div style={barTrack}>
        <div style={{ ...barFill, width: `${Math.max(0, Math.min(100, insight.score ?? 0))}%` }} />
      </div>
      <p style={categoryInterpretation}>{insight.interpretation}</p>
    </div>
  );
}

function CategoryIcon({ category }: { category: CategoryInsight["key"] }) {
  const pathByCategory: Record<CategoryInsight["key"], string> = {
    discipline: "M12 3l7 3v5c0 4.5-2.9 8.2-7 10-4.1-1.8-7-5.5-7-10V6l7-3z M9 12l2 2 4-5",
    riskManagement: "M12 4a8 8 0 108 8 M12 7v5l3 3",
    execution: "M4 12h16 M12 4v16 M8 8l8 8 M16 8l-8 8",
    consistency: "M4 16l4-4 3 3 5-7 4 4",
    psychology: "M8 13a4 4 0 118 0v5H8v-5z M9 10a3 3 0 016 0",
  };
  return (
    <span style={categoryIconWrap} aria-hidden="true">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d={pathByCategory[category]} />
      </svg>
    </span>
  );
}

function ScoreTrend({ months }: { months: PerformanceTimelineMonth[] }) {
  const scored = months
    .filter((month) => month.status === "READY" && month.evolutionScore != null)
    .slice()
    .reverse()
    .map((month) => ({ label: `${month.month.slice(0, 3)} ${String(month.year).slice(2)}`, score: month.evolutionScore }));
  return (
    <>
      {scored.length < 2 ? (
        <div style={inlineTrendNotice}>Trend tracking begins after your next eligible month.</div>
      ) : (
        <section style={trendCard} aria-label="Evolution Score trend">
          <div style={sectionHeader}>
            <div>
              <span style={eyebrow}>Evolution Score Trend</span>
              <p style={muted}>Eligible months only. Inactive months are not plotted as zero.</p>
            </div>
          </div>
        <div style={chartBox}>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={scored} margin={{ top: 12, right: 18, bottom: 8, left: -18 }}>
              <CartesianGrid stroke="rgba(148,163,184,0.12)" vertical={false} />
              <XAxis dataKey="label" stroke="#94a3b8" tickLine={false} axisLine={false} />
              <YAxis domain={[0, 100]} stroke="#94a3b8" tickLine={false} axisLine={false} />
              <Tooltip contentStyle={tooltipStyle} formatter={(value) => [`${value}/100`, "Evolution Score"]} />
              <Line type="monotone" dataKey="score" stroke="#22c55e" strokeWidth={3} dot={{ r: 4, fill: "#14b8a6", strokeWidth: 0 }} activeDot={{ r: 6 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
        </section>
      )}
    </>
  );
}

function Timeline({ months }: { months: PerformanceTimelineMonth[] }) {
  const items = useMemo(() => groupInactiveMonths(months), [months]);
  const previousEligibleFor = (month: PerformanceTimelineMonth) => {
    const index = months.findIndex((candidate) => candidate.periodStart === month.periodStart);
    return index < 0 ? null : months.slice(index + 1).find((candidate) => candidate.status === "READY") ?? null;
  };
  return (
    <section style={timelineSection}>
      <div style={sectionHeader}>
        <span style={eyebrow}>Monthly Chapters</span>
        <p style={muted}>Full chapters require at least five valid trades. Limited months never consume AI credits.</p>
      </div>
      <div style={timelineList}>
        {items.map((item) => item.type === "inactive-range"
          ? <InactiveRange key={item.key} item={item} />
          : <MonthChapter key={item.month.periodStart} month={item.month} previousEligible={previousEligibleFor(item.month)} />)}
      </div>
    </section>
  );
}

function InactiveRange({ item }: { item: Extract<JourneyTimelineItem, { type: "inactive-range" }> }) {
  const [expanded, setExpanded] = useState(false);
  return (
    <article style={inactiveGroupCard}>
      <div>
        <span style={statusPill("NO_ACTIVITY")}>Inactive period</span>
        <h3 style={inactiveTitle}>{item.months.length} inactive months hidden</h3>
        <p style={muted}>{item.startLabel} through {item.endLabel}</p>
      </div>
      <button type="button" style={secondaryButton} onClick={() => setExpanded((value) => !value)}>
        {expanded ? "Hide months" : "Show months"}
      </button>
      {expanded && (
        <div style={inactiveExpandedList}>
          {item.months.map((month) => <MonthChapter key={month.periodStart} month={month} compact />)}
        </div>
      )}
    </article>
  );
}

function MonthChapter({ month, compact = false, previousEligible = null }: { month: PerformanceTimelineMonth; compact?: boolean; previousEligible?: PerformanceTimelineMonth | null }) {
  const strongest = categoryFromScores(month.categoryScores, true);
  const weakest = categoryFromScores(month.categoryScores, false);
  const firstScored = month.scoreChange == null;
  const summary = month.status === "READY"
    ? journeySummary(month, firstScored)
    : month.message ?? "This month needs more data before a full chapter can be scored.";
  const improvement = firstScored
    ? initialStrength(month.categoryScores)
    : suppressDuplicate(month.narrative?.biggestImprovement, [summary]) ?? improvementFallback(strongest);
  const weakness = primaryWeakness(month.categoryScores, month.metrics);
  const nextFocus = usefulNextFocus(month.narrative?.nextFocus, weakest, month.metrics, [summary, improvement, weakness, month.narrative?.theme]);
  const theme = monthlyTheme(month, previousEligible);
  const observation = secondaryObservation(month.categoryScores, month.narrative?.biggestLeak, month.metrics);
  const isReady = month.status === "READY";

  return (
    <article style={{ ...monthCard, ...(compact ? compactMonthCard : {}) }}>
      <div style={monthRail} />
      <div style={monthHeader}>
        <div>
          <span style={statusPill(month.status)}>{statusLabel(month.status)}</span>
          <h3 style={monthTitle}>{monthLabel(month)}</h3>
          <p style={monthTheme}>{theme}</p>
        </div>
        <div style={{ textAlign: "right" }}>
          <div style={scoreBadge}>{formatScore(month.evolutionScore)}</div>
          <span style={mutedLabel}>{month.scoreConfidence ?? "LOW"} confidence</span>
        </div>
      </div>
      <div style={chapterSummaryGrid}>
        <MiniMetric label={firstScored ? "Comparison" : "Score Change"} value={firstScored ? "First scored month" : formatChange(month.scoreChange)} />
        <MiniMetric label="Trades" value={String(month.tradeCount)} />
        <MiniMetric label="Total R" value={formatR(month.metrics?.totalR)} />
        <MiniMetric label="Net P&L" value={formatMoney(month.metrics?.netPnl)} />
      </div>
      {isReady ? (
        <>
          <div style={chapterInsightGrid}>
            <ChapterInsight label={firstScored ? "Initial strength" : "Main improvement"} value={improvement} tone={positiveTone} />
            <ChapterInsight label="Primary weakness" value={weakness} tone={warningTone} />
          </div>
          {observation && <div style={secondaryObservationBox}>{observation}</div>}
          <div style={collapsedFocus}><strong>Next focus</strong><span>{nextFocus}</span></div>
          <details style={detailsBlock}>
            <summary style={detailsSummary}>View full {month.month} review ›</summary>
            <div style={expandedChapter}>
              <NarrativeSection month={month} improvement={improvement} weakness={weakness} nextFocus={nextFocus} />
              <div style={metricRow}>
                <MiniMetric label="Win Rate" value={`${month.metrics?.winRate ?? 0}%`} />
                <MiniMetric label="Avg R" value={formatR(month.metrics?.averageR)} />
                <MiniMetric label="Profit Factor" value={formatNumber(month.metrics?.profitFactor)} />
                <MiniMetric label="Metadata" value={`${month.metrics?.metadataCompletionPercent ?? 0}%`} />
              </div>
              <CategoryMiniScores scores={month.categoryScores} />
              {Boolean(month.milestones?.length) && <MonthMilestones milestones={month.milestones ?? []} />}
            </div>
          </details>
        </>
      ) : (
        <div style={month.status === "LIMITED_DATA" ? limitedBox : inactiveBox}>{month.message}</div>
      )}
    </article>
  );
}

function ChapterInsight({ label, value, tone }: { label: string; value: string; tone: CSSProperties }) {
  return (
    <div style={chapterInsight}>
      <span style={mutedLabel}>{label}</span>
      <strong style={tone}>{value}</strong>
    </div>
  );
}

function NarrativeSection({ month, improvement, weakness, nextFocus }: { month: PerformanceTimelineMonth; improvement: string; weakness: string; nextFocus: string }) {
  const summary = journeySummary(month, month.scoreChange == null);
  const theme = monthlyTheme(month);
  const emergingRisk = suppressDuplicate(month.narrative?.emergingRisk, [summary, improvement, weakness, nextFocus, theme]);
  const bestBehavior = suppressDuplicate(month.narrative?.bestBehavior, [summary, improvement, weakness, nextFocus, emergingRisk, theme]);
  const encouragement = suppressDuplicate(month.narrative?.encouragement, [summary, improvement, weakness, nextFocus, emergingRisk, bestBehavior, theme]);
  const evidence = uniqueEvidence(month.narrative?.supportingEvidence ?? [], [summary, improvement, weakness, nextFocus, theme]);

  return (
    <div style={narrativeGrid}>
      <div style={narrativePanel}>
        <span style={mutedLabel}>Coaching narrative</span>
        <p style={monthSummary}>{summary}</p>
      </div>
      <div style={narrativePanel}><span style={mutedLabel}>{month.scoreChange == null ? "Initial strength" : "Biggest improvement"}</span><p>{improvement}</p></div>
      <div style={narrativePanel}><span style={mutedLabel}>Primary weakness</span><p>{weakness}</p></div>
      {emergingRisk && <div style={narrativePanel}><span style={mutedLabel}>Emerging risk</span><p>{emergingRisk}</p></div>}
      {bestBehavior && <div style={narrativePanel}><span style={mutedLabel}>Best behavior</span><p>{bestBehavior}</p></div>}
      <div style={focusBox}><strong>Next action</strong><p>{nextFocus}</p></div>
      {evidence.length > 0 && (
        <div style={evidencePanel}>
          <span style={mutedLabel}>Supporting evidence</span>
          <ul style={evidenceList}>{evidence.slice(0, 4).map((item) => <li key={item}>{item}</li>)}</ul>
        </div>
      )}
      {month.narrative?.confidenceExplanation && (
        <div style={narrativePanel}><span style={mutedLabel}>Confidence explanation</span><p>{month.narrative.confidenceExplanation}</p></div>
      )}
    </div>
  );
}

function CategoryMiniScores({ scores }: { scores?: PerformanceCategoryScores | null }) {
  const insights = categoryInsights(scores, null);
  return (
    <div style={miniCategoryGrid}>
      {insights.map((insight) => (
        <div key={insight.key} style={miniCategoryCard}>
          <span style={mutedLabel}>{insight.label}</span>
          <strong>{formatScore(insight.score)}</strong>
        </div>
      ))}
    </div>
  );
}

function MonthMilestones({ milestones }: { milestones: PerformanceMilestone[] }) {
  return (
    <div style={monthMilestoneList}>
      {milestones.slice(0, 3).map((milestone) => (
        <span key={`${milestone.milestoneType}-${milestone.earnedDate}`} style={pill}>{milestone.title}</span>
      ))}
    </div>
  );
}

function Milestones({ data }: { data: PerformanceTimelineResponse }) {
  const [showAll, setShowAll] = useState(false);
  if (!data.milestones.length) return null;
  const groups = groupMilestones(data.milestones);
  const visible = showAll ? groups : groups.slice(0, 3);
  return (
    <section style={milestoneSection}>
      <div style={sectionHeader}>
        <div>
          <span style={eyebrow}>Milestones</span>
          <p style={muted}>Recent proof points from your trader-development journey.</p>
        </div>
        {groups.length > 3 && (
          <button type="button" style={secondaryButton} onClick={() => setShowAll((value) => !value)}>
            {showAll ? "Show fewer" : "View all milestones"}
          </button>
        )}
      </div>
      <div style={milestoneGrid}>
        {visible.map((group) => (
          <MilestoneCard key={group.key} group={group} />
        ))}
      </div>
    </section>
  );
}

function MilestoneCard({ group }: { group: MilestoneGroup }) {
  return (
    <div style={milestoneCard}>
      <div style={milestoneHeader}>
        <span style={milestoneIcon} aria-hidden="true">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 3l2.7 5.5 6.1.9-4.4 4.3 1 6.1L12 16.9 6.6 19.8l1-6.1-4.4-4.3 6.1-.9L12 3z" />
          </svg>
        </span>
        <strong>{group.title}</strong>
      </div>
      {group.alsoUnlocked.length > 0 && <span style={mutedLabel}>Also unlocked: {group.alsoUnlocked.join(", ")}</span>}
      <span style={mutedLabel}>Earned {formatDate(group.earnedDate)}</span>
      <p style={muted}>{group.evidence?.[0] ?? "Milestone earned from your scored trade history."}</p>
    </div>
  );
}

function Metric({ label, value, tone, supportingText }: { label: string; value: string; tone: CSSProperties; supportingText?: string }) {
  return (
    <div style={metricCard}>
      <span style={mutedLabel}>{label}</span>
      <strong style={{ ...metricValue, ...tone }}>{value}</strong>
      {supportingText && <span style={metricSupport}>{supportingText}</span>}
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div style={miniMetric}>
      <span style={mutedLabel}>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function headlineFor(data: PerformanceTimelineResponse) {
  if (!data.currentEvolutionScore) return "Your journey needs more trade history.";
  if (data.overallTrend === "IMPROVING") return "Your process is improving.";
  if (data.overallTrend === "DECLINING") return "Your process needs attention.";
  if (data.overallTrend === "STABLE") return "Your process is stabilizing.";
  return "Your trading journey is taking shape.";
}

function improvementFallback(category?: string | null) {
  if (!category) return "Your strongest process area will appear after an eligible scored month.";
  return `${category} is currently your strongest process signal. Protect what is working before adding complexity.`;
}

function uniqueEvidence(items: string[], duplicates: Array<string | null | undefined>) {
  const normalize = (value?: string | null) => cleanSentence(value).toLowerCase().replace(/[^\p{L}\p{N}\s]/gu, "").trim();
  const seen = new Set(duplicates.map(normalize).filter(Boolean));
  const result: string[] = [];
  for (const item of items) {
    const cleaned = cleanSentence(item);
    const normalized = normalize(cleaned);
    if (!cleaned || seen.has(normalized)) continue;
    seen.add(normalized);
    result.push(cleaned);
  }
  return result;
}

function statusLabel(status: string) {
  if (status === "READY") return "Full chapter";
  if (status === "LIMITED_DATA") return "Limited data";
  return "No activity";
}

function formatScore(score?: number | null) {
  return score == null ? "—" : `${score}/100`;
}

function formatChange(change?: number | null) {
  if (change == null) return "—";
  return `${change > 0 ? "+" : ""}${change}`;
}

function formatMoney(value?: number | null) {
  if (value == null) return "—";
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

function formatR(value?: number | null) {
  if (value == null) return "—";
  return `${value > 0 ? "+" : ""}${Number(value).toFixed(2)}R`;
}

function formatNumber(value?: number | null) {
  if (value == null) return "—";
  return Number(value).toFixed(2);
}

function formatDate(value?: string | null) {
  if (!value) return "just now";
  return new Date(value).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function confidencePill(confidence?: string | null): CSSProperties {
  if (confidence === "HIGH") return { ...pill, color: "#86efac", borderColor: "rgba(52,211,153,0.35)" };
  if (confidence === "MEDIUM") return { ...pill, color: "#bae6fd", borderColor: "rgba(56,189,248,0.32)" };
  return { ...pill, color: "#fde68a", borderColor: "rgba(245,158,11,0.32)" };
}

function errorMessage(error: any) {
  if (error?.response?.status === 402 || error?.response?.status === 429) {
    return error.response?.data?.message ?? "AI credit limit reached. Buy credits or wait until your plan resets.";
  }
  if (error?.response?.status === 403) {
    return error.response?.data?.message ?? "Upgrade to Pro to unlock AI Performance Timeline.";
  }
  return error?.response?.data?.message ?? "Unable to load AI Performance Timeline right now.";
}

function statusPill(status: string): CSSProperties {
  if (status === "READY") return { ...pill, color: "#6ee7b7", borderColor: "rgba(52,211,153,0.35)" };
  if (status === "LIMITED_DATA") return { ...pill, color: "#fde68a", borderColor: "rgba(245,158,11,0.35)", background: "rgba(245,158,11,0.1)" };
  return { ...pill, color: "#94a3b8", borderColor: "rgba(148,163,184,0.24)", background: "rgba(148,163,184,0.08)" };
}

function scoreTone(score?: number | null): CSSProperties {
  if ((score ?? 0) >= 75) return positiveTone;
  if ((score ?? 0) >= 55) return neutralTone;
  return warningTone;
}

function changePill(change?: number | null): CSSProperties {
  if (change == null) return { ...pill, padding: "4px 8px" };
  return { ...pill, padding: "4px 8px", color: change >= 0 ? "#86efac" : "#fca5a5", borderColor: change >= 0 ? "rgba(52,211,153,0.32)" : "rgba(248,113,113,0.32)" };
}

function categoryRating(score?: number | null): CSSProperties {
  if (score == null) return { ...pill, padding: "4px 8px" };
  if (score >= 80) return { ...pill, padding: "4px 8px", color: "#86efac", borderColor: "rgba(52,211,153,0.32)" };
  if (score >= 60) return { ...pill, padding: "4px 8px", color: "#fde68a", borderColor: "rgba(245,158,11,0.32)" };
  return { ...pill, padding: "4px 8px", color: "#fca5a5", borderColor: "rgba(248,113,113,0.32)" };
}

function roadmapStep(status: RoadmapStage["status"]): CSSProperties {
  const base: CSSProperties = {
    display: "grid",
    gridTemplateColumns: "32px minmax(0, 1fr)",
    gap: 10,
    alignItems: "start",
    padding: 12,
    borderRadius: 14,
    border: "1px solid rgba(148,163,184,0.12)",
    background: "rgba(2,6,23,0.42)",
  };
  if (status === "current") {
    return { ...base, borderColor: "rgba(52,211,153,0.36)", background: "rgba(34,197,94,0.09)" };
  }
  if (status === "complete") {
    return { ...base, borderColor: "rgba(45,212,191,0.18)" };
  }
  return { ...base, opacity: 0.76 };
}

function roadmapDot(status: RoadmapStage["status"]): CSSProperties {
  const base: CSSProperties = {
    width: 28,
    height: 28,
    display: "grid",
    placeItems: "center",
    borderRadius: 999,
    fontWeight: 900,
    border: "1px solid rgba(148,163,184,0.22)",
    color: "#94a3b8",
    background: "rgba(15,23,42,0.72)",
  };
  if (status === "current") return { ...base, color: "#86efac", borderColor: "rgba(52,211,153,0.36)", background: "rgba(34,197,94,0.12)" };
  if (status === "complete") return { ...base, color: "#67e8f9", borderColor: "rgba(45,212,191,0.28)", background: "rgba(20,184,166,0.1)" };
  return base;
}

function ScoreInfoModal({ onClose }: { onClose: () => void }) {
  return (
    <div style={modalBackdrop} role="presentation" onClick={onClose}>
      <div style={modalCard} role="dialog" aria-modal="true" aria-label="Evolution Score explanation" onClick={(event) => event.stopPropagation()}>
        <div style={sectionHeader}>
          <div>
            <span style={eyebrow}>Evolution Score</span>
            <h3 style={modalTitle}>How the score works</h3>
          </div>
          <button type="button" style={secondaryButton} onClick={onClose}>Close</button>
        </div>
        <p style={heroText}>The Evolution Score measures process quality and behavioral consistency. It does not predict future profitability.</p>
        <div style={weightGrid}>
          <MiniMetric label="Discipline" value="25%" />
          <MiniMetric label="Risk Management" value="25%" />
          <MiniMetric label="Execution" value="20%" />
          <MiniMetric label="Consistency" value="15%" />
          <MiniMetric label="Psychology" value="15%" />
        </div>
      </div>
    </div>
  );
}

const shell: CSSProperties = { display: "grid", gap: 22 };
const toolbar: CSSProperties = { display: "flex", alignItems: "end", gap: 14, flexWrap: "wrap", padding: 18, borderRadius: 18, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.72)" };
const controlGroup: CSSProperties = { display: "grid", gap: 8, minWidth: 240 };
const label: CSSProperties = { fontSize: 12, color: "#94a3b8", fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.05em" };
const select: CSSProperties = { padding: "12px 14px", borderRadius: 12, border: "1px solid rgba(148,163,184,0.26)", background: "rgba(2,6,23,0.78)", color: "#e5e7eb" };
const toolbarHelp: CSSProperties = { color: "#94a3b8", fontSize: 13, margin: "0 0 2px", maxWidth: 520 };
const notice: CSSProperties = { padding: 16, borderRadius: 14, background: "rgba(14,165,233,0.1)", border: "1px solid rgba(56,189,248,0.25)", color: "#bae6fd" };
const errorBox: CSSProperties = { padding: 16, borderRadius: 14, background: "rgba(239,68,68,0.1)", border: "1px solid rgba(248,113,113,0.28)", color: "#fecaca" };
const heroCard: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 300px), 1fr))", gap: 16, padding: 20, borderRadius: 24, border: "1px solid rgba(52,211,153,0.22)", background: "radial-gradient(circle at top right, rgba(34,197,94,0.18), transparent 38%), rgba(15,23,42,0.88)", boxShadow: "0 24px 80px rgba(0,0,0,0.32)" };
const heroMain: CSSProperties = { minWidth: 0 };
const heroScorePanel: CSSProperties = { display: "grid", alignContent: "start" };
const heroTopline: CSSProperties = { display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" };
const eyebrow: CSSProperties = { color: "#34d399", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.08em" };
const pill: CSSProperties = { padding: "6px 10px", borderRadius: 999, border: "1px solid rgba(148,163,184,0.2)", background: "rgba(15,23,42,0.72)", color: "#cbd5e1", fontSize: 12, fontWeight: 800 };
const heroTitle: CSSProperties = { margin: "12px 0 8px", fontSize: 32, lineHeight: 1.06, letterSpacing: "-0.03em" };
const heroText: CSSProperties = { color: "#cbd5e1", fontSize: 16, maxWidth: 740 };
const heroInsightGrid: CSSProperties = { gridColumn: "1 / -1", display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 10 };
const heroInsight: CSSProperties = { display: "grid", gap: 6, padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.12)" };
const heroMetricGrid: CSSProperties = { gridColumn: "1 / -1", display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: 10 };
const metricCard: CSSProperties = { minWidth: 0, padding: 13, borderRadius: 15, background: "rgba(2,6,23,0.55)", border: "1px solid rgba(148,163,184,0.14)" };
const mutedLabel: CSSProperties = { color: "#94a3b8", fontSize: 12, fontWeight: 800 };
const metricValue: CSSProperties = { display: "block", marginTop: 7, fontSize: 22, lineHeight: 1.1 };
const metricSupport: CSSProperties = { display: "block", marginTop: 6, color: "#94a3b8", fontSize: 12, lineHeight: 1.35 };
const positiveTone: CSSProperties = { color: "#86efac" };
const warningTone: CSSProperties = { color: "#fca5a5" };
const neutralTone: CSSProperties = { color: "#bae6fd" };
const scoreInfoButton: CSSProperties = { gridColumn: "1 / -1", justifySelf: "start", padding: "9px 11px", borderRadius: 12, border: "1px solid rgba(52,211,153,0.28)", background: "rgba(34,197,94,0.09)", color: "#86efac", fontWeight: 900, cursor: "pointer" };
const roadmapCard: CSSProperties = { display: "grid", gap: 16, padding: 20, borderRadius: 20, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.76)" };
const roadmapGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: 10 };
const roadmapCopy: CSSProperties = { display: "grid", gap: 5, minWidth: 0, color: "#e5e7eb" };
const nextMilestoneCard: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 260px), 1fr))", alignItems: "center", gap: 16, padding: 18, borderRadius: 20, border: "1px solid rgba(52,211,153,0.2)", background: "linear-gradient(135deg, rgba(34,197,94,0.12), rgba(14,165,233,0.08)), rgba(15,23,42,0.78)" };
const nextMilestoneTitle: CSSProperties = { margin: "8px 0 6px", fontSize: 22, lineHeight: 1.12 };
const nextMilestoneStats: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(120px, 1fr))", gap: 10 };
const trendCard: CSSProperties = { display: "grid", gap: 16, padding: 20, borderRadius: 20, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.78)" };
const inlineTrendNotice: CSSProperties = { padding: "10px 12px", borderRadius: 12, background: "rgba(14,165,233,0.07)", border: "1px solid rgba(56,189,248,0.16)", color: "#93c5fd", fontSize: 13 };
const chartBox: CSSProperties = { width: "100%", minWidth: 0, borderRadius: 16, padding: 10, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.12)" };
const tooltipStyle: CSSProperties = { background: "#020617", border: "1px solid rgba(148,163,184,0.24)", borderRadius: 12, color: "#e5e7eb" };
const categorySection: CSSProperties = { display: "grid", gap: 14 };
const categoryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 14 };
const categoryCard: CSSProperties = { padding: 16, borderRadius: 16, background: "rgba(15,23,42,0.82)", border: "1px solid rgba(148,163,184,0.14)" };
const categoryTopline: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 };
const categoryLabelRow: CSSProperties = { display: "flex", alignItems: "center", gap: 9, minWidth: 0 };
const categoryIconWrap: CSSProperties = { width: 30, height: 30, flex: "0 0 auto", display: "grid", placeItems: "center", borderRadius: 10, color: "#6ee7b7", background: "rgba(20,184,166,0.1)", border: "1px solid rgba(45,212,191,0.18)" };
const categoryScoreRow: CSSProperties = { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 };
const categoryValue: CSSProperties = { display: "block", fontSize: 24, margin: "8px 0" };
const categoryInterpretation: CSSProperties = { color: "#cbd5e1", fontSize: 13, lineHeight: 1.5, margin: "10px 0 0" };
const barTrack: CSSProperties = { height: 8, borderRadius: 999, background: "rgba(51,65,85,0.7)", overflow: "hidden" };
const barFill: CSSProperties = { height: "100%", borderRadius: 999, background: "linear-gradient(90deg, #22c55e, #14b8a6)" };
const timelineSection: CSSProperties = { display: "grid", gap: 16 };
const sectionHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 16, flexWrap: "wrap" };
const muted: CSSProperties = { color: "#94a3b8", margin: 0 };
const timelineList: CSSProperties = { display: "grid", gap: 16 };
const monthCard: CSSProperties = { position: "relative", padding: 20, paddingLeft: 28, borderRadius: 20, background: "rgba(15,23,42,0.86)", border: "1px solid rgba(148,163,184,0.16)", overflow: "hidden" };
const compactMonthCard: CSSProperties = { paddingTop: 14, paddingBottom: 14, background: "rgba(15,23,42,0.62)" };
const monthRail: CSSProperties = { position: "absolute", left: 0, top: 0, bottom: 0, width: 4, background: "linear-gradient(180deg, #22c55e, #14b8a6)" };
const monthHeader: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 18, flexWrap: "wrap" };
const monthTitle: CSSProperties = { margin: "10px 0", fontSize: 24 };
const monthTheme: CSSProperties = { margin: 0, color: "#cbd5e1" };
const scoreBadge: CSSProperties = { fontSize: 24, fontWeight: 900, color: "#e5e7eb" };
const monthSummary: CSSProperties = { color: "#cbd5e1", lineHeight: 1.6 };
const metricRow: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(120px, 1fr))", gap: 10, marginTop: 16 };
const chapterSummaryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(120px, 1fr))", gap: 10, marginTop: 12 };
const miniMetric: CSSProperties = { display: "grid", gap: 7, minWidth: 0, padding: 12, borderRadius: 12, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.12)" };
const chapterInsightGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 10, marginTop: 12 };
const chapterInsight: CSSProperties = { display: "grid", gap: 6, padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.12)" };
const focusBox: CSSProperties = { marginTop: 14, padding: 14, borderRadius: 14, background: "rgba(34,197,94,0.08)", border: "1px solid rgba(34,197,94,0.18)" };
const collapsedFocus: CSSProperties = { display: "grid", gap: 6, marginTop: 12, padding: 12, borderRadius: 14, background: "rgba(34,197,94,0.07)", border: "1px solid rgba(34,197,94,0.16)", color: "#cbd5e1" };
const secondaryObservationBox: CSSProperties = { marginTop: 12, padding: 12, borderRadius: 14, color: "#bae6fd", background: "rgba(14,165,233,0.08)", border: "1px solid rgba(56,189,248,0.18)" };
const limitedBox: CSSProperties = { marginTop: 14, padding: 14, borderRadius: 14, color: "#fde68a", background: "rgba(245,158,11,0.08)", border: "1px solid rgba(245,158,11,0.22)" };
const inactiveBox: CSSProperties = { marginTop: 14, padding: 14, borderRadius: 14, color: "#cbd5e1", background: "rgba(148,163,184,0.08)", border: "1px solid rgba(148,163,184,0.16)" };
const inactiveGroupCard: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16, flexWrap: "wrap", padding: 18, borderRadius: 18, background: "rgba(15,23,42,0.58)", border: "1px dashed rgba(148,163,184,0.26)" };
const inactiveTitle: CSSProperties = { margin: "8px 0 4px", fontSize: 20 };
const inactiveExpandedList: CSSProperties = { flexBasis: "100%", display: "grid", gap: 10, marginTop: 8 };
const secondaryButton: CSSProperties = { border: "1px solid rgba(148,163,184,0.22)", background: "rgba(15,23,42,0.72)", color: "#e5e7eb", borderRadius: 10, padding: "10px 12px", fontWeight: 800, cursor: "pointer" };
const detailsBlock: CSSProperties = { marginTop: 14 };
const detailsSummary: CSSProperties = { cursor: "pointer", color: "#86efac", fontWeight: 900, marginTop: 12, padding: "12px 14px", borderRadius: 14, background: "rgba(34,197,94,0.08)", border: "1px solid rgba(34,197,94,0.18)" };
const expandedChapter: CSSProperties = { display: "grid", gap: 14, marginTop: 14 };
const narrativeGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(230px, 1fr))", gap: 12 };
const narrativePanel: CSSProperties = { padding: 14, borderRadius: 14, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.12)" };
const evidencePanel: CSSProperties = { padding: 14, borderRadius: 14, background: "rgba(14,165,233,0.08)", border: "1px solid rgba(56,189,248,0.18)" };
const evidenceList: CSSProperties = { margin: "8px 0 0", paddingLeft: 18, color: "#cbd5e1", lineHeight: 1.6 };
const miniCategoryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 10 };
const miniCategoryCard: CSSProperties = { display: "grid", gap: 6, padding: 12, borderRadius: 12, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.12)" };
const monthMilestoneList: CSSProperties = { display: "flex", flexWrap: "wrap", gap: 8 };
const milestoneSection: CSSProperties = { display: "grid", gap: 14, padding: 20, borderRadius: 20, border: "1px solid rgba(148,163,184,0.16)", background: "rgba(15,23,42,0.78)" };
const milestoneGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 12 };
const milestoneCard: CSSProperties = { display: "grid", gap: 8, padding: 14, borderRadius: 14, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.12)" };
const milestoneHeader: CSSProperties = { display: "flex", alignItems: "center", gap: 10 };
const milestoneIcon: CSSProperties = { width: 32, height: 32, flex: "0 0 auto", display: "grid", placeItems: "center", borderRadius: 11, color: "#fde68a", background: "rgba(245,158,11,0.08)", border: "1px solid rgba(245,158,11,0.2)" };
const modalBackdrop: CSSProperties = { position: "fixed", inset: 0, zIndex: 50, display: "grid", placeItems: "center", padding: 18, background: "rgba(2,6,23,0.76)" };
const modalCard: CSSProperties = { width: "min(640px, 100%)", padding: 22, borderRadius: 20, background: "#0f172a", border: "1px solid rgba(52,211,153,0.22)", boxShadow: "0 24px 80px rgba(0,0,0,0.45)" };
const modalTitle: CSSProperties = { margin: "8px 0 0", fontSize: 24 };
const weightGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: 10, marginTop: 14 };
