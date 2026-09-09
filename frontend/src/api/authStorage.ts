export const TOKEN_STORAGE_KEY = "tradepilot_token";
export const USER_STORAGE_KEY = "tradepilot_user";

function browserStorageAvailable(storage: Storage | undefined) {
  if (typeof window === "undefined" || !storage) return false;

  try {
    const probe = "__tradepilot_storage_probe__";
    storage.setItem(probe, "1");
    storage.removeItem(probe);
    return true;
  } catch {
    return false;
  }
}

function primaryStorage() {
  if (typeof window === "undefined") return null;
  return import.meta.env.PROD ? window.sessionStorage : window.localStorage;
}

function legacyStorage() {
  if (typeof window === "undefined") return null;
  return window.localStorage;
}

export function setAuthItem(key: string, value: string) {
  const primary = primaryStorage();
  if (primary && browserStorageAvailable(primary)) {
    primary.setItem(key, value);
  }

  if (import.meta.env.PROD) {
    legacyStorage()?.removeItem(key);
  }
}

export function getAuthItem(key: string) {
  const primary = primaryStorage();
  if (primary && browserStorageAvailable(primary)) {
    const value = primary.getItem(key);
    if (value) return value;
  }

  return legacyStorage()?.getItem(key) ?? null;
}

export function removeAuthItem(key: string) {
  primaryStorage()?.removeItem(key);
  legacyStorage()?.removeItem(key);
}
