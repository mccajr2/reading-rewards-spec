import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { RewardsPage } from './RewardsPage';
import * as AuthContext from '../auth/AuthContext';
import * as api from '../../shared/api';

function mockOkResponse(data: unknown): Response {
  return {
    ok: true,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
    status: 200,
  } as unknown as Response;
}

describe('RewardsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'CHILD', firstName: 'Jamie' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('renders child rewards shop guidance heading', () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse({ totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 }));

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /your rewards shop/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/your rewards shop .* page guidance/i)).toBeInTheDocument();
  });

  it('renders parent rewards settings guidance heading for parent role', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '2', role: 'PARENT', firstName: 'Alex' },
      login: vi.fn(),
      logout: vi.fn(),
    });
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse({ totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 }));

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /rewards settings/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/rewards settings page guidance/i)).toBeInTheDocument();
  });

  it('displays balance from summary response', async () => {
    const summary = { totalEarned: 5.00, totalPaidOut: 2.00, totalSpent: 0.50, currentBalance: 2.50 };

    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path.includes('/rewards/summary')) return Promise.resolve(mockOkResponse(summary));
      return Promise.resolve(mockOkResponse({ rewards: [], totalCount: 0 }));
    });

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('$2.50')).toBeInTheDocument());
    expect(screen.getByText('$5.00')).toBeInTheDocument();
  });

  it('renders rewards history rows from paged API payload', async () => {
    const summary = { totalEarned: 2, totalPaidOut: 0, totalSpent: 0, currentBalance: 2 };
    const rewards = {
      rewards: [
        {
          id: 'r-1',
          type: 'EARN',
          amount: 1,
          createdAt: '2026-03-01T00:00:00Z',
          chapter: { id: 'c-1', name: 'Chapter 1' },
          bookRead: { book: { title: 'Charlotte\'s Web' } },
        },
        {
          id: 'r-2',
          type: 'SPEND',
          amount: 0.5,
          note: 'Sticker',
          createdAt: '2026-03-02T00:00:00Z',
        },
      ],
      totalCount: 2,
    };

    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path.includes('/rewards/summary')) return Promise.resolve(mockOkResponse(summary));
      if (path.includes('/rewards?page=')) return Promise.resolve(mockOkResponse(rewards));
      return Promise.resolve(mockOkResponse({ rewards: [], totalCount: 0 }));
    });

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText(/charlotte's web/i)).toBeInTheDocument());
    expect(screen.getByText(/chapter 1/i)).toBeInTheDocument();
    expect(screen.getByText(/sticker/i)).toBeInTheDocument();
  });

  it('renders child settlement request form instead of direct payout/spend', async () => {
    const user = userEvent.setup();
    const createSettlementSpy = vi.spyOn(api, 'createSettlementRequest').mockResolvedValue({
      id: 'req-1',
      childUserId: '1',
      requestType: 'SPEND',
      requestedAmount: 0.5,
      status: 'PENDING',
      note: 'Pencil',
      requestedAt: '2026-03-01T00:00:00Z',
    });
    vi.spyOn(api, 'listSettlementRequests').mockResolvedValue([]);
    vi.spyOn(api, 'getAllMessages').mockResolvedValue([]);

    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path.includes('/rewards/summary')) {
        return Promise.resolve(mockOkResponse({
          totalEarned: 3,
          totalPaidOut: 1,
          totalSpent: 0.5,
          currentBalance: 1.5,
          balancesByUnit: [
            { unitType: 'MONEY', unitLabel: 'USD', totalEarned: 3, totalPaidOut: 1, totalSpent: 0.5, currentBalance: 1.5 },
            { unitType: 'NON_MONEY', unitLabel: 'minutes screen time', totalEarned: 60, totalPaidOut: 0, totalSpent: 15, currentBalance: 45 },
          ],
        }));
      }
      if (path.includes('/reward-options')) {
        return Promise.resolve(mockOkResponse({
          options: [{
            id: 'opt-1',
            ownerUserId: 'parent-1',
            scopeType: 'FAMILY',
            name: 'Dollar Reward',
            description: '',
            valueType: 'MONEY',
            currencyCode: 'USD',
            moneyAmount: 1,
            earningBasis: 'PER_CHAPTER',
            active: true,
            createdAt: '2026-03-01T00:00:00Z',
            updatedAt: '2026-03-01T00:00:00Z',
          }],
          activeSelectionOptionId: 'opt-1',
        }));
      }
      if (path.includes('/rewards?page=')) {
        return Promise.resolve(mockOkResponse({ rewards: [], totalCount: 0 }));
      }
      return Promise.resolve(mockOkResponse({}));
    });

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText(/balances by reward unit/i)).toBeInTheDocument());
    expect(screen.getByTestId('payout-request-form')).toBeInTheDocument();
    expect(screen.queryByPlaceholderText(/payout amount/i)).not.toBeInTheDocument();

    await user.click(screen.getByTestId('type-spend'));
    await user.type(screen.getByTestId('amount-input'), '0.50');
    await user.type(screen.getByTestId('note-input'), 'Pencil');
    await user.click(screen.getByTestId('submit-request'));

    await waitFor(() => expect(createSettlementSpy).toHaveBeenCalledWith(
      'test-token',
      '1',
      expect.objectContaining({
        requestType: 'SPEND',
        requestedAmount: 0.5,
        note: 'Pencil',
        rewardOptionId: 'opt-1',
      })
    ));
  });
});
