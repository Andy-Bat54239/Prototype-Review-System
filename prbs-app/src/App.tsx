import { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import Login from './components/Login';
import Sidebar from './components/Sidebar';
import StudentDashboard from './components/StudentDashboard';
import SupervisorDashboard from './components/SupervisorDashboard';
import AdminPanel from './components/AdminPanel';
import { useBreakpoint } from './hooks/useBreakpoint';
import { adaptAvailability, adaptBooking, api, clearSession, getStoredUser } from './api';
import { User, Booking, Availability } from './types';

const DEFAULT_TABS: Record<string, string> = { student: 'calendar', supervisor: 'sessions', admin: 'users' };

export default function App() {
  const [user, setUser] = useState<User | null>(() => getStoredUser());
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [availability, setAvailability] = useState<Availability[]>([]);
  const [dataLoading, setDataLoading] = useState(false);
  const [dataError, setDataError] = useState('');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { isMobile } = useBreakpoint();
  const navigate = useNavigate();
  const location = useLocation();

  const loadWorkspaceData = async () => {
    if (!user) return;
    setDataLoading(true);
    setDataError('');
    try {
      const [bookingRows, availabilityRows] = await Promise.all([
        api.bookings(),
        api.availability(),
      ]);
      setBookings(bookingRows.map(adaptBooking));
      setAvailability(availabilityRows.map(adaptAvailability));
    } catch (e: any) {
      setDataError(e.message);
      if (e.message.includes('session expired')) {
        clearSession();
        setUser(null);
        navigate('/login');
      }
    } finally {
      setDataLoading(false);
    }
  };

  useEffect(() => {
    loadWorkspaceData();
  }, [user?.id]);

  useEffect(() => {
    if (!user) {
      if (location.pathname !== '/login') {
        navigate('/login');
      }
    } else {
      if (location.pathname === '/login' || location.pathname === '/') {
        const defaultTab = DEFAULT_TABS[user.role] || 'calendar';
        navigate(`/${user.role}/${defaultTab}`);
      }
    }
  }, [user, location.pathname]);

  const handleLogin = (userData: User) => {
    setUser(userData);
    const defaultTab = DEFAULT_TABS[userData.role] || 'calendar';
    navigate(`/${userData.role}/${defaultTab}`);
  };

  const handleLogout = async () => {
    await api.logout();
    setUser(null);
    setBookings([]);
    setAvailability([]);
    navigate('/login');
  };

  const notifCount = bookings.filter(b => b.status === 'confirmed').length;

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={handleLogin} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  const pathParts = location.pathname.split('/');
  const activeTab = pathParts[2] || DEFAULT_TABS[user.role];

  const handleTabChange = (tab: string) => {
    navigate(`/${user.role}/${tab}`);
    setSidebarOpen(false);
  };

  const renderDashboard = () => {
    const shared = { dataLoading, dataError, onRefresh: loadWorkspaceData };
    if (user.role === 'student') {
      return (
        <StudentDashboard
          activeTab={activeTab}
          bookings={bookings}
          setBookings={setBookings}
          availability={availability}
          {...shared}
        />
      );
    }
    if (user.role === 'supervisor') {
      return (
        <SupervisorDashboard
          activeTab={activeTab}
          setActiveTab={handleTabChange}
          bookings={bookings}
          setBookings={setBookings}
          availability={availability}
          setAvailability={setAvailability}
          {...shared}
        />
      );
    }
    if (user.role === 'admin') {
      return (
        <AdminPanel
          activeTab={activeTab}
          setActiveTab={handleTabChange}
        />
      );
    }
    return null;
  };

  return (
    <div style={{ display: 'flex', height: '100vh', fontFamily: 'DM Sans, system-ui, sans-serif', overflow: 'hidden' }}>
      <Sidebar
        user={user}
        activeTab={activeTab}
        setActiveTab={handleTabChange}
        notifCount={notifCount}
        onLogout={handleLogout}
        isMobile={isMobile}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
      />

      <div style={{ flex: 1, overflow: 'auto', background: '#F8F5F0', minWidth: 0 }}>
        {/* Top bar */}
        <div style={{
          background: 'white',
          borderBottom: '1px solid #EDE9E2',
          padding: isMobile ? '0 16px' : '0 36px',
          height: 60,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}>
          {isMobile ? (
            <button onClick={() => setSidebarOpen(true)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#1C1814', padding: 4, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="22" height="22" fill="none" viewBox="0 0 24 24"><path d="M3 12h18M3 6h18M3 18h18" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
            </button>
          ) : (
            <div style={{ fontSize: 13, color: '#B8AFA2', fontWeight: 500 }}>
              {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
            </div>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#F8F5F0', border: '1px solid #EDE9E2', borderRadius: 8, padding: isMobile ? '6px 10px' : '6px 14px', fontSize: 13, fontWeight: 600, color: '#1C1814', fontFamily: 'DM Sans, sans-serif' }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#1D5BAF', display: 'inline-block' }} />
              <span>{isMobile ? user.role : `${user.name || user.email} · ${user.role}`}</span>
          </div>
        </div>

        <div style={{ padding: isMobile ? '20px 16px' : '36px', maxWidth: 1100 }}>
          <Routes>
            <Route path="/student/:tab" element={renderDashboard()} />
            <Route path="/supervisor/:tab" element={renderDashboard()} />
            <Route path="/admin/:tab" element={renderDashboard()} />
            <Route path="*" element={<Navigate to={`/${user.role}/${DEFAULT_TABS[user.role]}`} replace />} />
          </Routes>
        </div>
      </div>
    </div>
  );
}
