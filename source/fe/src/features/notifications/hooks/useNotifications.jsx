import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notificationApi';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connectionState, setConnectionState] = useState('connecting');
  const [toasts, setToasts] = useState([]);
  const [dataVersion, setDataVersion] = useState(0);
  const [previewError, setPreviewError] = useState('');
  const toastTimers = useRef(new Map());
  const seenIds = useRef(new Set());
  const requestSequence = useRef(0);
  const eventRevision = useRef(0);
  const channelRef = useRef(null);
  const readsInFlight = useRef(new Map());

  const refresh = useCallback(async () => {
    const sequence = ++requestSequence.current;
    const revision = eventRevision.current;
    try {
      const page = await listNotifications({ size: 3, silent: true });
      if (sequence === requestSequence.current && revision === eventRevision.current) {
        page.content.forEach((item) => seenIds.current.add(item.id));
        setNotifications(page.content);
        setUnreadCount(page.unreadCount);
        setPreviewError('');
      }
    } catch (error) {
      if (sequence === requestSequence.current) setPreviewError(error.message);
      throw error;
    }
  }, []);

  const dismissToast = useCallback((id) => {
    clearTimeout(toastTimers.current.get(id));
    toastTimers.current.delete(id);
    setToasts((current) => current.filter((item) => item.id !== id));
  }, []);

  useEffect(() => {
    let active = true;
    let socket;
    let reconnectTimer;
    let syncTimer;
    let retryDelay = 1000;

    const scheduleSync = () => {
      if (!active || syncTimer != null || !navigator.onLine) return;
      syncTimer = window.setTimeout(() => {
        syncTimer = undefined;
        refresh().then(() => {
          if (active) setDataVersion((value) => value + 1);
        }).catch(() => undefined);
      }, 250);
    };

    refresh().catch(() => undefined);

    const connect = () => {
      if (!active || !navigator.onLine || socket) return;
      setConnectionState('connecting');
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const connection = new WebSocket(`${protocol}//${window.location.host}/ws/notifications`);
      socket = connection;
      connection.onopen = () => {
        if (!active || socket !== connection) return;
        retryDelay = 1000;
        setConnectionState('connected');
        scheduleSync();
      };
      connection.onmessage = (event) => {
        if (!active || socket !== connection) return;
        let notification;
        try {
          notification = JSON.parse(event.data);
        } catch {
          scheduleSync();
          return;
        }
        if (notification?.id == null || !notification.title) {
          scheduleSync();
          return;
        }
        if (seenIds.current.has(notification.id)) return;
        seenIds.current.add(notification.id);
        // Keep enough recent IDs to ignore duplicate events without growing the set indefinitely.
        while (seenIds.current.size > 500) seenIds.current.delete(seenIds.current.values().next().value);
        eventRevision.current += 1;
        setNotifications((current) => [notification, ...current].slice(0, 3));
        if (!notification.readAt) {
          setUnreadCount((count) => count + 1);
          setToasts((current) => [notification, ...current].slice(0, 3));
          toastTimers.current.set(notification.id, window.setTimeout(() => dismissToast(notification.id), 8000));
        }
        scheduleSync();
      };
      connection.onerror = () => connection.close();
      connection.onclose = () => {
        if (!active || socket !== connection) return;
        socket = undefined;
        setConnectionState('offline');
        if (!navigator.onLine) return;
        reconnectTimer = window.setTimeout(connect, retryDelay + Math.round(Math.random() * 300));
        retryDelay = Math.min(retryDelay * 2, 15000);
      };
    };

    const goOffline = () => {
      window.clearTimeout(reconnectTimer);
      const connection = socket;
      socket = undefined;
      connection?.close();
      setConnectionState('offline');
    };
    const goOnline = () => {
      window.clearTimeout(reconnectTimer);
      retryDelay = 1000;
      connect();
      scheduleSync();
    };
    const restoreVisible = () => {
      if (document.visibilityState === 'visible' && navigator.onLine) {
        if (!socket) goOnline();
        else scheduleSync();
      }
    };
    window.addEventListener('online', goOnline);
    window.addEventListener('offline', goOffline);
    document.addEventListener('visibilitychange', restoreVisible);
    const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('cfc-notification-reads');
    channel?.addEventListener('message', scheduleSync);
    channelRef.current = channel;
    const fallbackTimer = window.setInterval(() => {
      if (socket?.readyState !== WebSocket.OPEN && document.visibilityState === 'visible') scheduleSync();
    }, 30000);
    connect();
    if (!navigator.onLine) setConnectionState('offline');
    return () => {
      active = false;
      requestSequence.current += 1;
      window.clearTimeout(reconnectTimer);
      window.clearTimeout(syncTimer);
      window.clearInterval(fallbackTimer);
      window.removeEventListener('online', goOnline);
      window.removeEventListener('offline', goOffline);
      document.removeEventListener('visibilitychange', restoreVisible);
      channel?.close();
      channelRef.current = null;
      toastTimers.current.forEach((timer) => clearTimeout(timer));
      toastTimers.current.clear();
      socket?.close();
    };
  }, [dismissToast, refresh]);

  const markRead = useCallback(async (notification) => {
    if (notification.readAt) return;
    let request = readsInFlight.current.get(notification.id);
    if (!request) {
      request = markNotificationRead(notification.id).then((updated) => {
        eventRevision.current += 1;
        setNotifications((current) => current.map((item) => item.id === notification.id ? updated : item));
        setUnreadCount((count) => Math.max(0, count - 1));
        setDataVersion((value) => value + 1);
        channelRef.current?.postMessage('read');
        refresh().catch(() => undefined);
      }).finally(() => readsInFlight.current.delete(notification.id));
      readsInFlight.current.set(notification.id, request);
    }
    return request;
  }, [refresh]);

  const markAllRead = useCallback(async () => {
    const revision = eventRevision.current;
    await markAllNotificationsRead();
    const hasNewEvents = revision !== eventRevision.current;
    eventRevision.current += 1;
    if (!hasNewEvents) {
      setNotifications((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString() })));
      setUnreadCount(0);
    }
    setDataVersion((value) => value + 1);
    channelRef.current?.postMessage('read');
    refresh().catch(() => undefined);
  }, [refresh]);

  const value = useMemo(() => ({
    notifications, unreadCount, connectionState, dataVersion, previewError, refresh, markRead, markAllRead, toasts, dismissToast,
  }), [connectionState, dataVersion, dismissToast, markAllRead, markRead, notifications, previewError, refresh, toasts, unreadCount]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
