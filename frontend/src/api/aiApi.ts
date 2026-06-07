import { api } from "./api";
import type {
  AiTradeAnalysisRequest,
  AiTradeAnalysisJobResponse,
  AiTradeReviewJobResponse,
  AiTradeReviewRequest,
} from "../types/ai";

export async function startAiTradeAnalysisAsync(params: {
  request: AiTradeAnalysisRequest;
  chartImage?: File | null;
}): Promise<AiTradeAnalysisJobResponse> {
  const formData = new FormData();

  formData.append(
    "request",
    new Blob([JSON.stringify(params.request)], {
      type: "application/json",
    })
  );

  if (params.chartImage) {
    formData.append("chartImage", params.chartImage);
  }

  const response = await api.post<AiTradeAnalysisJobResponse>(
    "/ai/trade-analysis/async",
    formData
  );

  return response.data;
}

export async function getAiTradeAnalysisStatus(
  analysisId: number
): Promise<AiTradeAnalysisJobResponse> {
  const response = await api.get<AiTradeAnalysisJobResponse>(
    `/ai/trade-analysis/status/${analysisId}`
  );

  return response.data;
}

export async function startAiTradeReviewAsync(
  request: AiTradeReviewRequest
): Promise<AiTradeReviewJobResponse> {
  const response = await api.post<AiTradeReviewJobResponse>(
    "/ai/trade-review/async",
    request
  );

  return response.data;
}

export async function getAiTradeReviewStatus(
  reviewId: number
): Promise<AiTradeReviewJobResponse> {
  const response = await api.get<AiTradeReviewJobResponse>(
    `/ai/trade-review/status/${reviewId}`
  );

  return response.data;
}
