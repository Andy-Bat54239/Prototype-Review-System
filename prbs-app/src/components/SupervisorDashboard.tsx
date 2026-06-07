import { useState, Dispatch, SetStateAction } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, LineChart, Line,
} from 'recharts';
import { generateSlots, fmt12, fmtDate, ANALYTICS_DATA } from '../data';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { adaptAvailability, adaptBooking, api } from '../api';
import { Booking, Availability } from '../types';

interface StatusBadgeProps {
  status: Booking['status'];
}

function StatusBadge({ status }: StatusBadgeProps) {
  const map: Record<string, [string, string, string]> = {
    confirmed: ['bg-brand-lightBlue', 'text-brand-blue', 'Confirmed'],
    completed: ['bg-[#E8F4E8]', 'text-[#2E7D32]', 'Completed'],
    'no-show': ['bg-[#FDE8E8]', 'text-[#C62828]', 'No Show'],
  };
  const [bgClass, textClass, label] = map[status] || ['bg-gray-100', 'text-gray-600', 'Unknown'];
  return (
    <span className={`${bgClass} ${textClass} text-[11.5px] font-bold px-2.5 py-1 rounded-full tracking-wider font-sans`}>
      {label}
    </span>
  );
}

interface StudentDetailPanelProps {
  booking: Booking | null;
  onClose: () => void;
  onMark: (id: number, status: string) => Promise<void>;
  isMobile: boolean;
}

function StudentDetailPanel({ booking, onClose, onMark, isMobile }: StudentDetailPanelProps) {
  if (!booking) return null;
  const initials = booking.name.split(' ').map(n => n[0]).join('').slice(0, 2);
  const statusColors: Record<string, [string, string]> = { 
    confirmed: ['bg-brand-lightBlue', 'text-brand-blue'], 
    completed: ['bg-[#E8F4E8]', 'text-[#2E7D32]'], 
    'no-show': ['bg-[#FDE8E8]', 'text-[#C62828]'] 
  };
  const [sbg, scol] = statusColors[booking.status] || ['bg-gray-100', 'text-gray-600'];

  return (
    <>
      <div onClick={onClose} className="fixed inset-0 bg-[#0D1F45]/35 z-50" />
      <div 
        className="fixed top-0 right-0 h-screen bg-white z-[51] shadow-2xl flex flex-col transition-transform duration-200"
        style={{ 
          width: isMobile ? '100vw' : '380px',
          animation: 'slideIn 0.22s ease-out forwards'
        }}
      >
        <style>{`@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }`}</style>
        <div className="bg-gradient-to-br from-brand-navy to-brand-blue p-[32px_28px_28px] relative shrink-0">
          <button 
            onClick={onClose} 
            className="absolute top-4 right-4 bg-white/15 border-none rounded-full w-8 h-8 cursor-pointer color-white text-white flex items-center justify-center hover:bg-white/20"
          >
            <svg width="14" height="14" fill="none" viewBox="0 0 24 24">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/>
            </svg>
          </button>
          <div className="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center font-serif text-22 font-bold text-white mb-3.5 border-2 border-white/35">
            {initials}
          </div>
          <h3 className="font-serif text-white text-2xl font-bold mb-1.5">{booking.name}</h3>
          <div className="flex items-center gap-2">
            <span className={`${sbg} ${scol} text-[11.5px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider font-sans`}>
              {booking.status.replace('-', ' ')}
            </span>
            <span className="text-white/65 text-sm font-sans">{fmt12(booking.time)}</span>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-[24px_28px]">
          <div className="grid grid-cols-2 gap-3 mb-6">
            {[
              ['Student ID', booking.studentId || 'S' + String(booking.id).padStart(3, '0')],
              ['Group', `Group ${booking.group}`],
              ['Date', new Date(booking.date + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })],
              ['Time Slot', fmt12(booking.time)],
            ].map(([label, val]) => (
              <div key={label} className="bg-[#F8F5F0] rounded-xl p-[12px_14px]">
                <div className="text-[11px] font-bold text-brand-lightGray uppercase tracking-wider mb-1 font-sans">{label}</div>
                <div className="text-sm font-semibold text-brand-charcoal font-sans">{val}</div>
              </div>
            ))}
          </div>

          <div className="mb-6">
            <div className="text-[11px] font-bold text-brand-lightGray uppercase tracking-wider mb-2 font-sans">Project</div>
            <div className="bg-brand-lightBlue rounded-xl p-[14px_16px] text-base font-semibold text-brand-blue font-sans">
              {booking.project}
            </div>
          </div>

          <div className="mb-7">
            <div className="text-[11px] font-bold text-brand-lightGray uppercase tracking-wider mb-2 font-sans">Google Meet Link</div>
            <a 
              href={booking.meetUrl} 
              target="_blank" 
              rel="noreferrer"
              className="flex items-center gap-2 bg-[#F8F5F0] rounded-xl p-[12px_14px] text-[13px] text-brand-blue font-semibold no-underline border border-brand-darkBeige hover:border-brand-blue/50 word-break font-sans"
            >
              <svg className="w-3.5 h-3.5 stroke-brand-blue shrink-0" fill="none" viewBox="0 0 24 24">
                <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" strokeWidth="2" strokeLinecap="round"/>
                <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" strokeWidth="2" strokeLinecap="round"/>
              </svg>
              {booking.meetUrl}
            </a>
          </div>

          {booking.status === 'confirmed' && (
            <div className="flex flex-col gap-2.5">
              <a 
                href={booking.meetUrl} 
                target="_blank" 
                rel="noreferrer"
                className="block text-center bg-brand-blue hover:bg-brand-blue/90 text-white p-3.5 rounded-xl font-bold text-base no-underline font-sans"
              >
                Join Google Meet Session
              </a>
              <div className="grid grid-cols-2 gap-2.5">
                <button 
                  onClick={() => { onMark(booking.id, 'completed'); onClose(); }}
                  className="p-3 bg-[#E8F4E8] text-[#2E7D32] hover:bg-[#dceddc] border-none rounded-xl font-bold text-sm cursor-pointer font-sans"
                >
                  ✓ Mark Complete
                </button>
                <button 
                  onClick={() => { onMark(booking.id, 'no-show'); onClose(); }}
                  className="p-3 bg-[#FDE8E8] text-[#C62828] hover:bg-[#fad4d4] border-none rounded-xl font-bold text-sm cursor-pointer font-sans"
                >
                  ✗ No Show
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

interface SessionCardProps {
  booking: Booking;
  onMark: (id: number, status: string) => Promise<void>;
  onClick: () => void;
  isMobile: boolean;
}

function SessionCard({ booking, onMark, onClick, isMobile }: SessionCardProps) {
  const initials = booking.name.split(' ').map(n => n[0]).join('').slice(0, 2);

  return (
    <div 
      onClick={onClick}
      className={`bg-white rounded-[14px] p-[18px_20px] border border-brand-darkBeige flex gap-4 shadow-sm hover:shadow-lg hover:border-[#C5D5EE] transition-all duration-150 cursor-pointer ${
        isMobile ? 'flex-col items-start' : 'flex-row items-center'
      }`}
    >
      <div className="w-11 h-11 rounded-full bg-brand-lightBlue flex items-center justify-center shrink-0 font-serif text-[16px] font-bold text-brand-blue">
        {initials}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1 flex-wrap">
          <span className="font-bold text-[14.5px] text-brand-charcoal">{booking.name}</span>
          <StatusBadge status={booking.status} />
        </div>
        <div className="text-brand-gray text-[13px] flex gap-4 flex-wrap">
          <span>Group {booking.group}</span>
          <span className="overflow-hidden text-ellipsis whitespace-nowrap max-w-[220px]">{booking.project}</span>
        </div>
      </div>
      <div className={`shrink-0 ${isMobile ? 'w-full pl-[56px] text-left' : 'w-auto text-right'}`}>
        <div className="font-bold text-sm text-brand-charcoal mb-2">{fmt12(booking.time)}</div>
        {booking.status === 'confirmed' && (
          <div className="flex gap-1.5 flex-wrap">
            <a 
              href={booking.meetUrl} 
              target="_blank" 
              rel="noreferrer" 
              onClick={e => e.stopPropagation()}
              className="bg-brand-blue hover:bg-brand-blue/90 text-white text-xs font-semibold p-[5px_12px] rounded-lg no-underline font-sans"
            >
              Join Meet
            </a>
            <button 
              onClick={e => { e.stopPropagation(); onMark(booking.id, 'completed'); }}
              className="bg-[#E8F4E8] text-[#2E7D32] hover:bg-[#dceddc] border-none text-xs font-semibold p-[5px_10px] rounded-lg cursor-pointer font-sans"
            >
              ✓ Done
            </button>
            <button 
              onClick={e => { e.stopPropagation(); onMark(booking.id, 'no-show'); }}
              className="bg-[#FDE8E8] text-[#C62828] hover:bg-[#fad4d4] border-none text-xs font-semibold p-[5px_10px] rounded-lg cursor-pointer font-sans"
            >
              ✗ No Show
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

interface AnalyticsTabProps {
  bookings: Booking[];
  isMobile: boolean;
}

function AnalyticsTab({ bookings, isMobile }: AnalyticsTabProps) {
  const total = bookings.length;
  const noShows = bookings.filter(b => b.status === 'no-show').length;
  const completed = bookings.filter(b => b.status === 'completed').length;
  const confirmed = bookings.filter(b => b.status === 'confirmed').length;
  const rate = total > 0 ? Math.round(((total - noShows) / total) * 100) : 0;

  const pieData = [
    { name: 'Confirmed', value: confirmed, color: '#1D5BAF' },
    { name: 'Completed', value: completed, color: '#2E7D32' },
    { name: 'No Show',   value: noShows,   color: '#C62828' },
  ].filter(d => d.value > 0);

  const CustomTooltipBar = ({ active, payload, label }: any) => {
    if (!active || !payload?.length) return null;
    return (
      <div className="bg-white border border-brand-darkBeige rounded-lg p-[10px_14px] shadow-lg">
        <div className="text-xs font-bold text-brand-gray mb-1">{label}</div>
        <div className="text-base font-extrabold text-brand-blue">{payload[0].value} sessions</div>
      </div>
    );
  };

  const CustomTooltipPie = ({ active, payload }: any) => {
    if (!active || !payload?.length) return null;
    const data = payload[0].payload;
    return (
      <div className="bg-white border border-brand-darkBeige rounded-lg p-[10px_14px] shadow-lg">
        <div className="text-sm font-bold" style={{ color: data.color }}>{payload[0].name}</div>
        <div className="text-[15px] font-extrabold text-brand-charcoal">{payload[0].value}</div>
      </div>
    );
  };

  return (
    <div className="font-sans">
      <h3 className="font-serif text-[26px] text-brand-charcoal mb-5">Analytics Overview</h3>
      <div className={`grid gap-4 mb-7 ${isMobile ? 'grid-cols-2' : 'grid-cols-4'}`}>
        {[
          { label: 'Total Sessions', value: total,     sub: 'all time',   color: '#1D5BAF' },
          { label: 'Completed',      value: completed,  sub: 'reviewed',   color: '#2E7D32' },
          { label: 'No Shows',       value: noShows,    sub: 'missed',     color: '#C62828' },
          { label: 'Booking Rate',   value: `${rate}%`, sub: 'attendance', color: '#1C1814' },
        ].map(({ label, value, sub, color }) => (
          <div key={label} className="bg-white rounded-[14px] p-5 md:p-[20px_22px] border border-brand-darkBeige shadow-sm">
            <div className="w-2.5 h-2.5 rounded-full mb-3.5" style={{ background: color }} />
            <div className="text-2xl md:text-3xl font-extrabold text-brand-charcoal font-serif mb-1">{value}</div>
            <div className="text-[13px] font-semibold text-brand-charcoal mb-0.5">{label}</div>
            <div className="text-xs text-brand-lightGray">{sub}</div>
          </div>
        ))}
      </div>

      <div className={`grid gap-5 ${isMobile ? 'grid-cols-1' : 'grid-cols-[1fr_280px]'}`}>
        <div className="bg-white rounded-[14px] p-[24px_24px_16px] border border-brand-darkBeige shadow-sm min-w-0">
          <h4 className="font-serif text-[17px] text-brand-charcoal mb-1">Sessions Per Day</h4>
          <p className="text-brand-lightGray text-[12.5px] mb-5">Number of review sessions scheduled daily</p>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={ANALYTICS_DATA} barCategoryGap="30%" margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F0EDE8" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip content={<CustomTooltipBar />} cursor={{ fill: '#F0F4FB' }} />
              <Bar dataKey="sessions" fill="#1D5BAF" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-[14px] p-[24px_24px_16px] border border-brand-darkBeige shadow-sm w-full">
          <h4 className="font-serif text-[17px] text-brand-charcoal mb-1">Session Status</h4>
          <p className="text-brand-lightGray text-[12.5px] mb-2">Breakdown by outcome</p>
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={52} outerRadius={80} paddingAngle={3} dataKey="value">
                {pieData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
              </Pie>
              <Tooltip content={<CustomTooltipPie />} />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex flex-col gap-1.5 mt-1">
            {pieData.map(d => (
              <div key={d.name} className="flex items-center justify-between text-[12.5px]">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full inline-block" style={{ background: d.color }} />
                  <span className="text-brand-gray">{d.name}</span>
                </span>
                <span className="font-bold text-brand-charcoal">{d.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-[14px] p-[24px_24px_16px] border border-brand-darkBeige shadow-sm mt-5">
        <h4 className="font-serif text-[17px] text-brand-charcoal mb-1">Booking Trend</h4>
        <p className="text-brand-lightGray text-[12.5px] mb-5">Cumulative session volume over time</p>
        <ResponsiveContainer width="100%" height={180}>
          <LineChart data={ANALYTICS_DATA.map((d, i) => ({ ...d, cumulative: ANALYTICS_DATA.slice(0, i + 1).reduce((s, x) => s + x.sessions, 0) }))} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#F0EDE8" vertical={false} />
            <XAxis dataKey="day" tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} allowDecimals={false} />
            <Tooltip content={({ active, payload, label }) => active && payload?.length ? (
              <div className="bg-white border border-brand-darkBeige rounded-lg p-[10px_14px] shadow-lg">
                <div className="text-xs font-bold text-brand-gray mb-1">{label}</div>
                <div className="text-sm font-extrabold text-brand-blue">{payload[0].value} total</div>
              </div>
            ) : null} />
            <Line type="monotone" dataKey="cumulative" stroke="#1D5BAF" strokeWidth={2.5} dot={{ fill: '#1D5BAF', r: 4 }} activeDot={{ r: 6 }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

interface AvailabilityTabProps {
  availability: Availability[];
  setAvailability: Dispatch<SetStateAction<Availability[]>>;
  isMobile: boolean;
}

function AvailabilityTab({ availability, setAvailability, isMobile }: AvailabilityTabProps) {
  const [form, setForm] = useState({ date: '', start: '09:00', end: '11:00', duration: '15', meetUrl: '' });
  const setF = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }));
  const [added, setAdded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');

  const preview = form.date && form.start && form.end && form.duration
    ? generateSlots({ start: form.start, end: form.end, duration: parseInt(form.duration) })
    : [];

  const handleAdd = async () => {
    if (!form.date || !form.meetUrl) return;
    setSaving(true);
    setErr('');
    try {
      const saved = await api.createAvailability({
        date: form.date,
        startTime: form.start,
        endTime: form.end,
        durationMinutes: parseInt(form.duration),
        meetUrl: form.meetUrl,
      });
      const adapted = adaptAvailability(saved);
      setAvailability(prev => [...prev.filter(a => a.id !== adapted.id), adapted].sort((a, b) => a.date.localeCompare(b.date)));
      setAdded(true);
      setTimeout(() => setAdded(false), 2500);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={`grid gap-6 items-start ${isMobile ? 'grid-cols-1' : 'grid-cols-2'}`}>
      <div className="bg-white rounded-[14px] p-7 border border-brand-darkBeige shadow-sm">
        <h3 className="font-serif text-xl text-brand-charcoal mb-5">Add Availability</h3>
        {[
          ['Date', 'date', 'date'], 
          ['Start Time', 'start', 'time'], 
          ['End Time', 'end', 'time'], 
          ['Session Duration (min)', 'duration', 'number'], 
          ['Google Meet Link', 'meetUrl', 'text']
        ].map(([label, key, type]) => (
          <div key={key} className="mb-4">
            <label className="block text-xs font-bold text-brand-charcoal mb-1.5 uppercase tracking-wider font-sans">{label}</label>
            <input 
              type={type} 
              value={form[key as keyof typeof form]} 
              onChange={e => setF(key, e.target.value)}
              placeholder={key === 'meetUrl' ? 'https://meet.google.com/...' : key === 'duration' ? '15' : ''}
              className="w-full p-[10px_14px] border border-brand-darkBeige focus:border-brand-blue rounded-lg text-sm font-sans text-brand-charcoal outline-none bg-[#FAFAF8] transition-colors"
            />
          </div>
        ))}
        {err && <p className="text-[#D04040] text-xs mb-3">{err}</p>}
        <button 
          onClick={handleAdd} 
          disabled={saving} 
          className={`w-full p-3 border-none rounded-xl text-[14.5px] font-semibold font-sans mt-2 transition-colors text-white ${
            saving ? 'bg-[#7AAAD8]' : 'bg-brand-blue hover:bg-brand-blue/90 cursor-pointer'
          }`}
        >
          {saving ? 'Saving...' : added ? '✓ Availability Added!' : 'Generate Slots →'}
        </button>
      </div>

      <div className="flex flex-col gap-5">
        <div className="bg-white rounded-[14px] p-6 border border-brand-darkBeige shadow-sm">
          <h4 className="font-serif text-[17px] text-brand-charcoal mb-3.5">Slot Preview</h4>
          {preview.length === 0 ? (
            <p className="text-brand-lightGray text-sm">Fill in the form to preview generated slots.</p>
          ) : (
            <div>
              <p className="text-brand-gray text-xs mb-3">{preview.length} slots will be generated</p>
              <div className="flex flex-wrap gap-2">
                {preview.map(s => (
                  <span key={s} className="bg-brand-lightBlue text-brand-blue text-[12.5px] font-semibold p-[5px_12px] rounded-lg">
                    {fmt12(s)}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="bg-white rounded-[14px] p-6 border border-brand-darkBeige shadow-sm">
          <h4 className="font-serif text-[17px] text-brand-charcoal mb-3.5">Current Availability</h4>
          {availability.map(a => (
            <div key={a.date} className="flex justify-between items-center py-2.5 border-b border-[#F8F5F0]">
              <div>
                <div className="text-[13.5px] font-semibold text-brand-charcoal">
                  {new Date(a.date + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}
                </div>
                <div className="text-xs text-brand-lightGray">{a.start}–{a.end} · {a.duration}min slots</div>
              </div>
              <span className="bg-brand-lightBlue text-brand-blue text-[11px] font-bold p-[3px_10px] rounded-full uppercase">
                {generateSlots(a).length} slots
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

interface SupervisorDashboardProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  bookings: Booking[];
  setBookings: Dispatch<SetStateAction<Booking[]>>;
  availability: Availability[];
  setAvailability: Dispatch<SetStateAction<Availability[]>>;
  dataLoading: boolean;
  dataError: string;
}

export default function SupervisorDashboard({ activeTab, setActiveTab, bookings, setBookings, availability, setAvailability, dataLoading, dataError }: SupervisorDashboardProps) {
  const [selectedDate, setSelectedDate] = useState('');
  const [notif, setNotif] = useState(true);
  const [selectedStudent, setSelectedStudent] = useState<Booking | null>(null);
  const { isMobile } = useBreakpoint();

  const onMark = async (id: number, status: string) => {
    const saved = await api.updateBookingStatus(id, status.toUpperCase().replace('-', '_'));
    const adapted = adaptBooking(saved);
    setBookings(prev => prev.map(b => b.id === id ? adapted : b));
    setSelectedStudent(current => current?.id === id ? adapted : current);
  };

  const bookingsByDate: Record<string, Booking[]> = {};
  bookings.forEach(b => { 
    if (!bookingsByDate[b.date]) bookingsByDate[b.date] = []; 
    bookingsByDate[b.date].push(b); 
  });
  const upcomingDates = [...new Set(bookings.map(b => b.date))].sort();
  const currentDate = selectedDate || upcomingDates[0] || '';

  if (dataLoading) return <div className="text-brand-gray text-[15px] font-sans">Loading supervisor workspace...</div>;
  if (dataError) return <div className="bg-white rounded-xl p-6 border border-brand-darkBeige text-[#D04040] font-sans">{dataError}</div>;

  if (activeTab === 'analytics') return <AnalyticsTab bookings={bookings} isMobile={isMobile} />;

  if (activeTab === 'availability') return (
    <div>
      <h2 className="font-serif text-[26px] text-brand-charcoal mb-6">Manage Availability</h2>
      <AvailabilityTab availability={availability} setAvailability={setAvailability} isMobile={isMobile} />
    </div>
  );

  if (activeTab === 'calendar') {
    return (
      <div>
        <h2 className="font-serif text-[26px] text-brand-charcoal mb-6">Booking Calendar</h2>
        <div className="flex gap-3 flex-wrap mb-6">
          {upcomingDates.map(date => {
            const count = (bookingsByDate[date] || []).length;
            const sel = currentDate === date;
            return (
              <button 
                key={date} 
                onClick={() => { setSelectedDate(date); setActiveTab('sessions'); }}
                className={`p-[14px_20px] rounded-xl border transition-all duration-150 cursor-pointer font-sans text-left ${
                  sel 
                    ? 'border-brand-blue bg-brand-blue text-white' 
                    : 'border-brand-darkBeige bg-white text-brand-charcoal hover:border-brand-blue'
                }`}
              >
                <div className={`text-[13.5px] font-bold mb-1 ${sel ? 'text-white' : 'text-brand-charcoal'}`}>
                  {new Date(date + 'T00:00:00').toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
                </div>
                <div className={`text-xs font-semibold ${sel ? 'text-white/75' : 'text-brand-blue'}`}>
                  {count} booked
                </div>
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  const dayBookings = (bookingsByDate[currentDate] || []).sort((a, b) => a.time.localeCompare(b.time));
  return (
    <div className="font-sans">
      {notif && (
        <div className="bg-white border border-brand-darkBeige border-l-4 border-l-brand-blue rounded-xl p-[14px_18px] mb-6 flex items-center gap-3.5 shadow-sm">
          <div className="shrink-0">
            <svg className="w-5.5 h-5.5 stroke-brand-blue" fill="none" viewBox="0 0 24 24">
              <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" strokeWidth="1.8" strokeLinecap="round"/>
            </svg>
          </div>
          <div className="flex-1 min-w-0">
            <div className="font-bold text-brand-charcoal text-sm">Session in 5 minutes: Alice Uwase – Group 3</div>
            <div className="text-brand-gray text-[13px] mt-0.5">AI Crop Disease Monitor · Apr 25 at 9:00 AM</div>
          </div>
          <button 
            onClick={() => setNotif(false)} 
            className="bg-none border-none cursor-pointer text-brand-lightGray text-lg p-1 shrink-0 hover:text-brand-charcoal"
          >
            ✕
          </button>
        </div>
      )}

      <div className={`flex justify-between gap-4 mb-6 ${isMobile ? 'flex-col items-start' : 'flex-row items-end'}`}>
        <div>
          <h2 className="font-serif text-[26px] text-brand-charcoal mb-1">Today's Sessions</h2>
          <p className="text-brand-gray text-[14.5px]">{currentDate ? fmtDate(currentDate) : 'No sessions scheduled yet'}</p>
        </div>
        <div className="flex gap-2 flex-wrap">
          {upcomingDates.map(d => (
            <button 
              key={d} 
              onClick={() => setSelectedDate(d)}
              className={`p-[7px_14px] rounded-lg border font-sans text-xs font-semibold cursor-pointer transition-all duration-150 ${
                currentDate === d 
                  ? 'border-brand-blue bg-brand-blue text-white' 
                  : 'border-brand-darkBeige bg-white text-brand-gray hover:border-brand-blue'
              }`}
            >
              {new Date(d + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-2.5">
        {dayBookings.length === 0 ? (
          <div className="bg-white rounded-[14px] p-12 text-center border border-brand-darkBeige">
            <p className="text-brand-lightGray text-[15px]">No sessions scheduled for this date.</p>
          </div>
        ) : (
          dayBookings.map(b => (
            <SessionCard 
              key={b.id} 
              booking={b} 
              onMark={onMark} 
              onClick={() => setSelectedStudent(b)} 
              isMobile={isMobile} 
            />
          ))
        )}
      </div>

      <StudentDetailPanel 
        booking={selectedStudent} 
        onClose={() => setSelectedStudent(null)} 
        onMark={onMark} 
        isMobile={isMobile} 
      />
    </div>
  );
}
