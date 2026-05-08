import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { LoginPage } from './LoginPage';
import * as AuthContext from './AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

function renderWithAuth(loginFn: () => Promise<void>) {
  vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
    login: loginFn,
    logout: vi.fn(),
    token: null,
    user: null,
  });

  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('renders login heading and fields', () => {
    renderWithAuth(vi.fn());
    expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/username or email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('calls login and navigates on success', async () => {
    const loginFn = vi.fn().mockResolvedValue(undefined);
    renderWithAuth(loginFn);

    fireEvent.change(screen.getByLabelText(/username or email/i), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(loginFn).toHaveBeenCalledWith('user@example.com', 'password123'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
  });

  it('shows error message when login fails', async () => {
    const loginFn = vi.fn().mockRejectedValue(new Error('Invalid credentials'));
    renderWithAuth(loginFn);

    fireEvent.change(screen.getByLabelText(/username or email/i), { target: { value: 'bad@example.com' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrongpass' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument());
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
