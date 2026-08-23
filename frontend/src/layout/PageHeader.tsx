import type { CSSProperties, ReactNode } from "react";
import { eyebrow, heroPill } from "../styles/dashboardStyles";

type Props = {
  label: string;
  title: string;
  description?: string;
  titleTestId?: string;
  actions?: ReactNode;
};

export default function PageHeader({ label, title, description, titleTestId, actions }: Props) {
  return (
    <section style={header}>
      <div>
        <div style={eyebrow}>{label}</div>
        <h1 data-testid={titleTestId} style={titleStyle}>{title}</h1>
        {description && <p style={descriptionStyle}>{description}</p>}
      </div>
      {actions && <div style={actionsStyle}>{actions}</div>}
    </section>
  );
}

export function UserPill({ name }: { name: string }) {
  return (
    <div
      style={{
        ...heroPill,
        background: "rgba(15,23,42,0.72)",
        border: "1px solid rgba(148,163,184,0.24)",
        color: "#cbd5e1",
      }}
    >
      {name}
    </div>
  );
}

const header: CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-start",
  gap: 18,
  flexWrap: "wrap",
  marginBottom: 24,
};

const titleStyle: CSSProperties = {
  margin: "8px 0 8px",
  color: "#f8fafc",
  fontSize: 38,
  lineHeight: 1.05,
};

const descriptionStyle: CSSProperties = {
  margin: 0,
  color: "#94a3b8",
  maxWidth: 760,
  lineHeight: 1.65,
};

const actionsStyle: CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  flexWrap: "wrap",
};
