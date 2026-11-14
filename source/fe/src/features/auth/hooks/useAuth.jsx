import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getCurrentUser, login, logout } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const handleRefreshed = (event) => setUser(event.detail);
    const handleExpired = () => setUser(null);
    window.addEventListener('auth:refreshed', handleRefreshed);
    window.addEventListener('auth:expired', handleExpired);
    return () => {
      window.removeEventListener('auth:refreshed', handleRefreshed);
      window.removeEventListener('auth:expired', handleExpired);
    };
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    updateCurrentUser: setUser,
    signIn: async (credentials) => {
      const nextUser = await login(credentials);
      setUser(nextUser);
      return nextUser;
    },
    signOut: async () => {
      await logout().catch(() => undefined);
      setUser(null);
    },
  }), [loading, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
