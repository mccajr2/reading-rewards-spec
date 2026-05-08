import { useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

export function VerifyEmailPage() {
  const [params] = useSearchParams();

  const token = params.get('token');
  const summary = useMemo(() => {
    if (!token) {
      return 'Verification token missing. Please use the link from your email.';
    }
    return 'Verification token received. Backend verification flow will be wired in the next slice.';
  }, [token]);

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <h1>Email Verification</h1>
        <p>{summary}</p>
        <p className="auth-links">
          Return to <Link to="/login">login</Link>
        </p>
      </section>
    </main>
  );
}