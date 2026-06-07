import { useEffect, useState } from 'react';
import { useBreakpoint } from '../hooks/useBreakpoint';
import { adaptUser, api } from '../api';
import { User, Settings } from '../types';

interface AdminPanelProps {
  activeTab: string;
  setActiveTab?: (tab: string) => void;
}

export default function AdminPanel({ activeTab }: AdminPanelProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [settings, setSettings] = useState<Settings>({ otpExpiry: 10, cancelWindow: 60, reminderTime: 30 });
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const { isMobile } = useBreakpoint();

  useEffect(() => {
    let alive = true;
    async function loadAdminData() {
      setLoading(true);
      setErr('');
      try {
        const [userRows, settingsRow] = await Promise.all([api.users(), api.settings()]);
        if (!alive) return;
        setUsers(userRows.map(adaptUser));
        setSettings(settingsRow);
      } catch (e: any) {
        if (alive) setErr(e.message);
      } finally {
        if (alive) setLoading(false);
      }
    }
    loadAdminData();
    return () => { alive = false; };
  }, []);

  const toggleStatus = async (id: number) => {
    const current = users.find(u => u.id === id);
    if (!current) return;
    const nextStatus = current.status === 'active' ? 'INACTIVE' : 'ACTIVE';
    const savedUser = await api.updateUserStatus(id, nextStatus);
    setUsers(prev => prev.map(u => u.id === id ? adaptUser(savedUser) : u));
  };
  
  const setS = (k: keyof Settings, v: number) => setSettings(s => ({ ...s, [k]: v }));

  const handleSave = async () => {
    setErr('');
    try {
      const savedSettings = await api.updateSettings(settings);
      setSettings(savedSettings);
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const roleColors: Record<string, [string, string]> = { 
    student: ['bg-brand-lightBlue', 'text-brand-blue'], 
    supervisor: ['bg-[#E8EEF4]', 'text-[#1A4F7A]'], 
    admin: ['bg-[#F4ECE6]', 'text-[#7A3B0B]'] 
  };

  if (loading) return <div className="text-brand-gray text-[15px] font-sans">Loading admin workspace...</div>;
  if (err) return <div className="bg-white rounded-xl p-6 border border-brand-darkBeige text-[#D04040] font-sans">{err}</div>;

  if (activeTab === 'settings') {
    return (
      <div className="max-w-[580px] font-sans">
        <h2 className="font-serif text-[26px] text-brand-charcoal mb-2">Global Settings</h2>
        <p className="text-brand-gray text-[14.5px] mb-7">Configure system-wide parameters for the PRBS platform.</p>
        <div className="bg-white rounded-[14px] p-5 md:p-8 border border-brand-darkBeige shadow-sm">
          {[
            { key: 'otpExpiry',     label: 'OTP Expiry',            desc: 'How long a one-time passcode remains valid before expiring.',          unit: 'minutes', min: 1,  max: 60   },
            { key: 'cancelWindow',  label: 'Cancellation Window',   desc: 'Minimum notice required for a student to cancel a booking.',           unit: 'minutes', min: 1,  max: 1440 },
            { key: 'reminderTime',  label: 'Reminder Time',         desc: 'How early to send session reminder notifications.',                    unit: 'minutes', min: 5,  max: 120  },
          ].map(({ key, label, desc, unit, min, max }) => (
            <div key={key} className="mb-7 pb-7 border-b border-[#F8F5F0] last:border-b-0 last:pb-0">
              <div className="flex justify-between items-start mb-2.5 flex-wrap gap-3">
                <div className="flex-1 min-w-0">
                  <div className="text-[15px] font-bold text-brand-charcoal mb-0.5">{label}</div>
                  <div className="text-[13px] text-brand-gray max-w-[360px]">{desc}</div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <input 
                    type="number" 
                    min={min} 
                    max={max} 
                    value={settings[key as keyof Settings]}
                    onChange={e => setS(key as keyof Settings, parseInt(e.target.value) || 0)}
                    className="w-[72px] p-[8px_10px] border border-brand-darkBeige focus:border-brand-blue rounded-lg text-[15px] font-bold text-center font-sans text-brand-charcoal outline-none"
                  />
                  <span className="text-[13px] text-brand-gray whitespace-nowrap">{unit}</span>
                </div>
              </div>
              <input 
                type="range" 
                min={min} 
                max={max} 
                value={settings[key as keyof Settings]}
                onChange={e => setS(key as keyof Settings, parseInt(e.target.value))}
                className="w-full accent-brand-blue cursor-pointer" 
              />
              <div className="flex justify-between text-[11px] text-brand-lightGray mt-1">
                <span>{min} min</span><span>{max} min</span>
              </div>
            </div>
          ))}
          <button 
            onClick={handleSave}
            className={`text-white border-none rounded-xl p-[12px_28px] text-[14.5px] font-semibold cursor-pointer font-sans transition-colors duration-200 mt-2 ${
              saved ? 'bg-[#2E7D32] hover:bg-[#2E7D32]/90' : 'bg-brand-blue hover:bg-brand-blue/90'
            }`}
          >
            {saved ? '✓ Settings Saved' : 'Save Changes'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="font-sans">
      <div className={`flex justify-between gap-4 mb-6 ${isMobile ? 'flex-col items-start' : 'flex-row items-end'}`}>
        <div>
          <h2 className="font-serif text-[26px] text-brand-charcoal mb-1">User Management</h2>
          <p className="text-brand-gray text-[14.5px]">{users.length} users registered · {users.filter(u => u.status === 'active').length} active</p>
        </div>
        <div className="flex gap-2 flex-wrap">
          {['All', 'Student', 'Supervisor', 'Admin'].map(role => (
            <span key={role} className="text-[12.5px] text-brand-gray p-[5px_14px] rounded-full border border-brand-darkBeige bg-white cursor-pointer hover:bg-brand-beige">
              {role}
            </span>
          ))}
        </div>
      </div>

      {isMobile ? (
        /* Card layout on mobile */
        <div className="flex flex-col gap-3">
          {users.map(user => {
            const [roleBg, roleColor] = roleColors[user.role] || ['bg-gray-100', 'text-gray-600'];
            return (
              <div key={user.id} className="bg-white rounded-xl p-[16px_18px] border border-brand-darkBeige shadow-sm">
                <div className="flex items-center gap-3 mb-3">
                  <div className={`w-10 h-10 rounded-full ${roleBg} ${roleColor} flex items-center justify-center text-sm font-bold shrink-0`}>
                    {user.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-[14.5px] font-bold text-brand-charcoal">{user.name}</div>
                    <div className="text-[12.5px] text-brand-gray overflow-hidden text-ellipsis whitespace-nowrap">{user.email}</div>
                  </div>
                  <span className={`inline-flex items-center gap-1 text-[12.5px] font-semibold shrink-0 ${
                    user.status === 'active' ? 'text-[#2E7D32]' : 'text-[#C62828]'
                  }`}>
                    <span className={`w-1.5 h-1.5 rounded-full inline-block ${
                      user.status === 'active' ? 'bg-[#2E7D32]' : 'bg-[#C62828]'
                    }`} />
                    {user.status.charAt(0).toUpperCase() + user.status.slice(1)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className={`text-[11.5px] font-bold px-3 py-1 rounded-full uppercase tracking-wider ${roleBg} ${roleColor}`}>
                    {user.role}
                  </span>
                  <button 
                    onClick={() => toggleStatus(user.id)}
                    className={`border-none rounded-lg p-[5px_12px] text-xs font-semibold cursor-pointer font-sans ${
                      user.status === 'active' ? 'bg-[#FDE8E8] text-[#C62828] hover:bg-[#fad4d4]' : 'bg-[#E8F4E8] text-[#2E7D32] hover:bg-[#dceddc]'
                    }`}
                  >
                    {user.status === 'active' ? 'Deactivate' : 'Activate'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table layout on desktop */
        <div className="bg-white rounded-[14px] border border-brand-darkBeige overflow-hidden shadow-sm">
          <table className="w-full border-collapse">
            <thead>
              <tr className="bg-[#F8F5F0]">
                {['Name', 'Email', 'Role', 'Status', 'Actions'].map((h, i) => (
                  <th key={h} className={`p-[14px_20px] text-[11.5px] font-bold text-brand-gray tracking-wider uppercase border-b border-brand-darkBeige ${
                    i === 4 ? 'text-right' : 'text-left'
                  }`}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {users.map((user, idx) => {
                const [roleBg, roleColor] = roleColors[user.role] || ['bg-gray-100', 'text-gray-600'];
                return (
                  <tr 
                    key={user.id}
                    className={`border-b hover:bg-[#FAFAF8] transition-colors ${
                      idx < users.length - 1 ? 'border-b-[#F8F5F0]' : 'border-b-0'
                    }`}
                  >
                    <td className="p-[14px_20px]">
                      <div className="flex items-center gap-2.5">
                        <div className={`w-[34px] h-[34px] rounded-full ${roleBg} ${roleColor} flex items-center justify-center text-[12.5px] font-bold shrink-0`}>
                          {user.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                        </div>
                        <span className="text-sm font-semibold text-brand-charcoal">{user.name}</span>
                      </div>
                    </td>
                    <td className="p-[14px_20px] text-[13.5px] text-brand-gray">{user.email}</td>
                    <td className="p-[14px_20px]">
                      <span className={`text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider ${roleBg} ${roleColor}`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="p-[14px_20px]">
                      <span className={`inline-flex items-center gap-1.5 text-sm font-semibold ${
                        user.status === 'active' ? 'text-[#2E7D32]' : 'text-[#C62828]'
                      }`}>
                        <span className={`w-1.5 h-1.5 rounded-full inline-block ${
                          user.status === 'active' ? 'bg-[#2E7D32]' : 'bg-[#C62828]'
                        }`} />
                        {user.status.charAt(0).toUpperCase() + user.status.slice(1)}
                      </span>
                    </td>
                    <td className="p-[14px_20px] text-right">
                      <button 
                        onClick={() => toggleStatus(user.id)}
                        className={`border-none rounded-lg p-[5px_12px] text-[12.5px] font-semibold cursor-pointer font-sans ${
                          user.status === 'active' ? 'bg-[#FDE8E8] text-[#C62828] hover:bg-[#fad4d4]' : 'bg-[#E8F4E8] text-[#2E7D32] hover:bg-[#dceddc]'
                        }`}
                      >
                        {user.status === 'active' ? 'Deactivate' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
