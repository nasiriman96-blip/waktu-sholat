// Build PWA untuk Vercel: salin aplikasi ke _site, buat daftar cache offline, dan beri versi pada service worker.
const fs = require('fs'), path = require('path');
const SRC = path.join('app', 'src', 'main', 'assets'), OUT = '_site';
fs.rmSync(OUT, { recursive: true, force: true });
fs.cpSync(SRC, OUT, { recursive: true });

const files = [];
(function walk(d) {
  for (const f of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, f.name);
    if (f.isDirectory()) walk(p);
    else if (f.name !== 'sw.js' && f.name !== 'precache.json') files.push(path.relative(OUT, p).split(path.sep).join('/'));
  }
})(OUT);
fs.writeFileSync(path.join(OUT, 'precache.json'), JSON.stringify(['./', ...files]));

const ver = (process.env.VERCEL_GIT_COMMIT_SHA || String(Date.now())).slice(0, 8);
const sw = path.join(OUT, 'sw.js');
fs.writeFileSync(sw, fs.readFileSync(sw, 'utf8').replace('__VERSION__', ver));
console.log('PWA siap:', files.length, 'berkas, versi', ver);
