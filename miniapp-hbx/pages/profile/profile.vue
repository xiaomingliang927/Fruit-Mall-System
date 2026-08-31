<template>
	<view class="page">
		<view class="hd">个人资料</view>
		<view class="card">
			<view class="row">
				<text class="label">头像</text>
				<view class="avatar">{{ (me.nickname || '用')[0] }}</view>
			</view>
			<view class="row" @click="editNickname">
				<text class="label">昵称</text>
				<text class="value">{{ me.nickname || '-' }}</text>
				<text class="arrow">›</text>
			</view>
			<view class="row">
				<text class="label">手机号</text>
				<text class="value">{{ me.phone || '-' }}</text>
			</view>
			<view class="row">
				<text class="label">会员等级</text>
				<text class="value level">{{ levelText(me.level) }}</text>
			</view>
		</view>
		<view class="tip">头像功能后续接入图片上传</view>
	</view>
</template>

<script>
	import { api } from '../../api'

	export default {
		data() {
			return { me: {} }
		},
		onShow() {
			if (!uni.getStorageSync('token')) {
				return uni.navigateTo({ url: '/pages/login/login' })
			}
			this.load()
		},
		methods: {
			async load() {
				try {
					this.me = await api.get('/api/v1/users/me')
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			},
			levelText(level) {
				return { 1: '普通会员', 2: '白银会员', 3: '黄金会员', 4: '钻石会员' }[level] || '普通会员'
			},
			editNickname() {
				uni.showModal({
					title: '修改昵称',
					editable: true,
					placeholderText: '请输入昵称',
					content: this.me.nickname || '',
					success: async (r) => {
						if (!r.confirm) return
						const nickname = (r.content || '').trim()
						if (!nickname) return uni.showToast({ title: '昵称不能为空', icon: 'none' })
						try {
							await api.put('/api/v1/users/me', { nickname })
							uni.showToast({ title: '已保存', icon: 'none' })
							this.load()
						} catch (e) {
							uni.showToast({ title: e.message, icon: 'none' })
						}
					}
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }
	.hd { padding: 28rpx 32rpx; background: #fff; font-size: 34rpx; font-weight: 700; color: #222; }
	.card { background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 12rpx 28rpx; }
	.row {
		display: flex; align-items: center; padding: 26rpx 0;
		border-bottom: 1rpx solid #f0f2f4;
	}
	.row:last-child { border-bottom: none; }
	.label { width: 160rpx; flex: none; font-size: 28rpx; color: #555; }
	.value { flex: 1; text-align: right; font-size: 28rpx; color: #222; }
	.level { color: #17704a; font-weight: 600; }
	.arrow { margin-left: 12rpx; font-size: 30rpx; color: #999; }
	.avatar {
		margin-left: auto; width: 80rpx; height: 80rpx; border-radius: 50%;
		background: #17704a; color: #fff; font-size: 34rpx; font-weight: 700;
		display: flex; align-items: center; justify-content: center;
	}
	.tip { text-align: center; margin-top: 24rpx; font-size: 22rpx; color: #a5aca1; }
</style>
