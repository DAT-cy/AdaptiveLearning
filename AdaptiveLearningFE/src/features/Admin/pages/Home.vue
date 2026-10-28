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
    activeSection.value = section
    query.value = ''
    mobileMenuOpen.value = false
    router.replace({
      path: '/admin',
      query: section === 'overview' ? {} : { section },
    })
    if (section === 'results' && !selectedLearnerId.value && students.value.length) {
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
      message: `Bạn có chắc muốn cấp vai trò "${newRole === 'ROLE_ADMIN' ? 'Quản trị viên (ROLE_ADMIN)' : 'Người học (ROLE_USER)'}" cho tài khoản "${user.username}"?`,
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
      <div
        v-if="mobileMenuOpen"
        class="admin-scrim"
        @click="mobileMenuOpen = false"
      ></div>

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

        <template v-if="activeSection === 'overview'">
          <div class="overview-container">
            <!-- 1. Real-Data KPI Summary Cards -->
            <div class="overview-kpi-grid">
              <!-- KPI 1: Học viên hệ thống -->
              <div class="overview-kpi-card is-interactive" @click="openSection('accounts')">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-blue">
                    <Users :size="20" />
                  </div>
                  <span class="overview-kpi-action">Quản lý →</span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Học viên hệ thống</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(studentAccountsCount) }}</strong>
                  <div class="overview-kpi-meta">
                    <span class="status-dot online"></span>
                    <span>{{ formatAdminNumber(activeAccountsCount) }} hoạt động · {{ formatAdminNumber(premiumAccountsCount) }} Premium</span>
                  </div>
                </div>
              </div>

              <!-- KPI 2: Lượt làm bài trong kỳ -->
              <div class="overview-kpi-card">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-teal">
                    <ClipboardCheck :size="20" />
                  </div>
                  <span
                    v-if="displayAnalyticsData?.completionRateChange != null"
                    :class="['overview-delta-badge', displayAnalyticsData.completionRateChange > 0 ? 'delta-positive' : displayAnalyticsData.completionRateChange < 0 ? 'delta-negative' : 'delta-neutral']"
                  >
                    {{ displayAnalyticsData.completionRateChange > 0 ? '+' : displayAnalyticsData.completionRateChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.completionRateChange) }}%
                  </span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Lượt thi trong kỳ ({{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label }})</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(displayAnalyticsData?.startedAttempts ?? 0) }}</strong>
                  <div class="overview-kpi-meta">
                    <span>{{ formatAdminNumber(displayAnalyticsData?.completedAttempts ?? 0) }} đã nộp bài ({{ formatPercent(displayAnalyticsData?.completionRate) }})</span>
                  </div>
                </div>
              </div>

              <!-- KPI 3: Điểm trung bình & Tỷ lệ đạt -->
              <div class="overview-kpi-card">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-green">
                    <TrendingUp :size="20" />
                  </div>
                  <span
                    v-if="displayAnalyticsData?.averageScoreChange != null"
                    :class="['overview-delta-badge', displayAnalyticsData.averageScoreChange > 0 ? 'delta-positive' : displayAnalyticsData.averageScoreChange < 0 ? 'delta-negative' : 'delta-neutral']"
                  >
                    {{ displayAnalyticsData.averageScoreChange > 0 ? '+' : displayAnalyticsData.averageScoreChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.averageScoreChange) }}đ
                  </span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Điểm trung bình lượt thi</span>
                  <strong class="overview-kpi-value">{{ formatPercent(displayAnalyticsData?.averageScorePercent) }}</strong>
                  <div class="overview-kpi-meta">
                    <span>Tỷ lệ đạt chuẩn: <strong>{{ formatPercent(displayAnalyticsData?.passRate) }}</strong></span>
                  </div>
                </div>
              </div>

              <!-- KPI 4: Học liệu & Tri thức RAG -->
              <div class="overview-kpi-card is-interactive" @click="openSection('knowledge')">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-purple">
                    <Database :size="20" />
                  </div>
                  <span class="overview-kpi-action">Kho tri thức →</span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Học liệu & Tri thức RAG</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(exams.length) }} <span class="overview-kpi-unit">đề thi</span></strong>
                  <div class="overview-kpi-meta">
                    <span>{{ programs.length }} chứng chỉ · {{ knowledgeDocuments.length }} tài liệu ({{ formatAdminNumber(knowledgeChunkCount) }} chunks)</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. Loading State -->
            <div v-if="analyticsLoading" class="overview-status-card overview-loading-state" role="status" aria-live="polite">
              <LoaderCircle :size="32" class="overlay-spinner" />
              <div class="status-text">
                <h3>Đang tổng hợp dữ liệu học tập...</h3>
                <p>Hệ thống đang truy vấn số liệu thi và tiến độ người học.</p>
              </div>
            </div>

            <!-- 3. Error State -->
            <div v-else-if="analyticsError" class="overview-status-card overview-error-state" role="alert">
              <div class="error-info">
                <Info :size="24" class="error-icon" />
                <div>
                  <h3>Không thể tải báo cáo thống kê</h3>
                  <p>{{ analyticsError }}</p>
                </div>
              </div>
              <button type="button" class="admin-secondary-button" @click="loadAnalytics">
                <RefreshCw :size="15" />
                <span>Thử lại</span>
              </button>
            </div>

            <!-- 4. Content Area (When not loading and no error) -->
            <template v-else>
              <!-- AI Insight Banner (when real attempts exist) -->
              <div v-if="displayAnalyticsData && displayAnalyticsData.startedAttempts > 0" class="overview-insight-banner">
                <div class="insight-badge">
                  <Sparkles :size="15" />
                  <span>PHÂN TÍCH NHANH</span>
                </div>
                <p class="insight-content">{{ analyticsInsight }}</p>
                <span class="insight-period">{{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label }} qua · {{ programs.find((program) => String(program.certificateId) === String(analyticsCertificateId))?.name || 'Tất cả kỳ thi' }}</span>
              </div>

              <!-- Notice Banner when 0 attempts recorded -->
              <div v-else class="overview-notice-banner">
                <Info :size="18" class="notice-icon" />
                <div class="notice-content">
                  <strong>Chưa có lượt thi trong khoảng thời gian {{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label.toLowerCase() }}</strong>
                  <p>Khi học viên làm bài kiểm tra, biểu đồ xu hướng điểm và danh sách học viên cần hỗ trợ sẽ được tính toán tự động.</p>
                </div>
              </div>

              <!-- 2-Column Analytics Details Grid -->
              <div class="overview-detail-grid">
                <!-- Column 1: SVG Trend Chart -->
                <section class="overview-card" aria-labelledby="overview-chart-title">
                  <div class="overview-card-header">
                    <div>
                      <h2 id="overview-chart-title" class="overview-card-title">Xu hướng điểm số & Lượt thi</h2>
                      <p class="overview-card-subtitle">Điểm trung bình (%) và số lượt nộp bài theo từng mốc thời gian</p>
                    </div>
                    <div class="overview-chart-legend">
                      <span class="legend-item">
                        <span class="legend-color line-color"></span>
                        Điểm TB (%)
                      </span>
                      <span class="legend-item">
                        <span class="legend-color bar-color"></span>
                        Số lượt thi
                      </span>
                    </div>
                  </div>

                  <div class="overview-card-body">
                    <div v-if="!analyticsChartPoints.some((point) => point.y != null)" class="overview-empty-box">
                      <BarChart3 :size="36" class="empty-icon" />
                      <h4>Chưa có bài thi được chấm điểm</h4>
                      <p>Không có bài thi nào được hoàn thành và chấm điểm trong kỳ này để biểu diễn trên biểu đồ.</p>
                    </div>

                    <div v-else class="overview-chart-wrap">
                      <svg class="analytics-chart" viewBox="0 0 720 300" role="img" aria-label="Biểu đồ điểm trung bình và lượt thi">
                        <g class="analytics-chart-grid">
                          <template v-for="tick in [0, 25, 50, 75, 100]" :key="tick">
                            <line x1="48" :y1="22 + (100 - tick) * 2.1" x2="672" :y2="22 + (100 - tick) * 2.1" />
                            <text x="36" :y="26 + (100 - tick) * 2.1">{{ tick }}%</text>
                          </template>
                        </g>
                        <g class="analytics-chart-bars" aria-hidden="true">
                          <rect v-for="(point, index) in analyticsChartPoints" :key="`bar-${index}`" :x="point.x - 10" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32)" width="20" :height="Math.max(3, point.attempts / analyticsMaxAttempts * 32)" rx="3" />
                        </g>
                        <path v-if="analyticsScorePath" class="analytics-score-line" :d="analyticsScorePath" />
                        <g v-for="(point, index) in analyticsChartPoints" :key="`point-${index}`">
                          <circle v-if="point.y != null" class="analytics-score-point" :cx="point.x" :cy="point.y" r="4.5">
                            <title>{{ point.label }}: {{ formatPercent(point.averageScorePercent) }} ({{ point.attempts }} lượt)</title>
                          </circle>
                          <g v-if="point.annotation" class="analytics-annotation">
                            <title>{{ point.annotation }} · {{ point.label }}</title>
                            <line :x1="point.x" :y1="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 2" :x2="point.x" :y2="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 14" />
                            <rect :x="Math.min(625, Math.max(48, point.x - 42))" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 34" width="84" height="20" rx="4" />
                            <text :x="Math.min(625, Math.max(48, point.x - 42)) + 42" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 20">{{ point.annotation }}</text>
                          </g>
                          <text :class="['analytics-chart-label', { 'label-hidden-mobile': analyticsChartPoints.length > 8 && index % 2 === 1 }]" :x="point.x" y="280">{{ point.label }}</text>
                        </g>
                        <text class="analytics-chart-unit" x="48" y="16">Thang điểm (%)</text>
                      </svg>
                    </div>
                  </div>
                </section>

                <!-- Column 2: Learners Needing Support Table -->
                <section class="overview-card" aria-labelledby="overview-learners-title">
                  <div class="overview-card-header">
                    <div>
                      <h2 id="overview-learners-title" class="overview-card-title">Học viên cần theo dõi</h2>
                      <p class="overview-card-subtitle">Xếp theo điểm trung bình thấp nhất trong kỳ</p>
                    </div>
                    <span v-if="displayAnalyticsData?.learnersToSupport?.length" class="overview-badge-count">
                      {{ displayAnalyticsData.learnersToSupport.length }}
                    </span>
                  </div>

                  <div class="overview-card-body p-0">
                    <div v-if="!displayAnalyticsData?.learnersToSupport?.length" class="overview-empty-box">
                      <UserCheck :size="36" class="empty-icon icon-success" />
                      <h4>Không có học viên dưới chuẩn</h4>
                      <p>Tất cả học viên hoàn thành bài thi đều đạt kết quả tốt hoặc chưa có dữ liệu điểm thấp cần hỗ trợ.</p>
                    </div>

                    <div v-else class="overview-table-wrap">
                      <table class="overview-table">
                        <thead>
                          <tr>
                            <th>Học viên</th>
                            <th class="text-center">Lượt thi</th>
                            <th class="text-right">Điểm TB</th>
                            <th class="text-right">Báo cáo</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="learner in displayAnalyticsData.learnersToSupport" :key="learner.userId">
                            <td>
                              <div class="learner-cell">
                                <div class="learner-avatar">
                                  {{ (learner.username || 'U').slice(0, 2).toUpperCase() }}
                                </div>
                                <div class="learner-meta">
                                  <span class="learner-name" :title="learner.username">{{ learner.username }}</span>
                                  <small class="learner-date">{{ learner.lastAttemptAt ? formatAttemptDate(learner.lastAttemptAt) : 'Chưa có ngày thi' }}</small>
                                </div>
                              </div>
                            </td>
                            <td class="text-center">
                              <span class="attempts-pill">{{ learner.attempts }}</span>
                            </td>
                            <td class="text-right">
                              <span
                                :class="[
                                  'score-badge',
                                  learner.averageScorePercent < 50 ? 'score-low' : learner.averageScorePercent < 70 ? 'score-medium' : 'score-high'
                                ]"
                              >
                                {{ formatPercent(learner.averageScorePercent) }}
                              </span>
                            </td>
                            <td class="text-right">
                              <button
                                type="button"
                                class="overview-action-btn"
                                title="Xem chi tiết kết quả học tập"
                                @click="openLearnerReport(learner)"
                              >
                                Xem kết quả
                              </button>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </section>
              </div>

              <!-- 3 System Operations Pillars -->
              <div class="overview-pillars-grid">
                <!-- Pillar 1: Accounts -->
                <div class="overview-pillar-card" @click="openSection('accounts')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-blue">
                      <Users :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Vận hành Tài khoản</h3>
                      <p class="pillar-desc">Quản lý người học, phân quyền và trạng thái</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(totalAccountsCount) }}</span>
                      <span class="stat-label">Tổng tài khoản</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(studentAccountsCount) }}</span>
                      <span class="stat-label">Học viên</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(activeAccountsCount) }}</span>
                      <span class="stat-label">Hoạt động</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Xem danh sách tài khoản</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>

                <!-- Pillar 2: Exams & Programs -->
                <div class="overview-pillar-card" @click="openSection('exams')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-emerald">
                      <GraduationCap :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Đào tạo & Khảo thí</h3>
                      <p class="pillar-desc">Ngân hàng đề thi, bài kiểm tra và chứng chỉ</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(programs.length) }}</span>
                      <span class="stat-label">Chứng chỉ</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(exams.length) }}</span>
                      <span class="stat-label">Bài kiểm tra</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(displayAnalyticsData?.activeLearners ?? 0) }}</span>
                      <span class="stat-label">Thí sinh trong kỳ</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Quản lý đề thi & kỳ thi</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>

                <!-- Pillar 3: Knowledge Base & RAG -->
                <div class="overview-pillar-card" @click="openSection('knowledge')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-purple">
                      <Database :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Kho tri thức & RAG AI</h3>
                      <p class="pillar-desc">Tài liệu kiến thức nạp vào hệ thống AI Tutor</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(knowledgeDocuments.length) }}</span>
                      <span class="stat-label">Tài liệu RAG</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(activeKnowledgeCount) }}</span>
                      <span class="stat-label">Đang hiệu lực</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(knowledgeChunkCount) }}</span>
                      <span class="stat-label">Đoạn tri thức</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Quản lý kho tri thức RAG</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>

        <section
          v-else-if="activeSection === 'accounts'"
          class="admin-accounts-section"
        >
          <!-- KPI Summary Cards -->
          <div class="account-kpi-grid">
            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-total">
                <Users :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Tổng tài khoản</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(totalAccountsCount) }}</strong>
                <small class="account-kpi-hint">Toàn bộ tài khoản hệ thống</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-students">
                <GraduationCap :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Học viên</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(studentAccountsCount) }}</strong>
                <small class="account-kpi-hint">Tài khoản vai trò Người học</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-active">
                <UserCheck :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Đang hoạt động</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(activeAccountsCount) }}</strong>
                <small class="account-kpi-hint">Đã kích hoạt & xác thực email</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-premium">
                <Sparkles :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Gói Premium</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(premiumAccountsCount) }}</strong>
                <small class="account-kpi-hint">Học viên gói dịch vụ nâng cao</small>
              </div>
            </div>
          </div>

          <!-- Main Account Management Panel -->
          <div class="admin-panel account-main-panel">
            <div class="admin-panel-heading account-panel-header">
              <div>
                <h2>Danh sách người dùng & Phân quyền</h2>
                <p>Quản trị vai trò (ROLE_USER, ROLE_ADMIN), gói dịch vụ và trạng thái hoạt động của tài khoản.</p>
              </div>
              <div class="account-header-actions">
                <button
                  type="button"
                  class="admin-secondary-button"
                  :disabled="loading"
                  title="Tải lại danh sách tài khoản"
                  @click="loadData"
                >
                  <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                  <span>Làm mới</span>
                </button>
                <button
                  type="button"
                  class="admin-primary-button"
                  @click="openAccount"
                >
                  <Plus :size="16" />
                  <span>Tạo tài khoản</span>
                </button>
              </div>
            </div>

            <!-- Filter & Search Toolbar -->
            <div class="account-toolbar">
              <div class="account-search-wrap">
                <Search :size="16" class="account-search-icon" />
                <input
                  v-model="accountSearchQuery"
                  type="search"
                  placeholder="Tìm theo tên đăng nhập hoặc ID..."
                  aria-label="Tìm kiếm người dùng"
                  class="account-search-input"
                />
                <button
                  v-if="accountSearchQuery"
                  type="button"
                  class="account-search-clear"
                  aria-label="Xóa tìm kiếm"
                  @click="accountSearchQuery = ''"
                >
                  <X :size="14" />
                </button>
              </div>

              <div class="account-filters">
                <label class="account-filter-item">
                  <span class="filter-label">Vai trò</span>
                  <select v-model="accountRoleFilter" class="filter-select">
                    <option value="all">Tất cả vai trò</option>
                    <option value="ROLE_USER">Người học (ROLE_USER)</option>
                    <option value="ROLE_ADMIN">Quản trị viên (ROLE_ADMIN)</option>
                  </select>
                </label>

                <label class="account-filter-item">
                  <span class="filter-label">Gói cước</span>
                  <select v-model="accountPlanFilter" class="filter-select">
                    <option value="all">Tất cả gói</option>
                    <option value="FREE">Gói Miễn phí (Free)</option>
                    <option value="PREMIUM">⭐ Gói Cao cấp (Premium)</option>
                  </select>
                </label>

                <label class="account-filter-item">
                  <span class="filter-label">Trạng thái</span>
                  <select v-model="accountStatusFilter" class="filter-select">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="active">Đang hoạt động</option>
                    <option value="disabled">Đã khóa</option>
                    <option value="unverified">Chờ xác minh email</option>
                  </select>
                </label>

                <button
                  v-if="accountSearchQuery || accountRoleFilter !== 'all' || accountPlanFilter !== 'all' || accountStatusFilter !== 'all'"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetAccountFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <!-- Count & Meta summary -->
            <div class="account-meta-summary">
              <span>Hiển thị <strong>{{ paginatedUsers.length }}</strong> / <strong>{{ filteredUsers.length }}</strong> tài khoản phù hợp (Tổng số: {{ totalAccountsCount }})</span>
            </div>

            <!-- Loading State -->
            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách tài khoản từ hệ thống…</span>
            </div>

            <!-- Empty State -->
            <div v-else-if="!filteredUsers.length" class="admin-empty account-empty-state">
              <div class="empty-icon-wrap">
                <UserX :size="28" />
              </div>
              <strong>Không tìm thấy tài khoản phù hợp</strong>
              <p>Không có người dùng nào khớp với bộ lọc hoặc từ khóa tìm kiếm hiện tại.</p>
              <button
                v-if="accountSearchQuery || accountRoleFilter !== 'all' || accountPlanFilter !== 'all' || accountStatusFilter !== 'all'"
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetAccountFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <!-- Table & Card Views -->
            <div v-else class="account-data-container">
              <!-- Desktop Table View -->
              <div class="admin-table-wrap account-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 220px;">Tài khoản</th>
                      <th style="min-width: 150px;">Vai trò</th>
                      <th style="min-width: 180px;">Gói học & Hạn mức</th>
                      <th style="min-width: 150px;">Trạng thái</th>
                      <th style="text-align: right; min-width: 160px;">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in paginatedUsers" :key="user.userId" :class="{ 'row-disabled': !user.enabled }">
                      <td>
                        <div class="table-account">
                          <span
                            class="account-avatar"
                            :class="{ 'administrator': user.role === 'ROLE_ADMIN' }"
                          >
                            {{ user.username.slice(0, 1).toUpperCase() }}
                          </span>
                          <div class="table-account-details">
                            <strong class="account-username">{{ user.username }}</strong>
                            <small class="account-id">ID: #{{ user.userId }}</small>
                          </div>
                        </div>
                      </td>

                      <td>
                        <select
                          class="role-select"
                          :value="user.role"
                          aria-label="Thay đổi vai trò người dùng"
                          @change="updateRole(user, $event)"
                        >
                          <option value="ROLE_USER">Người học</option>
                          <option value="ROLE_ADMIN">Quản trị viên</option>
                        </select>
                      </td>

                      <td>
                        <select
                          class="role-select plan-select"
                          :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                          :value="user.plan || 'FREE'"
                          aria-label="Thay đổi gói cước người dùng"
                          @change="updateUserPlan(user, $event.target.value)"
                        >
                          <option value="FREE">Gói Free ({{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }}/{{ user.freeExamLimit || 0 }} lượt)</option>
                          <option value="PREMIUM">⭐ Gói Premium</option>
                        </select>
                      </td>

                      <td>
                        <span
                          :class="[
                            'account-state',
                            !user.enabled ? 'disabled' : user.emailVerified === false ? 'pending' : 'enabled',
                          ]"
                        >
                          <span class="state-dot" aria-hidden="true"></span>
                          <span>{{ !user.enabled ? 'Đã khóa' : user.emailVerified === false ? 'Chờ xác minh' : 'Hoạt động' }}</span>
                        </span>
                      </td>

                      <td>
                        <div class="account-actions-cell" style="justify-content: flex-end;">
                          <button
                            v-if="user.role === 'ROLE_USER'"
                            type="button"
                            class="table-action"
                            title="Xem chi tiết tiến độ học tập và lịch sử làm bài"
                            @click="openLearnerReport(user)"
                          >
                            <Eye :size="13" />
                            <span>Tiến độ</span>
                          </button>
                          <button
                            type="button"
                            class="table-action"
                            :class="user.enabled ? 'danger' : 'primary-action'"
                            :title="user.enabled ? 'Khóa truy cập của tài khoản' : 'Kích hoạt lại tài khoản'"
                            @click="updateStatus(user)"
                          >
                            <component :is="user.enabled ? Lock : Unlock" :size="13" />
                            <span>{{ user.enabled ? 'Khóa' : 'Mở khóa' }}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="account-mobile-grid">
                <article
                  v-for="user in paginatedUsers"
                  :key="`m-${user.userId}`"
                  class="account-mobile-card"
                  :class="{ 'card-disabled': !user.enabled }"
                >
                  <div class="account-card-header">
                    <div class="table-account">
                      <span
                        class="account-avatar"
                        :class="{ 'administrator': user.role === 'ROLE_ADMIN' }"
                      >
                        {{ user.username.slice(0, 1).toUpperCase() }}
                      </span>
                      <div class="table-account-details">
                        <strong class="account-username">{{ user.username }}</strong>
                        <small class="account-id">ID: #{{ user.userId }}</small>
                      </div>
                    </div>

                    <span
                      :class="[
                        'account-state',
                        !user.enabled ? 'disabled' : user.emailVerified === false ? 'pending' : 'enabled',
                      ]"
                    >
                      <span class="state-dot" aria-hidden="true"></span>
                      <span>{{ !user.enabled ? 'Đã khóa' : user.emailVerified === false ? 'Chờ xác minh' : 'Hoạt động' }}</span>
                    </span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Vai trò:</span>
                      <select
                        class="role-select"
                        :value="user.role"
                        @change="updateRole(user, $event)"
                      >
                        <option value="ROLE_USER">Người học</option>
                        <option value="ROLE_ADMIN">Quản trị viên</option>
                      </select>
                    </div>

                    <div class="card-field-row">
                      <span class="card-field-label">Gói học:</span>
                      <select
                        class="role-select plan-select"
                        :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                        :value="user.plan || 'FREE'"
                        @change="updateUserPlan(user, $event.target.value)"
                      >
                        <option value="FREE">Gói Free ({{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }}/{{ user.freeExamLimit || 0 }} lượt)</option>
                        <option value="PREMIUM">⭐ Gói Premium</option>
                      </select>
                    </div>
                  </div>

                  <div class="account-card-actions">
                    <button
                      v-if="user.role === 'ROLE_USER'"
                      type="button"
                      class="table-action mobile-card-btn"
                      @click="openLearnerReport(user)"
                    >
                      <Eye :size="14" />
                      <span>Xem tiến độ</span>
                    </button>
                    <button
                      type="button"
                      class="table-action mobile-card-btn"
                      :class="user.enabled ? 'danger' : 'primary-action'"
                      :title="user.enabled ? 'Khóa truy cập của tài khoản' : 'Kích hoạt lại tài khoản'"
                      @click="updateStatus(user)"
                    >
                      <component :is="user.enabled ? Lock : Unlock" :size="14" />
                      <span>{{ user.enabled ? 'Khóa' : 'Mở khóa' }}</span>
                    </button>
                  </div>
                </article>
              </div>

              <!-- Pagination for Accounts -->
              <div v-if="accountTotalPages > 1 || filteredUsers.length > 10" class="account-pagination">
                <div class="pagination-size">
                  <span>Hiển thị</span>
                  <select v-model.number="accountPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                    <option :value="10">10 tài khoản</option>
                    <option :value="25">25 tài khoản</option>
                    <option :value="50">50 tài khoản</option>
                  </select>
                </div>
                <div class="pagination-nav">
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="accountPage <= 1"
                    aria-label="Trang trước"
                    @click="accountPage = Math.max(1, accountPage - 1)"
                  >
                    <ChevronLeft :size="16" />
                    <span>Trước</span>
                  </button>
                  <span class="pagination-status">Trang <strong>{{ accountPage }}</strong> / {{ accountTotalPages }}</span>
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="accountPage >= accountTotalPages"
                    aria-label="Trang sau"
                    @click="accountPage = Math.min(accountTotalPages, accountPage + 1)"
                  >
                    <span>Sau</span>
                    <ChevronRight :size="16" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- GÓI CƯỚC & THANH TOÁN -->
        <section v-else-if="activeSection === 'billing'" class="admin-billing-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel billing-hero-panel">
            <div class="billing-hero-content">
              <div class="billing-hero-badge">
                <CreditCard :size="15" />
                <span>QUẢN LÝ GÓI CƯỚC & TÀI CHÍNH</span>
              </div>
              <h2>Quản trị gói cước & Tài chính hệ thống</h2>
              <p>Cấu hình danh mục gói học tập, quản lý hạn mức người học và theo dõi doanh thu thanh toán qua cổng Stripe & PayPal.</p>
            </div>
            <div class="billing-hero-actions">
              <button
                v-if="billingSubTab === 'plans'"
                type="button"
                class="admin-primary-button"
                @click="openPlanModal()"
              >
                <Plus :size="16" />
                <span>Thêm gói cước mới</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại dữ liệu gói cước và giao dịch"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="billing-metrics-grid">
            <article class="billing-stat-card revenue">
              <div class="billing-stat-icon">
                <TrendingUp :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>TỔNG DOANH THU (USD)</span>
                <strong>{{ formatMoney(totalRevenue) }}</strong>
                <small>{{ succeededPaymentsCount }} giao dịch thành công</small>
              </div>
            </article>

            <article class="billing-stat-card success">
              <div class="billing-stat-icon">
                <CheckCircle2 :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>ĐƠN THANH TOÁN THÀNH CÔNG</span>
                <strong>{{ succeededPaymentsCount }}</strong>
                <small>{{ payments.length ? Math.round((succeededPaymentsCount / payments.length) * 100) : 0 }}% tỷ lệ thành công</small>
              </div>
            </article>

            <article class="billing-stat-card premium">
              <div class="billing-stat-icon">
                <Sparkles :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>HỌC VIÊN GÓI PREMIUM</span>
                <strong>{{ premiumUsersCount }}</strong>
                <small>{{ users.length ? Math.round((premiumUsersCount / users.length) * 100) : 0 }}% tổng số người dùng</small>
              </div>
            </article>

            <article class="billing-stat-card pending">
              <div class="billing-stat-icon">
                <Layers :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>SỐ GÓI ĐANG MỞ BÁN</span>
                <strong>{{ plans.filter((p) => p.isActive).length }} / {{ plans.length }}</strong>
                <small>Gói cước trong hệ thống</small>
              </div>
            </article>
          </div>

          <!-- SUBTABS SWITCHER -->
          <div class="billing-tabs-nav" role="tablist">
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'plans' }]"
              role="tab"
              :aria-selected="billingSubTab === 'plans'"
              @click="billingSubTab = 'plans'"
            >
              <Layers :size="16" />
              <span>Quản lý các gói thanh toán</span>
              <span class="tab-badge">{{ plans.length }}</span>
            </button>
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'users' }]"
              role="tab"
              :aria-selected="billingSubTab === 'users'"
              @click="billingSubTab = 'users'"
            >
              <Users :size="16" />
              <span>Quản lý User thanh toán</span>
              <span class="tab-badge">{{ userBillings.length }}</span>
            </button>
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'transactions' }]"
              role="tab"
              :aria-selected="billingSubTab === 'transactions'"
              @click="billingSubTab = 'transactions'"
            >
              <CreditCard :size="16" />
              <span>Lịch sử giao dịch hệ thống</span>
              <span class="tab-badge">{{ payments.length }}</span>
            </button>
          </div>

          <!-- TAB 1: QUẢN LÝ CÁC GÓI THANH TOÁN -->
          <div v-if="billingSubTab === 'plans'" class="billing-tab-pane">
            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh mục gói cước…</span>
            </div>
            <div v-else-if="!plans.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Layers :size="28" />
              </div>
              <strong>Chưa có gói cước nào trong hệ thống</strong>
              <p>Bạn có thể tạo gói cước đầu tiên để học viên có thể đăng ký và nâng cấp.</p>
              <button type="button" class="admin-primary-button" style="margin-top: 10px;" @click="openPlanModal()">
                <Plus :size="15" />
                <span>Thêm gói cước mới</span>
              </button>
            </div>
            <div v-else class="billing-plans-grid">
              <article
                v-for="plan in plans"
                :key="plan.planId"
                class="billing-plan-card"
                :class="{ premium: plan.code.includes('PREMIUM'), inactive: !plan.isActive }"
              >
                <div class="plan-card-header">
                  <div>
                    <span class="plan-badge" :class="{ 'premium-badge': plan.code.includes('PREMIUM') }">
                      {{ plan.code }}
                    </span>
                    <h3>{{ plan.name }}</h3>
                  </div>
                  <div class="plan-price-tag">
                    <strong>{{ Number(plan.price) === 0 ? 'Miễn phí' : formatMoney(plan.price, plan.currency) }}</strong>
                    <small>{{ plan.currency }} / {{ plan.billingCycle === 'LIFETIME' ? 'Trọn đời' : plan.billingCycle === 'MONTHLY' ? 'Tháng' : plan.billingCycle === 'ANNUAL' ? 'Năm' : plan.billingCycle }}</small>
                  </div>
                </div>

                <p class="plan-card-desc">{{ plan.description || 'Chưa có mô tả cho gói cước này.' }}</p>

                <div class="plan-quota-badge">
                  <component :is="plan.examLimit === -1 ? Sparkles : ClipboardCheck" :size="14" />
                  <span>{{ plan.examLimit === -1 ? 'Không giới hạn lượt làm bài thi' : `Giới hạn: ${plan.examLimit} lượt thi thử` }}</span>
                </div>

                <ul class="plan-features-list">
                  <li v-for="(feat, idx) in parseFeatures(plan.featuresJson)" :key="idx">
                    <Check :size="15" />
                    <span>{{ feat }}</span>
                  </li>
                </ul>

                <div class="plan-card-footer">
                  <div class="plan-status-indicator">
                    <span class="status-dot" :class="{ active: plan.isActive }"></span>
                    <small>{{ plan.isActive ? 'Đang mở bán' : 'Tạm ẩn' }}</small>
                  </div>
                  <div class="plan-actions">
                    <button
                      type="button"
                      class="table-action"
                      :title="plan.isActive ? 'Tạm ẩn gói khỏi trang Bảng giá' : 'Mở bán lại gói này'"
                      @click="togglePlanStatus(plan)"
                    >
                      {{ plan.isActive ? 'Tạm ẩn' : 'Mở bán' }}
                    </button>
                    <button
                      type="button"
                      class="table-action"
                      title="Chỉnh sửa thông tin gói cước"
                      @click="openPlanModal(plan)"
                    >
                      <Pencil :size="13" />
                      <span>Sửa</span>
                    </button>
                    <button
                      v-if="!['FREE', 'PREMIUM'].includes(plan.code)"
                      type="button"
                      class="table-action danger"
                      title="Xóa gói cước"
                      @click="deletePlan(plan)"
                    >
                      <Trash2 :size="13" />
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <!-- TAB 2: QUẢN LÝ USER THANH TOÁN -->
          <div v-else-if="billingSubTab === 'users'" class="billing-tab-pane">
            <div class="admin-panel admin-table-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Danh sách học viên & Tình trạng gói cước</h2>
                  <p>Theo dõi hạn mức làm bài thi, gói dịch vụ và tổng chi tiêu của học viên.</p>
                </div>
                <div class="billing-table-filters">
                  <div class="billing-search-box">
                    <Search :size="15" />
                    <input
                      v-model="userBillingSearch"
                      type="search"
                      placeholder="Tìm theo ID, username..."
                      aria-label="Tìm học viên thanh toán"
                    />
                    <button
                      v-if="userBillingSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="userBillingSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="userBillingPlanFilter" class="billing-select-filter" aria-label="Lọc theo gói cước">
                    <option value="all">Tất cả gói</option>
                    <option value="PREMIUM">⭐ Gói Premium</option>
                    <option value="FREE">Gói Miễn Phí</option>
                  </select>
                  <button
                    v-if="userBillingSearch || userBillingPlanFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="userBillingSearch = ''; userBillingPlanFilter = 'all'"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedUserBillings.length }}</strong> / <strong>{{ filteredUserBillings.length }}</strong> tài khoản học viên (Tổng số: {{ userBillings.length }})</span>
              </div>

              <div v-if="loading" class="admin-empty admin-empty-loading">
                <LoaderCircle class="overlay-spinner" :size="24" />
                <span>Đang tải dữ liệu học viên…</span>
              </div>
              <div v-else-if="!filteredUserBillings.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <Users :size="28" />
                </div>
                <strong>Không tìm thấy tài khoản học viên phù hợp</strong>
                <p>Không có học viên nào khớp với bộ lọc tìm kiếm hiện tại.</p>
              </div>
              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap billing-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 200px;">Học viên</th>
                        <th style="min-width: 160px;">Gói cước hiện tại</th>
                        <th style="min-width: 150px;">Ngày kích hoạt</th>
                        <th style="min-width: 170px;">Lượt thi dùng</th>
                        <th style="min-width: 130px;">Tổng chi tiêu</th>
                        <th style="min-width: 110px;">Số đơn</th>
                        <th style="text-align: right; min-width: 190px;">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="user in paginatedUserBillings" :key="user.userId">
                        <td>
                          <div class="table-account">
                            <span class="account-avatar">
                              {{ user.username.slice(0, 1).toUpperCase() }}
                            </span>
                            <div class="table-account-details">
                              <strong class="account-username">{{ user.username }}</strong>
                              <small class="account-id">ID: #{{ user.userId }}</small>
                            </div>
                          </div>
                        </td>
                        <td>
                          <select
                            class="role-select plan-select"
                            :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                            :value="user.plan || 'FREE'"
                            aria-label="Thay đổi gói cước"
                            @change="updateUserPlan(user, $event.target.value)"
                          >
                            <option value="FREE">Gói Free</option>
                            <option value="PREMIUM">⭐ Gói Premium</option>
                          </select>
                        </td>
                        <td>
                          <span v-if="user.premiumActivatedAt" class="payment-time">
                            {{ formatPaymentDate(user.premiumActivatedAt) }}
                          </span>
                          <span v-else class="table-secondary">—</span>
                        </td>
                        <td>
                          <div class="quota-progress-cell">
                            <strong v-if="user.plan === 'PREMIUM'" class="unlimited-quota">
                              <Sparkles :size="13" />
                              Không giới hạn
                            </strong>
                            <span v-else class="limited-quota">
                              {{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }} / {{ user.freeExamLimit || 0 }} lượt
                            </span>
                          </div>
                        </td>
                        <td>
                          <strong class="payment-amount">{{ formatMoney(user.totalSpent) }}</strong>
                        </td>
                        <td>
                          <span class="order-count-badge">{{ user.paymentCount || 0 }} đơn</span>
                        </td>
                        <td>
                          <div class="table-row-actions" style="justify-content: flex-end;">
                            <button
                              type="button"
                              class="table-action"
                              title="Xem chi tiết các đơn thanh toán của học viên"
                              @click="viewUserPayments(user)"
                            >
                              <CreditCard :size="13" />
                              <span>Lịch sử GD</span>
                            </button>
                            <button
                              v-if="user.plan !== 'PREMIUM'"
                              type="button"
                              class="table-action"
                              title="Reset số lượt thi đã dùng về 0"
                              @click="resetUserQuota(user)"
                            >
                              <RotateCcw :size="13" />
                              <span>Reset lượt</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="billing-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="user in paginatedUserBillings"
                    :key="`b-user-${user.userId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div class="table-account">
                        <span class="account-avatar">
                          {{ user.username.slice(0, 1).toUpperCase() }}
                        </span>
                        <div class="table-account-details">
                          <strong class="account-username">{{ user.username }}</strong>
                          <small class="account-id">ID: #{{ user.userId }}</small>
                        </div>
                      </div>
                      <span class="order-count-badge">{{ user.paymentCount || 0 }} đơn</span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Gói cước:</span>
                        <select
                          class="role-select plan-select"
                          :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                          :value="user.plan || 'FREE'"
                          @change="updateUserPlan(user, $event.target.value)"
                        >
                          <option value="FREE">Gói Free</option>
                          <option value="PREMIUM">⭐ Gói Premium</option>
                        </select>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Lượt thi:</span>
                        <strong v-if="user.plan === 'PREMIUM'" class="unlimited-quota">
                          <Sparkles :size="13" />
                          Không giới hạn
                        </strong>
                        <span v-else class="limited-quota">
                          {{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }} / {{ user.freeExamLimit || 0 }} lượt
                        </span>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Tổng chi tiêu:</span>
                        <strong class="payment-amount">{{ formatMoney(user.totalSpent) }}</strong>
                      </div>

                      <div v-if="user.premiumActivatedAt" class="card-field-row">
                        <span class="card-field-label">Kích hoạt:</span>
                        <span class="payment-time">{{ formatPaymentDate(user.premiumActivatedAt) }}</span>
                      </div>
                    </div>

                    <div class="account-card-actions">
                      <button
                        type="button"
                        class="table-action mobile-card-btn"
                        @click="viewUserPayments(user)"
                      >
                        <CreditCard :size="14" />
                        <span>Lịch sử GD</span>
                      </button>
                      <button
                        v-if="user.plan !== 'PREMIUM'"
                        type="button"
                        class="table-action mobile-card-btn"
                        @click="resetUserQuota(user)"
                      >
                        <RotateCcw :size="14" />
                        <span>Reset lượt thi</span>
                      </button>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Users -->
                <div v-if="userBillingTotalPages > 1 || filteredUserBillings.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="userBillingPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                      <option :value="10">10 học viên</option>
                      <option :value="25">25 học viên</option>
                      <option :value="50">50 học viên</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="userBillingPage <= 1"
                      aria-label="Trang trước"
                      @click="userBillingPage = Math.max(1, userBillingPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ userBillingPage }}</strong> / {{ userBillingTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="userBillingPage >= userBillingTotalPages"
                      aria-label="Trang sau"
                      @click="userBillingPage = Math.min(userBillingTotalPages, userBillingPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- TAB 3: LỊCH SỬ GIAO DỊCH TOÀN HỆ THỐNG -->
          <div v-else-if="billingSubTab === 'transactions'" class="billing-tab-pane">
            <div class="admin-panel admin-table-panel billing-table-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Toàn bộ lịch sử giao dịch</h2>
                  <p>{{ filteredPayments.length }} giao dịch được ghi nhận qua cổng Stripe & PayPal.</p>
                </div>
                <div class="billing-table-filters">
                  <div class="billing-search-box">
                    <Search :size="15" />
                    <input
                      v-model="paymentSearch"
                      type="search"
                      placeholder="Tìm theo mã đơn, học viên..."
                      aria-label="Tìm kiếm giao dịch"
                    />
                    <button
                      v-if="paymentSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="paymentSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="paymentStatusFilter" class="billing-select-filter" aria-label="Lọc theo trạng thái giao dịch">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="SUCCEEDED">Thành công</option>
                    <option value="PENDING">Đang chờ</option>
                    <option value="FAILED">Thất bại</option>
                    <option value="REFUNDED">Hoàn tiền</option>
                  </select>
                  <select v-model="paymentProviderFilter" class="billing-select-filter" aria-label="Lọc theo cổng thanh toán">
                    <option value="all">Tất cả cổng</option>
                    <option value="STRIPE">Stripe</option>
                    <option value="PAYPAL">PayPal</option>
                  </select>
                  <button
                    v-if="paymentSearch || paymentStatusFilter !== 'all' || paymentProviderFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="paymentSearch = ''; paymentStatusFilter = 'all'; paymentProviderFilter = 'all'"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedPayments.length }}</strong> / <strong>{{ filteredPayments.length }}</strong> giao dịch (Tổng số: {{ payments.length }})</span>
              </div>

              <div v-if="loading" class="admin-empty admin-empty-loading">
                <LoaderCircle class="overlay-spinner" :size="24" />
                <span>Đang tải lịch sử giao dịch…</span>
              </div>
              <div v-else-if="!filteredPayments.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <CreditCard :size="28" />
                </div>
                <strong>Chưa có giao dịch thanh toán nào phù hợp</strong>
                <p>Học viên có thể nâng cấp gói cước tại trang Bảng giá (/pricing) qua cổng Stripe hoặc PayPal.</p>
              </div>
              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap billing-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 170px;">Mã giao dịch</th>
                        <th style="min-width: 190px;">Học viên</th>
                        <th style="min-width: 130px;">Cổng thanh toán</th>
                        <th style="min-width: 130px;">Số tiền</th>
                        <th style="min-width: 130px;">Trạng thái</th>
                        <th style="min-width: 150px;">Thời gian</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="payment in paginatedPayments" :key="payment.paymentId">
                        <td>
                          <span class="payment-id-badge">#PAY-{{ payment.paymentId }}</span>
                          <small v-if="payment.providerPaymentId" class="table-secondary">
                            Ref: {{ payment.providerPaymentId.slice(0, 18) }}...
                          </small>
                        </td>
                        <td>
                          <div class="table-account">
                            <span class="account-avatar">
                              {{ (payment.username || 'U').slice(0, 1).toUpperCase() }}
                            </span>
                            <div class="table-account-details">
                              <strong class="account-username">{{ payment.username || `User #${payment.userId}` }}</strong>
                              <small class="account-id">ID: #{{ payment.userId }}</small>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span class="provider-badge" :class="payment.provider?.toLowerCase()">
                            {{ payment.provider }}
                          </span>
                        </td>
                        <td>
                          <strong class="payment-amount">{{ formatMoney(payment.amount, payment.currency) }}</strong>
                        </td>
                        <td>
                          <span
                            class="payment-status-pill"
                            :class="payment.status?.toLowerCase()"
                          >
                            <span class="state-dot" aria-hidden="true"></span>
                            <span>{{
                              payment.status === 'SUCCEEDED'
                                ? 'Thành công'
                                : payment.status === 'PENDING'
                                ? 'Đang chờ'
                                : payment.status === 'FAILED'
                                ? 'Thất bại'
                                : payment.status === 'REFUNDED'
                                ? 'Đã hoàn tiền'
                                : payment.status
                            }}</span>
                          </span>
                        </td>
                        <td>
                          <span class="payment-time">{{ formatPaymentDate(payment.createdAt) }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="billing-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="payment in paginatedPayments"
                    :key="`b-pay-${payment.paymentId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div>
                        <span class="payment-id-badge">#PAY-{{ payment.paymentId }}</span>
                        <small v-if="payment.providerPaymentId" class="table-secondary" style="display: block; margin-top: 2px;">
                          Ref: {{ payment.providerPaymentId.slice(0, 16) }}...
                        </small>
                      </div>
                      <span
                        class="payment-status-pill"
                        :class="payment.status?.toLowerCase()"
                      >
                        <span class="state-dot" aria-hidden="true"></span>
                        <span>{{
                          payment.status === 'SUCCEEDED'
                            ? 'Thành công'
                            : payment.status === 'PENDING'
                            ? 'Đang chờ'
                            : payment.status === 'FAILED'
                            ? 'Thất bại'
                            : payment.status === 'REFUNDED'
                            ? 'Đã hoàn tiền'
                            : payment.status
                        }}</span>
                      </span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Học viên:</span>
                        <div class="table-account">
                          <span class="account-avatar" style="width: 26px; height: 26px; font-size: 11px;">
                            {{ (payment.username || 'U').slice(0, 1).toUpperCase() }}
                          </span>
                          <strong class="account-username">{{ payment.username || `User #${payment.userId}` }}</strong>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Cổng & Số tiền:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span class="provider-badge" :class="payment.provider?.toLowerCase()">
                            {{ payment.provider }}
                          </span>
                          <strong class="payment-amount">{{ formatMoney(payment.amount, payment.currency) }}</strong>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Thời gian:</span>
                        <span class="payment-time">{{ formatPaymentDate(payment.createdAt) }}</span>
                      </div>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Transactions -->
                <div v-if="paymentTotalPages > 1 || filteredPayments.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="paymentPageSize" class="pagination-select" aria-label="Số giao dịch mỗi trang">
                      <option :value="10">10 giao dịch</option>
                      <option :value="25">25 giao dịch</option>
                      <option :value="50">50 giao dịch</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="paymentPage <= 1"
                      aria-label="Trang trước"
                      @click="paymentPage = Math.max(1, paymentPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ paymentPage }}</strong> / {{ paymentTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="paymentPage >= paymentTotalPages"
                      aria-label="Trang sau"
                      @click="paymentPage = Math.min(paymentTotalPages, paymentPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- KẾT QUẢ & TIẾN ĐỘ HỌC TẬP -->
        <section v-else-if="activeSection === 'results'" class="admin-results-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel results-hero-panel">
            <div class="results-hero-content">
              <div class="results-hero-badge">
                <BarChart3 :size="15" />
                <span>KẾT QUẢ & TIẾN ĐỘ HỌC TẬP</span>
              </div>
              <h2>Báo cáo kết quả và tiến độ học tập</h2>
              <p>Theo dõi lịch sử làm bài thi, phân tích kỹ năng và đánh giá tiến độ điểm số của từng học viên.</p>
            </div>
            <div class="results-hero-controls">
              <div class="results-filter-control">
                <span class="control-label"><User :size="14" /> Học viên:</span>
                <select v-model="selectedLearnerId" class="results-select" @change="learnerCertificateId = ''; loadLearnerReport()">
                  <option value="" disabled>Chọn tài khoản học viên</option>
                  <option v-for="student in students" :key="student.userId" :value="String(student.userId)">
                    {{ student.username }} (#{{ student.userId }})
                  </option>
                </select>
              </div>

              <div v-if="programs.length" class="results-filter-control">
                <span class="control-label"><GraduationCap :size="14" /> Kỳ thi:</span>
                <select v-model="learnerCertificateId" class="results-select" @change="loadLearnerReport">
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                    {{ program.name }} ({{ program.code }})
                  </option>
                </select>
              </div>

              <button
                type="button"
                class="admin-secondary-button"
                :disabled="learnerLoading"
                title="Tải lại báo cáo học tập"
                @click="loadLearnerReport"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': learnerLoading }" />
                <span>Làm mới</span>
              </button>
            </div>
          </div>

          <!-- ERROR STATE -->
          <div v-if="learnerError" class="admin-alert-error" role="alert">
            <Info :size="16" />
            <span>{{ learnerError }}</span>
          </div>

          <!-- LOADING STATE -->
          <div v-if="learnerLoading" class="admin-empty admin-empty-loading">
            <LoaderCircle class="overlay-spinner" :size="24" />
            <span>Đang tải báo cáo học tập của học viên…</span>
          </div>

          <!-- LEARNER SELECTED -->
          <template v-else-if="selectedLearner">
            <!-- 4 SUMMARY KPI CARDS -->
            <div class="results-metrics-grid">
              <article class="results-stat-card blue">
                <div class="results-stat-icon">
                  <CheckCircle2 :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>LƯỢT THI ĐÃ HOÀN THÀNH</span>
                  <strong>{{ completedLearnerAttempts.length }}</strong>
                  <small>{{ learnerScopedAttempts.length }} lượt thi trong kỳ thi đang chọn</small>
                </div>
              </article>

              <article class="results-stat-card emerald">
                <div class="results-stat-icon">
                  <TrendingUp :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>ĐIỂM TRUNG BÌNH</span>
                  <strong>{{ learnerAverageScore == null ? '—' : `${Math.round(learnerAverageScore)}%` }}</strong>
                  <small>Tỷ lệ điểm các lượt thi đã chấm</small>
                </div>
              </article>

              <article class="results-stat-card purple">
                <div class="results-stat-icon">
                  <Sparkles :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>LẦN THI GẦN NHẤT</span>
                  <strong>{{ learnerLatestAttempt ? formatAdminNumber(learnerLatestAttempt.bandScore ?? learnerLatestAttempt.score) : '—' }}</strong>
                  <small>{{ learnerLatestAttempt ? formatAttemptDate(learnerLatestAttempt.submittedAt || learnerLatestAttempt.startedAt) : 'Chưa có lượt thi' }}</small>
                </div>
              </article>

              <article class="results-stat-card amber">
                <div class="results-stat-icon">
                  <BarChart3 :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>KỸ NĂNG CÓ DỮ LIỆU</span>
                  <strong>{{ learnerSkillCount }}</strong>
                  <small>Đo lường từ kết quả các bài thi đã nộp</small>
                </div>
              </article>
            </div>

            <!-- LEARNER ATTEMPTS HISTORY PANEL -->
            <div class="admin-panel results-history-panel">
              <div class="admin-panel-heading results-history-heading">
                <div>
                  <h2>Lịch sử làm bài thi</h2>
                  <p>{{ selectedLearner.username }} · {{ filteredLearnerAttempts.length }} lượt làm bài phù hợp</p>
                </div>
                <div class="results-table-filters">
                  <div class="results-search-box">
                    <Search :size="15" />
                    <input
                      v-model="attemptSearch"
                      type="search"
                      placeholder="Tìm bài thi hoặc mã lượt thi..."
                      aria-label="Tìm kiếm bài thi"
                    />
                    <button
                      v-if="attemptSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="attemptSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="attemptStatusFilter" class="results-select-filter" aria-label="Lọc trạng thái bài thi">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="GRADED">Đã chấm điểm</option>
                    <option value="SUBMITTED">Đã nộp bài</option>
                    <option value="IN_PROGRESS">Đang làm bài</option>
                  </select>
                  <button
                    v-if="attemptSearch || attemptStatusFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="resetAttemptFilters"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedLearnerAttempts.length }}</strong> / <strong>{{ filteredLearnerAttempts.length }}</strong> lượt thi</span>
              </div>

              <div v-if="!learnerScopedAttempts.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <ClipboardCheck :size="28" />
                </div>
                <strong>Chưa có lượt làm bài nào</strong>
                <p>Học viên "{{ selectedLearner.username }}" chưa thực hiện lượt làm bài nào trong kỳ thi đang chọn.</p>
              </div>

              <div v-else-if="!filteredLearnerAttempts.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <Search :size="28" />
                </div>
                <strong>Không tìm thấy bài thi phù hợp</strong>
                <p>Không có lượt thi nào khớp với từ khóa tìm kiếm hoặc bộ lọc trạng thái hiện tại.</p>
                <button
                  type="button"
                  class="overlay-button overlay-button-secondary"
                  style="margin-top: 8px;"
                  @click="resetAttemptFilters"
                >
                  Xóa bộ lọc tìm kiếm
                </button>
              </div>

              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap results-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 220px;">Bài kiểm tra</th>
                        <th style="min-width: 160px;">Thời gian nộp</th>
                        <th style="min-width: 130px;">Trạng thái</th>
                        <th style="min-width: 120px;">Điểm số</th>
                        <th style="min-width: 90px;">Band</th>
                        <th style="min-width: 110px;">Vi phạm</th>
                        <th style="text-align: right; min-width: 130px;">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="attempt in paginatedLearnerAttempts" :key="attempt.attemptId">
                        <td>
                          <div class="results-exam-cell">
                            <strong>{{ examTitle(attempt) }}</strong>
                            <small class="table-secondary">Mã lượt: #ATT-{{ attempt.attemptId }}</small>
                          </div>
                        </td>
                        <td>
                          <span class="payment-time">{{ formatAttemptDate(attempt.submittedAt || attempt.startedAt) }}</span>
                        </td>
                        <td>
                          <span
                            class="results-status-pill"
                            :class="String(attempt.status || '').toLowerCase()"
                          >
                            <span class="state-dot" aria-hidden="true"></span>
                            <span>{{
                              attempt.status === 'GRADED'
                                ? 'Đã chấm'
                                : attempt.status === 'SUBMITTED'
                                ? 'Đã nộp'
                                : attempt.status === 'IN_PROGRESS'
                                ? 'Đang làm'
                                : attempt.status || '—'
                            }}</span>
                          </span>
                        </td>
                        <td>
                          <strong class="results-score-value">{{ formatAttemptScore(attempt) }}</strong>
                        </td>
                        <td>
                          <span class="results-band-badge">{{ attempt.bandScore ?? '—' }}</span>
                        </td>
                        <td>
                          <span v-if="attempt.violationCount > 0" class="results-violation-badge">
                            {{ attempt.violationCount }} lần
                          </span>
                          <span v-else class="results-clean-badge">0</span>
                        </td>
                        <td>
                          <div class="table-row-actions" style="justify-content: flex-end;">
                            <button
                              type="button"
                              class="table-action"
                              :disabled="!attempt.submittedAt || attempt.score == null"
                              title="Xem chi tiết kết quả bài thi"
                              @click="viewAttemptResult(attempt)"
                            >
                              <Eye :size="13" />
                              <span>Chi tiết</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="results-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="attempt in paginatedLearnerAttempts"
                    :key="`m-attempt-${attempt.attemptId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div>
                        <strong>{{ examTitle(attempt) }}</strong>
                        <small class="table-secondary" style="display: block; margin-top: 2px;">#ATT-{{ attempt.attemptId }}</small>
                      </div>
                      <span
                        class="results-status-pill"
                        :class="String(attempt.status || '').toLowerCase()"
                      >
                        <span class="state-dot" aria-hidden="true"></span>
                        <span>{{
                          attempt.status === 'GRADED'
                            ? 'Đã chấm'
                            : attempt.status === 'SUBMITTED'
                            ? 'Đã nộp'
                            : attempt.status === 'IN_PROGRESS'
                            ? 'Đang làm'
                            : attempt.status || '—'
                        }}</span>
                      </span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Điểm số / Band:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <strong class="results-score-value">{{ formatAttemptScore(attempt) }}</strong>
                          <span v-if="attempt.bandScore != null" class="results-band-badge">Band {{ attempt.bandScore }}</span>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Thời gian:</span>
                        <span class="payment-time">{{ formatAttemptDate(attempt.submittedAt || attempt.startedAt) }}</span>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Vi phạm quy chế:</span>
                        <span :class="attempt.violationCount > 0 ? 'results-violation-badge' : 'results-clean-badge'">
                          {{ attempt.violationCount ?? 0 }} lần
                        </span>
                      </div>
                    </div>

                    <div class="account-card-actions">
                      <button
                        type="button"
                        class="table-action mobile-card-btn"
                        :disabled="!attempt.submittedAt || attempt.score == null"
                        @click="viewAttemptResult(attempt)"
                      >
                        <Eye :size="14" />
                        <span>Xem chi tiết kết quả</span>
                      </button>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Attempts -->
                <div v-if="attemptTotalPages > 1 || filteredLearnerAttempts.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="attemptPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                      <option :value="10">10 lượt thi</option>
                      <option :value="25">25 lượt thi</option>
                      <option :value="50">50 lượt thi</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="attemptPage <= 1"
                      aria-label="Trang trước"
                      @click="attemptPage = Math.max(1, attemptPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ attemptPage }}</strong> / {{ attemptTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="attemptPage >= attemptTotalPages"
                      aria-label="Trang sau"
                      @click="attemptPage = Math.min(attemptTotalPages, attemptPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- TWO-COLUMN ANALYTICS: SKILLS & SCORE TREND -->
            <div class="results-analytics-row">
              <!-- SKILLS MASTERY -->
              <section class="admin-panel results-skill-panel">
                <div class="admin-panel-heading">
                  <div>
                    <h2>Tiến độ theo kỹ năng</h2>
                    <p>{{ programs.find((program) => String(program.certificateId) === String(learnerCertificateId))?.name || 'Kỳ thi đã chọn' }}</p>
                  </div>
                </div>
                <div v-if="learnerProgress?.masteryBySkillJson && Object.keys(parseJson(learnerProgress.masteryBySkillJson)).length" class="skill-progress-list">
                  <div
                    v-for="(value, skill) in parseJson(learnerProgress.masteryBySkillJson)"
                    :key="skill"
                    class="skill-progress-row"
                  >
                    <div class="skill-label-wrap">
                      <span class="skill-name">{{ skill }}</span>
                    </div>
                    <div class="skill-progress-track">
                      <i :style="{ width: `${Math.min(100, Math.max(0, Number(value) || 0))}%` }"></i>
                    </div>
                    <strong class="skill-percent-value">{{ Math.round(Number(value) || 0) }}%</strong>
                  </div>
                </div>
                <div v-else class="learner-progress-empty">
                  <BarChart3 :size="20" />
                  <span>Chưa có dữ liệu phân tích kỹ năng cho kỳ thi này.</span>
                </div>
              </section>

              <!-- SCORE TREND -->
              <section class="admin-panel results-trend-panel">
                <div class="admin-panel-heading">
                  <div>
                    <h2>Diễn biến điểm thi</h2>
                    <p>{{ learnerScoreTrend.length }} lượt đã chấm gần nhất</p>
                  </div>
                </div>
                <div v-if="learnerScoreTrend.length" class="learner-trend-chart" role="img" aria-label="Biểu đồ diễn biến điểm thi">
                  <div
                    v-for="item in learnerScoreTrend"
                    :key="item.attemptId"
                    class="learner-trend-point"
                    :title="`Lượt #${item.attemptId}: ${item.scorePercent}%`"
                  >
                    <strong>{{ Math.round(Number(item.scorePercent) || 0) }}%</strong>
                    <i :style="{ height: `${Math.max(8, Math.min(100, Number(item.scorePercent) || 0))}%` }"></i>
                    <small>{{ formatTrendDate(item.submittedAt) }}</small>
                  </div>
                </div>
                <div v-else class="learner-progress-empty">
                  <BarChart3 :size="20" />
                  <span>Chưa có điểm đã chấm để so sánh theo thời gian.</span>
                </div>
              </section>
            </div>
          </template>

          <!-- EMPTY STATE WHEN NO LEARNER SELECTED -->
          <div v-else class="admin-empty">
            <div class="empty-icon-wrap">
              <User :size="28" />
            </div>
            <strong>Chưa có tài khoản người học để hiển thị</strong>
            <p>Vui lòng tạo hoặc chọn một tài khoản học viên để xem chi tiết tiến độ.</p>
          </div>
        </section>

        <!-- CHƯƠNG TRÌNH & KỲ THI -->
        <section v-else-if="activeSection === 'programs'" class="admin-programs-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel programs-hero-panel">
            <div class="programs-hero-content">
              <div class="programs-hero-badge">
                <GraduationCap :size="15" />
                <span>CHỨNG CHỈ & KỲ THI</span>
              </div>
              <h2>Quản lý Chứng chỉ & Chương trình thi</h2>
              <p>Cấu hình danh mục chứng chỉ chuẩn hóa (IELTS, TOEIC, THPT Quốc Gia), thang điểm và bộ kỹ năng đào tạo.</p>
            </div>
            <div class="programs-hero-actions">
              <button
                type="button"
                class="admin-primary-button"
                @click="openProgram()"
              >
                <Plus :size="16" />
                <span>Tạo chứng chỉ / kỳ thi</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại danh sách chứng chỉ"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="programs-metrics-grid">
            <article class="programs-stat-card blue">
              <div class="programs-stat-icon">
                <GraduationCap :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>TỔNG CHỨNG CHỈ / KỲ THI</span>
                <strong>{{ programs.length }}</strong>
                <small>Chương trình đang áp dụng</small>
              </div>
            </article>

            <article class="programs-stat-card emerald">
              <div class="programs-stat-icon">
                <ClipboardCheck :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>TỔNG ĐỀ KIỂM TRA ĐANG CÓ</span>
                <strong>{{ exams.length }}</strong>
                <small>Đề thi phân theo kỹ năng</small>
              </div>
            </article>

            <article class="programs-stat-card purple">
              <div class="programs-stat-icon">
                <Layers :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>MẪU CATALOG CHUẨN</span>
                <strong>{{ certificatePresets.length }}</strong>
                <small>IELTS, TOEIC, THPT Quốc Gia</small>
              </div>
            </article>
          </div>

          <!-- MAIN PROGRAMS PANEL -->
          <div class="admin-panel admin-table-panel">
            <div class="admin-panel-heading">
              <div>
                <h2>Danh sách kỳ thi trong hệ thống</h2>
                <p>{{ filteredPrograms.length }} danh mục kỳ thi đang sẵn sàng phục vụ học viên.</p>
              </div>
              <div class="programs-table-filters">
                <div class="programs-search-box">
                  <Search :size="15" />
                  <input
                    v-model="programSearch"
                    type="search"
                    placeholder="Tìm theo tên hoặc mã kỳ thi..."
                    aria-label="Tìm kiếm kỳ thi"
                  />
                  <button
                    v-if="programSearch"
                    type="button"
                    class="account-search-clear"
                    aria-label="Xóa tìm kiếm"
                    @click="programSearch = ''"
                  >
                    <X :size="14" />
                  </button>
                </div>
                <button
                  v-if="programSearch"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetProgramFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <div class="account-meta-summary" style="padding: 10px 20px 14px;">
              <span>Hiển thị <strong>{{ filteredPrograms.length }}</strong> / <strong>{{ programs.length }}</strong> kỳ thi</span>
            </div>

            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách kỳ thi…</span>
            </div>

            <!-- EMPTY CATALOG PRESET STATE -->
            <div v-else-if="!programs.length" class="admin-empty">
              <div class="catalog-empty-state">
                <div class="empty-icon-wrap" style="margin-bottom: 12px;">
                  <GraduationCap :size="28" />
                </div>
                <strong>Khởi tạo catalog kỳ thi mẫu</strong>
                <p style="color: #64748B; font-size: 13.5px; margin: 4px 0 16px;">
                  Chọn bộ dữ liệu chuẩn theo thang điểm và kỹ năng trong tài liệu nghiệp vụ để khởi tạo nhanh.
                </p>
                <div class="preset-actions-grid">
                  <button
                    v-for="preset in certificatePresets"
                    :key="preset.code"
                    type="button"
                    class="preset-card-btn"
                    @click="openPreset(preset)"
                  >
                    <div class="preset-card-icon">
                      <GraduationCap :size="18" />
                    </div>
                    <div class="preset-card-text">
                      <strong>{{ preset.name }}</strong>
                      <small>{{ preset.code }} · Thang {{ preset.totalScore }} điểm</small>
                    </div>
                  </button>
                </div>
              </div>
            </div>

            <div v-else-if="!filteredPrograms.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Search :size="28" />
              </div>
              <strong>Không tìm thấy kỳ thi phù hợp</strong>
              <p>Không có chương trình nào khớp với từ khóa tìm kiếm "{{ programSearch }}".</p>
              <button
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetProgramFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <div v-else>
              <!-- Desktop Table View -->
              <div class="admin-table-wrap programs-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 220px;">Chứng chỉ / Kỳ thi</th>
                      <th style="min-width: 100px;">Mã</th>
                      <th style="min-width: 200px;">Mô tả</th>
                      <th style="min-width: 120px;">Tổng điểm</th>
                      <th style="min-width: 130px;">Điểm đạt / Band</th>
                      <th style="min-width: 220px;">Kỹ năng / Môn</th>
                      <th style="min-width: 100px;">Số đề thi</th>
                      <th style="text-align: right; min-width: 150px;">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="program in filteredPrograms" :key="program.certificateId">
                      <td>
                        <div class="program-title-cell">
                          <strong>{{ program.name }}</strong>
                        </div>
                      </td>
                      <td>
                        <span class="program-code-badge">{{ program.code }}</span>
                      </td>
                      <td class="description-cell">
                        <span :title="program.description">{{ program.description || '—' }}</span>
                      </td>
                      <td>
                        <span class="program-score-pill">{{ program.totalScore }} điểm</span>
                      </td>
                      <td>
                        <span class="program-passing-pill">{{ program.passingBand || program.passingScore }}</span>
                      </td>
                      <td>
                        <div class="program-skills-tags">
                          <span
                            v-for="sk in parseSkillArray(program.skills)"
                            :key="sk.code"
                            class="skill-chip-tag"
                          >
                            {{ sk.label }}
                          </span>
                        </div>
                      </td>
                      <td>
                        <span class="order-count-badge">
                          {{ exams.filter((e) => e.certificateId === program.certificateId).length }} đề
                        </span>
                      </td>
                      <td>
                        <div class="table-row-actions" style="justify-content: flex-end;">
                          <button
                            type="button"
                            class="table-action"
                            title="Chỉnh sửa kỳ thi"
                            @click="openProgram(program)"
                          >
                            <Pencil :size="13" />
                            <span>Sửa</span>
                          </button>
                          <button
                            type="button"
                            class="table-action danger"
                            title="Xóa kỳ thi"
                            @click="deleteProgram(program)"
                          >
                            <Trash2 :size="13" />
                            <span>Xóa</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="programs-mobile-grid" style="padding: 14px;">
                <article
                  v-for="program in filteredPrograms"
                  :key="`m-prog-${program.certificateId}`"
                  class="account-mobile-card"
                >
                  <div class="account-card-header">
                    <div>
                      <strong>{{ program.name }}</strong>
                      <small class="table-secondary" style="display: block; margin-top: 2px;">{{ program.description || 'Chương trình luyện thi' }}</small>
                    </div>
                    <span class="program-code-badge">{{ program.code }}</span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Thang điểm:</span>
                      <div>
                        <span>Tổng: {{ program.totalScore }} điểm · Đạt: {{ program.passingBand || program.passingScore }}</span>
                      </div>
                    </div>

                    <div class="card-field-row">
                      <span class="card-field-label">Số đề thi:</span>
                      <span class="order-count-badge">
                        {{ exams.filter((e) => e.certificateId === program.certificateId).length }} đề thi
                      </span>
                    </div>

                    <div class="card-field-row" style="align-items: flex-start;">
                      <span class="card-field-label">Kỹ năng:</span>
                      <div class="program-skills-tags" style="justify-content: flex-end;">
                        <span
                          v-for="sk in parseSkillArray(program.skills)"
                          :key="sk.code"
                          class="skill-chip-tag"
                        >
                          {{ sk.label }}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div class="account-card-actions">
                    <button
                      type="button"
                      class="table-action mobile-card-btn"
                      @click="openProgram(program)"
                    >
                      <Pencil :size="14" />
                      <span>Chỉnh sửa</span>
                    </button>
                    <button
                      type="button"
                      class="table-action mobile-card-btn danger"
                      @click="deleteProgram(program)"
                    >
                      <Trash2 :size="14" />
                      <span>Xóa kỳ thi</span>
                    </button>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>

        <!-- BÀI KIỂM TRA & ĐỀ THI -->
        <section v-else-if="activeSection === 'exams'" class="admin-exams-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel exams-hero-panel">
            <div class="exams-hero-content">
              <div class="exams-hero-badge">
                <ClipboardCheck :size="15" />
                <span>DANH MỤC BÀI KIỂM TRA</span>
              </div>
              <h2>Quản trị Đề thi & Bài kiểm tra</h2>
              <p>Quản lý toàn bộ đề thi chuẩn hóa, cấu hình câu hỏi, phân loại theo kỹ năng và phát hành cho học viên.</p>
            </div>
            <div class="exams-hero-actions">
              <button
                type="button"
                class="admin-primary-button"
                :disabled="!programs.length || !topics.length"
                @click="showExamBuilder = true"
              >
                <Plus :size="16" />
                <span>Tạo bài kiểm tra</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại danh sách đề thi"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="exams-metrics-grid">
            <article class="exams-stat-card blue">
              <div class="exams-stat-icon">
                <ClipboardCheck :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>TỔNG SỐ ĐỀ THI</span>
                <strong>{{ exams.length }}</strong>
                <small>Đề kiểm tra trên hệ thống</small>
              </div>
            </article>

            <article class="exams-stat-card emerald">
              <div class="exams-stat-icon">
                <CheckCircle2 :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>ĐÃ PHÁT HÀNH</span>
                <strong>{{ exams.filter((e) => e.status === 'PUBLISHED').length }}</strong>
                <small>Học viên có thể làm bài</small>
              </div>
            </article>

            <article class="exams-stat-card amber">
              <div class="exams-stat-icon">
                <FileText :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>BẢN NHÁP / ĐANG XÂY</span>
                <strong>{{ exams.filter((e) => e.status !== 'PUBLISHED').length }}</strong>
                <small>Chưa mở cho người học</small>
              </div>
            </article>

            <article class="exams-stat-card purple">
              <div class="exams-stat-icon">
                <GraduationCap :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>CHƯƠNG TRÌNH ÁP DỤNG</span>
                <strong>{{ new Set(exams.map((e) => e.certificateId)).size }}</strong>
                <small>Kỳ thi có bộ đề</small>
              </div>
            </article>
          </div>

          <!-- MAIN EXAMS PANEL -->
          <div class="admin-panel admin-table-panel">
            <div class="admin-panel-heading">
              <div>
                <h2>Danh sách đề kiểm tra</h2>
                <p>{{ filteredExams.length }} đề thi sẵn sàng đánh giá năng lực học viên.</p>
              </div>
              <div class="exams-table-filters">
                <div class="exams-search-box">
                  <Search :size="15" />
                  <input
                    v-model="examSearch"
                    type="search"
                    placeholder="Tìm theo tiêu đề, mô tả..."
                    aria-label="Tìm kiếm đề thi"
                  />
                  <button
                    v-if="examSearch"
                    type="button"
                    class="account-search-clear"
                    aria-label="Xóa tìm kiếm"
                    @click="examSearch = ''"
                  >
                    <X :size="14" />
                  </button>
                </div>
                <select v-model="examProgramFilter" class="exams-select-filter" aria-label="Lọc theo kỳ thi">
                  <option value="all">Tất cả kỳ thi</option>
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                    {{ program.name }} ({{ program.code }})
                  </option>
                </select>
                <select v-model="examStatusFilter" class="exams-select-filter" aria-label="Lọc theo trạng thái">
                  <option value="all">Tất cả trạng thái</option>
                  <option value="PUBLISHED">Đã phát hành</option>
                  <option value="DRAFT">Bản nháp</option>
                </select>
                <button
                  v-if="examSearch || examProgramFilter !== 'all' || examStatusFilter !== 'all'"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetExamFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <div class="account-meta-summary" style="padding: 10px 20px 14px;">
              <span>Hiển thị <strong>{{ paginatedExams.length }}</strong> / <strong>{{ filteredExams.length }}</strong> đề kiểm tra</span>
            </div>

            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách bài kiểm tra…</span>
            </div>

            <div v-else-if="!exams.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <ClipboardCheck :size="28" />
              </div>
              <strong>Chưa có bài kiểm tra nào trong hệ thống</strong>
              <p>Bắt đầu tạo bài kiểm tra đầu tiên để phục vụ luyện tập và đánh giá năng lực học viên.</p>
              <button
                type="button"
                class="admin-primary-button"
                style="margin-top: 10px;"
                :disabled="!programs.length || !topics.length"
                @click="showExamBuilder = true"
              >
                <Plus :size="15" />
                <span>Tạo bài kiểm tra đầu tiên</span>
              </button>
            </div>

            <div v-else-if="!filteredExams.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Search :size="28" />
              </div>
              <strong>Không tìm thấy bài kiểm tra phù hợp</strong>
              <p>Không có đề thi nào khớp với bộ lọc hoặc từ khóa tìm kiếm hiện tại.</p>
              <button
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetExamFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <div v-else>
              <!-- Desktop Table View -->
              <div class="admin-table-wrap exams-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 240px;">Đề kiểm tra</th>
                      <th style="min-width: 190px;">Kỳ thi / Kỹ năng</th>
                      <th style="min-width: 140px;">Mục tiêu</th>
                      <th style="min-width: 120px;">Thời lượng</th>
                      <th style="min-width: 110px;">Số câu</th>
                      <th style="min-width: 130px;">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="exam in paginatedExams" :key="exam.examId">
                      <td>
                        <div class="results-exam-cell">
                          <strong>{{ exam.title }}</strong>
                          <small class="table-secondary">
                            {{ exam.description || 'Bài đánh giá học tập theo kỹ năng' }}
                          </small>
                        </div>
                      </td>
                      <td>
                        <div class="exam-program-cell">
                          <span class="exam-prog-name">{{ programName(exam.certificateId) }}</span>
                          <span class="exam-topic-name">· {{ topicName(exam.topicId) }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="program-passing-pill">
                          {{ exam.targetScoreMin != null ? `${exam.targetScoreMin}–${exam.targetScoreMax} ${exam.targetScoreUnit || ''}` : '—' }}
                        </span>
                      </td>
                      <td>
                        <span class="exam-duration-badge">
                          {{ exam.durationMinutes || 0 }} phút
                        </span>
                      </td>
                      <td>
                        <span class="order-count-badge">
                          {{ exam.questionCount || exam.totalQuestions || 0 }} câu
                        </span>
                      </td>
                      <td>
                        <span
                          class="results-status-pill"
                          :class="exam.status === 'PUBLISHED' ? 'published' : 'draft'"
                        >
                          <span class="state-dot" aria-hidden="true"></span>
                          <span>{{ exam.status === 'PUBLISHED' ? 'Đã phát hành' : exam.status || 'Bản nháp' }}</span>
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="exams-mobile-grid" style="padding: 14px;">
                <article
                  v-for="exam in paginatedExams"
                  :key="`m-exam-${exam.examId}`"
                  class="account-mobile-card"
                >
                  <div class="account-card-header">
                    <div>
                      <strong>{{ exam.title }}</strong>
                      <small class="table-secondary" style="display: block; margin-top: 2px;">
                        {{ programName(exam.certificateId) }} · {{ topicName(exam.topicId) }}
                      </small>
                    </div>
                    <span
                      class="results-status-pill"
                      :class="exam.status === 'PUBLISHED' ? 'published' : 'draft'"
                    >
                      <span class="state-dot" aria-hidden="true"></span>
                      <span>{{ exam.status === 'PUBLISHED' ? 'Đã phát hành' : exam.status || 'Bản nháp' }}</span>
                    </span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Thời lượng & Số câu:</span>
                      <div style="display: flex; gap: 8px;">
                        <span class="exam-duration-badge">{{ exam.durationMinutes || 0 }} phút</span>
                        <span class="order-count-badge">{{ exam.questionCount || exam.totalQuestions || 0 }} câu</span>
                      </div>
                    </div>

                    <div class="card-field-row">
                      <span class="card-field-label">Điểm mục tiêu:</span>
                      <span class="program-passing-pill">
                        {{ exam.targetScoreMin != null ? `${exam.targetScoreMin}–${exam.targetScoreMax} ${exam.targetScoreUnit || ''}` : '—' }}
                      </span>
                    </div>

                    <div v-if="exam.description" class="card-field-row">
                      <span class="card-field-label">Mô tả:</span>
                      <small class="table-secondary">{{ exam.description }}</small>
                    </div>
                  </div>
                </article>
              </div>

              <!-- Pagination for Exams -->
              <div v-if="examTotalPages > 1 || filteredExams.length > 10" class="account-pagination" style="padding: 14px 20px;">
                <div class="pagination-size">
                  <span>Hiển thị</span>
                  <select v-model.number="examPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                    <option :value="10">10 đề thi</option>
                    <option :value="25">25 đề thi</option>
                    <option :value="50">50 đề thi</option>
                  </select>
                </div>
                <div class="pagination-nav">
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="examPage <= 1"
                    aria-label="Trang trước"
                    @click="examPage = Math.max(1, examPage - 1)"
                  >
                    <ChevronLeft :size="16" />
                    <span>Trước</span>
                  </button>
                  <span class="pagination-status">Trang <strong>{{ examPage }}</strong> / {{ examTotalPages }}</span>
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="examPage >= examTotalPages"
                    aria-label="Trang sau"
                    @click="examPage = Math.min(examTotalPages, examPage + 1)"
                  >
                    <span>Sau</span>
                    <ChevronRight :size="16" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section v-else class="admin-knowledge-section">
          <!-- HERO PANEL -->
          <div class="knowledge-hero-panel">
            <div class="knowledge-hero-content">
              <div class="knowledge-hero-badge">
                <Sparkles :size="14" />
                <span>KHO TRI THỨC AI · RAG ENGINE</span>
              </div>
              <h1 class="knowledge-hero-title">Kho tri thức & Quản lý tài liệu RAG</h1>
              <p class="knowledge-hero-desc">
                Tải lên và quản lý tài liệu học tập PDF, tự động phân tách đoạn tri thức (chunks) làm nguồn dữ liệu chuẩn cho Trợ lý gia sư AI và thuật toán sinh đề thi thích ứng.
              </p>
            </div>
            <div class="knowledge-hero-actions">
              <button
                type="button"
                class="knowledge-btn-secondary"
                :disabled="loading"
                title="Tải lại dữ liệu"
                @click="loadData"
              >
                <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
                <span>Tải lại</span>
              </button>
              <a href="#kb-add" class="knowledge-btn-primary">
                <Upload :size="16" />
                <span>Nạp tài liệu mới</span>
              </a>
            </div>
          </div>

          <!-- 4 KPI METRICS -->
          <div class="knowledge-metrics-grid">
            <div class="knowledge-stat-card">
              <div class="knowledge-stat-header">
                <span class="knowledge-stat-label">Tổng số tài liệu</span>
                <div class="knowledge-stat-icon icon-blue">
                  <FileText :size="18" />
                </div>
              </div>
              <div class="knowledge-stat-value">{{ formatAdminNumber(knowledgeDocuments.length) }}</div>
              <div class="knowledge-stat-hint">Tệp PDF được nạp vào kho</div>
            </div>

            <div class="knowledge-stat-card">
              <div class="knowledge-stat-header">
                <span class="knowledge-stat-label">Đang sử dụng</span>
                <div class="knowledge-stat-icon icon-emerald">
                  <CircleCheck :size="18" />
                </div>
              </div>
              <div class="knowledge-stat-value">{{ formatAdminNumber(activeKnowledgeCount) }}</div>
              <div class="knowledge-stat-hint">
                Chiếm <strong>{{ activeKnowledgePercent }}%</strong> tổng tài liệu
              </div>
            </div>

            <div class="knowledge-stat-card">
              <div class="knowledge-stat-header">
                <span class="knowledge-stat-label">Đã tạm tắt</span>
                <div class="knowledge-stat-icon icon-amber">
                  <CirclePause :size="18" />
                </div>
              </div>
              <div class="knowledge-stat-value">{{ formatAdminNumber(inactiveKnowledgeCount) }}</div>
              <div class="knowledge-stat-hint">Không dùng khi truy xuất & tạo câu hỏi</div>
            </div>

            <div class="knowledge-stat-card">
              <div class="knowledge-stat-header">
                <span class="knowledge-stat-label">Tổng đoạn tri thức (chunks)</span>
                <div class="knowledge-stat-icon icon-purple">
                  <Database :size="18" />
                </div>
              </div>
              <div class="knowledge-stat-value">{{ formatAdminNumber(knowledgeChunkCount) }}</div>
              <div class="knowledge-stat-hint">Vector chunks được phân tách từ PDF</div>
            </div>
          </div>

          <!-- 2-COLUMN WORKSPACE: LEFT UPLOAD FORM, RIGHT DOCUMENT LIBRARY -->
          <div class="knowledge-workspace-grid">
            <!-- CỘT TRÁI: FORM NẠP TÀI LIỆU -->
            <div id="kb-add" class="knowledge-panel knowledge-import-panel">
              <div class="knowledge-panel-header">
                <div class="knowledge-panel-title-group">
                  <div class="knowledge-panel-icon-box">
                    <Upload :size="18" />
                  </div>
                  <div>
                    <h2 class="knowledge-panel-title">Nạp tài liệu PDF vào kho</h2>
                    <p class="knowledge-panel-subtitle">Gán theo kỳ thi và kỹ năng để tạo ngữ cảnh chuẩn xác</p>
                  </div>
                </div>
              </div>

              <form class="knowledge-upload-form" @submit.prevent="importKnowledge">
                <!-- DROPZONE -->
                <label
                  class="knowledge-dropzone"
                  :class="{ 'is-dragging': knowledgeDragging }"
                  @dragenter.prevent="knowledgeDragging = true"
                  @dragover.prevent="knowledgeDragging = true"
                  @dragleave="knowledgeDragging = false"
                  @drop="dropKnowledgeFiles"
                >
                  <input type="file" accept=".pdf,application/pdf" multiple @change="readKnowledgeFiles" />
                  <div class="knowledge-dropzone-icon">
                    <Upload :size="24" />
                  </div>
                  <strong class="knowledge-dropzone-title">Kéo thả tệp PDF vào đây hoặc bấm để duyệt tệp</strong>
                  <span class="knowledge-dropzone-subtitle">Hỗ trợ định dạng .PDF · Tối đa 20 MB mỗi tệp</span>
                </label>
                <p v-if="knowledgeFormErrors.files" class="knowledge-form-error" role="alert">
                  {{ knowledgeFormErrors.files }}
                </p>

                <!-- KỲ THI / CHỨNG CHỈ -->
                <div class="knowledge-field-group">
                  <label class="knowledge-field-label">
                    Kỳ thi / Chương trình áp dụng <span class="required-star">*</span>
                  </label>
                  <select
                    v-model="knowledgeForm.certificateId"
                    required
                    class="knowledge-select-input"
                    :class="{ 'has-error': knowledgeFormErrors.exam }"
                    @change="knowledgeFormErrors.exam = ''"
                  >
                    <option disabled value="">-- Chọn kỳ thi áp dụng --</option>
                    <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                      {{ program.name }} ({{ program.code }})
                    </option>
                  </select>
                  <p v-if="knowledgeFormErrors.exam" class="knowledge-form-error" role="alert">
                    {{ knowledgeFormErrors.exam }}
                  </p>
                </div>

                <!-- KỸ NĂNG / MÔN THI (TÙY CHỌN) -->
                <div v-if="knowledgeTopics.length" class="knowledge-field-group">
                  <div class="knowledge-field-label-row">
                    <label class="knowledge-field-label">Kỹ năng / Môn thi</label>
                    <span class="knowledge-field-sub">Tùy chọn</span>
                  </div>
                  <div class="knowledge-topic-chips" role="group">
                    <button
                      type="button"
                      class="knowledge-topic-chip"
                      :class="{ active: !knowledgeForm.topicId }"
                      @click="knowledgeForm.topicId = ''"
                    >
                      <Check v-if="!knowledgeForm.topicId" :size="14" />
                      <span>Tất cả kỹ năng</span>
                    </button>
                    <button
                      v-for="topic in knowledgeTopics"
                      :key="topic.topicId"
                      type="button"
                      class="knowledge-topic-chip"
                      :class="{ active: String(topic.topicId) === knowledgeForm.topicId }"
                      @click="knowledgeForm.topicId = String(topic.topicId)"
                    >
                      <Check v-if="String(topic.topicId) === knowledgeForm.topicId" :size="14" />
                      <span>{{ topic.name }}</span>
                    </button>
                  </div>
                </div>

                <!-- QUEUED FILES LIST -->
                <div v-if="knowledgeFiles.length" class="knowledge-queue-section">
                  <div class="knowledge-queue-header">
                    <span>Tệp đang chờ nạp ({{ knowledgeFiles.length }})</span>
                    <button type="button" class="knowledge-queue-clear" @click="knowledgeFiles = []">Xóa tất cả</button>
                  </div>
                  <div class="knowledge-queue-list">
                    <div v-for="item in knowledgeFiles" :key="item.id" class="knowledge-queue-item">
                      <div class="knowledge-queue-file-icon">
                        <FileText :size="18" />
                      </div>
                      <div class="knowledge-queue-file-details">
                        <div class="knowledge-queue-filename" :title="item.file.name">{{ item.file.name }}</div>
                        <div class="knowledge-queue-filesize">{{ (item.file.size / 1024 / 1024).toFixed(2) }} MB</div>
                      </div>
                      <div class="knowledge-queue-title-input-wrap">
                        <input
                          v-model.trim="item.title"
                          type="text"
                          placeholder="Tiêu đề tài liệu..."
                          required
                          maxlength="200"
                          class="knowledge-queue-title-input"
                          :class="{ 'has-error': knowledgeFormErrors.titles && !item.title.trim() }"
                          @input="knowledgeFormErrors.titles = ''"
                        />
                      </div>
                      <button
                        type="button"
                        class="knowledge-queue-remove-btn"
                        :disabled="importingKnowledge"
                        title="Bỏ tệp này"
                        @click="removeKnowledgeFile(item.id)"
                      >
                        <X :size="16" />
                      </button>
                    </div>
                  </div>
                  <p v-if="knowledgeFormErrors.titles" class="knowledge-form-error" role="alert">
                    {{ knowledgeFormErrors.titles }}
                  </p>
                </div>

                <!-- SUCCESS NOTICE -->
                <div v-if="knowledgeNotice" class="knowledge-success-banner" role="status">
                  <CheckCircle2 :size="18" />
                  <span>Đã nạp thành công <strong>“{{ knowledgeNotice }}”</strong> vào kho tri thức.</span>
                  <button type="button" class="knowledge-banner-close" aria-label="Đóng thông báo" @click="closeKnowledgeNotice">
                    <X :size="15" />
                  </button>
                </div>

                <!-- FORM FOOTER -->
                <div class="knowledge-form-footer">
                  <div class="knowledge-footer-hint">
                    <Info :size="15" />
                    <span>Tài liệu sau khi tải lên sẽ tự động phân tách thành các đoạn tri thức (chunks).</span>
                  </div>
                  <div class="knowledge-footer-actions">
                    <button
                      type="button"
                      class="knowledge-btn-secondary"
                      :disabled="importingKnowledge"
                      @click="clearKnowledgeForm"
                    >
                      Đặt lại
                    </button>
                    <button
                      type="submit"
                      class="knowledge-btn-primary"
                      :disabled="importingKnowledge || !knowledgeFiles.length"
                    >
                      <LoaderCircle v-if="importingKnowledge" :size="16" class="animate-spin" />
                      <Database v-else :size="16" />
                      <span>{{ importingKnowledge ? 'Đang nạp & phân tách...' : 'Đưa vào kho tri thức' }}</span>
                    </button>
                  </div>
                </div>
              </form>
            </div>

            <!-- CỘT PHẢI: DANH SÁCH TÀI LIỆU ĐÃ NẠP -->
            <div class="knowledge-panel knowledge-library-panel">
              <div class="knowledge-panel-header library-panel-header">
                <div class="knowledge-panel-title-group">
                  <div class="knowledge-panel-icon-box icon-box-library">
                    <Database :size="18" />
                  </div>
                  <div>
                    <h2 class="knowledge-panel-title">Danh mục tài liệu trong kho</h2>
                    <p class="knowledge-panel-subtitle">Nguồn dữ liệu tham khảo cho AI phân tích và tạo bài</p>
                  </div>
                </div>
                <span class="knowledge-count-badge">
                  {{ formatAdminNumber(filteredKnowledgeDocuments.length) }} tài liệu
                </span>
              </div>

              <!-- TOOLBAR -->
              <div class="knowledge-library-toolbar">
                <div class="knowledge-search-wrap">
                  <Search :size="16" class="knowledge-search-icon" />
                  <input
                    v-model="knowledgeSearch"
                    type="search"
                    placeholder="Tìm theo tên tài liệu, tệp PDF, kỳ thi..."
                    class="knowledge-search-input"
                    aria-label="Tìm kiếm trong kho tri thức"
                  />
                </div>

                <div class="knowledge-filters-row">
                  <div class="knowledge-filter-pills" role="tablist">
                    <button
                      type="button"
                      class="knowledge-filter-pill"
                      :class="{ active: knowledgeFilter === 'all' }"
                      @click="knowledgeFilter = 'all'"
                    >
                      <span>Tất cả</span>
                      <span class="pill-count">{{ formatAdminNumber(knowledgeDocuments.length) }}</span>
                    </button>
                    <button
                      type="button"
                      class="knowledge-filter-pill"
                      :class="{ active: knowledgeFilter === 'active' }"
                      @click="knowledgeFilter = 'active'"
                    >
                      <span>Đang dùng</span>
                      <span class="pill-count">{{ formatAdminNumber(activeKnowledgeCount) }}</span>
                    </button>
                    <button
                      type="button"
                      class="knowledge-filter-pill"
                      :class="{ active: knowledgeFilter === 'inactive' }"
                      @click="knowledgeFilter = 'inactive'"
                    >
                      <span>Đã tắt</span>
                      <span class="pill-count">{{ formatAdminNumber(inactiveKnowledgeCount) }}</span>
                    </button>
                  </div>

                  <div v-if="programs.length" class="knowledge-program-filter-wrap">
                    <select v-model="knowledgeProgramFilter" class="knowledge-program-select">
                      <option value="all">Tất cả kỳ thi</option>
                      <option v-for="prog in programs" :key="prog.certificateId" :value="String(prog.certificateId)">
                        {{ prog.code || prog.name }}
                      </option>
                    </select>
                  </div>
                </div>
              </div>

              <!-- DOCUMENT LIST -->
              <div v-if="!knowledgeDocuments.length" class="knowledge-empty-state">
                <div class="knowledge-empty-icon">
                  <Database :size="28" />
                </div>
                <h3 class="knowledge-empty-title">Kho tri thức đang trống</h3>
                <p class="knowledge-empty-desc">Chưa có tài liệu PDF nào được tải lên. Hãy nạp tài liệu ở khung bên trái để AI có dữ liệu sinh đề và giải thích.</p>
                <a href="#kb-add" class="knowledge-empty-action">
                  <Upload :size="15" />
                  <span>Nạp tài liệu đầu tiên</span>
                </a>
              </div>

              <div v-else-if="!filteredKnowledgeDocuments.length" class="knowledge-empty-state compact">
                <div class="knowledge-empty-icon">
                  <Search :size="22" />
                </div>
                <h3 class="knowledge-empty-title">Không tìm thấy tài liệu phù hợp</h3>
                <p class="knowledge-empty-desc">Thử điều chỉnh từ khóa tìm kiếm hoặc bộ lọc trạng thái kỳ thi.</p>
                <button type="button" class="knowledge-empty-action" @click="resetKnowledgeFilters">
                  <RotateCcw :size="14" />
                  <span>Đặt lại bộ lọc</span>
                </button>
              </div>

              <div v-else class="knowledge-documents-container">
                <div class="knowledge-document-cards">
                  <div
                    v-for="doc in paginatedKnowledgeDocuments"
                    :key="doc.documentId"
                    class="knowledge-doc-card"
                    :class="{ 'is-disabled': !doc.active }"
                  >
                    <div class="knowledge-doc-type-badge">
                      <span>PDF</span>
                    </div>
                    <div class="knowledge-doc-body">
                      <div class="knowledge-doc-title-row">
                        <h4 class="knowledge-doc-title" :title="doc.title">{{ doc.title }}</h4>
                      </div>
                      <div class="knowledge-doc-filename" :title="doc.fileName">
                        <FileText :size="13" />
                        <span>{{ doc.fileName }}</span>
                      </div>
                      <div class="knowledge-doc-tags">
                        <span class="doc-badge badge-program">
                          {{ programName(doc.certificateId) }}
                        </span>
                        <span v-if="knowledgeTopicName(doc.topicId)" class="doc-badge badge-topic">
                          {{ knowledgeTopicName(doc.topicId) }}
                        </span>
                        <span class="doc-badge badge-chunks">
                          <Sparkles :size="11" />
                          <span>{{ formatAdminNumber(doc.chunkCount) }} đoạn chunks</span>
                        </span>
                        <span class="doc-badge badge-date">
                          {{ knowledgeDate(doc.createdAt) }}
                        </span>
                      </div>
                    </div>

                    <div class="knowledge-doc-controls">
                      <!-- ACTIVE SWITCH -->
                      <div class="knowledge-toggle-group">
                        <button
                          type="button"
                          class="knowledge-ios-switch"
                          role="switch"
                          :aria-checked="doc.active"
                          :title="doc.active ? 'Bấm để tắt tài liệu' : 'Bấm để kích hoạt tài liệu'"
                          @click="toggleKnowledgeActive(doc)"
                        >
                          <span class="switch-handle"></span>
                        </button>
                        <span class="knowledge-toggle-label" :class="{ active: doc.active }">
                          {{ doc.active ? 'Đang dùng' : 'Đã tắt' }}
                        </span>
                      </div>

                      <!-- ACTIONS MENU -->
                      <DropdownMenu trigger-label="Tùy chọn tài liệu">
                        <template #trigger>
                          <button type="button" class="knowledge-more-btn" :aria-label="`Tùy chọn ${doc.title}`">
                            <MoreHorizontal :size="18" />
                          </button>
                        </template>
                        <DropdownMenuLabel>{{ doc.title }}</DropdownMenuLabel>
                        <DropdownMenuItem @select="viewKnowledgeDocument(doc)">
                          <template #icon><Eye :size="15" /></template>
                          <span>Xem đoạn kiến thức</span>
                        </DropdownMenuItem>
                        <DropdownMenuItem @select="editKnowledgeDocument(doc)">
                          <template #icon><Pencil :size="15" /></template>
                          <span>Sửa tài liệu</span>
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem destructive @select="removeKnowledge(doc)">
                          <template #icon><Trash2 :size="15" /></template>
                          <span>Xóa khỏi kho</span>
                        </DropdownMenuItem>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>

                <!-- PAGINATION -->
                <div v-if="knowledgeTotalPages > 1" class="knowledge-pagination-bar">
                  <span class="pagination-info">
                    Hiển thị <strong>{{ ((knowledgePage - 1) * knowledgePageSize) + 1 }}</strong> - <strong>{{ Math.min(knowledgePage * knowledgePageSize, filteredKnowledgeDocuments.length) }}</strong> trên <strong>{{ filteredKnowledgeDocuments.length }}</strong> tài liệu
                  </span>
                  <div class="pagination-actions">
                    <button
                      type="button"
                      class="knowledge-page-btn"
                      :disabled="knowledgePage <= 1"
                      @click="knowledgePage = Math.max(1, knowledgePage - 1)"
                    >
                      <ChevronLeft :size="15" />
                      <span>Trước</span>
                    </button>
                    <span class="knowledge-page-current">
                      {{ knowledgePage }} / {{ knowledgeTotalPages }}
                    </span>
                    <button
                      type="button"
                      class="knowledge-page-btn"
                      :disabled="knowledgePage >= knowledgeTotalPages"
                      @click="knowledgePage = Math.min(knowledgeTotalPages, knowledgePage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="15" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
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
          <select v-model="accountForm.role"><option value="ROLE_USER">Người học</option><option value="ROLE_ADMIN">Quản trị viên</option></select>
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
  /* ========================================================
     ADMIN OVERVIEW / DASHBOARD — LIGHT AI EDUCATION
     ======================================================== */
  .overview-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  /* Toolbar Controls */
  .analytics-controls {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }
  .analytics-time-filter {
    display: inline-flex;
    align-items: center;
    padding: 3px;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    gap: 2px;
  }
  .analytics-time-filter button {
    height: 32px;
    padding: 0 12px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: #64748B;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .analytics-time-filter button:hover {
    color: #0F172A;
    background: rgba(255, 255, 255, 0.6);
  }
  .analytics-time-filter button.active {
    color: #2563EB;
    background: #FFFFFF;
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  }
  .analytics-exam-filter select {
    height: 38px;
    min-width: 160px;
    max-width: 260px;
    padding: 0 32px 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: border-color 0.15s ease;
  }
  .analytics-exam-filter select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  }
  .overview-refresh-button {
    height: 38px;
  }

  /* Real-Data KPI Summary Cards */
  .overview-kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }
  .overview-kpi-card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 12px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    transition: all 0.2s ease;
  }
  .overview-kpi-card.is-interactive {
    cursor: pointer;
  }
  .overview-kpi-card.is-interactive:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);
  }
  .overview-kpi-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .overview-kpi-icon {
    display: grid;
    place-items: center;
    width: 42px;
    height: 42px;
    border-radius: 10px;
    flex-shrink: 0;
  }
  .overview-kpi-icon.icon-blue {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .overview-kpi-icon.icon-teal {
    background: #F0F9FF;
    color: #0284C7;
    border: 1px solid #E0F2FE;
  }
  .overview-kpi-icon.icon-green {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-kpi-icon.icon-purple {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .overview-kpi-action {
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    transition: transform 0.15s ease;
  }
  .overview-kpi-card.is-interactive:hover .overview-kpi-action {
    transform: translateX(2px);
  }
  .overview-kpi-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }
  .overview-kpi-label {
    font-size: 12.5px;
    font-weight: 500;
    color: #64748B;
  }
  .overview-kpi-value {
    font-size: 26px;
    font-weight: 700;
    color: #0F172A;
    line-height: 1.2;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .overview-kpi-unit {
    font-size: 14px;
    font-weight: 500;
    color: #64748B;
  }
  .overview-kpi-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #64748B;
    margin-top: 2px;
  }
  .status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    display: inline-block;
  }
  .status-dot.online {
    background: #16A34A;
    box-shadow: 0 0 0 2px #DCFCE7;
  }
  .overview-delta-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .overview-delta-badge.delta-positive {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-delta-badge.delta-negative {
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FEE2E2;
  }
  .overview-delta-badge.delta-neutral {
    background: #F1F5F9;
    color: #64748B;
    border: 1px solid #E2E8F0;
  }

  /* Status Cards (Loading & Error) */
  .overview-status-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    gap: 14px;
    padding: 44px 24px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .overview-status-card .status-text h3 {
    margin: 0 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: #0F172A;
  }
  .overview-status-card .status-text p {
    margin: 0;
    font-size: 13px;
    color: #64748B;
  }
  .overview-error-state {
    background: #FEF2F2;
    border-color: #FECACA;
    flex-direction: row;
    justify-content: space-between;
    text-align: left;
    padding: 24px 28px;
  }
  .overview-error-state .error-info {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }
  .overview-error-state .error-icon {
    color: #DC2626;
    flex-shrink: 0;
    margin-top: 2px;
  }
  .overview-error-state h3 {
    margin: 0 0 4px;
    font-size: 15px;
    font-weight: 600;
    color: #991B1B;
  }
  .overview-error-state p {
    margin: 0;
    font-size: 13px;
    color: #B91C1C;
  }

  /* Insight & Notice Banners */
  .overview-insight-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 12px 18px;
    background: #F5F3FF;
    border: 1px solid #EDE9FE;
    border-radius: 10px;
  }
  .insight-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 9px;
    background: #EDE9FE;
    color: #7C3AED;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
    white-space: nowrap;
    flex-shrink: 0;
  }
  .insight-content {
    margin: 0;
    font-size: 13.5px;
    font-weight: 500;
    color: #0F172A;
    line-height: 1.5;
    flex: 1;
  }
  .insight-period {
    font-size: 12px;
    font-weight: 500;
    color: #7C3AED;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .overview-notice-banner {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 14px 18px;
    background: #EFF6FF;
    border: 1px solid #DBEAFE;
    border-radius: 10px;
  }
  .notice-icon {
    color: #2563EB;
    flex-shrink: 0;
    margin-top: 2px;
  }
  .notice-content strong {
    display: block;
    font-size: 13.5px;
    font-weight: 600;
    color: #1E40AF;
    margin-bottom: 2px;
  }
  .notice-content p {
    margin: 0;
    font-size: 13px;
    color: #3B82F6;
    line-height: 1.45;
  }

  /* 2-Column Analytics Details */
  .overview-detail-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) minmax(340px, 1fr);
    gap: 20px;
  }
  .overview-card {
    display: flex;
    flex-direction: column;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    overflow: hidden;
  }
  .overview-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    padding: 18px 20px 14px;
    border-bottom: 1px solid #F1F5F9;
  }
  .overview-card-title {
    margin: 0;
    font-size: 15.5px;
    font-weight: 600;
    color: #0F172A;
    line-height: 1.3;
  }
  .overview-card-subtitle {
    margin: 3px 0 0;
    font-size: 12px;
    color: #64748B;
  }
  .overview-badge-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 24px;
    padding: 0 8px;
    border-radius: 12px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 12px;
    font-weight: 600;
  }
  .overview-card-body {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  .overview-card-body.p-0 {
    padding: 0;
  }
  .overview-chart-legend {
    display: flex;
    align-items: center;
    gap: 14px;
    font-size: 12px;
    color: #64748B;
  }
  .overview-chart-legend .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .overview-chart-legend .legend-color {
    display: inline-block;
    border-radius: 2px;
  }
  .overview-chart-legend .line-color {
    width: 16px;
    height: 3px;
    background: #2563EB;
  }
  .overview-chart-legend .bar-color {
    width: 10px;
    height: 10px;
    background: #CBD5E1;
  }

  .overview-empty-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    padding: 44px 20px;
    color: #64748B;
    margin: auto;
  }
  .overview-empty-box .empty-icon {
    color: #94A3B8;
    margin-bottom: 12px;
  }
  .overview-empty-box .empty-icon.icon-success {
    color: #16A34A;
  }
  .overview-empty-box h4 {
    margin: 0 0 6px;
    font-size: 14.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .overview-empty-box p {
    margin: 0;
    font-size: 12.5px;
    color: #64748B;
    max-width: 340px;
    line-height: 1.5;
  }

  /* SVG Chart */
  .overview-chart-wrap {
    width: 100%;
    overflow: hidden;
  }
  .analytics-chart {
    display: block;
    width: 100%;
    height: auto;
    overflow: visible;
  }
  .analytics-chart-grid line {
    stroke: #F1F5F9;
    stroke-width: 1;
  }
  .analytics-chart-grid text {
    fill: #94A3B8;
    font-family: inherit;
    font-size: 11px;
    text-anchor: end;
  }
  .analytics-chart-unit {
    fill: #64748B;
    font-family: inherit;
    font-size: 11px;
    font-weight: 500;
    text-anchor: start;
  }
  .analytics-chart-bars rect {
    fill: #E2E8F0;
    transition: fill 0.15s ease;
  }
  .analytics-chart-bars rect:hover {
    fill: #CBD5E1;
  }
  .analytics-score-line {
    fill: none;
    stroke: #2563EB;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 2.5;
  }
  .analytics-score-point {
    fill: #FFFFFF;
    stroke: #2563EB;
    stroke-width: 2.5;
    cursor: pointer;
    transition: r 0.15s ease;
  }
  .analytics-score-point:hover {
    r: 6;
  }
  .analytics-chart-label {
    fill: #64748B;
    font-family: inherit;
    font-size: 11px;
    text-anchor: middle;
  }
  .analytics-annotation line {
    stroke: #2563EB;
    stroke-dasharray: 2 3;
    stroke-width: 1;
  }
  .analytics-annotation rect {
    fill: #FFFFFF;
    stroke: #93C5FD;
    stroke-width: 1;
  }
  .analytics-annotation text {
    fill: #2563EB;
    font-family: inherit;
    font-size: 10px;
    font-weight: 600;
    text-anchor: middle;
  }

  /* Table for Learners to Support */
  .overview-table-wrap {
    width: 100%;
    overflow-x: auto;
  }
  .overview-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
  }
  .overview-table th {
    padding: 10px 16px;
    background: #F8FAFC;
    color: #64748B;
    font-size: 11.5px;
    font-weight: 600;
    border-bottom: 1px solid #E2E8F0;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
  }
  .overview-table td {
    padding: 12px 16px;
    border-bottom: 1px solid #F1F5F9;
    color: #334155;
    font-size: 13px;
    vertical-align: middle;
  }
  .overview-table tr:last-child td {
    border-bottom: none;
  }
  .overview-table tr:hover td {
    background: #F8FAFC;
  }
  .learner-cell {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 140px;
  }
  .learner-avatar {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: #EFF6FF;
    color: #2563EB;
    display: grid;
    place-items: center;
    font-size: 11px;
    font-weight: 700;
    flex-shrink: 0;
    border: 1px solid #DBEAFE;
  }
  .learner-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .learner-name {
    font-weight: 600;
    color: #0F172A;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 140px;
  }
  .learner-date {
    font-size: 11px;
    color: #94A3B8;
  }
  .attempts-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 22px;
    padding: 0 6px;
    border-radius: 6px;
    background: #F1F5F9;
    color: #475569;
    font-size: 12px;
    font-weight: 600;
  }
  .score-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .score-badge.score-low {
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FEE2E2;
  }
  .score-badge.score-medium {
    background: #FFFBEB;
    color: #D97706;
    border: 1px solid #FEF3C7;
  }
  .score-badge.score-high {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-action-btn {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 6px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    color: #2563EB;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
  }
  .overview-action-btn:hover {
    background: #EFF6FF;
    border-color: #BFDBFE;
  }

  /* 3 System Operations Pillars */
  .overview-pillars-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
    margin-top: 4px;
  }
  .overview-pillar-card {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .overview-pillar-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);
  }
  .pillar-header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }
  .pillar-icon {
    display: grid;
    place-items: center;
    width: 38px;
    height: 38px;
    border-radius: 10px;
    flex-shrink: 0;
  }
  .pillar-icon.pillar-icon-blue {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .pillar-icon.pillar-icon-emerald {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .pillar-icon.pillar-icon-purple {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .pillar-title {
    margin: 0;
    font-size: 14.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .pillar-desc {
    margin: 2px 0 0;
    font-size: 12px;
    color: #64748B;
  }
  .pillar-stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    padding: 10px 12px;
    background: #F8FAFC;
    border-radius: 8px;
    border: 1px solid #F1F5F9;
  }
  .pillar-stat-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    text-align: center;
  }
  .pillar-stat-item .stat-number {
    font-size: 15px;
    font-weight: 700;
    color: #0F172A;
    font-variant-numeric: tabular-nums;
  }
  .pillar-stat-item .stat-label {
    font-size: 11px;
    color: #64748B;
    white-space: nowrap;
  }
  .pillar-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    margin-top: auto;
    padding-top: 4px;
  }
  .pillar-footer .arrow-icon {
    font-size: 14px;
    transition: transform 0.15s ease;
  }
  .overview-pillar-card:hover .pillar-footer .arrow-icon {
    transform: translateX(3px);
  }
  .admin-primary-button,
  .admin-secondary-button {
    display: inline-flex;
    height: 38px;
    box-sizing: border-box;
    align-items: center;
    justify-content: center;
    gap: 7px;
    padding: 0 14px;
    border: 1px solid transparent;
    border-radius: 8px;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
  }
  .admin-primary-button {
    background: #2563EB;
    color: #FFFFFF;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  .admin-primary-button:hover:not(:disabled) {
    background: #1D4ED8;
  }
  .admin-primary-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .admin-secondary-button {
    border-color: #E2E8F0;
    background: #FFFFFF;
    color: #334155;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  }
  .admin-secondary-button:hover {
    background: #F8FAFC;
    border-color: #CBD5E1;
    color: #0F172A;
  }
  .admin-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }
  .admin-stats article {
    display: flex;
    min-width: 0;
    min-height: 104px;
    align-items: flex-start;
    gap: 14px;
    padding: 16px 18px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    transition: all 0.15s ease;
  }
  .admin-stats article:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
  .admin-stat-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    background: #F1F5F9;
    color: #2563EB;
    border: 1px solid #E2E8F0;
  }
  .admin-stat-icon.coral,
  .admin-stat-icon.violet,
  .admin-stat-icon.amber {
    background: #F1F5F9;
    color: #334155;
    border: 1px solid #E2E8F0;
  }
  .admin-stats article > div {
    display: grid;
    min-width: 0;
    gap: 2px;
    flex: 1 1 120px;
  }
  .admin-stats small {
    color: #64748B;
    font-size: 12.5px;
    font-weight: 500;
  }
  .admin-stats strong {
    color: #0F172A;
    font-size: 24px;
    line-height: 30px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .stat-caption,
  .stat-link {
    display: flex;
    align-items: center;
    min-height: 24px;
    margin-left: 52px;
    color: #64748B;
    font-size: 12px;
  }
  .stat-link {
    gap: 4px;
    border: 0;
    background: none;
    color: #2563EB;
    font-weight: 500;
    cursor: pointer;
  }
  .admin-overview-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(340px, 100%), 1fr));
    gap: 20px;
  }
  .admin-panel {
    min-width: 0;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    background: #FFFFFF;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04), 0 1px 2px -1px rgba(0, 0, 0, 0.02);
  }
  /* KNOWLEDGE BASE & RAG STYLES — LIGHT AI EDUCATION */
  .admin-knowledge-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .knowledge-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .knowledge-hero-content {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 800px;
  }
  .knowledge-hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    background: #F5F3FF;
    border: 1px solid #DDD6FE;
    border-radius: 20px;
    color: #7C3AED;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.04em;
    width: fit-content;
  }
  .knowledge-hero-title {
    margin: 0;
    color: #0F172A;
    font-size: 22px;
    line-height: 28px;
    font-weight: 700;
    letter-spacing: -0.01em;
  }
  .knowledge-hero-desc {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 20px;
  }
  .knowledge-hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
  .knowledge-btn-secondary {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    height: 40px;
    padding: 0 16px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    color: #334155;
    font-size: 13.5px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    text-decoration: none;
    white-space: nowrap;
  }
  .knowledge-btn-secondary:hover:not(:disabled) {
    background: #F8FAFC;
    border-color: #94A3B8;
    color: #0F172A;
  }
  .knowledge-btn-secondary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .knowledge-btn-primary {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 40px;
    padding: 0 18px;
    background: #2563EB;
    border: 1px solid #2563EB;
    border-radius: 8px;
    color: #FFFFFF;
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2);
    transition: all 0.15s ease;
    text-decoration: none;
    white-space: nowrap;
  }
  .knowledge-btn-primary:hover:not(:disabled) {
    background: #1D4ED8;
    border-color: #1D4ED8;
  }
  .knowledge-btn-primary:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
  .knowledge-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
  }
  .knowledge-stat-card {
    display: flex;
    flex-direction: column;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
  }
  .knowledge-stat-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  }
  .knowledge-stat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 8px;
  }
  .knowledge-stat-label {
    color: #64748B;
    font-size: 13px;
    font-weight: 500;
  }
  .knowledge-stat-icon {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    border-radius: 8px;
    flex-shrink: 0;
  }
  .knowledge-stat-icon.icon-teal,
  .knowledge-stat-icon.icon-blue {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #BFDBFE;
  }
  .knowledge-stat-icon.icon-emerald {
    background: #ECFDF5;
    color: #10B981;
    border: 1px solid #A7F3D0;
  }
  .knowledge-stat-icon.icon-amber {
    background: #FFFBEB;
    color: #D97706;
    border: 1px solid #FDE68A;
  }
  .knowledge-stat-icon.icon-purple {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #DDD6FE;
  }
  .knowledge-stat-value {
    color: #0F172A;
    font-size: 26px;
    line-height: 32px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    margin-bottom: 4px;
  }
  .knowledge-stat-hint {
    color: #94A3B8;
    font-size: 12px;
  }
  .knowledge-stat-hint strong {
    color: #2563EB;
  }
  .knowledge-workspace-grid {
    display: grid;
    grid-template-columns: minmax(360px, 460px) minmax(0, 1fr);
    gap: 20px;
    align-items: start;
  }
  .knowledge-panel {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .knowledge-panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 16px 20px;
    border-bottom: 1px solid #F1F5F9;
    background: #FFFFFF;
  }
  .knowledge-panel-title-group {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .knowledge-panel-icon-box {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 8px;
    color: #2563EB;
    flex-shrink: 0;
  }
  .knowledge-panel-icon-box.icon-box-library {
    background: #EFF6FF;
    border-color: #BFDBFE;
    color: #2563EB;
  }
  .knowledge-panel-title {
    margin: 0;
    color: #0F172A;
    font-size: 15px;
    line-height: 20px;
    font-weight: 600;
  }
  .knowledge-panel-subtitle {
    margin: 2px 0 0;
    color: #64748B;
    font-size: 12px;
  }
  .knowledge-count-badge {
    padding: 4px 10px;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 20px;
    color: #475569;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }
  .knowledge-upload-form {
    display: flex;
    flex-direction: column;
    padding: 20px;
    gap: 18px;
  }
  .knowledge-dropzone {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 24px 20px;
    background: #F8FAFC;
    border: 1.5px dashed #CBD5E1;
    border-radius: 12px;
    text-align: center;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-dropzone:hover,
  .knowledge-dropzone.is-dragging {
    background: #EFF6FF;
    border-color: #2563EB;
  }
  .knowledge-dropzone input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
    overflow: hidden;
  }
  .knowledge-dropzone-icon {
    display: grid;
    width: 44px;
    height: 44px;
    place-items: center;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    color: #2563EB;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  }
  .knowledge-dropzone-title {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 600;
  }
  .knowledge-dropzone-subtitle {
    color: #64748B;
    font-size: 12px;
  }
  .knowledge-form-error {
    margin: -10px 0 0;
    color: #DC2626;
    font-size: 12px;
  }
  .knowledge-field-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .knowledge-field-label-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .knowledge-field-label {
    color: #334155;
    font-size: 13px;
    font-weight: 600;
  }
  .knowledge-field-sub {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .required-star {
    color: #DC2626;
  }
  .knowledge-select-input {
    width: 100%;
    height: 40px;
    padding: 0 12px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    color: #0F172A;
    font-size: 13.5px;
    outline: none;
    transition: all 0.15s ease;
  }
  .knowledge-select-input:focus {
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
  .knowledge-select-input.has-error {
    border-color: #DC2626;
  }
  .knowledge-topic-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .knowledge-topic-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 20px;
    color: #475569;
    font-size: 12.5px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-topic-chip:hover {
    background: #F1F5F9;
    border-color: #CBD5E1;
    color: #0F172A;
  }
  .knowledge-topic-chip.active {
    background: #EFF6FF;
    border-color: #2563EB;
    color: #2563EB;
    font-weight: 600;
  }
  .knowledge-queue-section {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-top: 6px;
    border-top: 1px dashed #E2E8F0;
  }
  .knowledge-queue-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #475569;
    font-size: 12.5px;
    font-weight: 600;
  }
  .knowledge-queue-clear {
    background: none;
    border: none;
    color: #DC2626;
    font-size: 12px;
    cursor: pointer;
    padding: 0;
  }
  .knowledge-queue-clear:hover {
    text-decoration: underline;
  }
  .knowledge-queue-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 280px;
    overflow-y: auto;
  }
  .knowledge-queue-item {
    display: grid;
    grid-template-columns: 32px 140px minmax(0, 1fr) 28px;
    align-items: center;
    gap: 10px;
    padding: 8px 10px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
  }
  .knowledge-queue-file-icon {
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    background: #EFF6FF;
    border-radius: 6px;
    color: #2563EB;
  }
  .knowledge-queue-file-details {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .knowledge-queue-filename {
    color: #0F172A;
    font-size: 12.5px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .knowledge-queue-filesize {
    color: #64748B;
    font-size: 11px;
  }
  .knowledge-queue-title-input-wrap {
    min-width: 0;
  }
  .knowledge-queue-title-input {
    width: 100%;
    height: 32px;
    padding: 0 10px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 6px;
    color: #0F172A;
    font-size: 12.5px;
    outline: none;
    transition: all 0.15s ease;
  }
  .knowledge-queue-title-input:focus {
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .knowledge-queue-title-input.has-error {
    border-color: #DC2626;
  }
  .knowledge-queue-remove-btn {
    display: grid;
    width: 28px;
    height: 28px;
    place-items: center;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: #94A3B8;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-queue-remove-btn:hover:not(:disabled) {
    background: #FEE2E2;
    color: #DC2626;
  }
  .knowledge-success-banner {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 14px;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
    border-radius: 8px;
    color: #065F46;
    font-size: 13px;
  }
  .knowledge-success-banner span {
    flex: 1;
  }
  .knowledge-banner-close {
    background: none;
    border: none;
    color: #065F46;
    cursor: pointer;
    padding: 2px;
  }
  .knowledge-form-footer {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding-top: 14px;
    border-top: 1px solid #F1F5F9;
  }
  .knowledge-footer-hint {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    color: #64748B;
    font-size: 12px;
    line-height: 16px;
  }
  .knowledge-footer-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
  }
  .knowledge-library-toolbar {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 14px 20px;
    border-bottom: 1px solid #F1F5F9;
    background: #F8FAFC;
  }
  .knowledge-search-wrap {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
  }
  .knowledge-search-icon {
    position: absolute;
    left: 12px;
    color: #94A3B8;
    pointer-events: none;
  }
  .knowledge-search-input {
    width: 100%;
    height: 38px;
    padding: 0 14px 0 36px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    color: #0F172A;
    font-size: 13px;
    outline: none;
    transition: all 0.15s ease;
  }
  .knowledge-search-input:focus {
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
  .knowledge-filters-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
  }
  .knowledge-filter-pills {
    display: inline-flex;
    background: #E2E8F0;
    padding: 3px;
    border-radius: 8px;
    gap: 2px;
  }
  .knowledge-filter-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 10px;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: #475569;
    font-size: 12.5px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-filter-pill.active {
    background: #FFFFFF;
    color: #0F172A;
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  .pill-count {
    padding: 1px 6px;
    background: #F1F5F9;
    border-radius: 10px;
    font-size: 11px;
    color: #64748B;
  }
  .knowledge-filter-pill.active .pill-count {
    background: #EFF6FF;
    color: #2563EB;
  }
  .knowledge-program-filter-wrap {
    margin-left: auto;
  }
  .knowledge-program-select {
    height: 32px;
    padding: 0 10px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 6px;
    color: #334155;
    font-size: 12.5px;
    outline: none;
  }
  .knowledge-empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    padding: 40px 24px;
    gap: 10px;
  }
  .knowledge-empty-state.compact {
    padding: 30px 20px;
  }
  .knowledge-empty-icon {
    display: grid;
    width: 52px;
    height: 52px;
    place-items: center;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 12px;
    color: #2563EB;
    margin-bottom: 4px;
  }
  .knowledge-empty-title {
    margin: 0;
    color: #0F172A;
    font-size: 15px;
    font-weight: 600;
  }
  .knowledge-empty-desc {
    margin: 0;
    max-width: 380px;
    color: #64748B;
    font-size: 13px;
    line-height: 18px;
  }
  .knowledge-empty-action {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
    padding: 7px 14px;
    background: #EFF6FF;
    border: 1px solid #2563EB;
    border-radius: 8px;
    color: #2563EB;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    text-decoration: none;
    transition: all 0.15s ease;
  }
  .knowledge-empty-action:hover {
    background: #2563EB;
    color: #FFFFFF;
  }
  .knowledge-documents-container {
    display: flex;
    flex-direction: column;
  }
  .knowledge-document-cards {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 16px 20px;
  }
  .knowledge-doc-card {
    display: grid;
    grid-template-columns: 38px minmax(0, 1fr) auto;
    align-items: center;
    gap: 14px;
    padding: 12px 14px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    transition: all 0.15s ease;
  }
  .knowledge-doc-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }
  .knowledge-doc-card.is-disabled {
    background: #F8FAFC;
    opacity: 0.85;
  }
  .knowledge-doc-type-badge {
    display: grid;
    width: 38px;
    height: 38px;
    place-items: center;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 8px;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
  }
  .knowledge-doc-card.is-disabled .knowledge-doc-type-badge {
    background: #F1F5F9;
    border-color: #E2E8F0;
    color: #94A3B8;
  }
  .knowledge-doc-body {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
  }
  .knowledge-doc-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .knowledge-doc-title {
    margin: 0;
    color: #0F172A;
    font-size: 14px;
    line-height: 20px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .knowledge-doc-card.is-disabled .knowledge-doc-title {
    color: #64748B;
  }
  .knowledge-doc-filename {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #64748B;
    font-size: 12px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .knowledge-doc-tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
  }
  .doc-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 500;
  }
  .doc-badge.badge-program {
    background: #EFF6FF;
    color: #1D4ED8;
    border: 1px solid #BFDBFE;
  }
  .doc-badge.badge-topic {
    background: #F0FDF4;
    color: #15803D;
    border: 1px solid #BBF7D0;
  }
  .doc-badge.badge-chunks {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #DDD6FE;
    font-weight: 600;
  }
  .doc-badge.badge-date {
    background: #F1F5F9;
    color: #64748B;
  }
  .knowledge-doc-controls {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .knowledge-toggle-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .knowledge-ios-switch {
    position: relative;
    width: 38px;
    height: 22px;
    background: #CBD5E1;
    border: none;
    border-radius: 20px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    padding: 2px;
  }
  .knowledge-ios-switch[aria-checked='true'] {
    background: #2563EB;
  }
  .switch-handle {
    display: block;
    width: 18px;
    height: 18px;
    background: #FFFFFF;
    border-radius: 50%;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s ease;
  }
  .knowledge-ios-switch[aria-checked='true'] .switch-handle {
    transform: translateX(16px);
  }
  .knowledge-toggle-label {
    color: #94A3B8;
    font-size: 12px;
    font-weight: 500;
    min-width: 58px;
    white-space: nowrap;
  }
  .knowledge-toggle-label.active {
    color: #2563EB;
    font-weight: 600;
  }
  .knowledge-more-btn {
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    background: transparent;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    color: #64748B;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-more-btn:hover {
    background: #F1F5F9;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .knowledge-pagination-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 20px;
    border-top: 1px solid #F1F5F9;
    background: #F8FAFC;
    flex-wrap: wrap;
  }
  .pagination-info {
    color: #64748B;
    font-size: 12.5px;
  }
  .pagination-info strong {
    color: #0F172A;
  }
  .pagination-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .knowledge-page-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 30px;
    padding: 0 10px;
    background: #FFFFFF;
    border: 1px solid #CBD5E1;
    border-radius: 6px;
    color: #475569;
    font-size: 12.5px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-page-btn:hover:not(:disabled) {
    background: #F1F5F9;
    color: #0F172A;
  }
  .knowledge-page-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .knowledge-page-current {
    font-size: 12.5px;
    font-weight: 600;
    color: #0F172A;
    padding: 0 4px;
  }
  .admin-overlay-form { display: grid; gap: 20px; }
  .overlay-form-error { margin: 0; padding: 10px 14px; border: 1px solid #efc9c3; border-radius: var(--radius-lg); color: var(--danger); background: #fff7f6; font-size: 12.5px; }
  .admin-overlay-field { display: grid; min-width: 0; gap: 6px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
  .admin-overlay-field > b,.overlay-field > b { color: var(--required); }
  .admin-overlay-field input,.admin-overlay-field select,.admin-overlay-field textarea,.overlay-field input,.overlay-field select,.overlay-field textarea { width: 100%; min-width: 0; min-height: 44px; box-sizing: border-box; padding: 0 14px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text); background: var(--surface); font: inherit; font-size: 14px; }
  .admin-overlay-field textarea,.overlay-field textarea { min-height: 120px; padding: 12px 14px; line-height: 1.6; resize: vertical; }
  .admin-overlay-field input:focus-visible,.admin-overlay-field select:focus-visible,.admin-overlay-field textarea:focus-visible,.overlay-field input:focus-visible,.overlay-field select:focus-visible,.overlay-field textarea:focus-visible { outline: 2px solid var(--ds-primary); outline-offset: 2px; }
  .admin-overlay-field small,.overlay-field small { color: var(--text-muted); font-size: 12px; font-weight: 400; }
  .overlay-skill-field { display: grid; min-width: 0; gap: 8px; margin: 0; padding: 0; border: 0; }
  .overlay-skill-field legend { margin-bottom: 8px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
  .overlay-skill-field legend small { margin-left: 6px; color: var(--text-muted); font-size: 12px; font-weight: 400; }
  .overlay-loading-state { display: flex; min-height: 180px; align-items: center; justify-content: center; gap: 10px; color: var(--text-muted); }
  .overlay-spinner { flex: 0 0 auto; animation: overlay-spin 800ms linear infinite; }
  @keyframes overlay-spin { to { transform: rotate(360deg); } }
  .knowledge-preview-list { display: flex; flex-direction: column; gap: 10px; margin: 0; padding: 0; list-style: none; }
  .knowledge-preview-chunk { display: grid; grid-template-columns: 32px minmax(0, 1fr); align-items: start; gap: 12px; padding: 14px 16px; border: 1px solid #E2E8F0; border-radius: 10px; background: #F8FAFC; transition: all 0.15s ease; }
  .knowledge-preview-chunk:hover { background: #FFFFFF; border-color: #CBD5E1; box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04); }
  .knowledge-preview-chunk > span { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 6px; color: #2563EB; background: #EFF6FF; border: 1px solid #BFDBFE; font-size: 12px; font-weight: 700; font-variant-numeric: tabular-nums; }
  .knowledge-preview-chunk p { min-width: 0; margin: 2px 0 0; color: #1E293B; font-size: 13.5px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
  .knowledge-edit-form { display: grid; gap: 20px; }
  .admin-panel-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }
  .admin-panel-heading h2 {
    margin: 0;
    color: var(--text-strong);
    font-size: 17px;
    line-height: 24px;
    font-weight: 600;
  }
  .admin-panel-heading p {
    margin: 2px 0 0;
    color: var(--text-muted);
    font-size: 13px;
  }
  .admin-text-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px;
    border: 0;
    background: none;
    color: var(--ds-primary);
    font: inherit;
    font-size: 9px;
    cursor: pointer;
  }
  .admin-list {
    display: grid;
  }
  .admin-list article {
    display: flex;
    min-height: 60px;
    align-items: center;
    gap: 12px;
    border-top: 1px solid var(--border-subtle);
  }
  .account-avatar {
    display: grid;
    width: 34px;
    height: 34px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    background: #EFF6FF;
    color: #1D4ED8;
    font-size: 13px;
    font-style: normal;
    font-weight: 700;
    border: 1px solid #DBEAFE;
  }
  .account-avatar.administrator {
    background: #F5F3FF;
    color: #7C3AED;
    border-color: #DDD6FE;
  }
  .admin-list article > div {
    display: grid;
    min-width: 0;
    flex: 1;
    gap: 3px;
  }
  .admin-list strong {
    overflow: hidden;
    color: var(--text-strong);
    font-size: 14px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .admin-list small {
    color: var(--text-muted);
    font-size: 12px;
  }
  .account-state {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 24px;
    padding: 0 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
  }
  .state-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  .account-state.enabled {
    background: #F0FDF4;
    color: #166534;
    border: 1px solid #BBF7D0;
  }
  .account-state.enabled .state-dot {
    background: #16A34A;
  }
  .account-state.disabled {
    background: #FEF2F2;
    color: #991B1B;
    border: 1px solid #FECACA;
  }
  .account-state.disabled .state-dot {
    background: #DC2626;
  }
  .account-state.pending {
    background: #FFFBEB;
    color: #92400E;
    border: 1px solid #FDE68A;
  }
  .account-state.pending .state-dot {
    background: #D97706;
  }
  .account-plan {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 8px;
    border-radius: 6px;
    background: #F1F5F9;
    color: #475569;
    font-size: 11.5px;
    font-weight: 500;
  }
  .account-plan.premium {
    color: #7C3AED;
    background: #F5F3FF;
    border: 1px solid #DDD6FE;
  }
  .admin-empty {
    display: flex;
    min-height: 220px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 36px 20px;
    color: #64748B;
    font-size: 13.5px;
    text-align: center;
  }
  .admin-empty-loading {
    gap: 12px;
  }
  .empty-icon-wrap {
    display: grid;
    width: 48px;
    height: 48px;
    margin-bottom: 2px;
    place-items: center;
    border-radius: 10px;
    background: #F8FAFC;
    color: #64748B;
    border: 1px solid #E2E8F0;
  }
  .admin-empty strong {
    font-size: 14.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .admin-empty p {
    margin: 0 0 10px;
    max-width: 420px;
    color: #64748B;
    font-size: 13px;
    line-height: 1.5;
  }
  .content-link {
    display: flex;
    width: 100%;
    align-items: center;
    gap: 12px;
    min-height: 52px;
    padding: 8px 12px;
    border: 0;
    border-top: 1px solid #F1F5F9;
    background: #FFFFFF;
    text-align: left;
    cursor: pointer;
    transition: background 0.15s ease;
  }
  .content-link:hover {
    background: #F8FAFC;
  }
  .content-link > span:nth-child(2) {
    display: grid;
    flex: 1;
    gap: 2px;
  }
  .content-link strong {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 600;
  }
  .content-link small {
    color: #64748B;
    font-size: 12px;
  }
  .content-link > svg {
    color: #94A3B8;
  }
  .admin-table-panel {
    padding: 0;
    overflow: hidden;
  }
  .admin-table-wrap {
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    overscroll-behavior-inline: contain;
  }
  .admin-table {
    width: 100%;
    min-width: 680px;
    border-collapse: collapse;
    font-size: 13.5px;
    text-align: left;
  }
  .admin-table th {
    height: 38px;
    padding: 8px 16px;
    border-bottom: 1px solid #E2E8F0;
    background: #F8FAFC;
    color: #475569;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
  }
  .admin-table td {
    height: 50px;
    padding: 8px 16px;
    border-bottom: 1px solid #F1F5F9;
    color: #334155;
    white-space: nowrap;
  }
  .admin-table tbody tr:last-child td {
    border-bottom: 0;
  }
  .admin-table tbody tr:hover { background: #F8FAFC; }
  .table-account {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #0F172A;
  }
  .table-account strong {
    font-size: 13.5px;
    font-weight: 600;
  }
  .role-select {
    min-width: 125px;
    height: 32px;
    padding: 0 8px;
    border: 1px solid #CBD5E1;
    border-radius: 6px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .role-select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .plan-select.plan-premium-selected {
    background: #FAF5FF;
    border-color: #DDD6FE;
    color: #6D28D9;
    font-weight: 600;
  }
  .table-action {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 30px;
    padding: 0 10px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
  }
  .table-action:hover {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .table-action.danger {
    color: #DC2626;
    border-color: #FECACA;
    background: #FEF2F2;
  }
  .table-action.danger:hover {
    background: #FEE2E2;
    color: #B91C1C;
    border-color: #FCA5A5;
  }
  .table-action.primary-action {
    color: #2563EB;
    border-color: #BFDBFE;
    background: #EFF6FF;
  }
  .table-action.primary-action:hover {
    background: #DBEAFE;
    color: #1D4ED8;
    border-color: #93C5FD;
  }
  .account-actions-cell {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* ========================================================
     ADMIN ACCOUNT MANAGEMENT MODULE (LIGHT AI EDUCATION)
     ======================================================== */
  .admin-accounts-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-width: 0;
  }
  .account-kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 14px;
  }
  .account-kpi-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 18px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    transition: box-shadow 0.15s ease, border-color 0.15s ease;
  }
  .account-kpi-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
  }
  .account-kpi-icon {
    display: grid;
    width: 42px;
    height: 42px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .account-kpi-icon.icon-total {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .account-kpi-icon.icon-students {
    background: #F0F9FF;
    color: #0284C7;
    border: 1px solid #E0F2FE;
  }
  .account-kpi-icon.icon-active {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .account-kpi-icon.icon-premium {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .account-kpi-info {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 2px;
  }
  .account-kpi-label {
    font-size: 12px;
    font-weight: 500;
    color: #64748B;
  }
  .account-kpi-value {
    font-size: 22px;
    line-height: 26px;
    font-weight: 700;
    color: #0F172A;
    font-variant-numeric: tabular-nums;
  }
  .account-kpi-hint {
    font-size: 11px;
    color: #94A3B8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .account-main-panel {
    padding: 20px 24px;
    border-radius: 12px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
  }
  .account-panel-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 14px;
    margin-bottom: 0;
  }
  .account-header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
  .account-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 18px;
    padding-top: 16px;
    border-top: 1px solid #F1F5F9;
  }
  .account-search-wrap {
    position: relative;
    flex: 1 1 240px;
    min-width: 200px;
  }
  .account-search-icon {
    position: absolute;
    top: 50%;
    left: 10px;
    transform: translateY(-50%);
    color: #94A3B8;
    pointer-events: none;
  }
  .account-search-input {
    width: 100%;
    height: 36px;
    padding: 0 28px 0 34px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 13px;
    transition: all 0.15s ease;
  }
  .account-search-input:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
  .account-search-clear {
    position: absolute;
    top: 50%;
    right: 8px;
    transform: translateY(-50%);
    display: grid;
    width: 20px;
    height: 20px;
    place-items: center;
    border: 0;
    border-radius: 50%;
    background: #F1F5F9;
    color: #64748B;
    cursor: pointer;
  }
  .account-search-clear:hover {
    background: #E2E8F0;
    color: #0F172A;
  }
  .account-filters {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .account-filter-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .filter-label {
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
  .filter-select {
    height: 36px;
    padding: 0 10px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: border-color 0.15s ease;
  }
  .filter-select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .account-reset-filter-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 36px;
    padding: 0 11px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #F8FAFC;
    color: #475569;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .account-reset-filter-btn:hover {
    background: #F1F5F9;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .account-meta-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0 14px;
    font-size: 12.5px;
    color: #64748B;
  }
  .account-meta-summary strong {
    color: #0F172A;
    font-weight: 600;
  }
  .table-account-details {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 1px;
  }
  .account-username {
    font-size: 13.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .account-id {
    font-size: 11px;
    color: #94A3B8;
  }
  .row-disabled {
    background: #F8FAFC;
    opacity: 0.75;
  }
  .row-disabled:hover {
    background: #F1F5F9 !important;
  }
  .account-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .account-mobile-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 14px 16px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  }
  .account-mobile-card.card-disabled {
    background: #F8FAFC;
    opacity: 0.75;
  }
  .account-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .account-card-body {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 0;
    border-top: 1px solid #F1F5F9;
    border-bottom: 1px solid #F1F5F9;
  }
  .card-field-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .card-field-label {
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
  .account-card-actions {
    display: flex;
    gap: 8px;
  }
  .mobile-card-btn {
    flex: 1;
    justify-content: center;
    height: 34px;
    font-size: 12.5px;
  }
  .account-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    padding-top: 16px;
    margin-top: 14px;
    border-top: 1px solid #F1F5F9;
  }
  .pagination-size {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12.5px;
    color: #64748B;
  }
  .pagination-select {
    height: 32px;
    padding: 0 8px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 12px;
    cursor: pointer;
  }
  .pagination-nav {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .pagination-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 32px;
    padding: 0 10px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .pagination-btn:hover:not(:disabled) {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .pagination-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .pagination-status {
    font-size: 12.5px;
    color: #64748B;
  }
  .pagination-status strong {
    color: #0F172A;
  }
  /* RESULTS & LEARNER PROGRESS STYLES */
  .admin-results-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .results-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .results-hero-content {
    max-width: 680px;
  }
  .results-hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    margin-bottom: 8px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
  .results-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .results-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .results-hero-controls {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
  .results-filter-control {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #475569;
    font-size: 13px;
    font-weight: 500;
  }
  .results-filter-control .control-label {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #475569;
    font-size: 13px;
    font-weight: 600;
  }
  .results-select {
    height: 38px;
    max-width: 240px;
    padding: 0 12px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: border-color 0.15s ease;
  }
  .results-select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .admin-alert-error {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border: 1px solid #FECACA;
    border-radius: 8px;
    background: #FEF2F2;
    color: #991B1B;
    font-size: 13px;
  }
  .results-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .results-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    transition: transform 140ms ease, box-shadow 140ms ease;
  }
  .results-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .results-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .results-stat-card.blue .results-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .results-stat-card.emerald .results-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .results-stat-card.purple .results-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .results-stat-card.amber .results-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .results-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .results-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .results-stat-info strong {
    color: #0F172A;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .results-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .results-history-panel {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    overflow: hidden;
  }
  .results-history-heading {
    padding: 18px 24px;
  }
  .results-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .results-search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 11px;
    min-height: 36px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #64748B;
  }
  .results-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 200px;
  }
  .results-select-filter {
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .results-desktop-table {
    display: block;
  }
  .results-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .results-exam-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .results-exam-cell strong {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 600;
  }
  .results-status-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 9px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
  }
  .results-status-pill.graded {
    background: #ECFDF5;
    color: #065F46;
    border: 1px solid #A7F3D0;
  }
  .results-status-pill.submitted {
    background: #EFF6FF;
    color: #1D4ED8;
    border: 1px solid #BFDBFE;
  }
  .results-status-pill.in_progress {
    background: #FFFBEB;
    color: #92400E;
    border: 1px solid #FDE68A;
  }
  .results-status-pill.published {
    background: #ECFDF5;
    color: #065F46;
    border: 1px solid #A7F3D0;
  }
  .results-status-pill.draft {
    background: #F1F5F9;
    color: #475569;
    border: 1px solid #E2E8F0;
  }
  .results-score-value {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .results-band-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 5px;
    background: #FAF5FF;
    color: #7C3AED;
    border: 1px solid #E9D5FF;
    font-size: 11.5px;
    font-weight: 700;
  }
  .results-violation-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 5px;
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FECACA;
    font-size: 11.5px;
    font-weight: 600;
  }
  .results-clean-badge {
    color: #94A3B8;
    font-size: 12px;
  }
  .results-analytics-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }
  .results-skill-panel,
  .results-trend-panel {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .results-skill-panel .admin-panel-heading,
  .results-trend-panel .admin-panel-heading {
    padding: 0;
  }
  .skill-progress-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .skill-progress-row {
    display: grid;
    grid-template-columns: minmax(90px, 150px) minmax(80px, 1fr) 48px;
    align-items: center;
    gap: 12px;
  }
  .skill-label-wrap {
    min-width: 0;
  }
  .skill-name {
    color: #334155;
    font-size: 13px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
  .skill-progress-track {
    height: 8px;
    overflow: hidden;
    border-radius: 9999px;
    background: #F1F5F9;
  }
  .skill-progress-track i {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: #059669;
    transition: width 0.3s ease;
  }
  .skill-percent-value {
    color: #0F172A;
    font-size: 13px;
    font-weight: 700;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
  .learner-progress-empty {
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 80px;
    padding: 16px;
    border: 1px dashed #E2E8F0;
    border-radius: 8px;
    background: #F8FAFC;
    color: #64748B;
    font-size: 13px;
    justify-content: center;
  }
  .learner-progress-empty svg {
    color: #94A3B8;
  }
  .learner-trend-chart {
    display: flex;
    min-height: 170px;
    align-items: flex-end;
    gap: clamp(10px, 2vw, 22px);
    overflow-x: auto;
    padding: 16px 8px 4px;
    border-bottom: 1px solid #E2E8F0;
  }
  .learner-trend-point {
    display: grid;
    min-width: 42px;
    height: 160px;
    flex: 1;
    grid-template-rows: 20px minmax(14px, 1fr) 24px;
    justify-items: center;
    align-items: end;
    gap: 4px;
  }
  .learner-trend-point strong {
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .learner-trend-point i {
    width: min(32px, 75%);
    min-height: 8px;
    align-self: end;
    border-radius: 4px 4px 2px 2px;
    background: #2563EB;
    transition: height 0.3s ease;
  }
  .learner-trend-point:hover i {
    background: #1D4ED8;
  }
  .learner-trend-point small {
    align-self: center;
    color: #94A3B8;
    font-size: 10.5px;
  }
  .attempt-result-detail { display: grid; gap: 16px; }
  .attempt-result-score { display: grid; gap: 4px; padding: 16px; border: 1px solid #A7F3D0; border-radius: 10px; background: #ECFDF5; }
  .attempt-result-score span { color: #065F46; font-size: 12.5px; font-weight: 500; }
  .attempt-result-score strong { color: #0F172A; font-size: 26px; line-height: 32px; font-variant-numeric: tabular-nums; font-weight: 700; }
  .attempt-result-score small { color: #059669; font-size: 12px; font-weight: 600; }
  .attempt-result-detail dl { display: grid; margin: 0; border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; }
  .attempt-result-detail dl > div { display: flex; min-height: 44px; align-items: center; justify-content: space-between; gap: 16px; padding: 6px 14px; border-bottom: 1px solid #F1F5F9; }
  .attempt-result-detail dl > div:last-child { border: 0; }
  .attempt-result-detail dt { color: #64748B; font-size: 13px; }
  .attempt-result-detail dd { margin: 0; color: #0F172A; font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; }

  /* PROGRAMS & EXAMS MODULE STYLES */
  .admin-programs-section,
  .admin-exams-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .programs-hero-panel,
  .exams-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .programs-hero-content,
  .exams-hero-content {
    max-width: 680px;
  }
  .programs-hero-badge,
  .exams-hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    margin-bottom: 8px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
  .programs-hero-content h2,
  .exams-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .programs-hero-content p,
  .exams-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .programs-hero-actions,
  .exams-hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
  .programs-metrics-grid,
  .exams-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .programs-stat-card,
  .exams-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    transition: transform 140ms ease, box-shadow 140ms ease;
  }
  .programs-stat-card:hover,
  .exams-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .programs-stat-icon,
  .exams-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .programs-stat-card.blue .programs-stat-icon,
  .exams-stat-card.blue .exams-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .programs-stat-card.emerald .programs-stat-icon,
  .exams-stat-card.emerald .exams-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .programs-stat-card.purple .programs-stat-icon,
  .exams-stat-card.purple .exams-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .exams-stat-card.amber .exams-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .programs-stat-info,
  .exams-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .programs-stat-info span,
  .exams-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .programs-stat-info strong,
  .exams-stat-info strong {
    color: #0F172A;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .programs-stat-info small,
  .exams-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .programs-table-filters,
  .exams-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .programs-search-box,
  .exams-search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 11px;
    min-height: 36px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #64748B;
  }
  .programs-search-box input,
  .exams-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 210px;
  }
  .exams-select-filter {
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .programs-desktop-table,
  .exams-desktop-table {
    display: block;
  }
  .programs-mobile-grid,
  .exams-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .program-title-cell strong {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
  }
  .program-code-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 4px;
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }
  .program-score-pill {
    color: #0F172A;
    font-size: 13px;
    font-weight: 600;
  }
  .program-passing-pill {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F8FAFC;
    color: #334155;
    border: 1px solid #E2E8F0;
    font-size: 12px;
    font-weight: 600;
  }
  .program-skills-tags {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-wrap: wrap;
  }
  .skill-chip-tag {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    background: #F1F5F9;
    color: #475569;
    font-size: 11.5px;
    font-weight: 600;
  }
  .exam-program-cell {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  .exam-prog-name {
    color: #0F172A;
    font-size: 13px;
    font-weight: 600;
  }
  .exam-topic-name {
    color: #64748B;
    font-size: 12.5px;
  }
  .exam-duration-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F8FAFC;
    color: #334155;
    border: 1px solid #E2E8F0;
    font-size: 12px;
    font-weight: 600;
  }
  .preset-actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
    max-width: 720px;
    margin: 0 auto;
  }
  .preset-card-btn {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    cursor: pointer;
    text-align: left;
    transition: all 140ms ease;
  }
  .preset-card-btn:hover {
    border-color: #2563EB;
    background: #EFF6FF;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  }
  .preset-card-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    background: #EFF6FF;
    color: #2563EB;
  }
  .preset-card-btn:hover .preset-card-icon {
    background: #2563EB;
    color: #FFFFFF;
  }
  .preset-card-text strong {
    display: block;
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
  }
  .preset-card-text small {
    display: block;
    color: #64748B;
    font-size: 11.5px;
    margin-top: 2px;
  }
  .secondary-text {
    display: block;
    margin-top: 4px;
    color: var(--text-muted);
    font-size: 12px;
  }
  .description-cell {
    max-width: 330px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
  }
  .admin-scrim,
  .admin-menu-button {
    display: none;
  }
  @media (max-width: 1050px) {
    .admin-content {
      padding: 24px 20px 40px;
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
    .overview-kpi-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .overview-detail-grid {
      grid-template-columns: minmax(0, 1fr);
      gap: 20px;
    }
    .overview-pillars-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (max-width: 800px) {
    .admin-page-heading { align-items: flex-start; flex-direction: column; }
    .analytics-controls { width: 100%; justify-content: flex-start; }
    .overview-pillars-grid { grid-template-columns: minmax(0, 1fr); }
    .overview-insight-banner { flex-direction: column; align-items: flex-start; gap: 8px; }
    .learner-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .learner-report-toolbar { align-items: stretch; flex-direction: column; }
    .learner-report-filters { flex-wrap: wrap; }
    .knowledge-layout { gap: 12px; }
    .knowledge-upload-item { grid-template-columns: 32px minmax(0, 1fr) 30px; }
    .knowledge-upload-item .queued-title { grid-column: 2 / 3; }
    .knowledge-upload-item .knowledge-icon-button { grid-column: 3; grid-row: 1 / span 2; }
    .knowledge-document { grid-template-columns: 36px minmax(0, 1fr); }
    .knowledge-document-actions { grid-column: 2; justify-content: flex-end; }
    .knowledge-page { gap: 14px; }
  }
  @media (max-width: 520px) {
    .overview-kpi-grid { grid-template-columns: minmax(0, 1fr); }
    .analytics-controls { align-items: stretch; flex-direction: column; gap: 8px; width: 100%; }
    .analytics-time-filter { width: 100%; justify-content: stretch; }
    .analytics-time-filter button { min-width: 0; flex: 1; }
    .analytics-exam-filter select { width: 100%; max-width: none; }
    .overview-refresh-button { width: 100%; }
    .overview-card-header { flex-direction: column; gap: 10px; }
    .overview-chart-legend { width: 100%; justify-content: flex-start; }
    .analytics-chart-label.label-hidden-mobile { display: none; }
    .overview-error-state { flex-direction: column; align-items: stretch; gap: 14px; }
    .learner-summary-grid { grid-template-columns: minmax(0, 1fr); gap: 10px; }
    .learner-summary-card { min-height: auto; }
    .learner-report-filters { display: grid; grid-template-columns: minmax(0, 1fr); }
    .learner-report-filters label,
    .learner-report-filters select { width: 100%; max-width: none; }
    .skill-progress-row { grid-template-columns: minmax(75px, 100px) minmax(40px, 1fr) 42px; gap: 8px; font-size: 12px; }
    .account-actions-cell { align-items: flex-start; flex-direction: column; }
    .knowledge-filter button { font-size: 12px; gap: 4px; padding: 0 4px; }
    .knowledge-card-heading { padding-right: 12px; padding-left: 12px; }
    .knowledge-library-tools { padding-right: 10px; padding-left: 10px; }
    .knowledge-document-list { padding-right: 10px; padding-left: 10px; }
    .knowledge-document-meta { gap: 5px; }
    .knowledge-upload-item { grid-template-columns: 32px minmax(0, 1fr) 30px; }
    .queued-title { grid-column: 2 / 3; }
    .knowledge-form-footer { align-items: flex-start; flex-direction: column; }
    .knowledge-submit { width: 100%; }
  }
  @media (max-width: 1024px) {
    .admin-sidebar {
      z-index: 45;
      width: min(280px, 86vw);
      transform: translateX(-100%);
      transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
      box-shadow: 4px 0 24px rgba(15, 23, 42, 0.12);
    }
    .admin-sidebar.admin-sidebar-open {
      transform: translateX(0);
    }
    .admin-sidebar-close {
      display: grid;
    }
    .admin-workspace {
      width: 100%;
      margin-left: 0;
    }
    .admin-menu-button {
      display: grid;
      flex: 0 0 auto;
    }
    .admin-scrim {
      position: fixed;
      z-index: 40;
      inset: 0;
      display: block;
      background: rgba(15, 23, 42, 0.35);
      backdrop-filter: blur(2px);
    }
    .admin-topbar {
      gap: 12px;
      padding: 0 clamp(16px, 3vw, 32px);
    }
    .admin-search {
      width: auto;
      flex: 1 1 200px;
    }
    .admin-top-actions {
      gap: 8px;
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
  @media (max-width: 640px) {
    .breadcrumb-badge { display: none; }
    .admin-breadcrumb { max-width: 130px; overflow: hidden; text-overflow: ellipsis; }
    .admin-preview-learner-link span { display: none; }
    .admin-preview-learner-link { padding: 6px 8px; }
    .admin-profile div,
    .admin-profile > svg { display: none; }
    .admin-topbar { gap: 8px; padding-right: 14px; padding-left: 14px; }
    .admin-top-actions { gap: 4px; }
  }
  @media (max-width: 520px) {
    .admin-content {
      padding: 20px 16px 32px;
    }
    .admin-page-heading {
      align-items: flex-start;
      flex-direction: column;
    }
    .admin-page-heading h1 {
      font-size: 21px;
    }
    .admin-stats article {
      min-height: 90px;
    }
    .admin-panel {
      padding: 11px;
    }
    .admin-table { min-width: 680px; }
    .admin-table-panel {
      padding: 11px;
    }
    .form-row {
      grid-template-columns: minmax(0, 1fr);
    }
  }

  /* BILLING & SUBSCRIPTIONS STYLES */
  .admin-billing-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .billing-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .billing-hero-content {
    max-width: 680px;
  }
  .billing-hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    margin-bottom: 8px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
  .billing-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .billing-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .billing-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .billing-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    transition: transform 140ms ease, box-shadow 140ms ease;
  }
  .billing-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .billing-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .billing-stat-card.revenue .billing-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .billing-stat-card.success .billing-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .billing-stat-card.pending .billing-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .billing-stat-card.premium .billing-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .billing-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .billing-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .billing-stat-info strong {
    color: #0F172A;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .billing-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .billing-plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 20px;
  }
  .billing-plan-card {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 24px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    transition: all 140ms ease;
  }
  .billing-plan-card.premium {
    border: 1.5px solid #C4B5FD;
    background: linear-gradient(180deg, #FAF5FF 0%, #FFFFFF 100%);
    box-shadow: 0 4px 20px rgba(124, 58, 237, 0.05);
  }
  .plan-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
  .plan-badge {
    display: inline-block;
    padding: 3px 8px;
    margin-bottom: 6px;
    border-radius: 5px;
    background: #F1F5F9;
    color: #475569;
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.03em;
  }
  .plan-badge.premium-badge {
    background: linear-gradient(135deg, #7C3AED, #4F46E5);
    color: #FFFFFF;
  }
  .plan-card-header h3 {
    margin: 0;
    color: #0F172A;
    font-size: 17px;
    font-weight: 700;
  }
  .plan-price-tag {
    text-align: right;
  }
  .plan-price-tag strong {
    display: block;
    color: #0F172A;
    font-size: 22px;
    font-weight: 800;
  }
  .plan-price-tag small {
    color: #64748B;
    font-size: 11.5px;
  }
  .plan-card-desc {
    margin: 0;
    color: #475569;
    font-size: 13px;
    line-height: 1.5;
  }
  .plan-features-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .plan-features-list li {
    display: flex;
    align-items: center;
    gap: 9px;
    color: #334155;
    font-size: 13px;
    line-height: 1.4;
  }
  .plan-features-list li svg {
    flex: 0 0 auto;
    color: #10B981;
  }
  .plan-card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 14px;
    margin-top: auto;
    border-top: 1px solid #E2E8F0;
  }
  .plan-user-count {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #475569;
    font-size: 12.5px;
  }
  .plan-user-count strong {
    color: #0F172A;
    font-weight: 700;
  }
  .plan-upgrade-hint {
    color: #6366F1;
    font-size: 11.5px;
    font-weight: 500;
  }
  .billing-table-panel {
    background: #FFFFFF;
  }
  .billing-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .billing-search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 11px;
    min-height: 36px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #64748B;
  }
  .billing-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 200px;
  }
  .billing-select-filter {
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .payment-id-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 4px;
    background: #F1F5F9;
    color: #1E293B;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    font-weight: 600;
  }
  .provider-badge {
    display: inline-block;
    padding: 3px 9px;
    border-radius: 6px;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.03em;
  }
  .provider-badge.stripe {
    background: rgba(99, 91, 255, 0.1);
    color: #635BFF;
    border: 1px solid rgba(99, 91, 255, 0.2);
  }
  .provider-badge.paypal {
    background: rgba(0, 121, 193, 0.1);
    color: #0079C1;
    border: 1px solid rgba(0, 121, 193, 0.2);
  }
  .payment-amount {
    color: #0F172A;
    font-size: 14px;
    font-weight: 700;
  }
  .payment-status-pill {
    display: inline-block;
    padding: 3px 9px;
    border-radius: 9999px;
    font-size: 11.5px;
    font-weight: 600;
  }
  .payment-status-pill.succeeded {
    background: #DEF7EC;
    color: #03543F;
  }
  .payment-status-pill.pending {
    background: #FEF08A;
    color: #713F12;
  }
  .payment-status-pill.failed,
  .payment-status-pill.refunded {
    background: #FDE8E8;
    color: #9B1C1C;
  }
  .payment-time {
    color: #64748B;
    font-size: 12.5px;
  }
  .plan-select.plan-premium-selected {
    border-color: #C4B5FD;
    color: #6D28D9;
    font-weight: 600;
    background: #FAF5FF;
  }

  /* SUBTABS NAVIGATION */
  .billing-tabs-nav {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px;
    background: #F1F5F9;
    border-radius: 10px;
    width: fit-content;
    max-width: 100%;
    overflow-x: auto;
  }
  .billing-tab-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: #475569;
    font: inherit;
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 140ms ease;
    white-space: nowrap;
  }
  .billing-tab-btn:hover {
    color: #0F172A;
    background: rgba(255, 255, 255, 0.6);
  }
  .billing-tab-btn.active {
    background: #FFFFFF;
    color: #2563EB;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  }
  .billing-tab-btn .tab-badge {
    display: inline-block;
    padding: 1px 7px;
    border-radius: 9999px;
    background: #E2E8F0;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
  }
  .billing-tab-btn.active .tab-badge {
    background: #EFF6FF;
    color: #2563EB;
  }
  .billing-tab-pane {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  /* PLAN CARDS ENHANCEMENT */
  .billing-plan-card.inactive {
    opacity: 0.72;
    background: #F8FAFC;
    border-style: dashed;
  }
  .plan-quota-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 10px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }
  .plan-quota-badge svg {
    color: #2563EB;
  }
  .plan-status-indicator {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #64748B;
    font-size: 12px;
  }
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #94A3B8;
  }
  .status-dot.active {
    background: #10B981;
    box-shadow: 0 0 0 2px #DEF7EC;
  }
  .plan-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* USER BILLINGS TABLE */
  .quota-progress-cell {
    display: flex;
    align-items: center;
  }
  .unlimited-quota {
    display: inline-block;
    color: #7C3AED;
    font-size: 13px;
    font-weight: 700;
  }
  .limited-quota {
    color: #475569;
    font-size: 13px;
    font-weight: 500;
  }
  .order-count-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F1F5F9;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }
  .table-row-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .billing-hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
  .billing-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }

  /* USER PAYMENTS DRAWER */
  .user-payments-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 0;
  }
  .user-payment-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 14px 16px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
  }
  .user-payment-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .user-payment-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  @media (max-width: 768px) {
    .billing-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .billing-hero-actions {
      width: 100%;
      flex-direction: column;
      gap: 8px;
    }
    .billing-hero-actions button {
      width: 100%;
      justify-content: center;
    }
    .billing-table-filters {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
    }
    .billing-search-box {
      width: 100%;
    }
    .billing-search-box input {
      width: 100%;
    }
    .billing-select-filter {
      width: 100%;
    }
    .billing-tabs-nav {
      width: 100%;
    }
    .account-desktop-table,
    .billing-desktop-table,
    .results-desktop-table,
    .programs-desktop-table,
    .exams-desktop-table {
      display: none !important;
    }
    .account-mobile-grid,
    .billing-mobile-grid,
    .results-mobile-grid,
    .programs-mobile-grid,
    .exams-mobile-grid {
      display: flex !important;
    }
    .results-hero-panel,
    .programs-hero-panel,
    .exams-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .results-hero-controls,
    .programs-hero-actions,
    .exams-hero-actions {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .results-filter-control {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
    }
    .results-select {
      max-width: none;
      width: 100%;
    }
    .results-analytics-row {
      grid-template-columns: 1fr;
    }
    .billing-metrics-grid {
      grid-template-columns: 1fr;
    }
    .billing-plans-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-workspace-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .knowledge-hero-actions {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .knowledge-hero-actions button,
    .knowledge-hero-actions a {
      width: 100%;
      justify-content: center;
    }
    .knowledge-metrics-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-queue-item {
      grid-template-columns: 32px 1fr 28px;
    }
    .knowledge-queue-title-input-wrap {
      grid-column: 1 / -1;
    }
    .knowledge-doc-card {
      grid-template-columns: 1fr;
      gap: 12px;
    }
    .knowledge-doc-controls {
      justify-content: space-between;
      border-top: 1px solid #F1F5F9;
      padding-top: 8px;
      width: 100%;
    }
    .knowledge-pagination-bar {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .knowledge-pagination-bar .pagination-actions {
      justify-content: space-between;
      width: 100%;
    }
  }
</style>
