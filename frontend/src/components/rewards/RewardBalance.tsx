export type RewardBalanceSummary = {
  totalEarned: number;
  totalPaid: number;
  availableBalance: number;
};

export type RewardBalanceRow = {
  accumulationId: string;
  amountEarned: number;
  status: 'EARNED' | 'PENDING_PAYOUT' | 'PAID';
  calculationNote?: string;
  createdAt: string;
};

type RewardBalanceProps = {
  summary: RewardBalanceSummary;
  history: RewardBalanceRow[];
};

export function RewardBalance({ summary, history }: RewardBalanceProps) {
  return (
    <section aria-label="Reward balance">
      <div>
        <strong>Total Earned:</strong> ${summary.totalEarned.toFixed(2)}
      </div>
      <div>
        <strong>Total Paid:</strong> ${summary.totalPaid.toFixed(2)}
      </div>
      <div>
        <strong>Available:</strong> ${summary.availableBalance.toFixed(2)}
      </div>

      <h3>Reward History</h3>
      <ul>
        {history.map((row) => (
          <li key={row.accumulationId}>
            <span>{row.status}</span>{' '}
            <span>${row.amountEarned.toFixed(2)}</span>{' '}
            {row.calculationNote && <span>{row.calculationNote}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}
