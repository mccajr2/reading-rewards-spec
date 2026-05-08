import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from '../features/auth/AuthContext';
import { LoginPage } from '../features/auth/LoginPage';
import { SignupPage } from '../features/auth/SignupPage';
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage';
import { Nav } from '../features/nav/Nav';
import { SearchPage } from '../features/books/SearchPage';
import { ReadingListPage } from '../features/books/ReadingListPage';
import { HistoryPage } from '../features/books/HistoryPage';
import { RewardsPage } from '../features/rewards/RewardsPage';
import { ParentDashboard } from '../features/parent/ParentDashboard';
import { ParentSummary } from '../features/parent/ParentSummary';
import '../styles/global.css';
import '../features/books/SearchPage.css';
import '../features/books/ReadingListPage.css';
import '../features/books/HistoryPage.css';
import '../features/rewards/RewardsPage.css';
import '../features/parent/ParentDashboard.css';

function AuthenticatedLayout() {
  return (
    <div className="app-layout">
      <Nav />
      <div className="page-content">
        <Routes>
          <Route path="/" element={<Navigate to="/reading-list" replace />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/reading-list" element={<ReadingListPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/rewards" element={<RewardsPage />} />
          <Route path="/parent" element={<ParentDashboard />} />
          <Route path="/parent/summary" element={<ParentSummary />} />
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
