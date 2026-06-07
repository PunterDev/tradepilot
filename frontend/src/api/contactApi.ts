import { api } from "./axios";

export type ContactRequest = {
  name: string;
  email: string;
  subject: string;
  message: string;
};

export type ContactResponse = {
  success: boolean;
  message: string;
};

export async function sendContactMessage(payload: ContactRequest): Promise<ContactResponse> {
  const res = await api.post<ContactResponse>("/contact", payload);
  return res.data;
}
