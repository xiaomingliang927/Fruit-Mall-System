<template>
	<view class="page">
		<view class="card">
			<view class="h">售后订单</view>
			<view class="order-line">
				<text class="ono">{{ orderNo }}</text>
				<text class="oamt">¥{{ yuan(amount) }}</text>
			</view>
			<view class="tip">售后金额为整单实付金额；≤50 元自动秒审退款（坏果包赔），超额进入人工审核</view>
		</view>

		<view class="card">
			<view class="h">售后类型</view>
			<view class="types">
				<view class="tp" :class="{ on: type === 1 }" @tap="type = 1">仅退款<text class="tp-s">未收到货 / 坏果包赔</text></view>
				<view class="tp" :class="{ on: type === 2 }" @tap="type = 2">退货退款<text class="tp-s">已收到货，需寄回</text></view>
			</view>
		</view>

		<view class="card">
			<view class="h">申请原因</view>
			<textarea class="reason" v-model="reason" maxlength="200" placeholder="请填写申请原因（必填），如：坏果 / 腐烂 / 少件…" />
		</view>

		<view class="card">
			<view class="h">凭证图片<text class="h-s">选填，最多 3 张</text></view>
			<view class="imgs">
				<view class="ig" v-for="(img, i) in images" :key="img">
					<image class="ig-img" :src="imgUrl(img)" mode="aspectFill" @tap="preview(i)" />
					<view class="ig-del" @tap="del(i)">×</view>
				</view>
				<view v-if="images.length < 3" class="ig-add" :class="{ off: uploading }" @tap="choose">
					<text class="ig-plus">＋</text>
					<text class="ig-txt">{{ uploading ? '上传中' : '上传' }}</text>
				</view>
			</view>
			<view class="tip">坏果 / 少件请拍照上传，单张 ≤5MB，客服可在后台直接查看凭证</view>
		</view>

		<view class="bar">
			<view class="submit" :class="{ off: submitting }" @tap="submit">{{ submitting ? '提交中…' : '提交申请' }}</view>
		</view>
	</view>
</template>

<script>
	import { api, yuan, imgUrl, uploadImage, getBaseUrl } from '../../api'

	export default {
		data() {
			return {
				orderNo: '',
				amount: 0,
				type: 1,
				reason: '',
				images: [],
				uploading: false,
				submitting: false
			}
		},
		onLoad(opt) {
			this.orderNo = opt.orderNo || ''
			this.amount = Number(opt.amount) || 0
		},
		methods: {
			imgUrl,
			choose() {
				if (this.uploading) return
				const left = 3 - this.images.length
				if (left <= 0) return uni.showToast({ title: '最多上传 3 张', icon: 'none' })
				uni.chooseImage({
					count: left,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => this.uploadAll(res.tempFilePaths || [])
				})
			},
			async uploadAll(paths) {
				if (!paths.length) return
				this.uploading = true
				uni.showLoading({ title: '上传中…', mask: true })
				try {
					for (const p of paths) {
						if (this.images.length >= 3) break
						this.images.push(await uploadImage(p))
					}
				} catch (e) {
					uni.showToast({ title: e.message || '上传失败', icon: 'none' })
				} finally {
					uni.hideLoading()
					this.uploading = false
				}
			},
			del(i) {
				this.images.splice(i, 1)
			},
			preview(i) {
				uni.previewImage({ current: i, urls: this.images.map((p) => imgUrl(p)) })
			},
			chooseImage() {
				const left = 3 - this.images.length
				if (left <= 0) return
				uni.chooseImage({
					count: left,
					sizeType: ['compressed'],
					success: (res) => {
						for (const path of res.tempFilePaths) this.upload(path)
					}
				})
			},
			upload(path) {
				uni.uploadFile({
					url: getBaseUrl() + '/api/v1/upload/image',
					filePath: path,
					name: 'file',
					header: { Authorization: 'Bearer ' + (uni.getStorageSync('token') || '') },
					success: (res) => {
						const body = JSON.parse(res.data)
						if (body.code === 0) this.images.push(body.data.url)
						else uni.showToast({ title: body.message || '上传失败', icon: 'none' })
					},
					fail: () => uni.showToast({ title: '上传失败', icon: 'none' })
				})
			},
			async submit() {
				if (!this.reason.trim()) {
					return uni.showToast({ title: '请填写申请原因', icon: 'none' })
				}
				if (this.uploading) {
					return uni.showToast({ title: '凭证上传中，请稍候', icon: 'none' })
				}
				this.submitting = true
				try {
					const data = await api.post('/api/v1/refunds', {
						orderNo: this.orderNo,
						type: this.type,
						reason: this.reason.trim(),
						images: this.images
					})
					uni.showToast({
						title: data.statusText === '已退款' ? '审核通过，退款原路返回' : '已提交，等待审核',
						icon: 'success'
					})
					setTimeout(() => uni.navigateBack(), 900)
				} catch (e) {
					uni.showToast({ title: e.message, icon: 'none' })
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx 24rpx 180rpx; }
	.card { background: #fff; border-radius: 20rpx; padding: 26rpx; margin-bottom: 20rpx; border: 1rpx solid #eceef0; }
	.h { font-size: 29rpx; font-weight: 700; margin-bottom: 16rpx; }
	.h-s { font-size: 21rpx; color: #a5aca1; font-weight: 400; margin-left: 10rpx; }
	.order-line { display: flex; justify-content: space-between; align-items: center; }
	.ono { color: #6b7280; font-size: 25rpx; }
	.oamt { color: #f24e3e; font-size: 32rpx; font-weight: 800; }
	.tip { margin-top: 14rpx; background: #fff7f0; border-radius: 10rpx; padding: 14rpx 18rpx; font-size: 22rpx; color: #a55a22; line-height: 1.7; }
	.types { display: flex; gap: 18rpx; }
	.tp {
		flex: 1; border: 2rpx solid #e5e7eb; border-radius: 14rpx; padding: 20rpx;
		font-size: 27rpx; font-weight: 600; text-align: center;
	}
	.tp-s { display: block; font-size: 21rpx; color: #a5aca1; font-weight: 400; margin-top: 8rpx; }
	.tp.on { border-color: #f24e3e; color: #f24e3e; background: #fef4f4; }
	.reason {
		width: 100%; min-height: 160rpx; border: 2rpx solid #e5e7eb; border-radius: 12rpx;
		padding: 16rpx 20rpx; font-size: 26rpx; box-sizing: border-box;
	}
	.imgs { display: flex; flex-wrap: wrap; gap: 16rpx; }
	.ig { position: relative; width: 160rpx; height: 160rpx; }
	.ig-img { width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #f5f5f5; }
	.ig-del {
		position: absolute; top: 0; right: 0; width: 38rpx; height: 38rpx; line-height: 34rpx;
		text-align: center; background: rgba(15, 23, 42, .6); color: #fff;
		font-size: 26rpx; border-radius: 0 12rpx 0 12rpx;
	}
	.ig-add {
		width: 160rpx; height: 160rpx; border: 2rpx dashed #d9dde2; border-radius: 12rpx;
		background: #fafbfc; display: flex; flex-direction: column;
		align-items: center; justify-content: center; color: #a5aca1;
	}
	.ig-add.off { opacity: .55; }
	.ig-plus { font-size: 46rpx; line-height: 1; }
	.ig-txt { font-size: 21rpx; margin-top: 8rpx; }
	.bar {
		position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
		padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
		border-top: 1rpx solid #eceef0;
	}
	.submit {
		background: linear-gradient(90deg, #f37e5d, #f24e3e); color: #fff; text-align: center;
		border-radius: 44rpx; padding: 20rpx 0; font-size: 29rpx; font-weight: 600;
	}
	.submit.off { opacity: 0.6; }
	.ups { display: flex; gap: 16rpx; flex-wrap: wrap; }
	.up-thumb { position: relative; width: 150rpx; height: 150rpx; border-radius: 12rpx; overflow: hidden; }
	.up-img { width: 100%; height: 100%; }
	.up-del {
		position: absolute; top: 0; right: 0; width: 40rpx; height: 40rpx; line-height: 36rpx;
		background: rgba(0,0,0,.55); color: #fff; font-size: 26rpx; text-align: center;
	}
	.up-add {
		width: 150rpx; height: 150rpx; border: 2rpx dashed #c6ced6; border-radius: 12rpx;
		display: flex; flex-direction: column; align-items: center; justify-content: center;
		color: #a5aca1;
	}
	.up-plus { font-size: 44rpx; line-height: 1; }
	.up-txt { font-size: 20rpx; margin-top: 6rpx; }
</style>
