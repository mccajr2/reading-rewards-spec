import type { ChildRewardBalanceResponse } from '../../services/rewardApi';

type RewardsTypeCardsProps = {
  byType: ChildRewardBalanceResponse['byRewardType'];
};

function formatValue(unitLabel: string, value: number) {
  if (unitLabel === 'USD') {
    return `$${value.toFixed(2)}`;
  }
  if (unitLabel === 'minutes') {
    return `${value.toFixed(0)} min`;
  }
  return `${value.toFixed(0)} pts`;
}

export function RewardsTypeCards({ byType }: RewardsTypeCardsProps) {
  if (!byType.length) {
    return null;
  }

  return (
    <section aria-label="Rewards by type">
      <h3>Rewards By Type</h3>
      <div style={{ display: 'grid', gap: 10, gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))' }}>
        {byType.map((row) => (
          <article
            key={row.rewardType}
            data-accent={row.accent}
            style={{ border: '1px solid #d8dfe8', borderRadius: 12, padding: 12, background: '#fff' }}
          >
            <p style={{ margin: 0, fontWeight: 700 }}>{row.description}</p>
            <p style={{ margin: '6px 0' }}><strong>Earned:</strong> {formatValue(row.unitLabel, row.totalEarned)}</p>
            <p style={{ margin: '6px 0' }}><strong>Paid:</strong> {formatValue(row.unitLabel, row.totalPaid)}</p>
            <p style={{ margin: '6px 0 0' }}><strong>Available:</strong> {formatValue(row.unitLabel, row.availableBalance)}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
