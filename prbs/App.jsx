
// ─── Root App ─────────────────────────────────────────────────────
const { useState: useAppState, useEffect: useAppEffect } = React;

const DEFAULT_TABS = { student: 'calendar', supervisor: 'sessions', admin: 'users' };

function App() {
  const [user, setUser] = useAppState(null);
  const [activeTab, setActiveTab] = useAppState('calendar');
  const [bookings, setBookings] = useAppState([...MOCK_BOOKINGS_INIT]);
  const [availability, setAvailability] = useAppState([...MOCK_AVAILABILITY]);
  const [showRoleSwitcher, setShowRoleSwitcher] = useAppState(false);

  const handleLogin = (userData) => {
    setUser(userData);
    setActiveTab(DEFAULT_TABS[userData.role] || 'calendar');
  };

  const handleLogout = () => {
    setUser(null);
    setBookings([...MOCK_BOOKINGS_INIT]);
  };

  const switchRole = (role) => {
    const emails = { student: 'alice@university.ac.rw', supervisor: 'supervisor@university.ac.rw', admin: 'admin@university.ac.rw' };
    setUser({ email: emails[role], role });
    setActiveTab(DEFAULT_TABS[role]);
    setShowRoleSwitcher(false);
  };

  const handleSetTab = (tab) => setActiveTab(tab);

  const notifCount = bookings.filter(b => b.status === 'confirmed').length;

  if (!user) return <Login onLogin={handleLogin} />;

  const renderDashboard = () => {
    if (user.role === 'student') return <StudentDashboard activeTab={activeTab} setActiveTab={handleSetTab} bookings={bookings} setBookings={setBookings} availability={availability} />;
    if (user.role === 'supervisor') return <SupervisorDashboard activeTab={activeTab} setActiveTab={handleSetTab} bookings={bookings} setBookings={setBookings} availability={availability} setAvailability={setAvailability} />;
    if (user.role === 'admin') return <AdminPanel activeTab={activeTab} />;
    return null;
  };

  return (
    <div style={{ display: 'flex', height: '100vh', fontFamily: 'DM Sans, system-ui, sans-serif', overflow: 'hidden' }}>
      <Sidebar user={user} activeTab={activeTab} setActiveTab={handleSetTab} notifCount={notifCount} onLogout={handleLogout} />

      {/* Main content */}
      <div style={{ flex: 1, overflow: 'auto', background: '#F8F5F0' }}>
        {/* Top bar */}
        <div style={{ background: 'white', borderBottom: '1px solid #EDE9E2', padding: '0 36px', height: 60, display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 10 }}>
          <div style={{ fontSize: 13, color: '#B8AFA2', fontWeight: 500 }}>
            {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ position: 'relative' }}>
              <button onClick={() => setShowRoleSwitcher(s => !s)}
                style={{ display: 'flex', alignItems: 'center', gap: 8, background: '#F8F5F0', border: '1px solid #EDE9E2', borderRadius: 8, padding: '6px 14px', fontSize: 13, fontWeight: 600, color: '#1C1814', cursor: 'pointer', fontFamily: 'DM Sans, sans-serif' }}>
                <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#1D5BAF', display: 'inline-block' }} />
                Switch Role ↕
              </button>
              {showRoleSwitcher && (
                <div style={{ position: 'absolute', right: 0, top: '110%', background: 'white', border: '1px solid #EDE9E2', borderRadius: 10, boxShadow: '0 8px 24px rgba(28,24,20,0.12)', padding: 8, minWidth: 180, zIndex: 100 }}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: '#B8AFA2', textTransform: 'uppercase', letterSpacing: '0.06em', padding: '4px 12px 8px' }}>Demo: Switch Role</div>
                  {[['student','Student','alice@university.ac.rw'], ['supervisor','Supervisor','supervisor@...'], ['admin','Admin','admin@...']].map(([role, label, email]) => (
                    <button key={role} onClick={() => switchRole(role)}
                      style={{ width: '100%', display: 'flex', flexDirection: 'column', padding: '8px 12px', border: 'none', borderRadius: 7, background: user.role === role ? '#E5EDF8' : 'transparent', cursor: 'pointer', fontFamily: 'DM Sans, sans-serif', textAlign: 'left', marginBottom: 2 }}>
                      <span style={{ fontSize: 13.5, fontWeight: 700, color: user.role === role ? '#1D5BAF' : '#1C1814' }}>{label}</span>
                      <span style={{ fontSize: 11.5, color: '#B8AFA2' }}>{email}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Page content */}
        <div style={{ padding: '36px 36px', maxWidth: 1100 }}>
          {renderDashboard()}
        </div>
      </div>

      {/* Click-outside to close role switcher */}
      {showRoleSwitcher && <div style={{ position: 'fixed', inset: 0, zIndex: 9 }} onClick={() => setShowRoleSwitcher(false)} />}
    </div>
  );
}

const rootEl = document.getElementById('root');
ReactDOM.createRoot(rootEl).render(<App />);
