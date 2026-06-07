import { useEffect, useState, Dispatch, SetStateAction } from 'react';
import { generateSlots, fmt12, fmtDate } from '../data';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { adaptBooking, api } from '../api';
import { Booking, Availability } from '../types';

interface MiniCalendarProps {
  availDates: string[];
  selectedDate: string | null;
  setSelectedDate: (d: string) => void;
  bookings: Booking[];
  isMobile: boolean;
}

function MiniCalendar({ availDates, selectedDate, setSelectedDate, isMobile }: MiniCalendarProps) {
  const [viewMonth, setViewMonth] = useState<Date>(() => {
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
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  const pad = (d: number) => `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  const hasAvail = (d: number) => availDates.includes(pad(d));
  const isPast = (d: number) => new Date(`${pad(d)}T23:59:59`) < new Date();

  const cells: (number | null)[] = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  return (
    <div className={`bg-white rounded-[14px] p-6 border border-brand-darkBeige shadow-[0_2px_12px_rgba(28,24,20,0.07)] ${isMobile ? 'p-4' : 'p-6'}`}>
      <div className="flex items-center justify-between mb-5">
        <button 
          onClick={() => setViewMonth(new Date(year, month - 1, 1))} 
          className="bg-none border border-brand-darkBeige rounded-lg w-8 h-8 cursor-pointer flex items-center justify-center text-brand-gray hover:bg-brand-beige"
        >
          ‹
        </button>
        <span className="font-serif text-[17px] font-bold text-brand-charcoal">
          {monthNames[month]} {year}
        </span>
        <button 
          onClick={() => setViewMonth(new Date(year, month + 1, 1))} 
          className="bg-none border border-brand-darkBeige rounded-lg w-8 h-8 cursor-pointer flex items-center justify-center text-brand-gray hover:bg-brand-beige"
        >
          ›
        </button>
      </div>
      <div className="grid grid-cols-7 gap-0.5 mb-2">
        {dayNames.map(d => (
          <div key={d} className="text-center text-[11px] font-bold text-brand-lightGray py-1 tracking-wider">
            {d}
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7 gap-0.5">
        {cells.map((d, i) => {
          if (!d) return <div key={i} />;
          const dateStr = pad(d);
          const avail = hasAvail(d);
          const past = isPast(d);
          const sel = selectedDate === dateStr;
          const enabled = avail && !past;
          return (
            <button 
              key={i} 
              disabled={!enabled} 
              onClick={() => setSelectedDate(dateStr)}
              className={`aspect-square border-none rounded-lg text-[13.5px] transition-all duration-150 relative ${
                sel 
                  ? 'bg-brand-blue text-white font-bold' 
                  : enabled 
                    ? 'bg-brand-lightBlue text-brand-blue font-bold hover:bg-brand-blue hover:text-white' 
                    : 'bg-transparent text-[#C5BDB5]'
              }`}
            >
              {d}
              {enabled && !sel && (
                <span className="absolute bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-brand-blue" />
              )}
            </button>
          );
        })}
      </div>
      <div className="mt-4 flex gap-4">
        <div className="flex items-center gap-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-brand-lightBlue border border-brand-blue" />
          <span className="text-xs text-brand-gray font-sans">Available</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-brand-blue" />
          <span className="text-xs text-brand-gray font-sans">Selected</span>
        </div>
      </div>
    </div>
  );
}

interface BookingFormProps {
  slot: string;
  date: string;
  onBook: (form: { name: string; studentId: string; group: string; project: string }) => Promise<void>;
  onClose: () => void;
  isMobile: boolean;
}

function BookingForm({ slot, date, onBook, onClose, isMobile }: BookingFormProps) {
  const [form, setForm] = useState({ name: '', studentId: '', group: '', project: '' });
  const [submitted, setSubmitted] = useState(false);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');
  const set = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }));
  const valid = form.name && form.studentId && form.group && form.project;

  const handleSubmit = async () => {
    if (!valid) return;
    setSaving(true);
    setErr('');
    try {
      await onBook(form);
      setSubmitted(true);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setSaving(false);
    }
  };

  if (submitted) {
    return (
      <div className="fixed inset-0 bg-brand-charcoal/50 flex items-center justify-center z-[100] p-4">
        <div className="bg-white rounded-2xl p-8 md:p-12 w-full max-w-[420px] text-center shadow-2xl">
          <div className="w-16 h-16 rounded-full bg-brand-lightBlue flex items-center justify-center mx-auto mb-5">
            <svg className="w-7 h-7 stroke-brand-blue" fill="none" viewBox="0 0 24 24">
              <path d="M20 6L9 17l-5-5" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <h3 className="font-serif text-2xl text-brand-charcoal mb-2">Session Booked!</h3>
          <p className="text-brand-gray text-[14.5px] mb-1">{fmtDate(date)}</p>
          <p className="text-brand-blue text-lg font-bold mb-7">{fmt12(slot)}</p>
          <button 
            onClick={onClose} 
            className="background-brand-blue bg-brand-blue text-white border-none rounded-xl p-[12px_32px] text-[15px] font-semibold cursor-pointer font-sans hover:bg-brand-blue/90"
          >
            Done
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-brand-charcoal/50 flex items-center justify-center z-[100] p-4">
      <div className="bg-white rounded-2xl p-6 md:p-10 w-full max-w-[480px] max-h-[90vh] overflow-y-auto shadow-2xl">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h3 className="font-serif text-xl md:text-2xl text-brand-charcoal mb-1">Book Session</h3>
            <p className="text-brand-gray text-sm">{fmtDate(date)} · {fmt12(slot)}</p>
          </div>
          <button 
            onClick={onClose} 
            className="bg-none border-none cursor-pointer text-xl text-brand-lightGray p-1 hover:text-brand-charcoal"
          >
            ✕
          </button>
        </div>
        {[
          ['Full Name', 'name', 'text', 'e.g. Alice Uwase'], 
          ['Student ID', 'studentId', 'text', 'e.g. S001234'], 
          ['Group Number', 'group', 'number', 'e.g. 3'], 
          ['Project Name', 'project', 'text', 'e.g. AI Crop Monitor']
        ].map(([label, key, type, ph]) => (
          <div key={key} className="mb-4">
            <label className="block text-xs font-semibold text-brand-charcoal mb-1.5 tracking-wider uppercase font-sans">
              {label}
            </label>
            <input 
              type={type} 
              placeholder={ph} 
              value={form[key as keyof typeof form]} 
              onChange={e => set(key, e.target.value)}
              className="w-full p-[11px_14px] border border-brand-darkBeige focus:border-brand-blue rounded-lg text-[14.5px] font-sans text-brand-charcoal outline-none bg-[#FAFAF8] transition-colors"
            />
          </div>
        ))}
        {err && <p className="text-[#D04040] text-xs mb-3">{err}</p>}
        <div className="flex gap-3 mt-6">
          <button 
            onClick={onClose} 
            className="flex-1 p-3 border border-brand-darkBeige rounded-xl bg-white text-[14.5px] font-semibold cursor-pointer font-sans text-brand-gray hover:bg-brand-beige"
          >
            Cancel
          </button>
          <button 
            onClick={handleSubmit} 
            disabled={!valid || saving} 
            className={`flex-2 p-3 border-none rounded-xl text-white text-[14.5px] font-semibold font-sans transition-colors ${
              valid && !saving ? 'bg-brand-blue hover:bg-brand-blue/90 cursor-pointer' : 'bg-[#A5BFE0] cursor-default'
            }`}
          >
            {saving ? 'Booking...' : 'Confirm Booking'}
          </button>
        </div>
      </div>
    </div>
  );
}

interface StudentDashboardProps {
  activeTab: string;
  bookings: Booking[];
  setBookings: Dispatch<SetStateAction<Booking[]>>;
  availability: Availability[];
  dataLoading: boolean;
  dataError: string;
  onRefresh: () => Promise<void>;
}

export default function StudentDashboard({ activeTab, bookings, setBookings, availability, dataLoading, dataError, onRefresh }: StudentDashboardProps) {
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
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

  const handleBook = async (formData: { name: string; studentId: string; group: string; project: string }) => {
    if (!selectedDate || !selectedSlot) throw new Error('Please select an available date first.');
    const av = availability.find(a => a.date === selectedDate);
    if (!av) throw new Error('Please select an available date first.');
    const created = await api.createBooking({
      availabilityId: av.id,
      slotTime: selectedSlot,
      project: formData.project,
      groupNumber: parseInt(formData.group),
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

  if (dataLoading) return <div className="text-brand-gray text-[15px] font-sans">Loading your booking workspace...</div>;
  if (dataError) return <div className="bg-white rounded-xl p-6 border border-brand-darkBeige text-[#D04040] font-sans">{dataError}</div>;

  if (activeTab === 'mybooking') {
    return (
      <div className="max-w-[600px] font-sans">
        <h2 className="font-serif text-[26px] text-brand-charcoal mb-6">My Booking</h2>
        <div className="bg-gradient-to-r from-brand-blue to-[#1748A0] rounded-xl p-[16px_20px] mb-6 flex items-center gap-3.5 shadow-md">
          <div className="shrink-0">
            <svg className="w-5.5 h-5.5 stroke-white" fill="none" viewBox="0 0 24 24">
              <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" strokeWidth="1.8" strokeLinecap="round"/>
            </svg>
          </div>
          <div>
            <div className="text-white font-bold text-[14.5px]">Reminder: Your session is in 30 minutes</div>
            <div className="text-white/75 text-[13px]">Make sure your prototype demo is ready!</div>
          </div>
        </div>
        
        {myBooking ? (
          <div className="bg-white rounded-[14px] p-7 border border-brand-darkBeige shadow-[0_2px_12px_rgba(28,24,20,0.07)]">
            <div className="flex justify-between items-start mb-5">
              <div>
                <div className="text-xs font-bold text-brand-blue tracking-wider uppercase mb-1.5">Confirmed Session</div>
                <h3 className="font-serif text-xl text-brand-charcoal mb-1">{myBooking.project}</h3>
                <p className="text-brand-gray text-sm">Group {myBooking.group} · {myBooking.name}</p>
              </div>
              <div className="bg-brand-lightBlue text-brand-blue text-xs font-bold p-[4px_12px] rounded-full uppercase tracking-wider">
                Confirmed
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-5">
              {[
                ['Date', fmtDate(myBooking.date)], 
                ['Time', fmt12(myBooking.time)]
              ].map(([k, v]) => (
                <div key={k} className="bg-[#F8F5F0] rounded-xl p-[12px_16px]">
                  <div className="text-[11.5px] text-brand-lightGray font-bold uppercase tracking-wider mb-1">{k}</div>
                  <div className="text-[14.5px] text-brand-charcoal font-semibold">{v}</div>
                </div>
              ))}
            </div>

            {myBooking.meetUrl && (
              <div className="mb-5">
                <div className="text-[11.5px] text-brand-lightGray font-bold uppercase tracking-wider mb-2">Google Meet Link</div>
                <a 
                  href={myBooking.meetUrl} 
                  target="_blank" 
                  rel="noreferrer"
                  className="flex items-center gap-2 bg-[#F8F5F0] rounded-xl p-[12px_14px] text-sm text-brand-blue font-semibold no-underline border border-brand-darkBeige hover:border-brand-blue/50 word-break"
                >
                  <svg className="w-3.5 h-3.5 stroke-brand-blue shrink-0" fill="none" viewBox="0 0 24 24">
                    <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" strokeWidth="2" strokeLinecap="round"/>
                    <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" strokeWidth="2" strokeLinecap="round"/>
                  </svg>
                  {myBooking.meetUrl}
                </a>
              </div>
            )}

            <div className="flex gap-3">
              {myBooking.meetUrl && (
                <a 
                  href={myBooking.meetUrl} 
                  target="_blank" 
                  rel="noreferrer"
                  className="inline-block text-center bg-brand-blue hover:bg-brand-blue/90 text-white p-[10px_20px] rounded-xl font-bold text-sm no-underline font-sans"
                >
                  Join Meeting
                </a>
              )}
              <button 
                onClick={handleCancel} 
                disabled={canceling} 
                className="bg-none border border-brand-darkBeige rounded-xl p-[10px_20px] text-sm text-[#D04040] font-semibold cursor-pointer font-sans hover:bg-red-50 hover:border-red-200"
              >
                {canceling ? 'Canceling...' : 'Cancel Booking'}
              </button>
            </div>
          </div>
        ) : (
          <div className="bg-white rounded-[14px] p-12 text-center border border-brand-darkBeige">
            <div className="mb-4">
              <svg className="w-10 h-10 stroke-brand-lightGray mx-auto" fill="none" viewBox="0 0 24 24">
                <rect x="3" y="4" width="18" height="18" rx="2" strokeWidth="1.5"/>
                <path d="M16 2v4M8 2v4M3 10h18" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
            </div>
            <p className="text-brand-gray text-[15px]">No active booking. Go to <strong>Book a Session</strong> to schedule one.</p>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="font-sans">
      <h2 className="font-serif text-[26px] text-brand-charcoal mb-1.5">Book a Review Session</h2>
      <p className="text-brand-gray text-[14.5px] mb-7">Select an available date, then choose your preferred time slot.</p>
      
      <div className="grid grid-cols-1 md:grid-cols-[auto_1fr] gap-6 items-start">
        <div className="w-full md:w-80">
          <MiniCalendar 
            availDates={availDates} 
            selectedDate={selectedDate} 
            setSelectedDate={d => { setSelectedDate(d); setSelectedSlot(null); setShowForm(false); }} 
            bookings={bookings} 
            isMobile={isMobile} 
          />
        </div>
        
        <div>
          {!selectedDate ? (
            <div className="bg-white rounded-[14px] p-10 text-center border-2 border-dashed border-brand-darkBeige flex flex-col items-center justify-center min-h-[160px] md:min-h-[300px]">
              <div className="text-4xl mb-3">👆</div>
              <p className="text-brand-lightGray text-[15px] leading-relaxed">Select a highlighted date<br />to see available time slots</p>
            </div>
          ) : (
            <div className="bg-white rounded-[14px] p-6 border border-brand-darkBeige shadow-[0_2px_12px_rgba(28,24,20,0.07)]">
              <h3 className="font-serif text-lg text-brand-charcoal mb-1">{fmtDate(selectedDate)}</h3>
              <p className="text-brand-gray text-[13.5px] mb-5">{slotsForDate.length - bookedSlots.length} of {slotsForDate.length} slots available</p>
              
              <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                {slotsForDate.map(slot => {
                  const booked = bookedSlots.includes(slot);
                  const sel = selectedSlot === slot;
                  return (
                    <button 
                      key={slot} 
                      disabled={booked} 
                      onClick={() => { setSelectedSlot(slot); setShowForm(true); }}
                      className={`p-[12px_8px] border rounded-xl text-[13.5px] font-semibold transition-all duration-150 font-sans ${
                        sel 
                          ? 'border-brand-blue bg-brand-blue text-white' 
                          : booked 
                            ? 'border-brand-darkBeige bg-[#F8F5F0] text-[#C5BDB5] cursor-not-allowed' 
                            : 'border-brand-darkBeige bg-white text-brand-charcoal hover:border-brand-blue'
                      }`}
                    >
                      {fmt12(slot)}
                      {booked && <div className="text-[10px] text-[#C5BDB5] mt-0.5">Booked</div>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
      
      {showForm && selectedSlot && selectedDate && (
        <BookingForm 
          slot={selectedSlot} 
          date={selectedDate} 
          onBook={handleBook} 
          onClose={() => { setShowForm(false); setSelectedSlot(null); }} 
          isMobile={isMobile} 
        />
      )}
    </div>
  );
}
