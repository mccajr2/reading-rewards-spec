import { render, screen } from '@testing-library/react';
import { RewardsTypeCards } from '../RewardsTypeCards';

describe('RewardsTypeCards', () => {
  it('renders money/time/custom cards with type-aware values', () => {
    render(
      <RewardsTypeCards
        byType={[
          {
            rewardType: 'MONEY',
            description: 'Cash Rewards',
            totalEarned: 12.5,
            totalPaid: 5,
            availableBalance: 7.5,
            unitLabel: 'USD',
            accent: 'money',
          },
          {
            rewardType: 'TIME',
            description: 'Time Rewards',
            totalEarned: 30,
            totalPaid: 10,
            availableBalance: 20,
            unitLabel: 'minutes',
            accent: 'time',
          },
          {
            rewardType: 'CUSTOM_TEXT',
            description: 'Custom Rewards',
            totalEarned: 8,
            totalPaid: 3,
            availableBalance: 5,
            unitLabel: 'points',
            accent: 'custom',
          },
        ]}
      />
    );

    expect(screen.getByText(/cash rewards/i)).toBeInTheDocument();
    expect(screen.getByText('$12.50')).toBeInTheDocument();
    expect(screen.getByText(/30 min/i)).toBeInTheDocument();
    expect(screen.getByText(/8 pts/i)).toBeInTheDocument();
  });
});
