import axios from 'axios';
import { User, Booking, Availability, Settings } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const ACCESS_TOKEN_KEY = 'prbs.accessToken';
const REFRESH_TOKEN_KEY = 'prbs.refreshToken';
const USER_KEY = 'prbs.user';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosInstance.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

function getErrorMessage(error: any): string {
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  const status = error.response?.status;
  if (status === 401) {
    return 'Your session expired. Please sign in again.';
  }
  if (status === 403) {
    return 'You do not have permission to do that.';
  }
  return error.message || 'Something went wrong. Please try again.';
}

export function getStoredUser(): User | null {
  const text = sessionStorage.getItem(USER_KEY);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

function saveSession(auth: { accessToken: string; refreshToken: string; user: any }): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
  sessionStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
  sessionStorage.setItem(USER_KEY, JSON.stringify(normalizeUser(auth.user)));
}

function normalizeUser(user: any): User | null {
  return user ? { ...user, role: user.role.toLowerCase() } : null;
}

let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (originalRequest.url?.includes('/auth/refresh')) {
        clearSession();
        return Promise.reject(new Error('Session expired.'));
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return axiosInstance(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
      if (!refreshToken) {
        clearSession();
        return Promise.reject(new Error('No refresh token available.'));
      }

      try {
        const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
        saveSession(res.data);
        const newAccessToken = res.data.accessToken;
        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return axiosInstance(originalRequest);
      } catch (refreshError: any) {
        processQueue(refreshError, null);
        clearSession();
        return Promise.reject(new Error(getErrorMessage(refreshError)));
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(new Error(getErrorMessage(error)));
  }
);

export const api = {
  async sendOtp(email: string): Promise<void> {
    await axiosInstance.post('/auth/send-otp', { email });
  },

  async verifyOtp(email: string, code: string): Promise<User | null> {
    const res = await axiosInstance.post('/auth/verify-otp', { email, code });
    saveSession(res.data);
    return normalizeUser(res.data.user);
  },

  async logout(): Promise<void> {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    clearSession();
    if (!refreshToken) return;
    try {
      await axiosInstance.post('/auth/logout', { refreshToken });
    } catch {
      // Session is already gone locally; backend logout is best-effort.
    }
  },

  async me(): Promise<User> {
    const res = await axiosInstance.get('/me');
    return res.data;
  },

  async bookings(): Promise<any[]> {
    const res = await axiosInstance.get('/bookings/me');
    return res.data;
  },

  async availability(): Promise<any[]> {
    const res = await axiosInstance.get('/availability');
    return res.data;
  },

  async createBooking(payload: {
    availabilityId: number;
    slotTime: string;
    project: string;
    groupNumber: number;
  }): Promise<any> {
    const res = await axiosInstance.post('/bookings', payload);
    return res.data;
  },

  async cancelBooking(id: number): Promise<any> {
    const res = await axiosInstance.patch(`/bookings/${id}/cancel`);
    return res.data;
  },

  async updateBookingStatus(id: number, status: string): Promise<any> {
    const res = await axiosInstance.patch(`/bookings/${id}/status`, { status });
    return res.data;
  },

  async createAvailability(payload: {
    date: string;
    startTime: string;
    endTime: string;
    durationMinutes: number;
    meetUrl: string;
  }): Promise<any> {
    const res = await axiosInstance.post('/availability', payload);
    return res.data;
  },

  async users(): Promise<any[]> {
    const res = await axiosInstance.get('/users');
    return res.data;
  },

  async updateUserStatus(id: number, status: string): Promise<any> {
    const res = await axiosInstance.patch(`/users/${id}/status`, { status });
    return res.data;
  },

  async settings(): Promise<any> {
    const res = await axiosInstance.get('/settings');
    return res.data;
  },

  async updateSettings(payload: {
    otpExpiry: number;
    cancelWindow: number;
    reminderTime: number;
  }): Promise<any> {
    const res = await axiosInstance.patch('/settings', payload);
    return res.data;
  },
};

export function adaptBooking(b: any): Booking {
  const status = b.status.toLowerCase().replace('_', '-') as Booking['status'];
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

export function adaptAvailability(a: any): Availability {
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

export function adaptUser(u: any): User {
  const title = (value: string) => value.charAt(0) + value.slice(1).toLowerCase();
  return {
    id: u.id,
    name: u.name,
    email: u.email,
    role: u.role.toLowerCase() as User['role'],
    status: title(u.status).toLowerCase() as User['status'],
  };
}
