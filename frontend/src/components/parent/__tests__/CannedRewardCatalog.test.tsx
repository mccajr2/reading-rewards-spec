import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { CannedRewardCatalog } from '../CannedRewardCatalog';

describe('CannedRewardCatalog', () => {
  it('renders canned templates and triggers add action', () => {
    const onAdd = vi.fn().mockResolvedValue(undefined);

    render(
      <CannedRewardCatalog
        templates={[
          {
            cannedTemplateId: 'MONEY_050_PER_CHAPTER',
            rewardType: 'MONEY',
            amount: 0.5,
            unit: 'PER_CHAPTER',
            frequency: 'IMMEDIATE',
            description: '$0.50 per chapter',
          },
        ]}
        onAdd={onAdd}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /add/i }));
    expect(onAdd).toHaveBeenCalledWith(expect.objectContaining({ cannedTemplateId: 'MONEY_050_PER_CHAPTER' }));
  });
});
