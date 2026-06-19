import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

  it('renders parent dashboard guidance heading', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockResolvedValue(mockOkResponse([]));

    render(
      <MemoryRouter>
        <ParentDashboard />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /your dashboard/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/your dashboard page guidance/i)).toBeInTheDocument();
  });

    it('keeps reward configuration off the manage kids page', async () => {
      vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
        if (path === '/parent/kids') {
          return Promise.resolve(mockOkResponse([]));
        }
        if (path === '/parent/kids/summary') {
          return Promise.resolve(mockOkResponse({ kids: [] }));
        }
        return Promise.resolve(mockOkResponse([]));
      });

      render(
        <MemoryRouter>
          <ParentDashboard />
        </MemoryRouter>
      );

      await waitFor(() => expect(screen.getByText(/your kids/i)).toBeInTheDocument());
      expect(screen.queryByRole('heading', { name: /reward options/i })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /manage rewards/i })).toBeInTheDocument();
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
    const user = userEvent.setup();
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

    await user.type(screen.getByPlaceholderText(/username/i), 'jamie');
    await user.type(screen.getByPlaceholderText(/first name/i), 'Jamie');
    await user.type(screen.getByPlaceholderText(/password/i), 'KidPass1!');
    await user.click(screen.getByRole('button', { name: /add kid/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/parent/kids',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));
    await waitFor(() => expect(screen.getByText(/child account created/i)).toBeInTheDocument());
  });

  it('opens reset modal and posts reset password request', async () => {
    const user = userEvent.setup();
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
    await user.click(resetButton);

    await user.type(screen.getByPlaceholderText(/new password/i), 'NewPass1!');
    const modal = screen.getByText(/reset password for/i).closest('.modal');
    expect(modal).not.toBeNull();
    await user.click(within(modal as HTMLElement).getByRole('button', { name: /^reset password$/i }));

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
