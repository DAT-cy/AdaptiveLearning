<script setup>
import { computed, onMounted, ref } from 'vue'
import { Axios } from '@/services/Axios'
import { getToken } from '@/utils/auth'

const today = new Date()
const iso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const end = new Date(today)
const start = new Date(today)
start.setDate(start.getDate() - 6)
const from = ref(iso(start))
const to = ref(iso(end))
const data = ref(null)
const search = ref('')
const page = ref(1)
const pageSize = 10
const error = ref('')
const filtered = computed(() => (data.value?.students || []).filter((x) => `${x.name} ${x.username}`.toLowerCase().includes(search.value.toLowerCase())))
const pages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const rows = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const kpis = computed(() => data.value?.kpis || {})
const score = (value) => Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })
const load = async () => {
  error.value = ''
  try { const response = await Axios.get('/api/v1/reports', { from: from.value, to: to.value }); data.value = response.data }
  catch { error.value = 'Không tải được dữ liệu báo cáo.' }
}
const download = async (format) => {
  const response = await fetch(`/api/v1/reports/export.${format}?from=${from.value}&to=${to.value}`, { headers: { Authorization: `Bearer ${getToken() || ''}` } })
  if (!response.ok) { error.value = 'Không thể xuất báo cáo.'; return }
  const blob = await response.blob()
  const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `learning-report.${format}`; link.click(); URL.revokeObjectURL(link.href)
}
const maxDay = computed(() => Math.max(1, ...(data.value?.daily || []).map((x) => x.attempts)))
onMounted(load)
</script>

<template>
  <main class="reports">
    <header class="top"><div><p class="eyebrow">QUẢN TRỊ / PHÂN TÍCH</p><h1>Báo cáo học tập</h1></div><div class="exports"><button @click="download('xlsx')">Xuất Excel</button><button @click="download('pdf')">Xuất PDF</button></div></header>
    <section class="filters"><label>Từ ngày<input v-model="from" type="date"></label><label>Đến ngày<input v-model="to" type="date"></label><button class="primary" @click="page = 1; load()">Áp dụng</button></section>
    <p v-if="error" class="error">{{ error }}</p>
    <section class="kpis"><article><span>Tổng lượt thi</span><strong>{{ kpis.attempts ?? '—' }}</strong></article><article><span>Điểm trung bình</span><strong>{{ score(kpis.averageScore) }}</strong></article><article><span>Đã chấm</span><strong>{{ kpis.graded ?? '—' }}</strong></article><article><span>Tỷ lệ hoàn tất</span><strong>{{ score(kpis.completionRate) }}%</strong></article></section>
    <section class="report-section"><div class="section-head"><h2>Lượt thi theo ngày</h2><span>{{ from }} – {{ to }}</span></div><div class="chart" role="img" aria-label="Biểu đồ lượt thi theo ngày"><div v-for="item in data?.daily || []" :key="item.date" class="bar-column"><strong>{{ item.attempts }}</strong><div class="bar" :style="{ height: `${Math.max(4, item.attempts / maxDay * 130)}px` }"></div><small>{{ item.date.slice(5) }}</small></div></div></section>
    <section class="report-section"><div class="section-head"><h2>Hoạt động học viên</h2><input v-model="search" placeholder="Tìm tên hoặc email" @input="page = 1"></div><div class="table-wrap"><table><thead><tr><th>Học viên</th><th>Lượt thi</th><th>Điểm TB</th><th>Hoạt động gần nhất</th></tr></thead><tbody><tr v-for="student in rows" :key="student.userId" @click="search = student.username"><td><strong>{{ student.name || student.username }}</strong><small>{{ student.username }}</small></td><td>{{ student.attempts }}</td><td>{{ score(student.averageScore) }}</td><td>{{ student.lastActivity ? new Date(student.lastActivity).toLocaleString('vi-VN') : '—' }}</td></tr><tr v-if="!rows.length"><td colspan="4" class="empty">Không có dữ liệu trong kỳ đã chọn.</td></tr></tbody></table></div><footer><span>{{ filtered.length }} học viên</span><div><button :disabled="page <= 1" @click="page--">Trước</button><span>{{ page }} / {{ pages }}</span><button :disabled="page >= pages" @click="page++">Sau</button></div></footer></section>
    <section class="report-section"><div class="section-head"><h2>Hiệu quả đề thi</h2></div><div class="table-wrap"><table><thead><tr><th>Đề thi</th><th>Lượt làm</th><th>Điểm TB</th><th>Đạt</th><th>Tỷ lệ đạt</th></tr></thead><tbody><tr v-for="exam in data?.exams || []" :key="exam.examId"><td>{{ exam.title }}</td><td>{{ exam.attempts }}</td><td>{{ score(exam.averageScore) }}</td><td>{{ exam.passed }} / {{ exam.graded }}</td><td>{{ score(exam.passRate) }}%</td></tr></tbody></table></div></section>
  </main>
</template>

<style scoped>
.reports{max-width:1240px;margin:auto;padding:28px 24px 48px;color:#202a31}.top,.section-head,.filters,.exports,footer,footer div{display:flex;align-items:center;justify-content:space-between;gap:14px}.top{margin-bottom:24px}.eyebrow{font-size:11px;color:#64747a;margin:0 0 7px}h1{font-size:25px;margin:0}h2{font-size:16px;margin:0}.filters{justify-content:flex-start;background:#f2f5f4;padding:14px;margin-bottom:18px}.filters label{font-size:12px;color:#56646a;display:grid;gap:5px}input,button{font:inherit;border:1px solid #cbd4d2;border-radius:4px;padding:8px 10px;background:#fff;color:#263238}button{cursor:pointer}.primary{background:#176b60;color:white;border-color:#176b60;margin-top:17px}.exports button{font-size:13px}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:18px}.kpis article,.report-section{border:1px solid #e0e6e4;border-radius:5px;background:#fff}.kpis article{padding:16px;display:grid;gap:11px}.kpis span,.section-head span{font-size:12px;color:#6d7b80}.kpis strong{font-size:24px}.report-section{padding:18px;margin-bottom:16px}.section-head{margin-bottom:16px}.chart{height:190px;display:flex;align-items:flex-end;gap:10px;overflow-x:auto;border-bottom:1px solid #dbe2df}.bar-column{min-width:42px;height:180px;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;gap:5px}.bar{width:22px;background:#188273;border-radius:3px 3px 0 0;min-height:4px}.bar-column strong,.bar-column small{font-size:10px;color:#59686d}.table-wrap{overflow:auto}table{border-collapse:collapse;width:100%;font-size:13px;text-align:left}th,td{padding:11px 10px;border-bottom:1px solid #e8edeb;white-space:nowrap}th{color:#66747a;font-weight:600}td small{display:block;color:#738086;margin-top:3px}tbody tr{cursor:pointer}tbody tr:hover{background:#f5f8f7}.empty{text-align:center;color:#78858a;padding:24px}.error{color:#a33434}footer{font-size:12px;color:#6c797d;margin-top:12px}footer button:disabled{opacity:.45;cursor:default}@media(max-width:700px){.reports{padding:18px 12px}.kpis{grid-template-columns:repeat(2,1fr)}.top{align-items:flex-start;flex-direction:column}.filters{flex-wrap:wrap}.section-head{align-items:flex-start;flex-direction:column}}
</style>
