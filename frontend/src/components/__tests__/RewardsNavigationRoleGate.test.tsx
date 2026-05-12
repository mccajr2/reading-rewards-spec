import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../../app/App';

describe('Rewards navigation role gating', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows Manage Rewards nav for parent', () => {
    localStorage.setItem('jwtToken', 'parent-token');
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'p1',
        role: 'PARENT',
        firstName: 'Parent',
      })
    );

    render(
      <MemoryRouter initialEntries={['/parent/rewards']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getAllByText(/manage rewards/i).length).toBeGreaterThan(0);
    expect(screen.queryByRole('link', { name: /^rewards$/i })).not.toBeInTheDocument();
  });

  it('shows Rewards nav for child', () => {
    localStorage.setItem('jwtToken', 'child-token');
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'c1',
        role: 'CHILD',
        firstName: 'Child',
      })
    );

    render(
      <MemoryRouter initialEntries={['/child/rewards']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getAllByText(/^rewards$/i).length).toBeGreaterThan(0);
    expect(screen.queryByRole('link', { name: /manage rewards/i })).not.toBeInTheDocument();
  });
});
