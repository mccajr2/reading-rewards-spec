import type { RewardMessage } from '../../services/messageApi';

type MessageCenterProps = {
  messages: RewardMessage[];
  onMarkRead: (messageId: string) => Promise<void>;
};

export function MessageCenter({ messages, onMarkRead }: MessageCenterProps) {
  return (
    <section aria-label="Message center">
      <h3>Encouragement Messages</h3>
      {messages.length === 0 ? <p>No messages yet.</p> : null}
      <ul>
        {messages.map((message) => (
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
