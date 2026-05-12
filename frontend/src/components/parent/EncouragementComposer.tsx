import { useState } from 'react';

type ChildOption = {
  childId: string;
  childName: string;
};

type EncouragementComposerProps = {
  children: ChildOption[];
  onSend: (payload: { childId: string; messageText: string }) => Promise<void>;
};

export function EncouragementComposer({ children, onSend }: EncouragementComposerProps) {
  const [childId, setChildId] = useState(children[0]?.childId ?? '');
  const [messageText, setMessageText] = useState('');
  const [sending, setSending] = useState(false);
  const [status, setStatus] = useState('');

  const handleSend = async () => {
    if (!childId || !messageText.trim()) {
      setStatus('Choose a child and enter a message.');
      return;
    }
    setSending(true);
    setStatus('');
    try {
      await onSend({ childId, messageText: messageText.trim() });
      setMessageText('');
      setStatus('Encouragement sent');
    } catch {
      setStatus('Could not send encouragement right now');
    } finally {
      setSending(false);
    }
  };

  return (
    <section aria-label="Encouragement composer">
      <h2>Send Encouragement</h2>
      <label>
        Child
        <select aria-label="Child recipient" value={childId} onChange={(e) => setChildId(e.target.value)}>
          {children.map((child) => (
            <option key={child.childId} value={child.childId}>{child.childName}</option>
          ))}
        </select>
      </label>
      <label style={{ display: 'block', marginTop: 8 }}>
        Message
        <textarea
          aria-label="Encouragement message"
          value={messageText}
          onChange={(e) => setMessageText(e.target.value)}
          rows={3}
        />
      </label>
      <button type="button" onClick={() => void handleSend()} disabled={sending || children.length === 0}>
        {sending ? 'Sending…' : 'Send Encouragement'}
      </button>
      {status ? <p role="status">{status}</p> : null}
    </section>
  );
}
