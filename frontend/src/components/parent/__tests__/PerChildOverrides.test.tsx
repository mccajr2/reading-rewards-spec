import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { PerChildOverrides } from '../PerChildOverrides';

describe('PerChildOverrides', () => {
  it('renders grouped rewards and submits a child override', async () => {
    const onCreate = vi.fn().mockResolvedValue(undefined);

    render(
      <PerChildOverrides
        groups={[
          {
            childId: 'child-1',
            childName: 'Alice',
            rewards: [
              {
                rewardTemplateId: 'r-1',
                childId: 'child-1',
                rewardType: 'MONEY',
                amount: 1,
                unit: 'PER_CHAPTER',
                frequency: 'IMMEDIATE',
                cannedTemplateId: null,
                description: '$1 per chapter',
                isDeleted: false,
                createdAt: new Date().toISOString(),
              },
            ],
          },
        ]}
        onCreate={onCreate}
      />
    );

    expect(screen.getByRole('heading', { name: /alice/i })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/new reward for alice/i), {
      target: { value: '$2 per chapter' },
    });
    fireEvent.click(screen.getByRole('button', { name: /add override/i }));

    expect(onCreate).toHaveBeenCalledWith(
      'child-1',
      expect.objectContaining({ description: '$2 per chapter', childId: 'child-1' })
    );
  });
});
