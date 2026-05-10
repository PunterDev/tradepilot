import { api } from "./axios";

export type TradingAccount = {
  id: number;
  name: string;
  propFirm: string;
  accountNumber?: string;
  startingBalance?: number;
  status: string;
};

export type CreateTradingAccountRequest = {
  name: string;
  propFirm: string;
  accountNumber?: string;
  startingBalance?: number;
  status: string;
};

export async function getAccounts(): Promise<TradingAccount[]> {
  const res = await api.get("/accounts");
  return res.data;
}

export async function createAccount(
  payload: CreateTradingAccountRequest
): Promise<TradingAccount> {
  const res = await api.post("/accounts", payload);
  return res.data;
}

export async function deleteAccount(accountId: number): Promise<void> {
  await api.delete(`/accounts/${accountId}`);
}