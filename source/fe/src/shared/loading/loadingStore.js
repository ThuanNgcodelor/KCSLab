// Tiny external store counting in-flight API requests so the UI can show a global progress indicator.
let pending = 0;
let slowSince = 0;
const listeners = new Set();
let snapshot = { pending: 0 };

function emit() {
  snapshot = { pending };
  listeners.forEach((listener) => listener());
}

/** Marks a request as started; call the returned function when it settles. */
export function trackRequest() {
  pending += 1;
  if (pending === 1) slowSince = Date.now();
  emit();
  let finished = false;
  return () => {
    if (finished) return;
    finished = true;
    pending = Math.max(0, pending - 1);
    emit();
  };
}

export function subscribeLoading(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getLoadingSnapshot() { return snapshot; }
export function getLoadingSince() { return slowSince; }
