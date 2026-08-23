import type {
  PerformanceCategoryScores,
  PerformanceMilestone,
  PerformanceTimelineMetrics,
  PerformanceTimelineMonth,
  ScoreConfidence,
} from "../../types/performanceTimelineTypes";

export type JourneyTimelineItem =
  | { type: "month"; month: PerformanceTimelineMonth }
  | { type: "inactive-range"; key: string; months: PerformanceTimelineMonth[]; startLabel: string; endLabel: string };

export type CategoryInsight = {
  key: keyof PerformanceCategoryScores;
  label: string;
  score?: number | null;
  change?: number | null;
  rating: string;
  interpretation: string;
};

export type MilestoneGroup = {
  key: string;
  title: string;
  earnedDate: string;
  alsoUnlocked: string[];
  evidence?: string[];
};

export type RoadmapStage = {
  title: string;
  status: "complete" | "current" | "upcoming";
  description: string;
};

export type NextDevelopmentMilestone = {
  title: string;
  currentValue: string;
  targetValue: string;
  action: string;
};

export const categoryLabels: Array<{ key: keyof PerformanceCategoryScores; label: string }> = [
  { key: "discipline", label: "Discipline" },
  { key: "riskManagement", label: "Risk Management" },
  { key: "execution", label: "Execution" },
  { key: "consistency", label: "Consistency" },
  { key: "psychology", label: "Psychology" },
];

export function groupInactiveMonths(months: PerformanceTimelineMonth[]): JourneyTimelineItem[] {
  const items: JourneyTimelineItem[] = [];
  let inactive: PerformanceTimelineMonth[] = [];

  const flushInactive = () => {
    if (!inactive.length) return;
    if (inactive.length === 1) {
      items.push({ type: "month", month: inactive[0] });
    } else {
      const first = inactive[0];
      const last = inactive[inactive.length - 1];
      items.push({
        type: "inactive-range",
        key: `${first.periodStart}-${last.periodEnd}`,
        months: inactive,
        startLabel: monthLabel(first),
        endLabel: monthLabel(last),
      });
    }
    inactive = [];
  };

  for (const month of months) {
    if (month.status === "NO_ACTIVITY") {
      inactive.push(month);
    } else {
      flushInactive();
      items.push({ type: "month", month });
    }
  }
  flushInactive();
  return items;
}

export function monthLabel(month: PerformanceTimelineMonth) {
  return `${month.month} ${month.year}`;
}

export function latestReady(months: PerformanceTimelineMonth[]) {
  return months.find((month) => month.status === "READY");
}

export function previousReady(months: PerformanceTimelineMonth[]) {
  return months.filter((month) => month.status === "READY")[1] ?? null;
}

export function isFirstScoredMonth(months: PerformanceTimelineMonth[]) {
  return months.filter((month) => month.status === "READY").length < 2;
}

export function categoryInsights(current?: PerformanceCategoryScores | null, previous?: PerformanceCategoryScores | null): CategoryInsight[] {
  return categoryLabels.map(({ key, label }) => {
    const score = current?.[key] ?? null;
    const previousScore = previous?.[key] ?? null;
    return {
      key,
      label,
      score,
      change: score == null || previousScore == null ? null : score - previousScore,
      rating: ratingFor(score),
      interpretation: interpretationFor(key, score),
    };
  });
}

export function ratingFor(score?: number | null) {
  if (score == null) return "Not Scored";
  if (score >= 90) return "Excellent";
  if (score >= 80) return "Strong";
  if (score >= 70) return "Developing Well";
  if (score >= 60) return "Needs Attention";
  return "Priority Area";
}

function interpretationFor(key: keyof PerformanceCategoryScores, score?: number | null) {
  if (score == null) return "Import enough trades this month to score this area with confidence.";
  const rating = ratingFor(score).toLowerCase();
  if (key === "discipline") return `Rule-following and trade selection are currently ${rating}.`;
  if (key === "riskManagement") return `Sizing consistency and loss containment are currently ${rating}.`;
  if (key === "execution") return `Entry quality and trade management are currently ${rating}.`;
  if (key === "consistency") return `Day-to-day and week-to-week stability is currently ${rating}.`;
  return `Emotion tagging and trade-state awareness are currently ${rating}.`;
}

export function developmentStage(scores?: PerformanceCategoryScores | null, confidence?: ScoreConfidence | null) {
  if (!scores) return "Establishing Foundations";
  const weakest = weakestCategoryDetail(scores);
  if (!weakest) return "Establishing Foundations";
  if (allCategoryScoresAtLeast(scores, 80)) return "Protecting a Strong Process";
  if (weakest.score >= 65 && confidence === "HIGH") return "Protecting a Strong Process";
  if (weakest.key === "psychology") return "Strengthening Psychology";
  if (weakest.key === "execution") return "Improving Execution";
  if (weakest.key === "discipline") return "Building Discipline";
  if (weakest.key === "riskManagement") return "Rebuilding Risk Control";
  return "Building Consistency";
}

export function deterministicNextFocus(weakestCategory?: string | null, metrics?: PerformanceTimelineMetrics | null) {
  const category = (weakestCategory ?? "").toLowerCase();
  if (category.includes("psychology")) {
    if ((metrics?.metadataCompletionPercent ?? 0) < 80 || (metrics?.emotionalTradeCount ?? 0) === 0) {
      return "Complete emotion and confidence tags for every trade next month so psychological patterns can be measured reliably.";
    }
    return "Review emotionally tagged trades and define one mandatory pause rule after a losing trade.";
  }
  if (category.includes("execution")) {
    return "Review your lowest-execution trades and define one entry or trade-management rule to standardize next month.";
  }
  if (category.includes("discipline")) {
    return "Review every trade taken after a stopping condition and define one daily rule that prevents repeat violations.";
  }
  if (category.includes("risk")) {
    return "Review trades that differed from the configured 1R amount and standardize risk sizing before the next session.";
  }
  if (category.includes("consistency")) {
    return "Identify the day, session, or setup responsible for the largest variation in results and reduce exposure to that condition.";
  }
  return "Pick one process rule to review after every session next month.";
}

export function shortNextFocus(weakestCategory?: string | null, metrics?: PerformanceTimelineMetrics | null) {
  const category = (weakestCategory ?? "").toLowerCase();
  if (category.includes("psychology")) {
    if ((metrics?.metadataCompletionPercent ?? 0) < 80 || (metrics?.emotionalTradeCount ?? 0) === 0) {
      return "Record emotion and confidence after every trade this month.";
    }
    return "Define one pause rule after a losing trade.";
  }
  if (category.includes("execution")) return "Standardize one entry or trade-management rule next month.";
  if (category.includes("discipline")) return "Define one daily rule that prevents repeat violations.";
  if (category.includes("risk")) return "Standardize risk sizing before the next session.";
  if (category.includes("consistency")) return "Reduce exposure to your most variable day, session, or setup.";
  return "Review one process rule after every session.";
}

export function usefulNextFocus(
  aiFocus?: string | null,
  weakestCategory?: string | null,
  metrics?: PerformanceTimelineMetrics | null,
  duplicates: Array<string | null | undefined> = []
) {
  const focus = cleanSentence(aiFocus);
  const duplicateSet = normalizedSet(duplicates);
  if (focus && focus.length > 40 && !duplicateSet.has(normalizeCopy(focus)) && !isGenericFocus(focus)) {
    return focus;
  }
  return deterministicNextFocus(weakestCategory, metrics);
}

export function suppressDuplicate(value?: string | null, duplicates: Array<string | null | undefined> = []) {
  const cleaned = cleanSentence(value);
  if (!cleaned) return null;
  const duplicateSet = normalizedSet(duplicates);
  return duplicateSet.has(normalizeCopy(cleaned)) ? null : cleaned;
}

export function cleanSentence(value?: string | null) {
  return (value ?? "").replace(/\s+/g, " ").trim();
}

export function normalizeCopy(value?: string | null) {
  return cleanSentence(value)
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, "")
    .replace(/\s+/g, " ")
    .trim();
}

function normalizedSet(values: Array<string | null | undefined>) {
  return new Set(values.map(normalizeCopy).filter(Boolean));
}

function isGenericFocus(value: string) {
  const lower = value.toLowerCase();
  return lower.includes("keep improving")
    || lower.includes("continue reviewing")
    || lower.includes("stay consistent")
    || lower.includes("protect the behaviors")
    || lower.includes("positive expectancy");
}

export function categoryFromScores(scores?: PerformanceCategoryScores | null, strongest = true) {
  const detail = strongest ? strongestCategoryDetail(scores) : weakestCategoryDetail(scores);
  return detail?.label ?? null;
}

export function weakestCategoryDetail(scores?: PerformanceCategoryScores | null) {
  return categoryDetails(scores).sort((a, b) => a.score - b.score)[0] ?? null;
}

export function strongestCategoryDetail(scores?: PerformanceCategoryScores | null) {
  return categoryDetails(scores).sort((a, b) => b.score - a.score)[0] ?? null;
}

function categoryDetails(scores?: PerformanceCategoryScores | null) {
  if (!scores) return [];
  return categoryLabels
    .map(({ key, label }) => ({ key, label, score: scores[key] }))
    .filter((item): item is { key: keyof PerformanceCategoryScores; label: string; score: number } => item.score != null);
}

function allCategoryScoresAtLeast(scores: PerformanceCategoryScores, threshold: number) {
  const details = categoryDetails(scores);
  return details.length > 0 && details.every((item) => item.score >= threshold);
}

export function initialStrength(scores?: PerformanceCategoryScores | null) {
  const details = categoryDetails(scores).filter((item) => item.score >= 80).sort((a, b) => b.score - a.score);
  if (details.length >= 2) return `${details[0].label} and ${details[1].label} established a strong starting baseline.`;
  if (details.length === 1) return `${details[0].label} established the clearest starting strength.`;
  return "This month creates the starting benchmark for future comparison.";
}

export function primaryWeakness(scores?: PerformanceCategoryScores | null, metrics?: PerformanceTimelineMetrics | null) {
  const weakest = weakestCategoryDetail(scores);
  if (!weakest) return "The primary development area will appear after an eligible scored month.";
  if (weakest.score >= 90) {
    return `${weakest.label} is the lowest relative category, but all categories are currently strong. Treat this as a metric to monitor.`;
  }
  if (weakest.key === "psychology") {
    if ((metrics?.metadataCompletionPercent ?? 0) < 80 || (metrics?.emotionalTradeCount ?? 0) === 0) {
      return `Psychology is the main development area at ${weakest.score}/100. Incomplete emotion and confidence notes make recurring patterns harder to identify.`;
    }
    return `Psychology is the main development area at ${weakest.score}/100. Review emotional control and trade-state awareness.`;
  }
  if (weakest.key === "execution") return `Execution is the main development area at ${weakest.score}/100. Review entry quality and trade management.`;
  if (weakest.key === "discipline") return `Discipline is the main development area at ${weakest.score}/100. Review rule adherence and stopping conditions.`;
  if (weakest.key === "riskManagement") return `Risk management is the main development area at ${weakest.score}/100. Review sizing consistency and loss containment.`;
  return `Consistency is the main development area at ${weakest.score}/100. Review variation by day, session, and setup.`;
}

export function secondaryObservation(
  scores?: PerformanceCategoryScores | null,
  narrativeLeak?: string | null,
  metrics?: PerformanceTimelineMetrics | null
) {
  const leak = cleanSentence(narrativeLeak);
  if (!leak || !scores) return null;
  const strongest = strongestCategoryDetail(scores);
  if (!strongest || strongest.score < 90) return null;
  const lower = leak.toLowerCase();
  if (strongest.key === "riskManagement" && metrics?.largestDailyLossR != null && (lower.includes("risk") || lower.includes("loss"))) {
    return `Metric to monitor: largest daily loss remains the risk metric to keep watching.`;
  }
  return null;
}

export function scoreDescriptor(score?: number | null) {
  if (score == null) return "Not enough data yet";
  if (score >= 90) return "Exceptional Process";
  if (score >= 80) return "Strong Foundation";
  if (score >= 70) return "Developing Consistency";
  if (score >= 60) return "Building Core Habits";
  if (score >= 50) return "Priority Areas Identified";
  return "Rebuilding the Process";
}

export function journeySummary(month?: PerformanceTimelineMonth | null, firstScored = false) {
  if (!month) return "Import more trades to build a clearer performance journey.";
  const aiSummary = cleanSentence(month.narrative?.summary);
  if (aiSummary && !isGenericSummary(aiSummary)) return aiSummary;

  const strongest = categoryFromScores(month.categoryScores, true) ?? "your strongest category";
  const weakest = categoryFromScores(month.categoryScores, false) ?? "your main development area";
  if (firstScored) {
    return `${month.month} established your first measurable performance baseline across ${month.tradeCount} trades. ${strongest} led the process, while ${weakest} is the clearest development area.`;
  }
  const totalR = month.metrics?.totalR == null ? null : `${month.metrics.totalR > 0 ? "+" : ""}${Number(month.metrics.totalR).toFixed(2)}R`;
  const winRate = month.metrics?.winRate == null ? null : `${month.metrics.winRate}% win rate`;
  const metricParts = [totalR, winRate].filter(Boolean).join(" and ");
  return `${month.month} reviewed ${month.tradeCount} trades${metricParts ? ` with ${metricParts}` : ""}. ${strongest} led the process, while ${weakest} is the next area to tighten.`;
}

function isGenericSummary(summary: string) {
  const lower = summary.toLowerCase();
  return lower.includes("evolving trading process")
    || lower.includes("continue to focus")
    || lower.includes("shows areas of strength and weakness")
    || lower.includes("performance this month");
}

export function developmentRoadmap(months: PerformanceTimelineMonth[]): RoadmapStage[] {
  const current = latestReady(months);
  const scores = current?.categoryScores;
  const currentStage = developmentStage(scores, current?.scoreConfidence);
  const stages: Array<{ title: string; description: string; complete: boolean }> = [
    { title: "Establishing Foundations", description: "Build enough trade history for a reliable baseline.", complete: Boolean(current) },
    { title: "Building Discipline", description: "Tighten rule adherence and stopping conditions.", complete: (scores?.discipline ?? 0) >= 65 },
    { title: "Rebuilding Risk Control", description: "Keep sizing and loss containment consistent.", complete: (scores?.riskManagement ?? 0) >= 70 },
    { title: "Strengthening Psychology", description: "Record emotion and confidence so patterns become visible.", complete: (scores?.psychology ?? 0) >= 65 },
    { title: "Improving Execution", description: "Standardize entry quality and trade management.", complete: (scores?.execution ?? 0) >= 70 },
    { title: "Building Consistency", description: "Reduce variation across days, sessions, and setups.", complete: (scores?.consistency ?? 0) >= 75 },
    { title: "Protecting a Strong Process", description: "Maintain a balanced process without adding complexity.", complete: scores ? allCategoryScoresAtLeast(scores, 80) : false },
  ];

  return stages.map((stage) => ({
    title: stage.title,
    description: stage.description,
    status: stage.title === currentStage ? "current" : stage.complete ? "complete" : "upcoming",
  }));
}

export function nextDevelopmentMilestone(months: PerformanceTimelineMonth[]): NextDevelopmentMilestone | null {
  const current = latestReady(months);
  if (!current) return null;
  const scores = current.categoryScores;
  const weakest = weakestCategoryDetail(scores);
  if (!scores || !weakest) return null;

  const metadata = current.metrics?.metadataCompletionPercent ?? 0;
  if (weakest.key === "psychology" && metadata < 90) {
    return {
      title: "Complete psychology metadata on 90% of trades",
      currentValue: `${Math.round(metadata)}%`,
      targetValue: "90%",
      action: "Record emotion and confidence after each trade.",
    };
  }
  if (weakest.key === "psychology" && weakest.score < 65) {
    return thresholdMilestone("Psychology score above 65", weakest.score, 65, shortNextFocus(weakest.label, current.metrics));
  }
  if (weakest.key === "execution" && weakest.score < 70) {
    return thresholdMilestone("Execution score above 70", weakest.score, 70, shortNextFocus(weakest.label, current.metrics));
  }
  if (weakest.key === "discipline" && weakest.score < 65) {
    return thresholdMilestone("Discipline score above 65", weakest.score, 65, shortNextFocus(weakest.label, current.metrics));
  }
  if (weakest.key === "riskManagement" && weakest.score < 70) {
    return thresholdMilestone("Risk management score above 70", weakest.score, 70, shortNextFocus(weakest.label, current.metrics));
  }
  if (weakest.key === "consistency" && weakest.score < 75) {
    return thresholdMilestone("Consistency score above 75", weakest.score, 75, shortNextFocus(weakest.label, current.metrics));
  }
  if (isFirstScoredMonth(months)) {
    return {
      title: "Complete a second eligible month",
      currentValue: "1 month",
      targetValue: "2 months",
      action: "Import the next eligible month to unlock trend comparison.",
    };
  }
  return null;
}

function thresholdMilestone(title: string, current: number, target: number, action: string): NextDevelopmentMilestone {
  return {
    title,
    currentValue: `${current}/100`,
    targetValue: `${target}/100`,
    action,
  };
}

export function monthlyTheme(month: PerformanceTimelineMonth, previous?: PerformanceTimelineMonth | null) {
  if (month.status !== "READY") return month.narrative?.theme ?? month.message ?? "Limited Monthly Signal";
  const scores = month.categoryScores;
  if (!scores) return "Process Development Chapter";
  const discipline = scores.discipline ?? 0;
  const risk = scores.riskManagement ?? 0;
  const execution = scores.execution ?? 0;
  const consistency = scores.consistency ?? 0;
  const psychology = scores.psychology ?? 0;
  if (discipline >= 80 && risk >= 80 && psychology < 65) return "Strong Foundations, Psychology Next";
  if (risk >= 80 && execution < 65) return "Risk Discipline Established, Execution in Focus";
  if (previous?.categoryScores?.discipline != null && discipline - previous.categoryScores.discipline >= 5) return "Building Better Trading Discipline";
  if (consistency < 65) return "Stabilizing Results Across Sessions";
  if (risk < 65) return "Rebuilding Risk Control";
  if (execution < 65) return "Improving Setup and Trade Execution";
  if (psychology < 65) return "Strengthening Emotional Control";
  if (allCategoryScoresAtLeast(scores, 80)) return "Protecting a Strong Process";
  return month.narrative?.theme && !isGenericTheme(month.narrative.theme) ? month.narrative.theme : "Process Development Chapter";
}

function isGenericTheme(theme: string) {
  const lower = theme.toLowerCase();
  return lower.includes("process is holding up") || lower.includes("process pressure") || lower.includes("process development");
}

export function groupMilestones(milestones: PerformanceMilestone[]): MilestoneGroup[] {
  const groups: MilestoneGroup[] = [];
  const scoreByDate = new Map<string, PerformanceMilestone[]>();
  const nonScore: PerformanceMilestone[] = [];

  for (const milestone of milestones) {
    if (isScoreThresholdMilestone(milestone)) {
      const list = scoreByDate.get(milestone.earnedDate) ?? [];
      list.push(milestone);
      scoreByDate.set(milestone.earnedDate, list);
    } else {
      nonScore.push(milestone);
    }
  }

  for (const [earnedDate, scoreMilestones] of scoreByDate.entries()) {
    const sorted = scoreMilestones.sort((a, b) => scoreThreshold(b) - scoreThreshold(a));
    const primary = sorted[0];
    groups.push({
      key: `score-${earnedDate}`,
      title: primary.title,
      earnedDate,
      alsoUnlocked: sorted.slice(1).map((milestone) => milestone.title.replace(/^Evolution Score Above\s*/i, "").trim()).filter(Boolean),
      evidence: primary.evidence,
    });
  }

  for (const milestone of nonScore) {
    groups.push({
      key: `${milestone.milestoneType}-${milestone.earnedDate}`,
      title: milestone.title,
      earnedDate: milestone.earnedDate,
      alsoUnlocked: [],
      evidence: milestone.evidence,
    });
  }

  return groups.sort((a, b) => new Date(b.earnedDate).getTime() - new Date(a.earnedDate).getTime());
}

function isScoreThresholdMilestone(milestone: PerformanceMilestone) {
  return milestone.milestoneType.includes("EVOLUTION_SCORE") || milestone.title.toLowerCase().includes("evolution score above");
}

function scoreThreshold(milestone: PerformanceMilestone) {
  const match = milestone.title.match(/(\d+)/);
  return match ? Number(match[1]) : 0;
}
