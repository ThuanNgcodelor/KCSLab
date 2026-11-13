import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { useAuth } from '../features/auth/hooks/useAuth';
import { UsersPage } from '../features/users/pages/UsersPage';
import { UserFormPage } from '../features/users/pages/UserFormPage';
import { IntakePage } from '../features/intake/pages/IntakePage';
import { ReceiptCreatePage } from '../features/intake/pages/ReceiptCreatePage';
import { AdminLayout } from '../features/admin/components/AdminLayout';
import { DashboardPage } from '../features/admin/pages/DashboardPage';
import { TransfersPage } from '../features/transfer/pages/TransfersPage';
import { TransferCreatePage } from '../features/transfer/pages/TransferCreatePage';
import { TransferDetailPage } from '../features/transfer/pages/TransferDetailPage';
import { TestingRecordsPage } from '../features/testing/pages/TestingRecordsPage';
import { TestingRecordFormPage } from '../features/testing/pages/TestingRecordFormPage';
import { TestingRecordDetailPage } from '../features/testing/pages/TestingRecordDetailPage';

function ProtectedRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <div className="page-loading">Đang khởi tạo phiên…</div>;
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function RoleRoute({ roles }) {
  const { user } = useAuth();
  return roles.includes(user?.role) ? <Outlet /> : <Navigate to="/admin" replace />;
}

export function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<DashboardPage />} />
        <Route element={<RoleRoute roles={['ADMIN']} />}>
          <Route path="/admin/users" element={<UsersPage />} />
          <Route path="/admin/users/new" element={<UserFormPage />} />
          <Route path="/admin/users/:id/edit" element={<UserFormPage />} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'RECEPTIONIST']} />}>
          <Route path="/admin/intake" element={<IntakePage />} />
          <Route path="/admin/intake/new" element={<ReceiptCreatePage />} />
          <Route path="/admin/transfers/new" element={<TransferCreatePage />} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'RECEPTIONIST', 'TECHNICIAN']} />}>
          <Route path="/admin/transfers" element={<TransfersPage />} />
          <Route path="/admin/transfers/:id" element={<TransferDetailPage />} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'TECHNICIAN']} />}>
          <Route path="/admin/testing" element={<TestingRecordsPage />} />
          <Route path="/admin/testing/new" element={<TestingRecordFormPage />} />
          <Route path="/admin/testing/:id" element={<TestingRecordDetailPage />} />
          <Route path="/admin/testing/:id/edit" element={<TestingRecordFormPage />} />
        </Route>
        <Route path="/users" element={<Navigate to="/admin/users" replace />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/admin" replace />} />
  </Routes>;
}
