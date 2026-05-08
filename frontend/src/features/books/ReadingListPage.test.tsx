import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { ReadingListPage } from './ReadingListPage';
import * as AuthContext from '../auth/AuthContext';
import * as api from '../../shared/api';

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => vi.fn() };
});

function mockOkResponse(data: unknown): Response {
  return {
    ok: true,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
    status: 200,
  } as unknown as Response;
}

describe('ReadingListPage', () => {
  beforeEach(() => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'CHILD', firstName: 'Jamie' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('shows loading then displays book titles from in-progress reads', async () => {
    const bookReads = [
      {
        bookReadId: 'br-1',
        googleBookId: 'g-1',
        title: "Charlotte's Web",
        description: 'A classic',
        thumbnailUrl: '',
        authors: ['E.B. White'],
        startDate: '2026-01-01',
        readCount: 0,
        readChapterIds: [],
      },
    ];

    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse(bookReads));
      if (path.includes('/chapters') && !path.includes('/chapterreads')) return Promise.resolve(mockOkResponse([]));
      if (path.includes('/chapterreads')) return Promise.resolve(mockOkResponse([]));
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText("Charlotte's Web")).toBeInTheDocument());
  });

  it('shows empty state when no books are in progress', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse([]));
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText(/no books/i)).toBeInTheDocument());
  });
});
