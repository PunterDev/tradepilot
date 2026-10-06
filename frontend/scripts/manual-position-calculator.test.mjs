import assert from "node:assert/strict";
import test from "node:test";
import { calculateManualPosition, snapToTick, targetForR } from "../src/features/admin/backtest/manualPositionCalculator.mjs";

test("calculates a valid long position", () => { const result = calculateManualPosition({ direction: "LONG", entry: 100, stop: 90, target: 120 }, .25); assert.equal(result.valid, true); assert.equal(result.riskPoints, 10); assert.equal(result.rewardPoints, 20); assert.equal(result.potentialR, 2); });
test("calculates a valid short position", () => { const result = calculateManualPosition({ direction: "SHORT", entry: 100, stop: 110, target: 80 }, .25); assert.equal(result.valid, true); assert.equal(result.riskPoints, 10); assert.equal(result.rewardPoints, 20); assert.equal(result.potentialR, 2); });
test("rejects invalid long and short arrangements", () => { assert.equal(calculateManualPosition({ direction: "LONG", entry: 100, stop: 101, target: 120 }, .25).valid, false); assert.equal(calculateManualPosition({ direction: "SHORT", entry: 100, stop: 99, target: 80 }, .25).valid, false); assert.equal(calculateManualPosition({ direction: "SHORT", entry: 100, stop: 110, target: 101 }, .25).valid, false); });
test("snaps to the nearest tick and derives a target from R", () => { assert.equal(snapToTick(29547.13, .25), 29547.25); assert.equal(targetForR({ direction: "LONG", entry: 100, stop: 90, target: 110 }, 2, .25), 120); });
test("calculates the validated NQ example without premature rounding", () => { const result = calculateManualPosition({ direction: "LONG", entry: 29547.125, stop: 29530.75, target: 29555.75 }, .25); assert.equal(result.valid, true); assert.ok(Math.abs(result.potentialR - .5267175573) < .00001); });
