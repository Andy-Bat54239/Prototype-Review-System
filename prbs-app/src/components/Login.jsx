import { useState } from 'react';
import logoUrl from '../assets/logo.png';
import campusUrl from '../assets/campus.jpg';
import { useBreakpoint } from '../hooks/useBreakpoint';

export default function Login({ onLogin }) {
  const [step, setStep] = useState('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState('');
  const { isMobile } = useBreakpoint();

  const sendOTP = () => {
    if (!email.includes('@')) { setErr('Please enter a valid email address.'); return; }
    setErr('');
    setLoading(true);
    setTimeout(() => { setLoading(false); setStep('otp'); }, 1200);
  };

  const handleOtpChange = (i, v) => {
    if (!/^\d*$/.test(v)) return;
    const next = [...otp];
    next[i] = v.slice(-1);
    setOtp(next);
    if (v && i < 5) document.getElementById(`otp-${i + 1}`)?.focus();
  };

  const handleOtpKey = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) document.getElementById(`otp-${i - 1}`)?.focus();
  };

  const verifyOTP = () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const em = email.toLowerCase();
      let role = 'student';
      if (em.includes('supervisor')) role = 'supervisor';
      else if (em.includes('admin')) role = 'admin';
      onLogin({ email, role });
    }, 1000);
  };

  const filled = otp.every(d => d !== '');

  return (
    <div style={{ height: '100vh', background: '#F8F5F0', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: isMobile ? '0' : '24px', boxSizing: 'border-box', fontFamily: 'DM Sans, system-ui, sans-serif' }}>
      <div style={{ display: 'flex', width: '100%', maxWidth: 1140, height: isMobile ? '100%' : '100%', boxShadow: isMobile ? 'none' : '0 24px 80px rgba(28,24,20,0.13)', borderRadius: isMobile ? 0 : 20, overflow: 'hidden' }}>

        {/* Brand panel — hidden on mobile */}
        {!isMobile && (
          <div style={{ flex: '0 0 360px', padding: '44px 40px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', inset: 0, backgroundImage: `url(${campusUrl})`, backgroundSize: 'cover', backgroundPosition: 'center', filter: 'blur(4px)', transform: 'scale(1.08)' }} />
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(160deg, rgba(20,40,90,0.82) 0%, rgba(10,20,55,0.90) 100%)' }} />
            <div style={{ position: 'relative', zIndex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 40 }}>
                <img src={logoUrl} alt="AUCA Logo" style={{ width: 56, height: 56, borderRadius: '50%', background: 'white', padding: 3, objectFit: 'contain', flexShrink: 0 }} />
                <div>
                  <div style={{ color: 'white', fontSize: 15, fontWeight: 700, letterSpacing: '-0.01em', lineHeight: 1.2 }}>AUCA</div>
                  <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 11.5, lineHeight: 1.3 }}>Adventist University<br />of Central Africa</div>
                </div>
              </div>
              <h1 style={{ fontFamily: 'Playfair Display, Georgia, serif', color: 'white', fontSize: 32, fontWeight: 700, lineHeight: 1.2, margin: '0 0 14px' }}>Prototype Review Booking System</h1>
              <p style={{ color: 'rgba(255,255,255,0.55)', fontSize: 14.5, lineHeight: 1.65, margin: 0 }}>Schedule, track, and manage academic prototype review sessions — seamlessly.</p>
            </div>
            <div style={{ position: 'relative', zIndex: 1 }}>
              {['Students book sessions instantly', 'Supervisors manage their calendar', 'Admins control system settings'].map((t, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#7AAADE', flexShrink: 0 }} />
                  <span style={{ color: 'rgba(255,255,255,0.65)', fontSize: 13.5 }}>{t}</span>
                </div>
              ))}
              <p style={{ color: 'rgba(255,255,255,0.25)', fontSize: 12, marginTop: 32 }}>Adventist University of Central Africa · Rwanda</p>
            </div>
          </div>
        )}

        {/* Form panel */}
        <div style={{ flex: 1, background: '#FFFFFF', padding: isMobile ? '40px 24px' : '44px 48px', display: 'flex', flexDirection: 'column', justifyContent: 'center', overflowY: 'auto' }}>
          {/* Mobile logo */}
          {isMobile && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 36 }}>
              <img src={logoUrl} alt="AUCA Logo" style={{ width: 40, height: 40, borderRadius: '50%', background: '#E5EDF8', padding: 2, objectFit: 'contain' }} />
              <div>
                <div style={{ fontSize: 14, fontWeight: 700, color: '#1C1814', lineHeight: 1.2 }}>AUCA · PRBS</div>
                <div style={{ fontSize: 11, color: '#B8AFA2' }}>Review Booking System</div>
              </div>
            </div>
          )}

          {step === 'email' ? (
            <div>
              <h2 style={{ fontFamily: 'Playfair Display, Georgia, serif', fontSize: isMobile ? 24 : 28, fontWeight: 700, color: '#1C1814', margin: '0 0 8px', letterSpacing: '-0.02em' }}>Welcome back</h2>
              <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 36px' }}>Enter your university email to receive a one-time passcode.</p>
              <label style={{ display: 'block', fontSize: 13, fontWeight: 600, color: '#1C1814', marginBottom: 8, letterSpacing: '0.04em', textTransform: 'uppercase' }}>Email Address</label>
              <input
                type="email" value={email} onChange={e => { setEmail(e.target.value); setErr(''); }}
                onKeyDown={e => e.key === 'Enter' && sendOTP()}
                placeholder="yourname@university.ac.rw"
                style={{ width: '100%', padding: '14px 16px', border: '1.5px solid #EDE9E2', borderRadius: 10, fontSize: 15, outline: 'none', boxSizing: 'border-box', fontFamily: 'DM Sans, sans-serif', color: '#1C1814', background: '#FAFAF8', transition: 'border-color 0.2s' }}
                onFocus={e => e.target.style.borderColor = '#1D5BAF'}
                onBlur={e => e.target.style.borderColor = '#EDE9E2'}
              />
              {err && <p style={{ color: '#D04040', fontSize: 13, marginTop: 8 }}>{err}</p>}
              <button
                onClick={sendOTP} disabled={loading}
                style={{ marginTop: 20, width: '100%', padding: '14px', background: loading ? '#7AAAD8' : '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, fontSize: 15, fontWeight: 600, cursor: loading ? 'default' : 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'background 0.2s', letterSpacing: '-0.01em' }}
              >
                {loading ? 'Sending code…' : 'Send One-Time Code →'}
              </button>
              <div style={{ marginTop: 28, padding: 16, background: '#F8F5F0', borderRadius: 10, border: '1px solid #EDE9E2' }}>
                <p style={{ margin: 0, fontSize: 12.5, color: '#7A7069', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <svg width="13" height="13" fill="none" viewBox="0 0 24 24"><circle cx="8" cy="15" r="5" stroke="#7A7069" strokeWidth="2" /><path d="M13 10l8-8M21 2l-3 3M17 6l2-2" stroke="#7A7069" strokeWidth="2" strokeLinecap="round" /></svg>
                  Demo quick access:
                </p>
                {[['supervisor@university.ac.rw', 'Supervisor'], ['admin@university.ac.rw', 'Admin'], ['alice@university.ac.rw', 'Student']].map(([em, label]) => (
                  <button key={em} onClick={() => setEmail(em)} style={{ display: 'block', marginTop: 6, background: 'none', border: 'none', padding: 0, color: '#1D5BAF', fontSize: 13, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', textDecoration: 'underline' }}>
                    {em} <span style={{ color: '#7A7069', textDecoration: 'none' }}>({label})</span>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div>
              <button onClick={() => setStep('email')} style={{ background: 'none', border: 'none', color: '#7A7069', fontSize: 13.5, cursor: 'pointer', marginBottom: 28, padding: 0, display: 'flex', alignItems: 'center', gap: 6, fontFamily: 'DM Sans, sans-serif' }}>← Back</button>
              <h2 style={{ fontFamily: 'Playfair Display, Georgia, serif', fontSize: isMobile ? 24 : 28, fontWeight: 700, color: '#1C1814', margin: '0 0 8px' }}>Check your email</h2>
              <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 6px' }}>We sent a 6-digit code to</p>
              <p style={{ color: '#1C1814', fontSize: 15, fontWeight: 600, margin: '0 0 36px' }}>{email}</p>
              <div style={{ display: 'flex', gap: isMobile ? 6 : 10, marginBottom: 28 }}>
                {otp.map((d, i) => (
                  <input key={i} id={`otp-${i}`} type="text" inputMode="numeric" maxLength={1} value={d}
                    onChange={e => handleOtpChange(i, e.target.value)}
                    onKeyDown={e => handleOtpKey(i, e)}
                    style={{ flex: 1, minWidth: 0, height: isMobile ? 52 : 60, textAlign: 'center', fontSize: isMobile ? 20 : 24, fontWeight: 700, border: '1.5px solid', borderColor: d ? '#1D5BAF' : '#EDE9E2', borderRadius: 10, outline: 'none', fontFamily: 'DM Sans, sans-serif', color: '#1C1814', background: d ? '#E5EDF8' : '#FAFAF8', transition: 'all 0.15s' }}
                  />
                ))}
              </div>
              <button onClick={verifyOTP} disabled={!filled || loading}
                style={{ width: '100%', padding: '14px', background: filled && !loading ? '#1D5BAF' : '#A5BFE0', color: 'white', border: 'none', borderRadius: 10, fontSize: 15, fontWeight: 600, cursor: filled && !loading ? 'pointer' : 'default', fontFamily: 'DM Sans, sans-serif' }}>
                {loading ? 'Verifying…' : 'Verify & Sign In →'}
              </button>
              <p style={{ marginTop: 16, fontSize: 13, color: '#7A7069' }}>Any 6-digit code will work for this demo.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
