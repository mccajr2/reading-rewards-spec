import { useState } from 'react';
import {
  SettlementRequestDto,
  SettlementRequestType,
  RewardUnitBalanceDto,
  cancelSettlementRequest,
  createSettlementRequest,
  listSettlementRequests,
} from '../shared/api';
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input } from './shared';
import { Badge } from './ui/badge';

interface ChildPayoutRequestProps {
  token: string | null;
  childId: string;
  rewardOptionId?: string;
  balancesByUnit?: RewardUnitBalanceDto[];
  onSuccess?: () => void;
}

function formatAmount(amount: number, unitType?: string, unitLabel?: string) {
  if (unitType === 'NON_MONEY') {
    return `${amount.toFixed(2)} ${unitLabel ?? 'units'}`;
  }
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
 * Lets a child submit a payout or spend request to their parent for approval.
 */
export function ChildPayoutRequest({
  token,
  childId,
  rewardOptionId,
  balancesByUnit,
  onSuccess,
}: ChildPayoutRequestProps) {
  const [requests, setRequests] = useState<SettlementRequestDto[]>([]);
  const [requestType, setRequestType] = useState<SettlementRequestType>('PAYOUT');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);

  const activeBalance = balancesByUnit?.[0];

  const loadRequests = async () => {
    try {
      const data = await listSettlementRequests(token, childId);
      setRequests(data);
    } catch {
      // non-fatal
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    const parsedAmount = parseFloat(amount);
    if (!amount || isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Enter a valid amount greater than 0.');
      return;
    }

    setLoading(true);
    try {
      await createSettlementRequest(token, childId, {
        requestType,
        requestedAmount: parsedAmount,
        note: note.trim() || undefined,
        rewardOptionId,
      });
      setAmount('');
      setNote('');
      setSuccess(
        `Your ${requestType === 'PAYOUT' ? 'payout' : 'spend'} request has been sent to your parent!`
      );
      onSuccess?.();
      await loadRequests();
      setShowHistory(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (requestId: string) => {
    setLoading(true);
    try {
      await cancelSettlementRequest(token, requestId);
      await loadRequests();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cancel failed');
    } finally {
      setLoading(false);
    }
  };

  const toggleHistory = async () => {
    if (!showHistory) await loadRequests();
    setShowHistory((v) => !v);
  };

  return (
    <Card data-testid="payout-request-card">
      <CardHeader>
        <CardTitle>Request a Reward</CardTitle>
        <CardDescription>
          Ask your parent to approve a cash payout or a spend from your balance.
          {activeBalance && (
            <span className="mt-1 block font-medium text-text-primary">
              Available: {formatAmount(activeBalance.currentBalance, activeBalance.unitType, activeBalance.unitLabel)}
            </span>
          )}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit} className="space-y-3" data-testid="payout-request-form">
          <div className="grid grid-cols-2 gap-2">
            <Button
              type="button"
              variant={requestType === 'PAYOUT' ? 'default' : 'outline'}
              onClick={() => setRequestType('PAYOUT')}
              data-testid="type-payout"
            >
              Cash Payout
            </Button>
            <Button
              type="button"
              variant={requestType === 'SPEND' ? 'default' : 'outline'}
              onClick={() => setRequestType('SPEND')}
              data-testid="type-spend"
            >
              Spend Reward
            </Button>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium text-text-primary" htmlFor="request-amount">
              Amount
            </label>
            <Input
              id="request-amount"
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="e.g. 5.00"
              data-testid="amount-input"
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium text-text-primary" htmlFor="request-note">
              Note <span className="font-normal text-text-secondary">(optional)</span>
            </label>
            <Input
              id="request-note"
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="What is this for?"
              maxLength={200}
              data-testid="note-input"
            />
          </div>

          {error && (
            <p className="text-sm text-error" data-testid="payout-error">
              {error}
            </p>
          )}
          {success && (
            <p className="text-sm text-success" data-testid="payout-success">
              {success}
            </p>
          )}

          <Button type="submit" disabled={loading} className="w-full" data-testid="submit-request">
            {loading ? 'Sending…' : `Request ${requestType === 'PAYOUT' ? 'Cash Payout' : 'Spend'}`}
          </Button>
        </form>

        <Button variant="ghost" size="sm" onClick={toggleHistory} data-testid="toggle-history">
          {showHistory ? 'Hide' : 'View'} my requests
        </Button>

        {showHistory && (
          <div className="space-y-2" data-testid="request-history">
            {requests.length === 0 ? (
              <p className="text-sm text-text-secondary">No requests yet.</p>
            ) : (
              requests.map((req) => (
                <div
                  key={req.id}
                  className="flex items-center justify-between rounded-lg border border-border p-3 text-sm"
                  data-testid={`request-${req.id}`}
                >
                  <div>
                    <span className="font-medium">{req.requestType}</span>
                    <span className="ml-2 text-text-primary">
                      {formatAmount(req.requestedAmount)}
                    </span>
                    {req.note && (
                      <span className="ml-2 text-text-secondary">— {req.note}</span>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={statusBadgeVariant(req.status)} data-testid={`status-badge-${req.status.toLowerCase()}`}>
                      {req.status}
                    </Badge>
                    {req.status === 'PENDING' && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleCancel(req.id)}
                        disabled={loading}
                        data-testid={`cancel-${req.id}`}
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
