import { api } from "./api";
import type { FeatureAvailability } from "../config/featureFlags";

export async function getFeatureAvailability() {
  const response = await api.get<FeatureAvailability>("/features");
  return response.data;
}
