import { type ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from '../features/auth/AuthContext';
import { LoginPage } from '../features/auth/LoginPage';
import { SignupPage } from '../features/auth/SignupPage';
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage';
import { Nav } from '../features/nav/Nav';
import { SearchPage } from '../features/books/SearchPage';
import { ReadingListPage } from '../features/books/ReadingListPage';
import { HistoryPage } from '../features/books/HistoryPage';
import { ChildRewardsPage } from '../pages/ChildRewards/ChildRewardsPage';
import { ManageFamilyRewardsPage } from '../pages/ParentRewards/ManageFamilyRewardsPage';
import { ParentDashboard } from '../features/parent/ParentDashboard';
import { ParentSummary } from '../features/parent/ParentSummary';
import '../styles/global.css';
import '../features/books/SearchPage.css';
import '../features/books/ReadingListPage.css';
import '../features/books/HistoryPage.css';
import '../features/rewards/RewardsPage.css';
import '../features/parent/ParentDashboard.css';

function ParentOnlyRoute({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  if (user?.role !== 'PARENT') {
    return <Navigate to="/reading-list" replace />;
  }

  return <>{children}</>;
}

function ChildOnlyRoute({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  if (user?.role !== 'CHILD') {
    return <Navigate to="/parent" replace />;
  }

  return <>{children}</>;
}

function AuthenticatedLayout() {
  return (
    <div className="app-layout grid min-h-screen grid-cols-1 bg-background-alt text-text-primary md:grid-cols-3">
      <div className="md:col-span-3">
        <Nav />
      </div>
      <div className="page-content md:col-span-3">
        <Routes>
          <Route path="/" element={<Navigate to="/reading-list" replace />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/reading-list" element={<ReadingListPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/rewards" element={<Navigate to="/child/rewards" replace />} />
          <Route
            path="/parent/rewards"
            element={(
              <ParentOnlyRoute>
                <ManageFamilyRewardsPage />
              </ParentOnlyRoute>
            )}
          />
          <Route
            path="/child/rewards"
            element={(
              <ChildOnlyRoute>
                <ChildRewardsPage />
              </ChildOnlyRoute>
            )}
          />
          <Route
            path="/parent"
            element={(
              <ParentOnlyRoute>
                <ParentDashboard />
              </ParentOnlyRoute>
            )}
          />
          <Route
            path="/parent/summary"
            element={(
              <ParentOnlyRoute>
                <ParentSummary />
              </ParentOnlyRoute>
            )}
          />
          <Route
            path="/parent/summary/:childId"
            element={(
              <ParentOnlyRoute>
                <ParentSummary />
              </ParentOnlyRoute>
            )}
          />
          <Route path="*" element={<Navigate to="/reading-list" replace />} />
        </Routes>
      </div>
    </div>
  );
}

function AppRoutes() {
  const { token } = useAuth();

  if (!token) {
    return (
      <Routes>
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="*" element={<AuthenticatedLayout />} />
    </Routes>
  );
}

export function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
