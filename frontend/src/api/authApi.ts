import { api, TOKEN_STORAGE_KEY } from "./axios";

export async function autoLogin(): Promise<string> {
  const existingToken = localStorage.getItem(TOKEN_STORAGE_KEY);

  if (existingToken) {
    return existingToken;
  }

  const res = await api.get("/dev/token");
  const token = res.data.token;

  localStorage.setItem(TOKEN_STORAGE_KEY, token);

  return token;
}

export function logout() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}