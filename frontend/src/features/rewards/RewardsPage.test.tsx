import { render, screen, waitFor } from '@testing-library/react';
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
});
