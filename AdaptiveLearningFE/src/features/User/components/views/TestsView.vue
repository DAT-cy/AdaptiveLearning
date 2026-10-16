<script setup>
import { inject } from 'vue'
import { ArrowRight, BookOpen, CheckCircle2, Clock3, Search } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section class="page-view">
    <div class="view-heading"><div><p class="view-kicker">LUYỆN TẬP VÀ ĐÁNH GIÁ</p><h1>Bài kiểm tra</h1><p>Chọn bài kiểm tra đã được mở để bắt đầu.</p></div><label class="search-field"><Search :size="17" /><input v-model="home.searchQuery" placeholder="Tìm bài kiểm tra" aria-label="Tìm bài kiểm tra"></label></div>
    <div v-if="home.filteredExams.length" class="test-list"><article v-for="(exam, index) in home.filteredExams" :key="exam.examId" class="test-row"><span class="test-icon" :class="`test-icon-${index % 4}`"><BookOpen :size="20" /></span><div class="test-info"><h2>{{ exam.title }}</h2><p>{{ exam.description || 'Bài đánh giá kiến thức tổng hợp' }}</p><div class="test-meta"><span><Clock3 :size="14" /> {{ exam.durationMinutes }} phút</span><span>{{ exam.questionCount || exam.totalQuestions || 0 }} câu hỏi</span><span>{{ exam.totalPoints ?? '—' }} điểm</span></div></div><button class="primary-button" :disabled="home.loading" @click="home.startExam(exam)">Bắt đầu <ArrowRight :size="15" /></button></article></div>
    <div v-if="home.filteredExams.length" class="section-heading legacy-exam-heading"><div><h2>Đề thi đã xuất bản</h2><p>Các bài thi có sẵn trong hệ thống</p></div></div>
    <div v-if="!home.loading && !home.filteredExams.length" class="empty-page"><BookOpen :size="28" /><h2>Chưa có bài kiểm tra</h2><p>Các bài kiểm tra được quản trị viên mở sẽ xuất hiện tại đây.</p></div>
    <div v-if="home.history.length" class="recent-history"><div class="section-heading"><div><h2>Lịch sử gần đây</h2><p>Các lượt làm bài của bạn</p></div><button class="inline-link" @click="home.showView('history')">Xem tất cả <ArrowRight :size="14" /></button></div><article v-for="item in home.history.slice(0, 3)" :key="item.attemptId" class="history-row"><span class="history-status"><CheckCircle2 :size="16" /></span><div><strong>{{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}</strong><small>{{ home.formatDate(item.startedAt) }} · {{ home.localizeStatus(item.status) }}</small></div><strong class="history-score">{{ item.score ?? '—' }}<small v-if="item.maxScore != null"> / {{ item.maxScore }}</small></strong></article></div>
  </section>
</template>
