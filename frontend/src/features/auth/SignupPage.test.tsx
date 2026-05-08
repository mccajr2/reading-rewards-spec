import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { SignupPage } from './SignupPage';
import * as api from '../../shared/api';

describe('SignupPage', () => {
  it('renders signup heading and fields', () => {
    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /parent signup/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('shows success message after successful signup', async () => {
    vi.spyOn(api, 'postJson').mockResolvedValue('Signup complete. Check your email.');

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'parent@example.com' } });
    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'Jane' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password1!' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(screen.getByText(/signup complete/i)).toBeInTheDocument());
  });

  it('shows error message on signup failure', async () => {
    vi.spyOn(api, 'postJson').mockRejectedValue(new Error('Email already in use'));

    render(
      <MemoryRouter>
        <SignupPage />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'existing@example.com' } });
    fireEvent.change(screen.getByLabelText(/first name/i), { target: { value: 'Jane' } });
    fireEvent.change(screen.getByLabelText(/last name/i), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'Password1!' } });
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => expect(screen.getByText(/email already in use/i)).toBeInTheDocument());
  });
});
