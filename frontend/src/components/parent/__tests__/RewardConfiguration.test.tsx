import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { RewardTemplateBuilder } from '../RewardTemplateBuilder';

describe('RewardTemplateBuilder', () => {
  it('submits family reward payload', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<RewardTemplateBuilder onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText(/description/i), {
      target: { value: '$0.50 per chapter' },
    });

    fireEvent.change(screen.getByLabelText(/amount/i), {
      target: { value: '0.50' },
    });

    fireEvent.click(screen.getByRole('button', { name: /save family reward/i }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        description: '$0.50 per chapter',
        amount: 0.5,
        rewardType: 'MONEY',
      })
    );
  });
});
