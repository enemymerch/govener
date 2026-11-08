import { Building2 } from "lucide-react";
import type { ReactNode } from "react";

export function Brand() {
  return (
    <div className="brand">
      <span>
        <Building2 size={24} strokeWidth={1.8} />
      </span>
      yönetici<span className="brand-period">.</span>
    </div>
  );
}

export function Stat({
  label,
  value,
  detail,
  icon,
}: {
  label: string;
  value: string;
  detail: string;
  icon: ReactNode;
}) {
  return (
    <article className="stat">
      <div>
        <span>{label}</span>
        <span className="stat-icon">{icon}</span>
      </div>
      <strong>{value}</strong>
      <p>{detail}</p>
    </article>
  );
}
