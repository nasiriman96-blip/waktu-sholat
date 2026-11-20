/* Service worker: bisa dipakai offline. Hanya aktif saat dibuka lewat https (PWA), bukan di aplikasi Android. */
const CACHE='zikir-sholat-v__VERSION__';
self.addEventListener('install',e=>{
  e.waitUntil((async()=>{
    const c=await caches.open(CACHE);
    let list=['./','index.html','manifest.webmanifest','icon-192.png','icon-512.png'];
    try{const r=await fetch('precache.json',{cache:'no-store'});if(r.ok)list=await r.json()}catch(_){}
    await Promise.all(list.map(u=>c.add(u).catch(()=>{})));
    self.skipWaiting();
  })());
});
self.addEventListener('activate',e=>{
  e.waitUntil((async()=>{
    for(const k of await caches.keys())if(k!==CACHE)await caches.delete(k);
    await self.clients.claim();
  })());
});
self.addEventListener('fetch',e=>{
  const r=e.request;if(r.method!=='GET')return;
  e.respondWith((async()=>{
    const c=await caches.open(CACHE);
    const hit=await c.match(r,{ignoreSearch:true});
    const net=fetch(r).then(res=>{if(res&&(res.ok||res.type==='opaque'))c.put(r,res.clone());return res}).catch(()=>null);
    return hit||(await net)||new Response('Offline',{status:503});
  })());
});
