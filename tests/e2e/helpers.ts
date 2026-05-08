/**
 * Shared E2E test helpers.
 * Requires Docker stack running at http://localhost:3000.
 *
 * API contract (actual backend):
 *  - Signup: POST /api/auth/signup  { email, password, firstName, lastName }
 *            → always PARENT role; kid accounts via POST /api/parent/kids
 *  - Login:  POST /api/auth/login   { username, password }
 *            username = email (for parents) or username (for kids)
 *  - Dev verify: POST /api/auth/dev/verify?email=...  (disabled in prod profile)
 */

import { APIRequestContext } from '@playwright/test';

const API = 'http://localhost:3000/api';

/** Create a unique email for a test run to avoid collisions. */
export function uniqueEmail(prefix = 'testuser'): string {
  return `${prefix}+${Date.now()}@e2e.test`;
}

/** Create a unique short username (no @) for kid accounts. */
export function uniqueUsername(prefix = 'kid'): string {
  return `${prefix}${Date.now()}`;
}

/**
 * Sign up a PARENT account and force-verify it via the dev endpoint.
 * Signup requires firstName + lastName (not displayName).
 */
export async function signupAndVerify(
  request: APIRequestContext,
  email: string,
  password: string,
  firstName: string,
  lastName = 'TestUser'
) {
  const res = await request.post(`${API}/auth/signup`, {
    data: { email, password, firstName, lastName },
  });
  if (!res.ok()) throw new Error(`Signup failed: ${res.status()} ${await res.text()}`);

  // Force-verify without needing a real email (dev endpoint, disabled in prod)
  const verify = await request.post(`${API}/auth/dev/verify?email=${encodeURIComponent(email)}`);
  if (!verify.ok()) throw new Error(`Force-verify failed: ${verify.status()} ${await verify.text()}`);
}

/**
 * Login and return the JWT token.
 * The username field accepts both parent email and kid username.
 */
export async function login(
  request: APIRequestContext,
  username: string,
  password: string
): Promise<string> {
  const res = await request.post(`${API}/auth/login`, {
    data: { username, password },
  });
  if (!res.ok()) throw new Error(`Login failed: ${res.status()} ${await res.text()}`);
  const body = await res.json();
  return body.token as string;
}

/**
 * Create a child account under a logged-in parent.
 * Returns the kid's username (used for login).
 */
export async function createKid(
  request: APIRequestContext,
  parentToken: string,
  username: string,
  firstName: string,
  password: string
): Promise<string> {
  const res = await request.post(`${API}/parent/kids`, {
    data: { username, firstName, password },
    headers: { Authorization: `Bearer ${parentToken}` },
  });
  if (!res.ok()) throw new Error(`Create kid failed: ${res.status()} ${await res.text()}`);
  return username;
}
