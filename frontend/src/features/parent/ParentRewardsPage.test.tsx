import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { ParentRewardsPage } from './ParentRewardsPage';
import * as AuthContext from '../auth/AuthContext';
import * as api from '../../shared/api';

function mockOkResponse(data: unknown): Response {
  return {
    ok: true,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
    status: 200,
  } as unknown as Response;
}

describe('ParentRewardsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      token: 'test-token',
      user: { id: '1', role: 'PARENT', firstName: 'Alice' },
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('renders dedicated parent rewards guidance', async () => {
    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path) => {
      if (path === '/parent/kids') return Promise.resolve(mockOkResponse([]));
      if (path === '/reward-options') return Promise.resolve(mockOkResponse({ options: [] }));
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ParentRewardsPage />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: /manage rewards/i })).toBeInTheDocument());
  });

  it('creates reward option from reward configuration form', async () => {
    const user = userEvent.setup();
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/parent/kids' && !options?.method) {
        return Promise.resolve(mockOkResponse([{ id: 'k-1', firstName: 'Jamie', username: 'jamie' }]));
      }
      if (path === '/reward-options' && !options?.method) {
        return Promise.resolve(mockOkResponse({ options: [] }));
      }
      if (path === '/reward-options' && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse({ id: 'ro-1', name: 'Movie Night' }));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ParentRewardsPage />
      </MemoryRouter>
    );

    await user.clear(screen.getByPlaceholderText(/dollar amount/i));
    await user.type(screen.getByPlaceholderText(/dollar amount/i), '5.00');
    expect(screen.getByText(/\$5 per chapter/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /save reward option/i }));

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith(
      '/reward-options',
      'test-token',
      expect.objectContaining({ method: 'POST' })
    ));
    const postCall = fetchSpy.mock.calls.find(c => c[0] === '/reward-options' && c[2]?.method === 'POST');
    const payload = JSON.parse(String(postCall?.[2]?.body));
    expect(payload).not.toHaveProperty('name');
    expect(payload).not.toHaveProperty('description');
    await waitFor(() => expect(screen.getByText(/reward option created/i)).toBeInTheDocument());
  });

  it('supports child-scoped page milestone reward payload values', async () => {
    const user = userEvent.setup();
    const fetchSpy = vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/parent/kids' && !options?.method) {
        return Promise.resolve(mockOkResponse([{ id: 'k-1', firstName: 'Jamie', username: 'jamie' }]));
      }
      if (path === '/reward-options' && !options?.method) {
        return Promise.resolve(mockOkResponse({ options: [] }));
      }
      if (path === '/reward-options' && options?.method === 'POST') {
        return Promise.resolve(mockOkResponse({ id: 'ro-2', name: 'Pages Reward' }));
      }
      return Promise.resolve(mockOkResponse([]));
    });

    render(
      <MemoryRouter>
        <ParentRewardsPage />
      </MemoryRouter>
    );

    await user.selectOptions(screen.getByLabelText(/scope/i), 'CHILD');
    await user.selectOptions(screen.getByLabelText(/child/i), 'k-1');
    await user.selectOptions(screen.getByLabelText(/earning basis/i), 'PER_PAGE_MILESTONE');
    await user.clear(screen.getByPlaceholderText(/dollar amount/i));
    await user.type(screen.getByPlaceholderText(/dollar amount/i), '2');
    await user.type(screen.getByPlaceholderText(/page milestone size/i), '25');
    await user.click(screen.getByRole('button', { name: /save reward option/i }));

    await waitFor(() => {
      const postCall = fetchSpy.mock.calls.find(c => c[0] === '/reward-options' && c[2]?.method === 'POST');
      expect(postCall).toBeTruthy();
      const payload = JSON.parse(String(postCall?.[2]?.body));
      expect(payload.scopeType).toBe('CHILD');
      expect(payload.childUserId).toBe('k-1');
      expect(payload.earningBasis).toBe('PER_PAGE_MILESTONE');
      expect(payload.pageMilestoneSize).toBe(25);
    });
  });

  it('keeps the default reward view-only and prevents deactivating the last active option', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'fetchWithAuth').mockImplementation((path, _token, options) => {
      if (path === '/parent/kids' && !options?.method) {
        return Promise.resolve(mockOkResponse([]));
      }
      if (path === '/reward-options' && !options?.method) {
        return Promise.resolve(mockOkResponse({
          options: [
            {
              id: 'default-1',
              ownerUserId: '1',
              scopeType: 'FAMILY',
              name: '$1 per chapter',
              valueType: 'MONEY',
              moneyAmount: 1,
              earningBasis: 'PER_CHAPTER',
              active: true,
              createdAt: '2026-05-13T00:00:00Z',
              updatedAt: '2026-05-13T00:00:00Z',
            },
          ],
        }));
      }
      return Promise.resolve(mockOkResponse({}));
    });

    render(
      <MemoryRouter>
        <ParentRewardsPage />
      </MemoryRouter>
    );

    await screen.findByText(/default starter reward/i);
    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /deactivate/i })).toBeDisabled();

    expect(screen.getByText(/keep at least one reward option active/i)).toBeInTheDocument();
  });
});
