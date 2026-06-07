export const TODAY = new Date('2026-04-23');

export const MOCK_USERS = [
  { id: 1, name: 'Alice Uwase',       email: 'alice@university.ac.rw',      role: 'Student',    status: 'Active'   },
  { id: 2, name: 'James Nkosi',       email: 'james@university.ac.rw',      role: 'Student',    status: 'Active'   },
  { id: 3, name: 'Fatima Diallo',     email: 'fatima@university.ac.rw',     role: 'Student',    status: 'Active'   },
  { id: 4, name: 'Kwame Asante',      email: 'kwame@university.ac.rw',      role: 'Student',    status: 'Active'   },
  { id: 5, name: 'Priya Sharma',      email: 'priya@university.ac.rw',      role: 'Student',    status: 'Active'   },
  { id: 6, name: 'Chidi Okafor',      email: 'chidi@university.ac.rw',      role: 'Student',    status: 'Inactive' },
  { id: 7, name: 'Dr. Sarah Mensah',  email: 'supervisor@university.ac.rw', role: 'Supervisor', status: 'Active'   },
  { id: 8, name: 'Admin User',        email: 'admin@university.ac.rw',      role: 'Admin',      status: 'Active'   },
];

export const MOCK_BOOKINGS_INIT = [
  { id: 1, studentId: 'S001', name: 'Alice Uwase',   group: 3, project: 'AI Crop Disease Monitor',    date: '2026-04-25', time: '09:00', status: 'confirmed', meetUrl: 'https://meet.google.com/abc-defg-hij' },
  { id: 2, studentId: 'S002', name: 'James Nkosi',   group: 1, project: 'Smart Water Quality System', date: '2026-04-25', time: '09:15', status: 'confirmed', meetUrl: 'https://meet.google.com/abc-defg-hij' },
  { id: 3, studentId: 'S003', name: 'Fatima Diallo', group: 2, project: 'Telemedicine Platform',      date: '2026-04-25', time: '09:30', status: 'no-show',   meetUrl: 'https://meet.google.com/abc-defg-hij' },
  { id: 4, studentId: 'S004', name: 'Kwame Asante',  group: 4, project: 'Solar Grid Optimizer',       date: '2026-04-28', time: '09:00', status: 'confirmed', meetUrl: 'https://meet.google.com/xyz-uvwx-yz1' },
  { id: 5, studentId: 'S005', name: 'Priya Sharma',  group: 5, project: 'E-Learning Accessibility',  date: '2026-04-28', time: '09:15', status: 'confirmed', meetUrl: 'https://meet.google.com/xyz-uvwx-yz1' },
  { id: 6, studentId: 'S006', name: 'Chidi Okafor',  group: 6, project: 'Blockchain Land Registry',  date: '2026-04-29', time: '10:00', status: 'completed', meetUrl: 'https://meet.google.com/pqr-stuv-wx2' },
  { id: 7, studentId: 'S007', name: 'Amara Kone',    group: 7, project: 'Drone Delivery Network',     date: '2026-04-30', time: '09:45', status: 'confirmed', meetUrl: 'https://meet.google.com/pqr-stuv-wx2' },
  { id: 8, studentId: 'S008', name: 'Zara Abdi',     group: 8, project: 'NLP Swahili Chatbot',        date: '2026-05-05', time: '10:00', status: 'confirmed', meetUrl: 'https://meet.google.com/lmn-opqr-st3' },
];

export const MOCK_AVAILABILITY = [
  { date: '2026-04-25', start: '09:00', end: '11:00', duration: 15, meetUrl: 'https://meet.google.com/abc-defg-hij' },
  { date: '2026-04-28', start: '09:00', end: '11:00', duration: 15, meetUrl: 'https://meet.google.com/xyz-uvwx-yz1' },
  { date: '2026-04-29', start: '09:00', end: '12:00', duration: 15, meetUrl: 'https://meet.google.com/pqr-stuv-wx2' },
  { date: '2026-04-30', start: '09:00', end: '11:30', duration: 15, meetUrl: 'https://meet.google.com/pqr-stuv-wx2' },
  { date: '2026-05-05', start: '09:00', end: '12:00', duration: 15, meetUrl: 'https://meet.google.com/lmn-opqr-st3' },
  { date: '2026-05-07', start: '10:00', end: '12:00', duration: 15, meetUrl: 'https://meet.google.com/lmn-opqr-st3' },
];

export const MOCK_SETTINGS_INIT = { otpExpiry: 10, cancelWindow: 60, reminderTime: 30 };

export const ANALYTICS_DATA = [
  { day: 'Apr 21', sessions: 3 },
  { day: 'Apr 22', sessions: 5 },
  { day: 'Apr 23', sessions: 2 },
  { day: 'Apr 25', sessions: 4 },
  { day: 'Apr 28', sessions: 4 },
  { day: 'Apr 29', sessions: 6 },
  { day: 'Apr 30', sessions: 3 },
  { day: 'May 5',  sessions: 5 },
];

export function generateSlots(avail: { start: string; end: string; duration: number }): string[] {
  const slots: string[] = [];
  const [sh, sm] = avail.start.split(':').map(Number);
  const [eh, em] = avail.end.split(':').map(Number);
  let cur = sh * 60 + sm;
  const end = eh * 60 + em;
  while (cur + avail.duration <= end) {
    const hh = String(Math.floor(cur / 60)).padStart(2, '0');
    const mm = String(cur % 60).padStart(2, '0');
    slots.push(`${hh}:${mm}`);
    cur += avail.duration;
  }
  return slots;
}

export function fmt12(t: string): string {
  const [h, m] = t.split(':').map(Number);
  const ampm = h >= 12 ? 'PM' : 'AM';
  const h12 = h % 12 || 12;
  return `${h12}:${String(m).padStart(2, '0')} ${ampm}`;
}

export function fmtDate(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}
