import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';

describe('App routing', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows login page when no token is present', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
  });

  it('shows authenticated layout with nav when token and user exist', () => {
    localStorage.setItem('jwtToken', 'demo-token');
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: '1',
        role: 'CHILD',
        firstName: 'Jamie'
      })
    );

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/reading rewards/i)).toBeInTheDocument();
  });
});
