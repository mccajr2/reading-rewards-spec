import { useState } from 'react';

type PayoutReminderProps = {
  pendingAmount: number;
  onSend: (payload: { pendingAmount: number; note?: string; emailEnabled?: boolean }) => Promise<void>;
};

export function PayoutReminder({ pendingAmount, onSend }: PayoutReminderProps) {
  const [note, setNote] = useState('');
  const [emailEnabled, setEmailEnabled] = useState(true);
  const [sending, setSending] = useState(false);
  const [status, setStatus] = useState('');

  if (pendingAmount <= 0) {
    return null;
  }

  const handleSend = async () => {
    setSending(true);
    setStatus('');
    try {
      await onSend({ pendingAmount, note, emailEnabled });
      setStatus('Reminder sent to parent');
      setNote('');
    } catch {
      setStatus('Could not send reminder right now');
    } finally {
      setSending(false);
    }
  };

  return (
    <section aria-label="Payout reminder">
      <h3>Need a payout?</h3>
      <p>You have ${pendingAmount.toFixed(2)} waiting. Send a reminder to your parent.</p>
      <label>
        Note (optional)
        <input
          aria-label="Reminder note"
          type="text"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="I finished my reading goal this week"
        />
      </label>
      <label style={{ display: 'block', marginTop: 8 }}>
        <input
          type="checkbox"
          checked={emailEnabled}
          onChange={(e) => setEmailEnabled(e.target.checked)}
        />{' '}
        Also send parent email
      </label>
      <button type="button" onClick={() => void handleSend()} disabled={sending}>
        {sending ? 'Sending…' : 'Send Payout Reminder'}
      </button>
      {status ? <p role="status">{status}</p> : null}
    </section>
  );
}
