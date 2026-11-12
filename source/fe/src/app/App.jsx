import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { useAuth } from '../features/auth/hooks/useAuth';
import { UsersPage } from '../features/users/pages/UsersPage';
import { UserFormPage } from '../features/users/pages/UserFormPage';
import { AdminLayout } from '../features/admin/components/AdminLayout';
import { DashboardPage } from '../features/admin/pages/DashboardPage';

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
  return <Routes><Route path="/login" element={<LoginPage />} /><Route element={<ProtectedRoute />}><Route element={<AdminRoute />}><Route element={<AdminLayout />}><Route path="/admin" element={<DashboardPage />} /><Route path="/admin/users" element={<UsersPage />} /><Route path="/admin/users/new" element={<UserFormPage />} /><Route path="/admin/users/:id/edit" element={<UserFormPage />} /><Route path="/users" element={<Navigate to="/admin/users" replace />} /></Route></Route></Route><Route path="*" element={<Navigate to="/admin" replace />} /></Routes>;
}
