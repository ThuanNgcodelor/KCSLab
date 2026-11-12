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

  const value = useMemo(() => ({
    user,
    loading,
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
