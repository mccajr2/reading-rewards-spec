import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getText } from '../../shared/api';

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const [status, setStatus] = useState<'pending' | 'success' | 'error'>('pending');
  const [message, setMessage] = useState('Verifying your email...');

  const token = params.get('token');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('Verification token missing. Please use the link from your email.');
      return;
    }
    getText(`/auth/verify-email?token=${encodeURIComponent(token)}`)
      .then((text) => {
        setStatus('success');
        setMessage(text || 'Your email has been verified. You can now log in.');
      })
      .catch((err) => {
        setStatus('error');
        setMessage(err instanceof Error ? err.message : 'Verification failed.');
      });
  }, [token]);

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <h1>Email Verification</h1>
        <p>{message}</p>
        <p className="auth-links">
          {status === 'success' ? 'Continue to ' : 'Return to '}
          <Link to="/login">login</Link>
        </p>
      </section>
    </main>
  );
}