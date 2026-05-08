const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export type AuthUser = {
  id: string;
  role: 'PARENT' | 'CHILD';
  parentId?: string | null;
  email?: string | null;
  username?: string | null;
  firstName?: string | null;
  status?: string | null;
};

export type LoginResponse = {
  token: string;
  user: AuthUser;
};

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return JSON.parse(text) as T;
  }

  return text as T;
}

export async function getText(path: string): Promise<string> {
  const response = await fetch(`${API_URL}${path}`);
  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `Request failed with status ${response.status}`);
  }
  return text;
}

export async function fetchWithAuth(
  path: string,
  token: string | null,
  options: RequestInit = {}
): Promise<Response> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (response.status === 401) {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
  }
  return response;
}