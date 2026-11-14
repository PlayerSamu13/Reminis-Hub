// Herramienta de QA local: no es una dependencia ni un recurso de la aplicacion.
// RH_QA_MODULES apunta al node_modules que contiene selenium-webdriver y axe-core.
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const modules = process.env.RH_QA_MODULES;
if (!modules) throw new Error('Configura RH_QA_MODULES con los modulos de las herramientas de auditoria.');
const { Builder, By, Key, until } = require(path.join(modules, 'selenium-webdriver'));
const edge = require(path.join(modules, 'selenium-webdriver/edge'));
const base = process.env.RH_QA_URL || 'http://127.0.0.1:8081';
if (!['127.0.0.1', 'localhost', '[::1]'].includes(new URL(base).hostname)) {
    throw new Error('La auditoria solo admite un servidor local.');
}
const output = path.resolve(__dirname, '../docs/evidencias');
async function browser(withoutJs = false) {
    const options = new edge.Options().addArguments('--headless=new', '--window-size=1366,900');
    if (withoutJs) options.setUserPreferences({ 'profile.managed_default_content_settings.javascript': 2 });
    return new Builder().forBrowser('MicrosoftEdge').setEdgeOptions(options).build();
}
async function audits() {
    const driver = await browser();
    const results = [];
    try {
        await driver.manage().setTimeouts({ pageLoad: 30000, script: 60000 });
        await driver.get(base + '/proyectos?lang=es');
        const detail = await driver.findElement(By.css('.project-card h3 a')).getAttribute('href');
        const source = await fs.readFile(path.join(modules, 'axe-core/axe.min.js'), 'utf8');
        for (const [name, url] of [['catalogo-es', base + '/proyectos?lang=es'],
            ['catalogo-en', base + '/proyectos?lang=en'], ['detalle-es', detail + '?lang=es'],
            ['detalle-en', detail + '?lang=en']]) {
            await driver.get(url);
            const names = {};
            for (const selector of ['.brand', 'a[hreflang="es"]', 'a[hreflang="en"]', '.language-switch']) {
                names[selector] = await driver.findElement(By.css(selector)).getAccessibleName();
            }
            assert.ok(names['.brand'].includes('Reminis'));
            assert.equal(names['a[hreflang="es"]'], 'ES — Español');
            assert.equal(names['a[hreflang="en"]'], 'EN — English');
            assert.ok(names['.language-switch'].length > 0);
            const selects = await driver.executeScript(`return [...document.querySelectorAll('select')].map(el => {
                const s = getComputedStyle(el);
                return {id: el.id, color: s.color, backgroundColor: s.backgroundColor, backgroundImage: s.backgroundImage};
            })`);
            await driver.executeScript(source);
            const result = await driver.executeAsyncScript(function () {
                const done = arguments[arguments.length - 1];
                axe.run(document, { rules: { 'label-content-name-mismatch': { enabled: true } } })
                    .then(done).catch(error => done({ auditError: error.message }));
            });
            if (result.auditError) throw new Error(result.auditError);
            await fs.writeFile(path.join(output, 'axe-' + name + '.json'), JSON.stringify(result, null, 2));
            results.push({ view: name, names, selects, violations: result.violations.map(v => v.id), incomplete: result.incomplete.length });
            console.log(`${result.violations.length ? 'FAIL' : 'PASS'} axe ${name}: ${result.violations.length} reglas con infracciones; ${result.incomplete.length} revisiones manuales`);
        }
    } finally { await driver.quit(); }
    return results;
}
async function withoutJavascript() {
    const driver = await browser(true);
    const steps = [];
    try {
        await driver.manage().setTimeouts({ pageLoad: 30000, implicit: 3000 });
        await driver.get(base + '/proyectos?lang=es');
        assert.equal(await driver.executeScript('return typeof window.jQuery'), 'undefined');
        assert.equal((await driver.findElements(By.css('.project-card'))).length, 6);
        await driver.findElement(By.css('.pagination-bar a')).sendKeys(Key.ENTER);
        await driver.wait(until.urlContains('pagina=1'), 5000);
        assert.equal((await driver.findElements(By.css('.project-card'))).length, 2);
        await driver.findElement(By.css('.project-card h3 a')).sendKeys(Key.ENTER);
        await driver.findElement(By.css('.detail-page h1'));
        await driver.findElement(By.css('.back-link')).sendKeys(Key.ENTER);
        steps.push('Paginacion 6+2, apertura de detalle y regreso');
        await driver.findElement(By.css('#categoria option[value="WEB"]')).click();
        await driver.findElement(By.xpath('//select[@id="tecnologia"]/option[.="Java"]')).click();
        await driver.findElement(By.css('#anio option[value="2026"]')).click();
        await driver.findElement(By.id('q')).sendKeys('Aula');
        // Sin JS los filtros esperan el envio del formulario GET.
        assert.equal((await driver.findElements(By.css('.project-card'))).length, 6);
        await driver.findElement(By.css('button[form="filtros"]')).sendKeys(Key.ENTER);
        await driver.wait(until.urlContains('q=Aula'), 5000);
        assert.equal((await driver.findElements(By.css('.project-card'))).length, 1);
        steps.push('Texto y tres filtros combinados mediante formulario GET');
        await driver.findElement(By.css('a[hreflang="en"]')).sendKeys(Key.ENTER);
        assert.ok((await driver.findElement(By.css('.hero h1')).getText()).includes('Your next great idea.'));
        assert.equal(await driver.findElement(By.id('q')).getAttribute('value'), 'Aula');
        await driver.findElement(By.id('q')).clear();
        await driver.findElement(By.id('q')).sendKeys('SinCoincidenciaQA');
        await driver.findElement(By.css('button[form="filtros"]')).sendKeys(Key.ENTER);
        assert.equal(await driver.findElement(By.css('.empty-state h3')).getText(), 'No matches yet');
        assert.equal(await driver.executeScript('return typeof window.jQuery'), 'undefined');
        steps.push('Idioma conserva filtros y estado vacio en ingles');
        await fs.writeFile(path.join(output, 'sin-javascript.png'), await driver.takeScreenshot(), 'base64');
        console.log('PASS: recorrido sin JavaScript');
        return { status: 'PASS', steps };
    } finally { await driver.quit(); }
}
(async () => {
    await fs.mkdir(output, { recursive: true });
    const axe = process.argv.includes('--sin-js') ? [] : await audits();
    const noJs = await withoutJavascript();
    await fs.writeFile(path.join(output, 'revision-catalogo.json'), JSON.stringify({ date: new Date().toISOString(), axe, noJs }, null, 2));
    if (axe.some(result => result.violations.length)) process.exitCode = 1;
})().catch(error => { console.error('FAIL:', error.message); process.exitCode = 1; });
