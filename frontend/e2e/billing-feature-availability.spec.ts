import { expect, test } from "@playwright/test";
import type { FeatureAvailability } from "../src/config/featureFlags";
import { getEnabledProBenefits, getEnabledProMarketingBenefits } from "../src/config/planBenefits";
import { marketingPricing } from "../src/config/marketingPricing";

const disabledAvailability: FeatureAvailability = {
  v2FeaturesEnabled: false,
  features: {
    playbook: false,
    tradingDna: false,
    journey: false,
    tradingMemory: false,
    coach: true,
  },
};

const v2WithoutMemoryAvailability: FeatureAvailability = {
  v2FeaturesEnabled: true,
  features: {
    playbook: true,
    tradingDna: true,
    journey: true,
    tradingMemory: false,
    coach: true,
  },
};

const memoryAvailability: FeatureAvailability = {
  v2FeaturesEnabled: true,
  features: {
    playbook: true,
    tradingDna: true,
    journey: true,
    tradingMemory: true,
    coach: true,
  },
};

const onlyMemoryAvailability: FeatureAvailability = {
  v2FeaturesEnabled: true,
  features: {
    playbook: false,
    tradingDna: false,
    journey: false,
    tradingMemory: true,
    coach: true,
  },
};

test("billing benefits hide Trading Memory when V2 is disabled", () => {
  const benefits = getEnabledProBenefits(disabledAvailability);

  expect(benefits).toEqual(marketingPricing.pro.benefits);
  expect(benefits).not.toContain("AI Trading Memory and recurring pattern insights");
});

test("billing benefits hide Trading Memory when V2 is enabled but TRADING_MEMORY is disabled", () => {
  const benefits = getEnabledProBenefits(v2WithoutMemoryAvailability);

  expect(benefits).toContain("Trading DNA");
  expect(benefits).toContain("Performance Journey");
  expect(benefits).toContain("Trading Playbook");
  expect(benefits).not.toContain("AI Trading Memory and recurring pattern insights");
});

test("billing benefits show one compact Trading Memory item when TRADING_MEMORY is enabled", () => {
  const benefits = getEnabledProBenefits(memoryAvailability);

  expect(benefits.filter((benefit) => benefit.includes("Trading Memory"))).toEqual([
    "AI Trading Memory and recurring pattern insights",
  ]);
  expect(benefits).not.toContain("Similar trade comparisons");
  expect(benefits).not.toContain("Supporting trade evidence");
});

test("billing benefits fail closed when feature availability is missing", () => {
  const benefits = getEnabledProBenefits(null);

  expect(benefits).toEqual(marketingPricing.pro.benefits);
  expect(benefits).not.toContain("AI Trading Memory and recurring pattern insights");
  expect(benefits).not.toContain("Trading DNA");
});

test("free and active Pro subscribers use the same gated Pro benefit source", () => {
  const freeUserBenefits = getEnabledProBenefits(onlyMemoryAvailability);
  const proSubscriberBenefits = getEnabledProBenefits(onlyMemoryAvailability);

  expect(freeUserBenefits).toEqual(proSubscriberBenefits);
  expect(proSubscriberBenefits).toContain("AI Trading Memory and recurring pattern insights");
});

test("benefits have no duplicate Trading Memory item and no empty conditional rows", () => {
  const benefits = getEnabledProBenefits(memoryAvailability);
  const memoryItems = benefits.filter((benefit) => benefit.includes("Trading Memory"));

  expect(memoryItems).toHaveLength(1);
  expect(benefits.every((benefit) => benefit.trim().length > 0)).toBe(true);
});

test("public marketing pricing can show detailed Trading Memory benefits separately", () => {
  const benefits = getEnabledProMarketingBenefits(memoryAvailability);

  expect(benefits).toContain("AI Trading Memory");
  expect(benefits).toContain("Similar trade comparisons");
  expect(benefits).toContain("Recurring pattern insights");
  expect(benefits).toContain("Supporting trade evidence");
});
