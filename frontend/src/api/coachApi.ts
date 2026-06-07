import type {
  CoachGenerateRequest,
  CoachSnapshot,
  CoachSnapshotType,
} from "../types/coachTypes";
import { api } from "./axios";

export function getDailyCoach(accountId: number, date: string) {
  return api
    .get<CoachSnapshot>(`/coach/daily?accountId=${accountId}&date=${date}`)
    .then((res) => res.data);
}

export function getWeeklyCoach(accountId: number, weekStart: string) {
  return api
    .get<CoachSnapshot>(
      `/coach/weekly?accountId=${accountId}&weekStart=${weekStart}`
    )
    .then((res) => res.data);
}

export function generateDailyCoach(payload: CoachGenerateRequest) {
  return api
    .post<CoachSnapshot>("/coach/daily/generate", payload)
    .then((res) => res.data);
}

export function generateWeeklyCoach(payload: CoachGenerateRequest) {
  return api
    .post<CoachSnapshot>("/coach/weekly/generate", payload)
    .then((res) => res.data);
}

export function getCoachSnapshots(
  accountId: number,
  type: CoachSnapshotType
) {
  return api
    .get<CoachSnapshot[]>(`/coach/snapshots?accountId=${accountId}&type=${type}`)
    .then((res) => res.data);
}
