import { CoachDashboard } from "../../components/CoachDashboard";
import PageHeader from "../../layout/PageHeader";
import type { BillingUsage } from "../../types/billing";
import type { SelectedAccountId } from "../../types/dashboard";

type Props = {
  selectedAccountId: SelectedAccountId;
  usage?: BillingUsage | null;
  onUsageChanged: () => void;
};

export default function CoachPage({ selectedAccountId, usage, onUsageChanged }: Props) {
  return (
    <>
      <PageHeader
        label="AI Coach"
        title="Daily & Weekly Coach"
        description="Cached coaching snapshots, date selectors, and controlled regenerations."
      />
      <CoachDashboard
        selectedAccountId={selectedAccountId}
        usage={usage}
        onUsageChanged={onUsageChanged}
      />
    </>
  );
}
