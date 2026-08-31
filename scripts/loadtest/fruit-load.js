// fruit-mall k6 压测脚本（方案 B）
// 前置：后端已启动于 :8080；每 vU 独立账号（13900000000 起，开发万能码 123456）
// 用法：
//   冒烟（1 vU，验证脚本与数据）: k6 run scripts/loadtest/fruit-load.js --env BASE=http://localhost:8080 --env SMOKE=1
//   完整阶梯:                       k6 run scripts/loadtest/fruit-load.js --env BASE=http://localhost:8080
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const SMOKE = __ENV.SMOKE === '1';
const STAGE = SMOKE ? { target: 1, dur: '10s' } : { target: 100, dur: '60s' };

export const options = {
  scenarios: {
    browse: {   // L0 只读热路径
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: STAGE.target / 4 },
        { duration: '60s', target: STAGE.target / 4 },
      ],
      exec: 'browse',
    },
    user_flow: { // L1 购物车 + L2 订单读写
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: STAGE.target / 4 },
        { duration: '60s', target: STAGE.target / 4 },
        { duration: '10s', target: 0 },
      ],
      exec: 'userFlow',
      startTime: '20s',
    },
  },
  thresholds: {
    // 开发机基线阈值（方案文档第四节），超阈值 k6 退出码非 0
    'http_req_duration{group:::browse}': ['p(95)<300'],
    'http_req_duration{group:::cart}': ['p(95)<500'],
    'http_req_duration{group:::order}': ['p(95)<800'],
    'http_req_failed': ['rate<0.005'],
  },
};

// 每 vU 绑定独立账号，登录一次复用 token
function setup() {
  return { base: BASE };
}

function login(vuIndex) {
  const phone = String(13900000000 + vuIndex);
  const res = http.post(`${BASE}/api/v1/auth/sms-login`, JSON.stringify({ phone, code: '123456' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { '登录成功': (r) => r.status === 200 });
  // ApiResponse 结构：{ code, message, data: { token, ... } }
  const body = res.json();
  return body.data && (body.data.token || body.data.accessToken);
}

export function browse(data) {
  group('browse', () => {
    check(http.get(`${BASE}/api/v1/products?page=1&size=10`), {
      '商品列表 200': (r) => r.status === 200,
    });
    check(http.get(`${BASE}/api/v1/products/1`), { '商品详情 200': (r) => r.status === 200 });
    check(http.get(`${BASE}/api/v1/categories`), { '分类 200': (r) => r.status === 200 });
    check(http.get(`${BASE}/api/v1/banners`), { 'banner 200': (r) => r.status === 200 });
  });
  sleep(1);
}

export function userFlow(data) {
  const vu = __VU;
  const token = login(vu);
  if (!token) { sleep(1); return; }
  const auth = { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } };

  group('cart', () => {
    check(http.get(`${BASE}/api/v1/cart`, auth), { '购物车 200': (r) => r.status === 200 });
    check(
      http.post(`${BASE}/api/v1/cart/items`, JSON.stringify({ skuId: 1, quantity: 1 }), auth),
      { '加购 200': (r) => r.status === 200 }
    );
  });

  group('order', () => {
    // 需先为该账号造收货地址（见压测方案第五节第 3 条），否则会因 addressId 校验失败返回业务错误
    const addr = http.post(
      `${BASE}/api/v1/users/me/addresses`,
      JSON.stringify({ receiverName: `压测${vu}`, receiverPhone: '13900000000', province: '广东省', city: '深圳市', district: '南山区', detail: '压测地址 1 号' }),
      auth
    );
    const addressId = addr.status === 200 ? addr.json('data') : null;
    if (addressId) {
      const res = http.post(
        `${BASE}/api/v1/orders`,
        JSON.stringify({ addressId, items: [{ skuId: 1, quantity: 1 }] }),
        auth
      );
      // 库存不足等是正常业务响应，不算 http_req_failed
      check(res, { '下单有响应': (r) => r.status === 200 || r.status === 400 || r.status === 409 });
    }
    check(http.get(`${BASE}/api/v1/orders?page=1&size=10`, auth), { '订单列表 200': (r) => r.status === 200 });
  });

  sleep(1);
}
