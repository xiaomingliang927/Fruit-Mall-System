// 后台菜单配置（单一数据源）
// - 分组对应「六大中心」结构；只展示已完成页面，M2/M3 功能完成后在对应组内追加
// - perm 为 RBAC 权限点编码预留，第二阶段权限系统直接复用
export const menuGroups = [
  {
    title: '数据中心',
    items: [
      { path: '/dashboard', title: '经营看板', icon: 'Odometer', perm: 'dashboard' },
      { path: '/revenue', title: '营业统计', icon: 'TrendCharts', perm: 'revenue' },
    ],
  },
  {
    title: '商品中心',
    items: [
      { path: '/products', title: '商品列表', icon: 'Goods', perm: 'product:list' },
      // 分类管理 / 库存预警 —— M2 完成后追加
    ],
  },
  {
    title: '订单中心',
    items: [
      { path: '/orders', title: '订单列表', icon: 'List', perm: 'order:list' },
      { path: '/refunds', title: '售后管理', icon: 'Headset', perm: 'refund:list' },
      // 售后管理 / 评价管理 —— M2 完成后追加
    ],
  },
  {
    title: '会员中心',
    items: [
      { path: '/members', title: '会员列表', icon: 'User', perm: 'member:list' },
      // 消费统计 / 标签与黑名单 —— M2 完成后追加
    ],
  },
  {
    title: '营销中心',
    items: [
      { path: '/coupons', title: '优惠券', icon: 'Ticket', perm: 'coupon:list' },
      { path: '/banners', title: '轮播图', icon: 'Picture', perm: 'banner:list' },
      // 秒杀拼团 —— M3 完成后追加
    ],
  },
  // 系统设置（账号角色 RBAC/运费模板/支付参数/操作日志）—— M2 完成后启用
]
