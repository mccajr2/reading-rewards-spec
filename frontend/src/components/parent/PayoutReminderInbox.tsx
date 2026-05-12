import type { RewardMessage } from '../../services/messageApi';

type PayoutReminderInboxProps = {
  reminders: RewardMessage[];
  onMarkRead: (messageId: string) => Promise<void>;
};

export function PayoutReminderInbox({ reminders, onMarkRead }: PayoutReminderInboxProps) {
  return (
    <section aria-label="Payout reminder inbox">
      <h2>Payout Reminders</h2>
      {reminders.length === 0 ? <p>No payout reminders right now.</p> : null}
      <ul>
        {reminders.map((message) => (
          <li key={message.messageId}>
            <p style={{ marginBottom: 4 }}>{message.messageText}</p>
            <small>{new Date(message.createdAt).toLocaleString()}</small>
            {!message.isRead ? (
              <div>
                <button type="button" onClick={() => void onMarkRead(message.messageId)}>
                  Mark Read
                </button>
              </div>
            ) : (
              <div><small>Read</small></div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
