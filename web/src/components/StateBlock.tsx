type Props = {
  kind: "loading" | "empty" | "error";
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function StateBlock({ kind, message, actionLabel, onAction }: Props) {
  return (
    <div className={kind}>
      <div>{message}</div>
      {actionLabel && onAction ? (
        <div style={{ marginTop: 12 }}>
          <button type="button" className="btn secondary" onClick={onAction}>
            {actionLabel}
          </button>
        </div>
      ) : null}
    </div>
  );
}
