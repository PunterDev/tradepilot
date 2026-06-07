import { useEffect, useRef, useState } from "react";
import {
    getAiTradeReviewStatus,
    startAiTradeReviewAsync,
} from "../api/aiApi";
import type {
    AiTradeReviewRequest,
    AiTradeReviewResponse,
} from "../types/ai";

type PollingState = "IDLE" | "STARTING" | "POLLING" | "COMPLETED" | "FAILED";

type AiTradeReviewJobResponse = {
    reviewId?: number;
    tradeId?: number;
    status?: "PENDING" | "COMPLETED" | "FAILED" | string;
    review?: AiTradeReviewResponse | null;
    message?: string;
    errorMessage?: string;
};

type StartOptions = {
    forceRefresh?: boolean;
};

const loadingSteps = [
    "Analyzing trade execution...",
    "Reviewing discipline and risk...",
    "Finding repeated mistakes...",
    "Generating improvement plan...",
];

const CACHE_PREFIX = "tradepilot:ai-review:";
const POLL_INTERVAL_MS = 2_000;
const LOADING_TEXT_INTERVAL_MS = 2_500;
const MAX_POLL_ATTEMPTS = 45; // about 90 seconds
const MAX_STATUS_RETRIES = 2;

function normalizeReviewResponse(
    response: AiTradeReviewResponse | AiTradeReviewJobResponse | null | undefined
): AiTradeReviewResponse | null {
    if (!response) return null;

    const maybeJob = response as AiTradeReviewJobResponse;

    if (maybeJob.review) {
        return {
            ...maybeJob.review,
            id: (maybeJob.review as any).id ?? maybeJob.reviewId,
            tradeId: maybeJob.review.tradeId ?? maybeJob.tradeId,
            status: maybeJob.review.status ?? maybeJob.status,
            errorMessage: maybeJob.review.errorMessage ?? maybeJob.errorMessage,
        } as AiTradeReviewResponse;
    }

    return response as AiTradeReviewResponse;
}

function getReviewId(response: AiTradeReviewResponse | AiTradeReviewJobResponse) {
    return (response as any).id ?? (response as AiTradeReviewJobResponse).reviewId;
}

function getResponseStatus(response: AiTradeReviewResponse | AiTradeReviewJobResponse) {
    return (response as any).status ?? (response as AiTradeReviewJobResponse).review?.status;
}

function simpleHash(value: string) {
    let hash = 0;

    for (let i = 0; i < value.length; i += 1) {
        hash = (hash << 5) - hash + value.charCodeAt(i);
        hash |= 0;
    }

    return Math.abs(hash).toString(36);
}

function getCacheKey(request: AiTradeReviewRequest) {
    // Includes journal text and a few trade values so cached reviews refresh when the trade context changes.
    const trade = (request as any).trade ?? {};
    const keyPayload = JSON.stringify({
        tradeId: request.tradeId,
        journalText: request.journalText ?? "",
        symbol: trade.symbol,
        pnl: trade.pnl,
        entryTime: trade.entryTime ?? trade.openTime ?? trade.createdAt,
        exitTime: trade.exitTime ?? trade.closeTime ?? trade.updatedAt,
    });

    return `${CACHE_PREFIX}${request.tradeId}:${simpleHash(keyPayload)}`;
}

function readCachedReview(cacheKey: string): AiTradeReviewResponse | null {
    try {
        const raw = window.sessionStorage.getItem(cacheKey);
        return raw ? (JSON.parse(raw) as AiTradeReviewResponse) : null;
    } catch {
        return null;
    }
}

function writeCachedReview(cacheKey: string, review: AiTradeReviewResponse) {
    try {
        window.sessionStorage.setItem(cacheKey, JSON.stringify(review));
    } catch {
        // Ignore cache failures. The AI review still works without sessionStorage.
    }
}

export function useAiReviewPolling() {
    const [status, setStatus] = useState<PollingState>("IDLE");
    const [review, setReview] = useState<AiTradeReviewResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loadingText, setLoadingText] = useState(loadingSteps[0]);
    const [retryCount, setRetryCount] = useState(0);
    const [hasCachedReview, setHasCachedReview] = useState(false);
    const [cacheMessage, setCacheMessage] = useState<string | null>(null);

    const intervalRef = useRef<number | null>(null);
    const textIntervalRef = useRef<number | null>(null);
    const lastRequestRef = useRef<AiTradeReviewRequest | null>(null);
    const pollAttemptsRef = useRef(0);
    const statusRetryRef = useRef(0);
    const activeCacheKeyRef = useRef<string | null>(null);

    const clearTimers = () => {
        if (intervalRef.current) window.clearInterval(intervalRef.current);
        if (textIntervalRef.current) window.clearInterval(textIntervalRef.current);

        intervalRef.current = null;
        textIntervalRef.current = null;
    };

    const reset = () => {
        clearTimers();
        setStatus("IDLE");
        setReview(null);
        setError(null);
        setLoadingText(loadingSteps[0]);
        setRetryCount(0);
        setHasCachedReview(false);
        setCacheMessage(null);
        pollAttemptsRef.current = 0;
        statusRetryRef.current = 0;
        activeCacheKeyRef.current = null;
    };

    const start = async (
        request: AiTradeReviewRequest,
        options: StartOptions = {}
    ) => {
        clearTimers();

        lastRequestRef.current = request;
        pollAttemptsRef.current = 0;
        statusRetryRef.current = 0;
        setError(null);
        setLoadingText(loadingSteps[0]);

        const cacheKey = getCacheKey(request);
        activeCacheKeyRef.current = cacheKey;

        if (!options.forceRefresh) {
            const cachedReview = readCachedReview(cacheKey);

            if (cachedReview) {
                setReview(cachedReview);
                setHasCachedReview(true);
                setCacheMessage("Showing cached review while checking for a fresh update...");
            } else {
                setReview(null);
                setHasCachedReview(false);
                setCacheMessage(null);
            }
        } else {
            setReview(null);
            setHasCachedReview(false);
            setCacheMessage(null);
        }

        try {
            setStatus("STARTING");

            const pendingReview = await startAiTradeReviewAsync(
                options.forceRefresh ? { ...request, forceRefresh: true } : request
            );
            const reviewId = getReviewId(pendingReview as any);

            if (!reviewId) {
                throw new Error("Review ID was not returned from backend.");
            }

            const normalizedPendingReview = normalizeReviewResponse(pendingReview as any);
            if (normalizedPendingReview && !hasCachedReview) {
                setReview(normalizedPendingReview);
            }

            setStatus("POLLING");

            let stepIndex = 0;

            textIntervalRef.current = window.setInterval(() => {
                stepIndex = (stepIndex + 1) % loadingSteps.length;
                setLoadingText(loadingSteps[stepIndex]);
            }, LOADING_TEXT_INTERVAL_MS);

            const pollStatus = async () => {
                try {
                    pollAttemptsRef.current += 1;

                    const latestResponse = await getAiTradeReviewStatus(reviewId);
                    const latestReview = normalizeReviewResponse(latestResponse as any);
                    const latestStatus = getResponseStatus(latestResponse as any);

                    statusRetryRef.current = 0;

                    if (latestReview) {
                        setReview(latestReview);
                    }

                    if (latestStatus === "COMPLETED" || latestReview?.status === "COMPLETED") {
                        clearTimers();
                        setStatus("COMPLETED");
                        setHasCachedReview(false);
                        setCacheMessage("Review updated.");

                        if (latestReview && activeCacheKeyRef.current) {
                            writeCachedReview(activeCacheKeyRef.current, latestReview);
                        }

                        return;
                    }

                    if (latestStatus === "FAILED" || latestReview?.status === "FAILED") {
                        clearTimers();
                        setStatus("FAILED");
                        setError(
                            (latestResponse as any).errorMessage ||
                            latestReview?.errorMessage ||
                            "AI review failed."
                        );
                        return;
                    }

                    if (pollAttemptsRef.current >= MAX_POLL_ATTEMPTS) {
                        clearTimers();
                        setStatus("FAILED");
                        setError("AI review is taking too long. Please try again.");
                    }
                } catch {
                    statusRetryRef.current += 1;

                    if (statusRetryRef.current <= MAX_STATUS_RETRIES) {
                        setRetryCount(statusRetryRef.current);
                        setLoadingText(`Temporary connection issue. Retrying ${statusRetryRef.current}/${MAX_STATUS_RETRIES}...`);
                        return;
                    }

                    clearTimers();
                    setStatus("FAILED");
                    setError("Unable to check AI review status.");
                }
            };

            await pollStatus();
            intervalRef.current = window.setInterval(pollStatus, POLL_INTERVAL_MS);
        } catch (err) {
            clearTimers();
            setStatus("FAILED");
            setError(err instanceof Error ? err.message : "AI review failed.");
        }
    };

    const retry = () => {
        if (!lastRequestRef.current) return;
        setRetryCount((current) => current + 1);
        start(lastRequestRef.current, { forceRefresh: true });
    };

    useEffect(() => {
        return () => clearTimers();
    }, []);

    return {
        status,
        review,
        error,
        loadingText,
        retryCount,
        hasCachedReview,
        cacheMessage,
        isLoading: status === "STARTING" || status === "POLLING",
        isCompleted: status === "COMPLETED",
        isFailed: status === "FAILED",
        start,
        retry,
        reset,
    };
}
