import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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

  it('submits add-kid form and refreshes kids list', async () => {
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/parent/kids' && !options?.method) {
        return Promise.resolve(mockOkResponse([{ id: 'k-1', firstName: 'Jamie', username: 'jamie' }]));
      }
      if (path === '/parent/kids' && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse('Child account created'));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText(/username/i), { target: { value: 'jamie' } });
    fireEvent.change(screen.getByPlaceholderText(/first name/i), { target: { value: 'Jamie' } });
    fireEvent.change(screen.getByPlaceholderText(/password/i), { target: { value: 'KidPass1!' } });
    fireEvent.click(screen.getByRole('button', { name: /add kid/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/parent/kids',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));
    await waitFor(() => expect(screen.getByText(/child account created/i)).toBeInTheDocument());
  });

  it('opens reset modal and posts reset password request', async () => {
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/parent/kids' && !options?.method) {
        return Promise.resolve(mockOkResponse([{ id: 'k-1', firstName: 'Jamie', username: 'jamie' }]));
      }
      if (path === '/parent/reset-child-password' && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse('Child password reset successfully'));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    const resetButton = await screen.findByRole('button', { name: /reset password/i });
    fireEvent.click(resetButton);

    fireEvent.change(screen.getByPlaceholderText(/new password/i), { target: { value: 'NewPass1!' } });
    const modal = screen.getByText(/reset password for/i).closest('.modal');
    expect(modal).not.toBeNull();
    fireEvent.click(within(modal as HTMLElement).getByRole('button', { name: /^reset password$/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/parent/reset-child-password',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));
    await waitFor(() => expect(screen.getByText(/password reset successfully/i)).toBeInTheDocument());
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
