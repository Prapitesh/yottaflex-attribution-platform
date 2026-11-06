import { ArrowDownRight } from "lucide-react";

interface ModelOptionProps {
  name: string;
  description: string;
  color: "mint" | "blue" | "purple";
  active: boolean;
  onClick: () => void;
}

export function ModelOption({
  name,
  description,
  color,
  active,
  onClick,
}: ModelOptionProps) {
  return (
    <button
      className={`model ${active ? "selected" : ""}`}
      onClick={onClick}
      type="button"
    >
      <span className={`model-dot ${color}`} />
      <span>
        <strong>{name}</strong>
        <small>{description}</small>
      </span>
      <ArrowDownRight size={15} />
    </button>
  );
}
