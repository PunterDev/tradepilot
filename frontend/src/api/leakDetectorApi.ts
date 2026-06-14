import { api } from "./axios";
import type { LeakDetectorResponse } from "../types/leakDetectorTypes";

export function getLeakDetector(accountId: number) {
  return api
    .get<LeakDetectorResponse>(`/ai/leak-detector?accountId=${accountId}`)
    .then((res) => res.data);
}

export function regenerateLeakDetector(accountId: number) {
  return api
    .post<LeakDetectorResponse>("/ai/leak-detector/regenerate", { accountId })
    .then((res) => res.data);
}
