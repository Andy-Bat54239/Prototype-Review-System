import { useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, LineChart, Line,
} from 'recharts';
import { generateSlots, fmt12, fmtDate, ANALYTICS_DATA } from '../data';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { adaptAvailability, adaptBooking, api } from '../api';

function StatusBadge({ status }) {
  const map = {
    confirmed: ['#E5EDF8', '#1D5BAF', 'Confirmed'],
    completed: ['#E8F4E8', '#2E7D32', 'Completed'],
    'no-show': ['#FDE8E8', '#C62828', 'No Show'],
  };
  const [bg, col, label] = map[status] || ['#F5F5F5', '#666', 'Unknown'];
  return <span style={{ background: bg, color: col, fontSize: 11.5, fontWeight: 700, padding: '3px 10px', borderRadius: 20, letterSpacing: '0.04em' }}>{label}</span>;
}

function StudentDetailPanel({ booking, onClose, onMark, isMobile }) {
  if (!booking) return null;
  const initials = booking.name.split(' ').map(n => n[0]).join('').slice(0, 2);
  const statusColors = { confirmed: ['#E5EDF8', '#1D5BAF'], completed: ['#E8F4E8', '#2E7D32'], 'no-show': ['#FDE8E8', '#C62828'] };
  const [sbg, scol] = statusColors[booking.status] || ['#F5F5F5', '#666'];

  return (
    <>
      <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(13,31,69,0.35)', zIndex: 50 }} />
      <div style={{ position: 'fixed', top: 0, right: 0, height: '100vh', width: isMobile ? '100vw' : 380, background: 'white', zIndex: 51, boxShadow: '-8px 0 40px rgba(13,31,69,0.15)', display: 'flex', flexDirection: 'column', animation: 'slideIn 0.22s ease' }}>
        <style>{`@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }`}</style>
        <div style={{ background: 'linear-gradient(135deg, #0F2755, #1D5BAF)', padding: '32px 28px 28px', position: 'relative', flexShrink: 0 }}>
          <button onClick={onClose} style={{ position: 'absolute', top: 16, right: 16, background: 'rgba(255,255,255,0.15)', border: 'none', borderRadius: '50%', width: 32, height: 32, cursor: 'pointer', color: 'white', fontSize: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="14" height="14" fill="none" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12" stroke="white" strokeWidth="2.5" strokeLinecap="round"/></svg>
          </button>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'Playfair Display, serif', fontSize: 22, fontWeight: 700, color: 'white', marginBottom: 14, border: '2px solid rgba(255,255,255,0.35)' }}>{initials}</div>
          <h3 style={{ fontFamily: 'Playfair Display, serif', color: 'white', fontSize: 22, fontWeight: 700, margin: '0 0 6px' }}>{booking.name}</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ background: sbg, color: scol, fontSize: 11.5, fontWeight: 700, padding: '3px 10px', borderRadius: 20 }}>{booking.status.charAt(0).toUpperCase() + booking.status.slice(1).replace('-', ' ')}</span>
            <span style={{ color: 'rgba(255,255,255,0.65)', fontSize: 13 }}>{fmt12(booking.time)}</span>
          </div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '24px 28px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 24 }}>
            {[
              ['Student ID', booking.studentId || 'S' + String(booking.id).padStart(3, '0')],
              ['Group', `Group ${booking.group}`],
              ['Date', new Date(booking.date + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })],
              ['Time Slot', fmt12(booking.time)],
            ].map(([label, val]) => (
              <div key={label} style={{ background: '#F8F5F0', borderRadius: 10, padding: '12px 14px' }}>
                <div style={{ fontSize: 11, fontWeight: 700, color: '#B8AFA2', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>{label}</div>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#1C1814' }}>{val}</div>
              </div>
            ))}
          </div>

          <div style={{ marginBottom: 24 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: '#B8AFA2', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>Project</div>
            <div style={{ background: '#E5EDF8', borderRadius: 10, padding: '14px 16px', fontSize: 15, fontWeight: 600, color: '#1D5BAF' }}>{booking.project}</div>
          </div>

          <div style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: '#B8AFA2', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>Google Meet Link</div>
            <a href={booking.meetUrl} target="_blank" rel="noreferrer"
              style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#F8F5F0', borderRadius: 10, padding: '12px 14px', fontSize: 13, color: '#1D5BAF', fontWeight: 600, textDecoration: 'none', border: '1px solid #EDE9E2', wordBreak: 'break-all' }}>
              <svg width="14" height="14" fill="none" viewBox="0 0 24 24"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" stroke="#1D5BAF" strokeWidth="2" strokeLinecap="round"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" stroke="#1D5BAF" strokeWidth="2" strokeLinecap="round"/></svg>
              {booking.meetUrl}
            </a>
          </div>

          {booking.status === 'confirmed' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <a href={booking.meetUrl} target="_blank" rel="noreferrer"
                style={{ display: 'block', textAlign: 'center', background: '#1D5BAF', color: 'white', padding: '13px', borderRadius: 10, fontWeight: 700, fontSize: 15, textDecoration: 'none', fontFamily: 'DM Sans, sans-serif' }}>
                Join Google Meet Session
              </a>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <button onClick={() => { onMark(booking.id, 'completed'); onClose(); }}
                  style={{ padding: '11px', background: '#E8F4E8', color: '#2E7D32', border: 'none', borderRadius: 10, fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                  ✓ Mark Complete
                </button>
                <button onClick={() => { onMark(booking.id, 'no-show'); onClose(); }}
                  style={{ padding: '11px', background: '#FDE8E8', color: '#C62828', border: 'none', borderRadius: 10, fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
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

function SessionCard({ booking, onMark, onClick, isMobile }) {
  return (
    <div onClick={onClick}
      style={{ background: 'white', borderRadius: 12, padding: '18px 20px', border: '1px solid #EDE9E2', display: 'flex', alignItems: isMobile ? 'flex-start' : 'center', gap: 16, boxShadow: '0 1px 6px rgba(28,24,20,0.05)', transition: 'all 0.15s', cursor: 'pointer', flexWrap: isMobile ? 'wrap' : 'nowrap' }}
      onMouseEnter={e => { e.currentTarget.style.boxShadow = '0 4px 16px rgba(13,31,69,0.12)'; e.currentTarget.style.borderColor = '#C5D5EE'; }}
      onMouseLeave={e => { e.currentTarget.style.boxShadow = '0 1px 6px rgba(28,24,20,0.05)'; e.currentTarget.style.borderColor = '#EDE9E2'; }}>
      <div style={{ width: 44, height: 44, borderRadius: '50%', background: '#E5EDF8', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, fontFamily: 'Playfair Display, serif', fontSize: 16, fontWeight: 700, color: '#1D5BAF' }}>
        {booking.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 3, flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 700, fontSize: 14.5, color: '#1C1814' }}>{booking.name}</span>
          <StatusBadge status={booking.status} />
        </div>
        <div style={{ color: '#7A7069', fontSize: 13, display: 'flex', gap: 16, flexWrap: 'wrap' }}>
          <span>Group {booking.group}</span>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 220 }}>{booking.project}</span>
        </div>
      </div>
      <div style={{ textAlign: isMobile ? 'left' : 'right', flexShrink: 0, width: isMobile ? '100%' : 'auto', paddingLeft: isMobile ? 60 : 0 }}>
        <div style={{ fontWeight: 700, fontSize: 14, color: '#1C1814', marginBottom: 8 }}>{fmt12(booking.time)}</div>
        {booking.status === 'confirmed' && (
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            <a href={booking.meetUrl} target="_blank" rel="noreferrer" onClick={e => e.stopPropagation()}
              style={{ background: '#1D5BAF', color: 'white', fontSize: 12, fontWeight: 600, padding: '5px 12px', borderRadius: 7, textDecoration: 'none', fontFamily: 'DM Sans, sans-serif' }}>Join Meet</a>
            <button onClick={e => { e.stopPropagation(); onMark(booking.id, 'completed'); }}
              style={{ background: '#E8F4E8', color: '#2E7D32', border: 'none', fontSize: 12, fontWeight: 600, padding: '5px 10px', borderRadius: 7, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>✓ Done</button>
            <button onClick={e => { e.stopPropagation(); onMark(booking.id, 'no-show'); }}
              style={{ background: '#FDE8E8', color: '#C62828', border: 'none', fontSize: 12, fontWeight: 600, padding: '5px 10px', borderRadius: 7, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>✗ No Show</button>
          </div>
        )}
      </div>
    </div>
  );
}

function AnalyticsTab({ bookings, isMobile }) {
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

  const CustomTooltipBar = ({ active, payload, label }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{ background: 'white', border: '1px solid #EDE9E2', borderRadius: 8, padding: '10px 14px', boxShadow: '0 4px 16px rgba(13,31,69,0.12)' }}>
        <div style={{ fontSize: 12, fontWeight: 700, color: '#7A7069', marginBottom: 4 }}>{label}</div>
        <div style={{ fontSize: 16, fontWeight: 800, color: '#1D5BAF' }}>{payload[0].value} sessions</div>
      </div>
    );
  };

  const CustomTooltipPie = ({ active, payload }) => {
    if (!active || !payload?.length) return null;
    return (
      <div style={{ background: 'white', border: '1px solid #EDE9E2', borderRadius: 8, padding: '10px 14px', boxShadow: '0 4px 16px rgba(13,31,69,0.12)' }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: payload[0].payload.color }}>{payload[0].name}</div>
        <div style={{ fontSize: 15, fontWeight: 800, color: '#1C1814' }}>{payload[0].value}</div>
      </div>
    );
  };

  return (
    <div>
      <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 20px' }}>Analytics Overview</h3>
      <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr 1fr' : 'repeat(4, 1fr)', gap: 16, marginBottom: 28 }}>
        {[
          { label: 'Total Sessions', value: total,     sub: 'all time',   color: '#1D5BAF' },
          { label: 'Completed',      value: completed,  sub: 'reviewed',   color: '#2E7D32' },
          { label: 'No Shows',       value: noShows,    sub: 'missed',     color: '#C62828' },
          { label: 'Booking Rate',   value: `${rate}%`, sub: 'attendance', color: '#1C1814' },
        ].map(({ label, value, sub, color }) => (
          <div key={label} style={{ background: 'white', borderRadius: 14, padding: isMobile ? '16px' : '20px 22px', border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)' }}>
            <div style={{ width: 10, height: 10, borderRadius: '50%', background: color, marginBottom: 14 }} />
            <div style={{ fontSize: isMobile ? 24 : 30, fontWeight: 800, color: '#1C1814', fontFamily: 'Playfair Display, serif', marginBottom: 3 }}>{value}</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#1C1814', marginBottom: 2 }}>{label}</div>
            <div style={{ fontSize: 12, color: '#B8AFA2' }}>{sub}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr auto', gap: 20 }}>
        <div style={{ background: 'white', borderRadius: 14, padding: '24px 24px 16px', border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)', minWidth: 0 }}>
          <h4 style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, color: '#1C1814', margin: '0 0 4px' }}>Sessions Per Day</h4>
          <p style={{ color: '#B8AFA2', fontSize: 12.5, margin: '0 0 20px' }}>Number of review sessions scheduled daily</p>
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

        <div style={{ background: 'white', borderRadius: 14, padding: '24px 24px 16px', border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)', width: isMobile ? '100%' : 280 }}>
          <h4 style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, color: '#1C1814', margin: '0 0 4px' }}>Session Status</h4>
          <p style={{ color: '#B8AFA2', fontSize: 12.5, margin: '0 0 8px' }}>Breakdown by outcome</p>
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={52} outerRadius={80} paddingAngle={3} dataKey="value">
                {pieData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
              </Pie>
              <Tooltip content={<CustomTooltipPie />} />
            </PieChart>
          </ResponsiveContainer>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 4 }}>
            {pieData.map(d => (
              <div key={d.name} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 12.5 }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 8, height: 8, borderRadius: '50%', background: d.color, display: 'inline-block' }} />
                  <span style={{ color: '#7A7069' }}>{d.name}</span>
                </span>
                <span style={{ fontWeight: 700, color: '#1C1814' }}>{d.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{ background: 'white', borderRadius: 14, padding: '24px 24px 16px', border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)', marginTop: 20 }}>
        <h4 style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, color: '#1C1814', margin: '0 0 4px' }}>Booking Trend</h4>
        <p style={{ color: '#B8AFA2', fontSize: 12.5, margin: '0 0 20px' }}>Cumulative session volume over time</p>
        <ResponsiveContainer width="100%" height={180}>
          <LineChart data={ANALYTICS_DATA.map((d, i) => ({ ...d, cumulative: ANALYTICS_DATA.slice(0, i + 1).reduce((s, x) => s + x.sessions, 0) }))} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#F0EDE8" vertical={false} />
            <XAxis dataKey="day" tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11.5, fill: '#B8AFA2', fontFamily: 'DM Sans, sans-serif' }} axisLine={false} tickLine={false} allowDecimals={false} />
            <Tooltip content={({ active, payload, label }) => active && payload?.length ? (
              <div style={{ background: 'white', border: '1px solid #EDE9E2', borderRadius: 8, padding: '10px 14px', boxShadow: '0 4px 16px rgba(13,31,69,0.12)' }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: '#7A7069', marginBottom: 4 }}>{label}</div>
                <div style={{ fontSize: 15, fontWeight: 800, color: '#1D5BAF' }}>{payload[0].value} total</div>
              </div>
            ) : null} />
            <Line type="monotone" dataKey="cumulative" stroke="#1D5BAF" strokeWidth={2.5} dot={{ fill: '#1D5BAF', r: 4 }} activeDot={{ r: 6 }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

function AvailabilityTab({ availability, setAvailability, isMobile }) {
  const [form, setForm] = useState({ date: '', start: '09:00', end: '11:00', duration: '15', meetUrl: '' });
  const setF = (k, v) => setForm(f => ({ ...f, [k]: v }));
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
    } catch (e) {
      setErr(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: 24, alignItems: 'start' }}>
      <div style={{ background: 'white', borderRadius: 14, padding: 28, border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)' }}>
        <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 20, color: '#1C1814', margin: '0 0 20px' }}>Add Availability</h3>
        {[['Date', 'date', 'date'], ['Start Time', 'start', 'time'], ['End Time', 'end', 'time'], ['Session Duration (min)', 'duration', 'number'], ['Google Meet Link', 'meetUrl', 'text']].map(([label, key, type]) => (
          <div key={key} style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 700, color: '#1C1814', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</label>
            <input type={type} value={form[key]} onChange={e => setF(key, e.target.value)}
              placeholder={key === 'meetUrl' ? 'https://meet.google.com/...' : key === 'duration' ? '15' : ''}
              style={{ width: '100%', padding: '10px 14px', border: '1.5px solid #EDE9E2', borderRadius: 8, fontSize: 14, fontFamily: 'DM Sans, sans-serif', color: '#1C1814', outline: 'none', boxSizing: 'border-box', background: '#FAFAF8' }}
              onFocus={e => e.target.style.borderColor = '#1D5BAF'}
              onBlur={e => e.target.style.borderColor = '#EDE9E2'} />
          </div>
        ))}
        {err && <p style={{ color: '#D04040', fontSize: 13, margin: '0 0 12px' }}>{err}</p>}
        <button onClick={handleAdd} disabled={saving} style={{ width: '100%', padding: '12px', background: saving ? '#7AAAD8' : '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, fontSize: 14.5, fontWeight: 600, cursor: saving ? 'default' : 'pointer', fontFamily: 'DM Sans, sans-serif', marginTop: 8 }}>
          {saving ? 'Saving...' : added ? '✓ Availability Added!' : 'Generate Slots →'}
        </button>
      </div>
      <div>
        <div style={{ background: 'white', borderRadius: 14, padding: 24, border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)', marginBottom: 20 }}>
          <h4 style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, color: '#1C1814', margin: '0 0 14px' }}>Slot Preview</h4>
          {preview.length === 0 ? (
            <p style={{ color: '#B8AFA2', fontSize: 14 }}>Fill in the form to preview generated slots.</p>
          ) : (
            <div>
              <p style={{ color: '#7A7069', fontSize: 13, margin: '0 0 12px' }}>{preview.length} slots will be generated</p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {preview.map(s => <span key={s} style={{ background: '#E5EDF8', color: '#1D5BAF', fontSize: 12.5, fontWeight: 600, padding: '5px 12px', borderRadius: 8 }}>{fmt12(s)}</span>)}
              </div>
            </div>
          )}
        </div>
        <div style={{ background: 'white', borderRadius: 14, padding: 24, border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)' }}>
          <h4 style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, color: '#1C1814', margin: '0 0 14px' }}>Current Availability</h4>
          {availability.map(a => (
            <div key={a.date} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #F8F5F0' }}>
              <div>
                <div style={{ fontSize: 13.5, fontWeight: 600, color: '#1C1814' }}>{new Date(a.date + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}</div>
                <div style={{ fontSize: 12, color: '#B8AFA2' }}>{a.start}–{a.end} · {a.duration}min slots</div>
              </div>
              <span style={{ background: '#E5EDF8', color: '#1D5BAF', fontSize: 11, fontWeight: 700, padding: '3px 10px', borderRadius: 12 }}>{generateSlots(a).length} slots</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function SupervisorDashboard({ activeTab, setActiveTab, bookings, setBookings, availability, setAvailability, dataLoading, dataError }) {
  const [selectedDate, setSelectedDate] = useState('');
  const [notif, setNotif] = useState(true);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const { isMobile } = useBreakpoint();

  const onMark = async (id, status) => {
    const saved = await api.updateBookingStatus(id, status.toUpperCase().replace('-', '_'));
    const adapted = adaptBooking(saved);
    setBookings(prev => prev.map(b => b.id === id ? adapted : b));
    setSelectedStudent(current => current?.id === id ? adapted : current);
  };

  const bookingsByDate = {};
  bookings.forEach(b => { if (!bookingsByDate[b.date]) bookingsByDate[b.date] = []; bookingsByDate[b.date].push(b); });
  const upcomingDates = [...new Set(bookings.map(b => b.date))].sort();
  const currentDate = selectedDate || upcomingDates[0] || '';

  if (dataLoading) return <div style={{ color: '#7A7069', fontSize: 15 }}>Loading supervisor workspace...</div>;
  if (dataError) return <div style={{ background: 'white', borderRadius: 12, padding: 24, border: '1px solid #EDE9E2', color: '#D04040' }}>{dataError}</div>;

  if (activeTab === 'analytics') return <AnalyticsTab bookings={bookings} isMobile={isMobile} />;

  if (activeTab === 'availability') return (
    <div>
      <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 24px' }}>Manage Availability</h2>
      <AvailabilityTab availability={availability} setAvailability={setAvailability} isMobile={isMobile} />
    </div>
  );

  if (activeTab === 'calendar') {
    return (
      <div>
        <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 24px' }}>Booking Calendar</h2>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 24 }}>
          {upcomingDates.map(date => {
            const count = (bookingsByDate[date] || []).length;
            const sel = currentDate === date;
            return (
              <button key={date} onClick={() => { setSelectedDate(date); setActiveTab('sessions'); }}
                style={{ padding: '14px 20px', borderRadius: 12, border: '1.5px solid', borderColor: sel ? '#1D5BAF' : '#EDE9E2', background: sel ? '#1D5BAF' : 'white', cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'all 0.15s', textAlign: 'left' }}>
                <div style={{ fontSize: 13.5, fontWeight: 700, color: sel ? 'white' : '#1C1814', marginBottom: 4 }}>
                  {new Date(date + 'T00:00:00').toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' })}
                </div>
                <div style={{ fontSize: 12, color: sel ? 'rgba(255,255,255,0.75)' : '#1D5BAF', fontWeight: 600 }}>{count} booked</div>
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  const dayBookings = (bookingsByDate[currentDate] || []).sort((a, b) => a.time.localeCompare(b.time));
  return (
    <div>
      {notif && (
        <div style={{ background: 'white', border: '1px solid #EDE9E2', borderLeft: '4px solid #1D5BAF', borderRadius: 10, padding: '14px 18px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14, boxShadow: '0 2px 8px rgba(28,24,20,0.06)' }}>
          <div style={{ flexShrink: 0 }}><svg width="22" height="22" fill="none" viewBox="0 0 24 24"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="#1D5BAF" strokeWidth="1.8" strokeLinecap="round"/></svg></div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontWeight: 700, color: '#1C1814', fontSize: 14 }}>Session in 5 minutes: Alice Uwase – Group 3</div>
            <div style={{ color: '#7A7069', fontSize: 13, marginTop: 2 }}>AI Crop Disease Monitor · Apr 25 at 9:00 AM</div>
          </div>
          <button onClick={() => setNotif(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#B8AFA2', fontSize: 18, padding: 4, flexShrink: 0 }}>✕</button>
        </div>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'flex-end', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? 16 : 0, marginBottom: 24 }}>
        <div>
          <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 4px' }}>Today's Sessions</h2>
          <p style={{ color: '#7A7069', fontSize: 14.5, margin: 0 }}>{currentDate ? fmtDate(currentDate) : 'No sessions scheduled yet'}</p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {upcomingDates.map(d => (
            <button key={d} onClick={() => setSelectedDate(d)}
              style={{ padding: '7px 14px', borderRadius: 8, border: '1.5px solid', borderColor: currentDate === d ? '#1D5BAF' : '#EDE9E2', background: currentDate === d ? '#1D5BAF' : 'white', color: currentDate === d ? 'white' : '#7A7069', fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'all 0.15s' }}>
              {new Date(d + 'T00:00:00').toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}
            </button>
          ))}
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {dayBookings.length === 0
          ? <div style={{ background: 'white', borderRadius: 14, padding: 48, textAlign: 'center', border: '1px solid #EDE9E2' }}><p style={{ color: '#B8AFA2', fontSize: 15 }}>No sessions scheduled for this date.</p></div>
          : dayBookings.map(b => <SessionCard key={b.id} booking={b} onMark={onMark} onClick={() => setSelectedStudent(b)} isMobile={isMobile} />)
        }
      </div>
      <StudentDetailPanel booking={selectedStudent} onClose={() => setSelectedStudent(null)} onMark={onMark} isMobile={isMobile} />
    </div>
  );
}
