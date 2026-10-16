const fs = require('fs');
const os = require('os');
const path = require('path');
const puppeteer = require('puppeteer');

const YA_VERSION = '237';
const BLOCKS_VERSION = '39';
const SCRIPTS = [
  'build/blocklyeditor/msg/messages.nocache.js',
  'build/blocklyeditor/blockly-all.js',
  'build/blocklyeditor/component-types.js',
  'appengine/war/static/js/scroll-options-6.0.16.min.js',
  'appengine/war/static/js/workspace-search-9.1.10.min.js',
  'appengine/war/static/js/block-dynamic-connection-0.7.16.min.js',
  'appengine/war/static/js/blockly-field-colour-5.0.19.min.js',
  'appengine/war/static/js/workspace-multiselect-pr135-edf5dd2.min.js',
  'blocklyeditor/tests/testCommon.js',
  'blocklyeditor/src/msg/en/_messages.js',
];

function readForm(scmPath) {
  const scm = fs.readFileSync(scmPath, 'utf8');
  return scm.substring(scm.indexOf('{'), scm.lastIndexOf('}') + 1);
}

function page(appinventor) {
  const scripts = SCRIPTS.map(s => `<script src="file://${path.join(appinventor, s)}"></script>`).join('\n');
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'blocks-')), 'page.html');
  fs.writeFileSync(file, `<!doctype html><html><head><meta charset="utf-8">${scripts}</head>` +
    '<body style="margin:0;width:3000px;height:3000px"></body></html>');
  return file;
}

function load(form, bky, extensions, versions) {
  window.YA_VERSION = versions.ya;
  window.BLOCKS_VERSION = versions.blocks;
  componentTypeJson.push(...extensions);
  const workspace = Blockly.BlocklyEditor.create(document.body, 'Screen1', false, false);
  Blockly.common.setMainWorkspace(workspace);
  initComponentTypes();
  processVersion(versions.blocks, versions.ya);
  processForm(form);
  processBlocks(form, bky);
  workspace.cleanUp();
  return workspace.getAllBlocks(false).filter(b => b.isBadBlock && b.isBadBlock()).map(b => `bad block: ${b.type}`);
}

async function open(appinventor, scmPath, bkyPath, extensionsPath, browserOptions = {}) {
  const browser = await puppeteer.launch({ args: ['--allow-file-access-from-files', '--no-sandbox'], ...browserOptions });
  const tab = await browser.newPage();
  const errors = [];
  tab.on('pageerror', e => errors.push(e.message));
  await tab.goto('file://' + page(appinventor));
  errors.push(...await tab.evaluate(load, readForm(scmPath), fs.readFileSync(bkyPath, 'utf8'),
    JSON.parse(fs.readFileSync(extensionsPath, 'utf8')), { ya: YA_VERSION, blocks: BLOCKS_VERSION }));
  return { browser, tab, errors };
}

module.exports = { open, readForm };
