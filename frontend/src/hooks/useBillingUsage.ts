import { useEffect, useState } from "react";
import { getBillingUsage } from "../api/billingApi";
import type { BillingUsage } from "../types/billing";

export function useBillingUsage(enabled = true) {
  const [usage, setUsage] = useState<BillingUsage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function reload() {
    if (!enabled) return;
    setLoading(true);
    setError("");
    try {
      setUsage(await getBillingUsage());
    } catch (err) {
      console.error(err);
      setError("Failed to load billing usage.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void reload();
  }, [enabled]);

  return { usage, loading, error, reload };
}
