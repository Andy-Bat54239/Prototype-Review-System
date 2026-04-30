import logoUrl from '../assets/logo.png';

const ICONS = {
  calendar: <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="1.8"/><path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>,
  list:     <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>,
  chart:    <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><path d="M18 20V10M12 20V4M6 20v-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>,
  settings: <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.8"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" stroke="currentColor" strokeWidth="1.8"/></svg>,
  users:    <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/><circle cx="9" cy="7" r="4" stroke="currentColor" strokeWidth="1.8"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>,
  avail:    <svg width="18" height="18" fill="none" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="1.8"/><path d="M12 6v6l4 2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/></svg>,
  logout:   <svg width="16" height="16" fill="none" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>,
};

const NAV_BY_ROLE = {
  student: [
    { id: 'calendar',  label: 'Book a Session',   icon: 'calendar' },
    { id: 'mybooking', label: 'My Booking',        icon: 'list'     },
  ],
  supervisor: [
    { id: 'sessions',     label: "Today's Sessions", icon: 'list'     },
    { id: 'calendar',     label: 'Calendar',          icon: 'calendar' },
    { id: 'availability', label: 'Availability',       icon: 'avail'    },
    { id: 'analytics',    label: 'Analytics',          icon: 'chart'    },
  ],
  admin: [
    { id: 'users',    label: 'User Management', icon: 'users'    },
    { id: 'settings', label: 'Global Settings', icon: 'settings' },
  ],
};

const ROLE_LABELS = { student: 'Student', supervisor: 'Supervisor', admin: 'Administrator' };

export default function Sidebar({ user, activeTab, setActiveTab, notifCount, onLogout, isMobile, isOpen, onClose }) {
  const nav = NAV_BY_ROLE[user.role] || [];
  const initials = user.email.slice(0, 2).toUpperCase();

  const sidebarStyle = isMobile
    ? {
        position: 'fixed', top: 0, left: 0, height: '100vh', zIndex: 200,
        width: 240,
        transform: isOpen ? 'translateX(0)' : 'translateX(-100%)',
        transition: 'transform 0.25s ease',
        background: 'linear-gradient(180deg, #0F2755 0%, #0D1F45 100%)',
        display: 'flex', flexDirection: 'column', flexShrink: 0,
      }
    : {
        width: 240,
        background: 'linear-gradient(180deg, #0F2755 0%, #0D1F45 100%)',
        display: 'flex', flexDirection: 'column', height: '100vh', flexShrink: 0,
      };

  return (
    <>
      {isMobile && isOpen && (
        <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(13,31,69,0.5)', zIndex: 199 }} />
      )}
      <div style={sidebarStyle}>
        <div style={{ padding: '28px 24px 24px', borderBottom: '1px solid rgba(255,255,255,0.07)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <img src={logoUrl} alt="AUCA" style={{ width: 38, height: 38, borderRadius: '50%', background: 'white', padding: 2, objectFit: 'contain', flexShrink: 0 }} />
            <div>
              <div style={{ color: 'white', fontSize: 13, fontWeight: 700, letterSpacing: '-0.01em', lineHeight: 1.2 }}>AUCA · PRBS</div>
              <div style={{ color: 'rgba(255,255,255,0.35)', fontSize: 10.5, lineHeight: 1.3 }}>Review Booking System</div>
            </div>
          </div>
          {isMobile && (
            <button onClick={onClose}
              style={{ background: 'rgba(255,255,255,0.1)', border: 'none', borderRadius: '50%', width: 28, height: 28, cursor: 'pointer', color: 'white', fontSize: 14, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              ✕
            </button>
          )}
        </div>

        <nav style={{ flex: 1, padding: '16px 12px', overflowY: 'auto' }}>
          <div style={{ color: 'rgba(255,255,255,0.28)', fontSize: 10.5, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', padding: '0 12px', marginBottom: 8 }}>
            {ROLE_LABELS[user.role]}
          </div>
          {nav.map(item => {
            const active = activeTab === item.id;
            return (
              <button key={item.id} onClick={() => setActiveTab(item.id)}
                style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', borderRadius: 8, border: 'none', cursor: 'pointer', background: active ? 'rgba(29,91,175,0.35)' : 'transparent', color: active ? '#7AAADE' : 'rgba(255,255,255,0.55)', fontSize: 14, fontWeight: active ? 600 : 400, fontFamily: 'DM Sans, sans-serif', transition: 'all 0.15s', textAlign: 'left', marginBottom: 2, position: 'relative' }}>
                <span style={{ color: active ? '#7AAADE' : 'rgba(255,255,255,0.4)' }}>{ICONS[item.icon]}</span>
                {item.label}
                {item.id === 'sessions' && notifCount > 0 && (
                  <span style={{ marginLeft: 'auto', background: '#7AAADE', color: '#0D1F45', fontSize: 11, fontWeight: 700, borderRadius: 10, padding: '2px 7px', minWidth: 20, textAlign: 'center' }}>{notifCount}</span>
                )}
              </button>
            );
          })}
        </nav>

        <div style={{ padding: '16px 12px', borderTop: '1px solid rgba(255,255,255,0.07)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', marginBottom: 4 }}>
            <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'rgba(29,91,175,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: 12, fontWeight: 700, flexShrink: 0 }}>{initials}</div>
            <div style={{ overflow: 'hidden' }}>
              <div style={{ color: 'rgba(255,255,255,0.85)', fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{user.email}</div>
              <div style={{ color: 'rgba(255,255,255,0.35)', fontSize: 11.5 }}>{ROLE_LABELS[user.role]}</div>
            </div>
          </div>
          <button onClick={onLogout}
            style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 8, padding: '9px 12px', borderRadius: 8, border: 'none', background: 'transparent', color: 'rgba(255,255,255,0.35)', fontSize: 13, cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', transition: 'color 0.15s' }}
            onMouseEnter={e => e.currentTarget.style.color = 'rgba(255,255,255,0.7)'}
            onMouseLeave={e => e.currentTarget.style.color = 'rgba(255,255,255,0.35)'}>
            {ICONS.logout} Sign out
          </button>
        </div>
      </div>
    </>
  );
}
