import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import * as AuthContext from '../auth/AuthContext';
import * as api from '../../shared/api';
import { SearchPage } from './SearchPage';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

function okJson(data: unknown): Response {
  return {
    ok: true,
    status: 200,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
  } as unknown as Response;
}

describe('SearchPage chapter seeding', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    mockNavigate.mockReset();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: 'u1', role: 'CHILD', firstName: 'Kid' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('opens a chapter count modal and seeds chapters when none exist', async () => {
    const fetchMock = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path.startsWith('/search?')) {
        return Promise.resolve(okJson([
          {
            googleBookId: 'works/OL82563W',
            title: 'Harry Potter and the Philosopher\'s Stone',
            authors: ['J. K. Rowling'],
            description: 'desc',
            thumbnailUrl: null,
          },
        ]));
      }
      if (path === '/books' && options?.method === 'POST') {
        return Promise.resolve(okJson({ id: 'br-1', googleBookId: 'works/OL82563W' }));
      }
      if (path === '/children/u1/books/works/OL82563W/basis-selection' && options?.method === 'PUT') {
        return Promise.resolve(okJson({ bookReadId: 'br-1', bookEarningBasis: 'PER_CHAPTER' }));
      }
      if (path === '/bookreads/br-1/chapters' && !options?.method) {
        return Promise.resolve(okJson([]));
      }
      if (path === '/bookreads/br-1/chapters' && options?.method === 'POST') {
        return Promise.resolve(okJson([{ id: 'c1' }, { id: 'c2' }, { id: 'c3' }]));
      }
      return Promise.resolve(okJson([]));
    });

    render(
      <MemoryRouter>
        <SearchPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /find your next book/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/find your next book .* page guidance/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/title/i), { target: { value: 'Harry Potter' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));

    await waitFor(() => expect(screen.getByText(/harry potter and the philosopher's stone/i)).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /add to reading list/i }));

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/reward tracking/i), { target: { value: 'PER_CHAPTER' } });
    fireEvent.change(screen.getByLabelText(/chapter count/i), { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: /save chapters/i }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/children/u1/books/works/OL82563W/basis-selection',
        'test-token',
        expect.objectContaining({ method: 'PUT' })
      );
      expect(fetchMock).toHaveBeenCalledWith(
        '/bookreads/br-1/chapters',
        'test-token',
        expect.objectContaining({ method: 'POST' })
      );
    });
    expect(mockNavigate).toHaveBeenCalledWith('/reading-list', { state: { newGoogleBookId: 'works/OL82563W' } });
  });

  it('reuses existing shared chapters without opening the chapter count modal', async () => {
    const fetchMock = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path.startsWith('/search?')) {
        return Promise.resolve(okJson([
          {
            googleBookId: 'works/OL82563W',
            title: 'Harry Potter and the Chamber of Secrets',
            authors: ['J. K. Rowling'],
            description: 'desc',
            thumbnailUrl: null,
          },
        ]));
      }
      if (path === '/books' && options?.method === 'POST') {
        return Promise.resolve(okJson({ id: 'br-2', googleBookId: 'works/OL82563W' }));
      }
      if (path === '/children/u1/books/works/OL82563W/basis-selection' && options?.method === 'PUT') {
        return Promise.resolve(okJson({ bookReadId: 'br-2', bookEarningBasis: 'PER_BOOK' }));
      }
      if (path === '/bookreads/br-2/chapters' && !options?.method) {
        return Promise.resolve(okJson([{ id: 'c1', name: 'Chapter 1', chapterIndex: 1 }]));
      }
      if (path === '/bookreads/br-2/chapters' && options?.method === 'POST') {
        return Promise.resolve(okJson([]));
      }
      return Promise.resolve(okJson([]));
    });

    render(
      <MemoryRouter>
        <SearchPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText(/title/i), { target: { value: 'Harry Potter' } });
    fireEvent.click(screen.getByRole('button', { name: /search/i }));

    await waitFor(() => expect(screen.getByText(/harry potter and the chamber of secrets/i)).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /add to reading list/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/reward tracking/i), { target: { value: 'PER_BOOK' } });
    fireEvent.click(screen.getByRole('button', { name: /save chapters/i }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalled());
    expect(fetchMock).toHaveBeenCalledWith(
      '/children/u1/books/works/OL82563W/basis-selection',
      'test-token',
      expect.objectContaining({ method: 'PUT' })
    );
    expect(fetchMock).not.toHaveBeenCalledWith(
      '/bookreads/br-2/chapters',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    );
  });
});
