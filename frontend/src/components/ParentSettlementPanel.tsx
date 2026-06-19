import { useEffect, useState } from 'react';
import {
  FamilyMessageDto,
  SettlementRequestDto,
  approveSettlementRequest,
  getAllMessages,
  listParentPendingRequests,
  rejectSettlementRequest,
  sendEncouragement,
} from '../shared/api';
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle } from './shared';
import { Badge } from './ui/badge';

interface ParentSettlementPanelProps {
  token: string | null;
  targetChildId?: string;
  childName?: string;
  onSettlementChange?: () => void;
}

function formatAmount(amount: number) {
  return `$${amount.toFixed(2)}`;
}

function statusBadgeVariant(status: SettlementRequestDto['status']) {
  switch (status) {
    case 'PENDING':
      return 'warning' as const;
    case 'APPROVED':
      return 'success' as const;
    case 'REJECTED':
      return 'error' as const;
    default:
      return 'secondary' as const;
  }
}

/**
 * Parent-facing panel for approving settlement requests, sending encouragement,
 * and viewing family messages.
 */
export function ParentSettlementPanel({
  token,
  targetChildId,
  childName,
  onSettlementChange,
}: ParentSettlementPanelProps) {
  const [messages, setMessages] = useState<FamilyMessageDto[]>([]);
  const [pendingRequests, setPendingRequests] = useState<SettlementRequestDto[]>([]);
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

  const loadPendingRequests = async () => {
    try {
      const data = await listParentPendingRequests(token);
      if (targetChildId) {
        setPendingRequests(data.filter((req) => req.childUserId === targetChildId));
      } else {
        setPendingRequests(data);
      }
    } catch {
      // non-fatal
    }
  };

  useEffect(() => {
    loadMessages();
    loadPendingRequests();
  }, [targetChildId]);

  const handleApprove = async (requestId: string) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await approveSettlementRequest(token, requestId);
      setSuccess('Request approved.');
      await loadPendingRequests();
      onSettlementChange?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Approval failed');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (requestId: string) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await rejectSettlementRequest(token, requestId);
      setSuccess('Request rejected.');
      await loadPendingRequests();
      onSettlementChange?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Rejection failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSendEncouragement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim() || !targetChildId) return;
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      await sendEncouragement(token, targetChildId, body.trim());
      setBody('');
      setSuccess('Encouragement sent!');
      await loadMessages();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Send failed');
    } finally {
      setLoading(false);
    }
  };

  const visibleMessages = targetChildId
    ? messages.filter(
        (msg) =>
          msg.senderUserId === targetChildId ||
          msg.recipientUserId === targetChildId
      )
    : messages;

  return (
    <div className="space-y-4" data-testid="parent-settlement-panel">
      {pendingRequests.length > 0 && (
        <Card data-testid="pending-requests">
          <CardHeader>
            <CardTitle>Pending Requests</CardTitle>
            <CardDescription>
              {targetChildId && childName
                ? `Review ${childName}'s payout and spend requests.`
                : 'Review payout and spend requests from your children.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-2">
            {pendingRequests.map((req) => (
              <div
                key={req.id}
                className="flex flex-col gap-3 rounded-lg border border-border bg-accent-light p-3 sm:flex-row sm:items-center sm:justify-between"
                data-testid={`pending-request-${req.id}`}
              >
                <div className="text-sm">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant={statusBadgeVariant(req.status)}>{req.requestType}</Badge>
                    <span className="font-semibold text-text-primary">
                      {formatAmount(req.requestedAmount)}
                    </span>
                  </div>
                  {req.note && <p className="mt-1 text-text-secondary">{req.note}</p>}
                </div>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    onClick={() => handleApprove(req.id)}
                    disabled={loading}
                    data-testid={`approve-${req.id}`}
                  >
                    Approve
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => handleReject(req.id)}
                    disabled={loading}
                    data-testid={`reject-${req.id}`}
                  >
                    Reject
                  </Button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {targetChildId && (
        <Card data-testid="encouragement-form">
          <CardHeader>
            <CardTitle>Send Encouragement</CardTitle>
            <CardDescription>
              Send an in-app message to {childName ?? 'your child'}.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSendEncouragement} className="space-y-3" data-testid="message-form">
              <textarea
                value={body}
                onChange={(e) => setBody(e.target.value)}
                placeholder="Write an encouraging message…"
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
                  {loading ? 'Sending…' : 'Send Encouragement'}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

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

      <Card data-testid="message-thread">
        <CardHeader>
          <CardTitle>Family Messages</CardTitle>
          <CardDescription>Nudges and encouragement between you and your children.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          {visibleMessages.length === 0 ? (
            <p className="text-sm text-text-secondary">No messages yet.</p>
          ) : (
            visibleMessages.map((msg) => (
              <div
                key={msg.id}
                className={`rounded-lg border p-3 text-sm ${
                  msg.senderRole === 'CHILD'
                    ? 'border-primary/20 bg-primary-light text-text-primary'
                    : 'border-success/20 bg-background-alt text-text-primary'
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
        </CardContent>
      </Card>
    </div>
  );
}
