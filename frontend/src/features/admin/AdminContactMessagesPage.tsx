import { useEffect, useMemo, useState, type CSSProperties } from "react";
import axios from "axios";
import {
  getContactMessage,
  getContactMessageSummary,
  getContactMessages,
  replyToContactMessage,
  updateContactMessageNotes,
  updateContactMessageStatus,
  type AdminContactMessage,
  type ContactMessageStatus,
  type ContactMessageSummary,
} from "../../api/adminContactApi";
import type { PagedResponse } from "../../api/pagination";
import { button, card, input, label, mutedText } from "../../styles/dashboardStyles";

type Filter = ContactMessageStatus | "ALL" | "FAILED";

const filters: Filter[] = ["ALL", "NEW", "OPEN", "REPLIED", "ARCHIVED", "FAILED"];

export default function AdminContactMessagesPage() {
  const [messages, setMessages] = useState<AdminContactMessage[]>([]);
  const [pageInfo, setPageInfo] = useState<PagedResponse<AdminContactMessage> | null>(null);
  const [summary, setSummary] = useState<ContactMessageSummary | null>(null);
  const [selected, setSelected] = useState<AdminContactMessage | null>(null);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [notes, setNotes] = useState("");
  const [replyOpen, setReplyOpen] = useState(false);
  const [replySubject, setReplySubject] = useState("");
  const [replyBody, setReplyBody] = useState("");
  const [replyError, setReplyError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [submittingReply, setSubmittingReply] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    void load();
  }, [filter, page, pageSize]);

  useEffect(() => {
    setNotes(selected?.adminNotes ?? "");
  }, [selected?.id]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [nextSummary, nextMessages] = await Promise.all([
        getContactMessageSummary(),
        getContactMessages(filter === "FAILED" ? "ALL" : filter, filter === "FAILED", { page, size: pageSize }),
      ]);
      setSummary(nextSummary);
      setPageInfo(nextMessages);
      setMessages(nextMessages.content);
      setSelected((current) => {
        if (!current) return nextMessages.content[0] ?? null;
        return nextMessages.content.find((message) => message.id === current.id) ?? nextMessages.content[0] ?? null;
      });
    } catch {
      setError("Unable to load contact messages.");
    } finally {
      setLoading(false);
    }
  }

  async function openMessage(message: AdminContactMessage) {
    setError("");
    try {
      const next = await getContactMessage(message.id);
      setSelected(next);
      setMessages((current) => current.map((item) => item.id === next.id ? next : item));
      await refreshSummary();
    } catch {
      setError("Unable to open message.");
    }
  }

  async function refreshSummary() {
    setSummary(await getContactMessageSummary());
  }

  async function refreshCrmState(preferredMessageId?: number, fallbackMessage?: AdminContactMessage) {
    const [nextSummary, nextMessages] = await Promise.all([
      getContactMessageSummary(),
      getContactMessages(filter === "FAILED" ? "ALL" : filter, filter === "FAILED", { page, size: pageSize }),
    ]);
    setSummary(nextSummary);
    setPageInfo(nextMessages);
    setMessages(nextMessages.content);

    if (!preferredMessageId) {
      setSelected(nextMessages.content[0] ?? null);
      return;
    }

    const listMatch = nextMessages.content.find((message) => message.id === preferredMessageId);
    if (listMatch) {
      setSelected(listMatch);
      return;
    }

    setSelected(fallbackMessage ?? null);
  }

  async function changeStatus(nextStatus: ContactMessageStatus) {
    if (!selected) return;
    setSaving(true);
    setError("");
    try {
      const updated = await updateContactMessageStatus(selected.id, nextStatus);
      setSelected(updated);
      setMessages((current) => current.map((item) => item.id === updated.id ? updated : item));
      await refreshSummary();
    } catch {
      setError("Unable to update message status.");
    } finally {
      setSaving(false);
    }
  }

  async function saveNotes() {
    if (!selected) return;
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updated = await updateContactMessageNotes(selected.id, notes);
      setSelected(updated);
      setMessages((current) => current.map((item) => item.id === updated.id ? updated : item));
      setNotice("Notes saved.");
    } catch {
      setError("Unable to save notes.");
    } finally {
      setSaving(false);
    }
  }

  function startReply() {
    if (!selected) return;
    setReplySubject(selected.replySubject || `Re: ${selected.subject}`);
    setReplyBody(selected.replyBody || "");
    setReplyError("");
    setReplyOpen(true);
    setError("");
    setNotice("");
  }

  function closeReplyModal() {
    setReplyOpen(false);
    setReplySubject("");
    setReplyBody("");
    setReplyError("");
  }

  async function sendReply() {
    if (!selected || !replySubject.trim() || !replyBody.trim()) return;
    setSubmittingReply(true);
    setError("");
    setReplyError("");
    setNotice("");
    try {
      const updated = await replyToContactMessage(selected.id, replySubject.trim(), replyBody.trim());
      setSelected(updated);
      setMessages((current) => current.map((item) => item.id === updated.id ? updated : item));
      closeReplyModal();
      setNotice("Reply sent successfully.");
      await refreshCrmState(updated.id, updated);
    } catch (err) {
      setReplyError(getErrorMessage(err));
    } finally {
      setSubmittingReply(false);
    }
  }

  const filteredMessages = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return messages;
    return messages.filter((message) =>
      [message.name, message.email, message.subject].some((value) => value.toLowerCase().includes(needle))
    );
  }, [messages, query]);

  return (
    <div style={pageWrap}>
      <section style={header}>
        <div>
          <p style={eyebrow}>Mini CRM</p>
          <h1 style={title}>Contact Messages</h1>
          <p style={mutedText}>Triage leads, track replies, and keep notes without losing failed email submissions.</p>
        </div>
      </section>

      <section style={summaryGrid}>
        <Metric label="Total" value={summary?.total ?? 0} />
        <Metric label="New" value={summary?.newCount ?? 0} />
        <Metric label="Open" value={summary?.openCount ?? 0} />
        <Metric label="Replied" value={summary?.repliedCount ?? 0} />
        <Metric label="Failed Email" value={summary?.failedEmailCount ?? 0} />
      </section>

      <section style={controlsCard}>
        <div style={filterPills}>
          {filters.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => {
                setFilter(item);
                setPage(0);
              }}
              style={filter === item ? activePill : pill}
            >
              {item === "FAILED" ? "Failed Email" : item}
            </button>
          ))}
        </div>
        <div style={{ flex: 1, minWidth: 220 }}>
          <label style={label}>Search</label>
          <input style={input} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="name, email, or subject" />
        </div>
        <div style={{ minWidth: 132 }}>
          <label style={label}>Page size</label>
          <select
            style={input}
            value={pageSize}
            onChange={(event) => {
              setPageSize(Number(event.target.value));
              setPage(0);
            }}
          >
            {[25, 50, 100].map((size) => <option key={size} value={size}>{size}</option>)}
          </select>
        </div>
      </section>

      {notice && <div style={successBox}>{notice}</div>}
      {error && <div style={errorBox}>{error}</div>}

      <section style={contentGrid}>
        <div style={listCard}>
          {loading ? (
            <div style={emptyState}>Loading messages...</div>
          ) : filteredMessages.length === 0 ? (
            <div style={emptyState}>No contact messages found.</div>
          ) : (
            filteredMessages.map((message) => (
              <button
                key={message.id}
                type="button"
                onClick={() => void openMessage(message)}
                style={{
                  ...messageRow,
                  borderColor: selected?.id === message.id ? "rgba(45,212,191,0.52)" : "rgba(148,163,184,0.13)",
                  background: selected?.id === message.id ? "rgba(20,184,166,0.12)" : "rgba(2,6,23,0.42)",
                }}
              >
                <div style={rowTop}>
                  <strong>{message.subject}</strong>
                  <StatusBadge status={message.status} />
                </div>
                <span>{message.name} · {message.email}</span>
                <small>{formatDate(message.createdAt)}</small>
                {!message.emailSent && <span style={warningBadge}>Failed Email</span>}
              </button>
            )).concat([
              <PaginationControls
                key="pagination"
                page={pageInfo?.page ?? page}
                totalPages={pageInfo?.totalPages ?? 1}
                totalElements={pageInfo?.totalElements ?? filteredMessages.length}
                onPrevious={() => setPage((current) => Math.max(0, current - 1))}
                onNext={() => setPage((current) => Math.min((pageInfo?.totalPages ?? 1) - 1, current + 1))}
              />
            ])
          )}
        </div>

        <article style={detailCard}>
          {selected ? (
            <>
              <div style={rowTop}>
                <div>
                  <h2 style={{ margin: 0, color: "#f8fafc" }}>{selected.subject}</h2>
                  <p style={{ ...mutedText, margin: "6px 0 0" }}>{selected.name} · {selected.email}</p>
                </div>
                <div style={{ display: "grid", gap: 8, justifyItems: "end" }}>
                  <StatusBadge status={selected.status} />
                  {!selected.emailSent && <span style={warningBadge}>Email delivery failed</span>}
                </div>
              </div>

              <div style={detailGrid}>
                <Detail label="Created" value={formatDate(selected.createdAt)} />
                <Detail label="Remote address" value={selected.remoteAddress || "Unknown"} />
                <Detail label="User agent" value={selected.userAgent || "Unknown"} />
                <Detail label="Lead source" value={selected.leadSource || "Contact form"} />
                <Detail label="Last viewed" value={formatDate(selected.lastViewedAt)} />
                <Detail label="Replied" value={formatDate(selected.repliedAt)} />
              </div>

              <div style={messageBody}>{selected.message}</div>

              <div style={notesCard}>
                <label style={label}>Admin notes</label>
                <textarea
                  style={{ ...input, minHeight: 110, resize: "vertical" }}
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                  placeholder="Potential Pro customer. Asked about prop firm support."
                />
                <button type="button" onClick={saveNotes} disabled={saving} style={{ ...button, width: 150, marginTop: 10 }}>
                  Save Notes
                </button>
              </div>

              {selected.replyBody && (
                <div style={replyPreview}>
                  <strong>{selected.replySubject}</strong>
                  <p>{selected.replyBody}</p>
                </div>
              )}

              <div style={actionRow}>
                <button type="button" onClick={() => changeStatus("OPEN")} style={secondaryButton}>Mark Open</button>
                <button type="button" onClick={() => changeStatus("REPLIED")} style={purpleButton}>Mark Replied</button>
                <button type="button" onClick={() => changeStatus("ARCHIVED")} style={archiveButton}>Archive</button>
                <button type="button" onClick={startReply} style={{ ...button, width: "auto" }}>Reply</button>
              </div>
            </>
          ) : (
            <div style={emptyState}>Select a message to view details.</div>
          )}
        </article>
      </section>

      {replyOpen && selected && (
        <div style={modalOverlay}>
          <section style={modalCard}>
            <h2 style={{ margin: 0, color: "#f8fafc" }}>Reply to contact</h2>
            <p style={mutedText}>To: {selected.email}</p>
            <label style={label}>Subject</label>
            <input style={input} value={replySubject} onChange={(event) => setReplySubject(event.target.value)} />
            <label style={{ ...label, marginTop: 14 }}>Message</label>
            <textarea style={{ ...input, minHeight: 180, resize: "vertical" }} value={replyBody} onChange={(event) => setReplyBody(event.target.value)} />
            {replyError && <div style={{ ...errorBox, marginTop: 14 }}>{replyError}</div>}
            <div style={actionRow}>
              <button
                type="button"
                onClick={sendReply}
                disabled={submittingReply || !replySubject.trim() || !replyBody.trim()}
                style={{ ...button, width: 150 }}
              >
                {submittingReply ? "Sending..." : "Send Reply"}
              </button>
              <button type="button" onClick={closeReplyModal} disabled={submittingReply} style={archiveButton}>Cancel</button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

function PaginationControls({
  page,
  totalPages,
  totalElements,
  onPrevious,
  onNext,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <div style={paginationRow}>
      <span>{totalElements} messages · Page {Math.min(page + 1, Math.max(totalPages, 1))} of {Math.max(totalPages, 1)}</span>
      <div style={{ display: "flex", gap: 8 }}>
        <button type="button" onClick={onPrevious} disabled={page <= 0} style={pagerButton(page <= 0)}>Previous</button>
        <button type="button" onClick={onNext} disabled={page >= totalPages - 1} style={pagerButton(page >= totalPages - 1)}>Next</button>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div style={metricCard}><span>{label}</span><strong>{value}</strong></div>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div style={detailBox}><span>{label}</span><strong>{value}</strong></div>;
}

function StatusBadge({ status }: { status: ContactMessageStatus }) {
  return <span style={statusBadge(status)}>{status}</span>;
}

function formatDate(value?: string | null) {
  if (!value) return "Not yet";
  return new Date(value).toLocaleString();
}

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object") {
      const maybeError = (data as { error?: unknown; message?: unknown }).error;
      const maybeMessage = (data as { error?: unknown; message?: unknown }).message;
      if (typeof maybeMessage === "string") return maybeMessage;
      if (typeof maybeError === "string") return maybeError;
    }
  }
  return "Unable to send reply.";
}

const pageWrap: CSSProperties = { display: "grid", gap: 22 };
const header: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 20, flexWrap: "wrap" };
const eyebrow: CSSProperties = { margin: 0, color: "#34d399", fontSize: 12, fontWeight: 900, textTransform: "uppercase", letterSpacing: "0.12em" };
const title: CSSProperties = { margin: "8px 0", color: "#f8fafc", fontSize: 40, letterSpacing: "-0.04em" };
const summaryGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 14 };
const metricCard: CSSProperties = { ...card, padding: 18, display: "grid", gap: 6 };
const controlsCard: CSSProperties = { ...card, display: "flex", gap: 14, alignItems: "end", flexWrap: "wrap" };
const filterPills: CSSProperties = { display: "flex", gap: 8, flexWrap: "wrap" };
const pill: CSSProperties = { padding: "10px 12px", borderRadius: 999, border: "1px solid rgba(148,163,184,0.18)", background: "rgba(2,6,23,0.42)", color: "#94a3b8", fontWeight: 900, cursor: "pointer" };
const activePill: CSSProperties = { ...pill, color: "#022c22", background: "linear-gradient(135deg,#34d399,#22d3ee)" };
const contentGrid: CSSProperties = { display: "grid", gridTemplateColumns: "minmax(320px, 0.7fr) minmax(0, 1fr)", gap: 18 };
const listCard: CSSProperties = { ...card, padding: 14, display: "grid", gap: 10, alignContent: "start" };
const detailCard: CSSProperties = { ...card, minHeight: 620 };
const messageRow: CSSProperties = { width: "100%", textAlign: "left", padding: 14, borderRadius: 16, border: "1px solid rgba(148,163,184,0.13)", color: "#cbd5e1", cursor: "pointer", display: "grid", gap: 7 };
const rowTop: CSSProperties = { display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" };
const emptyState: CSSProperties = { color: "#94a3b8", padding: 16 };
const detailGrid: CSSProperties = { display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 10, marginTop: 18 };
const detailBox: CSSProperties = { padding: 12, borderRadius: 14, background: "rgba(2,6,23,0.42)", border: "1px solid rgba(148,163,184,0.13)", display: "grid", gap: 4, color: "#94a3b8", fontSize: 12, wordBreak: "break-word" };
const messageBody: CSSProperties = { marginTop: 18, padding: 18, borderRadius: 18, background: "rgba(2,6,23,0.5)", border: "1px solid rgba(148,163,184,0.13)", color: "#e5eefc", whiteSpace: "pre-wrap", lineHeight: 1.7 };
const notesCard: CSSProperties = { marginTop: 18, padding: 16, borderRadius: 18, background: "rgba(15,23,42,0.62)", border: "1px solid rgba(148,163,184,0.13)" };
const replyPreview: CSSProperties = { marginTop: 18, padding: 16, borderRadius: 18, background: "rgba(139,92,246,0.12)", border: "1px solid rgba(139,92,246,0.24)", color: "#ddd6fe", whiteSpace: "pre-wrap" };
const actionRow: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap", marginTop: 18 };
const secondaryButton: CSSProperties = { ...button, width: "auto", background: "rgba(14,165,233,0.2)", border: "1px solid rgba(56,189,248,0.24)", boxShadow: "none" };
const purpleButton: CSSProperties = { ...button, width: "auto", background: "linear-gradient(135deg,#8b5cf6,#6d28d9)", boxShadow: "0 14px 35px rgba(139,92,246,0.2)" };
const archiveButton: CSSProperties = { ...button, width: "auto", background: "rgba(71,85,105,0.82)", boxShadow: "none" };
const warningBadge: CSSProperties = { width: "fit-content", padding: "6px 8px", borderRadius: 999, color: "#fde68a", background: "rgba(245,158,11,0.14)", border: "1px solid rgba(245,158,11,0.24)", fontSize: 12, fontWeight: 900 };
const errorBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#fecaca", background: "rgba(239,68,68,0.12)", border: "1px solid rgba(239,68,68,0.28)" };
const successBox: CSSProperties = { padding: 14, borderRadius: 14, color: "#bbf7d0", background: "rgba(34,197,94,0.12)", border: "1px solid rgba(34,197,94,0.28)" };
const modalOverlay: CSSProperties = { position: "fixed", inset: 0, zIndex: 60, display: "grid", placeItems: "center", padding: 20, background: "rgba(2,6,23,0.78)", backdropFilter: "blur(10px)" };
const modalCard: CSSProperties = { ...card, width: "min(620px,100%)" };
const paginationRow: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10, color: "#94a3b8", padding: "8px 4px 2px", flexWrap: "wrap" };
const pagerButton = (disabled: boolean): CSSProperties => ({ ...archiveButton, opacity: disabled ? 0.45 : 1, cursor: disabled ? "not-allowed" : "pointer" });

function statusBadge(status: ContactMessageStatus): CSSProperties {
  const colors = {
    NEW: ["#bbf7d0", "rgba(34,197,94,0.14)", "rgba(34,197,94,0.3)"],
    OPEN: ["#bae6fd", "rgba(14,165,233,0.14)", "rgba(56,189,248,0.3)"],
    REPLIED: ["#ddd6fe", "rgba(139,92,246,0.14)", "rgba(139,92,246,0.3)"],
    ARCHIVED: ["#cbd5e1", "rgba(71,85,105,0.26)", "rgba(148,163,184,0.2)"],
  } satisfies Record<ContactMessageStatus, string[]>;
  const [color, background, border] = colors[status];
  return { width: "fit-content", padding: "7px 9px", borderRadius: 999, color, background, border: `1px solid ${border}`, fontSize: 12, fontWeight: 950 };
}
