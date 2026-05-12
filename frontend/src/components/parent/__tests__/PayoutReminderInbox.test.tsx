import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { PayoutReminderInbox } from '../PayoutReminderInbox';

describe('PayoutReminderInbox', () => {
  it('renders reminders and marks them read', async () => {
    const onMarkRead = vi.fn().mockResolvedValue(undefined);

    render(
      <PayoutReminderInbox
        reminders={[
          {
            messageId: 'm-1',
            senderId: 'child-1',
            recipientId: 'parent-1',
            messageType: 'PAYOUT_REMINDER',
            messageText: 'Jamie sent a payout reminder ($12.50 pending)',
            isRead: false,
            createdAt: '2026-05-12T12:00:00Z',
            readAt: null,
          },
        ]}
        onMarkRead={onMarkRead}
      />
    );

    expect(screen.getByText(/payout reminders/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /mark read/i }));
    expect(onMarkRead).toHaveBeenCalledWith('m-1');
  });
});
