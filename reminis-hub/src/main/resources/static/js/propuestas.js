/* Los índices contiguos corresponden a la lista validada por Spring; nunca se inserta HTML del usuario. */
$(function () {
    const lista = $('#coautores');
    const agregar = $('#agregar-autor');
    function renumerar() {
        lista.children('.coautor').each(function (indice) {
            const fila = $(this);
            const nombre = fila.find('input').eq(0);
            const evidencia = fila.find('input').eq(1);
            nombre.attr({id: 'autor-nombre-' + indice, name: 'autores[' + indice + '].nombrePublico'});
            evidencia.attr({id: 'autor-evidencia-' + indice, name: 'autores[' + indice + '].referenciaEvidencia'});
            fila.find('label').eq(0).attr('for', nombre.attr('id'));
            fila.find('label').eq(1).attr('for', evidencia.attr('id'));
        });
        agregar.prop('disabled', lista.children('.coautor').length >= 9);
    }
    agregar.prop('hidden', false).on('click', function () {
        if (lista.children('.coautor').length >= 9) return;
        const fragmento = document.getElementById('plantilla-autor').content.cloneNode(true);
        lista.append(fragmento);
        renumerar();
        lista.children().last().find('input').first().trigger('focus');
    });
    lista.find('.quitar-autor').prop('hidden', false);
    lista.on('click', '.quitar-autor', function () {
        $(this).closest('.coautor').remove();
        renumerar();
        agregar.trigger('focus');
    });
    renumerar();
    $('#errores-propuesta').trigger('focus');
});
