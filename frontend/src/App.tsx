import { App as RoutedApp } from './app/App';

// Task-path compatibility shim for feature scaffolding.
export { rewardRoutes } from './components/AuthContext';

export function App() {
  return <RoutedApp />;
}
