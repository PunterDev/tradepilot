import { api } from "./axios";

export async function seedDemoData(): Promise<void> {
  await api.post("/dev/seed-demo-data");
}

export async function clearDemoData(): Promise<void> {
  await api.post("/dev/clear-demo-data");
}

export async function mockUpgradePro(): Promise<void> {
  await api.post("/dev/mock-upgrade-pro");
}

export async function mockDowngradeFree(): Promise<void> {
  await api.post("/dev/mock-downgrade-free");
}

export async function backfillFeatureUsage(): Promise<void> {
  await api.post("/dev/feature-usage/backfill");
}
