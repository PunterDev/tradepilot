import { useEffect, useState } from "react";
import {
  parseTradovateCsv,
  type TradovateTrade,
} from "./utils/tradovateCsv";
import {
  createAccount,
  getAccounts,
  type TradingAccount,
} from "./api/accountApi";
import { getAllTrades, getTradesByAccount, importTradesFromCsv } from "./api/tradeApi";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { autoLogin, logout } from "./api/authApi";
import { deleteAccount } from "./api/accountApi";
import { saveJournal } from "./api/journalApi";
import PnlCalendar from "./components/PnlCalendar";
import {
  type DailyPnl,
  formatCurrency,
  calculateCurrentStreak,
  calculateBestStreak,
  buildDailyPnlFromDbTrades,
  getDefaultCalendarMonth,
} from "./utils/dashboardUtils";
import {
  page,
  hero,
  eyebrow,
  heroTitle,
  heroText,
  heroPill,
  card,
  tableCard,
  sectionHeader,
  sectionTitle,
  mutedText,
  formGrid,
  statsGrid,
  statsGridSmall,
  chartGrid,
  table,
  th,
  td,
  label,
  input,
  button,
  accountChip,
  tooltipStyle,
  monthLabel,
  calendarGrid,
  calendarHeaderCell,
  calendarCellBase,
  calendarDayNumber,
  calendarPnlWrap,
  calendarPnlAmount,
  calendarTradeCount,
  calendarBadge,
  calendarMonthlyFooter,
  hoverPopup,
  modalOverlay,
  modalCard,
  journalGrid,
  textarea
} from "./styles/dashboardStyles";


function App() {
  const [accounts, setAccounts] = useState<TradingAccount[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | "ALL" | "">("ALL");
  const [fileName, setFileName] = useState("");
  const [trades, setTrades] = useState<TradovateTrade[]>([]);
  const [dailyPnl, setDailyPnl] = useState<DailyPnl[]>([]);
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [parsing, setParsing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showAccountForm, setShowAccountForm] = useState(false);
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [newAccountName, setNewAccountName] = useState("");
  const [newPropFirm, setNewPropFirm] = useState("TOPSTEP");
  const [newAccountNumber, setNewAccountNumber] = useState("");
  const [newStartingBalance, setNewStartingBalance] = useState("50000");
  const [calendarYear, setCalendarYear] = useState(new Date().getFullYear());
  const [calendarMonth, setCalendarMonth] = useState(new Date().getMonth());
  const [authReady, setAuthReady] = useState(false);
  const [journalDate, setJournalDate] = useState("");
  const [journalMood, setJournalMood] = useState("Focused");
  const [journalSetupQuality, setJournalSetupQuality] = useState("Good");
  const [journalMistakes, setJournalMistakes] = useState("");
  const [journalLesson, setJournalLesson] = useState("");
  const [journalNotes, setJournalNotes] = useState("");

  async function handleSaveJournal() {
    if (!journalDate) {
      alert("Select a journal date first.");
      return;
    }

    try {
      const payload = {
        accountId: selectedAccountId === "ALL" ? null : Number(selectedAccountId),
        journalDate,
        mood: journalMood,
        setupQuality: journalSetupQuality,
        mistakes: journalMistakes,
        lessonLearned: journalLesson,
        notes: journalNotes,
      };

      await saveJournal(payload);

      alert("Journal saved to database ✅");
    } catch (error) {
      console.error(error);
      alert("Failed to save journal.");
    }
  }


  useEffect(() => {
    async function initAuth() {
      try {
        await autoLogin();
        setAuthReady(true);
      } catch (error) {
        console.error("Auto login failed", error);
        alert("Auto login failed. Check /api/dev/token.");
      }
    }

    initAuth();
  }, []);

  useEffect(() => {
    if (!authReady) return;

    async function loadAccounts() {
      setLoadingAccounts(true);
      try {
        const data = await getAccounts();
        setAccounts(data);
        setSelectedAccountId("ALL");
      } catch (error) {
        console.error("Failed to load accounts", error);
      } finally {
        setLoadingAccounts(false);
      }
    }

    loadAccounts();
  }, [authReady]);

  useEffect(() => {
    if (!selectedAccountId) return;

    async function loadSavedTrades() {
      try {
        const dbTrades =
          selectedAccountId === "ALL"
            ? await getAllTrades()
            : await getTradesByAccount(Number(selectedAccountId));

        const mappedTrades: TradovateTrade[] = dbTrades.map((trade: any) => ({
          symbol: trade.symbol,
          qty: trade.quantity,
          buyPrice: Number(trade.entryPrice ?? 0),
          sellPrice: Number(trade.exitPrice ?? 0),
          pnl: Number(trade.pnlAmount ?? 0),
          boughtTimestamp: trade.entryTime ?? "",
          soldTimestamp: trade.exitTime ?? trade.tradeDate,
          duration: "",
        }));

        const rebuiltDailyPnl = buildDailyPnlFromDbTrades(dbTrades);

        setTrades(mappedTrades);
        setDailyPnl(rebuiltDailyPnl);

        const defaultMonth = getDefaultCalendarMonth(rebuiltDailyPnl);
        setCalendarYear(defaultMonth.year);
        setCalendarMonth(defaultMonth.month);
      } catch (error) {
        console.error("Failed to load saved trades", error);
      }
    }

    loadSavedTrades();
  }, [selectedAccountId]);

  async function handleCsvUpload(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    setFileName(file.name);
    setParsing(true);

    try {
      const parsed = await parseTradovateCsv(file);
      setTrades(parsed.trades);
      setDailyPnl(parsed.dailyPnl);
      const defaultMonth = getDefaultCalendarMonth(parsed.dailyPnl);
      setCalendarYear(defaultMonth.year);
      setCalendarMonth(defaultMonth.month);
    } catch (error) {
      console.error(error);
      alert("Failed to parse CSV");
    } finally {
      setParsing(false);
    }
  }
  async function handleSaveToDatabase() {
    if (!selectedAccountId) {
      alert("Select an account first.");
      return;
    }

    if (selectedAccountId === "ALL") {
      alert("Select a specific account before importing CSV trades.");
      return;
    }

    if (trades.length === 0) {
      alert("Upload and parse a CSV first.");
      return;
    }

    setSaving(true);

    try {
      // 1. Save to DB
      const saved = await importTradesFromCsv(
        selectedAccountId,
        fileName,
        trades
      );

      // 2. Reload from DB (source of truth)
      const dbTrades = await getTradesByAccount(Number(selectedAccountId));

      // 3. Rebuild dashboard
      const rebuiltDailyPnl = buildDailyPnlFromDbTrades(dbTrades);
      setDailyPnl(rebuiltDailyPnl);

      // 4. Sync calendar view
      const defaultMonth = getDefaultCalendarMonth(rebuiltDailyPnl);
      setCalendarYear(defaultMonth.year);
      setCalendarMonth(defaultMonth.month);

      // (optional) update trades state too
      setTrades(
        dbTrades.map((t: any) => ({
          symbol: t.symbol,
          qty: t.quantity,
          buyPrice: Number(t.entryPrice ?? 0),
          sellPrice: Number(t.exitPrice ?? 0),
          pnl: Number(t.pnlAmount ?? 0),
          boughtTimestamp: t.entryTime ?? "",
          soldTimestamp: t.exitTime ?? t.tradeDate,
          duration: "",
        }))
      );

      alert(`Saved ${saved.length} new trades and synced from database ✅`);
    } catch (error) {
      console.error(error);
      alert("Failed to save trades to database.");
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateAccount() {
    if (!newAccountName.trim()) {
      alert("Account name is required.");
      return;
    }

    setCreatingAccount(true);

    try {
      const created = await createAccount({
        name: newAccountName.trim(),
        propFirm: newPropFirm,
        accountNumber: newAccountNumber.trim() || undefined,
        startingBalance: newStartingBalance ? Number(newStartingBalance) : undefined,
        status: "ACTIVE",
      });

      setAccounts((prev) => [created, ...prev]);
      setSelectedAccountId(created.id);
      setNewAccountName("");
      setNewPropFirm("TOPSTEP");
      setNewAccountNumber("");
      setNewStartingBalance("50000");
      setShowAccountForm(false);
    } catch (error) {
      console.error(error);
      alert("Failed to create account.");
    } finally {
      setCreatingAccount(false);
    }
  }

  async function handleDeleteAccount(accountId: number) {
    const confirmDelete = window.confirm(
      "Are you sure? This will delete the account and all its trades."
    );

    if (!confirmDelete) return;

    try {
      await deleteAccount(accountId);

      setAccounts((prev) => prev.filter((a) => a.id !== accountId));

      // Reset selection
      setSelectedAccountId("ALL");

      alert("Account deleted successfully");
    } catch (error) {
      console.error(error);
      alert("Failed to delete account");
    }
  }

  const totalPnl = dailyPnl.reduce((sum, day) => sum + day.totalPnl, 0);
  const totalTrades = trades.length;
  const winningTrades = trades.filter((trade) => trade.pnl > 0).length;
  const losingTrades = trades.filter((trade) => trade.pnl < 0).length;
  const breakevenTrades = trades.filter((trade) => trade.pnl === 0).length;

  const overallWinRate =
    totalTrades > 0 ? Number(((winningTrades / totalTrades) * 100).toFixed(2)) : 0;

  const winningDays = dailyPnl.filter((day) => day.totalPnl > 0).length;
  const losingDays = dailyPnl.filter((day) => day.totalPnl < 0).length;

  const worstDay = dailyPnl.reduce(
    (worst, day) => (day.totalPnl < worst ? day.totalPnl : worst),
    0
  );

  const currentWinStreak = calculateCurrentStreak(dailyPnl, "WIN");
  const currentLossStreak = calculateCurrentStreak(dailyPnl, "LOSS");
  const bestWinStreak = calculateBestStreak(dailyPnl, "WIN");
  const worstLossStreak = calculateBestStreak(dailyPnl, "LOSS");

  return (
    <div style={page}>
      <section style={hero}>
        <div>
          <div style={eyebrow}>TradePilot AI</div>
          <h1 style={heroTitle}>Prop Firm Performance Dashboard</h1>
          <p style={heroText}>
            Import Tradovate CSVs by account, review daily PnL, equity curve,
            streaks, and calendar performance.
          </p>
        </div>
        <div style={heroPill}>{accounts.length} Accounts</div>
        <button
          type="button"
          onClick={() => {
            logout();
            window.location.reload();
          }}
          style={{ ...button, width: 120 }}
        >
          Reset Auth
        </button>
      </section>

      <div style={{ ...card, marginBottom: 24 }}>
        <div style={sectionHeader}>
          <div>
            <h2 style={sectionTitle}>Trading Accounts</h2>
            <p style={mutedText}>
              Create accounts for Topstep, Apex, TPT, Tradovate, or any prop firm before importing CSV files.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setShowAccountForm((prev) => !prev)}
            style={{ ...button, width: 180 }}
          >
            {showAccountForm ? "Cancel" : "Add Account"}
          </button>
        </div>

        {showAccountForm && (
          <div style={{ ...formGrid, marginTop: 22 }}>
            <div>
              <label style={label}>Account Name</label>
              <input
                style={input}
                placeholder="Topstep 50K"
                value={newAccountName}
                onChange={(e) => setNewAccountName(e.target.value)}
              />
            </div>

            <div>
              <label style={label}>Prop Firm</label>
              <select
                style={input}
                value={newPropFirm}
                onChange={(e) => setNewPropFirm(e.target.value)}
              >
                <option value="TOPSTEP">Topstep</option>
                <option value="APEX">Apex</option>
                <option value="TAKE_PROFIT_TRADER">Take Profit Trader</option>
                <option value="TRADEOVATE">Tradovate</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div>
              <label style={label}>Account Number</label>
              <input
                style={input}
                placeholder="TS-001"
                value={newAccountNumber}
                onChange={(e) => setNewAccountNumber(e.target.value)}
              />
            </div>

            <div>
              <label style={label}>Starting Balance</label>
              <input
                style={input}
                type="number"
                placeholder="50000"
                value={newStartingBalance}
                onChange={(e) => setNewStartingBalance(e.target.value)}
              />
            </div>

            <div style={{ display: "flex", alignItems: "end" }}>
              <button
                type="button"
                onClick={handleCreateAccount}
                disabled={creatingAccount}
                style={button}
              >
                {creatingAccount ? "Creating..." : "Create Account"}
              </button>
            </div>
          </div>
        )}

        {accounts.length > 0 && (
          <div style={{ marginTop: 22 }}>
            <h3 style={{ marginBottom: 12 }}>Existing Accounts</h3>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 12 }}>
              <button
                type="button"
                onClick={() => setSelectedAccountId("ALL")}
                style={{
                  ...accountChip,
                  border:
                    selectedAccountId === "ALL"
                      ? "1px solid rgba(52,211,153,0.75)"
                      : "1px solid rgba(148,163,184,0.22)",
                  background:
                    selectedAccountId === "ALL"
                      ? "linear-gradient(135deg, rgba(16,185,129,0.22), rgba(34,197,94,0.12))"
                      : "rgba(15,23,42,0.6)",
                  color: selectedAccountId === "ALL" ? "#6ee7b7" : "#cbd5e1",
                }}
              >
                All Accounts
              </button>
              {accounts.map((account) => {
                const selected = selectedAccountId === account.id;

                return (
                  <div key={account.id} style={{ position: "relative" }}>
                    <button
                      type="button"
                      onClick={() => setSelectedAccountId(account.id)}
                      style={{
                        ...accountChip,
                        border: selected
                          ? "1px solid rgba(52,211,153,0.75)"
                          : "1px solid rgba(148,163,184,0.22)",
                        background: selected
                          ? "linear-gradient(135deg, rgba(16,185,129,0.22), rgba(34,197,94,0.12))"
                          : "rgba(15,23,42,0.6)",
                        color: selected ? "#6ee7b7" : "#cbd5e1",
                        paddingRight: 36, // space for delete button
                      }}
                    >
                      {account.name} · {account.propFirm}
                    </button>

                    {/* DELETE BUTTON */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteAccount(account.id);
                      }}
                      style={{
                        position: "absolute",
                        top: 6,
                        right: 6,
                        background: "transparent",
                        border: "none",
                        color: "#fb7185",
                        cursor: "pointer",
                        fontSize: 14,
                        fontWeight: 900,
                      }}
                    >
                      ✕
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      <div style={card}>
        <h2 style={sectionTitle}>Import Trades</h2>

        <div style={formGrid}>
          <div>
            <label style={label}>Trading Account</label>
            <select
              value={selectedAccountId}
              onChange={(e) => {
                const value = e.target.value;
                setSelectedAccountId(value === "ALL" ? "ALL" : value ? Number(value) : "");
              }}
              style={input}
              disabled={loadingAccounts}
            >
              <option value="ALL">All Accounts</option>
              <option value="">Select account</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name} — {account.propFirm}
                  {account.accountNumber ? ` — ${account.accountNumber}` : ""}
                </option>
              ))}
            </select>

            {accounts.length === 0 && !loadingAccounts && (
              <p style={{ color: "#fb7185", fontSize: 13 }}>
                No accounts found. Create one above first.
              </p>
            )}

            {selectedAccountId === "ALL" && (
              <p style={mutedText}>
                Select a specific account to import CSV trades. All Accounts is read-only combined analytics.
              </p>
            )}
          </div>

          <div>
            <label style={label}>Tradovate CSV</label>
            <input
              type="file"
              accept=".csv"
              onChange={handleCsvUpload}
              style={input}
              disabled={!selectedAccountId || selectedAccountId === "ALL"}
            />
          </div>

          <div style={{ display: "flex", alignItems: "end" }}>
            <button
              onClick={handleSaveToDatabase}
              disabled={!selectedAccountId || selectedAccountId === "ALL" || trades.length === 0 || saving}
              style={button}
            >
              {saving ? "Saving..." : "Save Imported Trades"}
            </button>
          </div>
        </div>

        {parsing && <p style={mutedText}>Parsing CSV...</p>}
        {fileName && <p style={mutedText}>Loaded file: {fileName}</p>}
      </div>

      <div style={statsGrid}>
        <StatCard
          title="Total PnL"
          value={formatCurrency(totalPnl)}
          positive={totalPnl >= 0}
          negative={totalPnl < 0}
        />
        <StatCard title="Win Rate" value={`${overallWinRate}%`} positive={overallWinRate >= 50} />
        <StatCard title="Total Trades" value={String(totalTrades)} />
        <StatCard title="Trading Days" value={String(dailyPnl.length)} />
        <StatCard title="Winning Days" value={String(winningDays)} positive />
        <StatCard title="Losing Days" value={String(losingDays)} negative />
      </div>

      <div style={statsGridSmall}>
        <StatCard title="Winning Trades" value={String(winningTrades)} positive />
        <StatCard title="Losing Trades" value={String(losingTrades)} negative />
        <StatCard title="Breakeven Trades" value={String(breakevenTrades)} />
        <StatCard title="Worst Day" value={formatCurrency(worstDay)} negative={worstDay < 0} />
      </div>

      <div style={statsGridSmall}>
        <StatCard title="Current Win Streak" value={`${currentWinStreak} days`} positive />
        <StatCard title="Current Loss Streak" value={`${currentLossStreak} days`} negative />
        <StatCard title="Best Win Streak" value={`${bestWinStreak} days`} positive />
        <StatCard title="Worst Loss Streak" value={`${worstLossStreak} days`} negative />
      </div>

      <div style={chartGrid}>
        <div style={card}>
          <h2 style={sectionTitle}>Daily PnL</h2>
          <div style={{ width: "100%", height: 320 }}>
            <ResponsiveContainer>
              <BarChart data={dailyPnl}>
                <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <YAxis stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={tooltipStyle} />
                <Bar dataKey="totalPnl" fill="#38bdf8" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div style={card}>
          <h2 style={sectionTitle}>Equity Curve</h2>
          <div style={{ width: "100%", height: 320 }}>
            <ResponsiveContainer>
              <LineChart data={dailyPnl}>
                <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
                <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <YAxis stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
                <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={tooltipStyle} />
                <Line
                  type="monotone"
                  dataKey="cumulativePnl"
                  stroke="#34d399"
                  strokeWidth={3}
                  dot={{ r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <PnlCalendar
        dailyPnl={dailyPnl}
        trades={trades}
        selectedAccountId={selectedAccountId}
        calendarYear={calendarYear}
        calendarMonth={calendarMonth}
        setCalendarYear={setCalendarYear}
        setCalendarMonth={setCalendarMonth}
        journalDate={journalDate}
        setJournalDate={setJournalDate}
        journalMood={journalMood}
        setJournalMood={setJournalMood}
        journalSetupQuality={journalSetupQuality}
        setJournalSetupQuality={setJournalSetupQuality}
        journalMistakes={journalMistakes}
        setJournalMistakes={setJournalMistakes}
        journalLesson={journalLesson}
        setJournalLesson={setJournalLesson}
        journalNotes={journalNotes}
        setJournalNotes={setJournalNotes}
        handleSaveJournal={handleSaveJournal}
      />

      <div style={{ ...card, marginTop: 32 }}>
        <h2 style={sectionTitle}>Daily Win Rate</h2>
        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer>
            <LineChart data={dailyPnl}>
              <CartesianGrid stroke="rgba(148,163,184,0.16)" strokeDasharray="4 4" />
              <XAxis dataKey="date" stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <YAxis domain={[0, 100]} stroke="#64748b" tick={{ fill: "#94a3b8", fontSize: 12 }} />
              <Tooltip formatter={(value) => `${Number(value).toFixed(2)}%`} contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="winRate" stroke="#38bdf8" strokeWidth={3} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <h2 style={{ marginTop: 40 }}>Daily PnL Table</h2>
      <div style={tableCard}>
        <table style={table}>
          <thead>
            <tr>
              <th style={th}>Date</th>
              <th style={th}>Trades</th>
              <th style={th}>Wins</th>
              <th style={th}>Losses</th>
              <th style={th}>BE</th>
              <th style={th}>Win Rate</th>
              <th style={th}>Daily PnL</th>
              <th style={th}>Equity</th>
            </tr>
          </thead>
          <tbody>
            {dailyPnl.length === 0 ? (
              <tr>
                <td colSpan={8} style={td}>
                  Select an account and upload your Tradovate CSV.
                </td>
              </tr>
            ) : (
              dailyPnl.map((day) => (
                <tr key={day.date}>
                  <td style={td}>{day.date}</td>
                  <td style={td}>{day.tradeCount}</td>
                  <td style={td}>{day.winningTrades}</td>
                  <td style={td}>{day.losingTrades}</td>
                  <td style={td}>{day.breakevenTrades}</td>
                  <td style={td}>{day.winRate}%</td>
                  <td
                    style={{
                      ...td,
                      color: day.totalPnl >= 0 ? "#34d399" : "#fb7185",
                      fontWeight: "bold",
                    }}
                  >
                    {formatCurrency(day.totalPnl)}
                  </td>
                  <td
                    style={{
                      ...td,
                      color: day.cumulativePnl >= 0 ? "#34d399" : "#fb7185",
                      fontWeight: "bold",
                    }}
                  >
                    {formatCurrency(day.cumulativePnl)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <h2 style={{ marginTop: 40 }}>Loaded Trades Preview</h2>
      <div style={tableCard}>
        <table style={table}>
          <thead>
            <tr>
              <th style={th}>Sold Time</th>
              <th style={th}>Symbol</th>
              <th style={th}>Qty</th>
              <th style={th}>Buy</th>
              <th style={th}>Sell</th>
              <th style={th}>PnL</th>
              <th style={th}>Duration</th>
            </tr>
          </thead>
          <tbody>
            {trades.length === 0 ? (
              <tr>
                <td colSpan={7} style={td}>
                  No trades loaded. Select an account or import a CSV.
                </td>
              </tr>
            ) : (
              trades.slice(0, 250).map((trade, index) => (
                <tr key={`${trade.soldTimestamp}-${index}`}>
                  <td style={td}>{trade.soldTimestamp}</td>
                  <td style={td}>{trade.symbol}</td>
                  <td style={td}>{trade.qty}</td>
                  <td style={td}>{trade.buyPrice}</td>
                  <td style={td}>{trade.sellPrice}</td>
                  <td
                    style={{
                      ...td,
                      color: trade.pnl >= 0 ? "#34d399" : "#fb7185",
                      fontWeight: 900,
                    }}
                  >
                    {formatCurrency(trade.pnl)}
                  </td>
                  <td style={td}>{trade.duration || "-"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

    </div>
  );
}

function StatCard({
  title,
  value,
  positive,
  negative,
}: {
  title: string;
  value: string;
  positive?: boolean;
  negative?: boolean;
}) {
  return (
    <div style={{ ...card, padding: 20, minHeight: 105 }}>
      <div
        style={{
          color: "#94a3b8",
          fontSize: 12,
          fontWeight: 800,
          textTransform: "uppercase",
          letterSpacing: "0.06em",
        }}
      >
        {title}
      </div>
      <div
        style={{
          fontSize: 28,
          fontWeight: 900,
          marginTop: 10,
          color: positive ? "#34d399" : negative ? "#fb7185" : "#f8fafc",
        }}
      >
        {value}
      </div>
    </div>
  );
}

export default App;
