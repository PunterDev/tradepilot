export type ReleaseFeatureKey = "PLAYBOOK" | "TRADING_DNA" | "JOURNEY" | "TRADING_MEMORY" | "TRADE_REPLAY";

export type FeatureAvailability = {
  v2FeaturesEnabled: boolean;
  v3FeaturesEnabled: boolean;
  features: {
    playbook: boolean;
    tradingDna: boolean;
    journey: boolean;
    tradingMemory: boolean;
    tradeReplay: boolean;
    coach: boolean;
  };
};

const TRUE_VALUES = new Set(["true", "1", "yes", "on"]);
const FALSE_VALUES = new Set(["false", "0", "no", "off", ""]);

export function parseBooleanEnv(value: unknown, fallback = false): boolean {
  if (typeof value !== "string") {
    return fallback;
  }

  const normalized = value.trim().toLowerCase();
  if (TRUE_VALUES.has(normalized)) {
    return true;
  }
  if (FALSE_VALUES.has(normalized)) {
    return false;
  }

  if (import.meta.env.DEV) {
    console.warn(
      `Unknown VITE_V2_FEATURES_ENABLED value "${value}". V2 features will remain disabled.`
    );
  }
  return fallback;
}

export const featureFlags = {
  v2FeaturesEnabled: parseBooleanEnv(import.meta.env.VITE_V2_FEATURES_ENABLED, false),
  v3FeaturesEnabled: parseBooleanEnv(import.meta.env.VITE_V3_FEATURES_ENABLED, false),
};

export function isFeatureEnabled(feature: ReleaseFeatureKey, availability?: FeatureAvailability | null): boolean {
  if (availability) {
    switch (feature) {
      case "PLAYBOOK":
        return availability.features.playbook;
      case "TRADING_DNA":
        return availability.features.tradingDna;
      case "JOURNEY":
        return availability.features.journey;
      case "TRADING_MEMORY":
        return availability.features.tradingMemory;
      case "TRADE_REPLAY":
        return availability.features.tradeReplay;
      default:
        return false;
    }
  }

  switch (feature) {
    case "PLAYBOOK":
    case "TRADING_DNA":
    case "JOURNEY":
    case "TRADING_MEMORY":
      return featureFlags.v2FeaturesEnabled;
    case "TRADE_REPLAY":
      return featureFlags.v3FeaturesEnabled;
    default:
      return false;
  }
}
