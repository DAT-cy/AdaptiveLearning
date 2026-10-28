<script setup>
import { inject } from 'vue'
import { Archive, Check, ChevronDown, ChevronUp, Compass, Plus, X } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
const today = new Date().toISOString().slice(0, 10)

const taskTypeLabels = {
  STUDY: 'Học theo mục tiêu',
  PRACTICE: 'Luyện tập',
  MOCK_TEST: 'Làm đề thi thử',
  REVIEW: 'Ôn tập',
  WRITING: 'Luyện viết',
  SPEAKING: 'Luyện nói',
}

const skillLabels = {
  LISTENING: 'Nghe',
  READING: 'Đọc',
  WRITING: 'Viết',
  SPEAKING: 'Nói',
}

function canCompletePlan() {
  return !home.planTasks.some((task) => task.status === 'PENDING')
}
</script>

<template>
  <section class="page-view">
    <div class="view-heading">
      <div><p class="view-kicker">MỤC TIÊU VÀ NHIỆM VỤ</p><h1>Kế hoạch học</h1><p>Mỗi kế hoạch gồm mục tiêu kỳ thi, lịch học và tiến độ nhiệm vụ.</p></div>
      <button class="primary-button" @click="home.showPlanForm = !home.showPlanForm"><Plus :size="17" /> Tạo kế hoạch</button>
    </div>

    <form v-if="home.showPlanForm" class="plan-form" @submit.prevent="home.createPlan">
      <div class="form-heading"><div><h2>Tạo kế hoạch mới</h2><p>Chọn mục tiêu kỳ thi và thời gian học mỗi tuần.</p></div><button class="icon-action" type="button" aria-label="Đóng" @click="home.showPlanForm = false"><X :size="18" /></button></div>
      <label class="form-field">Tên kế hoạch<input v-model="home.planForm.title" required maxlength="200" placeholder="Ví dụ: IELTS Academic - tháng 12" /></label>
      <div class="form-row">
        <label class="form-field">Chứng chỉ<select v-model="home.selectedCertificateId"><option value="">Không chọn</option><option v-for="certificate in home.certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label>
        <label class="form-field">Ngày dự kiến thi<input v-model="home.planForm.examDate" type="date" :min="today" required /></label>
      </div>
      <div class="form-row">
        <label class="form-field">Điểm mục tiêu<input v-model.number="home.planForm.targetScore" type="number" min="0" step="0.5" placeholder="Ví dụ: 7.0" /></label>
        <label class="form-field">Giờ học mỗi tuần<input v-model.number="home.planForm.hoursPerWeek" type="number" min="1" max="80" required /></label>
      </div>
      <div class="form-actions"><button class="secondary-button" type="button" @click="home.showPlanForm = false">Hủy</button><button class="primary-button" type="submit" :disabled="home.creatingPlan">{{ home.creatingPlan ? 'Đang tạo...' : 'Tạo kế hoạch' }}</button></div>
    </form>

    <div v-if="home.loading" class="empty-inline" role="status">Đang tải kế hoạch...</div>
    <div v-else-if="home.plans.length" class="plan-list">
      <article v-for="plan in home.plans" :key="plan.planId" class="plan-row">
        <span class="plan-row-icon"><Compass :size="21" /></span>
        <div class="plan-row-content">
          <div class="plan-row-title"><h2>{{ plan.title }}</h2><span :class="['plan-status', plan.status === 'ACTIVE' && 'plan-active']">{{ plan.status === 'ACTIVE' ? 'Đang học' : plan.status === 'COMPLETED' ? 'Hoàn thành' : 'Đã lưu trữ' }}</span></div>
          <p>{{ home.certificates.find((certificate) => certificate.certificateId === plan.certificateId)?.name || 'Kế hoạch học cá nhân' }} · {{ plan.hoursPerWeek }} giờ mỗi tuần · Ngày thi {{ home.formatDate(plan.examDate) }}</p>
          <div class="plan-progress"><div><span :style="{ width: `${plan.taskCount ? Math.round(plan.completedCount / plan.taskCount * 100) : 0}%` }"></span></div><small>{{ plan.completedCount || 0 }} / {{ plan.taskCount || 0 }} nhiệm vụ</small></div>
          <div v-if="home.expandedPlanId === plan.planId" class="plan-details">
            <div v-if="home.planDetailsLoading" class="empty-inline" role="status">Đang tải nhiệm vụ...</div>
            <template v-else>
              <p v-if="home.planSummary" class="plan-summary">{{ Math.round(home.planSummary.progressPercent) }}% hoàn thành · {{ home.planSummary.upcomingTasks }} nhiệm vụ sắp tới · {{ home.planSummary.overdueCount }} quá hạn</p>
              <div v-if="home.planTasks.length" class="plan-task-list">
                <div v-for="task in home.planTasks" :key="task.taskId" class="plan-task-row">
                  <div><strong>{{ taskTypeLabels[task.taskType] || task.taskType }} · {{ skillLabels[task.skill] || task.skill }}</strong><small>{{ home.formatDate(task.scheduledDate) }} · {{ task.durationMinutes }} phút</small></div>
                  <select :value="task.status" :disabled="plan.status !== 'ACTIVE' || home.updatingTaskId === task.taskId" :aria-label="`Trạng thái ${task.description}`" @change="home.updatePlanTask(task, $event.target.value)">
                    <option value="PENDING">Chưa làm</option><option value="COMPLETED">Hoàn thành</option><option value="SKIPPED">Bỏ qua</option>
                  </select>
                </div>
              </div>
              <p v-else class="empty-inline">Kế hoạch chưa có nhiệm vụ.</p>
              <div v-if="plan.status === 'ACTIVE'" class="plan-actions">
                <button class="secondary-button" :disabled="home.updatingPlanId === plan.planId" @click="home.archivePlan(plan.planId)"><Archive :size="15" /> Lưu trữ</button>
                <button class="primary-button" :disabled="home.updatingPlanId === plan.planId || !canCompletePlan()" @click="home.completePlan(plan.planId)"><Check :size="15" /> Hoàn tất</button>
              </div>
            </template>
          </div>
        </div>
        <button class="outline-button plan-details-toggle" type="button" :aria-expanded="home.expandedPlanId === plan.planId" @click="home.loadPlanDetails(plan)">
          {{ home.expandedPlanId === plan.planId ? 'Ẩn nhiệm vụ' : 'Xem nhiệm vụ' }}
          <ChevronUp v-if="home.expandedPlanId === plan.planId" :size="16" />
          <ChevronDown v-else :size="16" />
        </button>
      </article>
    </div>
    <div v-else-if="!home.showPlanForm" class="empty-page"><Compass :size="28" /><h2>Chưa có kế hoạch học</h2><p>Bắt đầu bằng mục tiêu kỳ thi và quỹ thời gian học mỗi tuần.</p><button class="primary-button" @click="home.showPlanForm = true"><Plus :size="16" /> Tạo kế hoạch đầu tiên</button></div>
  </section>
</template>

<style scoped>
.plan-details-toggle { flex: 0 0 auto; }
.plan-details { display: grid; gap: 10px; margin-top: 13px; padding-top: 12px; border-top: 1px solid #E2E8F0; }
.plan-summary { margin: 0; color: #475569; font-size: 11px; line-height: 1.5; }
.plan-task-list { display: grid; }
.plan-task-row { display: grid; grid-template-columns: minmax(0, 1fr) 128px; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #F1F5F9; }
.plan-task-row > div { display: grid; min-width: 0; gap: 4px; }
.plan-task-row strong { color: #0F172A; font-size: 11px; }
.plan-task-row small { color: #64748B; font-size: 10px; }
.plan-task-row select { width: 100%; min-height: 34px; padding: 0 8px; border: 1px solid #E2E8F0; border-radius: 6px; background: #FFFFFF; color: #334155; font: inherit; font-size: 10px; }
.plan-actions { display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 640px) {
  .plan-row { flex-wrap: wrap; }
  .plan-task-row { grid-template-columns: minmax(0, 1fr); gap: 7px; }
  .plan-task-row select { max-width: 180px; }
  .plan-details-toggle { margin-left: 48px; }
}
</style>
