import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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

function mockResponse(status: number, data: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
    status,
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

    await waitFor(() => expect(screen.getByRole('heading', { name: /your reading list/i })).toBeInTheDocument());
    expect(screen.getByLabelText(/your reading list .* page guidance/i)).toBeInTheDocument();
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

  it('shows chapter state and progress math from read chapter ids', async () => {
    const bookReads = [
      {
        bookReadId: 'br-2',
        googleBookId: 'g-2',
        title: 'Wonder',
        description: 'A story',
        thumbnailUrl: '',
        authors: ['R. J. Palacio'],
        startDate: '2026-01-01',
        readCount: 1,
        readChapterIds: ['c-1'],
      },
    ];

    const chapterRows = [
      { id: 'c-1', name: 'Part One', chapterIndex: 0 },
      { id: 'c-2', name: 'Part Two', chapterIndex: 1 },
    ];

    const chapterReads = [
      { chapterId: 'c-1', completionDate: '2026-02-01T00:00:00Z' },
    ];

    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse(bookReads));
      if (path === '/bookreads/br-2/chapters') return Promise.resolve(mockOkResponse(chapterRows));
      if (path === '/bookreads/br-2/chapterreads') return Promise.resolve(mockOkResponse(chapterReads));
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Wonder')).toBeInTheDocument());
    expect(screen.getByText('50%')).toBeInTheDocument();
    expect(screen.getByText('Part One')).toBeInTheDocument();
    expect(screen.getByText('Part Two')).toBeInTheDocument();
  });

  it('supports chapter rename UX and persists via API', async () => {
    const bookReads = [
      {
        bookReadId: 'br-3',
        googleBookId: 'g-3',
        title: 'Matilda',
        description: '',
        thumbnailUrl: '',
        authors: ['Roald Dahl'],
        startDate: '2026-01-01',
        readCount: 0,
        readChapterIds: [],
      },
    ];

    const chapterRows = [
      { id: 'c-rename', name: 'Chapter 1', chapterIndex: 0 },
    ];

    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse(bookReads));
      if (path === '/bookreads/br-3/chapters') return Promise.resolve(mockOkResponse(chapterRows));
      if (path === '/bookreads/br-3/chapterreads') return Promise.resolve(mockOkResponse([]));
      if (path === '/chapters/c-rename' && options?.method === 'PUT') {
        return Promise.resolve(mockOkResponse({ id: 'c-rename', name: 'The New Name', chapterIndex: 0 }));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    const chapterName = await screen.findByText('Chapter 1');
    fireEvent.doubleClick(chapterName);

    const editInput = await screen.findByDisplayValue('Chapter 1');
    fireEvent.change(editInput, { target: { value: 'The New Name' } });
    fireEvent.blur(editInput);

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/chapters/c-rename',
      'test-token',
      expect.objectContaining({ method: 'PUT' })
    ));
    await waitFor(() => expect(screen.getByText('The New Name')).toBeInTheDocument());
  });

  it('prompts for explicit reward option when completion event has multiple eligible options', async () => {
    const bookReads = [
      {
        bookReadId: 'br-4',
        googleBookId: 'g-4',
        title: 'Option Choice Book',
        description: '',
        thumbnailUrl: '',
        authors: ['Author'],
        startDate: '2026-01-01',
        bookEarningBasis: 'PER_CHAPTER',
        readCount: 0,
        readChapterIds: [],
      },
    ];

    const chapterRows = [
      { id: 'c-1', name: 'Chapter 1', chapterIndex: 0 },
      { id: 'c-2', name: 'Chapter 2', chapterIndex: 1 },
    ];

    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('2');

    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse(bookReads));
      if (path === '/bookreads/br-4/chapters') return Promise.resolve(mockOkResponse(chapterRows));
      if (path === '/bookreads/br-4/chapterreads') return Promise.resolve(mockOkResponse([]));
      if (path === '/bookreads/br-4/chapters/c-1/read' && options?.method === 'POST' && !options?.body) {
        return Promise.resolve(mockResponse(409, {
          availableOptions: [
            { id: 'opt-1', name: 'Option A' },
            { id: 'opt-2', name: 'Option B' },
          ],
        }));
      }
      if (path === '/bookreads/br-4/chapters/c-1/read' && options?.method === 'POST' && options?.body) {
        return Promise.resolve(mockOkResponse({}));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    const checkboxes = await screen.findAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);

    await waitFor(() => expect(promptSpy).toHaveBeenCalled());
    await waitFor(() => {
      const retryCall = fetchSpy.mock.calls.find(call =>
        call[0] === '/bookreads/br-4/chapters/c-1/read'
        && call[2]?.method === 'POST'
        && typeof call[2]?.body === 'string'
      );
      expect(retryCall).toBeTruthy();
      const body = JSON.parse(String(retryCall?.[2]?.body));
      expect(body.rewardOptionId).toBe('opt-2');
    });
  });

  it('shows a PER_BOOK mark as complete action and prompts for reward choice when needed', async () => {
    const bookReads = [
      {
        bookReadId: 'br-5',
        googleBookId: 'g-5',
        title: 'Per Book Choice',
        description: '',
        thumbnailUrl: '',
        authors: ['Author'],
        startDate: '2026-01-01',
        bookEarningBasis: 'PER_BOOK',
        readCount: 0,
        readChapterIds: [],
      },
    ];

    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('2');

    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/bookreads/in-progress') return Promise.resolve(mockOkResponse(bookReads));
      if (path === '/bookreads/br-5/chapters') return Promise.resolve(mockOkResponse([]));
      if (path === '/bookreads/br-5/chapterreads') return Promise.resolve(mockOkResponse([]));
      if (path === '/books/g-5/finish' && options?.method === 'POST' && !options?.body) {
        return Promise.resolve(mockResponse(409, {
          availableOptions: [
            { id: 'opt-1', name: 'Option A' },
            { id: 'opt-2', name: 'Option B' },
          ],
        }));
      }
      if (path === '/books/g-5/finish' && options?.method === 'POST' && options?.body) {
        return Promise.resolve(mockOkResponse({}));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ReadingListPage />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: /mark as complete/i }));

    await waitFor(() => expect(promptSpy).toHaveBeenCalled());
    await waitFor(() => {
      const retryCall = fetchSpy.mock.calls.find(call =>
        call[0] === '/books/g-5/finish'
        && call[2]?.method === 'POST'
        && typeof call[2]?.body === 'string'
      );
      expect(retryCall).toBeTruthy();
      const body = JSON.parse(String(retryCall?.[2]?.body));
      expect(body.rewardOptionId).toBe('opt-2');
    });
  });
});
