function disableOnClickListener({currentTarget: button}) {
  if (button.hasAttribute('disableOnClick')) {
    button.disabled = true;
  }
}

window.Vaadin.Flow.button = {
  initDisableOnClick: (button) => {
    if (!button.__hasDisableOnClickListener) {
      button.addEventListener('click', disableOnClickListener);
      button.__hasDisableOnClickListener = true;
    }
  }
}
