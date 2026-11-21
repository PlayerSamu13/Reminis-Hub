(() => {
    const button = document.querySelector('.nav-toggle');
    if (!button) return;
    const nav = document.getElementById(button.getAttribute('aria-controls'));
    const mobile = matchMedia('(max-width: 767px)');
    function show(open) {
        nav.hidden = mobile.matches && !open;
        button.hidden = !mobile.matches;
        button.setAttribute('aria-expanded', String(!nav.hidden));
    }
    button.addEventListener('click', () => show(nav.hidden));
    nav.parentElement.addEventListener('keydown', event => {
        if (event.key === 'Escape' && mobile.matches && !nav.hidden) {
            show(false);
            button.focus();
        }
    });
    mobile.addEventListener('change', () => show(false));
    show(false);
})();
