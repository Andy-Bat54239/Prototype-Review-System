import { useEffect, useState } from 'react';
import { generateSlots, fmt12, fmtDate } from '../data';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { adaptBooking, api } from '../api';

function MiniCalendar({ availDates, selectedDate, setSelectedDate, bookings, isMobile }) {
  const [viewMonth, setViewMonth] = useState(() => {
    const first = availDates[0] || new Date().toISOString().slice(0, 10);
    return new Date(`${first.slice(0, 7)}-01T00:00:00`);
  });

  useEffect(() => {
    if (availDates.length === 0) return;
    setViewMonth(new Date(`${availDates[0].slice(0, 7)}-01T00:00:00`));
  }, [availDates[0]]);

  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  const dayNames = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  const pad = d => `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  const hasAvail = d => availDates.includes(pad(d));
  const isPast = d => new Date(`${pad(d)}T23:59:59`) < new Date();

  const cells = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  return (
    <div style={{ background: 'white', borderRadius: 14, padding: isMobile ? 16 : 24, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <button onClick={() => setViewMonth(new Date(year, month - 1, 1))} style={{ background: 'none', border: '1px solid #EDE9E2', borderRadius: 8, width: 32, height: 32, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#7A7069' }}>‹</button>
        <span style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, fontWeight: 700, color: '#1C1814' }}>{monthNames[month]} {year}</span>
        <button onClick={() => setViewMonth(new Date(year, month + 1, 1))} style={{ background: 'none', border: '1px solid #EDE9E2', borderRadius: 8, width: 32, height: 32, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#7A7069' }}>›</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2, marginBottom: 8 }}>
        {dayNames.map(d => <div key={d} style={{ textAlign: 'center', fontSize: 11, fontWeight: 700, color: '#B8AFA2', padding: '4px 0', letterSpacing: '0.04em' }}>{d}</div>)}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
        {cells.map((d, i) => {
          if (!d) return <div key={i} />;
          const dateStr = pad(d);
          const avail = hasAvail(d);
          const past = isPast(d);
          const sel = selectedDate === dateStr;
          return (
            <button key={i} disabled={!avail || past} onClick={() => setSelectedDate(dateStr)}
              style={{ aspectRatio: '1', border: 'none', borderRadius: 8, cursor: avail && !past ? 'pointer' : 'default', fontSize: 13.5, fontWeight: avail ? 700 : 400, transition: 'all 0.15s',
                background: sel ? '#1D5BAF' : avail && !past ? '#E5EDF8' : 'transparent',
                color: sel ? 'white' : avail && !past ? '#1D5BAF' : '#C5BDB5',
                position: 'relative'
              }}>
              {d}
              {avail && !past && !sel && <span style={{ position: 'absolute', bottom: 3, left: '50%', transform: 'translateX(-50%)', width: 4, height: 4, borderRadius: '50%', background: '#1D5BAF' }} />}
            </button>
          );
        })}
      </div>
      <div style={{ marginTop: 16, display: 'flex', gap: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><div style={{ width: 10, height: 10, borderRadius: '50%', background: '#E5EDF8', border: '1.5px solid #1D5BAF' }} /><span style={{ fontSize: 12, color: '#7A7069' }}>Available</span></div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><div style={{ width: 10, height: 10, borderRadius: '50%', background: '#1D5BAF' }} /><span style={{ fontSize: 12, color: '#7A7069' }}>Selected</span></div>
      </div>
    </div>
  );
}

function BookingForm({ slot, date, onBook, onClose, isMobile }) {
  const [form, setForm] = useState({ name: '', studentId: '', group: '', project: '' });
  const [submitted, setSubmitted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const valid = form.name && form.studentId && form.group && form.project;

  const handleSubmit = async () => {
    if (!valid) return;
    setSaving(true);
    setErr('');
    try {
      await onBook({ ...form, date, time: slot });
      setSubmitted(true);
    } catch (e) {
      setErr(e.message);
    } finally {
      setSaving(false);
    }
  };

  if (submitted) return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(28,24,20,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
      <div style={{ background: 'white', borderRadius: 16, padding: isMobile ? 32 : 48, width: '100%', maxWidth: 420, textAlign: 'center', boxShadow: '0 24px 60px rgba(0,0,0,0.2)' }}>
        <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#E5EDF8', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
          <svg width="28" height="28" fill="none" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#1D5BAF" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </div>
        <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 24, color: '#1C1814', margin: '0 0 10px' }}>Session Booked!</h3>
        <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 8px' }}>{fmtDate(date)}</p>
        <p style={{ color: '#1D5BAF', fontSize: 16, fontWeight: 700, margin: '0 0 28px' }}>{fmt12(slot)}</p>
        <button onClick={onClose} style={{ background: '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, padding: '12px 32px', fontSize: 15, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>Done</button>
      </div>
    </div>
  );

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(28,24,20,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
      <div style={{ background: 'white', borderRadius: 16, padding: isMobile ? 24 : 40, width: '100%', maxWidth: 480, maxHeight: '90vh', overflowY: 'auto', boxShadow: '0 24px 60px rgba(0,0,0,0.2)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
          <div>
            <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 22, color: '#1C1814', margin: '0 0 4px' }}>Book Session</h3>
            <p style={{ color: '#7A7069', fontSize: 14, margin: 0 }}>{fmtDate(date)} · {fmt12(slot)}</p>
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 20, color: '#B8AFA2', padding: 4 }}>✕</button>
        </div>
        {[['Full Name', 'name', 'text', 'e.g. Alice Uwase'], ['Student ID', 'studentId', 'text', 'e.g. S001234'], ['Group Number', 'group', 'number', 'e.g. 3'], ['Project Name', 'project', 'text', 'e.g. AI Crop Monitor']].map(([label, key, type, ph]) => (
          <div key={key} style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', fontSize: 12.5, fontWeight: 600, color: '#1C1814', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>{label}</label>
            <input type={type} placeholder={ph} value={form[key]} onChange={e => set(key, e.target.value)}
              style={{ width: '100%', padding: '11px 14px', border: '1.5px solid #EDE9E2', borderRadius: 8, fontSize: 14.5, fontFamily: 'DM Sans, sans-serif', color: '#1C1814', outline: 'none', boxSizing: 'border-box', background: '#FAFAF8' }}
              onFocus={e => e.target.style.borderColor = '#1D5BAF'}
              onBlur={e => e.target.style.borderColor = '#EDE9E2'} />
          </div>
        ))}
        {err && <p style={{ color: '#D04040', fontSize: 13, margin: '0 0 12px' }}>{err}</p>}
        <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
          <button onClick={onClose} style={{ flex: 1, padding: '12px', border: '1.5px solid #EDE9E2', borderRadius: 10, background: 'white', fontSize: 14.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', color: '#7A7069' }}>Cancel</button>
          <button onClick={handleSubmit} disabled={!valid || saving} style={{ flex: 2, padding: '12px', border: 'none', borderRadius: 10, background: valid && !saving ? '#1D5BAF' : '#A5BFE0', color: 'white', fontSize: 14.5, fontWeight: 600, cursor: valid && !saving ? 'pointer' : 'default', fontFamily: 'DM Sans, sans-serif' }}>{saving ? 'Booking...' : 'Confirm Booking'}</button>
        </div>
      </div>
    </div>
  );
}

export default function StudentDashboard({ activeTab, bookings, setBookings, availability, dataLoading, dataError, onRefresh }) {
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [canceling, setCanceling] = useState(false);
  const { isMobile } = useBreakpoint();

  const availDates = availability.map(a => a.date);
  const myBooking = bookings.find(b => b.status !== 'cancelled');
  const slotsForDate = selectedDate ? (() => {
    const av = availability.find(a => a.date === selectedDate);
    return av ? generateSlots(av) : [];
  })() : [];
  const bookedSlots = selectedDate ? bookings.filter(b => b.date === selectedDate).map(b => b.time) : [];

  const handleBook = async (data) => {
    const av = availability.find(a => a.date === data.date);
    if (!av) throw new Error('Please select an available date first.');
    const created = await api.createBooking({
      availabilityId: av.id,
      slotTime: data.time,
      project: data.project,
      groupNumber: parseInt(data.group),
    });
    setBookings(prev => [adaptBooking(created), ...prev]);
  };
  const handleCancel = async () => {
    if (!myBooking) return;
    setCanceling(true);
    try {
      await api.cancelBooking(myBooking.id);
      await onRefresh();
    } finally {
      setCanceling(false);
    }
  };

  if (dataLoading) return <div style={{ color: '#7A7069', fontSize: 15 }}>Loading your booking workspace...</div>;
  if (dataError) return <div style={{ background: 'white', borderRadius: 12, padding: 24, border: '1px solid #EDE9E2', color: '#D04040' }}>{dataError}</div>;

  if (activeTab === 'mybooking') {
    return (
      <div style={{ maxWidth: 600 }}>
        <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 24px' }}>My Booking</h2>
        <div style={{ background: 'linear-gradient(135deg, #1D5BAF, #1748A0)', borderRadius: 12, padding: '16px 20px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14 }}>
          <div><svg width="22" height="22" fill="none" viewBox="0 0 24 24"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="white" strokeWidth="1.8" strokeLinecap="round"/></svg></div>
          <div>
            <div style={{ color: 'white', fontWeight: 700, fontSize: 14.5 }}>Reminder: Your session is in 30 minutes</div>
            <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: 13 }}>Make sure your prototype demo is ready!</div>
          </div>
        </div>
        {myBooking ? (
          <div style={{ background: 'white', borderRadius: 14, padding: 28, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: '#1D5BAF', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 6 }}>Confirmed Session</div>
                <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 20, color: '#1C1814', margin: '0 0 4px' }}>{myBooking.project}</h3>
                <p style={{ color: '#7A7069', fontSize: 14, margin: 0 }}>Group {myBooking.group} · {myBooking.name}</p>
              </div>
              <div style={{ background: '#E5EDF8', color: '#1D5BAF', fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 20 }}>Confirmed</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr', gap: 12, marginBottom: 20 }}>
              {[['Date', fmtDate(myBooking.date)], ['Time', fmt12(myBooking.time)]].map(([k, v]) => (
                <div key={k} style={{ background: '#F8F5F0', borderRadius: 10, padding: '12px 16px' }}>
                  <div style={{ fontSize: 11.5, color: '#B8AFA2', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>{k}</div>
                  <div style={{ fontSize: 14.5, color: '#1C1814', fontWeight: 600 }}>{v}</div>
                </div>
              ))}
            </div>
            {myBooking.meetUrl && (
              <div style={{ marginBottom: 20 }}>
                <div style={{ fontSize: 11.5, color: '#B8AFA2', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>Google Meet Link</div>
                <a href={myBooking.meetUrl} target="_blank" rel="noreferrer"
                  style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#F8F5F0', borderRadius: 10, padding: '12px 14px', fontSize: 13, color: '#1D5BAF', fontWeight: 600, textDecoration: 'none', border: '1px solid #EDE9E2', wordBreak: 'break-all' }}>
                  <svg width="14" height="14" fill="none" viewBox="0 0 24 24"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" stroke="#1D5BAF" strokeWidth="2" strokeLinecap="round"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" stroke="#1D5BAF" strokeWidth="2" strokeLinecap="round"/></svg>
                  {myBooking.meetUrl}
                </a>
              </div>
            )}
            <div style={{ display: 'flex', gap: 12 }}>
              {myBooking.meetUrl && (
                <a href={myBooking.meetUrl} target="_blank" rel="noreferrer"
                  style={{ display: 'inline-block', textAlign: 'center', background: '#1D5BAF', color: 'white', padding: '10px 20px', borderRadius: 10, fontWeight: 700, fontSize: 14, textDecoration: 'none', fontFamily: 'DM Sans, sans-serif' }}>
                  Join Meeting
                </a>
              )}
              <button onClick={handleCancel} disabled={canceling} style={{ background: 'none', border: '1.5px solid #EDE9E2', borderRadius: 10, padding: '10px 20px', fontSize: 14, color: '#D04040', fontWeight: 600, cursor: canceling ? 'default' : 'pointer', fontFamily: 'DM Sans, sans-serif' }}>{canceling ? 'Canceling...' : 'Cancel Booking'}</button>
            </div>
          </div>
        ) : (
          <div style={{ background: 'white', borderRadius: 14, padding: 48, textAlign: 'center', border: '1px solid #EDE9E2' }}>
            <div style={{ marginBottom: 16 }}><svg width="40" height="40" fill="none" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" stroke="#D9D2C7" strokeWidth="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="#D9D2C7" strokeWidth="1.5" strokeLinecap="round"/></svg></div>
            <p style={{ color: '#7A7069', fontSize: 15 }}>No active booking. Go to <strong>Book a Session</strong> to schedule one.</p>
          </div>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 6px' }}>Book a Review Session</h2>
      <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 28px' }}>Select an available date, then choose your preferred time slot.</p>
      <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'auto 1fr', gap: 24, alignItems: 'start' }}>
        <div style={isMobile ? {} : { width: 320 }}>
          <MiniCalendar availDates={availDates} selectedDate={selectedDate} setSelectedDate={d => { setSelectedDate(d); setSelectedSlot(null); setShowForm(false); }} bookings={bookings} isMobile={isMobile} />
        </div>
        <div>
          {!selectedDate ? (
            <div style={{ background: 'white', borderRadius: 14, padding: 40, textAlign: 'center', border: '2px dashed #EDE9E2', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: isMobile ? 160 : 300 }}>
              <div style={{ fontSize: 36, marginBottom: 12 }}>👆</div>
              <p style={{ color: '#B8AFA2', fontSize: 15, lineHeight: 1.6 }}>Select a highlighted date<br />to see available time slots</p>
            </div>
          ) : (
            <div style={{ background: 'white', borderRadius: 14, padding: 24, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
              <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 18, color: '#1C1814', margin: '0 0 4px' }}>{fmtDate(selectedDate)}</h3>
              <p style={{ color: '#7A7069', fontSize: 13.5, margin: '0 0 20px' }}>{slotsForDate.length - bookedSlots.length} of {slotsForDate.length} slots available</p>
              <div style={{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)', gap: 8 }}>
                {slotsForDate.map(slot => {
                  const booked = bookedSlots.includes(slot);
                  const sel = selectedSlot === slot;
                  return (
                    <button key={slot} disabled={booked} onClick={() => { setSelectedSlot(slot); setShowForm(true); }}
                      style={{ padding: '12px 8px', border: '1.5px solid', borderColor: sel ? '#1D5BAF' : '#EDE9E2', borderRadius: 10, background: sel ? '#1D5BAF' : booked ? '#F8F5F0' : 'white', color: sel ? 'white' : booked ? '#C5BDB5' : '#1C1814', fontSize: 13.5, fontWeight: 600, cursor: booked ? 'not-allowed' : 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'all 0.15s' }}>
                      {fmt12(slot)}
                      {booked && <div style={{ fontSize: 10, color: '#C5BDB5', marginTop: 2 }}>Booked</div>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
      {showForm && selectedSlot && selectedDate && (
        <BookingForm slot={selectedSlot} date={selectedDate} onBook={handleBook} onClose={() => { setShowForm(false); setSelectedSlot(null); }} isMobile={isMobile} />
      )}
    </div>
  );
}
