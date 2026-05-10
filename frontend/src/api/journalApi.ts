import { api } from "./axios";

export type JournalRequest = {
  accountId: number | null;
  journalDate: string;
  mood: string;
  setupQuality: string;
  mistakes: string;
  lessonLearned: string;
  notes: string;
};

export async function saveJournal(payload: JournalRequest) {
  const res = await api.post("/journals", payload);
  return res.data;
}

export async function getJournalByDate(accountId: number | null, date: string) {
  const res = await api.get("/journals/by-date", {
    params: {
      accountId,
      date,
    },
  });

  return res.data;
}