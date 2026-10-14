<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight, BarChart3, Bell, BookMarked, BookOpen, Brain, CalendarDays, Check, CheckCircle2,
  ChevronDown, CircleHelp, Clock3, Compass, Flame, GraduationCap, LayoutDashboard,
  LogOut, Menu, Plus, Search, Send, Settings, Sparkles, Target,
  Trophy, UserRound, X,
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { learningService } from '@/services/learningService'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeView = ref(route.meta.view || 'dashboard')
const searchQuery = ref('')
const exams = ref([])
const history = ref([])
const recommendations = ref([])
const certificates = ref([])
const plans = ref([])
const progress = ref(null)
const loading = ref(false)
const pageError = ref('')
const selectedCertificateId = ref('')
const showPlanForm = ref(false)
const creatingPlan = ref(false)
const planForm = reactive({ title: '', targetScore: '', examDate: '', hoursPerWeek: 5 })
const attempt = ref(null)
const attemptQuestions = ref([])
const answers = reactive({})
const result = ref(null)
const secondsLeft = ref(0)
const submitting = ref(false)
const saveState = ref('')
const saveTimers = new Map()
const conversations = ref([])
const currentConversation = ref(null)
const messages = ref([])
const tutorPrompt = ref('')
const askingTutor = ref(false)
const mobileMenuOpen = ref(false)
let clock

const viewPaths = {
  dashboard: '/home',
  plans: '/plans',
  courses: '/courses',
  tests: '/tests',
  progress: '/progress',
  history: '/history',
  tutor: '/tutor',
  settings: '/settings',
}

watch(() => route.meta.view, (view) => {
  if (view) activeView.value = view
  if (view === 'tutor') loadConversations()
}, { immediate: true })

const navigation = [
  { id: 'dashboard', label: 'Trang chủ', icon: LayoutDashboard },
  { id: 'plans', label: 'Lộ trình học', icon: BookMarked },
  { id: 'courses', label: 'Khóa học', icon: BookOpen },
  { id: 'tests', label: 'Bài kiểm tra', icon: CheckCircle2 },
  { id: 'progress', label: 'Thống kê tiến độ', icon: BarChart3 },
  { id: 'tutor', label: 'Trợ lý AI', icon: Sparkles },
  { id: 'settings', label: 'Cài đặt', icon: Settings },
]

const userName = computed(() => userStore.state.username || 'Học viên')
const greetingName = computed(() => userName.value.split('@')[0].split(/[._-]/)[0])
const initials = computed(() => userName.value.slice(0, 1).toUpperCase())
const publishedExams = computed(() => exams.value.filter((exam) => exam.status === 'PUBLISHED'))
const filteredExams = computed(() => publishedExams.value.filter((exam) => {
  const query = searchQuery.value.trim().toLowerCase()
  return !query || `${exam.title} ${exam.description || ''}`.toLowerCase().includes(query)
}))
const activePlans = computed(() => plans.value.filter((plan) => plan.status === 'ACTIVE'))
const activePlan = computed(() => activePlans.value[0] || null)
const activeRecommendations = computed(() => recommendations.value.filter((item) => item.status !== 'COMPLETED'))
const scoreHistory = computed(() => history.value
  .filter((item) => item.status === 'SUBMITTED' && item.score != null)
  .slice(0, 7)
  .reverse())
const scoreTrend = computed(() => scoreHistory.value.map((item, index) => ({
  label: item.startedAt ? new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(new Date(item.startedAt)) : `Lần ${index + 1}`,
  score: Number(item.score),
  maxScore: Number(item.maxScore) || Math.max(...scoreHistory.value.map((entry) => Number(entry.maxScore) || Number(entry.score) || 0), 1),
})))
const averageScore = computed(() => {
  const submitted = history.value.filter((item) => item.score != null)
  if (!submitted.length) return null
  return submitted.reduce((sum, item) => sum + Number(item.score), 0) / submitted.length
})
const averageScorePercent = computed(() => {
  const maxScore = Number(selectedCertificate.value?.totalScore)
  if (progress.value?.averageScore == null || !maxScore) return 0
  return Math.max(0, Math.min(100, Number(progress.value.averageScore) / maxScore * 100))
})
const doneCount = computed(() => history.value.filter((item) => item.status === 'SUBMITTED').length)
const completedPlanRatio = computed(() => activePlan.value?.taskCount
  ? Math.round((activePlan.value.completedCount / activePlan.value.taskCount) * 100)
  : 0)
const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 11 ? 'Chào buổi sáng' : hour < 17 ? 'Chào buổi chiều' : 'Chào buổi tối'
})
const selectedCertificate = computed(() => certificates.value.find((item) => String(item.certificateId) === String(selectedCertificateId.value)))
const masterySkills = computed(() => {
  const source = progress.value?.masteryBySkillJson
  if (!source) return []
  try {
    const parsed = typeof source === 'string' ? JSON.parse(source) : source
    if (Array.isArray(parsed)) return parsed.map((item) => ({ name: item.skill || item.name || item.topic || 'Kỹ năng', value: Number(item.mastery ?? item.score ?? item.value ?? 0) }))
    if (parsed && typeof parsed === 'object') return Object.entries(parsed).map(([name, value]) => ({ name, value: Number(typeof value === 'object' ? value.mastery ?? value.score ?? value.value ?? 0 : value) }))
  } catch { return [] }
  return []
})
const formattedTimeLeft = computed(() => `${Math.floor(secondsLeft.value / 60).toString().padStart(2, '0')}:${(secondsLeft.value % 60).toString().padStart(2, '0')}`)

function asList(value) {
  if (Array.isArray(value)) return value
  return value?.content || value?.items || []
}

function localizeStatus(status) {
  const labels = {
    ACTIVE: 'Đang học',
    ARCHIVED: 'Đã lưu trữ',
    COMPLETED: 'Hoàn thành',
    EXPIRED: 'Đã hết hạn',
    GRADED: 'Đã chấm điểm',
    GRADING: 'Đang chấm điểm',
    HIGH: 'Ưu tiên cao',
    IN_PROGRESS: 'Đang làm',
    LOW: 'Ưu tiên thấp',
    MEDIUM: 'Ưu tiên vừa',
    PENDING: 'Đang chờ',
    STARTED: 'Đang thực hiện',
    SUBMITTED: 'Đã nộp',
    VIOLATED: 'Có vi phạm',
  }
  return labels[status] || status
}

function showView(view) {
  mobileMenuOpen.value = false
  pageError.value = ''
  if (viewPaths[view]) {
    if (view === 'history') activeView.value = 'history'
    router.push(viewPaths[view])
  } else {
    activeView.value = view
  }
  if (view === 'progress' && selectedCertificateId.value) loadProgress()
}

async function loadDashboard() {
  loading.value = true
  pageError.value = ''
  const results = await Promise.allSettled([
    learningService.exams(), learningService.history(), learningService.recommendations(),
    learningService.certificates(), learningService.plans(),
  ])
  const [examResult, historyResult, recommendationResult, certificateResult, plansResult] = results
  if (examResult.status === 'fulfilled') exams.value = asList(examResult.value)
  if (historyResult.status === 'fulfilled') history.value = asList(historyResult.value)
  if (recommendationResult.status === 'fulfilled') recommendations.value = asList(recommendationResult.value)
  if (certificateResult.status === 'fulfilled') {
    certificates.value = asList(certificateResult.value)
    if (!selectedCertificateId.value && certificates.value.length) selectedCertificateId.value = String(certificates.value[0].certificateId)
  }
  if (plansResult.status === 'fulfilled') plans.value = asList(plansResult.value)
  if (results.every((item) => item.status === 'rejected')) {
    pageError.value = 'Không kết nối được backend. Hãy kiểm tra backend và cơ sở dữ liệu rồi tải lại.'
  }
  loading.value = false
  if (selectedCertificateId.value) loadProgress()
}

async function loadProgress() {
  if (!selectedCertificateId.value) return
  try {
    progress.value = await learningService.progress(selectedCertificateId.value)
  } catch {
    progress.value = null
  }
}

function formatDate(value) {
  if (!value) return 'Chưa đặt ngày thi'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value))
}

function recommendationTitle(item, index = 0) {
  return item.title || item.skill || item.topicName || `Gợi ý học tập ${index + 1}`
}

function certificateProgress(certificate) {
  const certificateExams = publishedExams.value.filter((exam) => String(exam.certificateId) === String(certificate.certificateId))
  if (!certificateExams.length) return { completed: 0, total: 0, percent: 0 }
  const examIds = new Set(certificateExams.map((exam) => String(exam.examId)))
  const completed = new Set(history.value
    .filter((item) => item.status === 'SUBMITTED' && examIds.has(String(item.examId)))
    .map((item) => String(item.examId))).size
  return { completed, total: certificateExams.length, percent: Math.round(completed / certificateExams.length * 100) }
}

async function markRecommendationStarted(item) {
  if (!item.recommendationId) return
  try {
    await learningService.startRecommendation(item.recommendationId)
    recommendations.value = recommendations.value.map((entry) => entry.recommendationId === item.recommendationId ? { ...entry, status: 'STARTED' } : entry)
  } catch (error) {
    pageError.value = error?.response?.data?.responseMessage || 'Chưa thể cập nhật gợi ý này.'
  }
}

async function createPlan() {
  creatingPlan.value = true
  pageError.value = ''
  try {
    const created = await learningService.createPlan({
      title: planForm.title.trim(),
      certificateId: selectedCertificateId.value ? Number(selectedCertificateId.value) : null,
      targetScore: planForm.targetScore ? Number(planForm.targetScore) : null,
      examDate: planForm.examDate,
      hoursPerWeek: Number(planForm.hoursPerWeek),
    })
    plans.value = [created, ...plans.value]
    Object.assign(planForm, { title: '', targetScore: '', examDate: '', hoursPerWeek: 5 })
    showPlanForm.value = false
  } catch (error) {
    pageError.value = error?.response?.data?.responseMessage || 'Không thể tạo lộ trình học.'
  } finally {
    creatingPlan.value = false
  }
}

async function loadConversations() {
  try {
    conversations.value = asList(await learningService.conversations())
    if (!currentConversation.value && conversations.value.length) await selectConversation(conversations.value[0])
  } catch { conversations.value = [] }
}

async function selectConversation(conversation) {
  currentConversation.value = conversation
  try { messages.value = asList(await learningService.messages(conversation.id)) }
  catch { messages.value = [] }
}

async function askTutor() {
  const prompt = tutorPrompt.value.trim()
  if (!prompt || askingTutor.value) return
  askingTutor.value = true
  pageError.value = ''
  try {
    if (!currentConversation.value) {
      currentConversation.value = await learningService.createConversation({
        certificateId: selectedCertificateId.value ? Number(selectedCertificateId.value) : null,
        title: prompt.slice(0, 80),
      })
      conversations.value.unshift(currentConversation.value)
    }
    await learningService.askTutor(currentConversation.value.id, { content: prompt })
    tutorPrompt.value = ''
    messages.value = asList(await learningService.messages(currentConversation.value.id))
  } catch (error) {
    pageError.value = error?.response?.data?.responseMessage || 'Trợ lý chưa thể trả lời lúc này.'
  } finally {
    askingTutor.value = false
  }
}

function parseOptions(question) {
  try {
    const parsed = JSON.parse(question.optionsJson || '[]')
    if (Array.isArray(parsed)) return parsed.map((option, index) => typeof option === 'string'
      ? { value: option, label: option }
      : { value: option.value ?? option.label ?? String(index), label: option.label ? `${option.label}. ${option.text ?? option.value ?? ''}` : option.text ?? option.value ?? String(index) })
    if (parsed && typeof parsed === 'object') return Object.entries(parsed).map(([value, label]) => ({ value, label: `${value}. ${label}` }))
  } catch { /* Some question formats use free-text answers. */ }
  return []
}

function restoreAnswer(raw) {
  try {
    const value = JSON.parse(raw)
    return value?.selected ?? value?.text ?? value?.value ?? ''
  } catch { return raw || '' }
}

function makeAnswer(question) {
  const value = typeof answers[question.questionId] === 'string' ? answers[question.questionId].trim() : answers[question.questionId]
  return {
    questionId: question.questionId,
    userAnswerJson: JSON.stringify(parseOptions(question).length ? { selected: value } : { text: value }),
    timeSpentSeconds: question.estimatedTimeSeconds || 0,
  }
}

async function startExam(exam) {
  loading.value = true
  pageError.value = ''
  result.value = null
  try {
    const examDetail = await learningService.exam(exam.examId)
    const started = await learningService.startAttempt(exam.examId)
    attempt.value = started
    attemptQuestions.value = await Promise.all((examDetail?.questions || []).map(async (mapping) => ({
      ...await learningService.question(mapping.questionId),
      questionOrder: mapping.questionOrder,
      sectionName: mapping.sectionName,
    })))
    const saved = await learningService.attempt(started.attemptId)
    for (const item of saved?.answers || []) answers[item.questionId] = restoreAnswer(item.userAnswerJson)
    secondsLeft.value = saved?.timeRemainingSeconds || (exam.durationMinutes || 0) * 60
    activeView.value = 'taking'
    clock = setInterval(() => {
      if (secondsLeft.value > 0) secondsLeft.value -= 1
      if (!secondsLeft.value && attempt.value && !submitting.value) submitExam()
    }, 1000)
  } catch (error) {
    pageError.value = error?.response?.data?.responseMessage || error?.message || 'Không thể bắt đầu bài thi.'
  } finally {
    loading.value = false
  }
}

function saveAnswer(question) {
  if (!attempt.value) return
  saveState.value = 'Đang lưu đáp án...'
  clearTimeout(saveTimers.get(question.questionId))
  saveTimers.set(question.questionId, setTimeout(async () => {
    try {
      await learningService.saveAnswer(attempt.value.attemptId, makeAnswer(question))
      saveState.value = 'Đã lưu đáp án'
    } catch { saveState.value = 'Đáp án sẽ được gửi khi nộp bài' }
  }, 350))
}

async function submitExam() {
  if (!attempt.value || submitting.value) return
  submitting.value = true
  pageError.value = ''
  for (const timer of saveTimers.values()) clearTimeout(timer)
  try {
    result.value = await learningService.submitAttempt(attempt.value.attemptId, attemptQuestions.value
      .filter((question) => answers[question.questionId] !== undefined && answers[question.questionId] !== '')
      .map(makeAnswer))
    clearInterval(clock)
    attempt.value = null
    activeView.value = 'result'
    await loadDashboard()
  } catch (error) {
    pageError.value = error?.response?.data?.responseMessage || 'Không thể nộp bài.'
  } finally { submitting.value = false }
}

function leaveAttempt() {
  if (attempt.value && !window.confirm('Rời bài thi? Lượt thi hiện tại vẫn được lưu.')) return
  clearInterval(clock)
  attempt.value = null
  activeView.value = 'tests'
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(loadDashboard)
onUnmounted(() => {
  clearInterval(clock)
  for (const timer of saveTimers.values()) clearTimeout(timer)
})
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar" :class="{ 'sidebar-open': mobileMenuOpen }">
      <a class="sidebar-brand" href="/home"><span class="brand-symbol"><GraduationCap :size="28" /></span><span>Adaptive Learning</span></a>
      <nav class="side-nav" aria-label="Điều hướng chính">
        <button v-for="item in navigation" :key="item.id" :class="['side-link', { selected: activeView === item.id || (item.id === 'tests' && ['taking', 'result'].includes(activeView)) }]" @click="showView(item.id)"><component :is="item.icon" :size="19" :stroke-width="1.9" /><span>{{ item.label }}</span></button>
      </nav>
      <div class="sidebar-quote"><span class="quote-icon"><Target :size="22" /></span><strong>Học hôm nay,<br>kiến tạo tương lai</strong><p>“Mỗi bước nhỏ đều là một tiến bộ lớn.”</p><div class="quote-mountain"><span></span><i></i><b>⚑</b></div></div>
      <div class="sidebar-bottom"><span class="connection-dot"></span><span>Adaptive Learning</span><span class="version">Học tập cá nhân hóa</span></div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <button class="mobile-menu-button" :aria-label="mobileMenuOpen ? 'Đóng menu' : 'Mở menu'" @click="mobileMenuOpen = !mobileMenuOpen"><X v-if="mobileMenuOpen" :size="20" /><Menu v-else :size="20" /></button>
        <label class="global-search"><Search :size="18" /><input v-model="searchQuery" type="search" placeholder="Tìm kiếm khóa học, bài học, kỹ năng..." aria-label="Tìm kiếm"></label>
        <button class="notification-button" title="Xem gợi ý học tập" aria-label="Xem gợi ý học tập" @click="showView('dashboard'); $nextTick(() => document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'center' }))"><Bell :size="19" /><span v-if="activeRecommendations.length" class="notification-dot"></span></button>
        <button class="profile-button" @click="showView('settings')"><span class="avatar">{{ initials }}</span><span class="profile-copy"><strong>{{ userName }}</strong><small>{{ userStore.state.role === 'ROLE_ADMIN' ? 'Quản trị viên' : 'Học viên' }}</small></span><ChevronDown :size="16" /></button>
      </header>

      <div class="mobile-scrim" v-if="mobileMenuOpen" @click="mobileMenuOpen = false"></div>
      <div class="content-area">
        <div v-if="pageError" class="notice" role="alert"><CircleHelp :size="17" />{{ pageError }}<button class="notice-close" aria-label="Đóng thông báo" @click="pageError = ''"><X :size="16" /></button></div>

        <template v-if="activeView === 'dashboard'">
          <div class="dashboard-grid">
            <div class="main-column">
              <section class="welcome-banner">
                <div class="welcome-copy"><p class="welcome-kicker">KHÔNG GIAN HỌC TẬP CỦA BẠN</p><h1>{{ greeting }}, {{ greetingName }}!</h1><p>Hôm nay là một ngày tốt để học hỏi và phát triển bản thân.<br class="wide-break"> Cùng khám phá bước tiếp theo trong hành trình của bạn.</p>
                  <div class="quick-stats"><div class="quick-stat"><span class="quick-icon lavender"><BookOpen :size="18" /></span><span><strong>{{ activePlans.length }}</strong><small>Lộ trình đang học</small></span></div><div class="quick-stat"><span class="quick-icon sky"><Target :size="18" /></span><span><strong>{{ doneCount }}</strong><small>Bài đã hoàn thành</small></span></div><div class="quick-stat"><span class="quick-icon peach"><Flame :size="18" /></span><span><strong>{{ publishedExams.length }}</strong><small>Đề luyện tập</small></span></div></div>
                </div>
                <div class="welcome-art" aria-hidden="true"><div class="art-orbit orbit-a"></div><div class="art-orbit orbit-b"></div><span class="art-node node-book"><BookOpen :size="19" /></span><span class="art-node node-ai"><Brain :size="22" /></span><span class="art-node node-bars"><BarChart3 :size="20" /></span><span class="art-node node-check"><Check :size="15" /></span><span class="art-track track-one"></span><span class="art-track track-two"></span><span class="art-spark spark-one">✦</span><span class="art-spark spark-two">✧</span></div>
              </section>

              <section class="section-block continue-section">
                <div class="section-heading"><div><h2>Tiếp tục học tập</h2><p>Lộ trình bạn đang theo đuổi</p></div><button class="inline-link" @click="showView('plans')">Xem tất cả <ArrowRight :size="15" /></button></div>
                <article v-if="activePlan" class="continue-card">
                  <div class="course-art"><span class="course-label">LỘ TRÌNH</span><Compass :size="38" /><div class="course-art-ring"></div><small>{{ selectedCertificate?.name || 'Học tập thích ứng' }}</small></div>
                  <div class="continue-body"><div class="continue-title"><h3>{{ activePlan.title }}</h3><span class="status-label"><span></span>Đang học</span></div><p class="course-tags"><span>{{ selectedCertificate?.name || 'Mục tiêu học tập' }}</span><span>{{ activePlan.hoursPerWeek || 0 }} giờ / tuần</span></p><div class="plan-progress"><div><span :style="{ width: `${completedPlanRatio}%` }"></span></div><small>{{ completedPlanRatio }}%</small></div><div class="continue-meta"><span><CalendarDays :size="13" /> {{ formatDate(activePlan.examDate) }}</span><span>{{ activePlan.completedCount || 0 }}/{{ activePlan.taskCount || 0 }} nhiệm vụ</span></div></div>
                  <button class="continue-button" @click="showView('plans')"><ArrowRight :size="16" /> Mở lộ trình</button>
                </article>
                <div v-else class="empty-inline"><span class="empty-symbol"><BookMarked :size="20" /></span><div><strong>Chưa có lộ trình học</strong><p>Tạo mục tiêu để hệ thống sắp xếp kế hoạch học tập cho bạn.</p></div><button class="outline-button" @click="showView('plans'); showPlanForm = true"><Plus :size="16" /> Tạo lộ trình</button></div>
              </section>

              <section class="section-block plan-overview">
                <div class="section-heading"><div><h2>Năng lực học tập</h2><p>{{ selectedCertificate?.name || 'Chọn chứng chỉ để xem mức độ thành thạo' }}</p></div><button class="inline-link" @click="showView('progress')">Xem chi tiết <ArrowRight :size="15" /></button></div>
                <div class="skill-overview">
                  <div class="mastery-summary"><div class="mastery-ring" :style="{ '--mastery': `${averageScorePercent}%` }"><span>{{ progress?.averageScore != null ? Number(progress.averageScore).toFixed(1) : '—' }}</span></div><strong>Điểm trung bình</strong><small>{{ progress?.totalAttempts || 0 }} lượt đã nộp</small></div>
                  <div v-if="masterySkills.length" class="skill-list"><div v-for="(skill, index) in masterySkills.slice(0, 4)" :key="skill.name" class="skill-row"><div class="skill-label"><span class="skill-bullet" :class="`bullet-${index % 4}`"><Target :size="13" /></span><strong>{{ skill.name }}</strong><small>{{ Math.round(skill.value) }}%</small></div><div class="skill-bar"><span :class="`bar-${index % 4}`" :style="{ width: `${Math.max(0, Math.min(100, skill.value))}%` }"></span></div></div></div>
                  <div v-else class="skills-empty"><Sparkles :size="22" /><strong>Chưa đủ dữ liệu đánh giá</strong><span>Hoàn thành một bài kiểm tra để xem phân tích kỹ năng.</span><button class="text-link" @click="showView('tests')">Tìm bài kiểm tra <ArrowRight :size="14" /></button></div>
                </div>
              </section>

              <section id="recommendations" class="section-block recommendation-section">
                <div class="section-heading"><div><h2>Bài học được đề xuất</h2><p>Dựa trên dữ liệu học tập của bạn</p></div><button class="inline-link" @click="showView('progress')">Xem phân tích <ArrowRight :size="15" /></button></div>
                <div v-if="activeRecommendations.length" class="recommendation-grid"><article v-for="(item, index) in activeRecommendations.slice(0, 3)" :key="item.recommendationId || index" class="recommendation-card"><span class="recommendation-icon" :class="`recommend-icon-${index % 3}`"><Target v-if="index === 0" :size="17" /><Brain v-else-if="index === 1" :size="17" /><BookOpen v-else :size="17" /></span><div class="recommendation-copy"><strong>{{ recommendationTitle(item, index) }}</strong><p>{{ item.reason || 'Gợi ý dựa trên kết quả học tập gần đây.' }}</p><span class="recommendation-level">{{ localizeStatus(item.priority || item.status || 'Đề xuất') }}</span></div><button class="recommendation-action" :disabled="item.status === 'STARTED'" :title="item.status === 'STARTED' ? 'Đang thực hiện' : 'Bắt đầu học'" @click="markRecommendationStarted(item)"><ArrowRight :size="16" /></button></article></div>
                <div v-else class="empty-recommendation"><Sparkles :size="19" /><span>Chưa có gợi ý mới. Hãy làm một bài kiểm tra để nhận đề xuất phù hợp.</span><button class="text-link" @click="showView('tests')">Chọn bài kiểm tra <ArrowRight :size="14" /></button></div>
              </section>
            </div>

            <aside class="right-column">
              <section class="assistant-panel">
                <div class="panel-heading"><span class="assistant-mark"><Brain :size="19" /></span><h2>Trợ lý AI</h2><span class="personalized"><Sparkles :size="12" /> Cá nhân hóa</span></div>
                <p class="assistant-description">Hỏi về bài học, kết quả hoặc những chủ đề bạn muốn hiểu sâu hơn.</p>
                <div class="assistant-message"><span class="bot-avatar"><Brain :size="18" /></span><p>{{ activeRecommendations[0]?.reason || 'Mình có thể giúp bạn giải đáp thắc mắc và ôn tập theo mục tiêu học tập.' }}</p></div>
                <button class="assistant-button" @click="showView('tutor')"><Sparkles :size="16" /> Hỏi AI ngay <ArrowRight :size="16" /></button>
              </section>

              <section class="weekly-panel">
                <div class="panel-heading"><span class="small-panel-icon purple"><BarChart3 :size="16" /></span><h2>Tiến độ của bạn</h2><button class="inline-link" @click="showView('progress')">Chi tiết <ArrowRight :size="14" /></button></div>
                <label v-if="certificates.length" class="certificate-select-label"><span>Chứng chỉ</span><select v-model="selectedCertificateId" @change="loadProgress"><option v-for="certificate in certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label>
                <div class="metric-grid"><div class="metric-cell"><span class="metric-icon blue"><Trophy :size="16" /></span><strong>{{ progress?.totalAttempts ?? history.length }}</strong><small>Lượt làm bài</small></div><div class="metric-cell"><span class="metric-icon mint"><CheckCircle2 :size="16" /></span><strong>{{ progress?.averageScore != null ? Number(progress.averageScore).toFixed(1) : averageScore != null ? averageScore.toFixed(1) : '—' }}</strong><small>Điểm trung bình</small></div><div class="metric-cell"><span class="metric-icon violet"><BookMarked :size="16" /></span><strong>{{ activePlans.length }}</strong><small>Lộ trình đang học</small></div><div class="metric-cell"><span class="metric-icon amber"><Flame :size="16" /></span><strong>{{ activeRecommendations.length }}</strong><small>Gợi ý cần xem</small></div></div>
              </section>

              <section class="goal-panel">
                <div class="panel-heading"><span class="small-panel-icon blue"><Target :size="16" /></span><h2>Mục tiêu hiện tại</h2><button class="inline-link" @click="showView('plans')">Chi tiết <ArrowRight :size="14" /></button></div>
                <div v-if="activePlan" class="goal-content"><div class="goal-heading"><span class="goal-symbol"><Target :size="18" /></span><strong>{{ activePlan.title }}</strong></div><div class="goal-track"><span :style="{ width: `${completedPlanRatio}%` }"></span></div><div class="goal-details"><span>{{ completedPlanRatio }}% hoàn thành</span><span>{{ activePlan.targetScore ? `Mục tiêu ${activePlan.targetScore} điểm` : formatDate(activePlan.examDate) }}</span></div></div>
                <div v-else class="goal-empty"><p>Bạn chưa đặt mục tiêu học tập.</p><button class="text-link" @click="showView('plans'); showPlanForm = true">Tạo mục tiêu <ArrowRight :size="14" /></button></div>
              </section>
            </aside>
          </div>
        </template>

        <section v-else-if="activeView === 'plans'" class="page-view">
          <div class="view-heading"><div><p class="view-kicker">KẾ HOẠCH CỦA BẠN</p><h1>Lộ trình học</h1><p>Theo dõi mục tiêu và lịch học được xây dựng cho bạn.</p></div><button class="primary-button" @click="showPlanForm = !showPlanForm"><Plus :size="17" /> Tạo lộ trình</button></div>
          <form v-if="showPlanForm" class="plan-form" @submit.prevent="createPlan"><div class="form-heading"><div><h2>Tạo lộ trình mới</h2><p>Chọn mục tiêu và thời gian học mỗi tuần.</p></div><button class="icon-action" type="button" aria-label="Đóng" @click="showPlanForm = false"><X :size="18" /></button></div><label class="form-field">Tên lộ trình<input v-model="planForm.title" required maxlength="200" placeholder="Ví dụ: Luyện thi IELTS" /></label><div class="form-row"><label class="form-field">Chứng chỉ<select v-model="selectedCertificateId"><option value="">Không chọn</option><option v-for="certificate in certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label><label class="form-field">Ngày dự kiến thi<input v-model="planForm.examDate" type="date" required /></label></div><div class="form-row"><label class="form-field">Điểm mục tiêu<input v-model="planForm.targetScore" type="number" min="0" step="0.5" placeholder="Ví dụ: 7.0" /></label><label class="form-field">Giờ học mỗi tuần<input v-model.number="planForm.hoursPerWeek" type="number" min="1" max="80" required /></label></div><div class="form-actions"><button class="secondary-button" type="button" @click="showPlanForm = false">Hủy</button><button class="primary-button" type="submit" :disabled="creatingPlan">{{ creatingPlan ? 'Đang tạo...' : 'Tạo kế hoạch' }}</button></div></form>
          <div v-if="plans.length" class="plan-list"><article v-for="plan in plans" :key="plan.planId" class="plan-row"><span class="plan-row-icon"><Compass :size="21" /></span><div class="plan-row-content"><div class="plan-row-title"><h2>{{ plan.title }}</h2><span :class="['plan-status', plan.status === 'ACTIVE' ? 'plan-active' : '']">{{ plan.status === 'ACTIVE' ? 'Đang học' : plan.status === 'COMPLETED' ? 'Hoàn thành' : 'Đã lưu trữ' }}</span></div><p>{{ certificates.find((certificate) => certificate.certificateId === plan.certificateId)?.name || 'Lộ trình học cá nhân' }} · {{ plan.hoursPerWeek }} giờ mỗi tuần · {{ formatDate(plan.examDate) }}</p><div class="plan-progress"><div><span :style="{ width: `${plan.taskCount ? Math.round(plan.completedCount / plan.taskCount * 100) : 0}%` }"></span></div><small>{{ plan.completedCount || 0 }} / {{ plan.taskCount || 0 }} nhiệm vụ</small></div><div class="roadmap-track" :aria-label="`Tiến độ ${plan.title}`"><div :class="{ reached: plan.completedCount > 0 }"><span>01</span><small>Bắt đầu</small></div><i :class="{ reached: plan.completedCount > 0 }"></i><div :class="{ reached: plan.completedCount > 0 && plan.completedCount < plan.taskCount, current: plan.status === 'ACTIVE' }"><span>02</span><small>Đang học</small></div><i :class="{ reached: plan.status === 'COMPLETED' }"></i><div :class="{ reached: plan.status === 'COMPLETED' }"><span>03</span><small>Hoàn thành</small></div></div></div><button class="icon-action" :title="`Xem thống kê ${plan.title}`" @click="showView('progress')"><ArrowRight :size="18" /></button></article></div>
          <div v-else-if="!loading && !showPlanForm" class="empty-page"><Compass :size="28" /><h2>Chưa có lộ trình</h2><p>Tạo kế hoạch theo ngày thi và số giờ học bạn có thể dành mỗi tuần.</p><button class="primary-button" @click="showPlanForm = true"><Plus :size="16" /> Tạo lộ trình đầu tiên</button></div>
        </section>

        <section v-else-if="activeView === 'courses'" class="page-view">
          <div class="view-heading"><div><p class="view-kicker">NỘI DUNG HỌC TẬP</p><h1>Khóa học và chứng chỉ</h1><p>Chọn lĩnh vực học tập để xem đề luyện tập tương ứng.</p></div><label class="search-field"><Search :size="17" /><input v-model="searchQuery" placeholder="Tìm khóa học" aria-label="Tìm khóa học"></label></div>
          <div v-if="certificates.length" class="certificate-grid"><article v-for="(certificate, index) in certificates.filter((item) => !searchQuery || `${item.name} ${item.description || ''}`.toLowerCase().includes(searchQuery.toLowerCase()))" :key="certificate.certificateId" class="certificate-card"><span class="certificate-icon" :class="`certificate-color-${index % 4}`"><GraduationCap :size="21" /></span><h2>{{ certificate.name }}</h2><p>{{ certificate.description || 'Chương trình học tập và đánh giá theo chứng chỉ.' }}</p><div class="certificate-stats"><span>{{ certificateProgress(certificate).total }} bài học</span><span v-if="certificate.passingScore">Mục tiêu {{ certificate.passingScore }}</span></div><div class="course-progress"><span>{{ certificateProgress(certificate).completed }}/{{ certificateProgress(certificate).total }} bài hoàn thành</span><strong>{{ certificateProgress(certificate).percent }}%</strong><div><i :style="{ width: `${certificateProgress(certificate).percent}%` }"></i></div></div><button class="outline-button" @click="showView('tests'); searchQuery = ''">{{ certificateProgress(certificate).completed ? 'Tiếp tục học' : 'Bắt đầu học' }} <ArrowRight :size="15" /></button></article></div>
          <div v-else class="empty-page"><GraduationCap :size="28" /><h2>Chưa có chứng chỉ học tập</h2><p>Các chứng chỉ được quản trị viên cấu hình sẽ hiển thị tại đây.</p></div>
        </section>

        <section v-else-if="activeView === 'tests'" class="page-view">
          <div class="view-heading"><div><p class="view-kicker">LUYỆN TẬP VÀ ĐÁNH GIÁ</p><h1>Bài kiểm tra</h1><p>Chọn đề thi để bắt đầu một lượt luyện tập mới.</p></div><label class="search-field"><Search :size="17" /><input v-model="searchQuery" placeholder="Tìm bài kiểm tra" aria-label="Tìm bài kiểm tra"></label></div>
          <div v-if="filteredExams.length" class="test-list"><article v-for="(exam, index) in filteredExams" :key="exam.examId" class="test-row"><span class="test-icon" :class="`test-icon-${index % 4}`"><BookOpen :size="20" /></span><div class="test-info"><h2>{{ exam.title }}</h2><p>{{ exam.description || 'Bài đánh giá kiến thức tổng hợp' }}</p><div class="test-meta"><span><Clock3 :size="14" /> {{ exam.durationMinutes }} phút</span><span>{{ exam.questionCount || exam.totalQuestions || 0 }} câu hỏi</span><span>{{ exam.totalPoints ?? '—' }} điểm</span></div></div><button class="primary-button" :disabled="loading" @click="startExam(exam)">Bắt đầu <ArrowRight :size="15" /></button></article></div>
          <div v-else-if="!loading" class="empty-page"><BookOpen :size="28" /><h2>Chưa có bài kiểm tra</h2><p>Đề thi đã xuất bản sẽ được hiển thị tại đây.</p></div>
          <div v-if="history.length" class="recent-history"><div class="section-heading"><div><h2>Lịch sử gần đây</h2><p>Các lượt làm bài của bạn</p></div><button class="inline-link" @click="showView('history')">Xem tất cả <ArrowRight :size="14" /></button></div><article v-for="item in history.slice(0, 3)" :key="item.attemptId" class="history-row"><span class="history-status"><CheckCircle2 :size="16" /></span><div><strong>{{ exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}</strong><small>{{ formatDate(item.startedAt) }} · {{ localizeStatus(item.status) }}</small></div><strong class="history-score">{{ item.score ?? '—' }}<small v-if="item.maxScore != null"> / {{ item.maxScore }}</small></strong></article></div>
        </section>

        <section v-else-if="activeView === 'progress' || activeView === 'history'" class="page-view">
          <div class="view-heading"><div><p class="view-kicker">PHÂN TÍCH HỌC TẬP</p><h1>{{ activeView === 'history' ? 'Lịch sử làm bài' : 'Thống kê tiến độ' }}</h1><p>Theo dõi kết quả theo chứng chỉ và các lượt đánh giá của bạn.</p></div><label v-if="certificates.length" class="select-field"><span>Chứng chỉ</span><select v-model="selectedCertificateId" @change="loadProgress"><option v-for="certificate in certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label></div>
          <div class="progress-summary"><div><span class="summary-icon"><Trophy :size="20" /></span><strong>{{ progress?.averageScore != null ? Number(progress.averageScore).toFixed(1) : averageScore != null ? averageScore.toFixed(1) : '—' }}</strong><small>Điểm trung bình</small></div><div><span class="summary-icon mint"><CheckCircle2 :size="20" /></span><strong>{{ progress?.totalAttempts ?? history.length }}</strong><small>Lượt làm bài</small></div><div><span class="summary-icon violet"><Compass :size="20" /></span><strong>{{ activePlans.length }}</strong><small>Lộ trình đang học</small></div></div>
          <div v-if="activeView === 'progress'" class="progress-visuals">
            <section class="detail-section trend-panel"><div class="section-heading"><div><h2>Tiến độ học tập</h2><p>Điểm số qua các lượt kiểm tra gần đây</p></div></div><div v-if="scoreTrend.length" class="score-chart"><div v-for="(entry, index) in scoreTrend" :key="`${entry.label}-${index}`" class="score-column"><strong>{{ entry.score }}</strong><div class="score-bar-track"><i :style="{ height: `${Math.max(8, Math.min(100, entry.score / entry.maxScore * 100))}%` }"></i></div><small>{{ entry.label }}</small></div></div><div v-else class="chart-empty">Hoàn thành bài kiểm tra để xem xu hướng điểm.</div></section>
            <section class="detail-section score-panel"><div class="section-heading"><div><h2>Tổng quan điểm số</h2><p>{{ selectedCertificate?.name || 'Kết quả học tập' }}</p></div></div><div class="score-ring" :style="{ '--score': `${averageScorePercent}%` }"><div><strong>{{ progress?.averageScore != null ? Number(progress.averageScore).toFixed(1) : averageScore != null ? averageScore.toFixed(1) : '—' }}</strong><small>điểm TB</small></div></div><div class="score-legend"><span><i></i>Lượt đã nộp <strong>{{ progress?.totalAttempts ?? history.length }}</strong></span><span><i></i>Đề đã xuất bản <strong>{{ publishedExams.length }}</strong></span></div></section>
          </div>
          <section v-if="activeView === 'progress'" class="detail-section"><div class="section-heading"><div><h2>Mức độ thành thạo</h2><p>Theo dõi kỹ năng từ các bài kiểm tra đã nộp</p></div></div><div v-if="masterySkills.length" class="mastery-list"><div v-for="(skill, index) in masterySkills" :key="skill.name" class="mastery-row"><div class="skill-label"><span class="skill-bullet" :class="`bullet-${index % 4}`"><Target :size="14" /></span><strong>{{ skill.name }}</strong><small>{{ Math.round(skill.value) }}%</small></div><div class="skill-bar"><span :class="`bar-${index % 4}`" :style="{ width: `${Math.max(0, Math.min(100, skill.value))}%` }"></span></div></div></div><div v-else class="empty-inline">Chưa có đủ dữ liệu để phân tích kỹ năng.</div></section>
          <section class="detail-section"><div class="section-heading"><div><h2>{{ activeView === 'history' ? 'Tất cả lượt làm bài' : 'Kết quả gần đây' }}</h2><p>Các bài thi và điểm số đã ghi nhận</p></div></div><div v-if="history.length" class="history-list"><article v-for="item in history" :key="item.attemptId" class="history-row"><span class="history-status"><CheckCircle2 :size="16" /></span><div><strong>{{ exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}</strong><small>{{ formatDate(item.startedAt) }} · {{ localizeStatus(item.status) }}</small></div><strong class="history-score">{{ item.score ?? '—' }}<small v-if="item.maxScore != null"> / {{ item.maxScore }}</small></strong></article></div><div v-else class="empty-inline">Chưa có lượt làm bài. <button class="text-link" @click="showView('tests')">Chọn bài kiểm tra <ArrowRight :size="14" /></button></div></section>
        </section>

        <section v-else-if="activeView === 'tutor'" class="page-view tutor-view">
          <div class="view-heading"><div><p class="view-kicker">HỖ TRỢ HỌC TẬP</p><h1>Trợ lý AI</h1><p>Trao đổi về bài học, kỹ năng và kết quả luyện tập của bạn.</p></div><button v-if="conversations.length" class="outline-button" @click="currentConversation = null; messages = []"><Plus :size="16" /> Cuộc trò chuyện mới</button></div>
          <div class="tutor-workspace"><aside class="conversation-list"><div class="conversation-list-heading"><strong>Cuộc trò chuyện</strong><button class="icon-action" aria-label="Tạo cuộc trò chuyện mới" title="Tạo cuộc trò chuyện mới" @click="currentConversation = null; messages = []"><Plus :size="17" /></button></div><button v-for="conversation in conversations" :key="conversation.id" :class="['conversation-item', { current: currentConversation?.id === conversation.id }]" @click="selectConversation(conversation)"><Brain :size="16" /><span>{{ conversation.title || 'Hỗ trợ học tập' }}</span></button><div v-if="!conversations.length" class="conversation-empty">Bắt đầu cuộc trò chuyện đầu tiên với gia sư AI.</div></aside><div class="chat-panel"><div v-if="messages.length" class="message-list"><article v-for="message in messages" :key="message.id" :class="['chat-message', message.role === 'USER' ? 'user-message' : 'assistant-message-row']"><span class="message-avatar"><UserRound v-if="message.role === 'USER'" :size="15" /><Brain v-else :size="16" /></span><div><small>{{ message.role === 'USER' ? 'Bạn' : 'Trợ lý AI' }}</small><p>{{ message.content }}</p></div></article></div><div v-else class="chat-empty"><span class="chat-empty-icon"><Sparkles :size="23" /></span><h2>Bạn muốn tìm hiểu điều gì?</h2><p>Hỏi gia sư AI về chủ đề đang học hoặc một câu hỏi bạn chưa rõ.</p><div class="prompt-suggestions"><button @click="tutorPrompt = 'Giúp mình lập kế hoạch ôn tập hiệu quả.'">Gợi ý kế hoạch ôn tập</button><button @click="tutorPrompt = 'Giải thích giúp mình một khái niệm đang học.'">Giải thích một khái niệm</button></div></div><form class="chat-composer" @submit.prevent="askTutor"><textarea v-model="tutorPrompt" rows="2" placeholder="Nhập câu hỏi của bạn..." aria-label="Câu hỏi cho trợ lý AI" @keydown.enter.exact.prevent="askTutor"></textarea><button class="send-button" type="submit" :disabled="askingTutor || !tutorPrompt.trim()" :aria-label="askingTutor ? 'Đang gửi' : 'Gửi câu hỏi'"><Send :size="18" /></button></form></div><aside class="tutor-features"><h2>Các tính năng</h2><button @click="tutorPrompt = 'Giải thích giúp mình bài học đang gặp khó khăn.'"><span class="feature-icon feature-blue"><BookOpen :size="17" /></span><span><strong>Giải thích bài học</strong><small>Hiểu sâu hơn về kiến thức</small></span><ArrowRight :size="15" /></button><button @click="tutorPrompt = 'Gợi ý lộ trình học phù hợp với mục tiêu của mình.'"><span class="feature-icon feature-green"><Compass :size="17" /></span><span><strong>Gợi ý lộ trình</strong><small>Cá nhân hóa theo tiến độ</small></span><ArrowRight :size="15" /></button><button @click="tutorPrompt = 'Phân tích giúp mình câu trả lời và chỉ ra điểm cần cải thiện.'"><span class="feature-icon feature-violet"><Target :size="17" /></span><span><strong>Hỗ trợ code</strong><small>Ví dụ và giải thích chi tiết</small></span><ArrowRight :size="15" /></button><button @click="tutorPrompt = 'Tạo cho mình một bài luyện tập ngắn về chủ đề đang học.'"><span class="feature-icon feature-orange"><Sparkles :size="17" /></span><span><strong>Tạo bài luyện</strong><small>Ôn tập theo nhu cầu</small></span><ArrowRight :size="15" /></button></aside></div>
        </section>

        <section v-else-if="activeView === 'settings'" class="page-view">
          <div class="view-heading"><div><p class="view-kicker">TÀI KHOẢN</p><h1>Cài đặt</h1><p>Thông tin đăng nhập và quyền truy cập của bạn.</p></div></div>
          <section class="account-panel"><div class="account-avatar">{{ initials }}</div><div class="account-details"><h2>{{ userName }}</h2><p>{{ userStore.state.role === 'ROLE_ADMIN' ? 'Quản trị viên' : 'Học viên' }}</p></div><button class="secondary-button" @click="logout"><LogOut :size="16" /> Đăng xuất</button></section>
        </section>

        <section v-else-if="activeView === 'taking'" class="page-view test-taking-view">
          <div class="view-heading"><div><p class="view-kicker">BÀI KIỂM TRA ĐANG DIỄN RA</p><h1>{{ exams.find((exam) => exam.examId === attempt?.examId)?.title || 'Bài kiểm tra' }}</h1><p>Đáp án được tự động lưu trong quá trình làm bài.</p></div><div class="timer" :class="{ urgent: secondsLeft < 300 }"><Clock3 :size="17" />{{ formattedTimeLeft }}</div></div><p class="save-state" aria-live="polite">{{ saveState }}</p>
          <article v-for="(question, index) in attemptQuestions" :key="question.questionId" class="question-panel"><div class="question-number">{{ index + 1 }}</div><div class="question-content"><div class="question-meta"><span>Câu {{ index + 1 }}</span><span v-if="question.sectionName">{{ question.sectionName }}</span><span>{{ question.points }} điểm</span></div><p>{{ question.content }}</p><div v-if="parseOptions(question).length" class="answer-options"><label v-for="option in parseOptions(question)" :key="option.value" class="answer-option"><input v-model="answers[question.questionId]" type="radio" :name="`question-${question.questionId}`" :value="option.value" @change="saveAnswer(question)"><span>{{ option.label }}</span></label></div><textarea v-else v-model="answers[question.questionId]" class="answer-text" rows="3" placeholder="Nhập câu trả lời" @input="saveAnswer(question)"></textarea></div></article>
          <div class="submit-bar"><button class="secondary-button" @click="leaveAttempt">Rời bài thi</button><button class="primary-button" :disabled="submitting || !attemptQuestions.length" @click="submitExam"><Send :size="16" />{{ submitting ? 'Đang nộp...' : 'Nộp bài' }}</button></div>
        </section>

        <section v-else-if="activeView === 'result' && result" class="result-panel"><span class="result-icon"><CheckCircle2 :size="28" /></span><p class="view-kicker">ĐÃ HOÀN THÀNH</p><h1>Kết quả bài thi</h1><strong class="result-score">{{ result.score ?? 0 }}<small> / {{ result.maxScore ?? 0 }}</small></strong><div class="result-counts"><div><strong>{{ result.correctCount }}</strong><small>Đúng</small></div><div><strong>{{ result.wrongCount }}</strong><small>Sai</small></div><div><strong>{{ result.unansweredCount }}</strong><small>Bỏ trống</small></div></div><button class="primary-button" @click="showView('dashboard')">Về trang chủ <ArrowRight :size="16" /></button></section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.app-shell { --ink: #101d49; --muted: #6e82a8; --line: #e6edf8; --blue: #2d68f3; --green: #23ad8c; display: flex; min-height: 100vh; color: var(--ink); background: #f5f8fd; font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
.sidebar { position: fixed; z-index: 10; inset: 0 auto 0 0; display: flex; width: 252px; flex-direction: column; padding: 20px 22px 16px; border-right: 1px solid #e7edf7; background: rgba(255,255,255,.94); }
.sidebar-brand { display: flex; min-height: 43px; align-items: center; gap: 8px; color: var(--ink); text-decoration: none; font-size: 16px; font-weight: 730; white-space: nowrap; }.brand-symbol { display: grid; width: 40px; height: 40px; place-items: center; color: var(--blue); }
.side-nav { display: grid; gap: 5px; margin-top: 25px; }.side-link { display: flex; min-height: 47px; align-items: center; gap: 14px; padding: 0 13px; border: 0; border-radius: 8px; background: transparent; color: #596e97; font: inherit; font-size: 13px; text-align: left; cursor: pointer; transition: background .15s, color .15s; }.side-link:hover { background: #f3f6fc; color: var(--blue); }.side-link.selected { background: #eaf1ff; color: #2868f4; font-weight: 600; }.side-link svg { flex: 0 0 auto; }
.sidebar-quote { position: relative; min-height: 220px; margin-top: auto; padding: 18px 15px 0; overflow: hidden; border-radius: 10px; background: linear-gradient(155deg, #f0f5ff 2%, #e8f0ff 65%, #dce8ff); }.quote-icon { display: inline-grid; width: 28px; height: 28px; margin-right: 8px; vertical-align: middle; place-items: center; color: var(--blue); }.sidebar-quote strong { display: inline-block; vertical-align: middle; font-size: 12px; line-height: 1.5; }.sidebar-quote p { position: relative; z-index: 2; margin: 13px 0; color: #7589af; font-size: 11px; line-height: 1.55; }.quote-mountain { position: absolute; right: -8px; bottom: -28px; left: -7px; height: 112px; overflow: hidden; background: linear-gradient(145deg, rgba(196,216,255,.25), rgba(120,159,242,.3)); clip-path: polygon(0 80%, 23% 35%, 39% 66%, 61% 12%, 100% 75%, 100% 100%, 0 100%); }.quote-mountain span { position: absolute; bottom: 5px; left: 0; width: 100%; height: 47px; background: rgba(149,180,243,.32); clip-path: polygon(0 80%, 34% 20%, 50% 58%, 70% 4%, 100% 82%, 100% 100%, 0 100%); }.quote-mountain i { position: absolute; right: 33%; bottom: 55px; width: 2px; height: 27px; background: #5484ef; }.quote-mountain b { position: absolute; right: calc(33% - 8px); bottom: 74px; color: #5884ee; font-size: 17px; }.sidebar-bottom { display: grid; grid-template-columns: 8px 1fr; align-items: center; gap: 3px 8px; margin-top: 18px; padding: 8px 4px 0; color: #617599; font-size: 11px; }.connection-dot { width: 7px; height: 7px; border-radius: 50%; background: #27b58d; }.version { grid-column: 2; color: #91a0ba; font-size: 10px; }
.workspace { width: calc(100% - 252px); min-width: 0; min-height: 100vh; margin-left: 252px; }.topbar { position: sticky; z-index: 5; top: 0; display: flex; height: 76px; align-items: center; gap: 22px; padding: 0 clamp(20px, 2.1vw, 34px); border-bottom: 1px solid #e9eef6; background: rgba(249,251,255,.92); backdrop-filter: blur(12px); }.global-search { display: flex; width: min(540px, 56%); height: 40px; align-items: center; gap: 9px; padding: 0 13px; border: 1px solid #e1e8f3; border-radius: 10px; background: #fff; color: #8093b3; }.global-search input,.search-field input { width: 100%; min-width: 0; border: 0; outline: 0; background: transparent; color: #273a61; font: inherit; font-size: 12px; }.global-search input::placeholder,.search-field input::placeholder { color: #8b9db9; }.notification-button { position: relative; display: grid; width: 36px; height: 36px; margin-left: auto; place-items: center; border: 0; background: transparent; color: #647b9f; cursor: pointer; }.notification-dot { position: absolute; top: 5px; right: 5px; width: 7px; height: 7px; border: 1px solid white; border-radius: 50%; background: #f35a69; }.profile-button { display: flex; align-items: center; gap: 10px; padding: 0; border: 0; background: transparent; color: var(--ink); text-align: left; cursor: pointer; }.avatar,.account-avatar { display: grid; width: 37px; height: 37px; flex: 0 0 auto; place-items: center; border: 2px solid #fff; border-radius: 50%; background: linear-gradient(145deg, #d9e7ff, #a8c4fb); color: #2757a6; font-size: 14px; font-weight: 700; box-shadow: 0 2px 8px rgba(50,85,150,.12); }.profile-copy { display: grid; gap: 2px; }.profile-copy strong { max-width: 190px; overflow: hidden; font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.profile-copy small { color: #8292ad; font-size: 10px; }.profile-button > svg { color: #8a9bb5; }.mobile-menu-button { display: none; }
.content-area { width: min(100%, 1500px); margin: 0 auto; padding: 20px clamp(18px, 1.6vw, 27px) 42px; }.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1.9fr) minmax(290px, .86fr); align-items: start; gap: 18px; }.main-column,.right-column { display: grid; min-width: 0; gap: 17px; }.welcome-banner { position: relative; display: flex; min-height: 248px; align-items: center; padding: 28px; overflow: hidden; border: 1px solid #e0eafd; border-radius: 10px; background: linear-gradient(112deg, #edf4ff 0%, #e9f1ff 66%, #dfeaff 100%); }.welcome-banner::after { position: absolute; right: 3%; bottom: -52%; width: 43%; aspect-ratio: 1; border: 1px solid rgba(136,171,239,.15); border-radius: 50%; content: ''; }.welcome-copy { position: relative; z-index: 2; width: min(100%, 625px); }.welcome-kicker,.view-kicker { margin: 0 0 9px; color: #3f6cc1; font-size: 10px; font-weight: 730; letter-spacing: .8px; }.welcome-copy h1 { margin: 0; color: #101d49; font-size: clamp(23px, 2.05vw, 29px); line-height: 1.25; font-weight: 730; letter-spacing: 0; }.welcome-copy > p:not(.welcome-kicker) { margin: 12px 0 0; color: #61769c; font-size: 13px; line-height: 1.7; }.quick-stats { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 22px; }.quick-stat { display: flex; min-width: 132px; align-items: center; gap: 9px; padding: 8px 10px; border: 1px solid rgba(255,255,255,.8); border-radius: 8px; background: rgba(255,255,255,.78); }.quick-icon { display: grid; width: 31px; height: 31px; flex: 0 0 auto; place-items: center; border-radius: 50%; }.quick-icon.lavender { color: #685cf0; background: #ebe8ff; }.quick-icon.sky { color: #2e78ef; background: #e2edff; }.quick-icon.peach { color: #f58246; background: #fff0e7; }.quick-stat > span:last-child { display: grid; gap: 1px; }.quick-stat strong { font-size: 17px; line-height: 1.1; }.quick-stat small { color: #7b8dad; font-size: 9px; white-space: nowrap; }
.welcome-art { position: absolute; top: 0; right: 0; width: 35%; height: 100%; overflow: hidden; }.art-orbit { position: absolute; right: -2%; bottom: -13%; width: 91%; aspect-ratio: 1; border: 1px solid rgba(87,141,240,.15); border-radius: 50%; }.orbit-b { right: 4%; bottom: -4%; width: 72%; border-style: dashed; border-color: rgba(87,141,240,.24); }.art-node { position: absolute; z-index: 2; display: grid; place-items: center; border: 4px solid rgba(255,255,255,.72); border-radius: 50%; color: white; box-shadow: 0 8px 20px rgba(71,113,196,.17); }.node-book { top: 35%; left: 12%; width: 37px; height: 37px; background: #4484fa; }.node-ai { top: 14%; left: 43%; width: 48px; height: 48px; background: #3171ed; }.node-bars { top: 20%; right: 12%; width: 40px; height: 40px; background: #94b6f7; }.node-check { top: 60%; left: 38%; width: 27px; height: 27px; border: 3px solid rgba(255,255,255,.8); background: #2ebd9a; }.art-track { position: absolute; border-top: 2px dashed #a7c5fa; transform: rotate(-18deg); }.track-one { top: 34%; left: 28%; width: 91px; }.track-two { top: 56%; left: 24%; width: 86px; transform: rotate(22deg); }.art-spark { position: absolute; color: rgba(69,125,237,.65); }.spark-one { top: 29%; right: 36%; font-size: 20px; }.spark-two { right: 10%; bottom: 22%; font-size: 25px; }
.section-block,.assistant-panel,.weekly-panel,.goal-panel,.plan-form,.plan-list,.certificate-card,.test-list,.detail-section,.account-panel,.tutor-workspace { border: 1px solid #e7edf6; border-radius: 9px; background: #fff; }.section-block { padding: 17px 17px 16px; }.section-heading,.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.section-heading { margin-bottom: 13px; }.section-heading h2,.panel-heading h2 { margin: 0; font-size: 15px; line-height: 1.3; font-weight: 700; }.section-heading p { margin: 4px 0 0; color: #8393ad; font-size: 11px; }.inline-link,.text-link { display: inline-flex; align-items: center; gap: 5px; flex: 0 0 auto; padding: 4px 0; border: 0; background: transparent; color: #2868f4; font: inherit; font-size: 11px; cursor: pointer; }.inline-link:hover,.text-link:hover { color: #164fc5; }
.continue-card { display: flex; align-items: center; gap: 16px; padding: 8px; border: 1px solid #e6edf8; border-radius: 8px; }.course-art { position: relative; display: flex; width: 195px; height: 124px; flex: 0 0 auto; align-items: center; justify-content: center; flex-direction: column; gap: 3px; overflow: hidden; border-radius: 8px; background: linear-gradient(135deg, #316edf, #5f7ded 54%, #7d91ed); color: white; }.course-art::before,.course-art::after { position: absolute; width: 120px; height: 120px; border: 1px solid rgba(255,255,255,.14); border-radius: 50%; content: ''; }.course-art::before { top: -57px; left: -24px; }.course-art::after { right: -38px; bottom: -73px; width: 154px; height: 154px; }.course-art > svg { z-index: 1; width: 38px; height: 38px; }.course-art small { z-index: 1; max-width: 85%; overflow: hidden; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.course-label { position: absolute; top: 10px; left: 11px; color: rgba(255,255,255,.76); font-size: 8px; font-weight: 700; letter-spacing: .7px; }.course-art-ring { position: absolute; right: 15px; top: 28px; width: 12px; height: 12px; border: 2px solid rgba(255,255,255,.45); border-radius: 50%; }.continue-body { min-width: 0; flex: 1; }.continue-title { display: flex; align-items: center; gap: 10px; }.continue-title h3 { overflow: hidden; margin: 0; font-size: 13px; font-weight: 680; text-overflow: ellipsis; white-space: nowrap; }.status-label { display: inline-flex; align-items: center; gap: 5px; color: #20a37d; font-size: 9px; white-space: nowrap; }.status-label span { width: 6px; height: 6px; border-radius: 50%; background: #27ba8d; }.course-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 9px 0 12px; color: #6880a8; font-size: 10px; }.course-tags span { padding: 3px 7px; border-radius: 4px; background: #eef4ff; color: #4671c3; }.plan-progress { display: flex; align-items: center; gap: 9px; }.plan-progress > div { height: 6px; flex: 1; overflow: hidden; border-radius: 5px; background: #eaf0fa; }.plan-progress > div span { display: block; height: 100%; border-radius: 5px; background: linear-gradient(90deg,#3477f6,#398de9); }.plan-progress small { color: #5a6f94; font-size: 9px; }.continue-meta { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 11px; color: #7e8eaa; font-size: 9px; }.continue-meta > span { display: inline-flex; align-items: center; gap: 4px; }.continue-meta svg { width: 13px; height: 13px; }.continue-button { display: inline-flex; min-height: 35px; align-items: center; gap: 6px; padding: 0 11px; border: 0; border-radius: 6px; background: #2d68f3; color: white; font: inherit; font-size: 10px; font-weight: 600; white-space: nowrap; cursor: pointer; }.continue-button:hover,.primary-button:hover,.assistant-button:hover { background: #2059df; }
.skill-overview { display: grid; grid-template-columns: minmax(145px, .62fr) minmax(0, 1.65fr); align-items: stretch; gap: 15px; padding: 12px; border: 1px solid #e9eff8; border-radius: 8px; }.mastery-summary { display: flex; align-items: center; flex-direction: column; justify-content: center; gap: 4px; padding: 8px 5px; border: 1px solid #e8edf6; border-radius: 7px; }.mastery-ring { --mastery: 0%; display: grid; width: 77px; height: 77px; margin-bottom: 4px; place-items: center; border-radius: 50%; background: conic-gradient(#35b795 var(--mastery), #dce8f8 0); }.mastery-ring::before { position: absolute; width: 60px; height: 60px; border-radius: 50%; background: #fff; content: ''; }.mastery-ring span { position: relative; z-index: 1; color: #2868e8; font-size: 13px; font-weight: 680; }.mastery-summary > strong { font-size: 10px; }.mastery-summary > small { color: #8292ad; font-size: 9px; }.skill-list,.mastery-list { display: grid; align-content: center; gap: 12px; }.skill-row { display: grid; gap: 5px; }.skill-label { display: flex; align-items: center; gap: 7px; }.skill-label strong { min-width: 0; flex: 1; overflow-wrap: anywhere; font-size: 10px; font-weight: 600; }.skill-label small { color: #657a9e; font-size: 9px; }.skill-bullet { display: grid; width: 24px; height: 24px; flex: 0 0 auto; place-items: center; border-radius: 7px; }.bullet-0 { color: #3779ed; background: #e7efff; }.bullet-1 { color: #745be9; background: #eeeaff; }.bullet-2 { color: #ec8053; background: #fff0e9; }.bullet-3 { color: #20a982; background: #e3f6ef; }.skill-bar,.goal-track { height: 5px; overflow: hidden; border-radius: 5px; background: #edf1f8; }.skill-bar { margin-left: 31px; }.skill-bar > span,.goal-track > span { display: block; height: 100%; border-radius: inherit; }.bar-0 { background: #2fae8b; }.bar-1 { background: #3675ef; }.bar-2 { background: #f39a52; }.bar-3 { background: #805ee8; }.skills-empty { display: flex; align-items: flex-start; flex-direction: column; justify-content: center; gap: 5px; padding: 8px 10px; color: #6f83a5; }.skills-empty > svg { color: #765ee8; }.skills-empty strong { color: var(--ink); font-size: 11px; }.skills-empty > span { font-size: 10px; line-height: 1.5; }
.recommendation-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 9px; }.recommendation-card { display: flex; min-width: 0; align-items: flex-start; gap: 9px; padding: 11px; border: 1px solid #e8edf6; border-radius: 8px; }.recommendation-icon,.small-panel-icon { display: grid; width: 31px; height: 31px; flex: 0 0 auto; place-items: center; border-radius: 8px; color: #7659e6; background: #eeeaff; }.recommend-icon-1 { color: #3378ed; background: #e8f0ff; }.recommend-icon-2 { color: #21a982; background: #e2f6ed; }.recommendation-copy { min-width: 0; flex: 1; }.recommendation-copy strong { display: block; overflow: hidden; font-size: 10px; line-height: 1.4; text-overflow: ellipsis; }.recommendation-copy p { display: -webkit-box; min-height: 28px; margin: 5px 0; overflow: hidden; color: #798aa7; font-size: 9px; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.recommendation-level { color: #647eaa; font-size: 8px; }.recommendation-action { display: grid; width: 25px; height: 25px; flex: 0 0 auto; place-items: center; border: 1px solid #e3eaf6; border-radius: 6px; background: #fff; color: #3775ee; cursor: pointer; }.recommendation-action:disabled { color: #25a581; cursor: default; }.empty-recommendation { display: flex; align-items: center; gap: 10px; padding: 17px; border: 1px solid #e8edf6; border-radius: 8px; color: #798aa7; font-size: 11px; }.empty-recommendation > svg { flex: 0 0 auto; color: #7659e6; }
.right-column { gap: 17px; }.assistant-panel,.weekly-panel,.goal-panel { padding: 17px; }.panel-heading { justify-content: flex-start; gap: 9px; }.panel-heading h2 { flex: 0 1 auto; }.assistant-mark { display: grid; width: 33px; height: 33px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #7d56ed; background: #eee7ff; }.personalized { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; padding: 5px 7px; border-radius: 12px; background: #eef4ff; color: #3370e2; font-size: 8px; white-space: nowrap; }.assistant-description { margin: 11px 0 13px; color: #7b8eae; font-size: 11px; line-height: 1.65; }.assistant-message { display: flex; align-items: flex-start; gap: 9px; }.bot-avatar { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border: 2px solid #d7e5ff; border-radius: 50%; background: #f4f8ff; color: #3977eb; }.assistant-message p { margin: 0; padding: 11px; border: 1px solid #e7edf7; border-radius: 8px; color: #4d638a; background: #fff; font-size: 10px; line-height: 1.6; }.assistant-button { display: flex; width: 100%; min-height: 40px; align-items: center; justify-content: center; gap: 8px; margin-top: 14px; border: 0; border-radius: 7px; background: #2d68f3; color: #fff; font: inherit; font-size: 11px; font-weight: 630; cursor: pointer; }.assistant-button svg:last-child { margin-left: 2px; }.small-panel-icon { width: 28px; height: 28px; border-radius: 50%; }.small-panel-icon.purple { color: #785de8; background: #f0ebff; }.small-panel-icon.blue { color: #3474ed; background: #eaf1ff; }.weekly-panel .panel-heading,.goal-panel .panel-heading { margin-bottom: 13px; }.weekly-panel .inline-link,.goal-panel .inline-link { margin-left: auto; }.certificate-select-label { display: grid; grid-template-columns: 70px 1fr; align-items: center; gap: 8px; margin-bottom: 10px; color: #8292ad; font-size: 10px; }.certificate-select-label select,.select-field select { min-width: 0; padding: 7px 25px 7px 9px; border: 1px solid #e3eaf5; border-radius: 6px; background: #fff; color: #425a84; font: inherit; font-size: 10px; }.metric-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 9px; }.metric-cell { display: grid; min-height: 94px; align-content: start; justify-items: start; padding: 10px; border: 1px solid #e7edf7; border-radius: 7px; }.metric-icon { display: grid; width: 28px; height: 28px; margin-bottom: 7px; place-items: center; border-radius: 50%; color: #4179d8; background: #eaf1ff; }.metric-icon.mint { color: #26a982; background: #e3f6ee; }.metric-icon.violet { color: #7960dd; background: #eeeaff; }.metric-icon.amber { color: #f48649; background: #fff0e7; }.metric-cell strong { font-size: 16px; line-height: 1.25; }.metric-cell small { margin-top: 2px; color: #8292ad; font-size: 9px; }
.goal-content { padding: 12px; border-radius: 8px; background: #f8faff; }.goal-heading { display: flex; align-items: center; gap: 8px; }.goal-symbol { display: grid; width: 28px; height: 28px; flex: 0 0 auto; place-items: center; border-radius: 7px; color: #3373ee; background: #e6efff; }.goal-heading strong { overflow-wrap: anywhere; font-size: 10px; }.goal-track { height: 6px; margin: 12px 0 7px; background: #e5ebf6; }.goal-track span { background: #3474f0; }.goal-details { display: flex; justify-content: space-between; gap: 8px; color: #8091ac; font-size: 9px; }.goal-empty { color: #8091ac; font-size: 10px; }.goal-empty p { margin: 0 0 7px; }
.page-view { display: grid; gap: 17px; }.view-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 5px 2px 3px; }.view-heading h1 { margin: 0; color: var(--ink); font-size: 27px; letter-spacing: 0; }.view-heading p:not(.view-kicker) { margin: 8px 0 0; color: #7d8eaa; font-size: 12px; }.view-heading .view-kicker { margin-bottom: 7px; }.primary-button,.secondary-button,.outline-button { display: inline-flex; min-height: 38px; align-items: center; justify-content: center; gap: 7px; padding: 0 13px; border: 0; border-radius: 6px; background: var(--blue); color: white; font: inherit; font-size: 11px; font-weight: 620; white-space: nowrap; cursor: pointer; }.primary-button:hover { background: #2059df; }.primary-button:disabled { cursor: wait; opacity: .6; }.secondary-button { border: 1px solid #e1e8f3; background: white; color: #52698f; }.secondary-button:hover,.outline-button:hover { background: #f4f7fd; }.outline-button { min-height: 34px; border: 1px solid #dfe7f3; background: #fff; color: #416bb8; }.search-field { display: flex; width: min(100%, 290px); height: 38px; align-items: center; gap: 8px; padding: 0 10px; border: 1px solid #dfe7f3; border-radius: 7px; background: #fff; color: #8495b1; }.search-field input { font-size: 11px; }.plan-form { display: grid; max-width: 690px; gap: 15px; padding: 20px; }.form-heading { display: flex; justify-content: space-between; align-items: flex-start; }.form-heading h2 { margin: 0; font-size: 16px; }.form-heading p { margin: 5px 0 0; color: #7c8da8; font-size: 11px; }.icon-action { display: grid; width: 33px; height: 33px; flex: 0 0 auto; place-items: center; border: 1px solid #e4ebf5; border-radius: 6px; background: white; color: #7083a3; cursor: pointer; }.form-field { display: grid; gap: 7px; color: #53698d; font-size: 11px; }.form-field input,.form-field select { width: 100%; min-height: 38px; box-sizing: border-box; padding: 0 10px; border: 1px solid #dfe7f3; border-radius: 6px; outline-color: #7ca3f4; background: white; color: #273a61; font: inherit; font-size: 12px; }.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }.form-actions { display: flex; justify-content: flex-end; gap: 9px; padding-top: 4px; }.plan-list { padding: 0 16px; }.plan-row { display: flex; align-items: center; gap: 13px; padding: 15px 0; }.plan-row + .plan-row { border-top: 1px solid #e9eef6; }.plan-row-icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 9px; color: #506be8; background: #eeefff; }.plan-row-content { min-width: 0; flex: 1; }.plan-row-title { display: flex; align-items: center; gap: 10px; }.plan-row-title h2 { overflow: hidden; margin: 0; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.plan-row-content > p { margin: 5px 0 9px; color: #7e8eaa; font-size: 10px; }.plan-row-content .plan-progress { max-width: 410px; }.plan-row-content .plan-progress small { min-width: 100px; }.plan-status { padding: 4px 7px; border-radius: 12px; background: #f0f3f8; color: #72829c; font-size: 9px; white-space: nowrap; }.plan-active { background: #e6f6ef; color: #1c9b74; }
.certificate-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 13px; }.certificate-card { display: flex; min-height: 220px; align-items: flex-start; flex-direction: column; padding: 17px; }.certificate-icon { display: grid; width: 39px; height: 39px; place-items: center; border-radius: 9px; color: #3578ef; background: #e9f0ff; }.certificate-color-1 { color: #7a5de7; background: #f0ebff; }.certificate-color-2 { color: #20a781; background: #e5f6ee; }.certificate-color-3 { color: #e98745; background: #fff1e6; }.certificate-card h2 { margin: 12px 0 5px; font-size: 14px; }.certificate-card p { min-height: 36px; margin: 0; color: #7e8eaa; font-size: 10px; line-height: 1.6; }.certificate-stats { display: flex; flex-wrap: wrap; gap: 8px; margin: 11px 0 13px; color: #617ba5; font-size: 9px; }.certificate-stats span { padding: 4px 7px; border-radius: 4px; background: #f1f5fc; }.certificate-card .outline-button { margin-top: auto; }
.test-list { padding: 0 16px; }.test-row { display: flex; align-items: center; gap: 13px; padding: 15px 0; }.test-row + .test-row { border-top: 1px solid #e9eef6; }.test-icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 9px; color: #3679ed; background: #eaf1ff; }.test-icon-1 { color: #7e61e8; background: #f0ebff; }.test-icon-2 { color: #1ea781; background: #e4f7ee; }.test-icon-3 { color: #e78646; background: #fff0e6; }.test-info { min-width: 0; flex: 1; }.test-info h2 { margin: 0; font-size: 13px; }.test-info p { margin: 4px 0 8px; color: #8291aa; font-size: 10px; }.test-meta { display: flex; flex-wrap: wrap; gap: 12px; color: #788ba9; font-size: 9px; }.test-meta span { display: inline-flex; align-items: center; gap: 4px; }.test-row .primary-button { min-height: 34px; }.recent-history,.detail-section { padding: 17px; }.history-row { display: flex; align-items: center; gap: 11px; padding: 12px 0; }.history-row + .history-row { border-top: 1px solid #edf0f6; }.history-status { display: grid; width: 31px; height: 31px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #25a882; background: #e5f6ef; }.history-row > div { display: grid; min-width: 0; flex: 1; gap: 4px; }.history-row > div > strong { overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.history-row small { color: #8392ac; font-size: 9px; }.history-score { font-size: 13px; }.history-score small { color: #8392ac; font-weight: 400; }.select-field { display: grid; grid-template-columns: auto 170px; align-items: center; gap: 9px; color: #7184a5; font-size: 10px; }.select-field select { min-height: 37px; }.progress-summary { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; }.progress-summary > div { display: grid; align-content: start; justify-items: start; padding: 15px; border: 1px solid #e6edf6; border-radius: 8px; background: #fff; }.summary-icon { display: grid; width: 34px; height: 34px; margin-bottom: 9px; place-items: center; border-radius: 50%; color: #3275ed; background: #eaf1ff; }.summary-icon.mint { color: #23a882; background: #e5f6ef; }.summary-icon.violet { color: #785de3; background: #efebff; }.progress-summary strong { font-size: 20px; }.progress-summary small { margin-top: 3px; color: #8493ac; font-size: 10px; }.detail-section > .section-heading { margin-bottom: 14px; }.mastery-list { max-width: 690px; gap: 15px; }.mastery-row { display: grid; gap: 6px; }.mastery-row .skill-label strong { font-size: 11px; }.mastery-row .skill-label small { font-size: 10px; }.mastery-row .skill-bar { height: 7px; margin-left: 31px; }
.empty-inline,.empty-page { display: flex; align-items: center; gap: 13px; padding: 16px; border: 1px solid #e8edf6; border-radius: 8px; color: #7c8eaa; background: #fff; }.empty-symbol { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border-radius: 8px; color: #7360e7; background: #f0edff; }.empty-inline > div { min-width: 0; flex: 1; }.empty-inline strong { color: var(--ink); font-size: 11px; }.empty-inline p { margin: 3px 0 0; font-size: 10px; }.empty-page { min-height: 220px; align-items: center; flex-direction: column; justify-content: center; text-align: center; }.empty-page > svg { color: #7086c7; }.empty-page h2 { margin: 0; font-size: 15px; }.empty-page p { max-width: 400px; margin: 0; color: #8090aa; font-size: 11px; line-height: 1.6; }.empty-page .primary-button { margin-top: 6px; }
.tutor-workspace { display: grid; min-height: 490px; grid-template-columns: 235px minmax(0,1fr); overflow: hidden; }.conversation-list { padding: 14px 10px; border-right: 1px solid #e8edf6; }.conversation-list-heading { display: flex; align-items: center; justify-content: space-between; padding: 2px 5px 12px; }.conversation-list-heading strong { font-size: 11px; }.conversation-item { display: flex; width: 100%; align-items: center; gap: 8px; padding: 9px 8px; border: 0; border-radius: 6px; background: transparent; color: #6d80a0; font: inherit; font-size: 10px; text-align: left; cursor: pointer; }.conversation-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.conversation-item.current,.conversation-item:hover { background: #eef4ff; color: #2f6ee7; }.conversation-empty { padding: 10px 7px; color: #8998b1; font-size: 10px; line-height: 1.6; }.chat-panel { display: flex; min-width: 0; flex-direction: column; padding: 18px; }.message-list { display: grid; flex: 1; align-content: start; gap: 14px; overflow: auto; padding: 2px 4px 14px; }.chat-message { display: flex; max-width: 86%; align-items: flex-start; gap: 8px; }.user-message { justify-self: end; flex-direction: row-reverse; }.message-avatar { display: grid; width: 27px; height: 27px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: #edf3ff; color: #3373eb; }.chat-message > div { padding: 9px 11px; border-radius: 8px; background: #f4f7fc; }.user-message > div { background: #edf3ff; }.chat-message small { color: #7d8da7; font-size: 9px; }.chat-message p { margin: 4px 0 0; color: #3e5277; font-size: 11px; line-height: 1.65; white-space: pre-wrap; }.chat-empty { display: flex; flex: 1; align-items: center; flex-direction: column; justify-content: center; padding: 25px; text-align: center; }.chat-empty-icon { display: grid; width: 45px; height: 45px; place-items: center; border-radius: 50%; background: #f0ebff; color: #785de6; }.chat-empty h2 { margin: 13px 0 5px; font-size: 16px; }.chat-empty p { max-width: 360px; margin: 0; color: #8191ad; font-size: 11px; line-height: 1.6; }.prompt-suggestions { display: flex; flex-wrap: wrap; justify-content: center; gap: 7px; margin-top: 15px; }.prompt-suggestions button { padding: 8px 10px; border: 1px solid #e3eaf6; border-radius: 6px; background: #fff; color: #54709f; font: inherit; font-size: 9px; cursor: pointer; }.chat-composer { display: flex; align-items: flex-end; gap: 8px; padding: 9px; border: 1px solid #e1e8f3; border-radius: 8px; }.chat-composer textarea { width: 100%; min-width: 0; resize: vertical; border: 0; outline: 0; color: #30476e; font: inherit; font-size: 11px; }.chat-composer textarea::placeholder { color: #9aa8bd; }.send-button { display: grid; width: 33px; height: 33px; flex: 0 0 auto; place-items: center; border: 0; border-radius: 6px; background: #2d68f3; color: white; cursor: pointer; }.send-button:disabled { background: #b8c9e7; cursor: not-allowed; }
.account-panel { display: flex; align-items: center; gap: 15px; padding: 18px; }.account-avatar { width: 52px; height: 52px; font-size: 18px; }.account-details { flex: 1; }.account-details h2 { margin: 0; font-size: 14px; }.account-details p { margin: 4px 0 0; color: #8191ad; font-size: 10px; }.account-panel .secondary-button { color: #a64d50; }
.timer { display: inline-flex; align-items: center; gap: 7px; padding: 8px 11px; border: 1px solid #dfe7f3; border-radius: 6px; color: #3b5077; background: #fff; font-size: 13px; font-weight: 650; font-variant-numeric: tabular-nums; }.timer.urgent { border-color: #f0ceca; color: #b44843; background: #fff5f4; }.save-state { margin: -10px 0 0; color: #8090aa; font-size: 10px; text-align: right; }.question-panel { display: flex; gap: 12px; padding: 17px; border: 1px solid #e6edf6; border-radius: 8px; background: #fff; }.question-number { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border: 1px solid #dfe7f3; border-radius: 50%; color: #4267a6; font-size: 11px; font-weight: 650; }.question-content { min-width: 0; flex: 1; }.question-meta { display: flex; flex-wrap: wrap; gap: 11px; color: #8291aa; font-size: 9px; }.question-content > p { margin: 8px 0 12px; color: #253a62; font-size: 12px; line-height: 1.65; white-space: pre-wrap; }.answer-options { display: grid; gap: 7px; }.answer-option { display: flex; align-items: flex-start; gap: 8px; padding: 9px 11px; border: 1px solid #e2e9f4; border-radius: 6px; color: #52678c; font-size: 10px; cursor: pointer; }.answer-option:has(input:checked) { border-color: #4381ed; background: #f1f6ff; }.answer-option input { accent-color: #2d68f3; }.answer-text { width: 100%; box-sizing: border-box; padding: 9px 11px; border: 1px solid #dfe7f3; border-radius: 6px; font: inherit; font-size: 11px; resize: vertical; }.submit-bar { display: flex; justify-content: space-between; padding-top: 4px; }.result-panel { display: flex; min-height: 400px; align-items: center; flex-direction: column; justify-content: center; padding: 30px; border: 1px solid #e6edf6; border-radius: 9px; background: white; text-align: center; }.result-icon { display: grid; width: 53px; height: 53px; margin-bottom: 14px; place-items: center; border-radius: 50%; background: #e5f6ef; color: #23a982; }.result-panel h1 { font-size: 24px; }.result-score { margin: 19px 0; color: #2868e8; font-size: 40px; }.result-score small { color: #95a2b8; font-size: 19px; font-weight: 400; }.result-counts { display: flex; width: min(100%, 390px); justify-content: space-around; margin-bottom: 22px; padding: 13px 0; border-top: 1px solid #e8edf6; border-bottom: 1px solid #e8edf6; }.result-counts div { display: grid; gap: 4px; }.result-counts strong { font-size: 17px; }.result-counts small { color: #8594ad; font-size: 9px; }
.notice { display: flex; align-items: center; gap: 9px; margin-bottom: 15px; padding: 11px 13px; border: 1px solid #f0d6d3; border-radius: 7px; background: #fff7f6; color: #9e4e49; font-size: 11px; }.notice-close { display: grid; width: 25px; height: 25px; margin-left: auto; place-items: center; border: 0; background: transparent; color: inherit; cursor: pointer; }
.roadmap-track { display: grid; grid-template-columns: auto 1fr auto 1fr auto; align-items: center; gap: 7px; max-width: 540px; margin-top: 14px; }.roadmap-track > div { display: grid; justify-items: center; gap: 4px; color: #97a5bc; }.roadmap-track > div > span { display: grid; width: 23px; height: 23px; place-items: center; border: 1px solid #dce5f2; border-radius: 50%; background: #fff; color: #8192ad; font-size: 8px; font-weight: 700; }.roadmap-track > div.reached > span,.roadmap-track > div.current > span { border-color: #3476f1; background: #3476f1; color: white; }.roadmap-track > div.current > span { box-shadow: 0 0 0 4px #e8f0ff; }.roadmap-track > div small { font-size: 8px; white-space: nowrap; }.roadmap-track > i { height: 2px; background: #e4eaf4; }.roadmap-track > i.reached { background: #55c6a7; }
.course-progress { display: grid; width: 100%; grid-template-columns: 1fr auto; align-items: center; gap: 6px 10px; margin: auto 0 13px; color: #7c8eaa; font-size: 9px; }.course-progress strong { color: #3474e9; font-size: 10px; }.course-progress > div { grid-column: 1 / -1; height: 5px; overflow: hidden; border-radius: 4px; background: #eaf0f8; }.course-progress > div i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg,#3475f2,#38c3a1); }
.test-list { display: grid; gap: 10px; padding: 0; border: 0; background: transparent; }.test-row { padding: 13px; border: 1px solid #e6edf6; border-radius: 8px; background: #fff; }.test-row + .test-row { border-top: 1px solid #e6edf6; }
.progress-visuals { display: grid; grid-template-columns: minmax(0,1.45fr) minmax(230px,.8fr); gap: 13px; }.trend-panel,.score-panel { min-width: 0; }.score-chart { display: flex; height: 168px; align-items: stretch; justify-content: space-around; gap: 10px; padding: 10px 8px 0; border-bottom: 1px solid #e8eef7; background: repeating-linear-gradient(to bottom, transparent 0, transparent 40px, #edf2f9 41px, transparent 42px); }.score-column { display: grid; min-width: 24px; flex: 1; grid-template-rows: 17px 1fr 19px; justify-items: center; align-items: flex-end; gap: 4px; color: #788ba9; }.score-column strong { color: #304c7d; font-size: 9px; }.score-column small { font-size: 8px; }.score-bar-track { display: flex; width: min(25px, 65%); height: 100%; align-items: flex-end; overflow: hidden; border-radius: 5px 5px 0 0; background: #f0f4fb; }.score-bar-track i { display: block; width: 100%; min-height: 5px; border-radius: 5px 5px 0 0; background: linear-gradient(180deg,#5d8df7,#3672ed); }.chart-empty { display: grid; min-height: 170px; place-items: center; color: #8494ad; font-size: 10px; text-align: center; }.score-ring { position: relative; display: grid; width: 136px; aspect-ratio: 1; margin: 4px auto 13px; place-items: center; border-radius: 50%; background: conic-gradient(#3476f1 var(--score), #eaf0f8 0); }.score-ring::before { position: absolute; width: 106px; aspect-ratio: 1; border-radius: 50%; background: #fff; content: ''; }.score-ring > div { position: relative; z-index: 1; display: grid; gap: 2px; justify-items: center; }.score-ring strong { color: #172e5c; font-size: 21px; }.score-ring small { color: #8392aa; font-size: 9px; }.score-legend { display: grid; gap: 8px; }.score-legend span { display: flex; align-items: center; gap: 7px; color: #7184a3; font-size: 9px; }.score-legend i { width: 8px; height: 8px; border-radius: 50%; background: #3476f1; }.score-legend span:nth-child(2) i { background: #43c3a1; }.score-legend strong { margin-left: auto; color: #283f6a; }
.tutor-workspace { grid-template-columns: 195px minmax(0,1fr) 220px; }.tutor-features { padding: 15px 12px; border-left: 1px solid #e8edf6; }.tutor-features h2 { margin: 2px 4px 12px; font-size: 12px; }.tutor-features button { display: flex; width: 100%; align-items: center; gap: 8px; margin-top: 5px; padding: 9px 6px; border: 0; border-radius: 6px; background: transparent; color: #8795ab; text-align: left; cursor: pointer; }.tutor-features button:hover { background: #f5f8fd; }.feature-icon { display: grid; width: 29px; height: 29px; flex: 0 0 auto; place-items: center; border-radius: 7px; }.feature-blue { color: #3274ed; background: #eaf1ff; }.feature-green { color: #17a984; background: #e6f7f0; }.feature-violet { color: #805de8; background: #f1edff; }.feature-orange { color: #e98241; background: #fff1e7; }.tutor-features button > span:nth-child(2) { display: grid; min-width: 0; flex: 1; gap: 3px; }.tutor-features strong { color: #35496c; font-size: 9px; }.tutor-features small { color: #8795ab; font-size: 8px; line-height: 1.35; }
@media (max-width: 1200px) { .tutor-workspace { grid-template-columns: 170px minmax(0,1fr); }.tutor-features { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 4px; border-top: 1px solid #e8edf6; border-left: 0; }.tutor-features h2 { grid-column: 1 / -1; }.progress-visuals { grid-template-columns: minmax(0,1.3fr) minmax(210px,.8fr); } }
@media (max-width: 900px) { .tutor-workspace { grid-template-columns: minmax(0,1fr); }.tutor-features { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media (max-width: 680px) { .roadmap-track { gap: 4px; }.roadmap-track > div small { font-size: 7px; }.progress-visuals { grid-template-columns: minmax(0,1fr); }.score-chart { height: 145px; }.score-panel { display: grid; grid-template-columns: 1fr 1fr; align-items: center; }.score-panel .section-heading { grid-column: 1 / -1; }.score-ring { width: 108px; margin: 0 auto; }.score-ring::before { width: 84px; }.score-legend { min-width: 0; }.tutor-features { grid-template-columns: minmax(0,1fr); }.tutor-features h2 { grid-column: auto; }.test-row { display: flex; }.certificate-card { min-height: 220px; } }
@media (max-width: 1200px) { .sidebar { width: 220px; padding-right: 17px; padding-left: 17px; }.workspace { width: calc(100% - 220px); margin-left: 220px; }.dashboard-grid { grid-template-columns: minmax(0,1.5fr) minmax(270px,.9fr); gap: 13px; }.welcome-art { width: 30%; opacity: .86; }.welcome-copy { width: 74%; }.continue-card { gap: 11px; }.course-art { width: 148px; }.recommendation-grid { grid-template-columns: 1fr; }.recommendation-card { align-items: center; }.recommendation-copy p { min-height: 0; }.sidebar-quote { min-height: 190px; } }
@media (max-width: 900px) { .sidebar { z-index: 20; width: 245px; transform: translateX(-100%); transition: transform .2s ease; }.sidebar.sidebar-open { transform: translateX(0); }.workspace { width: 100%; margin-left: 0; }.mobile-menu-button { display: grid; width: 35px; height: 35px; flex: 0 0 auto; place-items: center; border: 1px solid #e3eaf4; border-radius: 7px; background: white; color: #5c7196; cursor: pointer; }.topbar { height: 68px; gap: 12px; }.global-search { width: min(540px, 62%); }.dashboard-grid { grid-template-columns: minmax(0,1.55fr) minmax(260px,.85fr); }.mobile-scrim { position: fixed; z-index: 15; inset: 0; background: rgba(18,35,72,.24); } }
@media (max-width: 680px) { .topbar { padding: 0 12px; gap: 9px; }.global-search { width: auto; height: 37px; flex: 1; padding: 0 9px; }.global-search input { font-size: 10px; }.notification-button { width: 30px; margin-left: 0; }.profile-button { gap: 5px; }.profile-copy,.profile-button > svg { display: none; }.avatar { width: 33px; height: 33px; }.content-area { padding: 13px 12px 28px; }.dashboard-grid { grid-template-columns: minmax(0,1fr); gap: 13px; }.main-column,.right-column { gap: 13px; }.welcome-banner { min-height: 238px; padding: 20px 16px; }.welcome-copy { width: 100%; }.welcome-copy h1 { max-width: 25ch; font-size: 23px; }.welcome-copy > p:not(.welcome-kicker) { max-width: 46ch; font-size: 11px; }.welcome-art { right: -15px; bottom: -20px; top: auto; width: 170px; height: 135px; opacity: .32; }.quick-stats { position: relative; z-index: 3; gap: 5px; margin-top: 17px; }.quick-stat { min-width: 0; gap: 6px; padding: 7px; }.quick-icon { width: 27px; height: 27px; }.quick-stat strong { font-size: 14px; }.quick-stat small { max-width: 68px; font-size: 8px; white-space: normal; }.section-block { padding: 14px 12px; }.section-heading h2,.panel-heading h2 { font-size: 13px; }.continue-card { display: grid; grid-template-columns: 90px minmax(0,1fr); gap: 10px; padding: 8px; }.course-art { width: 90px; height: 98px; grid-row: span 2; }.course-art > svg { width: 29px; height: 29px; }.course-art small { max-width: 75px; font-size: 7px; }.course-label { top: 7px; left: 7px; font-size: 6px; }.continue-body { grid-column: 2; }.continue-title { align-items: flex-start; flex-direction: column; gap: 4px; }.continue-title h3 { max-width: 100%; font-size: 11px; }.course-tags { margin: 7px 0 8px; font-size: 8px; }.continue-meta { gap: 8px; margin-top: 8px; font-size: 8px; }.continue-button { grid-column: 2; justify-self: start; min-height: 31px; font-size: 9px; }.skill-overview { grid-template-columns: 105px minmax(0,1fr); gap: 8px; padding: 8px; }.mastery-ring { width: 64px; height: 64px; }.mastery-ring::before { width: 50px; height: 50px; }.skill-list { gap: 9px; }.skill-label strong { font-size: 9px; }.skill-label small { font-size: 8px; }.skill-bullet { width: 21px; height: 21px; }.skill-bar { margin-left: 28px; }.empty-recommendation { align-items: flex-start; flex-wrap: wrap; font-size: 10px; }.empty-recommendation > span { min-width: calc(100% - 34px); }.recommendation-grid { grid-template-columns: 1fr; }.assistant-panel,.weekly-panel,.goal-panel { padding: 14px; }.metric-cell { min-height: 82px; }.view-heading { align-items: flex-start; flex-direction: column; gap: 12px; }.view-heading h1 { font-size: 23px; }.search-field { width: 100%; }.plan-form { padding: 14px; }.form-row { grid-template-columns: 1fr; }.certificate-grid { grid-template-columns: 1fr; }.certificate-card { min-height: 190px; }.test-row { align-items: flex-start; gap: 9px; }.test-icon { width: 34px; height: 34px; }.test-info h2 { font-size: 11px; }.test-info p { font-size: 9px; }.test-meta { gap: 7px; font-size: 8px; }.test-row .primary-button { align-self: center; min-height: 31px; gap: 4px; padding: 0 8px; font-size: 9px; }.progress-summary { gap: 7px; }.progress-summary > div { padding: 10px; }.progress-summary strong { font-size: 17px; }.progress-summary small { font-size: 8px; }.select-field { grid-template-columns: auto minmax(120px,1fr); width: 100%; }.tutor-workspace { grid-template-columns: minmax(0,1fr); }.conversation-list { display: flex; max-height: 110px; gap: 5px; overflow-x: auto; border-right: 0; border-bottom: 1px solid #e8edf6; }.conversation-list-heading { min-width: 118px; padding: 4px; }.conversation-item { width: auto; min-width: 125px; }.chat-panel { min-height: 370px; padding: 11px; }.chat-empty { padding: 12px; }.account-panel { flex-wrap: wrap; }.account-panel .secondary-button { margin-left: auto; }.plan-row { gap: 9px; }.plan-row-icon { width: 34px; height: 34px; }.plan-row-title { align-items: flex-start; flex-direction: column; gap: 5px; }.plan-row-content > p { line-height: 1.6; }.plan-row-content .plan-progress small { min-width: 76px; }.empty-inline { align-items: flex-start; flex-wrap: wrap; }.empty-inline > div { flex-basis: calc(100% - 60px); }.empty-inline > .outline-button { margin-left: 50px; }.result-panel { min-height: 340px; }.question-panel { padding: 12px; } }
</style>
