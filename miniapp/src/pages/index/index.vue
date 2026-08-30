<template>
  <view class="page">
    <view class="search">
      <input class="search-input" v-model="keyword" placeholder="搜索水果：苹果 / 车厘子 / 草莓…" confirm-type="search" @confirm="search" />
      <view class="search-btn" @tap="search">搜索</view>
    </view>

    <scroll-view scroll-x class="cats">
      <view class="cat" :class="{ on: !categoryId }" @tap="pick(null)">全部</view>
      <view v-for="c in categories" :key="c.id" class="cat" :class="{ on: categoryId === c.id }" @tap="pick(c.id)">{{ c.name }}</view>
    </scroll-view>

    <view class="grid">
      <view v-for="p in products" :key="p.id" class="pcard" @tap="goDetail(p.id)">
        <image class="pimg" :src="imgUrl(p.mainImage)" mode="aspectFill" />
        <view class="pbody">
          <view class="pname">{{ p.name }}</view>
          <view class="psub">{{ p.subtitle }}</view>
          <view class="pfoot">
            <text class="price"><text class="rmb">¥</text>{{ yuan(p.minPrice) }}<text class="qi"> 起</text></text>
            <text class="sales">已售 {{ p.sales }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!products.length && !loading" class="empty">🔍 没有找到相关商品</view>
    <view v-if="finished && products.length" class="nomore">— 没有更多了 —</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { api, imgUrl, yuan } from '../../api'

const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const products = ref([])
const page = ref(1)
const size = 6
const finished = ref(false)
const loading = ref(false)

async function load(append = false) {
  if (loading.value) return
  loading.value = true
  try {
    const qs = new URLSearchParams({ page: page.value, size, keyword: keyword.value || '' })
    if (categoryId.value) qs.append('categoryId', categoryId.value)
    const data = await api.get(`/api/v1/products?${qs}`)
    products.value = append ? products.value.concat(data.records) : data.records
    finished.value = products.value.length >= data.total
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  } finally {
    loading.value = false
  }
}

function search() { page.value = 1; load() }
function pick(id) { categoryId.value = id; page.value = 1; load() }
function goDetail(id) { uni.navigateTo({ url: `/pages/detail/detail?id=${id}` }) }

onPullDownRefresh(async () => {
  page.value = 1
  await load()
  uni.stopPullDownRefresh()
})

// 触底加载下一页
import { onReachBottom } from '@dcloudio/uni-app'
onReachBottom(() => {
  if (!finished.value) { page.value += 1; load(true) }
})

load()
api.get('/api/v1/categories').then((d) => (categories.value = d))
</script>

<style scoped>
.search { display: flex; padding: 20rpx 24rpx; gap: 14rpx; }
.search-input {
  flex: 1; background: #fff; border-radius: 40rpx; padding: 16rpx 30rpx; font-size: 27rpx;
  border: 3rpx solid #2e9e5b;
}
.search-btn {
  background: #2e9e5b; color: #fff; border-radius: 40rpx; padding: 0 34rpx;
  display: flex; align-items: center; font-size: 27rpx;
}
.cats { white-space: nowrap; padding: 6rpx 24rpx 16rpx; }
.cat {
  display: inline-block; background: #fff; border-radius: 32rpx; padding: 12rpx 32rpx;
  margin-right: 14rpx; font-size: 26rpx; color: #374151;
}
.cat.on { background: #2e9e5b; color: #fff; font-weight: 600; }
.grid { display: flex; flex-wrap: wrap; padding: 0 18rpx; gap: 16rpx; }
.pcard {
  width: calc(50% - 8rpx); background: #fff; border-radius: 18rpx; overflow: hidden;
  box-shadow: 0 4rpx 14rpx rgba(30, 41, 59, 0.06);
}
.pimg { width: 100%; height: 320rpx; background: #f0f2ee; }
.pbody { padding: 16rpx 20rpx 20rpx; }
.pname { font-size: 29rpx; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.psub { font-size: 23rpx; color: #9ca3af; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pfoot { display: flex; justify-content: space-between; align-items: baseline; margin-top: 12rpx; }
.price { color: #dc2626; font-size: 33rpx; font-weight: 700; }
.rmb { font-size: 22rpx; }
.qi { font-size: 20rpx; font-weight: 400; }
.sales { color: #b6bcc6; font-size: 21rpx; }
.empty, .nomore { text-align: center; color: #9ca3af; padding: 60rpx 0 30rpx; font-size: 26rpx; }
</style>
