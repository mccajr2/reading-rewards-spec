import { useEffect, useState } from 'react';
import {
  FamilyMessageDto,
  SettlementRequestDto,
  approveSettlementRequest,
  getAllMessages,
  listParentPendingRequests,
  rejectSettlementRequest,
  sendEncouragement,
  sendNudge,
} from '../shared/api';

interface FamilyMessagesProps {
  token: string | null;
  role: 'PARENT' | 'CHILD';
  /** Required when role='PARENT' to send encouragement to a specific child. */
  targetChildId?: string;
  onSettlementChange?: () => void;
}

/**
 * FamilyMessages — in-app messaging panel for parents and children.
 *
 * Children can send a nudge to their parent (24-hour cooldown enforced).
 * Parents can send encouragement to a child and approve/reject settlement requests.
 */
export function FamilyMessages({
  token,
  role,
  targetChildId,
  onSettlementChange,
}: FamilyMessagesProps) {
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
    if (role !== 'PARENT') return;
    try {
      const data = await listParentPendingRequests(token);
      setPendingRequests(data);
    } catch {
      // non-fatal
    }
  };

  useEffect(() => {
    loadMessages();
    loadPendingRequests();
  }, []);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      if (role === 'CHILD') {
        await sendNudge(token, body.trim());
        setSuccess('Nudge sent to your parent!');
      } else if (role === 'PARENT' && targetChildId) {
        await sendEncouragement(token, targetChildId, body.trim());
        setSuccess('Encouragement sent!');
      }
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

  const handleApprove = async (requestId: string) => {
    setLoading(true);
    try {
      await approveSettlementRequest(token, requestId);
      setSuccess('Request approved!');
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

  const sendLabel = role === 'CHILD' ? 'Send Nudge' : 'Send Encouragement';
  const placeholder =
    role === 'CHILD'
      ? 'Write a message to nudge your parent…'
      : 'Write an encouraging message…';

  return (
    <div className="space-y-4" data-testid="family-messages">
      {/* Pending settlement requests — parent only */}
      {role === 'PARENT' && pendingRequests.length > 0 && (
        <div className="space-y-2" data-testid="pending-requests">
          <h3 className="text-sm font-semibold text-gray-700">Pending Requests</h3>
          {pendingRequests.map((req) => (
            <div
              key={req.id}
              className="flex items-center justify-between p-3 bg-yellow-50 border border-yellow-200 rounded-md text-sm"
              data-testid={`pending-request-${req.id}`}
            >
              <div>
                <span className="font-medium">{req.requestType}</span>
                <span className="ml-2">${req.requestedAmount.toFixed(2)}</span>
                {req.note && <span className="ml-2 text-gray-500">— {req.note}</span>}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handleApprove(req.id)}
                  disabled={loading}
                  className="px-3 py-1 bg-green-600 text-white rounded-md text-xs font-medium hover:bg-green-700 disabled:opacity-50"
                  data-testid={`approve-${req.id}`}
                >
                  Approve
                </button>
                <button
                  onClick={() => handleReject(req.id)}
                  disabled={loading}
                  className="px-3 py-1 bg-red-600 text-white rounded-md text-xs font-medium hover:bg-red-700 disabled:opacity-50"
                  data-testid={`reject-${req.id}`}
                >
                  Reject
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Message compose form */}
      {(role === 'CHILD' || (role === 'PARENT' && targetChildId)) && (
        <form onSubmit={handleSend} className="space-y-2" data-testid="message-form">
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder={placeholder}
            maxLength={500}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            data-testid="message-body"
          />
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-400">{body.length}/500</span>
            <button
              type="submit"
              disabled={loading || !body.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
              data-testid="send-message"
            >
              {loading ? 'Sending…' : sendLabel}
            </button>
          </div>
        </form>
      )}

      {error && (
        <p className="text-sm text-red-600" data-testid="message-error">
          {error}
        </p>
      )}
      {success && (
        <p className="text-sm text-green-600" data-testid="message-success">
          {success}
        </p>
      )}

      {/* Message thread */}
      <div className="space-y-2" data-testid="message-thread">
        {messages.length === 0 ? (
          <p className="text-sm text-gray-500">No messages yet.</p>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`p-3 rounded-md border text-sm ${
                msg.senderRole === 'CHILD'
                  ? 'bg-blue-50 border-blue-200 text-blue-900'
                  : 'bg-green-50 border-green-200 text-green-900'
              }`}
              data-testid={`message-${msg.id}`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-medium text-xs uppercase tracking-wide">
                  {msg.messageType} · {msg.senderRole}
                </span>
                <span className="text-xs text-gray-400">
                  {new Date(msg.createdAt).toLocaleString()}
                </span>
              </div>
              <p>{msg.body}</p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
