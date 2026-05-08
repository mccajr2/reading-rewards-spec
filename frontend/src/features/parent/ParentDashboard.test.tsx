import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { ParentDashboard } from './ParentDashboard';
import * as AuthContext from '../auth/AuthContext';
import * as api from '../../shared/api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

function mockOkResponse(data: unknown): Response {
  return {
    ok: true,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
    status: 200,
  } as unknown as Response;
}

describe('ParentDashboard', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'PARENT', firstName: 'Alice' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('renders manage kids heading', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse([]));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /manage kids/i })).toBeInTheDocument();
  });

  it('displays child names returned from the API', async () => {
    const kids = [
      { id: 'k-1', firstName: 'Jamie', username: 'jamie' },
      { id: 'k-2', firstName: 'Sam', username: 'sam123' },
    ];
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse(kids));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Jamie')).toBeInTheDocument());
    expect(screen.getByText('Sam')).toBeInTheDocument();
  });

  it('navigates to child detail from manage kids table', async () => {
    const kids = [{ id: 'k-1', firstName: 'Jamie', username: 'jamie' }];
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse(kids));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    const button = await screen.findByRole('button', { name: /view details for jamie/i });
    fireEvent.click(button);

    expect(mockNavigate).toHaveBeenCalledWith('/parent/summary/k-1');
  });

  it('shows no-kids message when list is empty', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse([]));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText(/no kids yet/i)).toBeInTheDocument());
  });

  it('redirects non-parent users away', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '2', role: 'CHILD', firstName: 'Jamie' },
      login: vi.fn(),
      logout: vi.fn(),
    });

    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse([]));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
