export type ManualDirection = "LONG" | "SHORT";
export type ManualPosition = { direction: ManualDirection; entry: number; stop: number; target: number };
export type ManualPositionCalculation = { valid: boolean; validationMessage: string | null; riskPoints?: number; rewardPoints?: number; riskTicks?: number; rewardTicks?: number; rewardRiskRatio?: number; potentialR?: number };
export const instrumentMetadata: Record<string, { tickSize: number }>;
export function tickSizeFor(instrument: string): number | null;
export function snapToTick(price: number, tickSize: number): number | null;
export function calculateManualPosition(position: ManualPosition | null, tickSize: number | null): ManualPositionCalculation;
export function targetForR(position: ManualPosition | null, targetR: number, tickSize: number | null): number | null;
