import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchWithAuth } from '../../shared/api';
import './SearchPage.css';

type BookResult = {
  googleBookId: string;
  title: string;
  authors: string[];
  description: string;
  thumbnailUrl: string;
};

export function SearchPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [isbn, setIsbn] = useState('');
  const [results, setResults] = useState<BookResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [adding, setAdding] = useState<string | null>(null);
  const [error, setError] = useState('');

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setResults([]);
    if (!title && !author && !isbn) {
      setError('Enter a title, author, or ISBN to search.');
      return;
    }
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (title) params.set('title', title);
      if (author) params.set('author', author);
      if (isbn) params.set('isbn', isbn);
      const res = await fetchWithAuth(`/search?${params}`, token);
      if (!res.ok) throw new Error('Search failed');
      setResults(await res.json());
    } catch {
      setError('Search failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = async (book: BookResult) => {
    setAdding(book.googleBookId);
    try {
      const res = await fetchWithAuth('/books', token, {
        method: 'POST',
        body: JSON.stringify(book),
      });
      if (res.ok) {
        navigate('/reading-list', { state: { newGoogleBookId: book.googleBookId } });
      }
    } finally {
      setAdding(null);
    }
  };

  return (
    <div className="page search-page">
      <h1>Search Books</h1>
      <form className="search-form" onSubmit={handleSearch}>
        <div className="search-fields">
          <input
            type="text"
            placeholder="Title"
            value={title}
            onChange={e => setTitle(e.target.value)}
            className="input"
          />
          <input
            type="text"
            placeholder="Author"
            value={author}
            onChange={e => setAuthor(e.target.value)}
            className="input"
          />
          <input
            type="text"
            placeholder="ISBN"
            value={isbn}
            onChange={e => setIsbn(e.target.value)}
            className="input"
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>
      {error && <p className="error-msg">{error}</p>}
      {!loading && results.length === 0 && !error && (
        <p className="muted">Search results will appear here.</p>
      )}
      <div className="book-results">
        {results.map(book => (
          <div key={book.googleBookId} className="book-card">
            {book.thumbnailUrl && (
              <img src={book.thumbnailUrl} alt={book.title} className="book-thumb" />
            )}
            <div className="book-info">
              <h3>{book.title}</h3>
              <p className="book-authors">{book.authors?.join(', ')}</p>
              {book.description && (
                <p className="book-desc">{book.description.slice(0, 200)}{book.description.length > 200 ? '…' : ''}</p>
              )}
            </div>
            <button
              className="btn btn-secondary"
              onClick={() => handleAdd(book)}
              disabled={adding === book.googleBookId}
            >
              {adding === book.googleBookId ? 'Adding…' : '+ Add to Reading List'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
