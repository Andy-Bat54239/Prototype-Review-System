import { useState } from 'react';
import { MOCK_USERS, MOCK_SETTINGS_INIT } from '../data';
import { useBreakpoint } from '../hooks/useBreakpoint';

export default function AdminPanel({ activeTab }) {
  const [users, setUsers] = useState(() => MOCK_USERS.map(u => ({ ...u, otpReset: false })));
  const [settings, setSettings] = useState({ ...MOCK_SETTINGS_INIT });
  const [saved, setSaved] = useState(false);
  const { isMobile } = useBreakpoint();

  const toggleStatus = (id) => setUsers(prev => prev.map(u => u.id === id ? { ...u, status: u.status === 'Active' ? 'Inactive' : 'Active' } : u));
  const resetOtp = (id) => setUsers(prev => prev.map(u => u.id === id ? { ...u, otpReset: true } : u));
  const setS = (k, v) => setSettings(s => ({ ...s, [k]: v }));

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const roleColors = { Student: ['#E5EDF8', '#1D5BAF'], Supervisor: ['#E8EEF4', '#1A4F7A'], Admin: ['#F4ECE6', '#7A3B0B'] };

  if (activeTab === 'settings') {
    return (
      <div style={{ maxWidth: 580 }}>
        <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 8px' }}>Global Settings</h2>
        <p style={{ color: '#7A7069', fontSize: 14.5, margin: '0 0 28px' }}>Configure system-wide parameters for the PRBS platform.</p>
        <div style={{ background: 'white', borderRadius: 14, padding: isMobile ? 20 : 32, border: '1px solid #EDE9E2', boxShadow: '0 2px 8px rgba(28,24,20,0.05)' }}>
          {[
            { key: 'otpExpiry',     label: 'OTP Expiry',            desc: 'How long a one-time passcode remains valid before expiring.',          unit: 'minutes', min: 1,  max: 60   },
            { key: 'cancelWindow',  label: 'Cancellation Window',   desc: 'Minimum notice required for a student to cancel a booking.',           unit: 'minutes', min: 0,  max: 1440 },
            { key: 'reminderTime',  label: 'Reminder Time',         desc: 'How early to send session reminder notifications.',                    unit: 'minutes', min: 5,  max: 120  },
          ].map(({ key, label, desc, unit, min, max }) => (
            <div key={key} style={{ marginBottom: 28, paddingBottom: 28, borderBottom: '1px solid #F8F5F0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10, flexWrap: 'wrap', gap: 12 }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 700, color: '#1C1814', marginBottom: 3 }}>{label}</div>
                  <div style={{ fontSize: 13, color: '#7A7069', maxWidth: 360 }}>{desc}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                  <input type="number" min={min} max={max} value={settings[key]}
                    onChange={e => setS(key, parseInt(e.target.value) || 0)}
                    style={{ width: 72, padding: '8px 10px', border: '1.5px solid #EDE9E2', borderRadius: 8, fontSize: 15, fontWeight: 700, textAlign: 'center', fontFamily: 'DM Sans, sans-serif', color: '#1C1814', outline: 'none' }}
                    onFocus={e => e.target.style.borderColor = '#1D5BAF'}
                    onBlur={e => e.target.style.borderColor = '#EDE9E2'} />
                  <span style={{ fontSize: 13, color: '#7A7069', whiteSpace: 'nowrap' }}>{unit}</span>
                </div>
              </div>
              <input type="range" min={min} max={max} value={settings[key]}
                onChange={e => setS(key, parseInt(e.target.value))}
                style={{ width: '100%', accentColor: '#1D5BAF', cursor: 'pointer' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#C5BDB5', marginTop: 3 }}>
                <span>{min} min</span><span>{max} min</span>
              </div>
            </div>
          ))}
          <button onClick={handleSave}
            style={{ background: saved ? '#2E7D32' : '#1D5BAF', color: 'white', border: 'none', borderRadius: 10, padding: '12px 28px', fontSize: 14.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'background 0.2s' }}>
            {saved ? '✓ Settings Saved' : 'Save Changes'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'flex-end', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? 16 : 0, marginBottom: 24 }}>
        <div>
          <h2 style={{ fontFamily: 'Playfair Display, serif', fontSize: 26, color: '#1C1814', margin: '0 0 4px' }}>User Management</h2>
          <p style={{ color: '#7A7069', fontSize: 14.5, margin: 0 }}>{users.length} users registered · {users.filter(u => u.status === 'Active').length} active</p>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {['All', 'Student', 'Supervisor', 'Admin'].map(role => (
            <span key={role} style={{ fontSize: 12.5, color: '#7A7069', padding: '5px 14px', borderRadius: 20, border: '1px solid #EDE9E2', background: 'white', cursor: 'pointer' }}>{role}</span>
          ))}
        </div>
      </div>

      {isMobile ? (
        /* Card layout on mobile */
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {users.map(user => {
            const [roleBg, roleColor] = roleColors[user.role] || ['#F5F5F5', '#666'];
            return (
              <div key={user.id} style={{ background: 'white', borderRadius: 12, padding: '16px 18px', border: '1px solid #EDE9E2', boxShadow: '0 1px 6px rgba(28,24,20,0.05)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                  <div style={{ width: 40, height: 40, borderRadius: '50%', background: roleBg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: roleColor, flexShrink: 0 }}>
                    {user.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14.5, fontWeight: 700, color: '#1C1814' }}>{user.name}</div>
                    <div style={{ fontSize: 12.5, color: '#7A7069', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{user.email}</div>
                  </div>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 12.5, fontWeight: 600, color: user.status === 'Active' ? '#2E7D32' : '#C62828', flexShrink: 0 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: user.status === 'Active' ? '#2E7D32' : '#C62828', display: 'inline-block' }} />
                    {user.status}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ background: roleBg, color: roleColor, fontSize: 11.5, fontWeight: 700, padding: '3px 12px', borderRadius: 20 }}>{user.role}</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button onClick={() => resetOtp(user.id)}
                      style={{ background: user.otpReset ? '#E8F4E8' : '#F8F5F0', color: user.otpReset ? '#2E7D32' : '#7A7069', border: '1px solid #EDE9E2', borderRadius: 7, padding: '5px 12px', fontSize: 12, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                      {user.otpReset ? '✓ Reset' : 'Reset OTP'}
                    </button>
                    <button onClick={() => toggleStatus(user.id)}
                      style={{ background: user.status === 'Active' ? '#FDE8E8' : '#E8F4E8', color: user.status === 'Active' ? '#C62828' : '#2E7D32', border: 'none', borderRadius: 7, padding: '5px 12px', fontSize: 12, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                      {user.status === 'Active' ? 'Deactivate' : 'Activate'}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table layout on desktop */
        <div style={{ background: 'white', borderRadius: 14, border: '1px solid #EDE9E2', overflow: 'hidden', boxShadow: '0 2px 8px rgba(28,24,20,0.05)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: '#F8F5F0' }}>
                {['Name', 'Email', 'Role', 'Status', 'Actions'].map((h, i) => (
                  <th key={h} style={{ padding: '14px 20px', textAlign: i === 4 ? 'right' : 'left', fontSize: 11.5, fontWeight: 700, color: '#7A7069', letterSpacing: '0.06em', textTransform: 'uppercase', borderBottom: '1px solid #EDE9E2' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {users.map((user, idx) => {
                const [roleBg, roleColor] = roleColors[user.role] || ['#F5F5F5', '#666'];
                return (
                  <tr key={user.id}
                    style={{ borderBottom: idx < users.length - 1 ? '1px solid #F8F5F0' : 'none', transition: 'background 0.1s' }}
                    onMouseEnter={e => e.currentTarget.style.background = '#FAFAF8'}
                    onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                    <td style={{ padding: '14px 20px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ width: 34, height: 34, borderRadius: '50%', background: roleBg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12.5, fontWeight: 700, color: roleColor, flexShrink: 0 }}>
                          {user.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                        </div>
                        <span style={{ fontSize: 14, fontWeight: 600, color: '#1C1814' }}>{user.name}</span>
                      </div>
                    </td>
                    <td style={{ padding: '14px 20px', fontSize: 13.5, color: '#7A7069' }}>{user.email}</td>
                    <td style={{ padding: '14px 20px' }}>
                      <span style={{ background: roleBg, color: roleColor, fontSize: 12, fontWeight: 700, padding: '3px 12px', borderRadius: 20 }}>{user.role}</span>
                    </td>
                    <td style={{ padding: '14px 20px' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 13, fontWeight: 600, color: user.status === 'Active' ? '#2E7D32' : '#C62828' }}>
                        <span style={{ width: 7, height: 7, borderRadius: '50%', background: user.status === 'Active' ? '#2E7D32' : '#C62828', display: 'inline-block' }} />
                        {user.status}
                      </span>
                    </td>
                    <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                        <button onClick={() => resetOtp(user.id)}
                          style={{ background: user.otpReset ? '#E8F4E8' : '#F8F5F0', color: user.otpReset ? '#2E7D32' : '#7A7069', border: '1px solid #EDE9E2', borderRadius: 7, padding: '5px 12px', fontSize: 12.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                          {user.otpReset ? '✓ OTP Reset' : 'Reset OTP'}
                        </button>
                        <button onClick={() => toggleStatus(user.id)}
                          style={{ background: user.status === 'Active' ? '#FDE8E8' : '#E8F4E8', color: user.status === 'Active' ? '#C62828' : '#2E7D32', border: 'none', borderRadius: 7, padding: '5px 12px', fontSize: 12.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                          {user.status === 'Active' ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
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
