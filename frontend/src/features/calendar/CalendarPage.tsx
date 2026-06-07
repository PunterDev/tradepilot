import { useEffect } from "react";
import { trackFeatureUsage } from "../../api/featureUsageApi";
import type React from "react";
import PnlCalendar from "../../components/PnlCalendar";
import PageHeader from "../../layout/PageHeader";
import type { SelectedAccountId } from "../../types/dashboard";
import type { DailyPnl } from "../../utils/dashboardUtils";
import type { TradovateTrade } from "../../utils/tradovateCsv";

type Props = {
  dailyPnl: DailyPnl[];
  trades: TradovateTrade[];
  selectedAccountId: SelectedAccountId;
  calendarYear: number;
  calendarMonth: number;
  setCalendarYear: React.Dispatch<React.SetStateAction<number>>;
  setCalendarMonth: React.Dispatch<React.SetStateAction<number>>;
  journalDate: string;
  setJournalDate: React.Dispatch<React.SetStateAction<string>>;
  journalMood: string;
  setJournalMood: React.Dispatch<React.SetStateAction<string>>;
  journalSetupQuality: string;
  setJournalSetupQuality: React.Dispatch<React.SetStateAction<string>>;
  journalMistakes: string;
  setJournalMistakes: React.Dispatch<React.SetStateAction<string>>;
  journalLesson: string;
  setJournalLesson: React.Dispatch<React.SetStateAction<string>>;
  journalNotes: string;
  setJournalNotes: React.Dispatch<React.SetStateAction<string>>;
  handleSaveJournal: () => void;
  onTradeSelect: (tradeId: number) => void;
};

export default function CalendarPage(props: Props) {
  useEffect(() => {
    void trackFeatureUsage("CALENDAR_VIEW").catch(() => undefined);
  }, []);

  return (
    <>
      <PageHeader
        label="Calendar"
        title="P&L Calendar"
        description="Review monthly P&L, click a trading day, and open trades directly from the calendar."
      />
      <PnlCalendar {...props} />
    </>
  );
}
