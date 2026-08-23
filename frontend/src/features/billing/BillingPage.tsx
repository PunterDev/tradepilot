import type { AuthUser } from "../../api/authApi";
import type { FeatureAvailability } from "../../config/featureFlags";
import type { BillingUsage } from "../../types/billing";
import AdminBillingDashboard from "./AdminBillingDashboard";
import UserBillingPage from "./UserBillingPage";

type Props = {
  user: AuthUser;
  usage: BillingUsage | null;
  loading: boolean;
  error: string;
  featureAvailability: FeatureAvailability;
};

export default function BillingPage({ user, usage, loading, error, featureAvailability }: Props) {
  if (user.role === "ADMIN") {
    return <AdminBillingDashboard />;
  }

  return (
    <UserBillingPage
      user={user}
      usage={usage}
      loading={loading}
      error={error}
      featureAvailability={featureAvailability}
    />
  );
}
