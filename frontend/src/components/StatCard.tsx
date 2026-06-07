import { card } from "../styles/dashboardStyles";

type StatCardProps = {
  title: string;
  value: string;
  positive?: boolean;
  negative?: boolean;
};

export default function StatCard({
  title,
  value,
  positive,
  negative,
}: StatCardProps) {
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
