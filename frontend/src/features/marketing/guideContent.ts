export type GuideSection = {
  title: string;
  paragraphs: string[];
  bullets?: string[];
};

export type GuideContent = {
  path: string;
  title: string;
  description: string;
  h1: string;
  eyebrow: string;
  intro: string;
  sections: GuideSection[];
  checklist?: string[];
  relatedLinks: { label: string; to: string }[];
  primaryCta: string;
  disclaimer?: string;
  source?: { label: string; href: string };
};

const topstepSource = {
  label: "TopstepX help documentation: Exporting Trades",
  href: "https://help.topstep.com/en/articles/14434175-topstepx",
};

const tradovateSource = {
  label: "Tradovate help: How Can I Run a Report Within Tradovate's Platform?",
  href: "https://tradovate.zendesk.com/hc/en-us/articles/16653178768275-How-Can-I-Run-a-Report-Within-Tradovate-s-Platform",
};

export const guides: Record<string, GuideContent> = {
  "/guides/how-to-export-topstep-trades": {
    path: "/guides/how-to-export-topstep-trades",
    title: "How to Export Topstep Trades for a Trading Journal | TradePilot Coach",
    description: "Export TopstepX orders for a trading journal, understand the orders_export.csv file TradePilot Coach supports, and avoid common import problems.",
    h1: "How to Export Your Topstep Trades",
    eyebrow: "Topstep export guide",
    intro: "For TradePilot Coach, export from the TopstepX Orders view when you need the orders_export.csv history that matches the current importer.",
    checklist: ["A completed-trade date range", "One source account in the export", "The original CSV file without renamed headers"],
    sections: [
      { title: "How to Export From TopstepX", paragraphs: ["TopstepX documents a simple flow: open the Orders or Trades tab, select Export, choose the date range, then export the file. For TradePilot Coach imports, use the Orders view so the exported order history matches the supported orders_export.csv shape."] },
      { title: "Which File TradePilot Coach Supports", paragraphs: ["TradePilot Coach checks for the Topstep orders_export.csv format. The file needs the order-history fields used to identify an execution, its account, contract, size, side, fill time, price, and opening or closing disposition."], bullets: ["Required data includes Id, AccountName, ContractName, Status, Size, Side, FilledAt, ExecutePrice, PositionDisposition, and CreationDisposition.", "Keep one source AccountName per file.", "TradePilot checks the file during upload and gives validation feedback for unsupported data."] },
      { title: "What TradePilot Coach Does With the CSV", paragraphs: ["Only Filled rows are treated as executions. Cancelled or unfilled bracket orders are ignored. TradePilot Coach rebuilds opening and closing executions into completed trades, does not save incomplete positions, rejects unsupported contracts with feedback, and uses duplicate protection so the same history is not counted twice."] },
      { title: "Common Import Problems", paragraphs: ["Use the original export rather than editing headers. If the file includes more than one source account, export each account separately. Open positions are intentionally left out of completed-trade analysis; close them in the source history before expecting a completed-trade record."] },
      { title: "What Happens After Import", paragraphs: ["Review completed trades by session, direction, size, and outcome. The useful next question is not whether an order existed, but whether the completed trade followed your plan and whether the same behavior repeats."] },
    ],
    relatedLinks: [
      { label: "Topstep trading journal", to: "/topstep-trading-journal" }, { label: "Futures trading journal", to: "/futures-trading-journal" }, { label: "AI trading journal", to: "/ai-trading-journal" }, { label: "Prop firm trading journal", to: "/prop-firm-trading-journal" },
    ],
    primaryCta: "Review completed Topstep trades",
    disclaimer: "TradePilot Coach is an independent product and is not affiliated with or endorsed by Topstep.",
    source: topstepSource,
  },
  "/guides/how-to-export-tradovate-trades": {
    path: "/guides/how-to-export-tradovate-trades",
    title: "How to Export Tradovate Trades to CSV | TradePilot Coach",
    description: "Export Tradovate trade history to CSV, understand compatible completed-trade files, and review how TradePilot Coach validates imported futures trades.",
    h1: "How to Export Your Tradovate Trade History",
    eyebrow: "Tradovate export guide",
    intro: "Use Tradovate Reports to filter the time period you want to review, download the result as CSV, then let TradePilot Coach check the file structure during upload.",
    checklist: ["A report filtered to the completed trade period you want to review", "The downloaded CSV without manual header edits", "One account context selected in TradePilot Coach"],
    sections: [
      { title: "Where Tradovate Reports Are Located", paragraphs: ["Tradovate documents that reports can be opened from the account area. Choose a report, set the timeframe and any relevant filters, run it, then use Download Report to export the CSV."] },
      { title: "Which Tradovate CSV TradePilot Coach Supports", paragraphs: ["TradePilot Coach supports compatible completed-trade CSVs with fields such as symbol, qty, buyPrice, sellPrice, pnl, boughtTimestamp, and soldTimestamp. Some performance-style exports also include buyFillId and sellFillId. TradePilot checks the CSV structure when you upload it."], bullets: ["Do not manually rename headers to force an upload.", "Incompatible rows return validation feedback.", "Use a completed-trade export rather than an open-position snapshot."] },
      { title: "How TradePilot Coach Interprets the File", paragraphs: ["When explicit side fields are present, TradePilot uses them for direction. Otherwise, timing can help infer long or short. Timestamps are normalized for session analysis, supported futures P&L is validated, and duplicate protection prevents the same completed trade from being counted again."] },
      { title: "Common Upload Problems", paragraphs: ["A report can be useful on its own but still lack the fields required for a completed-trade import. Re-run the report with the intended date range and criteria rather than editing data by hand. Review validation feedback before trying another upload."] },
      { title: "What to Review After Import", paragraphs: ["Start with completed trade history: session timing, trade frequency, size changes, and outcomes after losses. That creates a concrete review rather than a platform report you never revisit."] },
    ],
    relatedLinks: [
      { label: "Tradovate trading journal", to: "/tradovate-trading-journal" }, { label: "Futures trading journal", to: "/futures-trading-journal" }, { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
    primaryCta: "Review completed Tradovate trades",
    disclaimer: "TradePilot Coach is an independent product and is not affiliated with or endorsed by Tradovate.",
    source: tradovateSource,
  },
  "/guides/how-to-review-a-losing-trading-session": {
    path: "/guides/how-to-review-a-losing-trading-session",
    title: "How to Review a Losing Trading Session | TradePilot Coach",
    description: "Use a practical, completed-trade framework to review a losing trading session without chasing P&L or turning hindsight into a market call.",
    h1: "How to Review a Losing Trading Session Without Chasing P&L",
    eyebrow: "Trading review framework",
    intro: "A useful losing-session review separates valid strategy losses from execution errors, then identifies one behavior you can measure and guard against next session.",
    sections: [
      { title: "Start With the Session Plan", paragraphs: ["Write down the planned session window, setup conditions, size, and stop conditions before judging the result. A losing session can still contain trades that respected the plan."], bullets: ["Was the setup valid?", "Did the trade respect the plan?", "Did trading continue beyond the planned session?"] },
      { title: "Separate Strategy Losses From Execution Errors", paragraphs: ["Label each completed trade by process, not outcome. A valid loss and a chased entry may both lose money, but they point to different next actions."] },
      { title: "Review the First Clear Invalidation", paragraphs: ["Find the earliest moment the original idea was no longer valid. Then compare what happened after that point: did you exit, reduce, re-enter, or keep trading without a new reason? This is a process check, not a prediction exercise."] },
      { title: "Check What Changed After the First Loss", paragraphs: ["Compare position size, time between trades, setup quality, and trade frequency before and after the first loss. Smaller gaps between trades or larger size after losses are measurable observations worth reviewing."] },
      { title: "Identify One Repeatable Behavior", paragraphs: ["Avoid writing a long list of fixes. Select one pattern, such as extending the session after consecutive losses, and turn it into one guardrail for the next session. Review whether it appears again in future completed trade history."] },
    ],
    relatedLinks: [
      { label: "Trading after losses", to: "/trading-after-losses" }, { label: "Revenge trading", to: "/revenge-trading" }, { label: "Overtrading", to: "/overtrading" }, { label: "Trading discipline", to: "/trading-discipline" }, { label: "AI trading journal", to: "/ai-trading-journal" }, { label: "Futures trading journal", to: "/futures-trading-journal" }, { label: "Revenge trading calculator", to: "/tools/revenge-trading-calculator" },
    ],
    primaryCta: "Review a completed session",
  },
  "/guides/how-to-identify-overtrading-from-trade-history": {
    path: "/guides/how-to-identify-overtrading-from-trade-history",
    title: "How to Identify Overtrading From Your Trade History | TradePilot Coach",
    description: "Measure possible overtrading from your own completed trade history by comparing trade timing, performance, size, setup quality, and behavior after losses.",
    h1: "How to Tell If You Are Overtrading Using Your Own Trade History",
    eyebrow: "Overtrading review guide",
    intro: "Overtrading is contextual. Instead of choosing a universal trade limit, compare your own early and later trades, behavior after losses, trade spacing, size, and setup quality.",
    sections: [
      { title: "What Overtrading Actually Means", paragraphs: ["More trades do not automatically mean overtrading. The signal is a repeatable deterioration in decision quality, process, or outcomes relative to your own normal context."] },
      { title: "Compare Early vs Later Trades", paragraphs: ["Group completed trades by trade number or time of day. Compare expectancy, win rate, average loss, and net result. A later-trade difference is an observation to investigate, not a diagnosis by itself."] },
      { title: "Check What Happens After Losses", paragraphs: ["Look at trade count, time between trades, and size after one or more losses. Did trades increase after consecutive losses? Did the next setup meet the same standard as the first?" ] },
      { title: "Measure Time, Size, and Setup Quality", paragraphs: ["Review whether time between trades shrinks, positions grow, sessions extend, or rule violations appear. These measures help identify the trigger that turns normal participation into lower-quality activity."] },
      { title: "Build a Personal Stop Rule", paragraphs: ["Use the pattern to create a specific guardrail, such as pausing after a defined number of low-quality attempts or stopping when your planned session ends. Test the rule against future history rather than assuming it will solve every loss."] },
    ],
    relatedLinks: [
      { label: "Overtrading", to: "/overtrading" }, { label: "Overtrading calculator", to: "/tools/overtrading-calculator" }, { label: "Trading discipline", to: "/trading-discipline" }, { label: "Breaking trading rules", to: "/breaking-trading-rules" }, { label: "Trading after losses", to: "/trading-after-losses" }, { label: "Futures trading journal", to: "/futures-trading-journal" }, { label: "AI trading journal", to: "/ai-trading-journal" },
    ],
    primaryCta: "Review trade-history patterns",
  },
};
