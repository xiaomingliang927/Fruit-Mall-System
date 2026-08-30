// 生成底部 TabBar 线性图标 PNG（普通灰 / 选中深绿）
// 纯 Node 实现：3x 超采样光栅化 + 自写 PNG 编码器，无第三方依赖
// 用法：node scripts/gen-tab-icons.js
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'src', 'static', 'tab');
const SIZE = 81;            // 微信 TabBar 推荐尺寸
const SS = 3;               // 超采样倍数
const CANVAS = SIZE * SS;   // 243
const COLOR = { gray: [165, 172, 161], green: [23, 112, 74] };

// ---------- 极简画布 ----------
function makeCanvas() {
  return { w: CANVAS, h: CANVAS, px: new Float32Array(CANVAS * CANVAS) };
}
function stamp(cv, cx, cy, r) {
  const x0 = Math.max(0, Math.floor(cx - r)), x1 = Math.min(cv.w - 1, Math.ceil(cx + r));
  const y0 = Math.max(0, Math.floor(cy - r)), y1 = Math.min(cv.h - 1, Math.ceil(cy + r));
  for (let y = y0; y <= y1; y++) {
    for (let x = x0; x <= x1; x++) {
      const dx = x + 0.5 - cx, dy = y + 0.5 - cy;
      if (dx * dx + dy * dy <= r * r) cv.px[y * cv.w + x] = 1;
    }
  }
}
// 在 24 视窗坐标系画粗线（圆帽）
function line(cv, x1, y1, x2, y2, w = 1.9) {
  const s = SS * (SIZE / 24), r = (w / 2) * s;
  const ax = x1 * s, ay = y1 * s, bx = x2 * s, by = y2 * s;
  const dist = Math.hypot(bx - ax, by - ay);
  const steps = Math.max(1, Math.ceil(dist / (r * 0.5)));
  for (let i = 0; i <= steps; i++) {
    stamp(cv, ax + ((bx - ax) * i) / steps, ay + ((by - ay) * i) / steps, r);
  }
}
function circle(cv, cx, cy, r24, w = 1.9) {
  const s = SS * (SIZE / 24);
  const steps = Math.max(24, Math.ceil(2 * Math.PI * r24 * s / (s * 0.5)));
  for (let i = 0; i < steps; i++) {
    const a = (i / steps) * Math.PI * 2;
    stamp(cv, cx * s + Math.cos(a) * r24 * s, cy * s + Math.sin(a) * r24 * s, (w / 2) * s);
  }
}
function strokePoly(cv, pts, close = false, w = 1.9) {
  for (let i = 0; i < pts.length - 1; i++) line(cv, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], w);
  if (close) line(cv, pts[pts.length - 1][0], pts[pts.length - 1][1], pts[0][0], pts[0][1], w);
}
// 3x 降采样（盒式滤波 → 自带抗锯齿）
function downsample(cv) {
  const out = new Uint8Array(SIZE * SIZE * 4);
  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      let sum = 0;
      for (let dy = 0; dy < SS; dy++) for (let dx = 0; dx < SS; dx++) sum += cv.px[(y * SS + dy) * cv.w + (x * SS + dx)];
      const a = Math.round((sum / (SS * SS)) * 255);
      const o = (y * SIZE + x) * 4;
      out[o] = 0; out[o + 1] = 0; out[o + 2] = 0; out[o + 3] = a;
    }
  }
  return out;
}

// ---------- PNG 编码 ----------
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (const b of buf) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}
function encodePNG(rgba, w, h, rgb) {
  const raw = Buffer.alloc(h * (w * 4 + 1));
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0; // filter: none
    for (let x = 0; x < w; x++) {
      const si = (y * w + x) * 4, di = y * (w * 4 + 1) + 1 + x * 4;
      raw[di] = rgb[0]; raw[di + 1] = rgb[1]; raw[di + 2] = rgb[2]; raw[di + 3] = rgba[si + 3];
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// ---------- 四个图标（24 视窗坐标，Feather 风格） ----------
function drawHome(cv) {
  strokePoly(cv, [[3, 11], [12, 3], [21, 11]]);
  strokePoly(cv, [[5, 10], [5, 20], [10, 20], [10, 14], [14, 14], [14, 20], [19, 20], [19, 10]]);
}
function drawCategory(cv) {
  for (const [x, y] of [[3, 3], [14, 3], [3, 14], [14, 14]]) {
    strokePoly(cv, [[x + 1.2, y], [x + 6.8, y]], false);
    strokePoly(cv, [[x + 8, y + 1.2], [x + 8, y + 6.8]], false);
    strokePoly(cv, [[x + 6.8, y + 8], [x + 1.2, y + 8]], false);
    strokePoly(cv, [[x, y + 6.8], [x, y + 1.2]], false);
    circle(cv, x + 1.2, y + 1.2, 1.2, 0.5); circle(cv, x + 6.8, y + 1.2, 1.2, 0.5);
    circle(cv, x + 1.2, y + 6.8, 1.2, 0.5); circle(cv, x + 6.8, y + 6.8, 1.2, 0.5);
    circle(cv, x + 1.2, y, 1.2, 1.9); circle(cv, x + 6.8, y, 1.2, 1.9);
    circle(cv, x + 1.2, y + 8, 1.2, 1.9); circle(cv, x + 6.8, y + 8, 1.2, 1.9);
    circle(cv, x, y + 1.2, 1.2, 1.9); circle(cv, x + 8, y + 1.2, 1.2, 1.9);
    circle(cv, x, y + 6.8, 1.2, 1.9); circle(cv, x + 8, y + 6.8, 1.2, 1.9);
  }
}
function drawCart(cv) {
  strokePoly(cv, [[2, 2], [5.2, 2], [7.6, 14.5], [18.5, 14.5], [21, 6]]);
  circle(cv, 9.5, 19.5, 1.6);
  circle(cv, 17, 19.5, 1.6);
}
function drawMine(cv) {
  circle(cv, 12, 7, 4);
  strokePoly(cv, [[20, 21], [20, 19], [19, 17], [17, 15.6], [15, 15], [9, 15], [7, 15.6], [5, 17], [4, 19], [4, 21]]);
}

const icons = { home: drawHome, category: drawCategory, cart: drawCart, mine: drawMine };
fs.mkdirSync(OUT, { recursive: true });
for (const [name, draw] of Object.entries(icons)) {
  for (const [suffix, colorKey] of [['', 'gray'], ['-on', 'green']]) {
    const cv = makeCanvas();
    draw(cv);
    const rgba = downsample(cv);
    const file = path.join(OUT, `${name}${suffix}.png`);
    fs.writeFileSync(file, encodePNG(rgba, SIZE, SIZE, COLOR[colorKey]));
    console.log('written', path.basename(file));
  }
}
