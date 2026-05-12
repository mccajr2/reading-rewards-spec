const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export type RewardMessage = {
  messageId: string;
  senderId: string;
  recipientId: string;
  messageType: 'PAYOUT_REMINDER' | 'ENCOURAGEMENT' | 'PAYOUT_CONFIRMATION';
  messageText: string;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
};

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, init);
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  const text = await response.text();
  return text ? (JSON.parse(text) as T) : ({} as T);
}

export async function sendChildPayoutReminder(payload: {
  pendingAmount: number;
  note?: string;
  emailEnabled?: boolean;
}): Promise<{ messageId: string }> {
  return request<{ messageId: string }>('/child/rewards/messages/payout-reminder', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function listParentPayoutReminders(): Promise<{ reminders: RewardMessage[]; unreadCount: number }> {
  return request<{ reminders: RewardMessage[]; unreadCount: number }>('/parent/rewards/messages/payout-reminders');
}

export async function markParentPayoutReminderRead(messageId: string): Promise<RewardMessage> {
  return request<RewardMessage>(`/parent/rewards/messages/payout-reminders/${messageId}/read`, {
    method: 'POST',
  });
}

export async function sendParentEncouragement(payload: { childId: string; messageText: string }): Promise<RewardMessage> {
  return request<RewardMessage>('/parent/rewards/messages/encouragement', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}

export async function listChildInboxMessages(): Promise<{ messages: RewardMessage[]; unreadCount: number }> {
  return request<{ messages: RewardMessage[]; unreadCount: number }>('/child/rewards/messages/inbox');
}

export async function markChildInboxMessageRead(messageId: string): Promise<RewardMessage> {
  return request<RewardMessage>(`/child/rewards/messages/inbox/${messageId}/read`, {
    method: 'POST',
  });
}