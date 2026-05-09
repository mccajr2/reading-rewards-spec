import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { LoginPage } from './LoginPage';
import { SignupPage } from './SignupPage';
import { VerifyEmailPage } from './VerifyEmailPage';
import * as AuthContext from './AuthContext';
import * as api from '../../shared/api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [new URLSearchParams('token=test-token')],
  };
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
    vi.restoreAllMocks();
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

  it('submits signup form and shows backend success message', async () => {
    vi.spyOn(api, 'postJson').mockResolvedValue('Signup successful. Please check your email to verify your account.');

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'parent@example.com' } });
    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'Pat' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: 'Parent' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password1!' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(api.postJson).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByText(/signup successful/i)).toBeInTheDocument());
  });

  it('shows verification success message when token verifies', async () => {
    vi.spyOn(api, 'getText').mockResolvedValue('Email verified. You can now log in.');

    render(
      <MemoryRouter>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(api.getText).toHaveBeenCalledWith('/auth/verify-email?token=test-token'));
    await waitFor(() => expect(screen.getByText(/email verified/i)).toBeInTheDocument());
  });
});
