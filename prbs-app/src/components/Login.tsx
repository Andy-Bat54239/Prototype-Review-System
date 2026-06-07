import { useState, KeyboardEvent, ChangeEvent } from 'react';
import logoUrl from '../assets/logo.png';
import campusUrl from '../assets/campus.jpg';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { api } from '../api';
import { User } from '../types';

interface LoginProps {
  onLogin: (user: User) => void;
}

export default function Login({ onLogin }: LoginProps) {
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState<string[]>(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState('');
  const { isMobile } = useBreakpoint();

  const sendOTP = async () => {
    if (!email.includes('@')) {
      setErr('Please enter a valid email address.');
      return;
    }
    setErr('');
    setLoading(true);
    try {
      await api.sendOtp(email.trim());
      setStep('otp');
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleOtpChange = (i: number, v: string) => {
    if (!/^\d*$/.test(v)) return;
    const next = [...otp];
    next[i] = v.slice(-1);
    setOtp(next);
    if (v && i < 5) {
      document.getElementById(`otp-${i + 1}`)?.focus();
    }
  };

  const handleOtpKey = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) {
      document.getElementById(`otp-${i - 1}`)?.focus();
    }
  };

  const verifyOTP = async () => {
    setLoading(true);
    setErr('');
    try {
      const user = await api.verifyOtp(email.trim(), otp.join(''));
      if (user) onLogin(user);
    } catch (e: any) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  const filled = otp.every(d => d !== '');

  return (
    <div className="min-h-screen bg-[#F8F5F0] flex items-center justify-center p-6 md:p-12 box-border font-sans">
      <div className="flex w-full max-w-[960px] min-h-[580px] shadow-[0_24px_80px_rgba(28,24,20,0.13)] rounded-[20px] overflow-hidden">
        
        {/* Brand panel — hidden on mobile */}
        {!isMobile && (
          <div className="hidden md:flex flex-col justify-between w-[380px] p-[56px_48px] relative overflow-hidden shrink-0">
            <div 
              className="absolute inset-0"
              style={{ backgroundImage: `url(${campusUrl})`, backgroundSize: 'cover', backgroundPosition: 'center', filter: 'blur(4px)', transform: 'scale(1.08)' }} 
            />
            <div className="absolute inset-0" style={{ background: 'linear-gradient(160deg, rgba(20,40,90,0.82) 0%, rgba(10,20,55,0.90) 100%)' }} />
            
            <div className="relative z-10">
              <div className="flex items-center gap-[14px] mb-10">
                <img 
                  src={logoUrl} 
                  alt="AUCA Logo" 
                  className="w-14 h-14 rounded-full bg-white p-[3px] object-contain shrink-0" 
                />
                <div>
                  <div className="text-white text-[15px] font-bold tracking-tight leading-none mb-1">AUCA</div>
                  <div className="text-white/50 text-[11.5px] leading-tight">
                    Adventist University<br />of Central Africa
                  </div>
                </div>
              </div>
              <h1 className="font-serif text-white text-[32px] font-bold leading-tight mb-3.5">
                Prototype Review Booking System
              </h1>
              <p className="text-white/55 text-[14.5px] leading-[1.65]">
                Schedule, track, and manage academic prototype review sessions — seamlessly.
              </p>
            </div>
            
            <div className="relative z-10">
              {[
                'Students book sessions instantly', 
                'Supervisors manage their calendar', 
                'Admins control system settings'
              ].map((t, i) => (
                <div key={i} className="flex items-center gap-[10px] mb-[14px]">
                  <div className="w-1.5 h-1.5 rounded-full bg-[#7AAADE] shrink-0" />
                  <span className="text-white/65 text-[13.5px]">{t}</span>
                </div>
              ))}
              <p className="text-white/25 text-xs mt-8">
                Adventist University of Central Africa · Rwanda
              </p>
            </div>
          </div>
        )}

        {/* Form panel */}
        <div className="flex-1 bg-white p-10 md:p-[56px_52px] flex flex-col justify-center overflow-y-auto">
          {/* Mobile logo */}
          {isMobile && (
            <div className="flex items-center gap-2.5 mb-9">
              <img 
                src={logoUrl} 
                alt="AUCA Logo" 
                className="w-10 h-10 rounded-full bg-brand-lightBlue p-0.5 object-contain" 
              />
              <div>
                <div className="text-sm font-bold text-brand-charcoal leading-none">AUCA · PRBS</div>
                <div className="text-[11px] text-brand-lightGray">Review Booking System</div>
              </div>
            </div>
          )}

          {step === 'email' ? (
            <div>
              <h2 className="font-serif text-[28px] font-bold text-brand-charcoal mb-2 tracking-[-0.02em] leading-tight">
                Welcome back
              </h2>
              <p className="text-[#7A7069] text-[14.5px] mb-9">
                Enter your university email to receive a one-time passcode.
              </p>
              
              <label className="block text-[13px] font-semibold text-brand-charcoal mb-2 tracking-[0.04em] uppercase">
                Email Address
              </label>
              <input
                type="email" 
                value={email} 
                onChange={e => { setEmail(e.target.value); setErr(''); }}
                onKeyDown={e => e.key === 'Enter' && sendOTP()}
                placeholder="yourname@university.ac.rw"
                className="w-full p-[14px_16px] border-[1.5px] border-brand-darkBeige focus:border-brand-blue rounded-[10px] text-[15px] outline-none bg-[#FAFAF8] transition-colors duration-200 text-brand-charcoal font-sans"
              />
              {err && <p className="text-[#D04040] text-[13px] mt-2">{err}</p>}
              
              <button
                onClick={sendOTP} 
                disabled={loading}
                className={`mt-5 w-full p-[14px] text-white font-semibold rounded-[10px] text-[15px] transition-colors duration-200 font-sans tracking-[-0.01em] ${
                  loading ? 'bg-[#7AAAD8] cursor-default' : 'bg-brand-blue hover:bg-brand-blue/90 cursor-pointer'
                }`}
              >
                {loading ? 'Sending code…' : 'Send One-Time Code →'}
              </button>
              
              <div className="mt-7 p-4 bg-[#F8F5F0] rounded-[10px] border border-brand-darkBeige">
                <p className="m-0 text-[12.5px] text-brand-gray font-medium flex items-center gap-1.5">
                  <svg className="w-3.5 h-3.5 stroke-brand-gray" fill="none" viewBox="0 0 24 24">
                    <circle cx="8" cy="15" r="5" strokeWidth="2" />
                    <path d="M13 10l8-8M21 2l-3 3M17 6l2-2" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                  Demo quick access:
                </p>
                {[
                  ['supervisor@university.ac.rw', 'Supervisor'], 
                  ['admin@university.ac.rw', 'Admin'], 
                  ['alice@university.ac.rw', 'Student']
                ].map(([em, label]) => (
                  <button 
                    key={em} 
                    onClick={() => setEmail(em)} 
                    className="block mt-1.5 bg-none border-none p-0 text-brand-blue text-[13px] underline cursor-pointer font-sans"
                  >
                    {em} <span className="text-[#7A7069] no-underline">({label})</span>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div>
              <button 
                onClick={() => setStep('email')} 
                className="bg-none border-none text-brand-gray text-[13.5px] hover:text-brand-charcoal cursor-pointer mb-7 p-0 flex items-center gap-[6px] font-sans"
              >
                ← Back
              </button>
              
              <h2 className="font-serif text-[28px] font-bold text-brand-charcoal mb-2 leading-tight">
                Check your email
              </h2>
              <p className="text-[#7A7069] text-[14.5px] mb-1.5">
                We sent a 6-digit code to
              </p>
              <p className="text-brand-charcoal text-[15px] font-semibold mb-9">
                {email}
              </p>
              
              <div className="flex gap-[10px] mb-7">
                {otp.map((d, i) => (
                  <input 
                    key={i} 
                    id={`otp-${i}`} 
                    type="text" 
                    inputMode="numeric" 
                    maxLength={1} 
                    value={d}
                    onChange={e => handleOtpChange(i, e.target.value)}
                    onKeyDown={e => handleOtpKey(i, e)}
                    className={`w-[52px] h-[60px] text-center text-2xl font-bold border-[1.5px] outline-none transition-all duration-150 font-sans rounded-[10px] ${
                      d ? 'border-brand-blue bg-brand-lightBlue text-brand-blue' : 'border-brand-darkBeige bg-[#FAFAF8] text-brand-charcoal'
                    }`}
                  />
                ))}
              </div>
              {err && <p className="text-[#D04040] text-[13px] mb-3.5">{err}</p>}
              
              <button 
                onClick={verifyOTP} 
                disabled={!filled || loading}
                className={`w-full p-[14px] text-white font-semibold rounded-[10px] text-[15px] transition-colors duration-150 font-sans ${
                  filled && !loading ? 'bg-brand-blue hover:bg-brand-blue/90 cursor-pointer' : 'bg-[#A5BFE0] cursor-default'
                }`}
              >
                {loading ? 'Verifying…' : 'Verify & Sign In →'}
              </button>
              <p className="mt-4 text-[13px] text-brand-gray">
                Use the 6-digit code sent by the backend email service.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
