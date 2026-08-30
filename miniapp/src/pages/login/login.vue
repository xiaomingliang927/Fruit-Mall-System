<template>
  <view class="wrap">
    <view class="logo">🍊</view>
    <view class="t1">鲜果集</view>
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

const phone = ref('')
const code = ref('123456')

async function wxLogin() {
  uni.login({
    provider: 'weixin',
    success: async ({ code }) => {
      try {
        const data = await api.post('/api/v1/auth/wx-login', { code })
        setToken(data.token)
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
    uni.showToast({ title: `欢迎，${data.nickname}`, icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 700)
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  }
}
</script>

<style scoped>
.wrap { display: flex; flex-direction: column; align-items: center; padding: 120rpx 70rpx 0; }
.logo {
  width: 150rpx; height: 150rpx; border-radius: 44rpx; font-size: 84rpx; text-align: center; line-height: 150rpx;
  background: linear-gradient(135deg, #1f7a3d, #2e9e5b); box-shadow: 0 14rpx 34rpx rgba(31, 122, 61, 0.3);
}
.t1 { font-size: 44rpx; font-weight: 800; margin-top: 30rpx; letter-spacing: 4rpx; }
.t2 { color: #9ca3af; font-size: 25rpx; margin-top: 10rpx; letter-spacing: 2rpx; }
.wxbtn {
  margin-top: 90rpx; width: 100%; background: #07c160; color: #fff; border-radius: 48rpx;
  font-size: 31rpx; font-weight: 700;
}
.divider { color: #c8ccd4; font-size: 23rpx; margin: 50rpx 0 34rpx; }
.in {
  width: 100%; border: 3rpx solid #e5e7eb; border-radius: 44rpx; padding: 20rpx 36rpx;
  font-size: 28rpx; margin-bottom: 22rpx;
}
.smsbtn {
  width: 100%; background: #2e9e5b; color: #fff; border-radius: 48rpx; font-size: 31rpx; font-weight: 700;
}
.hint { color: #b6bcc6; font-size: 21rpx; text-align: center; line-height: 1.9; margin-top: 44rpx; }
</style>
