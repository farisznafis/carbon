interface ProgressBarProps {
  step: number;
  total: number;
}

export const ProgressBar = ({ step, total }: ProgressBarProps) => {
  const percentage = (step / total) * 100;

  return (
    <div className="mb-4 h-2 w-full rounded-full bg-muted">
      <div
        className="h-2 rounded-full bg-primary transition-all"
        style={{ width: `${percentage}%` }}
      />
    </div>
  );
};
