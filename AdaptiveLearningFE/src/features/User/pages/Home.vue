<script setup>
import { computed, onMounted, onUnmounted, provide, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  BarChart3, Bell, BookMarked, BookOpen, CheckCircle2, ChevronDown, CircleHelp, Crown,
  GraduationCap, Headphones, Info, KeyRound, LayoutDashboard, LockKeyhole, LogOut, Menu, Search, Settings, Sparkles, Target, User, X,
} from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { learningService } from '@/services/learningService'
import { homeContextKey } from '../composables/homeContext'
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  Modal,
  toast,
  useConfirm,
} from '@/components/ui/overlay'
import DashboardView from '../components/views/DashboardView.vue'
import PlansView from '../components/views/PlansView.vue'
import CoursesView from '../components/views/CoursesView.vue'
import TestsView from '../components/views/TestsView.vue'
import ProgressView from '../components/views/ProgressView.vue'
import TutorView from '../components/views/TutorView.vue'
import SettingsView from '../components/views/SettingsView.vue'
import ExamSessionView from '../components/views/ExamSessionView.vue'
import ExamResultView from '../components/views/ExamResultView.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { confirm } = useConfirm()
const activeView = ref(route.meta.view || 'dashboard')
const searchQuery = ref('')
const exams = ref([])
const examTemplates = ref([])
const entitlements = ref(null)
const creatingExamTemplateId = ref(null)
const practiceExamNotice = ref('')
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
const profileMenuOpen = ref(false)
const userStatus = ref('ready')
const statusLabel = computed(() => {
  if (userStatus.value === 'ready') return 'Sẵn sàng'
  if (userStatus.value === 'busy') return 'Bận'
  return 'Vắng mặt'
})
function cycleStatus() {
  if (userStatus.value === 'ready') userStatus.value = 'busy'
  else if (userStatus.value === 'busy') userStatus.value = 'away'
  else userStatus.value = 'ready'
}

const notificationsMuted = ref(false)
function toggleNotifications() {
  notificationsMuted.value = !notificationsMuted.value
  toast.info(notificationsMuted.value ? 'Đã tắt thông báo' : 'Đã bật thông báo')
}

const supportModalOpen = ref(false)
const aboutModalOpen = ref(false)
const passwordModalOpen = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordError = ref('')
const changingPassword = ref(false)

function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordError.value = 'Vui lòng nhập đầy đủ thông tin.'
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = 'Mật khẩu xác nhận không khớp.'
    return
  }
  changingPassword.value = true
  passwordError.value = ''
  setTimeout(() => {
    toast.success('Đổi mật khẩu thành công!')
    passwordModalOpen.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    changingPassword.value = false
  }, 400)
}

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

const userName = computed(() => userStore.state.fullName || userStore.state.username || 'Học viên')
const isPremium = computed(() => entitlements.value?.plan === 'PREMIUM' || userStore.state.role === 'ROLE_ADMIN')
const greetingName = computed(() => userStore.state.fullName || userName.value.split('@')[0].split(/[._-]/)[0])
const initials = computed(() => (userStore.state.fullName || userName.value).slice(0, 1).toUpperCase())
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

provide(homeContextKey, reactive({
  activeView, searchQuery, exams, examTemplates, entitlements, creatingExamTemplateId, practiceExamNotice, isPremium,
  history, recommendations, certificates, plans,
  progress, loading, pageError, selectedCertificateId, showPlanForm, creatingPlan,
  planForm, attempt, attemptQuestions, answers, result, secondsLeft, submitting,
  saveState, conversations, currentConversation, messages, tutorPrompt, askingTutor,
  userStore, userName, greetingName, initials, publishedExams, filteredExams,
  activePlans, activePlan, activeRecommendations, scoreTrend, averageScore,
  averageScorePercent, doneCount, completedPlanRatio, greeting, selectedCertificate,
  masterySkills, formattedTimeLeft,
  showView, loadProgress, formatDate, recommendationTitle, certificateProgress,
  markRecommendationStarted, createPlan, selectConversation, askTutor, startExam, createPracticeExam,
  parseOptions, saveAnswer, leaveAttempt, submitExam, logout, localizeStatus,
}))

function asList(value) {
  if (Array.isArray(value)) return value
  return value?.data || value?.content || value?.items || []
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
  if (view === 'tutor' && !isPremium.value) {
    mobileMenuOpen.value = false
    router.push('/pricing')
    return
  }
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
    learningService.certificates(), learningService.plans(), learningService.examTemplates(), learningService.entitlements(),
  ])
  const [examResult, historyResult, recommendationResult, certificateResult, plansResult, templateResult, entitlementResult] = results
  if (examResult.status === 'fulfilled') exams.value = asList(examResult.value)
  if (historyResult.status === 'fulfilled') history.value = asList(historyResult.value)
  if (recommendationResult.status === 'fulfilled') recommendations.value = asList(recommendationResult.value)
  if (certificateResult.status === 'fulfilled') {
    certificates.value = asList(certificateResult.value)
    if (!selectedCertificateId.value && certificates.value.length) selectedCertificateId.value = String(certificates.value[0].certificateId)
  }
  if (plansResult.status === 'fulfilled') plans.value = asList(plansResult.value)
  if (templateResult.status === 'fulfilled') examTemplates.value = asList(templateResult.value)
  if (entitlementResult.status === 'fulfilled') entitlements.value = entitlementResult.value
  if (results.every((item) => item.status === 'rejected')) {
    pageError.value = 'Không kết nối được backend. Hãy kiểm tra backend và cơ sở dữ liệu rồi tải lại.'
  }
  loading.value = false
  if (selectedCertificateId.value) loadProgress()
}

async function createPracticeExam(template) {
  if (creatingExamTemplateId.value) return
  creatingExamTemplateId.value = template.templateId
  practiceExamNotice.value = ''
  pageError.value = ''
  try {
    await learningService.generateExamFromTemplate(template.templateId, {
      sectionDifficulties: (template.sections || []).map(() => 'MEDIUM'),
    })
    practiceExamNotice.value = 'Đã tạo đề luyện tập.'
    await loadDashboard()
  } catch (error) {
    const responseCode = error?.response?.data?.responseCode
    if (responseCode === 'FREE_LIMIT_REACHED' || responseCode === 'PREMIUM_REQUIRED') {
      router.push('/pricing')
    } else {
      pageError.value = error?.response?.data?.responseMessage || 'Không thể tạo đề luyện tập. Hãy thử lại.'
    }
  } finally {
    creatingExamTemplateId.value = null
  }
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
  for (const key of Object.keys(answers)) delete answers[key]
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

async function leaveAttempt() {
  if (submitting.value) return
  if (attempt.value) {
    const shouldLeave = await confirm({
      title: 'Rời bài thi?',
      description: 'Lượt thi hiện tại vẫn được lưu.',
      confirmText: 'Rời bài thi',
      cancelText: 'Tiếp tục làm bài',
      variant: 'default',
    })
    if (!shouldLeave) return
  }
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
        <button v-for="item in navigation" :key="item.id" :class="['side-link', { selected: activeView === item.id || (item.id === 'tests' && ['taking', 'result'].includes(activeView)), 'side-link-locked': item.id === 'tutor' && !isPremium }]" :aria-label="item.id === 'tutor' && !isPremium ? 'Trợ lý AI, yêu cầu Premium' : item.label" @click="showView(item.id)"><component :is="item.icon" :size="19" :stroke-width="1.9" /><span>{{ item.label }}</span><LockKeyhole v-if="item.id === 'tutor' && !isPremium" :size="14" /></button>
      </nav>
      <div class="sidebar-quote"><span class="quote-icon"><Target :size="22" /></span><strong>Học hôm nay,<br>kiến tạo tương lai</strong><p>“Mỗi bước nhỏ đều là một tiến bộ lớn.”</p><div class="quote-mountain"><span></span><i></i><b>⚑</b></div></div>
      <div class="sidebar-bottom"><span class="connection-dot"></span><span>Adaptive Learning</span><span class="version">Học tập cá nhân hóa</span></div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <button class="mobile-menu-button" :aria-label="mobileMenuOpen ? 'Đóng menu' : 'Mở menu'" @click="mobileMenuOpen = !mobileMenuOpen"><X v-if="mobileMenuOpen" :size="20" /><Menu v-else :size="20" /></button>
        <RouterLink v-if="!isPremium" to="/pricing" class="topbar-upgrade"><Crown :size="15" />Nâng cấp</RouterLink>
        <label class="global-search"><Search :size="18" /><input v-model="searchQuery" type="search" placeholder="Tìm kiếm khóa học, bài học, kỹ năng..." aria-label="Tìm kiếm"></label>
        <button class="notification-button" title="Xem gợi ý học tập" aria-label="Xem gợi ý học tập" @click="showView('dashboard'); $nextTick(() => document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'center' }))"><Bell :size="19" /><span v-if="activeRecommendations.length" class="notification-dot"></span></button>
        <DropdownMenu v-model:open="profileMenuOpen" align="end" :side-offset="8">
          <template #trigger>
            <button class="profile-header-btn" type="button" aria-label="Mở menu tài khoản">
              <span class="profile-avatar-wrap">
                <span class="avatar">{{ initials }}</span>
                <span class="avatar-status-dot" :class="userStatus"></span>
              </span>
              <strong class="profile-header-name">{{ userName }}</strong>
              <ChevronDown :size="15" class="profile-header-chevron" />
            </button>
          </template>

          <div class="user-custom-menu">
            <!-- TRẠNG THÁI -->
            <div class="menu-control-row">
              <span class="menu-control-label">TRẠNG THÁI</span>
              <button type="button" class="menu-status-badge" @click.stop="cycleStatus" title="Nhấp để đổi trạng thái">
                <span class="status-live-dot" :class="userStatus"></span>
                <span class="status-live-text">{{ statusLabel }}</span>
                <ChevronDown :size="13" class="status-live-chevron" />
              </button>
            </div>

            <!-- TẮT THÔNG BÁO -->
            <div class="menu-control-row">
              <span class="menu-control-label">TẮT THÔNG BÁO</span>
              <button
                type="button"
                role="switch"
                :aria-checked="notificationsMuted"
                class="menu-switch"
                :class="{ active: notificationsMuted }"
                @click.stop="toggleNotifications"
                aria-label="Bật hoặc tắt thông báo"
              >
                <span class="menu-switch-thumb"></span>
              </button>
            </div>

            <DropdownMenuSeparator />

            <!-- MENU ITEMS -->
            <DropdownMenuItem @select="showView('settings')">
              <template #icon><User :size="17" /></template>
              {{ userStore.state.role === 'ROLE_ADMIN' ? 'Thông tin cán bộ' : 'Thông tin cá nhân' }}
            </DropdownMenuItem>

            <DropdownMenuItem @select="passwordModalOpen = true">
              <template #icon><KeyRound :size="17" /></template>
              Đổi mật khẩu
            </DropdownMenuItem>

            <DropdownMenuItem @select="supportModalOpen = true">
              <template #icon><Headphones :size="17" /></template>
              Hỗ trợ
            </DropdownMenuItem>

            <DropdownMenuItem @select="aboutModalOpen = true">
              <template #icon><Info :size="17" /></template>
              Giới thiệu
            </DropdownMenuItem>

            <DropdownMenuSeparator />

            <!-- ĐĂNG XUẤT -->
            <DropdownMenuItem destructive @select="logout">
              <template #icon><LogOut :size="17" /></template>
              Đăng xuất
            </DropdownMenuItem>
          </div>
        </DropdownMenu>
      </header>

      <div class="mobile-scrim" v-if="mobileMenuOpen" @click="mobileMenuOpen = false"></div>
      <div class="content-area">
        <div v-if="pageError" class="notice" role="alert"><CircleHelp :size="17" />{{ pageError }}<button class="notice-close" aria-label="Đóng thông báo" @click="pageError = ''"><X :size="16" /></button></div>

        <DashboardView v-if="activeView === 'dashboard'" />
        <PlansView v-else-if="activeView === 'plans'" />
        <CoursesView v-else-if="activeView === 'courses'" />
        <TestsView v-else-if="activeView === 'tests'" />
        <ProgressView v-else-if="activeView === 'progress' || activeView === 'history'" />
        <TutorView v-else-if="activeView === 'tutor'" />
        <SettingsView v-else-if="activeView === 'settings'" />
        <ExamSessionView v-else-if="activeView === 'taking'" />
        <ExamResultView v-else-if="activeView === 'result' && result" />
      </div>
    </section>

    <!-- Modal Hỗ trợ -->
    <Modal v-model:open="supportModalOpen" title="Trung tâm Hỗ trợ" description="Chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7" :icon="Headphones">
      <div class="modal-info-stack">
        <div class="modal-info-row">
          <strong>Tổng đài hỗ trợ</strong>
          <p>1900 6868 (8:00 - 22:00 hàng ngày)</p>
        </div>
        <div class="modal-info-row">
          <strong>Email tiếp nhận</strong>
          <p>hotro@adaptivelearning.edu.vn</p>
        </div>
        <div class="modal-info-row">
          <strong>Trợ lý AI 24/7</strong>
          <p>Truy cập mục "Trợ lý AI" ở thanh điều hướng bên trái để được giải đáp thắc mắc tức thì.</p>
        </div>
      </div>
      <template #footer>
        <button class="primary-button" @click="supportModalOpen = false">Đã hiểu</button>
      </template>
    </Modal>

    <!-- Modal Giới thiệu -->
    <Modal v-model:open="aboutModalOpen" title="Giới thiệu Adaptive Learning" description="Nền tảng học tập & luyện thi cá nhân hóa AI" :icon="Info">
      <div class="modal-info-stack">
        <p><strong>Adaptive Learning</strong> là hệ thống học tập thông minh, ứng dụng mô hình trí tuệ nhân tạo để phân tích năng lực, đánh giá điểm mạnh - điểm yếu và cá nhân hóa lộ trình cho từng học viên.</p>
        <div class="modal-info-row">
          <strong>Phiên bản</strong>
          <p>Adaptive Learning v2.4.0 (Build 2026)</p>
        </div>
      </div>
      <template #footer>
        <button class="primary-button" @click="aboutModalOpen = false">Đóng</button>
      </template>
    </Modal>

    <!-- Modal Đổi mật khẩu -->
    <Modal v-model:open="passwordModalOpen" title="Đổi mật khẩu" description="Cập nhật mật khẩu để tăng cường bảo mật cho tài khoản của bạn" :icon="KeyRound">
      <form class="password-form-stack" @submit.prevent="handleChangePassword">
        <div v-if="passwordError" class="notice notice-danger">{{ passwordError }}</div>
        <label class="form-field">
          <span>Mật khẩu hiện tại</span>
          <input v-model="passwordForm.oldPassword" type="password" placeholder="Nhập mật khẩu cũ" required />
        </label>
        <label class="form-field">
          <span>Mật khẩu mới</span>
          <input v-model="passwordForm.newPassword" type="password" placeholder="Tối thiểu 6 ký tự" required />
        </label>
        <label class="form-field">
          <span>Nhập lại mật khẩu mới</span>
          <input v-model="passwordForm.confirmPassword" type="password" placeholder="Xác nhận mật khẩu mới" required />
        </label>
        <div class="form-actions" style="margin-top: 12px;">
          <button type="button" class="secondary-button" @click="passwordModalOpen = false">Hủy</button>
          <button type="submit" class="primary-button" :disabled="changingPassword">Lưu mật khẩu mới</button>
        </div>
      </form>
    </Modal>
  </main>
</template>

<style>
.app-shell { --ink: #101d49; --muted: #6e82a8; --line: #e6edf8; --blue: #2d68f3; --green: #23ad8c; display: flex; min-height: 100vh; color: var(--ink); background: #f5f8fd; font-family: var(--font-sans); }
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
.template-test-list { margin-bottom: 20px; }
.legacy-exam-heading { margin: 20px 0 10px; }
.template-picker-backdrop { position: fixed; z-index: 80; inset: 0; display: grid; place-items: center; padding: 16px; background: rgba(20,32,52,.48); }
.template-picker { width: min(100%, 520px); max-height: min(90vh, 760px); overflow: auto; padding: 20px; border: 1px solid #e3e9ef; border-radius: 8px; background: #fff; box-shadow: 0 22px 70px rgba(10,25,38,.25); }
.template-picker-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding-bottom: 14px; border-bottom: 1px solid #e8edf1; }
.template-picker-heading h2 { margin: 2px 0 5px; color: #263a4e; font-size: 17px; }
.template-picker-heading p:not(.view-kicker) { margin: 0; color: #82909d; font-size: 10px; }
.template-difficulty-form { display: grid; gap: 0; }
.template-difficulty-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 0; border-bottom: 1px solid #edf1f4; }
.template-difficulty-row > span { display: grid; gap: 4px; }
.template-difficulty-row strong { color: #35495a; font-size: 10px; }
.template-difficulty-row small { color: #82909d; font-size: 9px; }
.template-difficulty-row select { width: min(48%, 190px); min-height: 36px; }
.template-difficulty-form footer { display: flex; justify-content: flex-end; gap: 8px; padding-top: 14px; }
.progress-visuals { display: grid; grid-template-columns: minmax(0,1.45fr) minmax(230px,.8fr); gap: 13px; }.trend-panel,.score-panel { min-width: 0; }.score-chart { display: flex; height: 168px; align-items: stretch; justify-content: space-around; gap: 10px; padding: 10px 8px 0; border-bottom: 1px solid #e8eef7; background: repeating-linear-gradient(to bottom, transparent 0, transparent 40px, #edf2f9 41px, transparent 42px); }.score-column { display: grid; min-width: 24px; flex: 1; grid-template-rows: 17px 1fr 19px; justify-items: center; align-items: flex-end; gap: 4px; color: #788ba9; }.score-column strong { color: #304c7d; font-size: 9px; }.score-column small { font-size: 8px; }.score-bar-track { display: flex; width: min(25px, 65%); height: 100%; align-items: flex-end; overflow: hidden; border-radius: 5px 5px 0 0; background: #f0f4fb; }.score-bar-track i { display: block; width: 100%; min-height: 5px; border-radius: 5px 5px 0 0; background: linear-gradient(180deg,#5d8df7,#3672ed); }.chart-empty { display: grid; min-height: 170px; place-items: center; color: #8494ad; font-size: 10px; text-align: center; }.score-ring { position: relative; display: grid; width: 136px; aspect-ratio: 1; margin: 4px auto 13px; place-items: center; border-radius: 50%; background: conic-gradient(#3476f1 var(--score), #eaf0f8 0); }.score-ring::before { position: absolute; width: 106px; aspect-ratio: 1; border-radius: 50%; background: #fff; content: ''; }.score-ring > div { position: relative; z-index: 1; display: grid; gap: 2px; justify-items: center; }.score-ring strong { color: #172e5c; font-size: 21px; }.score-ring small { color: #8392aa; font-size: 9px; }.score-legend { display: grid; gap: 8px; }.score-legend span { display: flex; align-items: center; gap: 7px; color: #7184a3; font-size: 9px; }.score-legend i { width: 8px; height: 8px; border-radius: 50%; background: #3476f1; }.score-legend span:nth-child(2) i { background: #43c3a1; }.score-legend strong { margin-left: auto; color: #283f6a; }
.tutor-workspace { grid-template-columns: 195px minmax(0,1fr) 220px; }.tutor-features { padding: 15px 12px; border-left: 1px solid #e8edf6; }.tutor-features h2 { margin: 2px 4px 12px; font-size: 12px; }.tutor-features button { display: flex; width: 100%; align-items: center; gap: 8px; margin-top: 5px; padding: 9px 6px; border: 0; border-radius: 6px; background: transparent; color: #8795ab; text-align: left; cursor: pointer; }.tutor-features button:hover { background: #f5f8fd; }.feature-icon { display: grid; width: 29px; height: 29px; flex: 0 0 auto; place-items: center; border-radius: 7px; }.feature-blue { color: #3274ed; background: #eaf1ff; }.feature-green { color: #17a984; background: #e6f7f0; }.feature-violet { color: #805de8; background: #f1edff; }.feature-orange { color: #e98241; background: #fff1e7; }.tutor-features button > span:nth-child(2) { display: grid; min-width: 0; flex: 1; gap: 3px; }.tutor-features strong { color: #35496c; font-size: 9px; }.tutor-features small { color: #8795ab; font-size: 8px; line-height: 1.35; }
@media (max-width: 1200px) { .tutor-workspace { grid-template-columns: 170px minmax(0,1fr); }.tutor-features { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 4px; border-top: 1px solid #e8edf6; border-left: 0; }.tutor-features h2 { grid-column: 1 / -1; }.progress-visuals { grid-template-columns: minmax(0,1.3fr) minmax(210px,.8fr); } }
@media (max-width: 900px) { .tutor-workspace { grid-template-columns: minmax(0,1fr); }.tutor-features { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media (max-width: 680px) { .roadmap-track { gap: 4px; }.roadmap-track > div small { font-size: 7px; }.progress-visuals { grid-template-columns: minmax(0,1fr); }.score-chart { height: 145px; }.score-panel { display: grid; grid-template-columns: 1fr 1fr; align-items: center; }.score-panel .section-heading { grid-column: 1 / -1; }.score-ring { width: 108px; margin: 0 auto; }.score-ring::before { width: 84px; }.score-legend { min-width: 0; }.tutor-features { grid-template-columns: minmax(0,1fr); }.tutor-features h2 { grid-column: auto; }.test-row { display: flex; }.certificate-card { min-height: 220px; } }
@media (max-width: 1200px) { .sidebar { width: 220px; padding-right: 17px; padding-left: 17px; }.workspace { width: calc(100% - 220px); margin-left: 220px; }.dashboard-grid { grid-template-columns: minmax(0,1.5fr) minmax(270px,.9fr); gap: 13px; }.welcome-art { width: 30%; opacity: .86; }.welcome-copy { width: 74%; }.continue-card { gap: 11px; }.course-art { width: 148px; }.recommendation-grid { grid-template-columns: 1fr; }.recommendation-card { align-items: center; }.recommendation-copy p { min-height: 0; }.sidebar-quote { min-height: 190px; } }
@media (max-width: 900px) { .sidebar { z-index: 20; width: 245px; transform: translateX(-100%); transition: transform .2s ease; }.sidebar.sidebar-open { transform: translateX(0); }.workspace { width: 100%; margin-left: 0; }.mobile-menu-button { display: grid; width: 35px; height: 35px; flex: 0 0 auto; place-items: center; border: 1px solid #e3eaf4; border-radius: 7px; background: white; color: #5c7196; cursor: pointer; }.topbar { height: 68px; gap: 12px; }.global-search { width: min(540px, 62%); }.dashboard-grid { grid-template-columns: minmax(0,1.55fr) minmax(260px,.85fr); }.mobile-scrim { position: fixed; z-index: 15; inset: 0; background: rgba(18,35,72,.24); } }
@media (max-width: 680px) { .topbar { padding: 0 12px; gap: 9px; }.global-search { width: auto; height: 37px; flex: 1; padding: 0 9px; }.global-search input { font-size: 10px; }.notification-button { width: 30px; margin-left: 0; }.profile-button { gap: 5px; }.profile-copy,.profile-button > svg { display: none; }.avatar { width: 33px; height: 33px; }.content-area { padding: 13px 12px 28px; }.dashboard-grid { grid-template-columns: minmax(0,1fr); gap: 13px; }.main-column,.right-column { gap: 13px; }.welcome-banner { min-height: 238px; padding: 20px 16px; }.welcome-copy { width: 100%; }.welcome-copy h1 { max-width: 25ch; font-size: 23px; }.welcome-copy > p:not(.welcome-kicker) { max-width: 46ch; font-size: 11px; }.welcome-art { right: -15px; bottom: -20px; top: auto; width: 170px; height: 135px; opacity: .32; }.quick-stats { position: relative; z-index: 3; gap: 5px; margin-top: 17px; }.quick-stat { min-width: 0; gap: 6px; padding: 7px; }.quick-icon { width: 27px; height: 27px; }.quick-stat strong { font-size: 14px; }.quick-stat small { max-width: 68px; font-size: 8px; white-space: normal; }.section-block { padding: 14px 12px; }.section-heading h2,.panel-heading h2 { font-size: 13px; }.continue-card { display: grid; grid-template-columns: 90px minmax(0,1fr); gap: 10px; padding: 8px; }.course-art { width: 90px; height: 98px; grid-row: span 2; }.course-art > svg { width: 29px; height: 29px; }.course-art small { max-width: 75px; font-size: 7px; }.course-label { top: 7px; left: 7px; font-size: 6px; }.continue-body { grid-column: 2; }.continue-title { align-items: flex-start; flex-direction: column; gap: 4px; }.continue-title h3 { max-width: 100%; font-size: 11px; }.course-tags { margin: 7px 0 8px; font-size: 8px; }.continue-meta { gap: 8px; margin-top: 8px; font-size: 8px; }.continue-button { grid-column: 2; justify-self: start; min-height: 31px; font-size: 9px; }.skill-overview { grid-template-columns: 105px minmax(0,1fr); gap: 8px; padding: 8px; }.mastery-ring { width: 64px; height: 64px; }.mastery-ring::before { width: 50px; height: 50px; }.skill-list { gap: 9px; }.skill-label strong { font-size: 9px; }.skill-label small { font-size: 8px; }.skill-bullet { width: 21px; height: 21px; }.skill-bar { margin-left: 28px; }.empty-recommendation { align-items: flex-start; flex-wrap: wrap; font-size: 10px; }.empty-recommendation > span { min-width: calc(100% - 34px); }.recommendation-grid { grid-template-columns: 1fr; }.assistant-panel,.weekly-panel,.goal-panel { padding: 14px; }.metric-cell { min-height: 82px; }.view-heading { align-items: flex-start; flex-direction: column; gap: 12px; }.view-heading h1 { font-size: 23px; }.search-field { width: 100%; }.plan-form { padding: 14px; }.form-row { grid-template-columns: 1fr; }.certificate-grid { grid-template-columns: 1fr; }.certificate-card { min-height: 190px; }.test-row { align-items: flex-start; gap: 9px; }.test-icon { width: 34px; height: 34px; }.test-info h2 { font-size: 11px; }.test-info p { font-size: 9px; }.test-meta { gap: 7px; font-size: 8px; }.test-row .primary-button { align-self: center; min-height: 31px; gap: 4px; padding: 0 8px; font-size: 9px; }.progress-summary { gap: 7px; }.progress-summary > div { padding: 10px; }.progress-summary strong { font-size: 17px; }.progress-summary small { font-size: 8px; }.select-field { grid-template-columns: auto minmax(120px,1fr); width: 100%; }.tutor-workspace { grid-template-columns: minmax(0,1fr); }.conversation-list { display: flex; max-height: 110px; gap: 5px; overflow-x: auto; border-right: 0; border-bottom: 1px solid #e8edf6; }.conversation-list-heading { min-width: 118px; padding: 4px; }.conversation-item { width: auto; min-width: 125px; }.chat-panel { min-height: 370px; padding: 11px; }.chat-empty { padding: 12px; }.account-panel { flex-wrap: wrap; }.account-panel .secondary-button { margin-left: auto; }.plan-row { gap: 9px; }.plan-row-icon { width: 34px; height: 34px; }.plan-row-title { align-items: flex-start; flex-direction: column; gap: 5px; }.plan-row-content > p { line-height: 1.6; }.plan-row-content .plan-progress small { min-width: 76px; }.empty-inline { align-items: flex-start; flex-wrap: wrap; }.empty-inline > div { flex-basis: calc(100% - 60px); }.empty-inline > .outline-button { margin-left: 50px; }.result-panel { min-height: 340px; }.question-panel { padding: 12px; } }
.side-link-locked { color: #6f7890; }.side-link-locked > svg:last-child { margin-left: auto; color: #8b6b1b; }
.free-quota-banner { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 16px; border: 1px solid #e0e7ef; border-radius: 8px; background: #fff; }.free-quota-banner > div { display: grid; width: min(100%, 480px); gap: 4px; }.free-quota-banner strong { color: #334155; font-size: 12px; }.free-quota-banner p { margin: 0; color: #526070; font-size: 11px; }.free-quota-track { height: 5px; margin-top: 4px; overflow: hidden; background: #eef1f6; }.free-quota-track i { display: block; height: 100%; background: #0f766e; }.quota-remaining { color: #475569; font-size: 11px; font-weight: 600; white-space: nowrap; }.quota-upgrade { display: inline-flex; min-height: 40px; align-items: center; gap: 7px; padding: 0 12px; border-radius: 6px; color: #fff; background: #0f766e; font-size: 11px; font-weight: 600; text-decoration: none; white-space: nowrap; }.quota-upgrade:hover { background: #115e59; }.practice-exam-notice { margin: 0; padding: 10px 0; border-bottom: 1px solid #e6edf6; color: #0f5f58; font-size: 12px; }
.user-menu-profile { display: flex; align-items: center; gap: 12px; padding: 10px 12px; min-width: 250px; }
.user-menu-avatar { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: #fff; font-size: 15px; font-weight: 700; box-shadow: 0 2px 8px rgba(37,99,235,.25); }
.user-menu-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.user-menu-name { font-size: 13px; font-weight: 700; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-menu-email { font-size: 11px; color: #64748b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-menu-badge-row { margin-top: 4px; }
.user-menu-badge { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 999px; font-size: 10px; font-weight: 600; }
.user-menu-badge.badge-premium { background: #fef3c7; color: #b45309; border: 1px solid #fde68a; }
.user-menu-badge.badge-free { background: #f1f5f9; color: #475569; border: 1px solid #e2e8f0; }
.user-menu-crown { color: #f59e0b; }
</style>
