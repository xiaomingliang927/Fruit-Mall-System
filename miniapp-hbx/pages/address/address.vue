<template>
	<view class="page">
		<view class="hd">
			<text class="hd-title">收货地址</text>
			<text class="hd-add" @click="toggleForm">{{ showForm ? '收起' : '新增地址' }}</text>
		</view>

		<!-- 新增表单 -->
		<view v-if="showForm" class="form-card">
			<view class="fld">
				<text class="lbl">收货人</text>
				<input class="inp" v-model.trim="form.receiver" placeholder="请输入姓名" />
			</view>
			<view class="fld">
				<text class="lbl">手机号</text>
				<input class="inp" v-model.trim="form.phone" maxlength="11" type="number" placeholder="请输入手机号" />
			</view>
			<view class="fld">
				<text class="lbl">省份</text>
				<input class="inp" v-model.trim="form.province" placeholder="如：广东省" />
			</view>
			<view class="fld">
				<text class="lbl">城市</text>
				<input class="inp" v-model.trim="form.city" placeholder="如：深圳市" />
			</view>
			<view class="fld">
				<text class="lbl">区/县</text>
				<input class="inp" v-model.trim="form.district" placeholder="如：南山区" />
			</view>
			<view class="fld">
				<text class="lbl">详细地址</text>
				<input class="inp" v-model.trim="form.detail" placeholder="街道、门牌号" />
			</view>
			<view class="fld fld-nob">
				<text class="lbl">设为默认</text>
				<switch :checked="form.isDefault" color="#17704a" @change="form.isDefault = $event.detail.value" />
			</view>
			<button class="save-btn" @click="save">保存地址</button>
		</view>

		<!-- 列表 -->
		<view v-if="!addresses.length && !loading" class="empty">还没有收货地址，点击右上角新增</view>
		<view v-for="a in addresses" :key="a.id" class="addr-card">
			<view class="addr-top">
				<text class="addr-name">{{ a.receiver }}</text>
				<text class="addr-phone">{{ a.phone }}</text>
				<text v-if="a.isDefault" class="tag">默认</text>
			</view>
			<view class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</view>
			<view class="addr-ops">
				<text class="op op-danger" @click="remove(a)">删除</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { api } from '../../api'

	export default {
		data() {
			return {
				addresses: [],
				loading: true,
				showForm: false,
				form: this.blankForm()
			}
		},
		onShow() {
			if (!uni.getStorageSync('token')) {
				return uni.navigateTo({ url: '/pages/login/login' })
			}
			this.load()
		},
		methods: {
			blankForm() {
				return { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
			},
			toggleForm() {
				this.showForm = !this.showForm
				if (!this.showForm) this.form = this.blankForm()
			},
			async load() {
				try {
					this.addresses = await api.get('/api/v1/users/me/addresses')
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			async save() {
				const f = this.form
				if (!f.receiver || !f.phone || !f.province || !f.city || !f.detail) {
					return uni.showToast({ title: '请完整填写收货信息', icon: 'none' })
				}
				try {
					// 第一条地址自动设为默认（后端未提供修改接口，新增时处理）
					const payload = { ...f, isDefault: this.addresses.length === 0 ? true : f.isDefault }
					await api.post('/api/v1/users/me/addresses', payload)
					uni.showToast({ title: '已保存', icon: 'none' })
					this.showForm = false
					this.form = this.blankForm()
					this.load()
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				}
			},
			remove(a) {
				uni.showModal({
					title: '删除地址',
					content: `确定删除「${a.receiver} ${a.phone}」吗？`,
					confirmText: '删除',
					confirmColor: '#e54d42',
					success: async (r) => {
						if (!r.confirm) return
						try {
							await api.delete(`/api/v1/users/me/addresses/${a.id}`)
							uni.showToast({ title: '已删除', icon: 'none' })
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
	.hd {
		display: flex; align-items: center; justify-content: space-between;
		padding: 28rpx 32rpx; background: #fff;
	}
	.hd-title { font-size: 34rpx; font-weight: 700; color: #222; }
	.hd-add { font-size: 26rpx; color: #17704a; font-weight: 600; }
	.empty { text-align: center; color: #a5aca1; padding-top: 180rpx; font-size: 26rpx; }
	.form-card {
		background: #fff; margin: 20rpx 24rpx; border-radius: 24rpx; padding: 12rpx 28rpx 28rpx;
	}
	.fld {
		display: flex; align-items: center; padding: 24rpx 0;
		border-bottom: 1rpx solid #f0f2f4;
	}
	.fld-nob { border-bottom: none; }
	.lbl { width: 150rpx; flex: none; font-size: 28rpx; color: #333; }
	.inp { flex: 1; font-size: 28rpx; color: #222; }
	.save-btn {
		margin-top: 28rpx; background: linear-gradient(90deg, #35b47e, #1f8a58);
		color: #fff; font-size: 30rpx; font-weight: 600; border-radius: 44rpx;
		line-height: 84rpx;
	}
	.addr-card {
		background: #fff; margin: 20rpx 24rpx 0; border-radius: 24rpx; padding: 28rpx;
	}
	.addr-top { display: flex; align-items: center; gap: 16rpx; }
	.addr-name { font-size: 30rpx; font-weight: 600; color: #222; }
	.addr-phone { font-size: 26rpx; color: #888; }
	.tag {
		font-size: 20rpx; color: #17704a; background: #e6f4ec;
		padding: 4rpx 14rpx; border-radius: 14rpx; margin-left: auto;
	}
	.addr-detail { margin-top: 14rpx; font-size: 26rpx; color: #555; line-height: 1.5; }
	.addr-ops {
		display: flex; justify-content: flex-end; margin-top: 18rpx;
		border-top: 1rpx solid #f0f2f4; padding-top: 18rpx;
	}
	.op { font-size: 26rpx; color: #555; }
	.op-danger { color: #e54d42; font-weight: 600; }
</style>
