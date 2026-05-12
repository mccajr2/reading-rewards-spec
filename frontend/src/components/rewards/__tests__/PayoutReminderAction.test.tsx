import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { PayoutReminder } from '../PayoutReminder';

describe('PayoutReminder', () => {
  it('sends reminder and shows confirmation state', async () => {
    const onSend = vi.fn().mockResolvedValue(undefined);

    render(<PayoutReminder pendingAmount={12.5} onSend={onSend} />);

    fireEvent.change(screen.getByLabelText(/reminder note/i), { target: { value: 'Please review my payout' } });
    fireEvent.click(screen.getByRole('button', { name: /send payout reminder/i }));

    await waitFor(() => {
      expect(onSend).toHaveBeenCalledWith({
        pendingAmount: 12.5,
        note: 'Please review my payout',
        emailEnabled: true,
      });
    });
    expect(await screen.findByRole('status')).toHaveTextContent(/reminder sent to parent/i);
  });
});
