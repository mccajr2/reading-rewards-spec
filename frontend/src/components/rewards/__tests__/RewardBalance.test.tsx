import { render, screen } from '@testing-library/react';
import { RewardBalance } from '../RewardBalance';

describe('RewardBalance', () => {
  it('renders totals and history rows', () => {
    render(
      <RewardBalance
        summary={{ totalEarned: 10.5, totalPaid: 4.0, availableBalance: 6.5 }}
        history={[
          {
            accumulationId: 'a1',
            amountEarned: 5,
            status: 'EARNED',
            calculationNote: '10 chapters @ $0.50',
            createdAt: new Date().toISOString(),
          },
        ]}
      />
    );

    expect(screen.getByText(/total earned/i)).toBeInTheDocument();
    expect(screen.getByText('$10.50')).toBeInTheDocument();
    expect(screen.getByText(/10 chapters @ \$0.50/i)).toBeInTheDocument();
  });

  it('renders payout transition statuses in history', () => {
    render(
      <RewardBalance
        summary={{ totalEarned: 20, totalPaid: 5, availableBalance: 15 }}
        history={[
          {
            accumulationId: 'a-earned',
            amountEarned: 5,
            status: 'EARNED',
            createdAt: new Date().toISOString(),
          },
          {
            accumulationId: 'a-paid',
            amountEarned: 5,
            status: 'PAID',
            createdAt: new Date().toISOString(),
          },
        ]}
      />
    );

    expect(screen.getByText('EARNED')).toBeInTheDocument();
    expect(screen.getByText('PAID')).toBeInTheDocument();
  });
});
