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

export type ChapterDto = {
  id: string;
  name: string;
  chapterIndex: number;
  bookGoogleBookId?: string;
};

export type ChapterReadDto = {
  id: string;
  chapterId: string;
  completionDate: string;
};

export type HistoryItemDto = {
  id: string;
  chapterId: string;
  chapterName: string;
  bookTitle: string;
  completionDate: string;
};

export type RewardType = 'EARN' | 'PAYOUT' | 'SPEND';

export type RewardSummaryDto = {
  totalEarned: number;
  totalPaidOut: number;
  totalSpent: number;
  currentBalance: number;
  balancesByUnit?: RewardUnitBalanceDto[];
};

export type RewardUnitBalanceDto = {
  unitType: 'MONEY' | 'NON_MONEY';
  unitLabel: string;
  totalEarned: number;
  totalPaidOut: number;
  totalSpent: number;
  currentBalance: number;
};

export type RewardHistoryItemDto = {
  id: string;
  type: RewardType;
  amount: number;
  note?: string;
  createdAt: string;
  rewardOptionId?: string;
  rewardOptionName?: string;
  rewardOptionBasis?: 'PER_CHAPTER' | 'PER_BOOK' | 'PER_PAGE_MILESTONE';
  unitType?: 'MONEY' | 'NON_MONEY';
  unitLabel?: string;
  chapterReadId?: string;
  completionDate?: string;
  chapter?: ChapterDto;
  bookRead?: {
    id?: string;
    startDate?: string;
    endDate?: string | null;
    inProgress?: boolean;
    book?: {
      googleBookId?: string;
      title?: string;
      authors?: string[];
    };
  };
};

export type RewardsPageResponseDto = {
  rewards: RewardHistoryItemDto[];
  totalCount: number;
};

export type RewardScopeType = 'FAMILY' | 'CHILD';
export type RewardEarningBasis = 'PER_CHAPTER' | 'PER_BOOK' | 'PER_PAGE_MILESTONE';
export type RewardValueType = 'MONEY' | 'NON_MONEY';

export type RewardOptionDto = {
  id: string;
  ownerUserId: string;
  childUserId?: string | null;
  scopeType: RewardScopeType;
  name: string;
  description?: string | null;
  valueType: RewardValueType;
  currencyCode?: string | null;
  moneyAmount?: number | null;
  nonMoneyQuantity?: number | null;
  nonMoneyUnitLabel?: string | null;
  earningBasis: RewardEarningBasis;
  pageMilestoneSize?: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type RewardOptionsResponseDto = {
  options: RewardOptionDto[];
  activeSelectionId?: string | null;
  activeSelectionOptionId?: string | null;
};

// ── Settlement & Messaging Types ─────────────────────────────────────────────

export type SettlementRequestType = 'PAYOUT' | 'SPEND';
export type SettlementRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type MessageType = 'NUDGE' | 'ENCOURAGEMENT';

export type SettlementRequestDto = {
  id: string;
  childUserId: string;
  requestType: SettlementRequestType;
  requestedAmount: number;
  status: SettlementRequestStatus;
  note?: string | null;
  requestedAt: string;
  resolvedAt?: string | null;
  resolvedByParentId?: string | null;
  rewardOptionId?: string | null;
};

export type FamilyMessageDto = {
  id: string;
  senderRole: 'PARENT' | 'CHILD';
  senderUserId: string;
  recipientUserId: string;
  messageType: MessageType;
  body: string;
  linkedSettlementRequestId?: string | null;
  emailNotificationSent: boolean;
  createdAt: string;
};

// ── Settlement API helpers ────────────────────────────────────────────────────

export async function createSettlementRequest(
  token: string | null,
  childId: string,
  payload: { requestType: SettlementRequestType; requestedAmount: number; note?: string; rewardOptionId?: string }
): Promise<SettlementRequestDto> {
  const r = await fetchWithAuth(`/children/${childId}/settlement-requests`, token, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function listSettlementRequests(
  token: string | null,
  childId: string
): Promise<SettlementRequestDto[]> {
  const r = await fetchWithAuth(`/children/${childId}/settlement-requests`, token);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function approveSettlementRequest(
  token: string | null,
  requestId: string
): Promise<SettlementRequestDto> {
  const r = await fetchWithAuth(`/settlement-requests/${requestId}/approve`, token, { method: 'POST' });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function rejectSettlementRequest(
  token: string | null,
  requestId: string
): Promise<SettlementRequestDto> {
  const r = await fetchWithAuth(`/settlement-requests/${requestId}/reject`, token, { method: 'POST' });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function cancelSettlementRequest(
  token: string | null,
  requestId: string
): Promise<SettlementRequestDto> {
  const r = await fetchWithAuth(`/settlement-requests/${requestId}`, token, { method: 'DELETE' });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function listParentPendingRequests(
  token: string | null
): Promise<SettlementRequestDto[]> {
  const r = await fetchWithAuth('/parent/settlement-requests', token);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

// ── Messaging API helpers ─────────────────────────────────────────────────────

export async function sendNudge(
  token: string | null,
  body: string,
  linkedSettlementRequestId?: string
): Promise<FamilyMessageDto> {
  const r = await fetchWithAuth('/messages/nudge', token, {
    method: 'POST',
    body: JSON.stringify({ body, linkedSettlementRequestId }),
  });
  if (r.status === 429) throw new Error('NUDGE_COOLDOWN');
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function sendEncouragement(
  token: string | null,
  childId: string,
  body: string,
  linkedSettlementRequestId?: string
): Promise<FamilyMessageDto> {
  const r = await fetchWithAuth(`/messages/encouragement/${childId}`, token, {
    method: 'POST',
    body: JSON.stringify({ body, linkedSettlementRequestId }),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function getMessageInbox(token: string | null): Promise<FamilyMessageDto[]> {
  const r = await fetchWithAuth('/messages/inbox', token);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function getAllMessages(token: string | null): Promise<FamilyMessageDto[]> {
  const r = await fetchWithAuth('/messages', token);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

// ── Generic HTTP helpers ──────────────────────────────────────────────────────

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