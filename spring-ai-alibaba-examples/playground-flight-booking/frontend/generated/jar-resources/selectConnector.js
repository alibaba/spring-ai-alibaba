(function () {
  const tryCatchWrapper = function (callback) {
    return window.Vaadin.Flow.tryCatchWrapper(callback, 'Vaadin Select');
  };

  window.Vaadin.Flow.selectConnector = {
    initLazy: (select) =>
      tryCatchWrapper(function (select) {
        // do not init this connector twice for the given select
        if (select.$connector) {
          return;
        }

        select.$connector = {};

        select.renderer = tryCatchWrapper(function (root) {
          const listBox = select.querySelector('vaadin-select-list-box');
          if (listBox) {
            if (root.firstChild) {
              root.removeChild(root.firstChild);
            }
            root.appendChild(listBox);
          }
        });
      })(select)
  };
})();
