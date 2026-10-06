export const instrumentMetadata = Object.freeze({
  NQ: Object.freeze({ tickSize: 0.25 }),
  ES: Object.freeze({ tickSize: 0.25 }),
});

export function tickSizeFor(instrument) {
  return instrumentMetadata[instrument]?.tickSize ?? null;
}

export function snapToTick(price, tickSize) {
  if (!Number.isFinite(price) || !Number.isFinite(tickSize) || tickSize <= 0) return null;
  return Number((Math.round(price / tickSize) * tickSize).toFixed(8));
}

export function calculateManualPosition(position, tickSize) {
  if (!position || !tickSize) return { valid: false, validationMessage: "Choose a supported instrument and position direction." };
  const { direction, entry, stop, target } = position;
  if (![entry, stop, target].every(Number.isFinite)) return { valid: false, validationMessage: "Enter a valid price for entry, stop, and target." };
  const long = direction === "LONG";
  if (long && !(target > entry && entry > stop)) return { valid: false, validationMessage: "Invalid long position: stop must be below entry and target must be above entry." };
  if (!long && direction !== "SHORT") return { valid: false, validationMessage: "Choose Long Position or Short Position." };
  if (!long && !(stop > entry && entry > target)) return { valid: false, validationMessage: "Invalid short position: stop must be above entry and target must be below entry." };
  const riskPoints = long ? entry - stop : stop - entry;
  const rewardPoints = long ? target - entry : entry - target;
  if (riskPoints <= 0) return { valid: false, validationMessage: "Risk must be greater than zero." };
  const rewardRiskRatio = rewardPoints / riskPoints;
  return { valid: true, validationMessage: null, riskPoints, rewardPoints, riskTicks: riskPoints / tickSize, rewardTicks: rewardPoints / tickSize, rewardRiskRatio, potentialR: rewardRiskRatio };
}

export function targetForR(position, targetR, tickSize) {
  if (!position || !Number.isFinite(targetR) || targetR < 0 || !tickSize) return null;
  const risk = position.direction === "LONG" ? position.entry - position.stop : position.stop - position.entry;
  if (!Number.isFinite(risk) || risk <= 0) return null;
  const target = position.direction === "LONG" ? position.entry + risk * targetR : position.entry - risk * targetR;
  return snapToTick(target, tickSize);
}
