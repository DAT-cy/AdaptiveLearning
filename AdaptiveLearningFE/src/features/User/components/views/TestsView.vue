<script setup>
import { inject } from 'vue'
import { ArrowRight, BookOpen, CheckCircle2, Clock3, Crown, Search } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section class="page-view">
    <div class="view-heading"><div><p class="view-kicker">LUYỆN TẬP VÀ ĐÁNH GIÁ</p><h1>Bài kiểm tra</h1><p>Tạo đề luyện tập hoặc tiếp tục với bài kiểm tra đã mở.</p></div><label class="search-field"><Search :size="17" /><input v-model="home.searchQuery" placeholder="Tìm bài kiểm tra" aria-label="Tìm bài kiểm tra"></label></div>
    <div v-if="home.entitlements && home.entitlements.plan === 'FREE'" class="free-quota-banner">
      <div><strong>Gói Free</strong><p>Đã dùng {{ home.entitlements.examsUsed }} / {{ home.entitlements.examLimit }} lượt tạo đề miễn phí.</p><div class="free-quota-track"><i :style="{ width: `${Math.min(100, home.entitlements.examsUsed / Math.max(1, home.entitlements.examLimit) * 100)}%` }"></i></div></div>
      <RouterLink v-if="home.entitlements.examsRemaining <= 0" to="/pricing" class="quota-upgrade"><Crown :size="15" />Nâng cấp</RouterLink>
      <span v-else class="quota-remaining">Còn {{ home.entitlements.examsRemaining }} lượt</span>
    </div>
    <p v-if="home.practiceExamNotice" class="practice-exam-notice" role="status">{{ home.practiceExamNotice }}</p>
    <div v-if="home.examTemplates.length" class="section-heading legacy-exam-heading"><div><h2>Tạo đề luyện tập</h2><p>Đề mới sẽ dùng lượt Free khi được tạo thành công.</p></div></div>
    <div v-if="home.examTemplates.length" class="template-test-list">
      <article v-for="(template, index) in home.examTemplates" :key="template.templateId" class="test-row template-test-row">
        <span class="test-icon" :class="`test-icon-${index % 4}`"><BookOpen :size="20" /></span>
        <div class="test-info"><h2>{{ template.title }}</h2><p>{{ template.description || 'Đề luyện tập cá nhân hóa theo cấu trúc đã chọn.' }}</p><div class="test-meta"><span><Clock3 :size="14" />{{ template.durationMinutes }} phút</span><span>{{ (template.sections || []).reduce((sum, section) => sum + Number(section.questionCount || 0), 0) }} câu hỏi</span><span>{{ (template.sections || []).length }} phần</span></div></div>
        <button v-if="home.entitlements?.plan !== 'PREMIUM' && home.entitlements?.examsRemaining <= 0" class="primary-button" @click="$router.push('/pricing')"><Crown :size="14" />Nâng cấp</button>
        <button v-else class="primary-button" :disabled="home.creatingExamTemplateId != null" @click="home.createPracticeExam(template)">{{ home.creatingExamTemplateId === template.templateId ? 'Đang tạo…' : 'Tạo đề' }}<ArrowRight :size="15" /></button>
      </article>
    </div>
    <div v-if="home.filteredExams.length" class="section-heading legacy-exam-heading"><div><h2>Đề kiểm tra của bạn</h2><p>Các bài thi đã tạo hoặc được mở sẵn</p></div></div>
    <div v-if="home.filteredExams.length" class="test-list"><article v-for="(exam, index) in home.filteredExams" :key="exam.examId" class="test-row"><span class="test-icon" :class="`test-icon-${index % 4}`"><BookOpen :size="20" /></span><div class="test-info"><h2>{{ exam.title }}</h2><p>{{ exam.description || 'Bài đánh giá kiến thức tổng hợp' }}</p><div class="test-meta"><span><Clock3 :size="14" /> {{ exam.durationMinutes }} phút</span><span>{{ exam.questionCount || exam.totalQuestions || 0 }} câu hỏi</span><span>{{ exam.totalPoints ?? '—' }} điểm</span></div></div><button class="primary-button" :disabled="home.loading" @click="home.startExam(exam)">Bắt đầu <ArrowRight :size="15" /></button></article></div>
    <div v-if="!home.loading && !home.filteredExams.length && !home.examTemplates.length" class="empty-page"><BookOpen :size="28" /><h2>Chưa có bài kiểm tra</h2><p>Các bài kiểm tra được quản trị viên mở sẽ xuất hiện tại đây.</p></div>
    <div v-if="home.history.length" class="recent-history"><div class="section-heading"><div><h2>Lịch sử gần đây</h2><p>Các lượt làm bài của bạn</p></div><button class="inline-link" @click="home.showView('history')">Xem tất cả <ArrowRight :size="14" /></button></div><article v-for="item in home.history.slice(0, 3)" :key="item.attemptId" class="history-row"><span class="history-status"><CheckCircle2 :size="16" /></span><div><strong>{{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}</strong><small>{{ home.formatDate(item.startedAt) }} · {{ home.localizeStatus(item.status) }}</small></div><strong class="history-score">{{ item.score ?? '—' }}<small v-if="item.maxScore != null"> / {{ item.maxScore }}</small></strong></article></div>
  </section>
</template>
