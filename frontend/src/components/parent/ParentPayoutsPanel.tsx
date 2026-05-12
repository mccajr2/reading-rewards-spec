import { useState } from 'react';

export type ParentAccumulationRow = {
  accumulationId: string;
  amountEarned: number;
  status: 'EARNED' | 'PENDING_PAYOUT' | 'PAID';
  calculationNote?: string;
};

type ParentPayoutsPanelProps = {
  childName: string;
  rows: ParentAccumulationRow[];
  onConfirmPayout: (accumulationIds: string[]) => Promise<void>;
};

export function ParentPayoutsPanel({ childName, rows, onConfirmPayout }: ParentPayoutsPanelProps) {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const pending = rows.filter((row) => row.status !== 'PAID');

  const toggle = (id: string) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  return (
    <section aria-label="Parent payouts panel">
      <h2>Pending Payouts: {childName}</h2>
      <ul>
        {pending.map((row) => (
          <li key={row.accumulationId}>
            <label>
              <input
                type="checkbox"
                checked={selectedIds.includes(row.accumulationId)}
                onChange={() => toggle(row.accumulationId)}
              />
              ${row.amountEarned.toFixed(2)} {row.calculationNote ?? ''}
            </label>
          </li>
        ))}
      </ul>
      <button
        type="button"
        onClick={() => void onConfirmPayout(selectedIds)}
        disabled={selectedIds.length === 0}
      >
        Confirm Payout
      </button>
    </section>
  );
}
