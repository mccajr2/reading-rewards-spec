import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ChildPayoutRequest } from '../../components/ChildPayoutRequest';
import { FamilyMessages } from '../../components/FamilyMessages';
import * as api from '../../shared/api';

// ── Helpers ───────────────────────────────────────────────────────────────────

const TOKEN = 'test-token';
const CHILD_ID = 'child-uuid-1';

function mockSettlement(overrides = {}): api.SettlementRequestDto {
  return {
    id: 'req-1',
    childUserId: CHILD_ID,
    requestType: 'PAYOUT',
    requestedAmount: 5,
    status: 'PENDING',
    requestedAt: new Date().toISOString(),
    ...overrides,
  };
}

function mockMessage(overrides = {}): api.FamilyMessageDto {
  return {
    id: 'msg-1',
    senderRole: 'CHILD',
    senderUserId: CHILD_ID,
    recipientUserId: 'parent-uuid-1',
    messageType: 'NUDGE',
    body: 'Can you approve?',
    emailNotificationSent: true,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

// ── ChildPayoutRequest tests ──────────────────────────────────────────────────

describe('ChildPayoutRequest', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('renders the form with PAYOUT selected by default', () => {
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([]);

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
    expect(screen.getByTestId('type-payout')).toBeInTheDocument();
    expect(screen.getByTestId('type-spend')).toBeInTheDocument();
    expect(screen.getByTestId('amount-input')).toBeInTheDocument();
  });

  it('shows success message after submitting a payout request', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'createSettlementRequest').mockResolvedValue(mockSettlement());
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([mockSettlement()]);

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    await user.type(screen.getByTestId('amount-input'), '5.00');
    await user.click(screen.getByTestId('submit-request'));

    await waitFor(() =>
      expect(screen.getByTestId('payout-success')).toBeInTheDocument()
    );
    expect(screen.getByTestId('payout-success')).toHaveTextContent(/payout/i);
  });

  it('shows error when amount is 0 or empty', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'createSettlementRequest').mockResolvedValue(mockSettlement());

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    await user.clear(screen.getByTestId('amount-input'));
    await user.type(screen.getByTestId('amount-input'), '0');
    await user.click(screen.getByTestId('submit-request'));

    expect(screen.getByTestId('payout-error')).toHaveTextContent(/valid amount/i);
    expect(api.createSettlementRequest).not.toHaveBeenCalled();
  });

  it('can toggle between PAYOUT and SPEND', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'createSettlementRequest').mockResolvedValue(
      mockSettlement({ requestType: 'SPEND' })
    );
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([]);

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    await user.click(screen.getByTestId('type-spend'));
    await user.type(screen.getByTestId('amount-input'), '2.50');
    await user.click(screen.getByTestId('submit-request'));

    await waitFor(() =>
      expect(api.createSettlementRequest).toHaveBeenCalledWith(
        TOKEN,
        CHILD_ID,
        expect.objectContaining({ requestType: 'SPEND', requestedAmount: 2.5 })
      )
    );
  });

  it('displays request history when toggled', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([
      mockSettlement({ status: 'PENDING' }),
      mockSettlement({ id: 'req-2', status: 'APPROVED' }),
    ]);

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    await user.click(screen.getByTestId('toggle-history'));

    await waitFor(() =>
      expect(screen.getByTestId('request-history')).toBeInTheDocument()
    );
    expect(screen.getByTestId('status-badge-pending')).toBeInTheDocument();
    expect(screen.getByTestId('status-badge-approved')).toBeInTheDocument();
  });

  it('can cancel a pending request', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([mockSettlement()]);
    vi.spyOn(api, 'cancelSettlementRequest').mockResolvedValue(
      mockSettlement({ status: 'CANCELLED' })
    );

    render(<ChildPayoutRequest token={TOKEN} childId={CHILD_ID} />);

    await user.click(screen.getByTestId('toggle-history'));
    await waitFor(() =>
      expect(screen.getByTestId('cancel-req-1')).toBeInTheDocument()
    );

    await user.click(screen.getByTestId('cancel-req-1'));

    expect(api.cancelSettlementRequest).toHaveBeenCalledWith(TOKEN, 'req-1');
  });
});

// ── FamilyMessages tests ──────────────────────────────────────────────────────

describe('FamilyMessages — child (nudge)', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('renders nudge compose form for child', () => {
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);

    render(
      <FamilyMessages token={TOKEN} role="CHILD" />
    );

    expect(screen.getByTestId('message-form')).toBeInTheDocument();
    expect(screen.getByTestId('message-body')).toBeInTheDocument();
    expect(screen.getByTestId('send-message')).toHaveTextContent(/nudge/i);
  });

  it('sends a nudge and shows success', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);
    vi.spyOn(api, 'sendNudge').mockResolvedValue(mockMessage());

    render(<FamilyMessages token={TOKEN} role="CHILD" />);

    await user.type(screen.getByTestId('message-body'), 'Can you approve?');
    await user.click(screen.getByTestId('send-message'));

    await waitFor(() =>
      expect(screen.getByTestId('message-success')).toHaveTextContent(/nudge sent/i)
    );
    expect(api.sendNudge).toHaveBeenCalledWith(TOKEN, 'Can you approve?');
  });

  it('shows cooldown error when 429 nudge is returned', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);
    vi.spyOn(api, 'sendNudge').mockRejectedValue(new Error('NUDGE_COOLDOWN'));

    render(<FamilyMessages token={TOKEN} role="CHILD" />);

    await user.type(screen.getByTestId('message-body'), 'Second nudge');
    await user.click(screen.getByTestId('send-message'));

    await waitFor(() =>
      expect(screen.getByTestId('message-error')).toHaveTextContent(/24 hours/i)
    );
  });
});

describe('FamilyMessages — parent (encouragement + approvals)', () => {
  beforeEach(() => vi.restoreAllMocks());

  const PARENT_CHILD_ID = 'child-uuid-2';

  it('renders encouragement form for parent with targetChildId', () => {
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    expect(screen.getByTestId('send-message')).toHaveTextContent(/encouragement/i);
  });

  it('sends encouragement and shows success', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);
    vi.spyOn(api, 'sendEncouragement').mockResolvedValue(
      mockMessage({ senderRole: 'PARENT', messageType: 'ENCOURAGEMENT' })
    );

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    await user.type(screen.getByTestId('message-body'), 'Great job!');
    await user.click(screen.getByTestId('send-message'));

    await waitFor(() =>
      expect(screen.getByTestId('message-success')).toHaveTextContent(/encouragement sent/i)
    );
    expect(api.sendEncouragement).toHaveBeenCalledWith(
      TOKEN, PARENT_CHILD_ID, 'Great job!'
    );
  });

  it('shows pending requests and approve/reject buttons', async () => {
    const pendingReq = mockSettlement({ id: 'req-pend-1' });
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([pendingReq]);

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    await waitFor(() =>
      expect(screen.getByTestId('pending-requests')).toBeInTheDocument()
    );
    expect(screen.getByTestId('approve-req-pend-1')).toBeInTheDocument();
    expect(screen.getByTestId('reject-req-pend-1')).toBeInTheDocument();
  });

  it('approves a pending request', async () => {
    const user = userEvent.setup();
    const pendingReq = mockSettlement({ id: 'req-app-1' });
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests')
      .mockResolvedValueOnce([pendingReq])
      .mockResolvedValue([]);
    vi.spyOn(api, 'approveSettlementRequest').mockResolvedValue(
      mockSettlement({ id: 'req-app-1', status: 'APPROVED' })
    );

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    await waitFor(() => screen.getByTestId('approve-req-app-1'));
    await user.click(screen.getByTestId('approve-req-app-1'));

    await waitFor(() =>
      expect(api.approveSettlementRequest).toHaveBeenCalledWith(TOKEN, 'req-app-1')
    );
    expect(screen.getByTestId('message-success')).toHaveTextContent(/approved/i);
  });

  it('rejects a pending request', async () => {
    const user = userEvent.setup();
    const pendingReq = mockSettlement({ id: 'req-rej-1' });
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);
    vi.spyOn(api, 'listParentPendingRequests')
      .mockResolvedValueOnce([pendingReq])
      .mockResolvedValue([]);
    vi.spyOn(api, 'rejectSettlementRequest').mockResolvedValue(
      mockSettlement({ id: 'req-rej-1', status: 'REJECTED' })
    );

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    await waitFor(() => screen.getByTestId('reject-req-rej-1'));
    await user.click(screen.getByTestId('reject-req-rej-1'));

    await waitFor(() =>
      expect(api.rejectSettlementRequest).toHaveBeenCalledWith(TOKEN, 'req-rej-1')
    );
  });

  it('displays message thread for both roles', async () => {
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([
      mockMessage({ id: 'msg-a', messageType: 'NUDGE', senderRole: 'CHILD' }),
      mockMessage({ id: 'msg-b', messageType: 'ENCOURAGEMENT', senderRole: 'PARENT' }),
    ]);
    vi.spyOn(api, 'listParentPendingRequests').mockResolvedValue([]);

    render(
      <FamilyMessages token={TOKEN} role="PARENT" targetChildId={PARENT_CHILD_ID} />
    );

    await waitFor(() =>
      expect(screen.getByTestId('message-thread')).toBeInTheDocument()
    );
    expect(screen.getByTestId('message-msg-a')).toBeInTheDocument();
    expect(screen.getByTestId('message-msg-b')).toBeInTheDocument();
  });
});
