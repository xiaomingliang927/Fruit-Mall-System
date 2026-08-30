// 生成近7天演示订单 SQL：让仪表板趋势图/状态分布有真实可看的数据
// 用法：node gen-demo-data.js  →  输出 docs/sql/demo-data.sql，再导入 MySQL
const fs = require('fs');
const path = require('path');

const skus = [
  { id: 1, price: 3990, spec: '5斤装', pid: 1, name: '阿克苏冰糖心苹果', img: '/images/products/aksu-apple.svg' },
  { id: 3, price: 4990, spec: '9斤装', pid: 2, name: '赣南脐橙', img: '/images/products/gannan-orange.svg' },
  { id: 4, price: 3990, spec: '1盒约1斤', pid: 3, name: '丹东红颜草莓', img: '/images/products/dandong-strawberry.svg' },
  { id: 6, price: 4990, spec: '125g×4盒', pid: 4, name: '云南高山蓝莓', img: '/images/products/yunnan-blueberry.svg' },
  { id: 7, price: 12900, spec: '2斤装', pid: 5, name: '智利车厘子 JJ级', img: '/images/products/chile-cherry.svg' },
  { id: 9, price: 9900, spec: '4-5斤整果', pid: 6, name: '泰国金枕头榴莲', img: '/images/products/thai-durian.svg' },
  { id: 10, price: 19900, spec: '十二层礼盒', pid: 7, name: '四季鲜果礼盒', img: '/images/products/gift-box.svg' },
];
const addr = JSON.stringify({ receiver: '张三', phone: '13800001111', province: '广东省', city: '深圳市', district: '南山区', detail: '科技园南路88号' });
const addrSql = addr.replace(/"/g, '\\"');

// 16 单：过去6天每天2单 + 今天2单；状态以已完成为主
const plan = [];
for (let d = 6; d >= 1; d--) {
  plan.push({ daysAgo: d, status: 40 });
  plan.push({ daysAgo: d, status: d % 3 === 0 ? 30 : 40 });
}
plan.push({ daysAgo: 0, status: 20 });
plan.push({ daysAgo: 0, status: 40 });

let sql = '-- 演示数据：近7日订单（仅用于仪表板展示，生产环境请勿执行）\nUSE fruit_mall;\n';
let seq = 1001;
for (const p of plan) {
  const itemCount = seq % 3 === 0 ? 2 : 1;
  const picks = [];
  let cursor = seq % skus.length;
  while (picks.length < itemCount) {
    const s = skus[cursor % skus.length];
    if (!picks.includes(s)) picks.push(s);
    cursor += 3;
  }
  let total = 0;
  const lines = picks.map((s) => {
    const qty = (seq % 2) + 1;
    total += s.price * qty;
    return { s, qty };
  });
  const orderNo = 'FM' + String(26080000 + seq * 7);
  seq++;
  const cancelled = p.status === 50;
  const hour = 9 + (seq % 10);
  const minute = (seq * 13) % 60;
  const timeExpr = (daysAgo, h, m) =>
    `DATE_SUB(NOW(), INTERVAL ${daysAgo} DAY) + INTERVAL ${h * 3600 + m * 60} SECOND`;
  sql += `INSERT INTO \`order\` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('${orderNo}',1,${p.status},${total},${cancelled ? 0 : total},0,'${addrSql}',1,'演示数据',
${cancelled ? 'NULL' : timeExpr(p.daysAgo, hour, minute)},
${timeExpr(p.daysAgo, hour, Math.max(0, minute - 5))});\n`;
  const v = '@o' + seq;
  sql += `SET ${v} = LAST_INSERT_ID();\n`;
  for (const { s, qty } of lines) {
    sql += `INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (${v},${s.pid},${s.id},'${s.name}','${s.spec}','${s.img}',${s.price},${qty},${s.price * qty});\n`;
  }
  if (!cancelled) {
    sql += `INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('${orderNo}','MOCK-DEMO-${seq}','MOCK',${total},1,${timeExpr(p.daysAgo, hour, minute)});\n`;
  }
}

const out = path.join(__dirname, '..', 'docs', 'sql', 'demo-data.sql');
fs.writeFileSync(out, sql);
console.log('written:', out, '| orders:', plan.length);
