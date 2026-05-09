import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'CHILD', firstName: 'Jamie' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('renders rewards heading', () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse({ totalEarned: 0, totalPaidOut: 0, totalSpent: 0, currentBalance: 0 }));

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /rewards/i })).toBeInTheDocument();
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

  it('submits spend and payout actions and refreshes summary', async () => {
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path.includes('/rewards/summary')) {
        return Promise.resolve(mockOkResponse({ totalEarned: 3, totalPaidOut: 1, totalSpent: 0.5, currentBalance: 1.5 }));
      }
      if (path.includes('/rewards?page=')) {
        return Promise.resolve(mockOkResponse({ rewards: [], totalCount: 0 }));
      }
      if (path.startsWith('/rewards/spend') && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse({}));
      }
      if (path.startsWith('/rewards/payout') && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse({}));
      }
      return Promise.resolve(mockOkResponse({}));
    });

    render(
      <MemoryRouter>
        <RewardsPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText(/spend amount/i), { target: { value: '0.50' } });
    fireEvent.change(screen.getByPlaceholderText(/what did you buy/i), { target: { value: 'Pencil' } });
    fireEvent.click(screen.getByRole('button', { name: /^spend$/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/rewards/spend?amount=0.50&note=Pencil',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));

    fireEvent.change(screen.getByPlaceholderText(/payout amount/i), { target: { value: '1.00' } });
    fireEvent.click(screen.getByRole('button', { name: /mark paid out/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/rewards/payout?amount=1.00',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));
  });
});
