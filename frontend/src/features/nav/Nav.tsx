import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import { Navigation, type NavigationItem } from '../../components/shared';

export function Nav() {
  const { user, logout, token } = useAuth();
  const [credits, setCredits] = useState<number | null>(null);
  const location = useLocation();

  const loadCredits = async () => {
    if (!token) return;
    try {
      const res = await fetchWithAuth('/credits', token);
      if (res.ok) {
        const data = await res.json();
        setCredits(data.dollars ?? 0);
      }
    } catch {
      // Keep nav usable when API is unavailable (e.g., isolated frontend tests).
    }
  };

  useEffect(() => {
    loadCredits();
    (window as any).updateCredits = loadCredits;
    return () => { delete (window as any).updateCredits; };
  }, [token]);

  const items: NavigationItem[] = [
    { label: 'Search', to: '/search' },
    { label: 'My Books', to: '/reading-list' },
    { label: 'History', to: '/history' },
    { label: 'Rewards', to: '/rewards' },
    { label: 'Manage Kids', to: '/parent', visible: user?.role === 'PARENT' }
  ];

  return (
    <Navigation
      brand="Reading Rewards"
      items={items}
      activePath={location.pathname}
      creditsText={credits !== null ? `$${credits.toFixed(2)}` : undefined}
      userText={user?.firstName ?? user?.username ?? user?.email ?? undefined}
      onLogout={logout}
    />
  );
}
