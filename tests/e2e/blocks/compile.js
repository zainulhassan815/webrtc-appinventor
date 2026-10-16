const fs = require('fs');
const { open, readForm } = require('./editor');

const USAGE = 'usage: compile.js <appinventor-dir> <Screen.scm> <Screen.bky> <components.json> <screen-class> <out.yail> [out.bky]';

async function main() {
  const [appinventor, scmPath, bkyPath, extensionsPath, screenClass, outYail, outBky] = process.argv.slice(2);
  if (!outYail) {
    console.error(USAGE);
    process.exit(2);
  }
  const { browser, tab, errors } = await open(appinventor, scmPath, bkyPath, extensionsPath);
  const result = await tab.evaluate((form, screenClass) => {
    const workspace = Blockly.common.getMainWorkspace();
    return {
      yail: AI.Yail.getFormYail(form, screenClass, false, workspace),
      bky: Blockly.SaveFile.get(true, workspace),
    };
  }, readForm(scmPath), screenClass);
  await browser.close();
  if (errors.length) {
    console.error(errors.join('\n'));
    process.exit(1);
  }
  fs.writeFileSync(outYail, result.yail);
  if (outBky) fs.writeFileSync(outBky, result.bky);
}

main();
