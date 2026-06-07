import { api } from "./axios";
import type { PagedResponse, PageParams } from "./pagination";

export type ContactMessageStatus = "NEW" | "OPEN" | "REPLIED" | "ARCHIVED";

export type AdminContactMessage = {
  id: number;
  name: string;
  email: string;
  subject: string;
  message: string;
  status: ContactMessageStatus;
  emailSent: boolean;
  emailError?: string | null;
  remoteAddress?: string | null;
  userAgent?: string | null;
  adminNotes?: string | null;
  leadSource?: string | null;
  repliedAt?: string | null;
  lastViewedAt?: string | null;
  replyBody?: string | null;
  replySubject?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ContactMessageSummary = {
  total: number;
  newCount: number;
  openCount: number;
  repliedCount: number;
  archivedCount: number;
  failedEmailCount: number;
};

export async function getContactMessages(
  status?: ContactMessageStatus | "ALL",
  emailFailed?: boolean,
  pageParams: PageParams = {}
): Promise<PagedResponse<AdminContactMessage>> {
  const res = await api.get<PagedResponse<AdminContactMessage>>("/admin/contact-messages", {
    params: {
      ...(status && status !== "ALL" ? { status } : {}),
      ...(emailFailed ? { emailFailed: true } : {}),
      ...pageParams,
    },
  });
  return res.data;
}

export async function getContactMessage(id: number): Promise<AdminContactMessage> {
  const res = await api.get<AdminContactMessage>(`/admin/contact-messages/${id}`);
  return res.data;
}

export async function updateContactMessageStatus(
  id: number,
  status: ContactMessageStatus
): Promise<AdminContactMessage> {
  const res = await api.patch<AdminContactMessage>(`/admin/contact-messages/${id}/status`, {
    status,
  });
  return res.data;
}

export async function updateContactMessageNotes(
  id: number,
  adminNotes: string
): Promise<AdminContactMessage> {
  const res = await api.patch<AdminContactMessage>(`/admin/contact-messages/${id}/notes`, {
    adminNotes,
  });
  return res.data;
}

export async function replyToContactMessage(
  id: number,
  subject: string,
  message: string
): Promise<AdminContactMessage> {
  const res = await api.post<AdminContactMessage>(`/admin/contact-messages/${id}/reply`, {
    subject,
    message,
  });
  return res.data;
}

export async function getContactMessageSummary(): Promise<ContactMessageSummary> {
  const res = await api.get<ContactMessageSummary>("/admin/contact-messages/summary");
  return res.data;
}
