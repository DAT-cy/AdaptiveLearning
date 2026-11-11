<script setup>
import { computed, onMounted, ref } from 'vue'
import { CheckCircle2, Circle, RefreshCw, Search } from 'lucide-vue-next'
import { adminService } from '@/services/adminService'
import ExamPreview from './ExamPreview.vue'

const sets = ref([])
const loading = ref(false)
const savingId = ref(null)
const error = ref('')
const search = ref('')
const preview = ref(null)
const savingPreview = ref(false)

const visibleSets = computed(() => {
  const query = search.value.trim().toLowerCase()
  return sets.value.filter((item) => !query || String(item.title || '').toLowerCase().includes(query))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await adminService.examSets()
    sets.value = Array.isArray(result) ? result : result?.items || []
  } catch (exception) {
    error.value = exception?.response?.data?.message || 'Không thể tải danh sách bộ đề.'
  } finally {
    loading.value = false
  }
}

async function toggle(item) {
  savingId.value = item.importId
  error.value = ''
  try {
    const updated = await adminService.setExamSetActive(item.importId, !item.active)
    item.active = updated?.active ?? !item.active
  } catch (exception) {
    error.value = exception?.response?.data?.message || 'Không thể cập nhật trạng thái bộ đề.'
  } finally {
    savingId.value = null
  }
}

async function openPreview(item) {
  try {
    preview.value = await adminService.examImport(item.importId)
  } catch (exception) {
    error.value = exception?.response?.data?.message || 'Không thể mở bộ đề.'
  }
}

async function savePreview() {
  if (!preview.value || savingPreview.value) return
  savingPreview.value = true
  try {
    preview.value = await adminService.updateExamImport(preview.value.importId, { questions: preview.value.questions })
    await load()
  } catch (exception) {
    error.value = exception?.response?.data?.message || 'Không thể lưu chỉnh sửa bộ đề.'
  } finally {
    savingPreview.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="exam-set-management">
    <header class="set-heading">
      <div>
        <h2>Quản lý bộ đề</h2>
        <p>Các bộ đề đã xác nhận sẽ chỉ hiển thị trong ngân hàng câu hỏi khi được bật.</p>
      </div>
      <button class="refresh-button" type="button" title="Tải lại" @click="load" :disabled="loading">
        <RefreshCw :size="16" :class="{ spin: loading }" />
        Tải lại
      </button>
    </header>
    <label class="set-search"><Search :size="16" /><input v-model="search" type="search" placeholder="Tìm bộ đề" /></label>
    <p v-if="error" class="set-error">{{ error }}</p>
    <div v-else-if="loading" class="set-state">Đang tải dữ liệu...</div>
    <div v-else-if="!visibleSets.length" class="set-state">Chưa có bộ đề nào được xác nhận.</div>
    <div v-else class="set-table">
      <button v-for="item in visibleSets" :key="item.importId" class="set-row" type="button" @click="openPreview(item)">
        <div class="set-icon"><CheckCircle2 :size="19" /></div>
        <div class="set-info"><strong>{{ item.title }}</strong><span>{{ item.questionCount || 0 }} câu hỏi · Đã xác nhận</span></div>
        <span class="set-status" :class="item.active ? 'is-active' : 'is-inactive'">
          <Circle :size="10" fill="currentColor" /> {{ item.active ? 'Đang hoạt động' : 'Đang tắt' }}
        </span>
        <span class="active-button" role="button" tabindex="0" @click.stop="toggle(item)" @keydown.enter.stop="toggle(item)">
          {{ item.active ? 'Tắt' : 'Active' }}
        </span>
      </button>
    </div>
    <ExamPreview v-if="preview" :detail="preview" editable :saving="savingPreview" @close="preview = null" @save="savePreview" />
  </section>
</template>

<style scoped>
.exam-set-management { padding: 24px 0 48px; }
.set-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:20px; }
.set-heading h2 { margin:0; color:#17202d; font-size:20px; }
.set-heading p { margin:6px 0 0; color:#64748b; font-size:13px; }
.refresh-button,.active-button { display:inline-flex; align-items:center; gap:7px; min-height:34px; padding:0 12px; border:1px solid #b7d8d6; border-radius:5px; background:#f0f8f7; color:#075e5c; font:inherit; font-size:12px; font-weight:600; cursor:pointer; }
.refresh-button:disabled,.active-button:disabled { opacity:.55; cursor:wait; }
.set-search { display:flex; align-items:center; gap:8px; max-width:430px; padding:0 10px; border:1px solid #cbd5df; border-radius:5px; color:#64748b; }
.set-search input { width:100%; height:36px; border:0; outline:0; font:inherit; font-size:12px; }
.set-table { margin-top:16px; border:1px solid #dce2e9; border-radius:6px; overflow:hidden; background:#fff; }
.set-row { display:grid; width:100%; grid-template-columns:36px minmax(0,1fr) auto auto; align-items:center; gap:12px; padding:14px 16px; border:0; border-bottom:1px solid #edf0f2; background:#fff; color:inherit; font:inherit; text-align:left; cursor:pointer; }
.set-row:last-child { border-bottom:0; }.set-icon { color:#176b5d; }.set-info { display:grid; gap:4px; min-width:0; }.set-info strong { color:#17202d; font-size:13px; }.set-info span { color:#64748b; font-size:11px; }.set-status { display:inline-flex; align-items:center; gap:5px; font-size:11px; white-space:nowrap; }.is-active { color:#16704b; }.is-inactive { color:#94a3b8; }.set-error,.set-state { margin-top:16px; padding:20px; color:#64748b; font-size:13px; }.set-error { color:#b42318; }.spin { animation:spin 1s linear infinite; } @keyframes spin { to { transform:rotate(360deg); } }
@media (max-width:700px) { .set-heading { flex-direction:column; }.set-row { grid-template-columns:28px minmax(0,1fr) auto; }.set-status { grid-column:2; }.active-button { grid-column:3; grid-row:1 / span 2; } }
</style>
