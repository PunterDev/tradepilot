import type { FeatureAvailability } from "./featureFlags";
import { marketingPricing } from "./marketingPricing";

function enabled(availability: FeatureAvailability | null | undefined, feature: keyof FeatureAvailability["features"]) {
  return Boolean(availability?.v2FeaturesEnabled && availability.features[feature]);
}

export function getEnabledProBenefits(availability?: FeatureAvailability | null) {
  const enabledV2Benefits = [
    ...(enabled(availability, "tradingMemory") ? marketingPricing.pro.v2Benefits.tradingMemory : []),
    ...(enabled(availability, "tradingDna") ? marketingPricing.pro.v2Benefits.tradingDna : []),
    ...(enabled(availability, "journey") ? marketingPricing.pro.v2Benefits.journey : []),
    ...(enabled(availability, "playbook") ? marketingPricing.pro.v2Benefits.playbook : []),
  ];

  return [...marketingPricing.pro.benefits, ...enabledV2Benefits];
}

export function getEnabledProMarketingBenefits(availability?: FeatureAvailability | null) {
  const enabledV2Benefits = [
    ...(enabled(availability, "tradingMemory") ? marketingPricing.pro.v2Benefits.tradingMemoryDetails : []),
    ...(enabled(availability, "tradingDna") ? marketingPricing.pro.v2Benefits.tradingDna : []),
    ...(enabled(availability, "journey") ? marketingPricing.pro.v2Benefits.journey : []),
    ...(enabled(availability, "playbook") ? marketingPricing.pro.v2Benefits.playbook : []),
  ];

  return [...marketingPricing.pro.benefits, ...enabledV2Benefits];
}
