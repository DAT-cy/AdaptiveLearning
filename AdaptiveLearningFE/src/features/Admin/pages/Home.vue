<script setup>
  import { computed, onMounted, reactive, ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import {
    ArrowRight,
    Bell,
    BookOpen,
    Check,
    CheckCircle2,
    BarChart3,
    ChevronDown,
    ChevronLeft,
    ChevronRight,
    ClipboardCheck,
    CircleCheck,
    CirclePause,
    CreditCard,
    Database,
    Eye,
    FileText,
    GraduationCap,
    Headphones,
    Info,
    Calendar,
    DollarSign,
    KeyRound,
    Layers,
    LayoutDashboard,
    LoaderCircle,
    Lock,
    LogOut,
    Menu,
    MoreHorizontal,
    Pencil,
    Plus,
    RefreshCw,
    RotateCcw,
    Search,
    Shield,
    Sliders,
    Sparkles,
    Tag,
    Trash2,
    TrendingUp,
    Unlock,
    Upload,
    User,
    UserCheck,
    Users,
    UserX,
    Wallet,
    X,
  } from 'lucide-vue-next'
  import { useUserStore } from '@/stores/user'
  import { adminService } from '@/services/adminService'
  import {
    Drawer,
    DropdownMenu,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    Modal,
    Tooltip,
    toast,
    useConfirm,
  } from '@/components/ui/overlay'
  import ExamBuilderDialog from '../components/ExamBuilderDialog.vue'
  import { adminContextKey } from '../composables/adminContext'
import '../styles/shared.css'
  import { provide } from 'vue'
  import OverviewView from '../components/views/OverviewView.vue'
  import AccountsView from '../components/views/AccountsView.vue'
  import BillingView from '../components/views/BillingView.vue'
  import ResultsView from '../components/views/ResultsView.vue'
  import ProgramsView from '../components/views/ProgramsView.vue'
  import ExamsView from '../components/views/ExamsView.vue'
  import KnowledgeView from '../components/views/KnowledgeView.vue'


  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const { confirm } = useConfirm()
  const sectionIds = [
    'overview',
    'accounts',
    'billing',
    'results',
    'programs',
    'exams',
    'knowledge',
  ]
  function resolveSectionId(sec) {
    const raw = String(sec || 'overview')
    if (raw === 'knowledge-base') return 'knowledge'
    return sectionIds.includes(raw) ? raw : 'overview'
  }
  const initialSection = resolveSectionId(route.query.section)
  const activeSection = ref(initialSection)

  const users = ref([])
  const payments = ref([])
  const plans = ref([])
  const userBillings = ref([])
  const billingSubTab = ref('plans')
  const selectedUserForPayments = ref(null)
  const userPaymentsList = ref([])
  const userPaymentsLoading = ref(false)
  const userPaymentsDrawerOpen = ref(false)
  const userBillingSearch = ref('')
  const userBillingPlanFilter = ref('all')
  const planModalOpen = ref(false)
  const editingPlanId = ref(null)
  const savingPlan = ref(false)
  const planModalError = ref('')
  const planForm = reactive({
    code: '',
    name: '',
    description: '',
    price: 0,
    currency: 'USD',
    billingCycle: 'LIFETIME',
    examLimit: -1,
    featuresText: '',
    isActive: true,
  })
  const loadingPayments = ref(false)
  const paymentSearch = ref('')
  const paymentStatusFilter = ref('all')
  const paymentProviderFilter = ref('all')
  const analyticsPeriod = ref('30d')
  const analyticsCertificateId = ref('')
  const analyticsData = ref(null)
  const showAnalyticsSample = ref(false)
  const analyticsLoading = ref(false)
  const analyticsError = ref('')
  let analyticsRequestSequence = 0
  const selectedLearnerId = ref('')
  const learnerAttempts = ref([])
  const learnerProgress = ref(null)
  const learnerCertificateId = ref('')
  const learnerLoading = ref(false)
  const learnerError = ref('')
  const selectedAttemptResult = ref(null)
  const attemptResultOpen = ref(false)
  const attemptResultLoading = ref(false)
  let learnerLoadSequence = 0
  const programs = ref([])
  const exams = ref([])
  const knowledgeDocuments = ref([])
  const knowledgeFiles = ref([])
  const knowledgeSearch = ref('')
  const knowledgeFilter = ref('all')
  const knowledgeProgramFilter = ref('all')
  const knowledgePage = ref(1)
  const knowledgePageSize = ref(8)
  const knowledgeDragging = ref(false)
  const knowledgeNotice = ref('')
  const knowledgeFormErrors = reactive({ files: '', exam: '', titles: '' })
  const knowledgeDetail = ref(null)
  const loadingKnowledgeDetail = ref(false)
  const knowledgePreviewRequest = ref(0)
  const knowledgeEditorOpen = ref(false)
  const editingKnowledgeDocumentId = ref(null)
  const savingKnowledgeEdit = ref(false)
  const knowledgeEditBaseline = ref('')
  const knowledgeEditForm = reactive({
    title: '',
    certificateId: '',
    topicId: '',
    content: '',
  })
  const knowledgeEditorRef = ref(null)
  const topics = ref([])
  const query = ref('')
  const loading = ref(false)
  const pageError = ref('')
  const adminModalError = ref('')
  const mobileMenuOpen = ref(false)
  const adminProfileOpen = ref(false)
  const adminStatus = ref('ready')
  const adminStatusLabel = computed(() => {
    if (adminStatus.value === 'ready') return 'Sẵn sàng'
    if (adminStatus.value === 'busy') return 'Bận'
    return 'Vắng mặt'
  })
  function cycleAdminStatus() {
    if (adminStatus.value === 'ready') adminStatus.value = 'busy'
    else if (adminStatus.value === 'busy') adminStatus.value = 'away'
    else adminStatus.value = 'ready'
  }
  const adminNotificationsMuted = ref(false)
  function toggleAdminNotifications() {
    adminNotificationsMuted.value = !adminNotificationsMuted.value
    toast.info(adminNotificationsMuted.value ? 'Đã tắt thông báo' : 'Đã bật thông báo')
  }

  const adminSupportModalOpen = ref(false)
  const adminAboutModalOpen = ref(false)
  const adminPasswordModalOpen = ref(false)
  const adminPasswordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const adminPasswordError = ref('')
  const adminChangingPassword = ref(false)

  function handleAdminChangePassword() {
    if (!adminPasswordForm.oldPassword || !adminPasswordForm.newPassword) {
      adminPasswordError.value = 'Vui lòng nhập đầy đủ thông tin.'
      return
    }
    if (adminPasswordForm.newPassword !== adminPasswordForm.confirmPassword) {
      adminPasswordError.value = 'Mật khẩu xác nhận không khớp.'
      return
    }
    adminChangingPassword.value = true
    adminPasswordError.value = ''
    setTimeout(() => {
      toast.success('Đổi mật khẩu thành công!')
      adminPasswordModalOpen.value = false
      adminPasswordForm.oldPassword = ''
      adminPasswordForm.newPassword = ''
      adminPasswordForm.confirmPassword = ''
      adminChangingPassword.value = false
    }, 400)
  }

  const modal = ref('')
  const adminModalRef = ref(null)
  const adminModalBaseline = ref('')
  const showExamBuilder = ref(false)
  const saving = ref(false)
  const editingCertificateId = ref(null)
  const importingKnowledge = ref(false)
  const accountForm = reactive({
    username: '',
    password: '',
    role: 'ROLE_USER',
    canApproveExams: false,
  })
  const programForm = reactive({
    name: '',
    code: '',
    description: '',
    totalScore: 100,
    passingScore: 50,
    passingBand: 0,
    skills: '[]',
  })
  const knowledgeForm = reactive({
    certificateId: '',
    topicId: '',
  })

  const adminModalOpen = computed({
    get: () => modal.value === 'account' || modal.value === 'program',
    set: (open) => {
      if (!open) {
        modal.value = ''
        adminModalBaseline.value = ''
      }
    },
  })
  const adminModalDirty = computed(() => {
    if (!adminModalBaseline.value) return false
    const current = modal.value === 'account' ? accountForm : programForm
    return JSON.stringify(current) !== adminModalBaseline.value
  })
  const adminModalTitle = computed(() => modal.value === 'account'
    ? 'Tạo tài khoản'
    : editingCertificateId.value
      ? 'Cập nhật chứng chỉ / kỳ thi'
      : 'Tạo chứng chỉ / kỳ thi')
  const adminModalDescription = computed(() => modal.value === 'account'
    ? 'Tạo thông tin đăng nhập và phân quyền cho người dùng.'
    : 'Cấu hình danh mục kỳ thi, thang điểm và các kỹ năng áp dụng.')
  const adminModalIcon = computed(() => modal.value === 'account' ? Users : GraduationCap)
  const knowledgePreviewOpen = computed({
    get: () => Boolean(knowledgeDetail.value || loadingKnowledgeDetail.value),
    set: (open) => {
      if (!open) closeKnowledgePreview()
    },
  })
  const knowledgePreviewChunks = computed(() =>
    Array.isArray(knowledgeDetail.value?.chunks)
      ? knowledgeDetail.value.chunks
      : String(knowledgeDetail.value?.content || '').trim()
        ? [String(knowledgeDetail.value.content).trim()]
        : []
  )
  const knowledgeEditTopics = computed(() =>
    topics.value
      .filter((topic) => String(topic.certificateId) === String(knowledgeEditForm.certificateId)
        && topic.active !== false
        && topic.isActive !== false)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  )
  const knowledgeEditDirty = computed(() =>
    Boolean(knowledgeEditBaseline.value)
      && JSON.stringify(knowledgeEditForm) !== knowledgeEditBaseline.value
  )

  const navigation = [
    { id: 'overview', label: 'Tổng quan hệ thống', icon: LayoutDashboard, group: 'BÁO CÁO & PHÂN TÍCH' },
    { id: 'results', label: 'Kết quả học tập', icon: BarChart3, group: 'BÁO CÁO & PHÂN TÍCH' },
    { id: 'programs', label: 'Kỳ thi & chứng chỉ', icon: GraduationCap, group: 'QUẢN LÝ ĐÀO TẠO' },
    { id: 'exams', label: 'Bài kiểm tra', icon: ClipboardCheck, group: 'QUẢN LÝ ĐÀO TẠO' },
    { id: 'knowledge', label: 'Kho kiến thức', icon: Database, group: 'QUẢN LÝ ĐÀO TẠO' },
    { id: 'accounts', label: 'Quản lý tài khoản', icon: Users, group: 'VẬN HÀNH & HỆ THỐNG' },
    { id: 'billing', label: 'Gói cước & Thanh toán', icon: CreditCard, group: 'VẬN HÀNH & HỆ THỐNG' },
  ]

  const navGroups = [
    {
      title: 'BÁO CÁO & PHÂN TÍCH',
      items: navigation.filter((item) => item.group === 'BÁO CÁO & PHÂN TÍCH'),
    },
    {
      title: 'QUẢN LÝ ĐÀO TẠO',
      items: navigation.filter((item) => item.group === 'QUẢN LÝ ĐÀO TẠO'),
    },
    {
      title: 'VẬN HÀNH & HỆ THỐNG',
      items: navigation.filter((item) => item.group === 'VẬN HÀNH & HỆ THỐNG'),
    },
  ]

  const currentTitle = computed(
    () =>
      activeSection.value === 'overview'
        ? 'Tổng quan hệ thống'
        : navigation.find((item) => item.id === activeSection.value)?.label ||
      'Tổng quan'
  )
  const analyticsTrend = computed(() => displayAnalyticsData.value?.trend || [])
  const analyticsChartPoints = computed(() => {
    const trend = analyticsTrend.value
    const scorePoints = trend
      .map((point, index) => ({
        ...point,
        x: trend.length <= 1 ? 360 : 48 + index * (624 / (trend.length - 1)),
        y: point.averageScorePercent == null
          ? null
          : 22 + (100 - Number(point.averageScorePercent)) * 2.1,
      }))
    return scorePoints
  })
  const analyticsScorePath = computed(() => {
    const points = analyticsChartPoints.value.filter((point) => point.y != null)
    return points.map((point, index) => `${index ? 'L' : 'M'} ${point.x} ${point.y}`).join(' ')
  })
  const analyticsMaxAttempts = computed(() => Math.max(1, ...analyticsTrend.value.map((point) => point.attempts || 0)))
  const analyticsInsight = computed(() => {
    const report = displayAnalyticsData.value
    if (!report || !report.startedAttempts) return 'Chưa có lượt thi trong khoảng thời gian này.'
    if (report.completionRateChange != null && Math.abs(report.completionRateChange) >= Math.abs(report.averageScoreChange || 0)) {
      const direction = report.completionRateChange > 0 ? 'tăng' : report.completionRateChange < 0 ? 'giảm' : 'không đổi'
      return `Tỷ lệ hoàn thành ${direction} ${formatSignedPoint(report.completionRateChange)} điểm phần trăm so với kỳ trước.`
    }
    if (report.averageScoreChange != null) {
      const direction = report.averageScoreChange > 0 ? 'tăng' : report.averageScoreChange < 0 ? 'giảm' : 'giữ ổn định'
      return `Điểm trung bình ${direction} ${formatSignedPoint(report.averageScoreChange)} điểm so với kỳ trước.`
    }
    return `Có ${formatAdminNumber(report.completedAttempts)} lượt thi hoàn thành trong kỳ; chưa đủ dữ liệu để so sánh với kỳ trước.`
  })
  const analyticsPeriods = [
    { id: '7d', label: '7 ngày' },
    { id: '30d', label: '30 ngày' },
    { id: '90d', label: '90 ngày' },
  ]
  const analyticsSample = {
    startedAttempts: 79,
    completedAttempts: 64,
    completionRate: 81.0,
    completionRateChange: 4.2,
    averageScorePercent: 71.8,
    averageScoreChange: 1.6,
    passRate: 63.7,
    activeLearners: 31,
    trend: [
      { label: '30/9', attempts: 10, averageScorePercent: 68.4 },
      { label: '7/10', attempts: 13, averageScorePercent: 71.2 },
      { label: '14/10', attempts: 21, averageScorePercent: 60.3, annotation: 'Nhiều lượt thi' },
      { label: '21/10', attempts: 15, averageScorePercent: 74.5 },
      { label: '27/10', attempts: 20, averageScorePercent: 71.8 },
    ],
    learnersToSupport: [
      { userId: 'demo-1', username: 'Nguyễn Minh Anh', attempts: 4, averageScorePercent: 44.8, lastAttemptAt: null },
      { userId: 'demo-2', username: 'Trần Gia Huy', attempts: 3, averageScorePercent: 51.6, lastAttemptAt: null },
      { userId: 'demo-3', username: 'Lê Ngọc Hà', attempts: 6, averageScorePercent: 58.3, lastAttemptAt: null },
      { userId: 'demo-4', username: 'Phạm Quốc Bảo', attempts: 2, averageScorePercent: 62.7, lastAttemptAt: null },
    ],
  }
  const displayAnalyticsData = computed(() => analyticsData.value)
  const adminName = computed(() => userStore.state.fullName || userStore.state.username || 'Quản trị viên')
  const activeUsers = computed(
    () => users.value.filter((item) => item.enabled && item.emailVerified !== false).length
  )
  const activeKnowledgeCount = computed(
    () => knowledgeDocuments.value.filter((document) => document.active).length
  )
  const inactiveKnowledgeCount = computed(
    () => knowledgeDocuments.value.length - activeKnowledgeCount.value
  )
  const activeKnowledgePercent = computed(() =>
    knowledgeDocuments.value.length
      ? Math.round(activeKnowledgeCount.value / knowledgeDocuments.value.length * 100)
      : 0
  )
  const knowledgeChunkCount = computed(() =>
    knowledgeDocuments.value.reduce(
      (total, document) => total + (document.chunkCount || 0),
      0
    )
  )
  function formatAdminNumber(value) {
    return Number(value || 0).toLocaleString('vi-VN')
  }
  function formatPercent(value) {
    return value == null ? '—' : `${Number(value).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`
  }
  function formatSignedPoint(value) {
    return Math.abs(Number(value) || 0).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
  }
  function parseJson(value) {
    try { return JSON.parse(value) || {} } catch { return {} }
  }
  function examTitle(attempt) {
    return exams.value.find((exam) => String(exam.examId) === String(attempt.examId))?.title || `Bài thi #${attempt.examId}`
  }
  function formatAttemptScore(attempt) {
    if (attempt.score == null) return 'Chưa có điểm'
    return attempt.maxScore ? `${formatAdminNumber(attempt.score)} / ${formatAdminNumber(attempt.maxScore)}` : formatAdminNumber(attempt.score)
  }
  function formatAttemptDate(value) {
    if (!value) return '—'
    return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  }
  function formatTrendDate(value) {
    return value ? new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(new Date(value)) : ''
  }
  async function viewAttemptResult(attempt) {
    attemptResultOpen.value = true
    attemptResultLoading.value = true
    selectedAttemptResult.value = { attempt, result: null, error: '' }
    try {
      selectedAttemptResult.value.result = await adminService.attemptResult(attempt.attemptId)
    } catch (error) {
      selectedAttemptResult.value.error = error?.response?.data?.message || 'Không tải được chi tiết kết quả.'
    } finally {
      attemptResultLoading.value = false
    }
  }
  const filteredKnowledgeDocuments = computed(() => {
    const search = knowledgeSearch.value.trim().toLocaleLowerCase('vi')
    return knowledgeDocuments.value.filter((document) => {
      if (knowledgeFilter.value === 'active' && !document.active) return false
      if (knowledgeFilter.value === 'inactive' && document.active) return false
      if (knowledgeProgramFilter.value !== 'all' && String(document.certificateId) !== String(knowledgeProgramFilter.value)) return false
      if (!search) return true
      return `${document.title} ${document.fileName} ${programName(document.certificateId)} ${knowledgeTopicName(document.topicId)}`
        .toLocaleLowerCase('vi')
        .includes(search)
    })
  })
  const knowledgeTotalPages = computed(() =>
    Math.max(1, Math.ceil(filteredKnowledgeDocuments.value.length / knowledgePageSize.value))
  )
  const paginatedKnowledgeDocuments = computed(() => {
    const start = (knowledgePage.value - 1) * knowledgePageSize.value
    return filteredKnowledgeDocuments.value.slice(start, start + knowledgePageSize.value)
  })
  function resetKnowledgeFilters() {
    knowledgeSearch.value = ''
    knowledgeFilter.value = 'all'
    knowledgeProgramFilter.value = 'all'
    knowledgePage.value = 1
  }
  watch([knowledgeSearch, knowledgeFilter, knowledgeProgramFilter], () => {
    knowledgePage.value = 1
  })
  const knowledgeTopics = computed(() =>
    topics.value.filter(
      (topic) => String(topic.certificateId) === String(knowledgeForm.certificateId)
        && topic.active !== false
        && topic.isActive !== false
    ).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  )
  const students = computed(() =>
    users.value.filter((item) => item.role === 'ROLE_USER')
  )
  const selectedLearner = computed(() => students.value.find((item) => String(item.userId) === String(selectedLearnerId.value)) || null)
  const selectedCertificateExamIds = computed(() => new Set(exams.value.filter((exam) => String(exam.certificateId) === String(learnerCertificateId.value)).map((exam) => String(exam.examId))))
  const learnerScopedAttempts = computed(() => learnerCertificateId.value && selectedCertificateExamIds.value.size
    ? learnerAttempts.value.filter((attempt) => selectedCertificateExamIds.value.has(String(attempt.examId)))
    : learnerAttempts.value)
  const completedLearnerAttempts = computed(() => learnerScopedAttempts.value.filter((attempt) => attempt.status === 'SUBMITTED' || attempt.submittedAt))
  const learnerAverageScore = computed(() => {
    const scored = completedLearnerAttempts.value.filter((attempt) => Number.isFinite(Number(attempt.score)) && Number(attempt.maxScore) > 0)
    return scored.length ? scored.reduce((total, attempt) => total + Number(attempt.score) / Number(attempt.maxScore) * 100, 0) / scored.length : null
  })
  const learnerLatestAttempt = computed(() => completedLearnerAttempts.value[0] || null)
  const learnerScoreTrend = computed(() => {
    const trend = parseJson(learnerProgress.value?.scoreTrendJson)
    return Array.isArray(trend) ? trend : []
  })
  const learnerSkillCount = computed(() => Object.keys(parseJson(learnerProgress.value?.masteryBySkillJson)).length)
  const accountSearchQuery = ref('')
  const accountRoleFilter = ref('all')
  const accountPlanFilter = ref('all')
  const accountStatusFilter = ref('all')
  const accountPage = ref(1)
  const accountPageSize = ref(10)

  function resetAccountFilters() {
    accountSearchQuery.value = ''
    accountRoleFilter.value = 'all'
    accountPlanFilter.value = 'all'
    accountStatusFilter.value = 'all'
    accountPage.value = 1
  }

  const totalAccountsCount = computed(() => users.value.length)
  const studentAccountsCount = computed(() => users.value.filter((u) => u.role === 'ROLE_USER').length)
  const activeAccountsCount = computed(() => users.value.filter((u) => u.enabled && u.emailVerified !== false).length)
  const premiumAccountsCount = computed(() => users.value.filter((u) => u.plan === 'PREMIUM').length)

  const filteredUsers = computed(() => {
    const q = (accountSearchQuery.value || query.value).trim().toLowerCase()
    return users.value.filter((item) => {
      if (q) {
        const matchesUsername = item.username?.toLowerCase().includes(q)
        const matchesId = String(item.userId || '').includes(q)
        const matchesRole = item.role?.toLowerCase().includes(q)
        if (!matchesUsername && !matchesId && !matchesRole) return false
      }
      if (accountRoleFilter.value !== 'all' && item.role !== accountRoleFilter.value) {
        return false
      }
      if (accountPlanFilter.value !== 'all') {
        const currentPlan = item.plan || 'FREE'
        if (currentPlan !== accountPlanFilter.value) return false
      }
      if (accountStatusFilter.value === 'active') {
        if (!item.enabled || item.emailVerified === false) return false
      } else if (accountStatusFilter.value === 'disabled') {
        if (item.enabled) return false
      } else if (accountStatusFilter.value === 'unverified') {
        if (item.emailVerified !== false) return false
      }
      return true
    })
  })

  const accountTotalPages = computed(() => Math.max(1, Math.ceil(filteredUsers.value.length / accountPageSize.value)))
  const paginatedUsers = computed(() => {
    const start = (accountPage.value - 1) * accountPageSize.value
    return filteredUsers.value.slice(start, start + accountPageSize.value)
  })

  watch([accountSearchQuery, accountRoleFilter, accountPlanFilter, accountStatusFilter, query], () => {
    accountPage.value = 1
  })
  // Results attempts search and pagination
  const attemptSearch = ref('')
  const attemptStatusFilter = ref('all')
  const attemptPage = ref(1)
  const attemptPageSize = ref(10)
  const filteredLearnerAttempts = computed(() => {
    const q = attemptSearch.value.trim().toLowerCase()
    return learnerScopedAttempts.value.filter((attempt) => {
      const matchesSearch = !q || examTitle(attempt).toLowerCase().includes(q) || String(attempt.attemptId).includes(q)
      const matchesStatus = attemptStatusFilter.value === 'all' || String(attempt.status || '').toUpperCase() === attemptStatusFilter.value
      return matchesSearch && matchesStatus
    })
  })
  const attemptTotalPages = computed(() => Math.max(1, Math.ceil(filteredLearnerAttempts.value.length / attemptPageSize.value)))
  const paginatedLearnerAttempts = computed(() => {
    const start = (attemptPage.value - 1) * attemptPageSize.value
    return filteredLearnerAttempts.value.slice(start, start + attemptPageSize.value)
  })
  watch([attemptSearch, attemptStatusFilter, selectedLearnerId, learnerCertificateId], () => {
    attemptPage.value = 1
  })
  function resetAttemptFilters() {
    attemptSearch.value = ''
    attemptStatusFilter.value = 'all'
    attemptPage.value = 1
  }

  // Programs search and skill parser
  const programSearch = ref('')
  const filteredPrograms = computed(() => {
    const q = (programSearch.value || query.value).trim().toLowerCase()
    return programs.value.filter((item) =>
      !q || `${item.name} ${item.code} ${item.description || ''}`
        .toLowerCase()
        .includes(q)
    )
  })
  function resetProgramFilters() {
    programSearch.value = ''
    query.value = ''
  }
  function parseSkillArray(skills) {
    const labels = {
      LISTENING: 'Nghe',
      READING: 'Đọc',
      WRITING: 'Viết',
      SPEAKING: 'Nói',
      TOAN: 'Toán',
      VAN: 'Ngữ văn',
      ANH: 'Tiếng Anh',
      LY: 'Vật lý',
      HOA: 'Hóa học',
      SINH: 'Sinh học',
      SU: 'Lịch sử',
      DIA: 'Địa lý',
    }
    try {
      const arr = typeof skills === 'string' ? JSON.parse(skills || '[]') : skills
      if (Array.isArray(arr)) {
        return arr.map((item) => ({ code: item, label: labels[item] || item }))
      }
    } catch {
      return []
    }
    return []
  }

  // Exams search, filter, pagination
  const examSearch = ref('')
  const examProgramFilter = ref('all')
  const examStatusFilter = ref('all')
  const examPage = ref(1)
  const examPageSize = ref(10)
  const filteredExams = computed(() => {
    const q = (examSearch.value || query.value).trim().toLowerCase()
    return exams.value.filter((item) => {
      const matchesSearch = !q || `${item.title || ''} ${item.description || ''} ${programName(item.certificateId)}`
        .toLowerCase()
        .includes(q)
      const matchesProgram = examProgramFilter.value === 'all' || String(item.certificateId) === String(examProgramFilter.value)
      const matchesStatus = examStatusFilter.value === 'all'
        || (examStatusFilter.value === 'PUBLISHED' && item.status === 'PUBLISHED')
        || (examStatusFilter.value === 'DRAFT' && item.status !== 'PUBLISHED')
      return matchesSearch && matchesProgram && matchesStatus
    })
  })
  const examTotalPages = computed(() => Math.max(1, Math.ceil(filteredExams.value.length / examPageSize.value)))
  const paginatedExams = computed(() => {
    const start = (examPage.value - 1) * examPageSize.value
    return filteredExams.value.slice(start, start + examPageSize.value)
  })
  watch([examSearch, examProgramFilter, examStatusFilter], () => {
    examPage.value = 1
  })
  function resetExamFilters() {
    examSearch.value = ''
    examProgramFilter.value = 'all'
    examStatusFilter.value = 'all'
    examPage.value = 1
    query.value = ''
  }
  function asList(response) {
    if (Array.isArray(response)) return response
    return response?.content || response?.items || []
  }

  async function loadData() {
    loading.value = true
    pageError.value = ''
    const results = await Promise.allSettled([
      adminService.users(),
      adminService.certificates(),
      adminService.exams(),
      adminService.knowledgeDocuments(),
      adminService.payments(),
      adminService.plans(),
      adminService.userBillings(),
    ])
    if (results[0].status === 'fulfilled')
      users.value = asList(results[0].value)
    if (results[1].status === 'fulfilled')
      programs.value = asList(results[1].value)
    if (results[2].status === 'fulfilled')
      exams.value = asList(results[2].value)
    if (results[3].status === 'fulfilled')
      knowledgeDocuments.value = asList(results[3].value)
    if (results[4].status === 'fulfilled')
      payments.value = asList(results[4].value)
    if (results[5].status === 'fulfilled')
      plans.value = asList(results[5].value)
    if (results[6].status === 'fulfilled')
      userBillings.value = asList(results[6].value)
    if (programs.value.length) {
      const topicResults = await Promise.allSettled(
        programs.value.map((program) =>
          adminService.topics(program.certificateId)
        )
      )
      topics.value = topicResults.flatMap((result) =>
        result.status === 'fulfilled' ? asList(result.value) : []
      )
      if (!knowledgeForm.certificateId) {
        knowledgeForm.certificateId = String(programs.value[0].certificateId)
      }
    }
    if (results.every((result) => result.status === 'rejected'))
      pageError.value = 'Không tải được dữ liệu quản trị. Vui lòng thử lại.'
    loading.value = false
    if (activeSection.value === 'results' && !selectedLearnerId.value && students.value.length) {
      selectedLearnerId.value = String(students.value[0].userId)
      loadLearnerReport()
    }
    if (activeSection.value === 'overview') loadAnalytics()
  }

  async function loadAnalytics() {
    const requestId = ++analyticsRequestSequence
    showAnalyticsSample.value = false
    analyticsLoading.value = true
    analyticsError.value = ''
    const filters = { period: analyticsPeriod.value }
    if (analyticsCertificateId.value) filters.certificateId = Number(analyticsCertificateId.value)
    try {
      const report = await adminService.analytics(filters)
      if (requestId === analyticsRequestSequence) analyticsData.value = report
    } catch (error) {
      if (requestId !== analyticsRequestSequence) return
      analyticsData.value = null
      analyticsError.value = error?.response?.data?.message || 'Không tải được số liệu thống kê. Vui lòng thử lại.'
    } finally {
      if (requestId === analyticsRequestSequence) analyticsLoading.value = false
    }
  }

  function openSection(section) {
    const sec = resolveSectionId(section)
    activeSection.value = sec
    query.value = ''
    mobileMenuOpen.value = false
    router.push({
      path: sec === 'overview' ? '/admin' : '/admin/' + sec
    })
    if (sec === 'results' && !selectedLearnerId.value && students.value.length) {
      selectedLearnerId.value = String(students.value[0].userId)
      loadLearnerReport()
    }
  }

  async function loadLearnerReport() {
    if (!selectedLearnerId.value) return
    const requestId = ++learnerLoadSequence
    const userId = selectedLearnerId.value
    learnerLoading.value = true
    learnerError.value = ''
    try {
      const attempts = asList(await adminService.userAttempts(userId))
      if (!learnerCertificateId.value && programs.value[0]) learnerCertificateId.value = String(programs.value[0].certificateId)
      const certificateId = learnerCertificateId.value
      const progress = certificateId
        ? await adminService.userProgress(userId, certificateId)
        : null
      if (requestId !== learnerLoadSequence) return
      learnerAttempts.value = attempts
      learnerProgress.value = progress
    } catch (error) {
      if (requestId !== learnerLoadSequence) return
      learnerError.value = error?.response?.data?.message || 'Không tải được kết quả học tập của tài khoản này.'
    } finally {
      if (requestId === learnerLoadSequence) learnerLoading.value = false
    }
  }
  function openLearnerReport(user) {
    selectedLearnerId.value = String(user.userId)
    learnerCertificateId.value = ''
    openSection('results')
    loadLearnerReport()
  }

  watch(
    () => route.query.section,
    (section) => {
      activeSection.value = resolveSectionId(section)
    }
  )


  watch([analyticsPeriod, analyticsCertificateId], () => {
    showAnalyticsSample.value = false
    if (activeSection.value === 'overview') loadAnalytics()
  })
  watch(activeSection, (section) => {
    if (section === 'overview') loadAnalytics()
    else showAnalyticsSample.value = false
  })

  watch(() => knowledgeEditForm.certificateId, (certificateId) => {
    if (knowledgeEditForm.topicId && !knowledgeEditTopics.value.some(
      (topic) => String(topic.topicId) === knowledgeEditForm.topicId
    )) knowledgeEditForm.topicId = ''
  })

  watch(
    () => knowledgeForm.certificateId,
    () => {
      if (
        knowledgeForm.topicId &&
        !knowledgeTopics.value.some((topic) => String(topic.topicId) === knowledgeForm.topicId)
      ) {
        knowledgeForm.topicId = ''
      }
    }
  )

  function openAccount() {
    adminModalError.value = ''
    Object.assign(accountForm, {
      username: '',
      password: '',
      role: 'ROLE_USER',
      canApproveExams: false,
    })
    modal.value = 'account'
    adminModalBaseline.value = JSON.stringify(accountForm)
  }

  function openProgram(program = null) {
    adminModalError.value = ''
    editingCertificateId.value = program?.certificateId ?? null
    Object.assign(
      programForm,
      program
        ? {
            name: program.name,
            code: program.code,
            description: program.description || '',
            totalScore: program.totalScore,
            passingScore: program.passingScore,
            passingBand: program.passingBand || 0,
            skills: program.skills || '[]',
          }
        : {
            name: '',
            code: '',
            description: '',
            totalScore: 100,
            passingScore: 50,
            passingBand: 0,
            skills: '[]',
          }
    )
    modal.value = 'program'
    adminModalBaseline.value = JSON.stringify(programForm)
  }

  function openPreset(preset) {
    openProgram()
    Object.assign(programForm, preset)
    adminModalBaseline.value = JSON.stringify(programForm)
  }

  function requestAdminModalClose() {
    adminModalRef.value?.requestClose()
  }

  function onAdminModalOpenChange(open) {
    if (!open) {
      modal.value = ''
      adminModalBaseline.value = ''
    }
  }

  function topicName(topicId) {
    if (topicId == null) return '—'
    return topics.value.find((topic) => String(topic.topicId) === String(topicId))?.name || 'Kỹ năng'
  }

  function programName(certificateId) {
    return (
      programs.value.find(
        (item) => String(item.certificateId) === String(certificateId)
      )?.name || 'Chưa gắn kỳ thi'
    )
  }

  function knowledgeTopicName(topicId) {
    if (topicId == null) return ''
    return topics.value.find((topic) => String(topic.topicId) === String(topicId))?.name || ''
  }

  function knowledgeSkillTone(topic) {
    const name = (typeof topic === 'string' ? topic : topic?.name || '').toLocaleLowerCase('vi')
    if (name.includes('listening') || name.includes('nghe')) return 'skill-blue'
    if (name.includes('reading') || name.includes('đọc')) return 'skill-teal'
    if (name.includes('writing') || name.includes('viết')) return 'skill-violet'
    if (name.includes('speaking') || name.includes('nói')) return 'skill-pink'
    const index = knowledgeTopics.value.findIndex((item) => item.name === (typeof topic === 'string' ? topic : topic?.name))
    return ['skill-blue', 'skill-teal', 'skill-violet', 'skill-pink', 'skill-amber'][Math.max(0, index) % 5]
  }

  function skillLabels(skills) {
    const labels = {
      LISTENING: 'Nghe',
      READING: 'Đọc',
      WRITING: 'Viết',
      SPEAKING: 'Nói',
      TOAN: 'Toán',
      VAN: 'Ngữ văn',
      ANH: 'Tiếng Anh',
      LY: 'Vật lý',
      HOA: 'Hóa học',
      SINH: 'Sinh học',
      SU: 'Lịch sử',
      DIA: 'Địa lý',
    }
    try {
      const values = JSON.parse(skills || '[]')
      if (Array.isArray(values))
        return values.map((value) => labels[value] || value).join(', ')
    } catch {
      return skills || 'Chưa cấu hình'
    }
    return 'Chưa cấu hình'
  }

  async function saveAccount() {
    saving.value = true
    adminModalError.value = ''
    try {
      await adminService.createUser({
        username: accountForm.username.trim(),
        password: accountForm.password,
        role: accountForm.role,
        canApproveExams: accountForm.canApproveExams,
      })
      modal.value = ''
      adminModalBaseline.value = ''
      await loadData()
      toast.success('Đã tạo tài khoản', { description: accountForm.username.trim() })
    } catch (error) {
      adminModalError.value = error?.response?.data?.responseMessage || 'Không thể tạo tài khoản.'
    } finally {
      saving.value = false
    }
  }

  async function saveProgram() {
    saving.value = true
    adminModalError.value = ''
    const wasEditing = Boolean(editingCertificateId.value)
    const savedName = programForm.name.trim()
    try {
      const payload = {
        ...programForm,
        name: programForm.name.trim(),
        code: programForm.code.trim(),
        totalScore: Number(programForm.totalScore),
        passingScore: Number(programForm.passingScore),
        passingBand: Number(programForm.passingBand),
      }
      if (editingCertificateId.value) {
        await adminService.updateCertificate(
          editingCertificateId.value,
          payload
        )
      } else {
        await adminService.createCertificate(payload)
      }
      modal.value = ''
      adminModalBaseline.value = ''
      await loadData()
      toast.success(wasEditing ? 'Đã cập nhật kỳ thi' : 'Đã tạo kỳ thi', { description: savedName })
      toast.success(editingCertificateId.value ? 'Đã cập nhật kỳ thi' : 'Đã tạo kỳ thi', { description: programForm.name.trim() })
    } catch (error) {
      adminModalError.value = error?.response?.data?.responseMessage ||
        'Không thể lưu chứng chỉ / kỳ thi.'
    } finally {
      saving.value = false
    }
  }

  function readKnowledgeFiles(event) {
    addKnowledgeFiles(Array.from(event.target.files || []))
    event.target.value = ''
  }

  function addKnowledgeFiles(files) {
    const known = new Set(
      knowledgeFiles.value.map((item) => `${item.file.name}:${item.file.size}:${item.file.lastModified}`)
    )
    let invalidCount = 0
    for (const file of files) {
      const key = `${file.name}:${file.size}:${file.lastModified}`
      if (!file.name.toLowerCase().endsWith('.pdf') || !file.size || file.size > 20 * 1024 * 1024) {
        invalidCount += 1
        continue
      }
      if (known.has(key)) continue
      known.add(key)
      knowledgeFiles.value.push({
        id: key,
        file,
        title: file.name.replace(/\.pdf$/i, ''),
      })
    }
    knowledgeNotice.value = ''
    knowledgeFormErrors.files = invalidCount
      ? 'Chỉ nhận file PDF tối đa 20 MB mỗi file.'
      : ''
    if (knowledgeFiles.value.length && !invalidCount) knowledgeFormErrors.files = ''
  }

  function dropKnowledgeFiles(event) {
    event.preventDefault()
    knowledgeDragging.value = false
    addKnowledgeFiles(Array.from(event.dataTransfer?.files || []))
  }

  function knowledgeDate(value) {
    if (!value) return 'Mới tải lên'
    const date = new Date(value)
    return Number.isNaN(date.getTime())
      ? 'Mới tải lên'
      : date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }

  function removeKnowledgeFile(id) {
    knowledgeFiles.value = knowledgeFiles.value.filter((item) => item.id !== id)
    knowledgeNotice.value = ''
  }

  function clearKnowledgeForm() {
    knowledgeFiles.value = []
    knowledgeForm.topicId = ''
    knowledgeForm.certificateId = programs.value[0]
      ? String(programs.value[0].certificateId)
      : ''
    knowledgeNotice.value = ''
    pageError.value = ''
    Object.assign(knowledgeFormErrors, { files: '', exam: '', titles: '' })
  }

  function closeKnowledgeNotice() {
    knowledgeNotice.value = ''
  }

  async function importKnowledge() {
    knowledgeFormErrors.files = knowledgeFiles.value.length
      ? ''
      : 'Chọn ít nhất một tệp PDF.'
    knowledgeFormErrors.exam = knowledgeForm.certificateId
      ? ''
      : 'Chọn kỳ thi cho tài liệu.'
    knowledgeFormErrors.titles = knowledgeFiles.value.some((item) => !item.title.trim())
      ? 'Nhập tiêu đề cho từng tài liệu.'
      : ''
    if (knowledgeFormErrors.files || knowledgeFormErrors.exam || knowledgeFormErrors.titles) return

    importingKnowledge.value = true
    pageError.value = ''
    let importedCount = 0
    let uploadError = ''
    try {
      for (const item of [...knowledgeFiles.value]) {
        const formData = new FormData()
        formData.append('title', item.title.trim())
        formData.append('certificateId', knowledgeForm.certificateId)
        if (knowledgeForm.topicId) formData.append('topicId', knowledgeForm.topicId)
        formData.append('file', item.file)
        try {
          await adminService.uploadKnowledgePdf(formData)
          importedCount += 1
          removeKnowledgeFile(item.id)
        } catch (error) {
          uploadError =
            `Tải ${item.file.name} thất bại: ` +
            (error?.response?.data?.message ||
              error?.response?.data?.responseMessage ||
              'Không thể thêm tài liệu vào kho kiến thức.')
          break
        }
      }
      if (importedCount) await loadData()
      if (uploadError) pageError.value = uploadError
      else if (importedCount) {
        knowledgeFilter.value = 'all'
        knowledgeNotice.value = `${formatAdminNumber(importedCount)} PDF`
      }
    } finally {
      importingKnowledge.value = false
    }
  }

  async function removeKnowledge(document) {
    const shouldDelete = await confirm({
      title: 'Xóa tài liệu?',
      description: `“${document.title}” và ${formatAdminNumber(document.chunkCount)} đoạn kiến thức sẽ bị xóa khỏi kho.`,
      confirmText: 'Xóa tài liệu',
      cancelText: 'Giữ lại',
      variant: 'danger',
    })
    if (!shouldDelete)
      return
    try {
      await adminService.deleteKnowledgeDocument(document.documentId)
      await loadData()
      toast.success('Đã xóa tài liệu', { description: document.title })
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể xóa tài liệu.'
    }
  }

  async function deleteProgram(program) {
    const shouldDelete = await confirm({
      title: 'Xóa kỳ thi?',
      description: `Bạn có chắc muốn xóa “${program.name}”? Các nội dung phụ thuộc kỳ thi có thể không còn dùng được.`,
      confirmText: 'Xóa kỳ thi',
      cancelText: 'Giữ lại',
      variant: 'danger',
    })
    if (!shouldDelete) return
    try {
      await adminService.deleteCertificate(program.certificateId)
      await loadData()
      toast.success('Đã xóa kỳ thi', { description: program.name })
    } catch (error) {
      pageError.value = error?.response?.data?.responseMessage || 'Không thể xóa chương trình.'
    }
  }

  async function toggleKnowledgeActive(document) {
    const previous = document.active
    document.active = !previous
    pageError.value = ''
    try {
      await adminService.setKnowledgeDocumentActive(
        document.documentId,
        document.active
      )
      if (knowledgeDetail.value?.documentId === document.documentId) {
        knowledgeDetail.value.active = document.active
      }
      await loadData()
    } catch (error) {
      document.active = previous
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật trạng thái tài liệu.'
    }
  }

  async function loadKnowledgeDocumentDetail(document) {
    const requestId = ++knowledgePreviewRequest.value
    loadingKnowledgeDetail.value = true
    try {
      const detail = await adminService.knowledgeDocument(document.documentId)
      if (requestId !== knowledgePreviewRequest.value) return null
      return detail
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể tải nội dung tài liệu.'
      return null
    } finally {
      if (requestId === knowledgePreviewRequest.value) loadingKnowledgeDetail.value = false
    }
  }

  async function viewKnowledgeDocument(document) {
    knowledgeDetail.value = null
    const detail = await loadKnowledgeDocumentDetail(document)
    if (detail) knowledgeDetail.value = detail
  }

  async function closeKnowledgePreview() {
    knowledgePreviewRequest.value += 1
    loadingKnowledgeDetail.value = false
    knowledgeDetail.value = null
  }

  async function editKnowledgeDocument(document = knowledgeDetail.value) {
    if (!document || loadingKnowledgeDetail.value || savingKnowledgeEdit.value) return
    let detail = document
    if (!Object.prototype.hasOwnProperty.call(document, 'content')) {
      try {
        detail = await adminService.knowledgeDocument(document.documentId)
      } catch (error) {
        pageError.value = error?.response?.data?.responseMessage
          || error?.response?.data?.message
          || 'Không thể tải tài liệu để sửa.'
        return
      }
    }
    if (!detail) return
    Object.assign(knowledgeEditForm, {
      title: detail.title,
      certificateId: String(detail.certificateId || ''),
      topicId: detail.topicId == null ? '' : String(detail.topicId),
      content: detail.content || '',
    })
    editingKnowledgeDocumentId.value = detail.documentId
    knowledgeEditBaseline.value = JSON.stringify(knowledgeEditForm)
    knowledgeDetail.value = null
    knowledgeEditorOpen.value = true
  }

  function onKnowledgeEditorOpenChange(open) {
    knowledgeEditorOpen.value = open
    if (!open) {
      editingKnowledgeDocumentId.value = null
      knowledgeEditBaseline.value = ''
    }
  }

  function requestKnowledgeEditorClose() {
    knowledgeEditorRef.value?.requestClose()
  }

  async function closeKnowledgeEditor() {
    knowledgeEditorOpen.value = false
    editingKnowledgeDocumentId.value = null
    knowledgeEditBaseline.value = ''
  }

  async function saveKnowledgeEdit() {
    if (!editingKnowledgeDocumentId.value || savingKnowledgeEdit.value) return
    savingKnowledgeEdit.value = true
    try {
      await adminService.updateKnowledgeDocument(editingKnowledgeDocumentId.value, {
        title: knowledgeEditForm.title.trim(),
        certificateId: Number(knowledgeEditForm.certificateId),
        topicId: knowledgeEditForm.topicId ? Number(knowledgeEditForm.topicId) : null,
        content: knowledgeEditForm.content,
      })
      await loadData()
      await closeKnowledgeEditor()
      toast.success('Đã lưu thay đổi', { description: knowledgeEditForm.title.trim() })
    } catch (error) {
      pageError.value = error?.response?.data?.responseMessage
        || error?.response?.data?.message
        || 'Không thể cập nhật tài liệu.'
    } finally {
      savingKnowledgeEdit.value = false
    }
  }

  async function updateRole(user, event) {
    const oldRole = user.role
    const newRole = event.target.value
    if (oldRole === newRole) return
    const confirmed = await confirm({
      title: 'Xác nhận thay đổi vai trò',
      message: `Bạn có chắc muốn cấp vai trò "${newRole === 'ROLE_ADMIN' ? 'Quản trị viên' : newRole === 'ROLE_TEACHER' ? 'Giáo viên' : 'Người học'}" cho tài khoản "${user.username}"?`,
      confirmLabel: 'Xác nhận',
      cancelLabel: 'Hủy',
    })
    if (!confirmed) {
      event.target.value = oldRole
      return
    }
    user.role = newRole
    try {
      await adminService.updateUserRole(user.userId, newRole)
      toast.success(`Đã cập nhật vai trò thành công cho "${user.username}"`)
      await loadData()
    } catch (error) {
      user.role = oldRole
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật quyền tài khoản.'
      toast.error(pageError.value)
    }
  }

  async function updateExamApprovalPermission(user, event) {
    const allowed = event.target.checked
    try {
      await adminService.updateExamApprovalPermission(user.userId, allowed)
      user.canApproveExams = allowed
      toast.success(allowed ? 'Đã cấp quyền duyệt đề' : 'Đã thu hồi quyền duyệt đề', { description: user.username })
    } catch (error) {
      event.target.checked = !allowed
      pageError.value = error?.response?.data?.responseMessage || 'Không thể cập nhật quyền duyệt đề.'
      toast.error(pageError.value)
    }
  }

  async function updateStatus(user) {
    const nextEnabled = !user.enabled
    if (!nextEnabled) {
      const confirmed = await confirm({
        title: 'Xác nhận khóa tài khoản',
        message: `Bạn có chắc muốn khóa tài khoản "${user.username}"? Người dùng này sẽ không thể đăng nhập hoặc làm bài thi.`,
        confirmLabel: 'Khóa tài khoản',
        cancelLabel: 'Hủy',
      })
      if (!confirmed) return
    }
    user.enabled = nextEnabled
    try {
      await adminService.updateUserStatus(user.userId, nextEnabled)
      toast.success(nextEnabled ? `Đã mở khóa tài khoản "${user.username}"` : `Đã khóa tài khoản "${user.username}"`)
      await loadData()
    } catch (error) {
      user.enabled = !nextEnabled
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật trạng thái tài khoản.'
      toast.error(pageError.value)
    }
  }

  async function updateUserPlan(user, newPlan) {
    const oldPlan = user.plan || 'FREE'
    if (oldPlan === newPlan) return
    const confirmed = await confirm({
      title: 'Xác nhận thay đổi gói cước',
      message: `Bạn có chắc muốn chuyển tài khoản "${user.username}" sang gói ${newPlan === 'PREMIUM' ? 'Cao cấp (Premium)' : 'Miễn phí (Free)'}?`,
      confirmLabel: 'Đồng ý',
      cancelLabel: 'Hủy',
    })
    if (!confirmed) return
    user.plan = newPlan
    try {
      await adminService.updateUserPlan(user.userId, newPlan)
      toast.success(`Đã cập nhật gói cước thành công cho "${user.username}"`)
      await loadData()
    } catch (error) {
      user.plan = oldPlan
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật gói cước tài khoản.'
      toast.error(pageError.value)
    }
  }

  const totalRevenue = computed(() => {
    return payments.value
      .filter((p) => p.status === 'SUCCEEDED')
      .reduce((sum, p) => sum + (Number(p.amount) || 0), 0)
  })

  const succeededPaymentsCount = computed(() => {
    return payments.value.filter((p) => p.status === 'SUCCEEDED').length
  })

  const pendingPaymentsCount = computed(() => {
    return payments.value.filter((p) => p.status === 'PENDING').length
  })

  const premiumUsersCount = computed(() => {
    return users.value.filter((u) => u.plan === 'PREMIUM').length
  })

  const freeUsersCount = computed(() => {
    return users.value.filter((u) => u.plan !== 'PREMIUM').length
  })

  const filteredPayments = computed(() => {
    return payments.value.filter((p) => {
      const q = paymentSearch.value.trim().toLowerCase()
      const matchesSearch =
        !q ||
        String(p.paymentId).includes(q) ||
        (p.username && p.username.toLowerCase().includes(q)) ||
        (p.providerPaymentId && p.providerPaymentId.toLowerCase().includes(q)) ||
        (p.providerTransactionId && p.providerTransactionId.toLowerCase().includes(q))
      const matchesStatus =
        paymentStatusFilter.value === 'all' ||
        p.status === paymentStatusFilter.value
      const matchesProvider =
        paymentProviderFilter.value === 'all' ||
        p.provider === paymentProviderFilter.value
      return matchesSearch && matchesStatus && matchesProvider
    })
  })

  function formatPaymentDate(dateString) {
    if (!dateString) return '—'
    try {
      const d = new Date(dateString)
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(d)
    } catch {
      return dateString
    }
  }

  function formatMoney(amount, currency = 'USD') {
    if (amount == null) return '$0.00'
    return `$${Number(amount).toFixed(2)} ${currency}`
  }

  function parseFeatures(jsonOrString) {
    if (!jsonOrString) return []
    try {
      const parsed = typeof jsonOrString === 'string' ? JSON.parse(jsonOrString) : jsonOrString
      return Array.isArray(parsed) ? parsed : [String(parsed)]
    } catch {
      return [String(jsonOrString)]
    }
  }

  function openPlanModal(plan = null) {
    editingPlanId.value = plan ? plan.planId : null
    planModalError.value = ''
    if (plan) {
      const features = parseFeatures(plan.featuresJson)
      Object.assign(planForm, {
        code: plan.code,
        name: plan.name,
        description: plan.description || '',
        price: Number(plan.price) || 0,
        currency: plan.currency || 'USD',
        billingCycle: plan.billingCycle || 'LIFETIME',
        examLimit: plan.examLimit ?? -1,
        featuresText: features.join('\n'),
        isActive: plan.isActive !== false,
      })
    } else {
      Object.assign(planForm, {
        code: '',
        name: '',
        description: '',
        price: 0,
        currency: 'USD',
        billingCycle: 'LIFETIME',
        examLimit: -1,
        featuresText: '',
        isActive: true,
      })
    }
    planModalOpen.value = true
  }

  async function savePlan() {
    if (!planForm.code.trim()) {
      planModalError.value = 'Vui lòng nhập Mã gói cước (ví dụ: PRO_MONTHLY).'
      return
    }
    if (!planForm.name.trim()) {
      planModalError.value = 'Vui lòng nhập Tên gói cước.'
      return
    }
    if (planForm.price < 0 || isNaN(Number(planForm.price))) {
      planModalError.value = 'Giá gói cước không hợp lệ (phải từ 0 trở lên).'
      return
    }
    savingPlan.value = true
    planModalError.value = ''
    const featuresList = planForm.featuresText
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
    const payload = {
      code: planForm.code.trim().toUpperCase(),
      name: planForm.name.trim(),
      description: planForm.description.trim(),
      price: Number(planForm.price) || 0,
      currency: planForm.currency || 'USD',
      billingCycle: planForm.billingCycle || 'LIFETIME',
      examLimit: Number(planForm.examLimit) ?? -1,
      featuresJson: JSON.stringify(featuresList),
      isActive: planForm.isActive,
    }
    try {
      if (editingPlanId.value) {
        await adminService.updatePlan(editingPlanId.value, payload)
        toast.success(`Đã cập nhật gói "${planForm.name}" thành công`)
      } else {
        await adminService.createPlan(payload)
        toast.success(`Đã tạo gói cước mới "${planForm.name}"`)
      }
      planModalOpen.value = false
      await loadData()
    } catch (error) {
      planModalError.value =
        error?.response?.data?.responseMessage ||
        'Không thể lưu thông tin gói cước.'
    } finally {
      savingPlan.value = false
    }
  }

  async function togglePlanStatus(plan) {
    if (plan.isActive) {
      const confirmed = await confirm({
        title: 'Xác nhận tạm ẩn gói cước',
        message: `Bạn có chắc muốn tạm ẩn gói "${plan.name}"? Học viên sẽ không còn thấy gói này trên trang Bảng giá.`,
        confirmLabel: 'Tạm ẩn gói',
        cancelLabel: 'Hủy',
      })
      if (!confirmed) return
    }
    try {
      await adminService.togglePlan(plan.planId)
      plan.isActive = !plan.isActive
      toast.success(
        plan.isActive
          ? `Đã mở bán lại gói "${plan.name}"`
          : `Đã tạm ẩn gói "${plan.name}"`
      )
    } catch (error) {
      toast.error('Không thể cập nhật trạng thái gói.')
    }
  }

  async function deletePlan(plan) {
    const confirmed = await confirm({
      title: 'Xóa gói cước',
      message: `Bạn có chắc muốn xóa gói cước "${plan.name}"? Hành động này không thể hoàn tác.`,
      confirmLabel: 'Xóa gói',
      cancelLabel: 'Hủy',
    })
    if (!confirmed) return
    try {
      await adminService.deletePlan(plan.planId)
      toast.success(`Đã xóa gói "${plan.name}"`)
      await loadData()
    } catch (error) {
      toast.error(error?.response?.data?.responseMessage || 'Không thể xóa gói cước này.')
    }
  }

  const filteredUserBillings = computed(() => {
    return userBillings.value.filter((u) => {
      const q = userBillingSearch.value.trim().toLowerCase()
      const matchesSearch =
        !q ||
        String(u.userId).includes(q) ||
        (u.username && u.username.toLowerCase().includes(q))
      const matchesPlan =
        userBillingPlanFilter.value === 'all' ||
        (userBillingPlanFilter.value === 'PREMIUM' && u.plan === 'PREMIUM') ||
        (userBillingPlanFilter.value === 'FREE' && u.plan !== 'PREMIUM')
      return matchesSearch && matchesPlan
    })
  })
  const userBillingPage = ref(1)
  const userBillingPageSize = ref(10)
  const userBillingTotalPages = computed(() => Math.max(1, Math.ceil(filteredUserBillings.value.length / userBillingPageSize.value)))
  const paginatedUserBillings = computed(() => {
    const start = (userBillingPage.value - 1) * userBillingPageSize.value
    return filteredUserBillings.value.slice(start, start + userBillingPageSize.value)
  })

  const paymentPage = ref(1)
  const paymentPageSize = ref(10)
  const paymentTotalPages = computed(() => Math.max(1, Math.ceil(filteredPayments.value.length / paymentPageSize.value)))
  const paginatedPayments = computed(() => {
    const start = (paymentPage.value - 1) * paymentPageSize.value
    return filteredPayments.value.slice(start, start + paymentPageSize.value)
  })

  watch([userBillingSearch, userBillingPlanFilter], () => {
    userBillingPage.value = 1
  })
  watch([paymentSearch, paymentStatusFilter, paymentProviderFilter], () => {
    paymentPage.value = 1
  })

  async function viewUserPayments(userBilling) {
    selectedUserForPayments.value = userBilling
    userPaymentsDrawerOpen.value = true
    userPaymentsLoading.value = true
    try {
      userPaymentsList.value = await adminService.userPayments(userBilling.userId)
    } catch (error) {
      userPaymentsList.value = []
      toast.error('Không thể tải lịch sử thanh toán của học viên này.')
    } finally {
      userPaymentsLoading.value = false
    }
  }

  async function resetUserQuota(userBilling) {
    const confirmed = await confirm({
      title: 'Reset lượt làm bài thi',
      message: `Đặt lại số lượt thi miễn phí đã dùng về 0 cho "${userBilling.username}"?`,
      confirmLabel: 'Reset ngay',
      cancelLabel: 'Hủy',
    })
    if (!confirmed) return
    try {
      await adminService.resetUserQuota(userBilling.userId)
      toast.success(`Đã reset số lượt thi về 0 cho "${userBilling.username}"`)
      await loadData()
    } catch (error) {
      toast.error('Không thể reset lượt thi cho tài khoản này.')
    }
  }

  const certificatePresets = [
    {
      name: 'IELTS Academic',
      code: 'IELTS',
      description: 'Luyện thi IELTS Academic theo 4 kỹ năng.',
      totalScore: 9,
      passingScore: 6,
      passingBand: 6.5,
      skills: '["LISTENING","READING","WRITING","SPEAKING"]',
    },
    {
      name: 'TOEIC',
      code: 'TOEIC',
      description: 'Luyện thi TOEIC Listening & Reading.',
      totalScore: 990,
      passingScore: 700,
      passingBand: 0,
      skills: '["LISTENING","READING"]',
    },
    {
      name: 'THPT Quốc Gia',
      code: 'THPT',
      description: 'Ôn thi tốt nghiệp THPT theo môn và mục tiêu điểm.',
      totalScore: 30,
      passingScore: 21,
      passingBand: 0,
      skills: '["TOAN","VAN","ANH","LY","HOA","SINH","SU","DIA"]',
    },
  ]

  // Sync activeSection with route
  watch(
    () => route.path,
    (newPath) => {
      const match = newPath.match(/\/admin\/([a-z-]+)/)
      if (match && match[1]) {
        const mapped = resolveSectionId(match[1])
        if (mapped !== activeSection.value) {
          activeSection.value = mapped
        }
      } else if (newPath === '/admin' && route.query.section) {
        activeSection.value = resolveSectionId(route.query.section)
      } else if (newPath === '/admin') {
        activeSection.value = 'overview'
      }
    },
    { immediate: true }
  )

  const adminContext = {
    // User accounts counts
    studentAccountsCount,
    activeAccountsCount,
    premiumAccountsCount,
    activeUsers,

    // Billing metrics
    totalRevenue,
    succeededPaymentsCount,
    pendingPaymentsCount,
    premiumUsersCount,
    freeUsersCount,

    // Programs presets
    certificatePresets,
    openPreset,

    // Knowledge additions
    activeKnowledgePercent,
    knowledgeForm,
    knowledgeFormErrors,
    knowledgeTopics,
    importingKnowledge,

    // Results / Learner Report additions
    students,
    selectedLearner,
    selectedLearnerId,
    learnerCertificateId,
    loadLearnerReport,
    learnerLoading,
    learnerError,
    learnerAttempts,
    learnerProgress,
    learnerScopedAttempts,
    completedLearnerAttempts,
    learnerAverageScore,
    learnerLatestAttempt,
    learnerScoreTrend,
    learnerSkillCount,
    filteredLearnerAttempts,
    paginatedLearnerAttempts,
    resetAttemptFilters,

    // Analytics Chart
    analyticsTrend,
    analyticsChartPoints,
    analyticsScorePath,
    analyticsMaxAttempts,

    totalAccountsCount,
    accountSearchQuery,
    accountPlanFilter,
    topics,
    knowledgeChunkCount,
    analyticsInsight,
    displayAnalyticsData,
    formatAttemptDate,

    activeSection,
    analyticsPeriod,
    analyticsPeriods,
    analyticsCertificateId,
    analyticsLoading,
    analyticsError,
    analyticsData,
    programs,
    loadAnalytics,
    openSection,
    openAccount,
    openProgram,
    formatAdminNumber,
    formatPercent,
    formatSignedPoint,
    formatTrendDate,
    formatMoney,
    openLearnerReport,
    loading,
    users,
    query,
    accountRoleFilter,
    accountStatusFilter,
    accountPage,
    accountPageSize,
    filteredUsers,
    accountTotalPages,
    paginatedUsers,
    resetAccountFilters,
    updateRole,
    updateExamApprovalPermission,
    updateStatus,
    updateUserPlan,
    plans,
    billingSubTab,
    openPlanModal,
    togglePlanStatus,
    deletePlan,
    parseFeatures,
    userBillings,
    userBillingSearch,
    userBillingPlanFilter,
    userBillingPage,
    userBillingPageSize,
    filteredUserBillings,
    paginatedUserBillings,
    userBillingTotalPages,
    viewUserPayments,
    resetUserQuota,
    payments,
    loadingPayments,
    paymentSearch,
    paymentStatusFilter,
    paymentProviderFilter,
    paymentPage,
    paymentPageSize,
    filteredPayments,
    paymentTotalPages,
    paginatedPayments,
    formatPaymentDate,
    loadData,
    attemptSearch,
    attemptStatusFilter,
    attemptPage,
    attemptPageSize,
    attemptTotalPages,
    resetAttemptFilters,
    viewAttemptResult,
    examTitle,
    formatAttemptScore,
    formatPercent,
    programSearch,
    filteredPrograms,
    resetProgramFilters,
    deleteProgram,
    parseSkillArray,
    exams,
    examSearch,
    examProgramFilter,
    examStatusFilter,
    examPage,
    examPageSize,
    filteredExams,
    examTotalPages,
    paginatedExams,
    resetExamFilters,
    showExamBuilder,
    programName,
    knowledgeDocuments,
    knowledgeFiles,
    knowledgeSearch,
    knowledgeFilter,
    knowledgeProgramFilter,
    knowledgePage,
    knowledgePageSize,
    knowledgeDragging,
    knowledgeNotice,
    activeKnowledgeCount,
    inactiveKnowledgeCount,
    filteredKnowledgeDocuments,
    knowledgeTotalPages,
    paginatedKnowledgeDocuments,
    readKnowledgeFiles,
    dropKnowledgeFiles,
    removeKnowledgeFile,
    clearKnowledgeForm,
    importKnowledge,
    closeKnowledgeNotice,
    resetKnowledgeFilters,
    knowledgeTopicName,
    knowledgeDate,
    toggleKnowledgeActive,
    viewKnowledgeDocument,
    editKnowledgeDocument,
    removeKnowledge
  }

  provide(adminContextKey, adminContext)

  function logout() {
    userStore.logout()
    router.push('/login')
  }

  onMounted(loadData)
</script>


<template>
  <main class="admin-shell">
    <aside
      class="admin-sidebar"
      :class="{ 'admin-sidebar-open': mobileMenuOpen }"
    >
      <div class="admin-sidebar-header">
        <a class="admin-brand" href="/admin" @click.prevent="openSection('overview')">
          <span class="admin-brand-mark"><Shield :size="20" /></span>
          <span class="admin-brand-copy">
            <strong>Adaptive <em>Learning</em></strong>
            <small>Cổng Quản Trị Hệ Thống</small>
          </span>
        </a>
        <button
          v-if="mobileMenuOpen"
          type="button"
          class="admin-sidebar-close"
          aria-label="Đóng menu điều hướng"
          @click="mobileMenuOpen = false"
        >
          <X :size="18" />
        </button>
      </div>

      <nav class="admin-nav" aria-label="Điều hướng quản trị">
        <div v-for="grp in navGroups" :key="grp.title" class="admin-nav-group">
          <div class="admin-workspace-label">{{ grp.title }}</div>
          <button
            v-for="item in grp.items"
            :key="item.id"
            :class="['admin-nav-link', { active: activeSection === item.id }]"
            :aria-current="activeSection === item.id ? 'page' : undefined"
            @click="openSection(item.id)"
          >
            <span class="admin-nav-icon" :class="`nav-icon-${item.id}`">
              <component :is="item.icon" :size="17" aria-hidden="true" />
            </span>
            <span class="admin-nav-label">{{ item.label }}</span>
            <span v-if="activeSection === item.id" class="admin-nav-indicator" aria-hidden="true"></span>
          </button>
        </div>
      </nav>

      <div class="admin-sidebar-bottom">
        <div class="admin-status-indicator">
          <span class="admin-status-dot"></span>
          <div>
            <strong>Hệ thống sẵn sàng</strong>
            <small>Quyền Quản trị viên</small>
          </div>
        </div>
        <RouterLink to="/home" class="admin-switch-learner-btn" title="Chuyển sang giao diện người học">
          <Eye :size="14" />
          <span>Giao diện Học viên</span>
        </RouterLink>
      </div>
    </aside>

    <div
      v-if="mobileMenuOpen"
      class="admin-scrim"
      aria-hidden="true"
      @click="mobileMenuOpen = false"
    ></div>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <div class="admin-topbar-left">
          <button
            class="admin-menu-button"
            :aria-label="mobileMenuOpen ? 'Đóng menu' : 'Mở menu'"
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <Menu :size="19" />
          </button>

          <div class="admin-breadcrumb">
            <span class="breadcrumb-badge">Quản trị</span>
            <ChevronRight :size="14" class="breadcrumb-arrow" />
            <span class="breadcrumb-page">{{ currentTitle }}</span>
          </div>
        </div>

        <label class="admin-search">
          <Search :size="16" />
          <input
            v-model="query"
            type="search"
            placeholder="Tìm tài khoản, chương trình, bài kiểm tra..."
            aria-label="Tìm kiếm quản trị"
          />
        </label>

        <div class="admin-top-actions">
          <RouterLink to="/home" class="admin-preview-learner-link" title="Xem giao diện học viên">
            <Eye :size="14" />
            <span>Cổng học viên</span>
          </RouterLink>

          <Tooltip label="Thông báo mới">
            <template #trigger>
              <button class="admin-notification" aria-label="Thông báo">
                <Bell :size="17" />
                <i></i>
              </button>
            </template>
          </Tooltip>
          <DropdownMenu v-model:open="adminProfileOpen" align="end" :side-offset="8">
            <template #trigger>
              <button class="profile-header-btn" type="button" aria-label="Mở menu quản trị viên">
                <span class="profile-avatar-wrap">
                  <span class="avatar admin-header-avatar">{{ adminName.slice(0, 1).toUpperCase() }}</span>
                  <span class="avatar-status-dot" :class="adminStatus"></span>
                </span>
                <strong class="profile-header-name">{{ adminName }}</strong>
                <ChevronDown :size="15" class="profile-header-chevron" />
              </button>
            </template>

            <div class="user-custom-menu">
              <!-- TRẠNG THÁI -->
              <div class="menu-control-row">
                <span class="menu-control-label">TRẠNG THÁI</span>
                <button type="button" class="menu-status-badge" @click.stop="cycleAdminStatus" title="Nhấp để đổi trạng thái">
                  <span class="status-live-dot" :class="adminStatus"></span>
                  <span class="status-live-text">{{ adminStatusLabel }}</span>
                  <ChevronDown :size="13" class="status-live-chevron" />
                </button>
              </div>

              <!-- TẮT THÔNG BÁO -->
              <div class="menu-control-row">
                <span class="menu-control-label">TẮT THÔNG BÁO</span>
                <button
                  type="button"
                  role="switch"
                  :aria-checked="adminNotificationsMuted"
                  class="menu-switch"
                  :class="{ active: adminNotificationsMuted }"
                  @click.stop="toggleAdminNotifications"
                  aria-label="Bật hoặc tắt thông báo"
                >
                  <span class="menu-switch-thumb"></span>
                </button>
              </div>

              <DropdownMenuSeparator />

              <!-- MENU ITEMS -->
              <DropdownMenuItem @select="openSection('accounts')">
                <template #icon><User :size="17" /></template>
                Thông tin cán bộ
              </DropdownMenuItem>

              <DropdownMenuItem @select="adminPasswordModalOpen = true">
                <template #icon><KeyRound :size="17" /></template>
                Đổi mật khẩu
              </DropdownMenuItem>

              <DropdownMenuItem @select="adminSupportModalOpen = true">
                <template #icon><Headphones :size="17" /></template>
                Hỗ trợ
              </DropdownMenuItem>

              <DropdownMenuItem @select="adminAboutModalOpen = true">
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
        </div>
      </header>

      <div class="admin-content">
        <div v-if="pageError" class="admin-alert" role="alert">
          <div class="admin-alert-body">
            <Info :size="16" class="admin-alert-icon" />
            <span>{{ pageError }}</span>
          </div>
          <button aria-label="Đóng thông báo" @click="pageError = ''">
            <X :size="15" />
          </button>
        </div>
        <div v-if="activeSection !== 'knowledge'" class="admin-page-heading">
          <div>
            <p class="admin-kicker">{{ activeSection === 'overview' ? 'BÁO CÁO & PHÂN TÍCH' : 'TRUNG TÂM QUẢN TRỊ' }}</p>
            <h1>{{ currentTitle }}</h1>
            <p>{{ activeSection === 'overview' ? 'Theo dõi chỉ số học tập, lượt thi thực tế và trạng thái vận hành nền tảng theo thời gian thực.' : 'Quản lý tài khoản và nội dung phục vụ lộ trình học tập cá nhân.' }}</p>
          </div>
          <div class="heading-actions">
            <div v-if="activeSection === 'overview'" class="analytics-controls">
              <div class="analytics-time-filter" role="group" aria-label="Khoảng thời gian thống kê">
                <button v-for="period in analyticsPeriods" :key="period.id" type="button" :aria-pressed="analyticsPeriod === period.id" :class="{ active: analyticsPeriod === period.id }" @click="analyticsPeriod = period.id">{{ period.label }}</button>
              </div>
              <label class="analytics-exam-filter">
                <span class="sr-only">Lọc theo kỳ thi</span>
                <select v-model="analyticsCertificateId" aria-label="Lọc theo kỳ thi">
                  <option value="">Tất cả kỳ thi</option>
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }}</option>
                </select>
              </label>
              <button
                type="button"
                class="admin-secondary-button overview-refresh-button"
                :disabled="analyticsLoading"
                title="Làm mới số liệu thống kê"
                aria-label="Làm mới số liệu thống kê"
                @click="loadAnalytics"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': analyticsLoading }" />
                <span>Làm mới</span>
              </button>
            </div>
            <button
              v-if="activeSection === 'accounts'"
              class="admin-primary-button"
              @click="openAccount"
            >
              <Users :size="16" />
              Tạo tài khoản
            </button>
            <button
              v-if="activeSection === 'programs'"
              class="admin-secondary-button"
              @click="openProgram"
            >
              <GraduationCap :size="16" />
              Tạo chứng chỉ
            </button>
          </div>
        </div>


        <OverviewView v-if="activeSection === 'overview'" />
        <AccountsView v-else-if="activeSection === 'accounts'" />
        <BillingView v-else-if="activeSection === 'billing'" />
        <ResultsView v-else-if="activeSection === 'results'" />
        <ProgramsView v-else-if="activeSection === 'programs'" />
        <ExamsView v-else-if="activeSection === 'exams'" />
        <KnowledgeView v-else-if="activeSection === 'knowledge'" />

      </div>
    </section>

    <ExamBuilderDialog
      :open="showExamBuilder"
      :programs="programs"
      :topics="topics"
      :draft-key="adminName"
      @close="showExamBuilder = false"
      @saved="loadData"
    />

    <Modal
      ref="adminModalRef"
      v-model:open="adminModalOpen"
      :title="adminModalTitle"
      :description="adminModalDescription"
      :dirty="adminModalDirty"
      :submitting="saving"
      size="md"
    >
      <form v-if="modal === 'account'" id="admin-account-form" class="admin-overlay-form" @submit.prevent="saveAccount">
        <p v-if="adminModalError" class="overlay-form-error" role="alert">{{ adminModalError }}</p>
        <label class="admin-overlay-field">Tên đăng nhập <b aria-hidden="true">*</b>
          <input v-model.trim="accountForm.username" required maxlength="50" autocomplete="username" placeholder="vd: hocvien01" />
        </label>
        <label class="admin-overlay-field">Mật khẩu <b aria-hidden="true">*</b>
          <input v-model="accountForm.password" type="password" required minlength="8" autocomplete="new-password" placeholder="Tối thiểu 8 ký tự" />
        </label>
        <label class="admin-overlay-field">Vai trò
          <select v-model="accountForm.role"><option value="ROLE_USER">Người học</option><option value="ROLE_TEACHER">Giáo viên</option><option value="ROLE_ADMIN">Quản trị viên</option></select>
        </label>
        <label v-if="accountForm.role === 'ROLE_TEACHER'" class="admin-overlay-field admin-overlay-checkbox">
          <input v-model="accountForm.canApproveExams" type="checkbox" /> Có quyền duyệt bộ đề và đáp án
        </label>
      </form>
      <form v-else-if="modal === 'program'" id="admin-program-form" class="admin-overlay-form" @submit.prevent="saveProgram">
        <p v-if="adminModalError" class="overlay-form-error" role="alert">{{ adminModalError }}</p>
        <div class="form-row">
          <label class="admin-overlay-field">Tên chứng chỉ / kỳ thi <b aria-hidden="true">*</b>
            <input v-model.trim="programForm.name" required maxlength="120" placeholder="vd: IELTS Academic, TOEIC, THPT Quốc Gia" />
          </label>
          <label class="admin-overlay-field">Mã kỳ thi <b aria-hidden="true">*</b>
            <input v-model.trim="programForm.code" required maxlength="50" placeholder="IELTS, TOEIC hoặc THPT" />
          </label>
        </div>
        <label class="admin-overlay-field">Mô tả chương trình thi
          <textarea v-model.trim="programForm.description" rows="2" placeholder="Thang điểm, mục tiêu và kỹ năng / môn thi"></textarea>
        </label>
        <div class="form-row">
          <label class="admin-overlay-field">Tổng điểm <b aria-hidden="true">*</b>
            <input v-model.number="programForm.totalScore" type="number" min="1" required />
          </label>
          <label class="admin-overlay-field">Điểm đạt <b aria-hidden="true">*</b>
            <input v-model.number="programForm.passingScore" type="number" min="0" required />
          </label>
          <label class="admin-overlay-field">Band / điểm mục tiêu
            <input v-model.number="programForm.passingBand" type="number" min="0" step="0.5" />
          </label>
        </div>
        <label class="admin-overlay-field">Kỹ năng / môn thi (Mảng JSON) <b aria-hidden="true">*</b>
          <textarea v-model.trim="programForm.skills" rows="3" required placeholder='["LISTENING","READING","WRITING","SPEAKING"]'></textarea>
          <small class="table-secondary">Định dạng mảng JSON: ["LISTENING","READING"] hoặc ["TOAN","VAN","ANH"]</small>
        </label>
      </form>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-secondary" @click="requestAdminModalClose">Hủy</button>
        <button v-if="modal === 'account'" type="submit" form="admin-account-form" class="overlay-button overlay-button-primary overlay-button-loading" :class="{ 'is-loading': saving }" :disabled="saving">
          <LoaderCircle v-if="saving" class="overlay-spinner" :size="16" />
          <Check v-else :size="16" />{{ saving ? 'Đang lưu…' : 'Tạo tài khoản' }}
        </button>
        <button v-else type="submit" form="admin-program-form" class="overlay-button overlay-button-primary overlay-button-loading" :class="{ 'is-loading': saving }" :disabled="saving">
          <LoaderCircle v-if="saving" class="overlay-spinner" :size="16" />
          <Check v-else :size="16" />{{ saving ? 'Đang lưu…' : editingCertificateId ? 'Cập nhật kỳ thi' : 'Tạo kỳ thi' }}
        </button>
      </template>
    </Modal>

    <Modal
      v-model:open="knowledgePreviewOpen"
      :title="loadingKnowledgeDetail ? 'Đang tải tài liệu…' : 'Xem trước đoạn kiến thức'"
      :description="knowledgeDetail ? `${knowledgeDetail.title} · ${knowledgeDetail.fileName} · ${formatAdminNumber(knowledgePreviewChunks.length)} đoạn` : 'Đang lấy các đoạn đã tách của tài liệu.'"
      :icon="FileText"
      size="lg"
      @open-change="(open) => !open && closeKnowledgePreview()"
    >
      <div v-if="loadingKnowledgeDetail" class="overlay-loading-state"><LoaderCircle class="overlay-spinner" :size="20" />Đang tải đoạn kiến thức…</div>
      <ol v-else class="knowledge-preview-list">
        <li v-for="(chunk, index) in knowledgePreviewChunks" :key="index" class="knowledge-preview-chunk">
          <span>{{ formatAdminNumber(index + 1) }}</span>
          <p>{{ chunk }}</p>
        </li>
      </ol>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-secondary" @click="closeKnowledgePreview">Đóng</button>
        <button v-if="knowledgeDetail" type="button" class="overlay-button overlay-button-primary" @click="editKnowledgeDocument()"><Pencil :size="16" />Sửa tài liệu</button>
      </template>
    </Modal>

    <Drawer
      ref="knowledgeEditorRef"
      :open="knowledgeEditorOpen"
      title="Sửa tài liệu"
      :description="knowledgeEditForm.title || 'Cập nhật thông tin và nội dung tài liệu.'"
      :dirty="knowledgeEditDirty"
      :submitting="savingKnowledgeEdit"
      size="lg"
      @update:open="onKnowledgeEditorOpenChange"
    >
      <form id="knowledge-edit-form" class="knowledge-edit-form" @submit.prevent="saveKnowledgeEdit">
        <label class="overlay-field">Tên tài liệu <b aria-hidden="true">*</b>
          <input v-model.trim="knowledgeEditForm.title" required maxlength="200" />
        </label>
        <label class="overlay-field">Kỳ thi <b aria-hidden="true">*</b>
          <select v-model="knowledgeEditForm.certificateId" required>
            <option disabled value="">Chọn kỳ thi</option>
            <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }}</option>
          </select>
        </label>
        <fieldset v-if="knowledgeEditTopics.length" class="overlay-skill-field">
          <legend>Kỹ năng / môn thi <small>Tùy chọn</small></legend>
          <div class="knowledge-skill-choice" role="group" aria-label="Chọn kỹ năng hoặc môn thi cho tài liệu">
            <button type="button" class="knowledge-skill-chip skill-auto" :class="{ selected: !knowledgeEditForm.topicId }" :aria-pressed="!knowledgeEditForm.topicId" @click="knowledgeEditForm.topicId = ''"><Check v-if="!knowledgeEditForm.topicId" :size="15" /><span v-else class="knowledge-skill-dot"></span>Tất cả</button>
            <button v-for="topic in knowledgeEditTopics" :key="topic.topicId" type="button" class="knowledge-skill-chip" :class="[knowledgeSkillTone(topic), { selected: String(topic.topicId) === knowledgeEditForm.topicId }]" :aria-pressed="String(topic.topicId) === knowledgeEditForm.topicId" @click="knowledgeEditForm.topicId = String(topic.topicId)">
              <Check v-if="String(topic.topicId) === knowledgeEditForm.topicId" :size="15" /><span v-else class="knowledge-skill-dot"></span>{{ topic.name }}
            </button>
          </div>
        </fieldset>
        <label class="overlay-field">Nội dung tài liệu <b aria-hidden="true">*</b>
          <textarea v-model="knowledgeEditForm.content" required maxlength="500000" rows="14"></textarea>
          <small>{{ formatAdminNumber(knowledgeEditForm.content.length) }} / 500.000 ký tự</small>
        </label>
      </form>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-secondary" :disabled="savingKnowledgeEdit" @click="requestKnowledgeEditorClose">Hủy</button>
        <button type="submit" form="knowledge-edit-form" class="overlay-button overlay-button-loading overlay-button-primary" :class="{ 'is-loading': savingKnowledgeEdit }" :disabled="savingKnowledgeEdit">
          <LoaderCircle v-if="savingKnowledgeEdit" class="overlay-spinner" :size="16" />
          <Check v-else :size="16" />{{ savingKnowledgeEdit ? 'Đang lưu…' : 'Lưu thay đổi' }}
        </button>
      </template>
    </Drawer>

    <Drawer
      :open="attemptResultOpen"
      title="Chi tiết kết quả thi"
      :description="selectedAttemptResult?.attempt ? `${examTitle(selectedAttemptResult.attempt)} · ${formatAttemptDate(selectedAttemptResult.attempt.submittedAt)}` : 'Kết quả bài thi'"
      size="md"
      @update:open="attemptResultOpen = $event"
    >
      <div v-if="attemptResultLoading" class="overlay-loading-state"><LoaderCircle class="overlay-spinner" :size="20" />Đang tải kết quả…</div>
      <p v-else-if="selectedAttemptResult?.error" class="learner-report-error" role="alert">{{ selectedAttemptResult.error }}</p>
      <div v-else-if="selectedAttemptResult?.result" class="attempt-result-detail">
        <div class="attempt-result-score"><span>Điểm đạt được</span><strong>{{ formatAttemptScore(selectedAttemptResult.result) }}</strong><small v-if="selectedAttemptResult.result.bandScore != null">Band {{ selectedAttemptResult.result.bandScore }}</small></div>
        <dl>
          <div><dt>Số câu đúng</dt><dd>{{ selectedAttemptResult.result.correctCount ?? '—' }}</dd></div>
          <div><dt>Số câu sai</dt><dd>{{ selectedAttemptResult.result.wrongCount ?? '—' }}</dd></div>
          <div><dt>Bỏ trống</dt><dd>{{ selectedAttemptResult.result.unansweredCount ?? '—' }}</dd></div>
          <div><dt>Thời gian làm</dt><dd>{{ selectedAttemptResult.result.timeTakenSeconds == null ? '—' : `${Math.floor(selectedAttemptResult.result.timeTakenSeconds / 60)} phút` }}</dd></div>
          <div><dt>Vi phạm quy chế</dt><dd>{{ selectedAttemptResult.result.violationCount ?? 0 }}</dd></div>
        </dl>
      </div>
      <template #footer><button type="button" class="overlay-button overlay-button-secondary" @click="attemptResultOpen = false">Đóng</button></template>
    </Drawer>

    <!-- MODAL CẤU HÌNH GÓI CƯỚC THANH TOÁN -->
    <Modal
      v-model:open="planModalOpen"
      :title="editingPlanId ? 'Chỉnh sửa gói cước' : 'Thêm gói cước mới'"
      :description="editingPlanId ? 'Cập nhật giá bán, quyền lợi và thời hạn áp dụng.' : 'Tạo gói học tập mới cho học viên đăng ký trên hệ thống.'"
      :submitting="savingPlan"
      size="md"
    >
      <form id="plan-form" class="admin-overlay-form" @submit.prevent="savePlan">
        <p v-if="planModalError" class="overlay-form-error" role="alert">{{ planModalError }}</p>
        <div class="form-row">
          <label class="admin-overlay-field">Mã gói cước <b aria-hidden="true">*</b>
            <input
              v-model.trim="planForm.code"
              :disabled="editingPlanId && ['FREE', 'PREMIUM'].includes(planForm.code)"
              required
              maxlength="32"
              placeholder="vd: PREMIUM_PLUS"
            />
          </label>
          <label class="admin-overlay-field">Tên gói cước <b aria-hidden="true">*</b>
            <input v-model.trim="planForm.name" required maxlength="100" placeholder="vd: Gói Tháng Chuyên Sâu" />
          </label>
        </div>

        <div class="form-row">
          <label class="admin-overlay-field">Giá gói <b aria-hidden="true">*</b>
            <input v-model.number="planForm.price" type="number" step="0.01" min="0" required placeholder="0.00" />
          </label>
          <label class="admin-overlay-field">Tiền tệ
            <select v-model="planForm.currency">
              <option value="USD">USD ($)</option>
              <option value="VND">VND (đ)</option>
            </select>
          </label>
        </div>

        <div class="form-row">
          <label class="admin-overlay-field">Chu kỳ thanh toán
            <select v-model="planForm.billingCycle">
              <option value="LIFETIME">Trọn đời (Một lần)</option>
              <option value="MONTHLY">Hàng tháng (30 ngày)</option>
              <option value="ANNUAL">Hàng năm (365 ngày)</option>
            </select>
          </label>
          <label class="admin-overlay-field">Giới hạn lượt thi
            <input v-model.number="planForm.examLimit" type="number" placeholder="-1 cho không giới hạn" />
            <small class="table-secondary">Nhập -1 nếu không giới hạn lượt thi</small>
          </label>
        </div>

        <label class="admin-overlay-field">Mô tả gói cước
          <textarea v-model="planForm.description" rows="2" placeholder="Tóm tắt mục đích và đối tượng học viên của gói cước"></textarea>
        </label>

        <label class="admin-overlay-field">Quyền lợi tính năng (Mỗi dòng một tính năng)
          <textarea
            v-model="planForm.featuresText"
            rows="4"
            placeholder="5 lượt thi chuẩn hóa miễn phí&#10;Trợ lý AI và Gia sư học tập 24/7&#10;Chấm Speaking & Writing chi tiết"
          ></textarea>
        </label>

        <label class="admin-overlay-checkbox">
          <input v-model="planForm.isActive" type="checkbox" />
          <span>Mở bán gói cước này cho học viên</span>
        </label>
      </form>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-secondary" :disabled="savingPlan" @click="planModalOpen = false">Hủy</button>
        <button type="submit" form="plan-form" class="overlay-button overlay-button-loading overlay-button-primary" :class="{ 'is-loading': savingPlan }" :disabled="savingPlan">
          <LoaderCircle v-if="savingPlan" class="overlay-spinner" :size="16" />
          <Check v-else :size="16" />{{ savingPlan ? 'Đang lưu…' : 'Lưu gói cước' }}
        </button>
      </template>
    </Modal>

    <!-- DRAWER CHI TIẾT GIAO DỊCH HỌC VIÊN -->
    <Drawer
      :open="userPaymentsDrawerOpen"
      :title="`Lịch sử thanh toán: ${selectedUserForPayments?.username || ''}`"
      :description="`Mã học viên: #${selectedUserForPayments?.userId || ''} · Gói hiện tại: ${selectedUserForPayments?.plan || 'FREE'}`"
      size="md"
      @update:open="userPaymentsDrawerOpen = $event"
    >
      <div v-if="userPaymentsLoading" class="overlay-loading-state">
        <LoaderCircle class="overlay-spinner" :size="20" />Đang tải lịch sử giao dịch...
      </div>
      <div v-else-if="!userPaymentsList.length" class="admin-empty">
        <CreditCard :size="32" class="admin-empty-icon" />
        <strong>Học viên này chưa phát sinh giao dịch</strong>
        <p>Tài khoản đang sử dụng gói mặc định của hệ thống.</p>
      </div>
      <div v-else class="user-payments-list">
        <div v-for="item in userPaymentsList" :key="item.paymentId" class="user-payment-card">
          <div class="user-payment-head">
            <span class="payment-id-badge">#PAY-{{ item.paymentId }}</span>
            <span class="provider-badge" :class="item.provider?.toLowerCase()">{{ item.provider }}</span>
            <strong class="payment-amount">{{ formatMoney(item.amount, item.currency) }}</strong>
          </div>
          <div class="user-payment-meta">
            <span class="payment-status-pill" :class="item.status?.toLowerCase()">
              {{ item.status === 'SUCCEEDED' ? 'Thành công' : item.status === 'PENDING' ? 'Đang chờ' : item.status }}
            </span>
            <small class="payment-time">{{ formatPaymentDate(item.createdAt) }}</small>
          </div>
          <small v-if="item.providerPaymentId" class="table-secondary">
            Ref ID: {{ item.providerPaymentId }}
          </small>
        </div>
      </div>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-secondary" @click="userPaymentsDrawerOpen = false">Đóng</button>
      </template>
    </Drawer>

    <!-- Modal Hỗ trợ -->
    <Modal v-model:open="adminSupportModalOpen" title="Trung tâm Hỗ trợ" description="Chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7" :icon="Headphones">
      <div class="modal-info-stack">
        <div class="modal-info-row">
          <strong>Kênh hỗ trợ quản trị</strong>
          <p>1900 6868 (Line nội bộ: 102)</p>
        </div>
        <div class="modal-info-row">
          <strong>Email kỹ thuật</strong>
          <p>admin-support@adaptivelearning.edu.vn</p>
        </div>
        <div class="modal-info-row">
          <strong>Tài liệu hướng dẫn quản trị</strong>
          <p>Truy cập chuyên mục Quản trị & Kho kiến thức để xem các tài liệu hướng dẫn.</p>
        </div>
      </div>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-primary" @click="adminSupportModalOpen = false">Đã hiểu</button>
      </template>
    </Modal>

    <!-- Modal Giới thiệu -->
    <Modal v-model:open="adminAboutModalOpen" title="Giới thiệu Adaptive Learning" description="Nền tảng quản trị học tập & luyện thi cá nhân hóa AI" :icon="Info">
      <div class="modal-info-stack">
        <p><strong>Adaptive Learning Admin Portal</strong> - Hệ thống quản lý người dùng, phân phối đề thi, thiết lập gói cước và vận hành kho kiến thức AI.</p>
        <div class="modal-info-row">
          <strong>Phiên bản</strong>
          <p>Adaptive Learning v2.4.0 (Build 2026)</p>
        </div>
      </div>
      <template #footer>
        <button type="button" class="overlay-button overlay-button-primary" @click="adminAboutModalOpen = false">Đóng</button>
      </template>
    </Modal>

    <!-- Modal Đổi mật khẩu -->
    <Modal v-model:open="adminPasswordModalOpen" title="Đổi mật khẩu" description="Cập nhật mật khẩu để bảo vệ tài khoản quản trị của bạn" :icon="KeyRound">
      <form class="password-form-stack" @submit.prevent="handleAdminChangePassword">
        <div v-if="adminPasswordError" class="notice notice-danger">{{ adminPasswordError }}</div>
        <div class="password-form-group">
          <label>Mật khẩu hiện tại</label>
          <input v-model="adminPasswordForm.oldPassword" type="password" placeholder="Nhập mật khẩu cũ" required />
        </div>
        <div class="password-form-group">
          <label>Mật khẩu mới</label>
          <input v-model="adminPasswordForm.newPassword" type="password" placeholder="Tối thiểu 6 ký tự" required />
        </div>
        <div class="password-form-group">
          <label>Nhập lại mật khẩu mới</label>
          <input v-model="adminPasswordForm.confirmPassword" type="password" placeholder="Xác nhận mật khẩu mới" required />
        </div>
        <div class="form-actions" style="margin-top: 14px; display: flex; justify-content: flex-end; gap: 8px;">
          <button type="button" class="overlay-button overlay-button-secondary" @click="adminPasswordModalOpen = false">Hủy</button>
          <button type="submit" class="overlay-button overlay-button-primary" :disabled="adminChangingPassword">
            {{ adminChangingPassword ? 'Đang lưu...' : 'Lưu mật khẩu mới' }}
          </button>
        </div>
      </form>
    </Modal>
  </main>
</template>

<style scoped>
  .admin-shell {
    position: relative;
    display: flow-root;
    width: 100%;
    min-width: 0;
    min-height: 100vh;
    color: #0F172A;
    background: #F8FAFC;
    font-family: var(--font-sans);
    font-size: 14px;
    line-height: 1.5;
  }
  .admin-shell,
  .admin-shell * { box-sizing: border-box; }
  .admin-shell :focus-visible { outline: 2px solid #2563EB; outline-offset: 2px; }
  .admin-scrim { display: none; }
  .admin-sidebar {
    position: fixed;
    z-index: 30;
    inset: 0 auto 0 0;
    display: flex;
    width: 264px;
    flex-direction: column;
    padding: 16px 14px;
    border-right: 1px solid #E2E8F0;
    background: #FFFFFF;
    color: #475569;
    overflow-y: auto;
  }
  .admin-sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
  .admin-sidebar-close {
    display: none;
    width: 32px;
    height: 32px;
    place-items: center;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #64748B;
    cursor: pointer;
    transition: all 140ms ease;
  }
  .admin-sidebar-close:hover {
    background: #F1F5F9;
    color: #0F172A;
  }
  .admin-brand {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 6px 8px;
    color: #0F172A;
    text-decoration: none;
    border-radius: 8px;
    transition: background 150ms ease;
  }
  .admin-brand:hover {
    background: #F8FAFC;
  }
  .admin-brand-mark {
    display: grid;
    width: 36px;
    height: 36px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    color: #fff;
    background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%);
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.2);
  }
  .admin-brand-copy {
    display: grid;
    min-width: 0;
    gap: 1px;
  }
  .admin-brand-copy strong {
    font-size: 14px;
    font-weight: 700;
    color: #0F172A;
    letter-spacing: -0.02em;
    white-space: nowrap;
  }
  .admin-brand-copy em {
    color: #2563EB;
    font-style: normal;
  }
  .admin-brand-copy small {
    color: #64748B;
    font-size: 11px;
    font-weight: 500;
  }
  .admin-nav {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 10px;
  }
  .admin-nav-group {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  .admin-workspace-label {
    padding: 6px 10px 4px;
    color: #94A3B8;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
  }
  .admin-nav-link {
    position: relative;
    display: flex;
    min-height: 40px;
    align-items: center;
    gap: 10px;
    padding: 0 12px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: #475569;
    font: inherit;
    font-size: 13.5px;
    font-weight: 500;
    text-align: left;
    cursor: pointer;
    transition: all 140ms ease;
  }
  .admin-nav-icon {
    display: grid;
    width: 20px;
    height: 20px;
    flex: 0 0 auto;
    place-items: center;
    color: #64748B;
    background: transparent !important;
    border-radius: 0;
    box-shadow: none !important;
    transition: color 140ms ease;
  }
  .admin-nav-link:hover {
    background: #F8FAFC;
    color: #0F172A;
  }
  .admin-nav-link:hover .admin-nav-icon {
    color: #0F172A;
    transform: none;
  }
  .admin-nav-link.active {
    background: #EFF6FF;
    color: #1D4ED8;
    font-weight: 600;
  }
  .admin-nav-link.active .admin-nav-icon {
    color: #2563EB;
  }
  .admin-nav-label {
    min-width: 0;
    flex: 1;
  }
  .admin-nav-indicator {
    display: block;
    position: absolute;
    right: 8px;
    width: 4px;
    height: 18px;
    border-radius: 2px;
    background: #2563EB;
  }
  .admin-sidebar-bottom {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: auto;
    padding-top: 14px;
    border-top: 1px solid #E2E8F0;
  }
  .admin-status-indicator {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 8px 10px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #F8FAFC;
    color: #334155;
  }
  .admin-status-dot {
    width: 8px;
    height: 8px;
    flex: 0 0 auto;
    border-radius: 50%;
    background: #16A34A;
    box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.2);
  }
  .admin-status-indicator div {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 1px;
  }
  .admin-status-indicator strong {
    font-size: 11.5px;
    font-weight: 600;
    color: #1E293B;
    line-height: 1.2;
  }
  .admin-status-indicator small {
    color: #64748B;
    font-size: 10.5px;
    line-height: 1.2;
  }
  .admin-switch-learner-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 7px;
    padding: 7px 12px;
    font-size: 12.5px;
    font-weight: 500;
    color: #475569;
    text-decoration: none;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    transition: all 140ms ease;
  }
  .admin-switch-learner-btn:hover {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .admin-workspace {
    min-width: 0;
    width: calc(100% - 264px);
    box-sizing: border-box;
    margin-left: 264px;
    background: #F8FAFC;
    min-height: 100vh;
  }
  .admin-topbar {
    position: sticky;
    z-index: 20;
    top: 0;
    display: flex;
    height: 60px;
    box-sizing: border-box;
    align-items: center;
    gap: 16px;
    padding: 0 32px;
    border-bottom: 1px solid #E2E8F0;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(12px);
  }
  .admin-topbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }
  .admin-breadcrumb {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    white-space: nowrap;
  }
  .breadcrumb-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 7px;
    border-radius: 4px;
    background: #F1F5F9;
    color: #475569;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }
  .breadcrumb-arrow {
    color: #94A3B8;
    flex-shrink: 0;
  }
  .breadcrumb-page {
    font-weight: 600;
    color: #0F172A;
  }
  .admin-search {
    display: flex;
    width: min(100%, 380px);
    min-width: 0;
    height: 38px;
    flex: 1 1 240px;
    align-items: center;
    gap: 9px;
    padding: 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    color: #64748B;
    background: #FFFFFF;
    transition: all 0.15s ease;
  }
  .admin-search:focus-within {
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
  .admin-search input {
    width: 100%;
    min-width: 0;
    border: 0;
    outline: 0;
    color: #0F172A;
    font: inherit;
    font-size: 13.5px;
    background: transparent;
  }
  .admin-search input::placeholder {
    color: #94A3B8;
  }
  .admin-top-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
  }
  .admin-preview-learner-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 11px;
    font-size: 12.5px;
    font-weight: 500;
    color: #475569;
    text-decoration: none;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    transition: all 140ms ease;
    white-space: nowrap;
  }
  .admin-preview-learner-link:hover {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .admin-notification,
  .admin-menu-button {
    position: relative;
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    color: #64748B;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .admin-notification:hover,
  .admin-menu-button:hover {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .admin-notification i {
    position: absolute;
    top: 7px;
    right: 7px;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    border: 1.5px solid #FFFFFF;
    background: #EF4444;
  }
  .admin-profile {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 10px 4px 5px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    text-align: left;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .admin-profile:hover {
    background: #F8FAFC;
    border-color: #CBD5E1;
  }
  .admin-profile > span {
    display: grid;
    width: 28px;
    height: 28px;
    place-items: center;
    border-radius: 6px;
    background: #2563EB;
    color: #FFFFFF;
    font-size: 12px;
    font-weight: 700;
  }
  .admin-profile div {
    display: grid;
    gap: 1px;
    line-height: 1.2;
  }
  .admin-profile strong {
    font-size: 13px;
    font-weight: 600;
    color: #0F172A;
  }
  .admin-profile small {
    color: #64748B;
    font-size: 11px;
  }
  .admin-profile > svg {
    margin-left: 4px;
    color: #94A3B8;
    transition: color 0.15s ease;
  }
  .admin-profile:hover > svg {
    color: #2563EB;
  }
  .admin-menu-avatar {
    background: linear-gradient(135deg, #1E293B, #0F172A) !important;
  }
  .badge-admin {
    background: #EFF6FF !important;
    color: #1D4ED8 !important;
    border: 1px solid #BFDBFE !important;
  }
  .admin-menu-button {
    display: none;
  }
  .admin-content {
    width: 100%;
    max-width: none;
    min-width: 0;
    box-sizing: border-box;
    margin: 0;
    padding: 24px 32px 56px;
  }
  .admin-content > *,
  .admin-content section,
  .admin-content form { min-width: 0; }
  .admin-page-heading,
  .admin-panel-heading { min-width: 0; }
  .admin-panel-heading h2 { margin: 0; color: #0F172A; font-size: 15px; line-height: 22px; font-weight: 600; }
  .admin-panel-heading p { margin: 2px 0 0; color: #64748B; font-size: 13px; }
  .admin-alert {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 20px;
    padding: 10px 14px;
    border: 1px solid #FECACA;
    border-radius: 8px;
    background: #FEF2F2;
    color: #991B1B;
    font-size: 13.5px;
    box-shadow: 0 1px 2px rgba(239, 68, 68, 0.05);
  }
  .admin-alert-body {
    display: flex;
    align-items: center;
    gap: 9px;
  }
  .admin-alert-icon {
    flex: 0 0 auto;
    color: #DC2626;
  }
  .admin-alert button {
    display: grid;
    width: 26px;
    height: 26px;
    flex: 0 0 auto;
    place-items: center;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #991B1B;
    cursor: pointer;
    transition: background 0.15s ease;
  }
  .admin-alert button:hover {
    background: rgba(185, 28, 28, 0.12);
  }
  .admin-page-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 24px;
  }
  .admin-kicker {
    margin: 0 0 4px;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  .admin-page-heading h1 {
    margin: 0;
    color: #0F172A;
    font-size: 24px;
    line-height: 32px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .admin-page-heading > div > p:last-child {
    margin: 4px 0 0;
    max-width: 680px;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .heading-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }

  .analytics-controls {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }
  .analytics-time-filter {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    padding: 3px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #F1F5F9;
  }
  .analytics-time-filter button {
    height: 32px;
    padding: 0 12px;
    border: 0;
    border-radius: 6px;
    color: #64748B;
    background: transparent;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .analytics-time-filter button:hover { color: #0F172A; background: rgba(255,255,255,.6); }
  .analytics-time-filter button.active { color: #2563EB; background: #FFFFFF; font-weight: 600; box-shadow: 0 1px 2px rgba(15,23,42,.06); }
  .analytics-exam-filter select {
    width: 100%;
    height: 38px;
    min-width: 160px;
    max-width: 260px;
    padding: 0 32px 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    color: #0F172A;
    background: #FFFFFF;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .overview-refresh-button { height: 38px; }

  @media (max-width: 1024px) {
    .admin-sidebar {
      width: min(280px, calc(100vw - 48px));
      transform: translateX(-100%);
      transition: transform 180ms ease;
    }
    .admin-sidebar.admin-sidebar-open { transform: translateX(0); }
    .admin-sidebar-close { display: grid; }
    .admin-scrim { position: fixed; z-index: 29; inset: 0; display: block; background: rgba(15,23,42,.44); }
    .admin-workspace { width: 100%; margin-left: 0; }
    .admin-menu-button { display: grid; flex: 0 0 auto; }
    .admin-topbar { gap: 12px; padding-right: 20px; padding-left: 20px; }
    .admin-topbar-left { flex: 0 1 auto; }
    .admin-search { flex: 1 1 180px; }
    .admin-top-actions { gap: 8px; }
    .admin-preview-learner-link { display: none; }
    .admin-content { padding-right: 20px; padding-left: 20px; }
  }

  @media (max-width: 640px) {
    .admin-topbar { height: auto; min-height: 60px; flex-wrap: wrap; gap: 8px; padding: 10px 14px; }
    .admin-topbar-left { flex: 1 1 auto; }
    .admin-breadcrumb { min-width: 0; font-size: 12px; }
    .breadcrumb-page { max-width: 118px; overflow: hidden; text-overflow: ellipsis; }
    .admin-top-actions { flex: 0 0 auto; margin-left: auto; }
    .profile-header-name,
    .profile-header-chevron { display: none; }
    .admin-search { order: 3; width: 100%; max-width: none; flex: 1 1 100%; }
    .admin-content { padding: 20px 14px 40px; }
    .admin-page-heading { align-items: stretch; flex-direction: column; gap: 14px; }
    .heading-actions { width: 100%; justify-content: flex-start; }
    .analytics-controls { width: 100%; gap: 8px; }
    .analytics-time-filter { max-width: 100%; overflow-x: auto; }
    .analytics-time-filter button { padding: 0 9px; white-space: nowrap; }
    .analytics-exam-filter { flex: 1 1 150px; min-width: 0; }
    .analytics-exam-filter select { min-width: 0; max-width: none; }
  }

  @media (prefers-reduced-motion: reduce) {
    .admin-sidebar { transition: none; }
  }
</style>
