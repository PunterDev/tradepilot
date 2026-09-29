import { useEffect } from "react";
import { Link } from "react-router-dom";
import Seo from "../../components/Seo";
import { pageStructuredData, type BreadcrumbItem } from "../../seo/structuredData";
import { activationAwareCtaDestination, activationSourceForPublicPath } from "../../utils/activationRedirect";
import { trackSeoCtaClick, trackSeoLandingPageView } from "../../utils/seoAttribution";
import "./HomePage.css";
import { MarketingShell } from "./MarketingLayout";
import { Breadcrumbs } from "./Breadcrumbs";

type Faq = {
  question: string;
  answer: string;
};

type LandingPageContent = {
  path: string;
  keyword: string;
  title: string;
  description: string;
  h1: string;
  eyebrow: string;
  intro: string;
  cta: string;
  secondaryCta?: string;
  proof: string[];
  workflow?: string[];
  sections: {
    eyebrow: string;
    title: string;
    copy: string;
    bullets: string[];
  }[];
  comparison?: {
    title: string;
    rows: [string, string, string][];
  };
  links: {
    label: string;
    to: string;
  }[];
  faq: Faq[];
  disclaimer?: string;
};

export const seoLandingPages: Record<string, LandingPageContent> = {
  "/futures-trading-journal": {
    path: "/futures-trading-journal",
    keyword: "futures trading journal",
    title: "Futures Trading Journal With AI Coaching | TradePilot Coach",
    description:
      "Journal NQ, MNQ, ES and MES futures trades, uncover costly trading patterns, review sessions and get AI-powered coaching with TradePilot Coach.",
    h1: "A Trading Journal Built for Futures Traders",
    eyebrow: "Futures trading journal",
    intro:
      "TradePilot Coach helps futures traders import completed trade history, measure execution, and review the behaviors that keep showing up after the closing bell.",
    cta: "Analyze My Futures Trades",
    secondaryCta: "Compare Imports",
    proof: ["Topstep and Tradovate CSV import", "R-multiple analytics", "Session review", "AI coaching"],
    workflow: ["Import CSV", "Review trades", "Detect leaks", "Coach next session"],
    sections: [
      {
        eyebrow: "Futures context",
        title: "Generic journals can miss the behaviors futures traders care about.",
        copy:
          "NQ, MNQ, ES and MES traders need more than a blank note field. TradePilot Coach organizes contract, direction, size, session, P&L, and 1R context so the review starts from the market you actually traded.",
        bullets: ["Track futures symbols and normalized contracts", "Review long and short execution separately", "Connect trade outcomes to time-of-day behavior"],
      },
      {
        eyebrow: "Behavioral review",
        title: "Turn trade history into a repeatable coaching loop.",
        copy:
          "After import, TradePilot Coach helps you inspect performance patterns, session quality, overtrading, revenge trading, and sizing mistakes using your own completed trades.",
        bullets: ["R-multiple analytics when account risk is configured", "Session analysis for market-time behavior", "AI reviews and Daily or Weekly Coach where available"],
      },
      {
        eyebrow: "Pricing",
        title: "Start free, upgrade when the deeper review matters.",
        copy:
          "The Free plan includes core journaling, CSV trade import, dashboard, calendar, analytics, and monthly AI credits. Pro adds deeper coaching and leak-detection workflows for $29.99 per month.",
        bullets: ["Free plan for getting organized", "Pro option for serious review cycles", "No signals, no trade calls, no promises of profit"],
      },
    ],
    comparison: {
      title: "What TradePilot Coach Helps Futures Traders Review",
      rows: [
        ["Import", "Manual entry or spreadsheets", "Topstep and Tradovate CSV workflows"],
        ["Risk", "Raw dollars only", "R-multiple analytics when 1R is configured"],
        ["Timing", "Hard to compare sessions", "Session analysis by market-time context"],
      ],
    },
    links: [
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Tradovate trading journal", to: "/tradovate-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
    ],
    faq: [
      {
        question: "Can I journal NQ, MNQ, ES and MES trades?",
        answer:
          "TradePilot Coach supports futures trade records and uses futures point values for supported import workflows. Unsupported contracts are rejected with validation rather than forced into bad analytics.",
      },
      {
        question: "Does TradePilot Coach predict futures trades?",
        answer:
          "No. TradePilot Coach analyzes trades that already happened. It does not provide market predictions, trade signals, or financial advice.",
      },
    ],
  },
  "/topstep-trading-journal": {
    path: "/topstep-trading-journal",
    keyword: "Topstep trading journal",
    title: "Topstep Trading Journal & AI Trade Analysis | TradePilot Coach",
    description:
      "Import your Topstep trade history into TradePilot Coach to analyze performance, trading behavior, sessions and recurring mistakes.",
    h1: "AI Trading Journal for Topstep Traders",
    eyebrow: "Topstep CSV workflow",
    intro:
      "TradePilot Coach imports supported Topstep orders_export.csv history so futures traders can review completed trades, sessions, and repeated execution mistakes in one private journal.",
    cta: "Import My Topstep History",
    secondaryCta: "See Futures Journal",
    proof: ["orders_export.csv support", "One source AccountName per file", "Duplicate-safe keys", "AI coaching"],
    workflow: ["Export", "Upload", "Review", "Discover Patterns"],
    sections: [
      {
        eyebrow: "Actual import support",
        title: "Built around Topstep order-history exports, not vague copy.",
        copy:
          "TradePilot Coach currently supports the Topstep orders_export.csv shape. Filled opening and closing executions are reconstructed into completed futures trades; cancelled rows and unfinished positions are ignored or reported during preview.",
        bullets: ["Requires the supported Topstep order-history headers", "Rejects multiple AccountName values in one file for MVP safety", "Uses Topstep-prefixed duplicate keys"],
      },
      {
        eyebrow: "Prop-firm behavior",
        title: "Review the mistakes that tend to repeat in evaluations.",
        copy:
          "Use TradePilot Coach to study whether losses cluster after a bad start, whether size changes under stress, and whether your trading worsens in specific sessions.",
        bullets: ["Futures performance review", "Behavioral pattern analysis", "Session Review and AI coaching where available"],
      },
      {
        eyebrow: "Workflow",
        title: "Export, upload, review, and discover patterns.",
        copy:
          "After account setup, upload the supported CSV, preview what TradePilot Coach found, save valid trades, then use dashboard and AI review tools to inspect your data.",
        bullets: ["CSV import into your selected trading account", "Preview warnings for unsupported rows", "Completed trades feed calendar, analytics, and coaching"],
      },
    ],
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "How to export Topstep trades", to: "/guides/how-to-export-topstep-trades" },
    ],
    faq: [
      {
        question: "Which Topstep file does TradePilot Coach support?",
        answer:
          "TradePilot Coach supports the Topstep orders_export.csv order-history export with required headers such as Id, AccountName, ContractName, Status, Size, Side, FilledAt, ExecutePrice, PositionDisposition, and CreationDisposition.",
      },
      {
        question: "Can one Topstep file include multiple source accounts?",
        answer:
          "Not currently. For MVP safety, TradePilot Coach rejects Topstep files that contain more than one AccountName value.",
      },
    ],
    disclaimer: "TradePilot Coach is an independent product and is not affiliated with or endorsed by Topstep.",
  },
  "/tradovate-trading-journal": {
    path: "/tradovate-trading-journal",
    keyword: "Tradovate trading journal",
    title: "Tradovate Trading Journal With AI Insights | TradePilot Coach",
    description:
      "Import Tradovate futures trades into TradePilot Coach and uncover patterns in your performance, risk, sessions and trading behavior.",
    h1: "Turn Your Tradovate Trades Into Actionable Insights",
    eyebrow: "Tradovate CSV journal",
    intro:
      "TradePilot Coach turns supported Tradovate futures CSV history into a private review workspace for performance, risk, session behavior, and AI-powered trade analysis.",
    cta: "Analyze My Tradovate Trades",
    secondaryCta: "Explore AI Journal",
    proof: ["Tradovate-compatible CSV", "Session classification", "Futures P&L checks", "Free and Pro plans"],
    workflow: ["Export CSV", "Upload", "Save trades", "Review patterns"],
    sections: [
      {
        eyebrow: "Why journal Tradovate trades",
        title: "Your platform records fills. Your journal should explain behavior.",
        copy:
          "TradePilot Coach imports supported Tradovate trade CSVs, normalizes futures contracts where possible, and organizes entries, exits, direction, quantity, P&L, and session context for review.",
        bullets: ["Supports TradePilot-compatible Tradovate trade CSV fields", "Uses explicit side fields where present", "Falls back to buy/sell timestamp order when needed"],
      },
      {
        eyebrow: "Import workflow",
        title: "Move from raw export to reviewable trades.",
        copy:
          "Upload a supported Tradovate CSV after selecting a trading account. TradePilot Coach validates rows, reconciles supported futures P&L within tolerance, and saves valid trades into the dashboard.",
        bullets: ["Duplicate protection uses deterministic external trade keys", "Offset-less timestamps are treated as New York market-local time", "Calendar, analytics, Session Review, and coaching use the imported records"],
      },
      {
        eyebrow: "AI review",
        title: "Find trading leaks without turning AI into a signal service.",
        copy:
          "TradePilot Coach analyzes completed Tradovate trades for behavioral patterns such as overtrading, weak sessions, sizing mistakes, and rule-breaking themes.",
        bullets: ["AI Trade Review for selected trades where available", "AI Leak Detector and coaching on eligible plans", "No predictions or trade recommendations"],
      },
    ],
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading psychology", to: "/trading-psychology" },
      { label: "How to export Tradovate trades", to: "/guides/how-to-export-tradovate-trades" },
    ],
    faq: [
      {
        question: "Does TradePilot Coach connect directly to Tradovate?",
        answer:
          "No direct API connection is claimed here. TradePilot Coach supports CSV import for compatible Tradovate trade exports.",
      },
      {
        question: "How are Tradovate sessions classified?",
        answer:
          "Tradovate CSV timestamps are treated as America/New_York market-local timestamps for session classification.",
      },
    ],
    disclaimer: "TradePilot Coach is an independent product and is not affiliated with or endorsed by Tradovate.",
  },
  "/prop-firm-trading-journal": {
    path: "/prop-firm-trading-journal",
    keyword: "prop firm trading journal",
    title: "Prop Firm Trading Journal for Futures Traders | TradePilot Coach",
    description:
      "Analyze prop-firm trading performance, rule-breaking patterns, overtrading and costly behavior with TradePilot Coach's AI-powered trading journal.",
    h1: "A Trading Journal Built for Prop Firm Traders",
    eyebrow: "Prop firm trading journal",
    intro:
      "TradePilot Coach helps futures prop-firm traders study discipline, session deterioration, and repeated evaluation mistakes using completed trades instead of promises or signals.",
    cta: "Find My Biggest Trading Leak",
    secondaryCta: "See Topstep Workflow",
    proof: ["No signals", "No trade calls", "No profit promises", "Behavioral review"],
    workflow: ["Import trades", "Measure behavior", "Review leaks", "Set a guardrail"],
    sections: [
      {
        eyebrow: "Evaluation pressure",
        title: "Passing rules is not the same as keeping discipline.",
        copy:
          "Prop-firm traders often know the rules and still break process after consecutive losses, a green start, or a frustrating session. TradePilot Coach helps identify whether the recurring problem is strategy, timing, sizing, or behavior.",
        bullets: ["Continuing after consecutive losses", "Overtrading after a poor start", "Oversizing when trying to recover"],
      },
      {
        eyebrow: "Behavioral leaks",
        title: "See how costly habits show up across completed trades.",
        copy:
          "Use TradePilot Coach to review repeated evaluation mistakes: weak session selection, broken rules, inconsistent execution, and deterioration late in a trading day.",
        bullets: ["Session deterioration analysis", "Rule-breaking and mistake themes", "R-multiple context when configured"],
      },
      {
        eyebrow: "Clear boundaries",
        title: "A coach for review, not a shortcut around risk.",
        copy:
          "TradePilot Coach does not tell you what to trade next and does not imply that any trader will pass a challenge or become profitable.",
        bullets: ["No signals", "No trade calls", "No promises of profit"],
      },
    ],
    links: [
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
    ],
    faq: [
      {
        question: "Will TradePilot Coach help me pass a prop challenge?",
        answer:
          "TradePilot Coach can help you review your own completed trades and behavior. It does not guarantee passing an evaluation or improving trading outcomes.",
      },
      {
        question: "Is TradePilot Coach only for Topstep traders?",
        answer:
          "No. TradePilot Coach is built for futures and prop-firm traders broadly, with current CSV import support for compatible Tradovate and Topstep exports.",
      },
    ],
  },
  "/ai-trading-journal": {
    path: "/ai-trading-journal",
    keyword: "AI trading journal",
    title: "AI Trading Journal for Futures Traders | TradePilot Coach",
    description:
      "TradePilot Coach uses AI to analyze completed trades, identify behavioral patterns and help futures traders review what is actually hurting their performance.",
    h1: "AI That Analyzes Your Trading - Not Predicts It",
    eyebrow: "AI trading journal",
    intro:
      "TradePilot Coach uses AI as a review layer for completed trades. It studies your actual history, highlights repeated behaviors, and helps you decide what to review before the next session.",
    cta: "Analyze My Trading Free",
    secondaryCta: "See Futures Journal",
    proof: ["Completed-trade analysis", "Behavioral patterns", "Session insights", "Weekly coaching"],
    workflow: ["Import history", "Add context", "Generate review", "Apply one rule"],
    sections: [
      {
        eyebrow: "Positioning",
        title: "AI signals predict. TradePilot Coach reviews.",
        copy:
          "TradePilot Coach does not forecast market direction, issue trade calls, or recommend entries. It analyzes trades you already took so you can understand what behavior is hurting your process.",
        bullets: ["Reviews historical execution", "Uses your own trade records and metadata", "Keeps recommendations focused on review and process"],
      },
      {
        eyebrow: "What AI reviews",
        title: "From individual trade decisions to weekly patterns.",
        copy:
          "Depending on your plan and enabled features, TradePilot Coach can support AI Trade Review, Session Review, Daily Coach, Weekly Coach, and AI Leak Detector workflows.",
        bullets: ["Trade-level decisions and context", "Session-level strengths and mistakes", "Recurring behavior across your history"],
      },
      {
        eyebrow: "Guardrails",
        title: "Useful coaching needs honest limitations.",
        copy:
          "TradePilot Coach uses limited-data states and cached results where appropriate. AI usage is subject to monthly credit allowances, and all trading remains risky.",
        bullets: ["Free plan includes monthly AI credits", "Pro includes 100 monthly AI credits", "No financial advice or market predictions"],
      },
    ],
    comparison: {
      title: "AI Signals vs. TradePilot Coach",
      rows: [
        ["Goal", "Tell you what to trade next", "Explain what happened in trades you took"],
        ["Data", "Market prediction inputs", "Your completed trade history and notes"],
        ["Output", "Calls and forecasts", "Review, patterns, and next-session guardrails"],
      ],
    },
    links: [
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Topstep trading journal", to: "/topstep-trading-journal" },
      { label: "Tradovate trading journal", to: "/tradovate-trading-journal" },
      { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
      { label: "Trading psychology", to: "/trading-psychology" },
      { label: "How to review a losing trading session", to: "/guides/how-to-review-a-losing-trading-session" },
    ],
    faq: [
      {
        question: "Does an AI trading journal give signals?",
        answer:
          "TradePilot Coach does not. It uses AI to analyze completed trades, session behavior, and repeated patterns from your own trading history.",
      },
      {
        question: "What can I do after the first insight?",
        answer:
          "A typical path is signup, CSV import, first personalized insight, then deeper Pro review workflows when the trader wants more coaching depth.",
      },
    ],
  },
  "/trading-psychology": {
    path: "/trading-psychology",
    keyword: "trading psychology",
    title: "Trading Psychology You Can Actually Measure | TradePilot Coach",
    description:
      "Learn how futures traders can measure revenge trading, overtrading, rule breaking, discipline issues and post-loss behavior using completed trade history.",
    h1: "Trading Psychology You Can Actually Measure",
    eyebrow: "Behavioral trading analysis",
    intro:
      "Trading psychology is easier to improve when it becomes visible in your data. TradePilot Coach helps traders review completed trades for repeated behavior, not vague motivation.",
    cta: "Measure My Trading Behavior",
    secondaryCta: "Read About Revenge Trading",
    proof: ["Revenge trading", "Overtrading", "Rule breaking", "Post-loss behavior"],
    workflow: ["Identify trigger", "Segment trades", "Measure cost", "Adjust one rule"],
    sections: [
      {
        eyebrow: "Behavior over slogans",
        title: "Discipline problems leave a trail in completed trades.",
        copy:
          "A trader can understand risk management and still violate it under pressure. The useful question is when the behavior appears, how often it repeats, and what it costs in P&L or R.",
        bullets: ["Trades after losses", "Trades outside planned sessions", "Size changes after frustration"],
      },
      {
        eyebrow: "Measurable patterns",
        title: "Turn psychology into segments you can inspect.",
        copy:
          "Instead of labeling a whole trader as disciplined or undisciplined, segment historical trades by context: after a loss, after two losses, after a target, late in session, or outside a planned setup.",
        bullets: ["Compare baseline trades with trigger-state trades", "Review frequency and expectancy changes", "Look for behavior that repeats across weeks"],
      },
      {
        eyebrow: "TradePilot Coach's role",
        title: "AI reviews the trades already in your journal.",
        copy:
          "TradePilot Coach does not diagnose emotions or predict markets. It analyzes completed futures trades, notes, sessions, sizing, and outcomes so traders can choose a concrete review priority.",
        bullets: ["No trade calls", "No guaranteed outcomes", "Historical patterns do not guarantee future performance"],
      },
    ],
    comparison: {
      title: "Behavioral Patterns Worth Measuring",
      rows: [
        ["Revenge trading", "A vague feeling after a red day", "Performance after one or more losses"],
        ["Overtrading", "Just counting total trades", "Expectancy by trade number and session window"],
        ["Rule breaking", "A note saying discipline was bad", "Rule, trigger, violation, cost, and adjustment"],
      ],
    },
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "How to review a losing trading session", to: "/guides/how-to-review-a-losing-trading-session" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
    faq: [
      {
        question: "Can trading psychology really be measured?",
        answer:
          "Some emotional states cannot be measured directly, but the behaviors connected to them can often be reviewed through timing, sizing, frequency, setup notes, rule violations, and post-loss performance.",
      },
      {
        question: "Does TradePilot Coach cure discipline problems?",
        answer:
          "No. TradePilot Coach helps traders identify and review historical patterns. The trader remains responsible for risk, decisions, and process changes.",
      },
    ],
  },
  "/revenge-trading": {
    path: "/revenge-trading",
    keyword: "revenge trading",
    title: "Revenge Trading: How to Identify and Measure It | TradePilot Coach",
    description:
      "Learn what revenge trading looks like, how to identify it in your futures trade history, and how to measure what trading after losses may be costing you.",
    h1: "Revenge Trading: Stop Guessing and Measure the Pattern",
    eyebrow: "Revenge trading analysis",
    intro:
      "Revenge trading is not just being angry after a loss. It is the measurable shift in behavior that can happen when a trader tries to win money back instead of following the original plan.",
    cta: "Check My Trades for Revenge Trading",
    secondaryCta: "Review Post-Loss Trading",
    proof: ["Post-loss segments", "Consecutive-loss review", "Size changes", "Session deterioration"],
    workflow: ["Find loss trigger", "Compare next trades", "Measure size and frequency", "Create a guardrail"],
    sections: [
      {
        eyebrow: "Definition",
        title: "What revenge trading means in a journal.",
        copy:
          "In practice, revenge trading often appears as faster re-entry, larger size, lower setup quality, or a refusal to stop after the trade idea is invalidated.",
        bullets: ["Taking the next trade too quickly", "Increasing contracts after a loss", "Moving from planned setup to impulse trade"],
      },
      {
        eyebrow: "Why it hides",
        title: "You may not notice it while the session is moving.",
        copy:
          "The trader may experience the next entry as confidence, urgency, or a normal opportunity. Historical review can separate the feeling from the pattern.",
        bullets: ["Review trades after one loss", "Review trades after two consecutive losses", "Compare session results before and after the trigger"],
      },
      {
        eyebrow: "Example",
        title: "A hypothetical revenge-trading segment.",
        copy:
          "Example only, not TradePilot Coach user results: trades before a second consecutive loss are +$860, while trades afterward are -$1,740. The useful question is whether your own history shows a similar shift.",
        bullets: ["Baseline P&L before the trigger", "Post-trigger P&L and average R", "Time between trades after losses"],
      },
    ],
    comparison: {
      title: "Revenge Trading Signals to Measure",
      rows: [
        ["Frequency", "I felt active today", "Trades per hour after a loss"],
        ["Sizing", "I was trying to recover", "Contract size after losing trades"],
        ["Quality", "It looked like a setup", "Setup notes and rule adherence after the trigger"],
      ],
    },
    links: [
      { label: "Revenge trading calculator", to: "/tools/revenge-trading-calculator" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
    faq: [
      {
        question: "How do I know if I am revenge trading?",
        answer:
          "Look for measurable changes after losses: faster re-entry, larger size, worse expectancy, more trades, or more rule violations compared with baseline trades.",
      },
      {
        question: "Should every trader stop after one loss?",
        answer:
          "Not universally. Traders should use their own data and risk plan to decide whether performance deteriorates after losses.",
      },
    ],
  },
  "/overtrading": {
    path: "/overtrading",
    keyword: "overtrading",
    title: "Overtrading: Signs, Causes & How to Measure It | TradePilot Coach",
    description:
      "Learn the signs of overtrading, why trade count alone is not enough, and how futures traders can measure whether too many trades are hurting performance.",
    h1: "Are You Losing Money Because You Trade Too Much?",
    eyebrow: "Overtrading analysis",
    intro:
      "Overtrading is not defined by a universal number of trades. It becomes a problem when additional trades show lower quality, worse expectancy, or behavior outside your plan.",
    cta: "Analyze My Overtrading Pattern",
    secondaryCta: "Explore Discipline",
    proof: ["Trade-number analysis", "Time-of-day behavior", "Expectancy degradation", "Post-loss frequency"],
    workflow: ["Group trades", "Compare early vs late", "Check session windows", "Set trade-count rules"],
    sections: [
      {
        eyebrow: "Measurement",
        title: "Trade count alone does not prove overtrading.",
        copy:
          "A scalper and a selective setup trader can have different normal trade counts. The question is whether later trades, off-plan trades, or post-loss trades perform materially worse.",
        bullets: ["First 1-3 trades versus later trades", "P&L by trade number", "Trades outside the planned session"],
      },
      {
        eyebrow: "Common signs",
        title: "Overtrading often shows up as degradation.",
        copy:
          "Look for lower win rate, worse average R, larger losers, more trades after a daily objective, or increased frequency after consecutive losses.",
        bullets: ["Win rate degradation", "Expectancy degradation", "Position sizing changes"],
      },
      {
        eyebrow: "Example",
        title: "A useful overtrading review compares segments.",
        copy:
          "Example only: trades 1-3 average +0.42R, while trades 4+ average -0.31R. That does not prove the trader should always stop at three trades, but it gives a specific rule to test.",
        bullets: ["Measure average R by trade number", "Compare morning and afternoon windows", "Review outcomes after reaching a daily goal"],
      },
    ],
    comparison: {
      title: "Ways to Analyze Overtrading",
      rows: [
        ["Trade number", "How many trades did I take?", "How did trade 4+ perform versus trades 1-3?"],
        ["Session timing", "Was today busy?", "Which time window produced the low-quality trades?"],
        ["Trigger state", "I was frustrated", "Did frequency rise after losses or targets?"],
      ],
    },
    links: [
      { label: "Overtrading calculator", to: "/tools/overtrading-calculator" },
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "How to identify overtrading from trade history", to: "/guides/how-to-identify-overtrading-from-trade-history" },
      { label: "Trading after losses", to: "/trading-after-losses" },
    ],
    faq: [
      {
        question: "How many trades per day is overtrading?",
        answer:
          "There is no universal number. Overtrading depends on your strategy, session plan, trade quality, expectancy, and whether extra trades are outside your rules.",
      },
      {
        question: "Can a trading journal help reduce overtrading?",
        answer:
          "A journal can make the pattern visible by showing when extra trades occur, what triggers them, and whether their outcomes differ from planned trades.",
      },
    ],
  },
  "/trading-discipline": {
    path: "/trading-discipline",
    keyword: "trading discipline",
    title: "Trading Discipline: Measure It Instead of Relying on Willpower | TradePilot Coach",
    description:
      "Learn how to improve trading discipline by measuring rule adherence, session behavior, sizing consistency and repeated decision patterns.",
    h1: "Trading Discipline Becomes Easier to Improve When You Can Measure It",
    eyebrow: "Trading discipline",
    intro:
      "Motivation can fade in the middle of a session. A measurable discipline review shows when rules break, how often it happens, and which conditions deserve the next adjustment.",
    cta: "Measure My Trading Behavior",
    secondaryCta: "See Rule Breaking",
    proof: ["Planned sessions", "Max-trade rules", "Daily-loss behavior", "Position sizing"],
    workflow: ["Name the rule", "Find the context", "Measure frequency", "Review cost"],
    sections: [
      {
        eyebrow: "Rule adherence",
        title: "Knowing the correct rule is not the same as following it.",
        copy:
          "A trader can know to stop after predefined conditions and still continue when the market feels close to turning. The journal should show when, how often, under what conditions, and at what cost.",
        bullets: ["Respecting planned sessions", "Respecting maximum trades", "Respecting maximum loss"],
      },
      {
        eyebrow: "Consistency",
        title: "Discipline includes sizing, timing, and setup selection.",
        copy:
          "Discipline is not a personality label. It is a set of behaviors that can be reviewed: contract size, trade timing, setup quality, and whether the trade matched the plan.",
        bullets: ["Keeping position size consistent", "Avoiding unplanned setups", "Stopping after predefined conditions"],
      },
      {
        eyebrow: "Improvement loop",
        title: "Measure one behavior before trying to fix everything.",
        copy:
          "A useful review ends with one guardrail: stop after a specific trigger, reduce size in a weak window, or pause when the setup quality drops.",
        bullets: ["Choose a high-cost behavior", "Create a next-session rule", "Review whether the rule changed outcomes"],
      },
    ],
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "How to review a losing trading session", to: "/guides/how-to-review-a-losing-trading-session" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
    faq: [
      {
        question: "How can I improve trading discipline?",
        answer:
          "Start by measuring one specific behavior: trades outside your plan, size increases after losses, session overruns, or entries after a daily stop.",
      },
      {
        question: "Is discipline only about emotions?",
        answer:
          "No. Emotions matter, but discipline is visible through decisions: timing, sizing, trade selection, stopping rules, and repeated behavior.",
      },
    ],
  },
  "/breaking-trading-rules": {
    path: "/breaking-trading-rules",
    keyword: "breaking trading rules",
    title: "Why Traders Keep Breaking Their Own Rules | TradePilot Coach",
    description:
      "Understand why traders break trading rules and how to measure the trigger, violation, cost and adjustment in a futures trading journal.",
    h1: "You Know the Rule. Why Do You Keep Breaking It?",
    eyebrow: "Rule-breaking analysis",
    intro:
      "Rule breaking is rarely a knowledge problem. It is often a repeatable sequence: a trigger appears, the trader violates the plan, and the cost becomes clear only after review.",
    cta: "Find My Most Expensive Rule Break",
    secondaryCta: "Measure Discipline",
    proof: ["Decision fatigue", "Loss chasing", "FOMO", "Daily-stop review"],
    workflow: ["Rule", "Trigger", "Violation", "Cost"],
    sections: [
      {
        eyebrow: "Framework",
        title: "Rule -> Trigger -> Violation -> Cost -> Review -> Adjustment",
        copy:
          "A rule becomes measurable when each break is tied to a context. Was the trigger a loss, missed move, fatigue, daily stop, or planned-session boundary?",
        bullets: ["Write the rule clearly", "Label the trigger honestly", "Review the P&L or R cost"],
      },
      {
        eyebrow: "Common triggers",
        title: "The same rule may break under the same pressure.",
        copy:
          "FOMO, loss chasing, increasing size, decision fatigue, and continuing after a daily stop can all create repeated rule breaks that feel different in real time.",
        bullets: ["Trading outside planned hours", "Increasing size after frustration", "Entering without the planned setup"],
      },
      {
        eyebrow: "Journal review",
        title: "The goal is not shame. The goal is pattern recognition.",
        copy:
          "A good trading journal helps you see whether rule breaks cluster in specific windows, after specific outcomes, or when confidence and emotion labels change.",
        bullets: ["Measure recurring patterns", "Separate strategy issues from behavior issues", "Create one adjustment for the next session"],
      },
    ],
    comparison: {
      title: "From Rule Break to Review",
      rows: [
        ["Rule", "Do not trade after daily stop", "Defined as a trackable behavior"],
        ["Trigger", "Two losses and missed move", "Segmented against similar moments"],
        ["Adjustment", "Try harder tomorrow", "Specific next-session guardrail"],
      ],
    },
    links: [
      { label: "Trading discipline", to: "/trading-discipline" },
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Trading after losses", to: "/trading-after-losses" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Trading psychology", to: "/trading-psychology" },
    ],
    faq: [
      {
        question: "Why do I keep breaking my trading rules?",
        answer:
          "Often because the rule is tested under a recurring trigger: losses, FOMO, fatigue, missed moves, size changes, or pressure to recover.",
      },
      {
        question: "What should I record after breaking a rule?",
        answer:
          "Record the rule, trigger, violation, trade context, size, session, outcome, and one adjustment you can review next time.",
      },
    ],
  },
  "/trading-after-losses": {
    path: "/trading-after-losses",
    keyword: "trading after losses",
    title: "Trading After Losses: What Happens to Your Performance? | TradePilot Coach",
    description:
      "Learn how to analyze trading after losses by comparing baseline trades, trades after one loss, and trades after consecutive losses.",
    h1: "What Happens to Your Trading After You Lose?",
    eyebrow: "Post-loss trading",
    intro:
      "Some traders recover calmly after a loss. Others trade faster, larger, or lower-quality setups. The answer should come from your own historical trades, not a universal rule.",
    cta: "Analyze My Post-Loss Performance",
    secondaryCta: "Compare Revenge Trading",
    proof: ["Baseline comparison", "One-loss segment", "Two-loss segment", "Time-between-trades"],
    workflow: ["Baseline trades", "After one loss", "After two losses", "Review change"],
    sections: [
      {
        eyebrow: "Segmentation",
        title: "Compare baseline trades with post-loss trades.",
        copy:
          "A practical review separates normal-condition trades from trades immediately after one loss and trades after two consecutive losses.",
        bullets: ["Win rate by segment", "Average P&L and average R", "Number of trades after each loss state"],
      },
      {
        eyebrow: "Behavior shifts",
        title: "Losses can change more than the next outcome.",
        copy:
          "The post-loss pattern may appear in position size, time between trades, setup quality, session duration, or the decision to keep trading past a planned stop.",
        bullets: ["Position size changes", "Shorter time between trades", "Longer sessions after a red start"],
      },
      {
        eyebrow: "No universal rule",
        title: "Your data should decide whether performance deteriorates.",
        copy:
          "Avoid blanket rules like always stopping after two losses unless your data and risk plan support it. The goal is to discover the conditions where your own process weakens.",
        bullets: ["Historical patterns do not guarantee future results", "Use sample size and context", "Turn the finding into a testable guardrail"],
      },
    ],
    comparison: {
      title: "Post-Loss Performance Segments",
      rows: [
        ["Baseline", "All trades mixed together", "Trades without immediate loss trigger"],
        ["After one loss", "I think I stayed calm", "Next-trade win rate, average R, and size"],
        ["After two losses", "I should probably stop", "Measured deterioration or resilience"],
      ],
    },
    links: [
      { label: "Revenge trading", to: "/revenge-trading" },
      { label: "Revenge trading calculator", to: "/tools/revenge-trading-calculator" },
      { label: "Overtrading", to: "/overtrading" },
      { label: "Futures trading journal", to: "/futures-trading-journal" },
      { label: "Breaking trading rules", to: "/breaking-trading-rules" },
      { label: "How to review a losing trading session", to: "/guides/how-to-review-a-losing-trading-session" },
    ],
    faq: [
      {
        question: "What should I do after a losing trade?",
        answer:
          "Follow your risk plan. Then review whether your historical post-loss trades show worse expectancy, larger size, faster re-entry, or more rule breaks.",
      },
      {
        question: "Should I stop after two consecutive losses?",
        answer:
          "Not as a universal rule. Use your own data to determine whether performance meaningfully deteriorates after one or two losses.",
      },
    ],
  },
};

const landingBreadcrumbLabels: Record<string, string> = {
  "/futures-trading-journal": "Futures Trading Journal",
  "/topstep-trading-journal": "Topstep Trading Journal",
  "/tradovate-trading-journal": "Tradovate Trading Journal",
  "/prop-firm-trading-journal": "Prop Firm Trading Journal",
  "/ai-trading-journal": "AI Trading Journal",
  "/trading-psychology": "Trading Psychology",
  "/revenge-trading": "Revenge Trading",
  "/overtrading": "Overtrading",
  "/trading-discipline": "Trading Discipline",
  "/breaking-trading-rules": "Breaking Trading Rules",
  "/trading-after-losses": "Trading After Losses",
};

const behavioralChildPaths = new Set([
  "/revenge-trading",
  "/overtrading",
  "/trading-discipline",
  "/breaking-trading-rules",
  "/trading-after-losses",
]);

function landingBreadcrumbs(content: LandingPageContent): BreadcrumbItem[] {
  const items: BreadcrumbItem[] = [{ label: "Home", path: "/" }];
  if (behavioralChildPaths.has(content.path)) {
    items.push({ label: "Trading Psychology", path: "/trading-psychology" });
  }
  items.push({ label: landingBreadcrumbLabels[content.path] ?? content.h1, path: content.path });
  return items;
}

export function SeoLandingPage({ content }: { content: LandingPageContent }) {
  useEffect(() => {
    trackSeoLandingPageView(content.path);
  }, [content.path]);
  const isBehavioralPage = [
    "/trading-psychology",
    "/revenge-trading",
    "/overtrading",
    "/trading-discipline",
    "/breaking-trading-rules",
    "/trading-after-losses",
  ].includes(content.path);

  const breadcrumbs = landingBreadcrumbs(content);
  const structuredData = pageStructuredData({
    path: content.path,
    title: content.title,
    description: content.description,
    breadcrumbs,
  });

  return (
    <MarketingShell>
      <Seo
        title={content.title}
        description={content.description}
        path={content.path}
        structuredData={structuredData}
      />

      <section className="marketing-hero seo-landing-hero">
        <div className="marketing-container">
          <Breadcrumbs items={breadcrumbs} />
          <div className="marketing-hero-grid">
          <div>
            <div className="marketing-eyebrow">{content.eyebrow}</div>
            <h1 className="marketing-title">{content.h1}</h1>
            <p className="marketing-subtitle hero-support">{content.intro}</p>
            <div className="marketing-hero-actions">
              <TrackedCta content={content} label={content.cta} to="/register" primary />
              {content.secondaryCta && (
                <TrackedCta
                  content={content}
                  label={content.secondaryCta}
                  to={content.links[0]?.to ?? "/"}
                />
              )}
            </div>
            <p className="marketing-trust-copy">
              {isBehavioralPage
                ? "TradePilot Coach analyzes completed trades and historical behavior. No signals, trade calls, market predictions, or profit promises."
                : "Import Topstep and Tradovate trade history by CSV. No signals, trade calls, or profit promises."}
            </p>
            <div className="marketing-proof-row compact" aria-label={`${content.keyword} highlights`}>
              {content.proof.map((item) => (
                <div className="marketing-proof-item" key={item}>{item}</div>
              ))}
            </div>
          </div>
          <SeoInsightPanel content={content} />
          </div>
        </div>
      </section>

      {content.workflow && (
        <section className="marketing-section">
          <div className="marketing-container">
            <header className="marketing-section-header centered">
              <div className="marketing-eyebrow">Workflow</div>
              <h2 className="marketing-section-title">From Search to First Personalized Insight</h2>
              <p className="marketing-section-copy">
                The path is simple: open a free account, import a supported CSV, then review the first insight TradePilot Coach can support from your data.
              </p>
            </header>
            <div className="workflow-flow" aria-label={`${content.keyword} workflow`}>
              {content.workflow.map((step) => (
                <span key={step}>{step}</span>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="marketing-section">
        <div className="marketing-container seo-section-grid">
          {content.sections.map((section) => (
            <article className="seo-story-card" key={section.title}>
              <div className="marketing-eyebrow">{section.eyebrow}</div>
              <h2>{section.title}</h2>
              <p>{section.copy}</p>
              <ul>
                {section.bullets.map((bullet) => (
                  <li key={bullet}>{bullet}</li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>

      {content.comparison && (
        <section className="marketing-section">
          <div className="marketing-container">
            <header className="marketing-section-header">
              <div className="marketing-eyebrow">Comparison</div>
              <h2 className="marketing-section-title">{content.comparison.title}</h2>
            </header>
            <div className="comparison-table" role="table" aria-label={content.comparison.title}>
              <div className="comparison-row comparison-header" role="row">
                <div role="columnheader">Area</div>
                <div role="columnheader">Common Approach</div>
                <div role="columnheader">TradePilot Coach</div>
              </div>
              {content.comparison.rows.map(([area, common, tradepilot]) => (
                <div className="comparison-row" role="row" key={area}>
                  <div role="cell" data-label="Area">{area}</div>
                  <div role="cell" data-label="Common Approach">{common}</div>
                  <div role="cell" data-label="TradePilot Coach">{tradepilot}</div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="marketing-section">
        <div className="marketing-container audience-panel seo-link-panel">
          <div>
            <div className="marketing-eyebrow">Keep exploring</div>
            <h2 className="marketing-section-title">Related TradePilot Coach Pages</h2>
            <p className="marketing-section-copy">
              Use these focused pages to compare import workflows, futures journaling, prop-firm review, and AI coaching.
            </p>
          </div>
          <nav className="audience-grid" aria-label="Related SEO pages">
            {content.links.map((link) => (
              <Link className="audience-item seo-related-link" key={link.to} to={link.to}>
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </section>

      <section className="marketing-section faq-section" id="faq">
        <div className="marketing-container">
          <header className="marketing-section-header centered">
            <div className="marketing-eyebrow">FAQ</div>
            <h2 className="marketing-section-title">Clear Expectations Before You Import</h2>
          </header>
          <div className="faq-grid">
            {content.faq.map((faq) => (
              <article className="faq-card" key={faq.question}>
                <h3>{faq.question}</h3>
                <p>{faq.answer}</p>
              </article>
            ))}
          </div>
          {content.disclaimer && <p className="seo-disclaimer">{content.disclaimer}</p>}
          <div className="final-cta">
            <h2 className="marketing-section-title">Ready to review the trades you already took?</h2>
            <p className="marketing-section-copy">
              Start with a free TradePilot Coach account, import supported CSV history, and use your first review loop to find the behavior worth fixing next.
            </p>
            <div className="final-cta-actions">
              <TrackedCta content={content} label={content.cta} to="/register" primary />
              <TrackedCta content={content} label="View Pricing" to="/#pricing" />
            </div>
          </div>
        </div>
      </section>
    </MarketingShell>
  );
}

function TrackedCta({
  content,
  label,
  to,
  primary = false,
}: {
  content: LandingPageContent;
  label: string;
  to: string;
  primary?: boolean;
}) {
  const source = activationSourceForPublicPath(content.path);
  const destination = primary && source ? activationAwareCtaDestination(source) : to;

  return (
    <Link
      className={`marketing-button ${primary ? "primary" : "ghost"}`}
      to={destination}
      onClick={() => trackSeoCtaClick(content.path, label, destination)}
    >
      {label}
    </Link>
  );
}

function SeoInsightPanel({ content }: { content: LandingPageContent }) {
  return (
    <aside className="seo-insight-panel" aria-label={`${content.keyword} TradePilot Coach preview`}>
      <div className="report-kicker">TradePilot Coach review loop</div>
      <h2>Completed trades become coaching data.</h2>
      <div className="seo-insight-metrics">
        <div>
          <span>Import</span>
          <strong>CSV</strong>
        </div>
        <div>
          <span>Risk</span>
          <strong>1R</strong>
        </div>
        <div>
          <span>Focus</span>
          <strong>Behavior</strong>
        </div>
      </div>
      <div className="seo-insight-card">
        <span>Detected pattern</span>
        <strong>Losses cluster after continuing past the planned session window.</strong>
        <p>Review session quality, sizing, and whether the next trade followed your rules.</p>
      </div>
      <ol className="seo-insight-steps">
        <li>Import supported Topstep or Tradovate CSV history.</li>
        <li>Review trades, sessions, and R-multiple context.</li>
        <li>Use AI coaching to pick one next-session guardrail.</li>
      </ol>
    </aside>
  );
}
