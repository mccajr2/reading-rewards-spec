import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { postJson } from '../../shared/api';

export function SignupPage() {
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const response = await postJson<string>('/auth/signup', {
        email,
        firstName,
        lastName,
        password
      });
      setMessage(typeof response === 'string' ? response : 'Signup complete. Check your email.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Signup failed');
    }
  };

  return (
    <main className="auth-shell">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>Parent Signup</h1>
        <p>Create a parent account and verify by email before login.</p>

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>

        <label>
          First Name
          <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
        </label>

        <label>
          Last Name
          <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
        </label>

        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>

        {message ? <p className="success-text">{message}</p> : null}
        {error ? <p className="error-text">{error}</p> : null}

        <button className="primary-button" type="submit">
          Create account
        </button>

        <p className="auth-links">
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </form>
    </main>
  );
}