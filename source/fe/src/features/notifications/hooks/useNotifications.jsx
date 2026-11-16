import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notificationApi';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connectionState, setConnectionState] = useState('connecting');
  const [toasts, setToasts] = useState([]);
  const reconnectTimer = useRef(null);
  const toastTimers = useRef(new Map());
  const seenIds = useRef(new Set());
  const requestSequence = useRef(0);
  const eventRevision = useRef(0);

  const refresh = useCallback(async () => {
    const sequence = ++requestSequence.current;
    const revision = eventRevision.current;
    const page = await listNotifications({ size: 3 });
    if (sequence === requestSequence.current && revision === eventRevision.current) {
      page.content.forEach((item) => seenIds.current.add(item.id));
      setNotifications(page.content);
      setUnreadCount(page.unreadCount);
    }
    return page;
  }, []);

  const dismissToast = useCallback((id) => {
    clearTimeout(toastTimers.current.get(id));
    toastTimers.current.delete(id);
    setToasts((current) => current.filter((item) => item.id !== id));
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
      socket.onopen = () => {
        retryDelay = 1000; setConnectionState('connected');
        // REST is authoritative, including events missed while disconnected.
        refresh().catch(() => undefined);
      };
      socket.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data);
          if (notification.id == null || !notification.title) throw new Error('Invalid notification');
          const duplicate = seenIds.current.has(notification.id);
          seenIds.current.add(notification.id);
          // Bound the cache; recent IDs are enough to prevent duplicate live delivery.
          if (seenIds.current.size > 500) seenIds.current.delete(seenIds.current.values().next().value);
          eventRevision.current += 1;
          setNotifications((current) => [notification, ...current.filter((item) => item.id !== notification.id)].slice(0, 3));
          if (!duplicate && !notification.readAt) {
            setUnreadCount((count) => count + 1);
            setToasts((current) => [notification, ...current.filter((item) => item.id !== notification.id)].slice(0, 3));
            toastTimers.current.set(notification.id, window.setTimeout(() => dismissToast(notification.id), 8000));
          }
          refresh().catch(() => undefined);
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
      toastTimers.current.forEach((timer) => clearTimeout(timer));
      toastTimers.current.clear();
      socket?.close();
    };
  }, [dismissToast, refresh]);

  const markRead = useCallback(async (notification) => {
    if (!notification.readAt) await markNotificationRead(notification.id);
    eventRevision.current += 1;
    setNotifications((current) => current.map((item) => (
      item.id === notification.id ? { ...item, readAt: item.readAt ?? new Date().toISOString() } : item
    )));
    if (!notification.readAt) setUnreadCount((count) => Math.max(0, count - 1));
  }, []);

  const markAllRead = useCallback(async () => {
    await markAllNotificationsRead();
    eventRevision.current += 1;
    setNotifications((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
    setUnreadCount(0);
  }, []);

  const value = useMemo(() => ({
    notifications, unreadCount, connectionState, refresh, markRead, markAllRead, toasts, dismissToast,
  }), [connectionState, dismissToast, markAllRead, markRead, notifications, refresh, toasts, unreadCount]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
