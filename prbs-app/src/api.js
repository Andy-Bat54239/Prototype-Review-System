const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const ACCESS_TOKEN_KEY = 'prbs.accessToken';
const REFRESH_TOKEN_KEY = 'prbs.refreshToken';
const USER_KEY = 'prbs.user';

function readJsonSafe(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function errorMessage(status, body) {
  if (body?.message) return body.message;
  if (status === 401) return 'Your session expired. Please sign in again.';
  if (status === 403) return 'You do not have permission to do that.';
  return 'Something went wrong. Please try again.';
}

function saveSession(auth) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
  sessionStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(normalizeUser(auth.user)));
}

function normalizeUser(user) {
  return user ? { ...user, role: user.role.toLowerCase() } : null;
}

export function getStoredUser() {
  return readJsonSafe(sessionStorage.getItem(USER_KEY));
}

export function clearSession() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
  if (!refreshToken) throw new Error('No refresh token available.');

  const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  const text = await res.text();
  const body = readJsonSafe(text);
  if (!res.ok) {
    clearSession();
    throw new Error(errorMessage(res.status, body));
  }
  saveSession(body);
  return body.accessToken;
}

export async function request(path, options = {}, retry = true) {
  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const text = await res.text();
  const body = readJsonSafe(text);

  if (res.status === 401 && retry) {
    await refreshAccessToken();
    return request(path, options, false);
  }

  if (!res.ok) throw new Error(errorMessage(res.status, body));
  return body;
}

export const api = {
  async sendOtp(email) {
    await request('/auth/send-otp', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  async verifyOtp(email, code) {
    const auth = await request('/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ email, code }),
    });
    saveSession(auth);
    return normalizeUser(auth.user);
  },

  async logout() {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    clearSession();
    if (!refreshToken) return;
    try {
      await request('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      }, false);
    } catch {
      // Session is already gone locally; backend logout is best-effort.
    }
  },

  me: () => request('/me'),
  bookings: () => request('/bookings/me'),
  availability: () => request('/availability'),
  createBooking: (payload) => request('/bookings', { method: 'POST', body: JSON.stringify(payload) }),
  cancelBooking: (id) => request(`/bookings/${id}/cancel`, { method: 'PATCH' }),
  updateBookingStatus: (id, status) => request(`/bookings/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }),
  createAvailability: (payload) => request('/availability', { method: 'POST', body: JSON.stringify(payload) }),
  users: () => request('/users'),
  updateUserStatus: (id, status) => request(`/users/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }),
  settings: () => request('/settings'),
  updateSettings: (payload) => request('/settings', { method: 'PATCH', body: JSON.stringify(payload) }),
};

export function adaptBooking(b) {
  const status = b.status.toLowerCase().replace('_', '-');
  return {
    id: b.id,
    studentId: b.studentId,
    name: b.studentName || 'Student',
    supervisorId: b.supervisorId,
    supervisorName: b.supervisorName,
    group: b.groupNumber,
    project: b.project,
    date: b.date,
    time: String(b.time).slice(0, 5),
    status,
    meetUrl: b.meetUrl,
    reminderSent: b.reminderSent,
  };
}

export function adaptAvailability(a) {
  return {
    id: a.id,
    supervisorId: a.supervisorId,
    supervisorName: a.supervisorName,
    date: a.date,
    start: String(a.startTime).slice(0, 5),
    end: String(a.endTime).slice(0, 5),
    duration: a.durationMinutes,
    meetUrl: a.meetUrl,
  };
}

export function adaptUser(u) {
  const title = (value) => value.charAt(0) + value.slice(1).toLowerCase();
  return {
    ...u,
    role: title(u.role),
    status: title(u.status),
  };
}
