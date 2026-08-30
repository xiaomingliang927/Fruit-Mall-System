// 从 Wikimedia Commons 检索真实水果照片并下载到后端静态目录
// 用法：node fetch-product-photos.js
// 注意：Commons 图片为自由许可（CC 等），商用上线请替换为自有拍摄或已购商业图库照片
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, 'src', 'main', 'resources', 'static', 'images', 'products');
const UA = 'FruitMallDemo/0.1 (local development; contact: dev@fruitmall.local)';

const items = [
  ['aksu-apple', 'red apple fruit'],
  ['gannan-orange', 'orange fruit citrus'],
  ['dandong-strawberry', 'strawberries fruit'],
  ['yunnan-blueberry', 'blueberries'],
  ['chile-cherry', 'cherries fruit'],
  ['thai-durian', 'durian fruit'],
  ['gift-box', 'fruit basket'],
];

async function search(query) {
  const url = 'https://commons.wikimedia.org/w/api.php?action=query&format=json'
    + '&generator=search&gsrsearch=' + encodeURIComponent(query + ' filetype:bitmap')
    + '&gsrnamespace=6&gsrlimit=6&prop=imageinfo&iiprop=url%7Cmime&iiurlwidth=800';
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  const body = await res.json();
  const pages = Object.values((body.query || {}).pages || {});
  // 按搜索排序取第一张 JPEG 位图
  pages.sort((a, b) => (a.index || 99) - (b.index || 99));
  for (const p of pages) {
    const info = (p.imageinfo || [])[0];
    if (info && /image\/jpe?g/.test(info.mime) && info.thumburl) {
      return { url: info.thumburl, title: p.title };
    }
  }
  return null;
}

async function download(url, file) {
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error('HTTP ' + res.status);
  const buf = Buffer.from(await res.arrayBuffer());
  // JPEG 魔数校验
  if (buf[0] !== 0xff || buf[1] !== 0xd8) throw new Error('not a JPEG');
  fs.writeFileSync(file, buf);
  return buf.length;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  for (const [name, query] of items) {
    const out = path.join(OUT, name + '.jpg');
    try {
      const hit = await search(query);
      if (!hit) throw new Error('no jpeg result');
      const size = await download(hit.url, out);
      console.log(`OK  ${name}.jpg  ${(size / 1024).toFixed(0)}KB  <- ${hit.title}`);
    } catch (e) {
      console.log(`FAIL ${name}: ${e.message}`);
    }
  }
})();
