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
  v2FeaturesEnabled: boolean;
};

export default function SessionReviewPage({ accounts, selectedAccountId, trades, onTradeSelect, v2FeaturesEnabled }: Props) {
  return (
    <>
      <PageHeader
        label="Trade Replay"
        title="Session Review"
        titleTestId="session-review-heading"
        description="Replay a full trading day, inspect the execution timeline, and generate day-level AI coaching."
      />
      <SessionReviewPanel
        accounts={accounts}
        selectedAccountId={selectedAccountId}
        trades={trades}
        onTradeSelect={onTradeSelect}
        v2FeaturesEnabled={v2FeaturesEnabled}
      />
    </>
  );
}
