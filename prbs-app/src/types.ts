export interface User {
  id: number;
  name: string;
  email: string;
  role: 'student' | 'supervisor' | 'admin';
  status: 'active' | 'inactive';
}

export interface Booking {
  id: number;
  studentId: number | string;
  name: string;
  supervisorId: number;
  supervisorName: string;
  group: number;
  project: string;
  date: string;
  time: string;
  status: 'confirmed' | 'completed' | 'no-show' | 'cancelled';
  meetUrl: string;
  reminderSent: boolean;
}

export interface Availability {
  id: number;
  supervisorId: number;
  supervisorName: string;
  date: string;
  start: string;
  end: string;
  duration: number;
  meetUrl: string;
}

export interface Settings {
  otpExpiry: number;
  cancelWindow: number;
  reminderTime: number;
}
