import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notificationApi';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connectionState, setConnectionState] = useState('connecting');
  const reconnectTimer = useRef(null);

  const refresh = useCallback(async () => {
    const page = await listNotifications({ size: 3 });
    setNotifications(page.content);
    setUnreadCount(page.unreadCount);
    return page;
  }, []);

  useEffect(() => {
    let active = true;
    let socket;
    let retryDelay = 1000;

    refresh().catch(() => undefined);

    const connect = () => {
      if (!active) return;
      setConnectionState('connecting');
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      socket = new WebSocket(`${protocol}//${window.location.host}/ws/notifications`);
      socket.onopen = () => { retryDelay = 1000; setConnectionState('connected'); };
      socket.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data);
          setNotifications((current) => [notification, ...current.filter((item) => item.id !== notification.id)].slice(0, 3));
          setUnreadCount((count) => count + (notification.readAt ? 0 : 1));
        } catch {
          refresh().catch(() => undefined);
        }
      };
      socket.onerror = () => socket.close();
      socket.onclose = () => {
        if (!active) return;
        setConnectionState('offline');
        reconnectTimer.current = window.setTimeout(connect, retryDelay);
        retryDelay = Math.min(retryDelay * 2, 15000);
      };
    };

    connect();
    return () => {
      active = false;
      window.clearTimeout(reconnectTimer.current);
      socket?.close();
    };
  }, [refresh]);

  const markRead = useCallback(async (notification) => {
    if (!notification.readAt) await markNotificationRead(notification.id);
    setNotifications((current) => current.map((item) => (
      item.id === notification.id ? { ...item, readAt: item.readAt ?? new Date().toISOString() } : item
    )));
    if (!notification.readAt) setUnreadCount((count) => Math.max(0, count - 1));
  }, []);

  const markAllRead = useCallback(async () => {
    await markAllNotificationsRead();
    setNotifications((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
    setUnreadCount(0);
  }, []);

  const value = useMemo(() => ({
    notifications, unreadCount, connectionState, refresh, markRead, markAllRead,
  }), [connectionState, markAllRead, markRead, notifications, refresh, unreadCount]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
