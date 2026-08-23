import { api } from "./axios";
import type { PerformanceTimelineResponse } from "../types/performanceTimelineTypes";

export function getPerformanceTimeline(accountId: number) {
  return api
    .get<PerformanceTimelineResponse>(`/ai/performance-timeline?accountId=${accountId}`)
    .then((res) => res.data);
}

export function regeneratePerformanceTimeline(accountId: number) {
  return api
    .post<PerformanceTimelineResponse>(`/ai/performance-timeline/regenerate?accountId=${accountId}`)
    .then((res) => res.data);
}
