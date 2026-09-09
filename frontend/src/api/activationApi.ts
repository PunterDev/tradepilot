import { api } from "./axios";
import type { ActivationStatus } from "../types/activation";

export async function getActivationStatus(): Promise<ActivationStatus> {
  const response = await api.get<ActivationStatus>("/activation/status");
  return response.data;
}
