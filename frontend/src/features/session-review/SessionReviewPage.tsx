import SessionReviewPanel from "../../components/SessionReviewPanel";
import PageHeader from "../../layout/PageHeader";
import type { TradingAccount } from "../../api/accountApi";
import type { SelectedAccountId } from "../../types/dashboard";
import type { TradovateTrade } from "../../utils/tradovateCsv";

type Props = {
  accounts: TradingAccount[];
  selectedAccountId: SelectedAccountId;
  trades: TradovateTrade[];
  onTradeSelect: (tradeId: number) => void;
};

export default function SessionReviewPage({ accounts, selectedAccountId, trades, onTradeSelect }: Props) {
  return (
    <>
      <PageHeader
        label="Trade Replay"
        title="Session Review"
        description="Replay a full trading day, inspect the execution timeline, and generate day-level AI coaching."
      />
      <SessionReviewPanel
        accounts={accounts}
        selectedAccountId={selectedAccountId}
        trades={trades}
        onTradeSelect={onTradeSelect}
      />
    </>
  );
}
