import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { RewardSelector } from '../RewardSelector';

describe('RewardSelector', () => {
  it('renders available rewards and updates selected value', () => {
    const onSelect = vi.fn();

    render(
      <RewardSelector
        rewards={[
          {
            rewardTemplateId: 'r1',
            rewardType: 'MONEY',
            amount: 0.5,
            unit: 'PER_CHAPTER',
            frequency: 'IMMEDIATE',
            description: '$0.50 per chapter',
            cannedTemplateId: 'MONEY_050_PER_CHAPTER',
            scope: 'FAMILY',
          },
          {
            rewardTemplateId: 'r2',
            rewardType: 'TIME',
            amount: 5,
            unit: 'PER_CHAPTER',
            frequency: 'IMMEDIATE',
            description: '5 minutes screentime per chapter',
            cannedTemplateId: 'TIME_5MIN_PER_CHAPTER',
            scope: 'FAMILY',
          },
        ]}
        selectedRewardTemplateId="r1"
        onSelect={onSelect}
      />
    );

    fireEvent.click(screen.getByDisplayValue('r2'));
    expect(onSelect).toHaveBeenCalledWith('r2');
  });
});
