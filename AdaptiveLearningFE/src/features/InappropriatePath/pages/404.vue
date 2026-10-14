<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Compass, GraduationCap, Home, SearchX } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const homePath = computed(() => userStore.state.role === 'ROLE_ADMIN' ? '/admin' : '/home')

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push(homePath.value)
}
</script>

<template>
  <main class="not-found-page">
    <header class="not-found-header">
      <a class="not-found-brand" :href="homePath" @click.prevent="router.push(homePath)">
        <span class="brand-mark"><GraduationCap :size="25" :stroke-width="2.2" /></span>
        <strong>Adaptive Learning</strong>
      </a>
      <span class="status-label"><i></i> Hệ thống học tập</span>
    </header>

    <section class="not-found-content" aria-labelledby="not-found-title">
      <div class="not-found-copy">
        <p class="not-found-kicker"><span></span> LỖI 404</p>
        <h1 id="not-found-title">Không tìm thấy trang</h1>
        <p class="not-found-description">Đường dẫn này không tồn tại hoặc đã được thay đổi. Hãy kiểm tra lại địa chỉ hoặc quay về khu vực học tập của bạn.</p>
        <div class="not-found-actions">
          <button class="home-button" @click="router.push(homePath)"><Home :size="17" /> Về trang của tôi <ArrowRight :size="15" /></button>
          <button class="back-button" @click="goBack"><ArrowLeft :size="16" /> Quay lại</button>
        </div>
        <p class="requested-path"><span>Mã lỗi</span><code>404</code><i></i><span>Địa chỉ</span><code>{{ route.fullPath }}</code></p>
      </div>

      <div class="route-illustration" aria-hidden="true">
        <div class="route-grid"></div>
        <div class="route-line route-line-one"></div>
        <div class="route-line route-line-two"></div>
        <span class="route-node node-home"><Home :size="19" /></span>
        <span class="route-node node-compass"><Compass :size="20" /></span>
        <span class="route-lost"><SearchX :size="44" :stroke-width="1.5" /></span>
        <span class="route-coordinate coordinate-one">21°01′ N</span>
        <span class="route-coordinate coordinate-two">105°51′ E</span>
        <span class="route-caption">Đang tìm đường trở về</span>
      </div>
    </section>

    <footer class="not-found-footer"><span>Adaptive Learning</span><span>Học tập theo lộ trình của bạn</span></footer>
  </main>
</template>

<style scoped>
.not-found-page { display: flex; min-height: 100vh; flex-direction: column; padding: 0 6.2%; color: #14264e; background: #f7f9fd; font-family: Inter, ui-sans-serif, system-ui, sans-serif; }
.not-found-header { display: flex; height: 76px; flex: 0 0 auto; align-items: center; justify-content: space-between; border-bottom: 1px solid #e9eef6; }.not-found-brand { display: inline-flex; align-items: center; gap: 9px; color: #152957; text-decoration: none; }.brand-mark { display: grid; width: 34px; height: 34px; place-items: center; color: #286af4; }.not-found-brand strong { font-size: 14px; }.status-label { display: inline-flex; align-items: center; gap: 7px; color: #7889a5; font-size: 10px; }.status-label i,.not-found-kicker span,.requested-path i { width: 7px; height: 7px; border-radius: 50%; background: #24b48c; }
.not-found-content { display: grid; width: min(100%,1120px); flex: 1; grid-template-columns: minmax(0,1.05fr) minmax(340px,.95fr); align-items: center; gap: 55px; margin: 0 auto; padding: 44px 0; }.not-found-copy { max-width: 510px; }.not-found-kicker { display: flex; align-items: center; gap: 8px; margin: 0 0 16px; color: #4773c7; font-size: 10px; font-weight: 700; }.not-found-kicker span { background: #f39a56; }.not-found-copy h1 { max-width: 12ch; margin: 0; color: #112553; font-size: 40px; line-height: 1.16; }.not-found-description { max-width: 440px; margin: 15px 0 22px; color: #7485a2; font-size: 13px; line-height: 1.75; }.not-found-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }.home-button,.back-button { display: inline-flex; min-height: 40px; align-items: center; justify-content: center; gap: 8px; padding: 0 13px; border: 1px solid transparent; border-radius: 6px; font: inherit; font-size: 11px; font-weight: 600; cursor: pointer; }.home-button { background: #286af4; color: #fff; box-shadow: 0 5px 12px rgba(40,106,244,.15); }.home-button:hover { background: #1c5ddd; }.back-button { border-color: #dfe7f2; background: #fff; color: #526889; }.back-button:hover { border-color: #cbd8ea; background: #fdfefe; }.requested-path { display: flex; align-items: center; gap: 8px; margin: 24px 0 0; color: #8a99b1; font-size: 9px; }.requested-path code { max-width: 210px; overflow: hidden; color: #5e7398; font: 10px ui-monospace, SFMono-Regular, Consolas, monospace; text-overflow: ellipsis; white-space: nowrap; }.requested-path i { width: 4px; height: 4px; margin: 0 3px; background: #c0cbda; }
.route-illustration { position: relative; min-height: 345px; overflow: hidden; border: 1px solid #e5ecf6; border-radius: 8px; background: linear-gradient(145deg,#f0f6ff,#fff 62%,#f3f9f8); }.route-grid { position: absolute; inset: 0; opacity: .44; background-image: linear-gradient(#dce7f5 1px,transparent 1px),linear-gradient(90deg,#dce7f5 1px,transparent 1px); background-size: 30px 30px; mask-image: linear-gradient(135deg,#000,transparent 82%); }.route-line { position: absolute; z-index: 1; height: 2px; border-top: 2px dashed #b5c9eb; transform-origin: left center; }.route-line-one { top: 48%; left: 19%; width: 32%; transform: rotate(-18deg); }.route-line-two { top: 38%; left: 52%; width: 30%; transform: rotate(28deg); }.route-node { position: absolute; z-index: 2; display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid #d9e6f8; border-radius: 50%; background: #fff; box-shadow: 0 5px 14px rgba(43,83,148,.08); }.node-home { top: 59%; left: 16%; color: #286af4; }.node-compass { top: 23%; right: 14%; color: #1cac86; }.route-lost { position: absolute; z-index: 3; top: 34%; left: 43%; display: grid; width: 76px; height: 76px; place-items: center; border: 1px solid #e3ebf6; border-radius: 8px; background: #fff; box-shadow: 0 12px 26px rgba(43,73,126,.1); color: #7790b6; }.route-lost::after { position: absolute; right: -7px; bottom: -7px; width: 23px; height: 23px; border: 3px solid #f7f9fd; border-radius: 50%; background: #f5a35f; content: ''; }.route-coordinate { position: absolute; color: #8ba0c2; font: 9px ui-monospace, SFMono-Regular, Consolas, monospace; }.coordinate-one { top: 20%; left: 19%; }.coordinate-two { right: 15%; bottom: 24%; }.route-caption { position: absolute; bottom: 12%; left: 50%; padding: 7px 10px; border: 1px solid #e5ecf6; border-radius: 5px; background: rgba(255,255,255,.9); color: #7488a8; font-size: 9px; white-space: nowrap; transform: translateX(-50%); }
.not-found-footer { display: flex; height: 52px; flex: 0 0 auto; align-items: center; justify-content: space-between; border-top: 1px solid #e9eef6; color: #8c9ab1; font-size: 9px; }.not-found-footer span:first-child { color: #597096; font-weight: 650; }
@media (max-width: 760px) { .not-found-page { padding: 0 18px; }.not-found-header { height: 62px; }.not-found-brand strong { font-size: 12px; }.status-label { font-size: 8px; }.not-found-content { grid-template-columns: minmax(0,1fr); gap: 24px; padding: 30px 0 24px; }.not-found-copy h1 { max-width: 18ch; font-size: 32px; }.not-found-description { font-size: 12px; }.route-illustration { min-height: 225px; }.route-lost { top: 32%; left: calc(50% - 38px); }.node-home { top: 55%; left: 12%; }.node-compass { top: 20%; right: 12%; }.route-caption { bottom: 10%; }.not-found-footer { height: 44px; font-size: 8px; } }
</style>
