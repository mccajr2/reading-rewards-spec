import { useEffect, useState } from 'react';
import { FamilyMessageDto, getAllMessages, sendNudge } from '../shared/api';
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle } from './shared';

interface ChildNudgePanelProps {
  token: string | null;
}

/**
 * Child-facing panel for sending nudges to their parent.
 */
export function ChildNudgePanel({ token }: ChildNudgePanelProps) {
  const [messages, setMessages] = useState<FamilyMessageDto[]>([]);
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadMessages = async () => {
    try {
      const data = await getAllMessages(token);
      setMessages(data);
    } catch {
      // non-fatal
    }
  };

  useEffect(() => {
    loadMessages();
  }, []);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      await sendNudge(token, body.trim());
      setSuccess('Nudge sent to your parent!');
      setBody('');
      await loadMessages();
    } catch (err) {
      if (err instanceof Error && err.message === 'NUDGE_COOLDOWN') {
        setError('You can only send one nudge per 24 hours. Try again later.');
      } else {
        setError(err instanceof Error ? err.message : 'Send failed');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card data-testid="family-messages">
      <CardHeader>
        <CardTitle>Nudge Your Parent</CardTitle>
        <CardDescription>
          Send a friendly reminder if you are waiting on a payout or just want encouragement.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSend} className="space-y-3" data-testid="message-form">
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Write a message to nudge your parent…"
            maxLength={500}
            rows={3}
            className="w-full resize-none rounded-md border border-border bg-background px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            data-testid="message-body"
          />
          <div className="flex items-center justify-between">
            <span className="text-xs text-text-secondary">{body.length}/500</span>
            <Button
              type="submit"
              disabled={loading || !body.trim()}
              data-testid="send-message"
            >
              {loading ? 'Sending…' : 'Send Nudge'}
            </Button>
          </div>
        </form>

        {error && (
          <p className="text-sm text-error" data-testid="message-error">
            {error}
          </p>
        )}
        {success && (
          <p className="text-sm text-success" data-testid="message-success">
            {success}
          </p>
        )}

        <div className="space-y-2" data-testid="message-thread">
          {messages.length === 0 ? (
            <p className="text-sm text-text-secondary">No messages yet.</p>
          ) : (
            messages.map((msg) => (
              <div
                key={msg.id}
                className={`rounded-lg border p-3 text-sm ${
                  msg.senderRole === 'CHILD'
                    ? 'border-primary/20 bg-primary-light'
                    : 'border-success/20 bg-background-alt'
                }`}
                data-testid={`message-${msg.id}`}
              >
                <div className="mb-1 flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase tracking-wide text-text-secondary">
                    {msg.messageType} · {msg.senderRole}
                  </span>
                  <span className="text-xs text-text-secondary">
                    {new Date(msg.createdAt).toLocaleString()}
                  </span>
                </div>
                <p>{msg.body}</p>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  );
}
