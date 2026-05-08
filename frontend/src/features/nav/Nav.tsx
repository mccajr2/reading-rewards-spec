import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import './Nav.css';

export function Nav() {
  const { user, logout, token } = useAuth();
  const [credits, setCredits] = useState<number | null>(null);
  const location = useLocation();

  const loadCredits = async () => {
    if (!token) return;
    const res = await fetchWithAuth('/credits', token);
    if (res.ok) {
      const data = await res.json();
      setCredits(data.dollars ?? 0);
    }
  };

  useEffect(() => {
    loadCredits();
    (window as any).updateCredits = loadCredits;
    return () => { delete (window as any).updateCredits; };
  }, [token]);

  const isActive = (path: string) => location.pathname === path ? 'nav-link active' : 'nav-link';

  return (
    <nav className="app-nav">
      <div className="nav-left">
        <span className="nav-brand">📚 Reading Rewards</span>
        <Link className={isActive('/search')} to="/search">Search</Link>
        <Link className={isActive('/reading-list')} to="/reading-list">My Books</Link>
        <Link className={isActive('/history')} to="/history">History</Link>
        <Link className={isActive('/rewards')} to="/rewards">Rewards</Link>
        {user?.role === 'PARENT' && (
          <Link className={isActive('/parent')} to="/parent">Manage Kids</Link>
        )}
      </div>
      <div className="nav-right">
        {credits !== null && (
          <span className="credits-badge">${credits.toFixed(2)}</span>
        )}
        <span className="nav-username">{user?.firstName ?? user?.username ?? user?.email}</span>
        <button className="nav-logout" onClick={logout}>Logout</button>
      </div>
    </nav>
  );
}
