// 重新生成 7 张商品插画 SVG（手绘矢量风格，替代 emoji 占位图）
// 文件名保持不变，数据库路径无需变动
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, 'src', 'main', 'resources', 'static', 'images', 'products');

/** 通用页面骨架：浅色渐变背景 + 装饰圆 + 底部商品名 */
function page(tint1, tint2, inner, name) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
<defs>
  <radialGradient id="bg" cx="50%" cy="38%" r="75%">
    <stop offset="0%" stop-color="${tint1}"/><stop offset="100%" stop-color="${tint2}"/>
  </radialGradient>
  <filter id="soft" x="-40%" y="-40%" width="180%" height="180%">
    <feGaussianBlur stdDeviation="10"/>
  </filter>
</defs>
<rect width="800" height="600" fill="url(#bg)"/>
<circle cx="672" cy="96" r="82" fill="#ffffff" opacity="0.35"/>
<circle cx="96" cy="500" r="120" fill="#ffffff" opacity="0.28"/>
<circle cx="716" cy="452" r="42" fill="#ffffff" opacity="0.3"/>
<ellipse cx="400" cy="472" rx="152" ry="26" fill="#3d5c47" opacity="0.16" filter="url(#soft)"/>
${inner}
<text x="400" y="556" font-size="42" fill="#1b4d2e" text-anchor="middle"
  font-family="PingFang SC, Microsoft YaHei, sans-serif" font-weight="bold">${name}</text>
</svg>`;
}

const apple = `
<defs>
  <radialGradient id="abody" cx="35%" cy="30%" r="80%">
    <stop offset="0%" stop-color="#ff8a80"/><stop offset="55%" stop-color="#e53935"/><stop offset="100%" stop-color="#b71c1c"/>
  </radialGradient>
  <linearGradient id="leaf" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="#66bb6a"/><stop offset="100%" stop-color="#2e7d32"/>
  </linearGradient>
</defs>
<path d="M400 218 q14 -52 44 -70" stroke="#7a4b2a" stroke-width="14" fill="none" stroke-linecap="round"/>
<ellipse cx="470" cy="164" rx="52" ry="26" fill="url(#leaf)" transform="rotate(-24 470 164)"/>
<ellipse cx="452" cy="176" rx="52" ry="26" fill="#43a047" opacity="0.85" transform="rotate(-8 452 176)"/>
<path d="M400 200 C330 168 250 210 246 300 C242 390 300 452 400 462 C500 452 558 390 554 300 C550 210 470 168 400 200 Z" fill="url(#abody)"/>
<ellipse cx="330" cy="268" rx="42" ry="24" fill="#ffffff" opacity="0.38" transform="rotate(-32 330 268)"/>
<ellipse cx="472" cy="402" rx="26" ry="12" fill="#ffffff" opacity="0.12" transform="rotate(-18 472 402)"/>`;

const orange = `
<defs>
  <radialGradient id="obody" cx="36%" cy="30%" r="80%">
    <stop offset="0%" stop-color="#ffb74d"/><stop offset="55%" stop-color="#fb8c00"/><stop offset="100%" stop-color="#e65100"/>
  </radialGradient>
  <linearGradient id="oleaf" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="#66bb6a"/><stop offset="100%" stop-color="#2e7d32"/>
  </linearGradient>
</defs>
<ellipse cx="352" cy="352" rx="126" ry="122" fill="url(#obody)"/>
<ellipse cx="452" cy="352" rx="126" ry="122" fill="url(#obody)" opacity="0.96"/>
<g fill="#8d3b00" opacity="0.22">
  <circle cx="380" cy="330" r="4"/><circle cx="430" cy="300" r="4"/><circle cx="480" cy="330" r="4"/>
  <circle cx="350" cy="390" r="4"/><circle cx="405" cy="415" r="4"/><circle cx="470" cy="398" r="4"/>
  <circle cx="520" cy="370" r="4"/><circle cx="508" cy="290" r="4"/><circle cx="410" cy="270" r="4"/>
</g>
<circle cx="400" cy="242" r="13" fill="#8d6e63"/>
<ellipse cx="446" cy="212" rx="54" ry="26" fill="url(#oleaf)" transform="rotate(-18 446 212)"/>
<ellipse cx="372" cy="218" rx="44" ry="21" fill="#43a047" opacity="0.9" transform="rotate(10 372 218)"/>
<ellipse cx="330" cy="300" rx="40" ry="22" fill="#ffffff" opacity="0.35" transform="rotate(-30 330 300)"/>`;

const strawberry = `
<defs>
  <radialGradient id="sbody" cx="38%" cy="30%" r="80%">
    <stop offset="0%" stop-color="#ff7d6e"/><stop offset="55%" stop-color="#e53935"/><stop offset="100%" stop-color="#b71c1c"/>
  </radialGradient>
</defs>
<path d="M400 210 C480 210 528 268 516 344 C505 420 452 474 400 488 C348 474 295 420 284 344 C272 268 320 210 400 210 Z" fill="url(#sbody)"/>
<g fill="#ffe082" opacity="0.95">
  <ellipse cx="352" cy="286" rx="7" ry="10"/><ellipse cx="448" cy="286" rx="7" ry="10"/>
  <ellipse cx="400" cy="322" rx="7" ry="10"/><ellipse cx="316" cy="330" rx="7" ry="10"/><ellipse cx="484" cy="330" rx="7" ry="10"/>
  <ellipse cx="368" cy="376" rx="7" ry="10"/><ellipse cx="432" cy="376" rx="7" ry="10"/>
  <ellipse cx="400" cy="430" rx="7" ry="10"/><ellipse cx="344" cy="424" rx="7" ry="10"/><ellipse cx="456" cy="424" rx="7" ry="10"/>
</g>
<path d="M400 150 q-10 40 0 62" stroke="#7a4b2a" stroke-width="12" fill="none" stroke-linecap="round"/>
<g fill="#2e7d32">
  <path d="M400 232 C360 216 316 222 288 244 C322 254 366 252 400 240 Z"/>
  <path d="M400 232 C440 216 484 222 512 244 C478 254 434 252 400 240 Z"/>
  <path d="M400 236 C382 214 352 200 322 200 C342 222 372 236 400 242 Z"/>
  <path d="M400 236 C418 214 448 200 478 200 C458 222 428 236 400 242 Z"/>
  <path d="M400 210 C392 232 392 246 400 258 C408 246 408 232 400 210 Z" fill="#43a047"/>
</g>
<ellipse cx="348" cy="300" rx="30" ry="16" fill="#ffffff" opacity="0.3" transform="rotate(-34 348 300)"/>`;

const blueberry = `
<defs>
  <radialGradient id="bbody" cx="36%" cy="30%" r="80%">
    <stop offset="0%" stop-color="#8c9eff"/><stop offset="55%" stop-color="#5c6bc0"/><stop offset="100%" stop-color="#283593"/>
  </radialGradient>
</defs>
<circle cx="330" cy="308" r="86" fill="url(#bbody)"/>
<circle cx="470" cy="308" r="86" fill="url(#bbody)"/>
<circle cx="400" cy="382" r="98" fill="url(#bbody)"/>
<g fill="#1a237e" opacity="0.9">
  <path d="M400 330 l7 15 16 2 -12 11 3 16 -14 -8 -14 8 3 -16 -12 -11 16 -2 Z" transform="scale(0.9) translate(45 40)"/>
</g>
<g fill="#e8eaf6" opacity="0.4">
  <ellipse cx="304" cy="278" rx="26" ry="14" transform="rotate(-28 304 278)"/>
  <ellipse cx="446" cy="278" rx="24" ry="13" transform="rotate(22 446 278)"/>
  <ellipse cx="372" cy="344" rx="30" ry="15" transform="rotate(-20 372 344)"/>
</g>`;

const cherry = `
<defs>
  <radialGradient id="cbody" cx="36%" cy="30%" r="80%">
    <stop offset="0%" stop-color="#ff7043"/><stop offset="50%" stop-color="#e53935"/><stop offset="100%" stop-color="#8e0000"/>
  </radialGradient>
  <linearGradient id="cleaf" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="#66bb6a"/><stop offset="100%" stop-color="#2e7d32"/>
  </linearGradient>
</defs>
<path d="M338 320 C330 250 356 208 416 184" stroke="#5d4037" stroke-width="11" fill="none" stroke-linecap="round"/>
<path d="M474 306 C478 250 458 212 416 184" stroke="#5d4037" stroke-width="11" fill="none" stroke-linecap="round"/>
<ellipse cx="466" cy="172" rx="52" ry="25" fill="url(#cleaf)" transform="rotate(-20 466 172)"/>
<circle cx="332" cy="392" r="84" fill="url(#cbody)"/>
<circle cx="478" cy="378" r="76" fill="url(#cbody)"/>
<ellipse cx="302" cy="358" rx="30" ry="17" fill="#ffffff" opacity="0.35" transform="rotate(-30 302 358)"/>
<ellipse cx="456" cy="348" rx="24" ry="14" fill="#ffffff" opacity="0.35" transform="rotate(-26 456 348)"/>`;

const durian = `
<defs>
  <radialGradient id="dbody" cx="38%" cy="32%" r="80%">
    <stop offset="0%" stop-color="#e6ee9c"/><stop offset="55%" stop-color="#c0ca33"/><stop offset="100%" stop-color="#827717"/>
  </radialGradient>
</defs>
<g fill="#6d6413">
  <path d="M262 350 l-44 22 48 14 Z"/><path d="M538 350 l44 22 -48 14 Z"/>
  <path d="M300 440 l-26 40 46 -12 Z"/><path d="M500 440 l26 40 -46 -12 Z"/>
  <path d="M330 238 l-30 -36 44 16 Z"/><path d="M470 238 l30 -36 -44 16 Z"/>
  <path d="M400 224 l0 -44 18 40 Z"/>
</g>
<ellipse cx="400" cy="348" rx="152" ry="128" fill="url(#dbody)"/>
<g fill="#827717" opacity="0.75">
  <path d="M330 292 l14 -26 12 26 Z"/><path d="M390 268 l14 -26 12 26 Z"/><path d="M452 286 l14 -26 12 26 Z"/>
  <path d="M306 352 l14 -26 12 26 Z"/><path d="M366 336 l14 -26 12 26 Z"/><path d="M428 340 l14 -26 12 26 Z"/><path d="M488 352 l14 -26 12 26 Z"/>
  <path d="M336 412 l14 -26 12 26 Z"/><path d="M400 424 l14 -26 12 26 Z"/><path d="M462 410 l14 -26 12 26 Z"/>
</g>
<path d="M400 218 q4 -26 20 -38" stroke="#5d4037" stroke-width="12" fill="none" stroke-linecap="round"/>
<ellipse cx="336" cy="300" rx="36" ry="20" fill="#ffffff" opacity="0.28" transform="rotate(-30 336 300)"/>`;

const gift = `
<defs>
  <linearGradient id="gbox" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%" stop-color="#ef5350"/><stop offset="100%" stop-color="#c62828"/>
  </linearGradient>
  <linearGradient id="grid" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%" stop-color="#d84315"/><stop offset="100%" stop-color="#b71c1c"/>
  </linearGradient>
  <linearGradient id="gribbon" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%" stop-color="#ffe082"/><stop offset="100%" stop-color="#ffb300"/>
  </linearGradient>
</defs>
<rect x="286" y="316" width="228" height="150" rx="14" fill="url(#gbox)"/>
<rect x="270" y="282" width="260" height="46" rx="11" fill="url(#grid)"/>
<rect x="378" y="282" width="44" height="184" fill="url(#gribbon)"/>
<ellipse cx="352" cy="258" rx="42" ry="26" fill="url(#gribbon)" transform="rotate(-24 352 258)"/>
<ellipse cx="448" cy="258" rx="42" ry="26" fill="url(#gribbon)" transform="rotate(24 448 258)"/>
<circle cx="400" cy="266" r="17" fill="#ffca28"/>
<path d="M514 322 l52 -18 -6 40 Z" fill="#fff59d"/>
<circle cx="562" cy="336" r="6" fill="#ffb300"/>
<ellipse cx="322" cy="356" rx="34" ry="16" fill="#ffffff" opacity="0.22" transform="rotate(-14 322 356)"/>`;

const items = [
  ['aksu-apple', '阿克苏冰糖心苹果', '#fff3ef', '#ffdbd2', apple],
  ['gannan-orange', '赣南脐橙', '#fff7e8', '#ffe6bd', orange],
  ['dandong-strawberry', '丹东红颜草莓', '#fff1f2', '#ffd9dd', strawberry],
  ['yunnan-blueberry', '云南高山蓝莓', '#f2f4ff', '#dfe4ff', blueberry],
  ['chile-cherry', '智利车厘子 JJ级', '#fff2ee', '#ffdad1', cherry],
  ['thai-durian', '泰国金枕头榴莲', '#f9fbe6', '#eef3c3', durian],
  ['gift-box', '四季鲜果礼盒', '#fdf3f0', '#fadbd4', gift],
];

for (const [file, name, t1, t2, art] of items) {
  fs.writeFileSync(path.join(OUT, file + '.svg'), page(t1, t2, art, name));
}
console.log('regenerated', items.length, 'illustrated SVGs');
