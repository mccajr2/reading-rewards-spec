import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, vi } from 'vitest';
import { App } from '../../app/App';

describe('Rewards navigation role gating', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => ({ dollars: 0 }),
      text: async () => '',
      headers: new Headers(),
    } as Response);
  });

  afterEach(() => {
    vi.restoreAllMocks();
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
