import { coachMetricClass, coachMetricStyle } from "./coachStyles";

type CoachInsightCardProps = {
    title: string;
    value?: string | number | null;
    description?: string;
  };
  
  export function CoachInsightCard({
    title,
    value,
    description,
  }: CoachInsightCardProps) {
    return (
      <div className={coachMetricClass} style={coachMetricStyle}>
        <p className="text-sm font-medium text-[#94a3b8]">{title}</p>
        <p className="mt-2 text-lg font-semibold text-[#f8fafc]">
          {value ?? "Not enough data"}
        </p>
        {description && (
          <p className="mt-2 text-sm text-[#94a3b8]">{description}</p>
        )}
      </div>
    );
  }
