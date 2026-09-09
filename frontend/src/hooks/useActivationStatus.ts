import { useEffect, useState } from "react";
import { getActivationStatus } from "../api/activationApi";
import type { ActivationStatus } from "../types/activation";

export function useActivationStatus(enabled = true) {
  const [status, setStatus] = useState<ActivationStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function reload() {
    if (!enabled) return;
    setLoading(true);
    setError("");
    try {
      setStatus(await getActivationStatus());
    } catch (err) {
      console.error(err);
      setError("Failed to load activation status.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void reload();
  }, [enabled]);

  return { status, loading, error, reload };
}
