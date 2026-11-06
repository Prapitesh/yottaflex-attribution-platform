import type { ReactNode } from "react";

interface MetricCardProps {
  icon: ReactNode;
  label: string;
  value: string;
  note: string;
  trend: string;
}

export function MetricCard({ icon, label, value, note, trend }: MetricCardProps) {
  return (
    <div className="metric">
      <div className="metric-top">
        <span className="metric-icon">{icon}</span>
        <span className="metric-trend">{trend}</span>
      </div>
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
      <div className="metric-note">{note}</div>
    </div>
  );
}
