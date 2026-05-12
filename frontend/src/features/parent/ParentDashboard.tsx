import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import { Button, Input, Modal, PageGuidance } from '../../components/shared';
import './ParentDashboard.css';

type Kid = {
  id: string;
  firstName: string;
  username: string;
};

type KidSummary = {
  id: string;
  firstName: string;
  username: string;
  booksRead: number;
  chaptersRead: number;
  totalEarned: number;
  currentBalance: number;
};

export function ParentDashboard() {
  const { user, token } = useAuth();
  const navigate = useNavigate();

  const [kids, setKids] = useState<Kid[]>([]);
  const [kidSummaries, setKidSummaries] = useState<KidSummary[]>([]);
  const [form, setForm] = useState({ username: '', firstName: '', password: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [resetTarget, setResetTarget] = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [resetMsg, setResetMsg] = useState('');

  useEffect(() => {
    if (!user || user.role !== 'PARENT') { navigate('/'); return; }
    loadKids();
    loadKidSummaries();
  }, []);

  async function loadKids() {
    const r = await fetchWithAuth('/parent/kids', token);
    if (r.ok) setKids(await r.json());
  }

  async function loadKidSummaries() {
    const r = await fetchWithAuth('/parent/kids/summary', token);
    if (!r.ok) {
      setKidSummaries([]);
      return;
    }
    const payload = await r.json();
    setKidSummaries(Array.isArray(payload?.kids) ? payload.kids : []);
  }

  const handleAddKid = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(''); setSuccess('');
    const res = await fetchWithAuth('/parent/kids', token, {
      method: 'POST',
      body: JSON.stringify(form),
    });
    if (!res.ok) {
      setError(await res.text() || 'Failed to add child');
      return;
    }
    setSuccess('Child account created!');
    setForm({ username: '', firstName: '', password: '' });
    loadKids();
    loadKidSummaries();
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setResetMsg('');
    const res = await fetchWithAuth('/parent/reset-child-password', token, {
      method: 'POST',
      body: JSON.stringify({ childUsername: resetTarget, newPassword }),
    });
    if (!res.ok) {
      setResetMsg(await res.text() || 'Failed to reset password');
      return;
    }
    setResetMsg('Password reset successfully!');
    setNewPassword('');
    setTimeout(() => { setResetTarget(null); setResetMsg(''); }, 1500);
  };

  return (
    <div className="page parent-dashboard">
      <PageGuidance
        title="Your Dashboard"
        description="Here's a snapshot of each child's reading progress. You can see books read, rewards earned, and upcoming milestones at a glance."
        instructions="Click on any child's card to view detailed progress, or use the sections below to manage accounts and settings."
        tone="parent"
      />

      <section className="quick-actions-section">
        <h2>Your Reading</h2>
        <p className="muted">Use your own parent account reading list without affecting child activity.</p>
        <div className="quick-actions-row">
          <Button variant="secondary" onClick={() => navigate('/search')}>
            Add Book To My Reading List
          </Button>
          <Button variant="secondary" onClick={() => navigate('/reading-list')}>
            View My Reading List
          </Button>
        </div>
      </section>

      <section className="summary-cards-section">
        <h2>Child Summary Cards</h2>
        {kidSummaries.length === 0 ? (
          <p className="muted">No child summaries yet. Add a kid to get started.</p>
        ) : (
          <div className="summary-card-grid">
            {kidSummaries.map(kid => (
              <article className="kid-summary-card" key={kid.id}>
                <div className="kid-summary-head">
                  <h3>{kid.firstName}</h3>
                  <span className="kid-summary-username">@{kid.username}</span>
                </div>
                <div className="kid-summary-metrics">
                  <span>Books: <strong>{kid.booksRead}</strong></span>
                  <span>Chapters: <strong>{kid.chaptersRead}</strong></span>
                  <span>Earned: <strong>${kid.totalEarned.toFixed(2)}</strong></span>
                  <span>Balance: <strong>${kid.currentBalance.toFixed(2)}</strong></span>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => navigate(`/parent/summary/${kid.id}`)}
                  aria-label={`Open summary for ${kid.firstName}`}
                >
                  Open Summary
                </Button>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="kids-section">
        <h2>Your Kids</h2>
        {kids.length === 0 ? (
          <p className="muted">No kids yet. Add one below.</p>
        ) : (
          <div className="table-scroll">
            <table className="kids-table">
              <thead>
                <tr>
                  <th>First Name</th>
                  <th>Username</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {kids.map(kid => (
                  <tr key={kid.id}>
                    <td>{kid.firstName}</td>
                    <td>{kid.username}</td>
                    <td>
                      <div className="table-actions">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => navigate(`/parent/summary/${kid.id}`)}
                          aria-label={`View details for ${kid.firstName}`}
                        >
                          View Details
                        </Button>
                        <Button variant="secondary" size="sm" onClick={() => { setResetTarget(kid.username); setResetMsg(''); setNewPassword(''); }}>
                          Reset Password
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="add-kid-section">
        <h2>Add a New Kid</h2>
        <form className="add-kid-form" onSubmit={handleAddKid}>
          <Input className="input" type="text" placeholder="Username" value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} required />
          <Input className="input" type="text" placeholder="First Name" value={form.firstName} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} required />
          <Input className="input" type="password" placeholder="Password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required />
          {error && <p className="error-msg">{error}</p>}
          {success && <p className="success-msg">{success}</p>}
          <Button type="submit">Add Kid</Button>
        </form>
      </section>

      {resetTarget && (
        <Modal
          open={Boolean(resetTarget)}
          onOpenChange={(open) => {
            if (!open) setResetTarget(null);
          }}
          title="Reset Password"
          description={`Enter a new password for ${resetTarget}`}
        >
          <form className="modal" onSubmit={handleResetPassword}>
            <h3>Reset Password for {resetTarget}</h3>
            <Input className="input" type="password" placeholder="New Password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required />
            {resetMsg && <p className={resetMsg.includes('success') ? 'success-msg' : 'error-msg'}>{resetMsg}</p>}
            <div className="modal-actions">
              <Button type="button" variant="secondary" onClick={() => setResetTarget(null)}>Cancel</Button>
              <Button type="submit">Reset Password</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
