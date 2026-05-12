import { fireEvent, render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import { EncouragementComposer } from '../../parent/EncouragementComposer';
import { MessageCenter } from '../MessageCenter';

describe('Encouragement messaging', () => {
  it('submits parent encouragement and renders child inbox message history', async () => {
    const onSend = vi.fn().mockResolvedValue(undefined);
    const onMarkRead = vi.fn().mockResolvedValue(undefined);

    render(
      <>
        <EncouragementComposer
          children={[{ childId: 'child-1', childName: 'Jamie' }]}
          onSend={onSend}
        />
        <MessageCenter
          messages={[
            {
              messageId: 'msg-1',
              senderId: 'parent-1',
              recipientId: 'child-1',
              messageType: 'ENCOURAGEMENT',
              messageText: 'Awesome reading streak!',
              isRead: false,
              createdAt: '2026-05-12T12:00:00Z',
              readAt: null,
            },
          ]}
          onMarkRead={onMarkRead}
        />
      </>
    );

    fireEvent.change(screen.getByLabelText(/encouragement message/i), { target: { value: 'Keep going!' } });
    fireEvent.click(screen.getByRole('button', { name: /send encouragement/i }));

    expect(onSend).toHaveBeenCalledWith({ childId: 'child-1', messageText: 'Keep going!' });
    expect(screen.getByText(/awesome reading streak/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /mark read/i }));
    expect(onMarkRead).toHaveBeenCalledWith('msg-1');
  });
});
