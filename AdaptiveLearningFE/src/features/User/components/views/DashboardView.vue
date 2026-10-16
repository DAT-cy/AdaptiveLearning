<script setup>
import { inject } from 'vue'
import {
  ArrowRight, BarChart3, BookMarked, BookOpen, Brain, CalendarDays, Check,
  CheckCircle2, Compass, Flame, Plus, Sparkles, Target, Trophy,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <div class="dashboard-grid">
    <div class="main-column">
      <section class="welcome-banner">
        <div class="welcome-copy">
          <p class="welcome-kicker">KHÔNG GIAN HỌC TẬP CỦA BẠN</p>
          <h1>{{ home.greeting }}, {{ home.greetingName }}!</h1>
          <p>Hôm nay là một ngày tốt để học hỏi và phát triển bản thân.<br class="wide-break"> Cùng khám phá bước tiếp theo trong hành trình của bạn.</p>
          <div class="quick-stats">
            <div class="quick-stat"><span class="quick-icon lavender"><BookOpen :size="18" /></span><span><strong>{{ home.activePlans.length }}</strong><small>Lộ trình đang học</small></span></div>
            <div class="quick-stat"><span class="quick-icon sky"><Target :size="18" /></span><span><strong>{{ home.doneCount }}</strong><small>Bài đã hoàn thành</small></span></div>
            <div class="quick-stat"><span class="quick-icon peach"><Flame :size="18" /></span><span><strong>{{ home.publishedExams.length }}</strong><small>Đề luyện tập</small></span></div>
          </div>
        </div>
        <div class="welcome-art" aria-hidden="true"><div class="art-orbit orbit-a"></div><div class="art-orbit orbit-b"></div><span class="art-node node-book"><BookOpen :size="19" /></span><span class="art-node node-ai"><Brain :size="22" /></span><span class="art-node node-bars"><BarChart3 :size="20" /></span><span class="art-node node-check"><Check :size="15" /></span><span class="art-track track-one"></span><span class="art-track track-two"></span><span class="art-spark spark-one">✦</span><span class="art-spark spark-two">✧</span></div>
      </section>

      <section class="section-block continue-section">
        <div class="section-heading"><div><h2>Tiếp tục học tập</h2><p>Lộ trình bạn đang theo đuổi</p></div><button class="inline-link" @click="home.showView('plans')">Xem tất cả <ArrowRight :size="15" /></button></div>
        <article v-if="home.activePlan" class="continue-card">
          <div class="course-art"><span class="course-label">LỘ TRÌNH</span><Compass :size="38" /><div class="course-art-ring"></div><small>{{ home.selectedCertificate?.name || 'Học tập thích ứng' }}</small></div>
          <div class="continue-body"><div class="continue-title"><h3>{{ home.activePlan.title }}</h3><span class="status-label"><span></span>Đang học</span></div><p class="course-tags"><span>{{ home.selectedCertificate?.name || 'Mục tiêu học tập' }}</span><span>{{ home.activePlan.hoursPerWeek || 0 }} giờ / tuần</span></p><div class="plan-progress"><div><span :style="{ width: `${home.completedPlanRatio}%` }"></span></div><small>{{ home.completedPlanRatio }}%</small></div><div class="continue-meta"><span><CalendarDays :size="13" /> {{ home.formatDate(home.activePlan.examDate) }}</span><span>{{ home.activePlan.completedCount || 0 }}/{{ home.activePlan.taskCount || 0 }} nhiệm vụ</span></div></div>
          <button class="continue-button" @click="home.showView('plans')"><ArrowRight :size="16" /> Mở lộ trình</button>
        </article>
        <div v-else class="empty-inline"><span class="empty-symbol"><BookMarked :size="20" /></span><div><strong>Chưa có lộ trình học</strong><p>Tạo mục tiêu để hệ thống sắp xếp kế hoạch học tập cho bạn.</p></div><button class="outline-button" @click="home.showView('plans'); home.showPlanForm = true"><Plus :size="16" /> Tạo lộ trình</button></div>
      </section>

      <section class="section-block plan-overview">
        <div class="section-heading"><div><h2>Năng lực học tập</h2><p>{{ home.selectedCertificate?.name || 'Chọn chứng chỉ để xem mức độ thành thạo' }}</p></div><button class="inline-link" @click="home.showView('progress')">Xem chi tiết <ArrowRight :size="15" /></button></div>
        <div class="skill-overview">
          <div class="mastery-summary"><div class="mastery-ring" :style="{ '--mastery': `${home.averageScorePercent}%` }"><span>{{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : '—' }}</span></div><strong>Điểm trung bình</strong><small>{{ home.progress?.totalAttempts || 0 }} lượt đã nộp</small></div>
          <div v-if="home.masterySkills.length" class="skill-list"><div v-for="(skill, index) in home.masterySkills.slice(0, 4)" :key="skill.name" class="skill-row"><div class="skill-label"><span class="skill-bullet" :class="`bullet-${index % 4}`"><Target :size="13" /></span><strong>{{ skill.name }}</strong><small>{{ Math.round(skill.value) }}%</small></div><div class="skill-bar"><span :class="`bar-${index % 4}`" :style="{ width: `${Math.max(0, Math.min(100, skill.value))}%` }"></span></div></div></div>
          <div v-else class="skills-empty"><Sparkles :size="22" /><strong>Chưa đủ dữ liệu đánh giá</strong><span>Hoàn thành một bài kiểm tra để xem phân tích kỹ năng.</span><button class="text-link" @click="home.showView('tests')">Tìm bài kiểm tra <ArrowRight :size="14" /></button></div>
        </div>
      </section>

      <section id="recommendations" class="section-block recommendation-section">
        <div class="section-heading"><div><h2>Bài học được đề xuất</h2><p>Dựa trên dữ liệu học tập của bạn</p></div><button class="inline-link" @click="home.showView('progress')">Xem phân tích <ArrowRight :size="15" /></button></div>
        <div v-if="home.activeRecommendations.length" class="recommendation-grid"><article v-for="(item, index) in home.activeRecommendations.slice(0, 3)" :key="item.recommendationId || index" class="recommendation-card"><span class="recommendation-icon" :class="`recommend-icon-${index % 3}`"><Target v-if="index === 0" :size="17" /><Brain v-else-if="index === 1" :size="17" /><BookOpen v-else :size="17" /></span><div class="recommendation-copy"><strong>{{ home.recommendationTitle(item, index) }}</strong><p>{{ item.reason || 'Gợi ý dựa trên kết quả học tập gần đây.' }}</p><span class="recommendation-level">{{ home.localizeStatus(item.priority || item.status || 'Đề xuất') }}</span></div><button class="recommendation-action" :disabled="item.status === 'STARTED'" :title="item.status === 'STARTED' ? 'Đang thực hiện' : 'Bắt đầu học'" @click="home.markRecommendationStarted(item)"><ArrowRight :size="16" /></button></article></div>
        <div v-else class="empty-recommendation"><Sparkles :size="19" /><span>Chưa có gợi ý mới. Hãy làm một bài kiểm tra để nhận đề xuất phù hợp.</span><button class="text-link" @click="home.showView('tests')">Chọn bài kiểm tra <ArrowRight :size="14" /></button></div>
      </section>
    </div>

    <aside class="right-column">
      <section class="assistant-panel"><div class="panel-heading"><span class="assistant-mark"><Brain :size="19" /></span><h2>Trợ lý AI</h2><span class="personalized"><Sparkles :size="12" /> Cá nhân hóa</span></div><p class="assistant-description">Hỏi về bài học, kết quả hoặc những chủ đề bạn muốn hiểu sâu hơn.</p><div class="assistant-message"><span class="bot-avatar"><Brain :size="18" /></span><p>{{ home.activeRecommendations[0]?.reason || 'Mình có thể giúp bạn giải đáp thắc mắc và ôn tập theo mục tiêu học tập.' }}</p></div><button class="assistant-button" @click="home.showView('tutor')"><Sparkles :size="16" /> Hỏi AI ngay <ArrowRight :size="16" /></button></section>
      <section class="weekly-panel"><div class="panel-heading"><span class="small-panel-icon purple"><BarChart3 :size="16" /></span><h2>Tiến độ của bạn</h2><button class="inline-link" @click="home.showView('progress')">Chi tiết <ArrowRight :size="14" /></button></div><label v-if="home.certificates.length" class="certificate-select-label"><span>Chứng chỉ</span><select v-model="home.selectedCertificateId" @change="home.loadProgress"><option v-for="certificate in home.certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label><div class="metric-grid"><div class="metric-cell"><span class="metric-icon blue"><Trophy :size="16" /></span><strong>{{ home.progress?.totalAttempts ?? home.history.length }}</strong><small>Lượt làm bài</small></div><div class="metric-cell"><span class="metric-icon mint"><CheckCircle2 :size="16" /></span><strong>{{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : home.averageScore != null ? home.averageScore.toFixed(1) : '—' }}</strong><small>Điểm trung bình</small></div><div class="metric-cell"><span class="metric-icon violet"><BookMarked :size="16" /></span><strong>{{ home.activePlans.length }}</strong><small>Lộ trình đang học</small></div><div class="metric-cell"><span class="metric-icon amber"><Flame :size="16" /></span><strong>{{ home.activeRecommendations.length }}</strong><small>Gợi ý cần xem</small></div></div></section>
      <section class="goal-panel"><div class="panel-heading"><span class="small-panel-icon blue"><Target :size="16" /></span><h2>Mục tiêu hiện tại</h2><button class="inline-link" @click="home.showView('plans')">Chi tiết <ArrowRight :size="14" /></button></div><div v-if="home.activePlan" class="goal-content"><div class="goal-heading"><span class="goal-symbol"><Target :size="18" /></span><strong>{{ home.activePlan.title }}</strong></div><div class="goal-track"><span :style="{ width: `${home.completedPlanRatio}%` }"></span></div><div class="goal-details"><span>{{ home.completedPlanRatio }}% hoàn thành</span><span>{{ home.activePlan.targetScore ? `Mục tiêu ${home.activePlan.targetScore} điểm` : home.formatDate(home.activePlan.examDate) }}</span></div></div><div v-else class="goal-empty"><p>Bạn chưa đặt mục tiêu học tập.</p><button class="text-link" @click="home.showView('plans'); home.showPlanForm = true">Tạo mục tiêu <ArrowRight :size="14" /></button></div></section>
    </aside>
  </div>
</template>
