// @ts-ignore
if (import.meta.hot) {
  // @ts-ignore
  const hot = import.meta.hot;

  const isLiveReloadDisabled = () => {
    // Checks if live reload is disabled in the debug window
    return sessionStorage.getItem('vaadin.live-reload.active') === 'false';
  };

  const preventViteReload = (payload: any) => {
    // Changing the path prevents Vite from reloading
    payload.path = '/_fake/path.html';
  };

  let pendingNavigationTo: string | undefined = undefined;

  window.addEventListener('vaadin-router-go', (routerEvent: any) => {
    pendingNavigationTo = routerEvent.detail.pathname + routerEvent.detail.search;
  });
  hot.on('vite:beforeFullReload', (payload: any) => {
    if (isLiveReloadDisabled()) {
      preventViteReload(payload);
    }
    if (pendingNavigationTo) {
      // Force reload with the new URL
      location.href = pendingNavigationTo;
      preventViteReload(payload);
    }
  });
}
