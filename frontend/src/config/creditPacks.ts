import type { CreditPackId } from "../api/billingApi";

export type CreditPackConfig = {
  id: CreditPackId;
  name: string;
  credits: number;
  amountCents: number;
  copy: string;
};

export const creditPacks: CreditPackConfig[] = [
  { id: "STARTER_25", name: "Starter", credits: 25, amountCents: 999, copy: "A quick top-up for a heavy review week." },
  { id: "POWER_75", name: "Power", credits: 75, amountCents: 2499, copy: "More room for coaching, refreshes, and leak checks." },
  { id: "ELITE_200", name: "Elite", credits: 200, amountCents: 5999, copy: "A deep reserve for active traders and multiple accounts." },
];

export function formatCreditPackPrice(amountCents: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(amountCents / 100);
}
