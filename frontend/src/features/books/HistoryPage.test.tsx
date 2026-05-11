import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { HistoryPage } from './HistoryPage';
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

describe('HistoryPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'CHILD', firstName: 'Jamie' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('renders child reading progress guidance and history rows', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(
      mockOkResponse([
        {
          id: 'h-1',
          bookTitle: 'History Test Book',
          chapterName: 'Chapter One',
          completionDate: '2026-05-11T12:00:00.000Z',
        },
      ])
    );

    render(
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: /log your reading/i })).toBeInTheDocument());
    expect(screen.getByLabelText(/log your reading .* page guidance/i)).toBeInTheDocument();
    expect(screen.getByText('History Test Book')).toBeInTheDocument();
    expect(screen.getByText('Chapter One')).toBeInTheDocument();
  });
});
