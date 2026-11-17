import { useCallback, useState } from 'react';

/**
 * Filter/sort/page state for a server-side list. Changing anything except the page returns to page 0, so the
 * user never lands on an empty page after narrowing a search. The server does all sorting and filtering.
 */
export function useListState(defaults) {
  const [state, setState] = useState({ page: 0, ...defaults });
  const set = useCallback((patch) => setState((current) => ({
    ...current, ...patch, page: 'page' in patch ? patch.page : 0,
  })), []);
  const reset = useCallback(() => setState({ page: 0, ...defaults }), [defaults]); // eslint-disable-line react-hooks/exhaustive-deps
  return [state, set, reset];
}

/** Drops empty values so they are not sent as empty query parameters. */
export function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value != null));
}
