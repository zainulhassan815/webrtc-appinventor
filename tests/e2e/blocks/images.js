const fs = require('fs');
const path = require('path');
const { open } = require('./editor');

const USAGE = 'usage: images.js <appinventor-dir> <Screen.scm> <Screen.bky> <components.json> <out-dir> ' +
  '<block id | Component.Event | procedure:name | global:name>...';

function find(name) {
  const workspace = Blockly.common.getMainWorkspace();
  const [component, event] = name.split('.');
  const [kind, label] = name.split(':');
  const named = { procedure: 'procedures_defnoreturn', global: 'global_declaration' }[kind];
  const block = workspace.getBlockById(name) || workspace.getTopBlocks(false).find(b => named
    ? b.type === named && b.getFieldValue('NAME') === label
    : b.type === 'component_event' && b.instanceName === component && b.eventName === event);
  if (!block) return false;
  Blockly.exportBlockAsPng(block);
  return true;
}

async function downloaded(dir, before) {
  for (let i = 0; i < 100; i++) {
    const added = fs.readdirSync(dir).filter(f => !before.has(f) && f.endsWith('.png'));
    if (added.length) return added[0];
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  throw new Error('no image was downloaded');
}

async function main() {
  const [appinventor, scmPath, bkyPath, extensionsPath, outDir, ...names] = process.argv.slice(2);
  if (!names.length) {
    console.error(USAGE);
    process.exit(2);
  }
  fs.mkdirSync(outDir, { recursive: true });
  const downloads = fs.mkdtempSync(path.join(outDir, '.downloads-'));
  const { browser, tab, errors } = await open(appinventor, scmPath, bkyPath, extensionsPath);
  const cdp = await tab.createCDPSession();
  await cdp.send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: path.resolve(downloads) });
  for (const name of names) {
    const before = new Set(fs.readdirSync(downloads));
    if (!await tab.evaluate(find, name)) {
      errors.push(`no top-level block ${name}`);
      continue;
    }
    const file = await downloaded(downloads, before);
    fs.renameSync(path.join(downloads, file), path.join(outDir, name.replace(/[:.]/g, '-') + '.png'));
  }
  await browser.close();
  fs.rmSync(downloads, { recursive: true });
  if (errors.length) {
    console.error(errors.join('\n'));
    process.exit(1);
  }
}

main();
