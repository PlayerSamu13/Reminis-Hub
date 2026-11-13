/* Mejora progresiva: sin JavaScript, formulario y paginación siguen siendo enlaces GET. */
$(function () {
    const form = $('#filtros');
    const status = $('#search-status');
    let timer;
    let pending;
    let generation = 0;

    function load(url, moveFocus) {
        const current = ++generation;
        if (pending) pending.abort();
        $('#resultados').attr('aria-busy', 'true');
        status.removeClass('search-error').addClass('visually-hidden').text(status.data('loading'));
        const endpoint = new URL(url, window.location.origin);
        endpoint.pathname = form.attr('action').replace(/\/proyectos$/, '/catalogo/resultados');
        pending = $.ajax({url: endpoint.pathname + endpoint.search, dataType: 'html'});
        pending.done(function (html) {
            if (current !== generation) return;
            $('#resultados').replaceWith(html);
            status.text($('#resultados .result-count').text());
            window.history.replaceState(null, '', url);
            // Actualizar idioma conserva los filtros de la última búsqueda AJAX.
            $('.language-switch a').each(function () {
                const languageUrl = new URL(url, window.location.origin);
                languageUrl.searchParams.set('lang', this.hreflang);
                $(this).attr('href', languageUrl.pathname + languageUrl.search);
            });
            if (moveFocus) $('#resultados').trigger('focus');
        }).fail(function (_, reason) {
            if (reason === 'abort' || current !== generation) return;
            status.removeClass('visually-hidden').addClass('search-error').text(status.data('error'));
        }).always(function () {
            if (current === generation) $('#resultados').removeAttr('aria-busy');
        });
    }

    form.on('submit', function (event) {
        event.preventDefault();
        clearTimeout(timer);
        if (!this.reportValidity()) return;
        load(this.action + '?' + form.serialize(), false);
    });
    // Cancelar al escribir evita que una respuesta anterior sustituya una búsqueda nueva.
    $('#q').on('input', function () {
        clearTimeout(timer);
        ++generation;
        if (pending) pending.abort();
        $('#resultados').removeAttr('aria-busy');
        timer = setTimeout(function () { form.trigger('submit'); }, 300);
    });
    form.find('select').on('change', function () { form.trigger('submit'); });
    $(document).on('click', '#resultados .pagination-bar a', function (event) {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        clearTimeout(timer);
        load(this.href, true);
    });
});
