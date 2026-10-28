<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, GraduationCap, Home, Lock, ShieldAlert, ShieldX } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const homePath = computed(() => (userStore.state.role === 'ROLE_ADMIN' ? '/admin' : '/home'))

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(homePath.value)
  }
}
</script>

<template>
  <main class="forbidden-page">
    <header class="forbidden-header">
      <a class="forbidden-brand" :href="homePath" @click.prevent="router.push(homePath)">
        <span class="brand-mark"><GraduationCap :size="25" :stroke-width="2.2" /></span>
        <strong>Adaptive Learning</strong>
      </a>
      <span class="status-label"><i></i> Hệ thống bảo mật</span>
    </header>

    <section class="forbidden-content" aria-labelledby="forbidden-title">
      <div class="forbidden-copy">
        <p class="forbidden-kicker"><span></span> LỖI 403</p>
        <h1 id="forbidden-title">Truy cập bị từ chối</h1>
        <p class="forbidden-description">
          Bạn không có quyền truy cập vào trang hoặc tài nguyên này. Nếu bạn cần quyền hạn đặc biệt, vui lòng liên hệ quản trị viên hoặc quay trở về trang làm việc của bạn.
        </p>
        <div class="forbidden-actions">
          <button class="home-button" type="button" @click="router.push(homePath)">
            <Home :size="17" /> Về trang của tôi <ArrowRight :size="15" />
          </button>
          <button class="back-button" type="button" @click="goBack">
            <ArrowLeft :size="16" /> Quay lại
          </button>
        </div>
        <p class="requested-path">
          <span>Mã lỗi</span><code>403</code><i></i><span>Địa chỉ</span><code>{{ route.fullPath }}</code>
        </p>
      </div>

      <div class="forbidden-illustration" aria-hidden="true">
        <div class="forbidden-grid"></div>
        <div class="forbidden-glow"></div>
        <div class="shield-circle">
          <div class="shield-core">
            <ShieldAlert :size="48" :stroke-width="1.8" />
          </div>
        </div>
        <span class="badge-lock"><Lock :size="18" :stroke-width="2.2" /></span>
        <span class="badge-shield"><ShieldX :size="18" :stroke-width="2.2" /></span>
        <span class="forbidden-badge">Phân quyền: Không hợp lệ</span>
      </div>
    </section>

    <footer class="forbidden-footer">
      <span>Adaptive Learning</span>
      <span>Nền tảng học tập thông minh & Bảo mật</span>
    </footer>
  </main>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  padding: 0 6.2%;
  color: #0f172a;
  background: #f8fafc;
  font-family: var(--font-sans, system-ui, -apple-system, sans-serif);
}

.forbidden-header {
  display: flex;
  height: 76px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
}

.forbidden-brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #0f172a;
  text-decoration: none;
}

.forbidden-brand:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 4px;
  border-radius: 4px;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #2563eb;
}

.forbidden-brand strong {
  font-size: 14px;
  font-weight: 700;
}

.status-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #64748b;
  font-size: 11px;
}

.status-label i,
.forbidden-kicker span,
.requested-path i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #dc2626;
}

.forbidden-content {
  display: grid;
  width: min(100%, 1120px);
  flex: 1;
  grid-template-columns: minmax(0, 1.05fr) minmax(340px, 0.95fr);
  align-items: center;
  gap: 55px;
  margin: 0 auto;
  padding: 44px 0;
}

.forbidden-copy {
  max-width: 510px;
}

.forbidden-kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  color: #dc2626;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.forbidden-kicker span {
  background: #dc2626;
}

.forbidden-copy h1 {
  max-width: 14ch;
  margin: 0;
  color: #0f172a;
  font-size: 40px;
  font-weight: 800;
  line-height: 1.16;
  letter-spacing: -0.02em;
}

.forbidden-description {
  max-width: 440px;
  margin: 15px 0 24px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.forbidden-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.home-button,
.back-button {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.home-button:focus-visible,
.back-button:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.home-button {
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.home-button:hover {
  background: #1d4ed8;
}

.back-button {
  border-color: #e2e8f0;
  background: #ffffff;
  color: #334155;
}

.back-button:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.requested-path {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 28px 0 0;
  color: #94a3b8;
  font-size: 11px;
}

.requested-path code {
  max-width: 240px;
  overflow: hidden;
  color: #475569;
  font: 11px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.requested-path i {
  width: 4px;
  height: 4px;
  margin: 0 3px;
  background: #cbd5e1;
}

.forbidden-illustration {
  position: relative;
  min-height: 345px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: linear-gradient(145deg, #eff6ff, #ffffff 60%, #fef2f2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.forbidden-grid {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image: linear-gradient(#e2e8f0 1px, transparent 1px), linear-gradient(90deg, #e2e8f0 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(circle at center, #000 40%, transparent 80%);
}

.forbidden-glow {
  position: absolute;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(220, 38, 38, 0.08) 0%, rgba(37, 99, 235, 0.04) 50%, transparent 70%);
  border-radius: 50%;
}

.shield-circle {
  position: relative;
  z-index: 2;
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  border: 1px solid #fee2e2;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 16px 32px rgba(220, 38, 38, 0.08), 0 4px 8px rgba(15, 23, 42, 0.03);
}

.shield-core {
  display: grid;
  place-items: center;
  color: #dc2626;
}

.badge-lock {
  position: absolute;
  top: 25%;
  right: 20%;
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
}

.badge-shield {
  position: absolute;
  bottom: 25%;
  left: 20%;
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  color: #7c3aed;
  box-shadow: 0 6px 16px rgba(124, 58, 237, 0.08);
}

.forbidden-badge {
  position: absolute;
  bottom: 12%;
  left: 50%;
  padding: 6px 14px;
  border: 1px solid #fecaca;
  border-radius: 9999px;
  background: rgba(254, 242, 242, 0.95);
  color: #b91c1c;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  transform: translateX(-50%);
  backdrop-filter: blur(4px);
}

.forbidden-footer {
  display: flex;
  height: 52px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #e2e8f0;
  color: #94a3b8;
  font-size: 11px;
}

.forbidden-footer span:first-child {
  color: #64748b;
  font-weight: 600;
}

@media (max-width: 768px) {
  .forbidden-page {
    padding: 0 18px;
  }
  .forbidden-header {
    height: 62px;
  }
  .forbidden-content {
    grid-template-columns: minmax(0, 1fr);
    gap: 28px;
    padding: 32px 0 24px;
  }
  .forbidden-copy h1 {
    font-size: 30px;
  }
  .forbidden-illustration {
    min-height: 230px;
  }
  .shield-circle {
    width: 80px;
    height: 80px;
  }
  .badge-lock {
    top: 18%;
    right: 14%;
  }
  .badge-shield {
    bottom: 18%;
    left: 14%;
  }
  .forbidden-footer {
    height: 44px;
    font-size: 10px;
  }
}
</style>
