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
      { path: '/reviews', title: '评价管理', icon: 'ChatDotRound', perm: 'review:list' }
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
  {
    title: '系统设置',
    items: [
      { path: '/roles', title: '角色权限', icon: 'UserFilled', perm: 'role:list' },
      // 操作日志 / 支付参数 / 运费模板 —— 后续追加
    ],
  },
]

/**
 * 按当前账号的权限点过滤菜单：
 * - 未取到权限（老 token）或超级管理员 → 全展示；
 * - 其余账号隐藏无权限的菜单项，空分组整体不显示。
 */
export function visibleMenuGroups(perms) {
  const owned = Array.isArray(perms) ? perms : []
  if (!owned.length) return menuGroups
  return menuGroups
    .map((g) => ({ ...g, items: g.items.filter((i) => !i.perm || owned.includes(i.perm)) }))
    .filter((g) => g.items.length)
}
