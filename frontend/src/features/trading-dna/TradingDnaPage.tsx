import TradingDnaPanel from "../../components/TradingDnaPanel";
import PageHeader from "../../layout/PageHeader";
import type { TradingAccount } from "../../api/accountApi";
import type { BillingUsage } from "../../types/billing";
import type { SelectedAccountId } from "../../types/dashboard";

type Props = {
  selectedAccountId: SelectedAccountId;
  accounts: TradingAccount[];
  accountsLoading: boolean;
  usage?: BillingUsage | null;
  onInvalidAccount: () => void;
  onUsageChanged: () => void;
};

export default function TradingDnaPage(props: Props) {
  return (
    <>
      <PageHeader
        label="Trading DNA"
        title="Behavior Intelligence"
        description="Long-term strengths, weaknesses, emotional patterns, and consistency trends."
      />
      <TradingDnaPanel {...props} />
    </>
  );
}
