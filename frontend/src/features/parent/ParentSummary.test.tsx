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
    vi.restoreAllMocks();
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

    await waitFor(() => expect(screen.getByRole('heading', { name: /manage child accounts/i })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /view details for jamie/i }));

    await waitFor(() => expect(screen.getByRole('heading', { name: /child account details/i })).toBeInTheDocument());
    expect(screen.getByText('Jamie')).toBeInTheDocument();
    expect(screen.getByText(/username: jamie/i)).toBeInTheDocument();
  });

  it('renders child-detail books, chapter ledger, and reward history', async () => {
    vi.spyOn(api, 'fetchWithAuth')
      .mockResolvedValueOnce(
        mockOkResponse({
          kids: [
            {
              id: 'kid-1',
              firstName: 'Jamie',
              username: 'jamie',
              booksRead: 1,
              chaptersRead: 1,
              totalEarned: 1,
              currentBalance: 1,
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
          books: [
            {
              bookReadId: 'br-1',
              googleBookId: 'gb-1',
              title: 'Charlotte\'s Web',
              authors: ['E.B. White'],
              thumbnailUrl: '',
              startDate: '2026-05-08T10:00:00.000Z',
              endDate: undefined,
              inProgress: true,
              chapters: [
                {
                  id: 'ch-1',
                  index: 0,
                  name: 'Chapter 1',
                  isRead: true,
                  chapterReadId: 'cr-1',
                  earnedReward: 1,
                },
              ],
            },
          ],
          rewards: [
            {
              id: 'rw-1',
              type: 'EARN',
              amount: 1,
              chapterReadId: 'cr-1',
              note: 'seed reward',
              createdAt: '2026-05-08T10:05:00.000Z',
            },
          ],
          totalEarned: 1,
          currentBalance: 1,
        })
      );

    render(
      <MemoryRouter initialEntries={['/parent/summary/kid-1']}>
        <Routes>
          <Route path="/parent/summary/:childId" element={<ParentSummary />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: /child account details/i })).toBeInTheDocument());
    expect(screen.getByText('Charlotte\'s Web')).toBeInTheDocument();
    expect(screen.getByText(/chapters \(1\/1 read\)/i)).toBeInTheDocument();
    expect(screen.getByText('EARN')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /reward history/i })).toBeInTheDocument();
    expect(screen.getByText('seed reward')).toBeInTheDocument();
  });

  it('supports reversal confirm flow and refreshes child detail after success', async () => {
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth')
      .mockResolvedValueOnce(
        mockOkResponse({
          kids: [
            {
              id: 'kid-1',
              firstName: 'Jamie',
              username: 'jamie',
              booksRead: 1,
              chaptersRead: 1,
              totalEarned: 1,
              currentBalance: 1,
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
          books: [
            {
              bookReadId: 'br-1',
              googleBookId: 'gb-1',
              title: 'Book',
              authors: ['Auth'],
              thumbnailUrl: '',
              startDate: '2026-05-08T10:00:00.000Z',
              endDate: undefined,
              inProgress: true,
              chapters: [
                {
                  id: 'ch-1',
                  index: 0,
                  name: 'Chapter 1',
                  isRead: true,
                  chapterReadId: 'cr-1',
                  earnedReward: 1,
                },
              ],
            },
          ],
          rewards: [
            {
              id: 'rw-1',
              type: 'EARN',
              amount: 1,
              chapterReadId: 'cr-1',
              note: 'seed reward',
              createdAt: '2026-05-08T10:05:00.000Z',
            },
          ],
          totalEarned: 1,
          currentBalance: 1,
        })
      )
      .mockResolvedValueOnce(mockOkResponse({}))
      .mockResolvedValueOnce(
        mockOkResponse({
          child: {
            id: 'kid-1',
            firstName: 'Jamie',
            username: 'jamie',
          },
          books: [
            {
              bookReadId: 'br-1',
              googleBookId: 'gb-1',
              title: 'Book',
              authors: ['Auth'],
              thumbnailUrl: '',
              startDate: '2026-05-08T10:00:00.000Z',
              endDate: undefined,
              inProgress: true,
              chapters: [
                {
                  id: 'ch-1',
                  index: 0,
                  name: 'Chapter 1',
                  isRead: false,
                },
              ],
            },
          ],
          rewards: [],
          totalEarned: 0,
          currentBalance: 0,
        })
      );

    render(
      <MemoryRouter initialEntries={['/parent/summary/kid-1']}>
        <Routes>
          <Route path="/parent/summary/:childId" element={<ParentSummary />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: /child account details/i })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /reverse/i }));
    fireEvent.click(screen.getByRole('button', { name: /confirm/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('/parent/kid-1/chapter-reads/cr-1/reverse', 'test-token', {
        method: 'POST',
      });
    });

    await waitFor(() => expect(screen.getByText('Not read')).toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: /reward history/i })).not.toBeInTheDocument();
  });
});
