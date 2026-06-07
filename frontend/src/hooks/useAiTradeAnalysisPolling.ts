import { useEffect, useRef, useState } from "react";
import {
  getAiTradeAnalysisStatus,
  startAiTradeAnalysisAsync,
} from "../api/aiTradeAnalysisApi";
import type {
  AiTradeAnalysisJobResponse,
  AiTradeAnalysisRequest,
  AiTradeAnalysisResponse,
  AiTradeAnalysisStatus,
} from "../types/ai";

const loadingSteps = [
  "Reading trade execution...",
  "Checking risk and discipline...",
  "Reviewing journal context...",
  "Analyzing chart screenshot if provided...",
  "Building improvement plan...",
];

function getJobId(job: AiTradeAnalysisJobResponse): number | undefined {
  return job.analysisId ?? job.reviewId ?? job.id;
}

function extractAnalysis(job: AiTradeAnalysisJobResponse): AiTradeAnalysisResponse | null {
  return job.analysis ?? job.review ?? null;
}

export function useAiTradeAnalysisPolling() {
  const [status, setStatus] = useState<AiTradeAnalysisStatus>("IDLE");
  const [analysis, setAnalysis] = useState<AiTradeAnalysisResponse | null>(null);
  const [error, setError] = useState("");
  const [loadingText, setLoadingText] = useState(loadingSteps[0]);
  const [retryCount, setRetryCount] = useState(0);
  const [cacheMessage, setCacheMessage] = useState("");

  const intervalRef = useRef<number | null>(null);
  const textIntervalRef = useRef<number | null>(null);
  const timeoutRef = useRef<number | null>(null);
  const lastRequestRef = useRef<AiTradeAnalysisRequest | null>(null);
  const lastChartImageRef = useRef<File | null>(null);

  const isLoading = status === "STARTING" || status === "POLLING";
  const isFailed = status === "FAILED";
  const isCompleted = status === "COMPLETED";
  const hasCachedReview = Boolean(analysis && isLoading);

  function clearTimers() {
    if (intervalRef.current) window.clearInterval(intervalRef.current);
    if (textIntervalRef.current) window.clearInterval(textIntervalRef.current);
    if (timeoutRef.current) window.clearTimeout(timeoutRef.current);

    intervalRef.current = null;
    textIntervalRef.current = null;
    timeoutRef.current = null;
  }

  function resetForNewRun() {
    clearTimers();
    setStatus("IDLE");
    setError("");
    setLoadingText(loadingSteps[0]);
    setRetryCount(0);
    setCacheMessage("");
  }

  async function start(request: AiTradeAnalysisRequest, chartImage?: File | null) {
    const previousAnalysis = analysis;
    resetForNewRun();

    if (previousAnalysis) {
      setAnalysis(previousAnalysis);
      setCacheMessage("Showing previous analysis while TradePilot generates a refreshed version.");
    }

    lastRequestRef.current = request;
    lastChartImageRef.current = chartImage ?? null;

    try {
      setStatus("STARTING");

      const job = await startAiTradeAnalysisAsync({ request, chartImage });
      const jobId = getJobId(job);
      const immediateAnalysis = extractAnalysis(job);

      if (immediateAnalysis || job.status === "COMPLETED") {
        setAnalysis(immediateAnalysis);
        setStatus("COMPLETED");
        setCacheMessage(job.message ?? "Cached AI analysis returned.");
        return;
      }

      if (!jobId) {
        throw new Error("AI analysis ID was not returned from backend.");
      }

      setStatus("POLLING");

      let stepIndex = 0;
      textIntervalRef.current = window.setInterval(() => {
        stepIndex = (stepIndex + 1) % loadingSteps.length;
        setLoadingText(loadingSteps[stepIndex]);
      }, 2500);

      timeoutRef.current = window.setTimeout(() => {
        clearTimers();
        setStatus("FAILED");
        setError("AI analysis timed out. Please try again.");
      }, 90000);

      intervalRef.current = window.setInterval(async () => {
        try {
          const latestJob = await getAiTradeAnalysisStatus(jobId);
          const latestAnalysis = extractAnalysis(latestJob);

          if (latestAnalysis) {
            setAnalysis(latestAnalysis);
          }

          if (latestJob.status === "COMPLETED") {
            clearTimers();
            setStatus("COMPLETED");
            setCacheMessage("");
          }

          if (latestJob.status === "FAILED") {
            clearTimers();
            setStatus("FAILED");
            setError(latestJob.message || latestJob.errorMessage || "AI analysis failed.");
          }
        } catch (err) {
          clearTimers();
          setStatus("FAILED");
          setError(err instanceof Error ? err.message : "Unable to check AI analysis status.");
        }
      }, 2000);
    } catch (err) {
      clearTimers();
      setStatus("FAILED");
      setError(err instanceof Error ? err.message : "AI analysis failed.");
    }
  }

  function retry() {
    if (!lastRequestRef.current) return;
    setRetryCount((value) => value + 1);
    void start(lastRequestRef.current, lastChartImageRef.current);
  }

  useEffect(() => {
    return () => clearTimers();
  }, []);

  return {
    status,
    analysis,
    error,
    loadingText,
    retryCount,
    hasCachedReview,
    cacheMessage,
    isLoading,
    isFailed,
    isCompleted,
    start,
    retry,
    clear: resetForNewRun,
  };
}
