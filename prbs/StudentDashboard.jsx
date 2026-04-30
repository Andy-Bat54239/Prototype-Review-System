
// ─── Student Dashboard ────────────────────────────────────────────
const { useState: useStudentState, useEffect: useStudentEffect } = React;

function MiniCalendar({ availDates, selectedDate, setSelectedDate, bookings }) {
  const [viewMonth, setViewMonth] = useStudentState(() => new Date('2026-04-01'));

  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  const dayNames = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  const pad = d => String(year) + '-' + String(month+1).padStart(2,'0') + '-' + String(d).padStart(2,'0');
  const hasAvail = d => availDates.includes(pad(d));
  const isBooked = d => bookings.some(b => b.date === pad(d));
  const isPast = d => new Date(pad(d)) < new Date('2026-04-23');

  const cells = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  return (
    <div style={{ background: 'white', borderRadius: 14, padding: 24, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <button onClick={() => setViewMonth(new Date(year, month-1, 1))} style={{ background: 'none', border: '1px solid #EDE9E2', borderRadius: 8, width: 32, height: 32, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#7A7069' }}>‹</button>
        <span style={{ fontFamily: 'Playfair Display, serif', fontSize: 17, fontWeight: 700, color: '#1C1814' }}>{monthNames[month]} {year}</span>
        <button onClick={() => setViewMonth(new Date(year, month+1, 1))} style={{ background: 'none', border: '1px solid #EDE9E2', borderRadius: 8, width: 32, height: 32, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#7A7069' }}>›</button>
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

function BookingForm({ slot, date, onBook, onClose }) {
  const [form, setForm] = useStudentState({ name: '', studentId: '', group: '', project: '' });
  const [submitted, setSubmitted] = useStudentState(false);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const valid = form.name && form.studentId && form.group && form.project;

  const handleSubmit = () => {
    if (!valid) return;
    onBook({ ...form, date, time: slot });
    setSubmitted(true);
  };

  if (submitted) return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(28,24,20,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
      <div style={{ background: 'white', borderRadius: 16, padding: 48, maxWidth: 420, textAlign: 'center', boxShadow: '0 24px 60px rgba(0,0,0,0.2)' }}>
        <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#E5EDF8', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}><svg width="28" height="28" fill="none" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#1D5BAF" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/></svg></div>
        <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 24, color: '#1C1814', margin: '0 0 10px' }}>Session Booked!</h3>
        <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 8px' }}>{fmtDate(date)}</p>
        <p style={{ color: '#1D5BAF', fontSize: 16, fontWeight: 700, margin: '0 0 28px' }}>{fmt12(slot)}</p>
        <button onClick={onClose} style={{ background: '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, padding: '12px 32px', fontSize: 15, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>Done</button>
      </div>
    </div>
  );

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(28,24,20,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
      <div style={{ background: 'white', borderRadius: 16, padding: 40, width: 480, boxShadow: '0 24px 60px rgba(0,0,0,0.2)' }}>
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
        <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
          <button onClick={onClose} style={{ flex: 1, padding: '12px', border: '1.5px solid #EDE9E2', borderRadius: 10, background: 'white', fontSize: 14.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', color: '#7A7069' }}>Cancel</button>
          <button onClick={handleSubmit} disabled={!valid} style={{ flex: 2, padding: '12px', border: 'none', borderRadius: 10, background: valid ? '#1D5BAF' : '#A5BFE0', color: 'white', fontSize: 14.5, fontWeight: 600, cursor: valid ? 'pointer' : 'default', fontFamily: 'DM Sans, sans-serif' }}>Confirm Booking</button>
        </div>
      </div>
    </div>
  );
}

function StudentDashboard({ activeTab, bookings, setBookings, availability }) {
  const [selectedDate, setSelectedDate] = useStudentState(null);
  const [selectedSlot, setSelectedSlot] = useStudentState(null);

  const availDates = availability.map(a => a.date);
  const myBooking = bookings.find(b => b.studentId === 'CURRENT');

  const slotsForDate = selectedDate ? (() => {
    const av = availability.find(a => a.date === selectedDate);
    if (!av) return [];
    return generateSlots(av);
  })() : [];

  const bookedSlots = selectedDate ? bookings.filter(b => b.date === selectedDate).map(b => b.time) : [];

  const handleBook = (data) => {
    const newB = { id: Date.now(), studentId: 'CURRENT', name: data.name, group: parseInt(data.group), project: data.project, date: data.date, time: data.time, status: 'confirmed', meetUrl: 'https://meet.google.com/new-booking' };
    setBookings(prev => [...prev, newB]);
  };

  const handleCancel = () => setBookings(prev => prev.filter(b => b.studentId !== 'CURRENT'));

  if (activeTab === 'mybooking') {
    return (
      <div style={{ maxWidth: 600 }}>
        <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 24px' }}>My Booking</h2>
        {/* Reminder banner */}
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
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
              {[['Date', fmtDate(myBooking.date)], ['Time', fmt12(myBooking.time)]].map(([k, v]) => (
                <div key={k} style={{ background: '#F8F5F0', borderRadius: 10, padding: '12px 16px' }}>
                  <div style={{ fontSize: 11.5, color: '#B8AFA2', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>{k}</div>
                  <div style={{ fontSize: 14.5, color: '#1C1814', fontWeight: 600 }}>{v}</div>
                </div>
              ))}
            </div>
            <button onClick={handleCancel} style={{ background: 'none', border: '1.5px solid #EDE9E2', borderRadius: 10, padding: '10px 20px', fontSize: 14, color: '#D04040', fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>Cancel Booking</button>
          </div>
        ) : (
          <div style={{ background: 'white', borderRadius: 14, padding: 48, textAlign: 'center', border: '1px solid #EDE9E2' }}>
            <div style={{ marginBottom: 16 }}><svg width="40" height="40" fill="none" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" stroke="#D9D2C7" strokeWidth="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="#D9D2C7" strokeWidth="1.5" strokeLinecap="round"/></svg></div>
            <p style={{ color: '#7A7069', fontSize: 15 }}>You don't have an active booking.<br />Go to <strong>Book a Session</strong> to schedule one.</p>
          </div>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 6px' }}>Book a Review Session</h2>
      <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 28px' }}>Select an available date, then choose your preferred time slot.</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: 24 }}>
        <div style={{ width: 320 }}>
          <MiniCalendar availDates={availDates} selectedDate={selectedDate} setSelectedDate={d => { setSelectedDate(d); setSelectedSlot(null); }} bookings={bookings} />
        </div>
        <div>
          {!selectedDate ? (
            <div style={{ background: 'white', borderRadius: 14, padding: 40, textAlign: 'center', border: '2px dashed #EDE9E2', height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ marginBottom: 16 }}><svg width="40" height="40" fill="none" viewBox="0 0 24 24"><path d="M9 11V6a2 2 0 014 0v4.5M13 10.5V9a2 2 0 014 0v5.5c0 3-2 5-5 5H9a5 5 0 01-5-5v-2a2 2 0 014 0" stroke="#D9D2C7" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg></div>
              <p style={{ color: '#B8AFA2', fontSize: 15 }}>Select a highlighted date<br />to see available time slots</p>
            </div>
          ) : (
            <div style={{ background: 'white', borderRadius: 14, padding: 24, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
              <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 18, color: '#1C1814', margin: '0 0 4px' }}>{fmtDate(selectedDate)}</h3>
              <p style={{ color: '#7A7069', fontSize: 13.5, margin: '0 0 20px' }}>{slotsForDate.length - bookedSlots.length} of {slotsForDate.length} slots available</p>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
                {slotsForDate.map(slot => {
                  const booked = bookedSlots.includes(slot);
                  const sel = selectedSlot === slot;
                  return (
                    <button key={slot} disabled={booked} onClick={() => setSelectedSlot(slot)}
                      style={{ padding: '12px 8px', border: '1.5px solid', borderColor: sel ? '#1D5BAF' : booked ? '#EDE9E2' : '#EDE9E2', borderRadius: 10, background: sel ? '#1D5BAF' : booked ? '#F8F5F0' : 'white', color: sel ? 'white' : booked ? '#C5BDB5' : '#1C1814', fontSize: 13.5, fontWeight: sel ? 700 : 600, cursor: booked ? 'not-allowed' : 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'all 0.15s', position: 'relative' }}>
                      {fmt12(slot)}
                      {booked && <div style={{ fontSize: 10, color: '#C5BDB5', marginTop: 2 }}>Booked</div>}
                    </button>
                  );
                })}
              </div>
              {selectedSlot && (
                <button onClick={() => {}} style={{ marginTop: 20, width: '100%', padding: '13px', background: '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, fontSize: 15, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}
                  onClick={() => document.getElementById('open-booking-form').click()}>
                  Book {fmt12(selectedSlot)} →
                </button>
              )}
              <button id="open-booking-form" style={{ display: 'none' }} onClick={() => {}} />
            </div>
          )}
        </div>
      </div>
      {selectedSlot && selectedDate && (
        <BookingForm slot={selectedSlot} date={selectedDate} onBook={handleBook} onClose={() => { setSelectedSlot(null); }} />
      )}
    </div>
  );
}

// Trigger the form directly
const _origStudentDash = StudentDashboard;
function StudentDashboardWrapper(props) {
  const [selectedDate, setSelectedDate] = useStudentState(null);
  const [selectedSlot, setSelectedSlot] = useStudentState(null);
  const [showForm, setShowForm] = useStudentState(false);
  const { bookings, setBookings, availability, activeTab } = props;

  const availDates = availability.map(a => a.date);
  const myBooking = bookings.find(b => b.studentId === 'CURRENT');
  const slotsForDate = selectedDate ? (() => { const av = availability.find(a => a.date === selectedDate); if (!av) return []; return generateSlots(av); })() : [];
  const bookedSlots = selectedDate ? bookings.filter(b => b.date === selectedDate).map(b => b.time) : [];
  const handleBook = (data) => { setBookings(prev => [...prev, { id: Date.now(), studentId: 'CURRENT', name: data.name, group: parseInt(data.group), project: data.project, date: data.date, time: data.time, status: 'confirmed', meetUrl: 'https://meet.google.com/new-booking' }]); };
  const handleCancel = () => setBookings(prev => prev.filter(b => b.studentId !== 'CURRENT'));

  if (activeTab === 'mybooking') {
    return (
      <div style={{ maxWidth: 600 }}>
        <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 24px' }}>My Booking</h2>
        <div style={{ background: 'linear-gradient(135deg, #1D5BAF, #1748A0)', borderRadius: 12, padding: '16px 20px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14 }}>
          <div><svg width="22" height="22" fill="none" viewBox="0 0 24 24"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="white" strokeWidth="1.8" strokeLinecap="round"/></svg></div>
          <div><div style={{ color: 'white', fontWeight: 700, fontSize: 14.5 }}>Reminder: Your session is in 30 minutes</div><div style={{ color: 'rgba(255,255,255,0.75)', fontSize: 13 }}>Make sure your prototype demo is ready!</div></div>
        </div>
        {myBooking ? (
          <div style={{ background: 'white', borderRadius: 14, padding: 28, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
              <div><div style={{ fontSize: 12, fontWeight: 700, color: '#1D5BAF', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 6 }}>Confirmed Session</div><h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 20, color: '#1C1814', margin: '0 0 4px' }}>{myBooking.project}</h3><p style={{ color: '#7A7069', fontSize: 14, margin: 0 }}>Group {myBooking.group} · {myBooking.name}</p></div>
              <div style={{ background: '#E5EDF8', color: '#1D5BAF', fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 20 }}>Confirmed</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
              {[['Date', fmtDate(myBooking.date)], ['Time', fmt12(myBooking.time)]].map(([k, v]) => (
                <div key={k} style={{ background: '#F8F5F0', borderRadius: 10, padding: '12px 16px' }}><div style={{ fontSize: 11.5, color: '#B8AFA2', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>{k}</div><div style={{ fontSize: 14.5, color: '#1C1814', fontWeight: 600 }}>{v}</div></div>
              ))}
            </div>
            <button onClick={handleCancel} style={{ background: 'none', border: '1.5px solid #EDE9E2', borderRadius: 10, padding: '10px 20px', fontSize: 14, color: '#D04040', fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>Cancel Booking</button>
          </div>
        ) : (
          <div style={{ background: 'white', borderRadius: 14, padding: 48, textAlign: 'center', border: '1px solid #EDE9E2' }}><div style={{ marginBottom: 16 }}><svg width="40" height="40" fill="none" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" stroke="#D9D2C7" strokeWidth="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="#D9D2C7" strokeWidth="1.5" strokeLinecap="round"/></svg></div><p style={{ color: '#7A7069', fontSize: 15 }}>No active booking. Go to <strong>Book a Session</strong> to schedule one.</p></div>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 6px' }}>Book a Review Session</h2>
      <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 28px' }}>Select an available date, then choose your preferred time slot.</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: 24, alignItems: 'start' }}>
        <div style={{ width: 320 }}>
          <MiniCalendar availDates={availDates} selectedDate={selectedDate} setSelectedDate={d => { setSelectedDate(d); setSelectedSlot(null); setShowForm(false); }} bookings={bookings} />
        </div>
        <div>
          {!selectedDate ? (
            <div style={{ background: 'white', borderRadius: 14, padding: 40, textAlign: 'center', border: '2px dashed #EDE9E2', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 300 }}>
              <div style={{ fontSize: 36, marginBottom: 12 }}>👆</div>
              <p style={{ color: '#B8AFA2', fontSize: 15, lineHeight: 1.6 }}>Select a highlighted date<br />to see available time slots</p>
            </div>
          ) : (
            <div style={{ background: 'white', borderRadius: 14, padding: 24, boxShadow: '0 2px 12px rgba(28,24,20,0.07)', border: '1px solid #EDE9E2' }}>
              <h3 style={{ fontFamily: 'Playfair Display, serif', fontSize: 18, color: '#1C1814', margin: '0 0 4px' }}>{fmtDate(selectedDate)}</h3>
              <p style={{ color: '#7A7069', fontSize: 13.5, margin: '0 0 20px' }}>{slotsForDate.length - bookedSlots.length} of {slotsForDate.length} slots available</p>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
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
        <BookingForm slot={selectedSlot} date={selectedDate} onBook={handleBook} onClose={() => { setShowForm(false); setSelectedSlot(null); }} />
      )}
    </div>
  );
}

Object.assign(window, { StudentDashboard: StudentDashboardWrapper });
