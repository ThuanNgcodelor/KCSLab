if (import.meta.env.PROD && window.isSecureContext && 'serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js').catch((error) => {
    console.warn('Không thể bật chế độ PWA cho CFC Lab.', error);
  });
}
