import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { useAuth } from '../features/auth/hooks/useAuth';
import { SplashScreen } from '../shared/components/Loading';

const AdminLayout = lazy(() => import('../features/admin/components/AdminLayout').then(m => ({ default: m.AdminLayout })));
const LoginPage = lazy(() => import('../features/auth/pages/LoginPage').then(m => ({ default: m.LoginPage })));
const TestMethodsPage = lazy(() => import('../features/methods/pages/TestMethodsPage').then(m => ({ default: m.TestMethodsPage })));
const UsersPage = lazy(() => import('../features/users/pages/UsersPage').then(m => ({ default: m.UsersPage })));
const UserFormPage = lazy(() => import('../features/users/pages/UserFormPage').then(m => ({ default: m.UserFormPage })));
const ReceptionistDashboardPage = lazy(() => import('../features/intake/pages/ReceptionistDashboardPage').then(m => ({ default: m.ReceptionistDashboardPage })));
const IntakePage = lazy(() => import('../features/intake/pages/IntakePage').then(m => ({ default: m.IntakePage })));
const ReceiptCreatePage = lazy(() => import('../features/intake/pages/ReceiptCreatePage').then(m => ({ default: m.ReceiptCreatePage })));
const ReceiptDetailPage = lazy(() => import('../features/intake/pages/ReceiptDetailPage').then(m => ({ default: m.ReceiptDetailPage })));
const DashboardPage = lazy(() => import('../features/admin/pages/DashboardPage').then(m => ({ default: m.DashboardPage })));
const TransfersPage = lazy(() => import('../features/transfer/pages/TransfersPage').then(m => ({ default: m.TransfersPage })));
const TransferCreatePage = lazy(() => import('../features/transfer/pages/TransferCreatePage').then(m => ({ default: m.TransferCreatePage })));
const TransferDetailPage = lazy(() => import('../features/transfer/pages/TransferDetailPage').then(m => ({ default: m.TransferDetailPage })));
const TestingRecordsPage = lazy(() => import('../features/testing/pages/TestingRecordsPage').then(m => ({ default: m.TestingRecordsPage })));
const TestingRecordFormPage = lazy(() => import('../features/testing/pages/TestingRecordFormPage').then(m => ({ default: m.TestingRecordFormPage })));
const TestingRecordDetailPage = lazy(() => import('../features/testing/pages/TestingRecordDetailPage').then(m => ({ default: m.TestingRecordDetailPage })));
const NotificationsPage = lazy(() => import('../features/notifications/pages/NotificationsPage').then(m => ({ default: m.NotificationsPage })));
const ProfilePage = lazy(() => import('../features/profile/pages/ProfilePage').then(m => ({ default: m.ProfilePage })));
const ResultReleasesPage = lazy(() => import('../features/results/pages/ResultReleasesPage').then(m => ({ default: m.ResultReleasesPage })));
const ResultReleaseDetailPage = lazy(() => import('../features/results/pages/ResultReleaseDetailPage').then(m => ({ default: m.ResultReleaseDetailPage })));
const ReceivingDashboardPage = lazy(() => import('../features/receiving/pages/ReceivingDashboardPage').then(m => ({ default: m.ReceivingDashboardPage })));
const ReceivingListPage = lazy(() => import('../features/receiving/pages/ReceivingListPage').then(m => ({ default: m.ReceivingListPage })));
const ReceivingDetailPage = lazy(() => import('../features/receiving/pages/ReceivingDetailPage').then(m => ({ default: m.ReceivingDetailPage })));
const TestRequestsPage = lazy(() => import('../features/sampling/pages/TestRequestsPage').then(m => ({ default: m.TestRequestsPage })));
const SamplingPlanPage = lazy(() => import('../features/sampling/pages/SamplingPlanPage').then(m => ({ default: m.SamplingPlanPage })));
const FieldSamplingPage = lazy(() => import('../features/sampling/pages/FieldSamplingPage').then(m => ({ default: m.FieldSamplingPage })));
const TestRequestFormPage = lazy(() => import('../features/sampling/pages/TestRequestFormPage').then(m => ({ default: m.TestRequestFormPage })));

function ProtectedRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <SplashScreen />;
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function RoleRoute({ roles }) {
  const { user } = useAuth();
  return roles.includes(user?.role) ? <Outlet /> : <Navigate to="/admin" replace />;
}

function IntakeHome() {
  const { user } = useAuth();
  return user?.role === 'RECEPTIONIST' ? <ReceptionistDashboardPage /> : <Navigate to="/admin/intake/receipts" replace />;
}

function TransferRoleRedirect({ children }) {
  const { user } = useAuth();
  const location = useLocation();
  return user?.role === 'TECHNICIAN'
    ? <Navigate to={location.pathname.replace('/admin/transfers', '/admin/receiving/transfers') + location.search} replace />
    : children;
}

export function App() {
  return <Suspense fallback={<SplashScreen />}><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<DashboardPage />} />
        <Route path="/admin/notifications" element={<NotificationsPage />} />
        <Route path="/admin/profile" element={<ProfilePage />} />
        <Route element={<RoleRoute roles={['ADMIN']} />}>
          <Route path="/admin/users" element={<UsersPage />} />
          <Route path="/admin/test-methods" element={<TestMethodsPage />} />
          <Route path="/admin/users/new" element={<UserFormPage />} />
          <Route path="/admin/users/:id/edit" element={<UserFormPage />} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'RECEPTIONIST']} />}>
          <Route path="/admin/requests" element={<TestRequestsPage />} />
          <Route path="/admin/requests/new" element={<TestRequestFormPage />} />
          <Route path="/admin/requests/:id" element={<TestRequestFormPage />} />
          <Route path="/admin/requests/:id/sampling-plan" element={<SamplingPlanPage />} />
          <Route path="/admin/requests/:id/field-sampling" element={<FieldSamplingPage />} />
          <Route path="/admin/intake" element={<IntakeHome />} />
          <Route path="/admin/intake/receipts" element={<IntakePage />} />
          <Route path="/admin/intake/new" element={<ReceiptCreatePage />} />
          <Route path="/admin/intake/:id" element={<ReceiptDetailPage />} />
          <Route path="/admin/transfers/new" element={<TransferCreatePage />} />
          <Route path="/admin/transfers/:id/edit" element={<TransferCreatePage />} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'RECEPTIONIST', 'TECHNICIAN']} />}>
          <Route path="/admin/transfers" element={<TransferRoleRedirect><TransfersPage /></TransferRoleRedirect>} />
          <Route path="/admin/transfers/:id" element={<TransferRoleRedirect><TransferDetailPage /></TransferRoleRedirect>} />
        </Route>
        <Route element={<RoleRoute roles={['ADMIN', 'TECHNICIAN']} />}>
          <Route path="/admin/receiving" element={<ReceivingDashboardPage />} />
          <Route path="/admin/receiving/transfers" element={<ReceivingListPage />} />
          <Route path="/admin/receiving/transfers/:id" element={<ReceivingDetailPage />} />
          <Route path="/admin/testing" element={<TestingRecordsPage />} />
          <Route path="/admin/testing/new" element={<TestingRecordFormPage />} />
          <Route path="/admin/testing/:id" element={<TestingRecordDetailPage />} />
          <Route path="/admin/testing/:id/edit" element={<TestingRecordFormPage />} />
          <Route path="/admin/results" element={<ResultReleasesPage />} />
          <Route path="/admin/results/:id" element={<ResultReleaseDetailPage />} />
        </Route>
        <Route path="/users" element={<Navigate to="/admin/users" replace />} />
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/admin" replace />} />
  </Routes></Suspense>;
}
