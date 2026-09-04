<template>
  <view class="wrap">
    <view class="mark">鲜</view>
    <view class="t1">果上选</view>
    <view class="t2">产地直采 · 新鲜到家</view>

    <button class="wxbtn" @tap="wxLogin">微信一键登录</button>

    <view class="divider"><text>或使用手机号登录（演示）</text></view>

    <input class="in" v-model="phone" type="number" maxlength="11" placeholder="手机号" />
    <input class="in" v-model="code" maxlength="6" placeholder="验证码（演示环境固定 123456）" />
    <button class="smsbtn" @tap="smsLogin">登录 / 注册</button>

    <view class="hint">
      微信登录：开发模式后端自动派生 openid，无需真实 AppID<br />
      真实上线前在 backend 配置 app.wechat.appid/secret
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { api, setToken } from '../../api'
import { pushLocalToServer, syncFromServer, updateCartBadge } from '../../utils/cart.js'

const phone = ref('')
const code = ref('123456')

async function wxLogin() {
  uni.login({
    provider: 'weixin',
    success: async ({ code }) => {
      try {
        const data = await api.post('/api/v1/auth/wx-login', { code })
        setToken(data.token)
		await pushLocalToServer()
		syncFromServer().then(updateCartBadge)
        uni.showToast({ title: `欢迎，${data.nickname}`, icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 700)
      } catch (e) {
        uni.showToast({ title: e.message, icon: 'none' })
      }
    },
    fail: () => uni.showToast({ title: '微信登录调用失败', icon: 'none' }),
  })
}

async function smsLogin() {
  if (!/^1\d{10}$/.test(phone.value)) {
    return uni.showToast({ title: '请输入 11 位手机号', icon: 'none' })
  }
  try {
    const data = await api.post('/api/v1/auth/sms-login', { phone: phone.value, code: code.value })
    setToken(data.token)
    await pushLocalToServer()
    syncFromServer().then(updateCartBadge)
    uni.showToast({ title: `欢迎，${data.nickname}`, icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 700)
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  }
}
</script>

<style scoped>
.wrap { display: flex; flex-direction: column; align-items: center; padding: 120rpx 70rpx 0; }
.mark {
  width: 130rpx; height: 130rpx; border-radius: 24rpx; font-size: 60rpx; font-weight: 700;
  color: #fff; text-align: center; line-height: 130rpx;
  background: #1f7a3d;
}
.t1 { font-size: 42rpx; font-weight: 700; margin-top: 28rpx; letter-spacing: 3rpx; color: #1f2937; }
.t2 { color: #9ca3af; font-size: 24rpx; margin-top: 10rpx; letter-spacing: 1rpx; }
.wxbtn {
  margin-top: 90rpx; width: 100%; background: #07c160; color: #fff; border-radius: 8rpx;
  font-size: 30rpx; font-weight: 600;
}
.divider { color: #c8ccd4; font-size: 23rpx; margin: 50rpx 0 34rpx; }
.in {
  width: 100%; border: 2rpx solid #e5e7eb; border-radius: 8rpx; padding: 20rpx 30rpx;
  font-size: 28rpx; margin-bottom: 22rpx;
}
.smsbtn {
  width: 100%; background: #1f7a3d; color: #fff; border-radius: 8rpx; font-size: 30rpx; font-weight: 600;
}
.hint { color: #b6bcc6; font-size: 21rpx; text-align: center; line-height: 1.9; margin-top: 44rpx; }
</style>
