const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export type MessageApiStubResponse = {
  status: 'not_implemented';
  feature: 'reward-customization';
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

export async function getParentMessagesStub(): Promise<MessageApiStubResponse> {
  return request<MessageApiStubResponse>('/parent/rewards/messages');
}

export async function getChildMessagesStub(): Promise<MessageApiStubResponse> {
  return request<MessageApiStubResponse>('/child/rewards/messages');
}