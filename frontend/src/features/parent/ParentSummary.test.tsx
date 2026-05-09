import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { ParentSummary } from './ParentSummary';
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

describe('ParentSummary drill-down wiring', () => {
  beforeEach(() => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'PARENT', firstName: 'Alice' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('navigates from summary list into child detail route', async () => {
    vi.spyOn(api, 'fetchWithAuth')
      .mockResolvedValueOnce(
        mockOkResponse({
          kids: [
            {
              id: 'kid-1',
              firstName: 'Jamie',
              username: 'jamie',
              booksRead: 2,
              chaptersRead: 10,
              totalEarned: 3.5,
              currentBalance: 1.5,
            },
          ],
        })
      )
      .mockResolvedValueOnce(
        mockOkResponse({
          child: {
            id: 'kid-1',
            firstName: 'Jamie',
            username: 'jamie',
          },
          books: [],
          rewards: [],
          totalEarned: 3.5,
          currentBalance: 1.5,
        })
      );

    render(
      <MemoryRouter initialEntries={['/parent/summary']}>
        <Routes>
          <Route path="/parent/summary" element={<ParentSummary />} />
          <Route path="/parent/summary/:childId" element={<ParentSummary />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText("Children's Summary")).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /view details for jamie/i }));

    await waitFor(() => expect(screen.getByRole('heading', { name: /child detail/i })).toBeInTheDocument());
    expect(screen.getByText('Jamie')).toBeInTheDocument();
    expect(screen.getByText(/username: jamie/i)).toBeInTheDocument();
  });
});
