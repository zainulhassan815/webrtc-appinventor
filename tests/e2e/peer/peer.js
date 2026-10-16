const path = require('path');
const puppeteer = require('puppeteer');

const USAGE = 'usage: peer.js <room> <peer-id> <seconds>, with SUPABASE_URL and SUPABASE_KEY set';

async function call({ url, key, room, id, seconds }) {
  const media = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
  const { connection, hangUp } = startCall({
    client: supabase.createClient(url, key), room, id, media,
    report: line => console.log(line), showRemote: () => {}, ended: () => {},
  });
  let connected = false;
  for (const deadline = Date.now() + seconds * 1000; Date.now() < deadline;) {
    await new Promise(resolve => setTimeout(resolve, 1000));
    connected ||= connection.connectionState === 'connected';
  }
  const frames = { received: 0, sent: 0 };
  (await connection.getStats()).forEach(stat => {
    if (stat.kind !== 'video') return;
    if (stat.type === 'inbound-rtp') frames.received = stat.framesDecoded;
    if (stat.type === 'outbound-rtp') frames.sent = stat.framesEncoded;
  });
  await hangUp();
  return { connected, frames };
}

async function main() {
  const [room, id, seconds] = process.argv.slice(2);
  const { SUPABASE_URL: url, SUPABASE_KEY: key } = process.env;
  if (!seconds || !url || !key) {
    console.error(USAGE);
    process.exit(2);
  }
  const browser = await puppeteer.launch({
    args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream', '--allow-file-access-from-files', '--no-sandbox'],
  });
  const tab = await browser.newPage();
  tab.on('console', message => console.log(message.text()));
  await tab.goto('file://' + path.join(__dirname, 'peer.html'));
  const result = await tab.evaluate(call, { url, key, room, id, seconds: Number(seconds) });
  await browser.close();
  console.log(JSON.stringify({ connected: result.connected, frames: result.frames }));
  process.exit(result.connected && result.frames.received > 0 && result.frames.sent > 0 ? 0 : 1);
}

main();
