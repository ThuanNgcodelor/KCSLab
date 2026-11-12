import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { useAuth } from '../features/auth/hooks/useAuth';
import { UsersPage } from '../features/users/pages/UsersPage';

function ProtectedRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="page-loading">Đang khởi tạo phiên…</div>;
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function AdminRoute() {
  const { user } = useAuth();
  return user?.role === 'ADMIN' ? <Outlet /> : <Navigate to="/login" replace />;
}

export function App() {
  return <Routes><Route path="/login" element={<LoginPage />} /><Route element={<ProtectedRoute />}><Route element={<AdminRoute />}><Route path="/users" element={<UsersPage />} /></Route></Route><Route path="*" element={<Navigate to="/users" replace />} /></Routes>;
}
