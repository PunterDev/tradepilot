import { useCallback, useEffect, useRef, useState } from "react";

type CacheEntry<T> = {
  expiresAt: number;
  value: T;
};

const requestCache = new Map<string, CacheEntry<unknown>>();

export function useCachedRequest<T>(
  key: string,
  loader: () => Promise<T>,
  ttlMs = 45_000
) {
  const loaderRef = useRef(loader);
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  loaderRef.current = loader;

  const refresh = useCallback(async (force = false) => {
    setLoading(true);
    setError("");
    try {
      const cached = requestCache.get(key) as CacheEntry<T> | undefined;
      if (!force && cached && cached.expiresAt > Date.now()) {
        setData(cached.value);
        return cached.value;
      }

      const value = await loaderRef.current();
      requestCache.set(key, { value, expiresAt: Date.now() + ttlMs });
      setData(value);
      return value;
    } catch {
      setError("Unable to load data.");
      throw new Error("Unable to load data.");
    } finally {
      setLoading(false);
    }
  }, [key, ttlMs]);

  useEffect(() => {
    void refresh(false).catch(() => undefined);
  }, [refresh]);

  return { data, loading, error, refresh };
}
