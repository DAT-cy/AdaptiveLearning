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
    Upload,
    User,
    Users,
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
  const initialSection = String(route.query.section || 'overview')
  const activeSection = ref(
    sectionIds.includes(initialSection) ? initialSection : 'overview'
  )
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
    { id: 'overview', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'accounts', label: 'Tài khoản', icon: Users },
    { id: 'billing', label: 'Gói cước & Thanh toán', icon: CreditCard },
    { id: 'results', label: 'Kết quả học tập', icon: BarChart3 },
    { id: 'programs', label: 'Kỳ thi & chứng chỉ', icon: GraduationCap },
    { id: 'exams', label: 'Bài kiểm tra', icon: ClipboardCheck },
    { id: 'knowledge', label: 'Kho kiến thức', icon: Database },
  ]

  const currentTitle = computed(
    () =>
      activeSection.value === 'overview'
        ? 'Thống kê học tập'
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
  const displayAnalyticsData = computed(() => showAnalyticsSample.value ? analyticsSample : analyticsData.value)
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
      if (!search) return true
      return `${document.title} ${document.fileName} ${programName(document.certificateId)} ${knowledgeTopicName(document.topicId)}`
        .toLocaleLowerCase('vi')
        .includes(search)
    })
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
  const filteredUsers = computed(() =>
    users.value.filter((item) =>
      `${item.username} ${item.role}`
        .toLowerCase()
        .includes(query.value.toLowerCase())
    )
  )
  const filteredPrograms = computed(() =>
    programs.value.filter((item) =>
      `${item.name} ${item.code} ${item.description || ''}`
        .toLowerCase()
        .includes(query.value.toLowerCase())
    )
  )
  const filteredExams = computed(() =>
    exams.value.filter((item) =>
      `${item.title || ''} ${item.description || ''} ${programName(item.certificateId)}`
        .toLowerCase()
        .includes(query.value.toLowerCase())
    )
  )
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
      const requested = String(section || 'overview')
      activeSection.value = sectionIds.includes(requested)
        ? requested
        : 'overview'
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
    user.role = event.target.value
    try {
      await adminService.updateUserRole(user.userId, user.role)
    } catch (error) {
      user.role = oldRole
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật quyền tài khoản.'
    }
  }

  async function updateStatus(user) {
    const enabled = !user.enabled
    user.enabled = enabled
    try {
      await adminService.updateUserStatus(user.userId, enabled)
    } catch (error) {
      user.enabled = !enabled
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật trạng thái tài khoản.'
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
    if (!planForm.code.trim() || !planForm.name.trim()) {
      planModalError.value = 'Vui lòng nhập đầy đủ Mã gói và Tên gói.'
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
    try {
      await adminService.togglePlan(plan.planId)
      plan.isActive = !plan.isActive
      toast.success(
        plan.isActive
          ? `Đã kích hoạt gói "${plan.name}"`
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
      <a class="admin-brand" href="/admin">
        <span class="admin-brand-mark"><Shield :size="20" /></span>
        <span class="admin-brand-copy">
          <strong>Adaptive <em>Learning</em></strong>
          <small>Trung tâm quản trị</small>
        </span>
      </a>
      <div class="admin-workspace-label">QUẢN TRỊ HỌC TẬP</div>
      <nav class="admin-nav" aria-label="Điều hướng quản trị">
        <button
          v-for="item in navigation"
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
      </nav>
      <div class="admin-sidebar-bottom">
        <span class="admin-status-dot"></span>
        <div>
          <strong>Hệ thống hoạt động</strong>
          <small>Quyền quản trị</small>
        </div>
      </div>
    </aside>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <Tooltip :label="mobileMenuOpen ? 'Đóng menu điều hướng' : 'Mở menu điều hướng'">
          <template #trigger>
            <button class="admin-menu-button" :aria-label="mobileMenuOpen ? 'Đóng menu' : 'Mở menu'" @click="mobileMenuOpen = !mobileMenuOpen">
              <X v-if="mobileMenuOpen" :size="19" />
              <Menu v-else :size="19" />
            </button>
          </template>
        </Tooltip>
        <label class="admin-search">
          <Search :size="17" />
          <input
            v-model="query"
            type="search"
            placeholder="Tìm tài khoản, chương trình, bài kiểm tra..."
            aria-label="Tìm kiếm quản trị"
          />
        </label>
        <div class="admin-top-actions">
          <Tooltip label="Thông báo mới">
            <template #trigger>
              <button class="admin-notification" aria-label="Thông báo">
                <Bell :size="18" />
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
            <p class="admin-kicker">{{ activeSection === 'overview' ? 'BÁO CÁO HỌC TẬP' : 'TRUNG TÂM QUẢN TRỊ' }}</p>
            <h1>{{ currentTitle }}</h1>
            <p>{{ activeSection === 'overview' ? 'Kết quả làm bài và tiến độ người học trong kỳ được chọn.' : 'Quản lý tài khoản và nội dung phục vụ lộ trình học tập cá nhân.' }}</p>
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
          <div v-if="analyticsLoading" class="analytics-state analytics-loading" role="status" aria-live="polite">
            <span class="analytics-loading-line"></span><span>Đang tổng hợp kết quả học tập…</span>
          </div>
          <div v-else-if="analyticsError" class="analytics-state analytics-error" role="alert">
            <div><strong>Không thể tải báo cáo</strong><span>{{ analyticsError }}</span></div>
            <button type="button" class="analytics-retry" @click="loadAnalytics">Thử lại</button>
          </div>
          <div v-else-if="!displayAnalyticsData || !displayAnalyticsData.startedAttempts" class="analytics-state analytics-empty">
            <p class="analytics-overline">{{ analyticsPeriods.find((period) => period.id === analyticsPeriod)?.label }} · {{ programs.find((program) => String(program.certificateId) === String(analyticsCertificateId))?.name || 'Tất cả kỳ thi' }}</p>
            <h2>Chưa có lượt thi trong khoảng thời gian này</h2>
            <p>Khi người học bắt đầu làm bài, xu hướng điểm và tỷ lệ hoàn thành sẽ được tổng hợp tại đây.</p>
            <button type="button" class="analytics-sample-button" @click="showAnalyticsSample = true">Xem dữ liệu minh họa</button>
          </div>
          <template v-else>
            <p v-if="showAnalyticsSample" class="analytics-demo-notice">Dữ liệu minh họa, không phải số liệu thật</p>
            <div class="analytics-observation" aria-live="polite">
              <span>ĐIỂM ĐÁNG CHÚ Ý</span>
              <p>{{ analyticsInsight }}</p>
            </div>
            <section class="analytics-lead" aria-label="Tổng quan kết quả học tập">
              <div class="analytics-primary-metric">
                <span>TỶ LỆ HOÀN THÀNH</span>
                <strong>{{ formatPercent(displayAnalyticsData.completionRate) }}</strong>
                <div class="analytics-primary-meta">
                  <span>{{ formatAdminNumber(displayAnalyticsData.completedAttempts) }} / {{ formatAdminNumber(displayAnalyticsData.startedAttempts) }} lượt thi đã nộp</span>
                  <span v-if="displayAnalyticsData.completionRateChange != null" :class="['analytics-delta', displayAnalyticsData.completionRateChange > 0 ? 'positive' : displayAnalyticsData.completionRateChange < 0 ? 'negative' : 'neutral']">
                    {{ displayAnalyticsData.completionRateChange > 0 ? '+' : displayAnalyticsData.completionRateChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.completionRateChange) }} đpt
                  </span>
                </div>
                <p>So với {{ analyticsPeriods.find((period) => period.id === analyticsPeriod)?.label.toLowerCase() }} liền trước</p>
              </div>
              <dl class="analytics-supporting-metrics">
                <div><dt>Điểm trung bình</dt><dd>{{ formatPercent(displayAnalyticsData.averageScorePercent) }}</dd><small v-if="displayAnalyticsData.averageScoreChange != null" :class="['analytics-delta', displayAnalyticsData.averageScoreChange > 0 ? 'positive' : displayAnalyticsData.averageScoreChange < 0 ? 'negative' : 'neutral']">{{ displayAnalyticsData.averageScoreChange > 0 ? '+' : displayAnalyticsData.averageScoreChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.averageScoreChange) }} điểm so với kỳ trước</small><small v-else>Trên các lượt đã chấm</small></div>
                <div><dt>Tỷ lệ đạt</dt><dd>{{ formatPercent(displayAnalyticsData.passRate) }}</dd><small>Trong số bài đã chấm</small></div>
                <div><dt>Người học có lượt thi</dt><dd>{{ formatAdminNumber(displayAnalyticsData.activeLearners) }}</dd><small>Trong khoảng thời gian đã chọn</small></div>
              </dl>
            </section>
            <div class="analytics-detail-grid">
              <section class="analytics-chart-section" aria-labelledby="analytics-chart-title">
                <div class="analytics-section-heading">
                  <div><h2 id="analytics-chart-title">Điểm số theo thời gian</h2><p>Điểm trung bình và số lượt làm theo tuần</p></div>
                  <div class="analytics-legend" aria-label="Chú giải biểu đồ"><span class="legend-score">Điểm TB</span><span class="legend-volume">Số lượt</span></div>
                </div>
                <div v-if="!analyticsChartPoints.some((point) => point.y != null)" class="analytics-chart-empty">Chưa có bài thi được chấm điểm trong kỳ này.</div>
                <svg v-else class="analytics-chart" viewBox="0 0 720 300" role="img" aria-label="Biểu đồ điểm trung bình theo thời gian; cột xám biểu thị số lượt thi">
                  <g class="analytics-chart-grid">
                    <template v-for="tick in [0, 25, 50, 75, 100]" :key="tick">
                      <line x1="48" :y1="22 + (100 - tick) * 2.1" x2="672" :y2="22 + (100 - tick) * 2.1" />
                      <text x="36" :y="26 + (100 - tick) * 2.1">{{ tick }}</text>
                    </template>
                  </g>
                  <g class="analytics-chart-bars" aria-hidden="true">
                    <rect v-for="(point, index) in analyticsChartPoints" :key="`bar-${index}`" :x="point.x - 9" :y="256 - Math.max(2, point.attempts / analyticsMaxAttempts * 28)" width="18" :height="Math.max(2, point.attempts / analyticsMaxAttempts * 28)" rx="2" />
                  </g>
                  <path v-if="analyticsScorePath" class="analytics-score-line" :d="analyticsScorePath" />
                  <g v-for="(point, index) in analyticsChartPoints" :key="`point-${index}`">
                    <circle v-if="point.y != null" class="analytics-score-point" :cx="point.x" :cy="point.y" r="4"><title>{{ point.label }}: {{ formatPercent(point.averageScorePercent) }}</title></circle>
                    <g v-if="point.annotation" class="analytics-annotation">
                      <title>{{ point.annotation }} · {{ point.label }}</title>
                      <line :x1="point.x" :y1="256 - Math.max(2, point.attempts / analyticsMaxAttempts * 28) - 2" :x2="point.x" :y2="256 - Math.max(2, point.attempts / analyticsMaxAttempts * 28) - 14" />
                      <rect :x="Math.min(630, Math.max(48, point.x - 41))" :y="256 - Math.max(2, point.attempts / analyticsMaxAttempts * 28) - 32" width="82" height="18" rx="2" />
                      <text :x="Math.min(630, Math.max(48, point.x - 41)) + 41" :y="256 - Math.max(2, point.attempts / analyticsMaxAttempts * 28) - 19">{{ point.annotation }}</text>
                    </g>
                    <text :class="['analytics-chart-label', { 'label-hidden-mobile': analyticsChartPoints.length > 8 && index % 2 === 1 }]" :x="point.x" y="278">{{ point.label }}</text>
                  </g>
                  <text class="analytics-chart-unit" x="48" y="16">Điểm (%)</text>
                </svg>
              </section>
              <section class="analytics-learners-section" aria-labelledby="analytics-learners-title">
                <div class="analytics-section-heading">
                  <div><h2 id="analytics-learners-title">Người học cần theo dõi</h2><p>Xếp theo điểm trung bình thấp nhất</p></div>
                </div>
                <div v-if="!displayAnalyticsData.learnersToSupport.length" class="analytics-chart-empty">Chưa có kết quả đã chấm để xếp hạng.</div>
                <div v-else class="analytics-table-wrap">
                  <table class="analytics-table">
                    <thead><tr><th>Người học</th><th>Lượt</th><th>Điểm TB</th></tr></thead>
                    <tbody>
                      <tr v-for="learner in displayAnalyticsData.learnersToSupport" :key="learner.userId">
                        <td><strong>{{ learner.username }}</strong><small>{{ learner.lastAttemptAt ? formatAttemptDate(learner.lastAttemptAt) : '—' }}</small></td>
                        <td>{{ learner.attempts }}</td>
                        <td>{{ formatPercent(learner.averageScorePercent) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
          </template>
        </template>

        <section
          v-else-if="activeSection === 'accounts'"
          class="admin-panel admin-table-panel"
        >
          <div class="admin-panel-heading">
            <div>
              <h2>Tài khoản người dùng</h2>
              <p>{{ filteredUsers.length }} tài khoản khớp tìm kiếm</p>
            </div>
            <button class="admin-primary-button" @click="openAccount">
              <Users :size="16" />
              Tạo tài khoản
            </button>
          </div>
          <div v-if="loading" class="admin-empty admin-empty-loading">
            <LoaderCircle class="overlay-spinner" :size="22" />
            <span>Đang tải danh sách tài khoản…</span>
          </div>
          <div v-else-if="!filteredUsers.length" class="admin-empty">
            <div class="empty-icon-wrap">
              <Users :size="26" />
            </div>
            <strong>Không có tài khoản phù hợp</strong>
            <p>Không tìm thấy tài khoản người dùng nào khớp với bộ lọc tìm kiếm hiện tại.</p>
          </div>
          <div v-else class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Tài khoản</th>
                  <th>Loại tài khoản</th>
                  <th>Gói học</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in filteredUsers" :key="user.userId">
                  <td>
                    <span class="table-account">
                      <i
                        class="account-avatar"
                        :class="
                          user.role === 'ROLE_ADMIN' ? 'administrator' : ''
                        "
                      >
                        {{ user.username.slice(0, 1).toUpperCase() }}
                      </i>
                      <strong>{{ user.username }}</strong>
                    </span>
                  </td>
                  <td>
                    <select
                      class="role-select"
                      :value="user.role"
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
                      {{ !user.enabled ? 'Đã khóa' : user.emailVerified === false ? 'Chờ xác minh email' : 'Hoạt động' }}
                    </span>
                  </td>
                  <td>
                    <button v-if="user.role === 'ROLE_USER'" class="table-action" @click="openLearnerReport(user)">
                      Xem tiến độ
                    </button>
                    <button class="table-action" @click="updateStatus(user)">
                      {{ user.enabled ? 'Khóa' : 'Mở khóa' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
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
              <h2>Quản lý các gói thanh toán & User gói cước</h2>
              <p>Thiết lập danh mục các gói học tập, theo dõi tình trạng thanh toán của học viên và giám sát doanh thu toàn hệ thống.</p>
            </div>
            <div class="billing-hero-actions">
              <button v-if="billingSubTab === 'plans'" class="admin-primary-button" @click="openPlanModal()">
                <Plus :size="16" />
                Thêm gói cước mới
              </button>
              <button class="admin-secondary-button" @click="loadData">
                <RefreshCw :size="15" />
                Làm mới dữ liệu
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
            <div class="billing-plans-grid">
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
                    <strong>{{ Number(plan.price) === 0 ? '0đ' : `$${Number(plan.price).toFixed(2)}` }}</strong>
                    <small>{{ plan.currency }} / {{ plan.billingCycle === 'LIFETIME' ? 'Trọn đời' : plan.billingCycle === 'MONTHLY' ? 'Tháng' : plan.billingCycle === 'ANNUAL' ? 'Năm' : plan.billingCycle }}</small>
                  </div>
                </div>

                <p class="plan-card-desc">{{ plan.description || 'Chưa có mô tả cho gói cước này.' }}</p>

                <div class="plan-quota-badge">
                  <ClipboardCheck :size="14" />
                  <span>{{ plan.examLimit === -1 ? 'Không giới hạn lượt làm bài thi' : `Giới hạn: ${plan.examLimit} lượt thi thử` }}</span>
                </div>

                <ul class="plan-features-list">
                  <li v-for="(feat, idx) in parseFeatures(plan.featuresJson)" :key="idx">
                    <Check :size="15" /> {{ feat }}
                  </li>
                </ul>

                <div class="plan-card-footer">
                  <div class="plan-status-indicator">
                    <span class="status-dot" :class="{ active: plan.isActive }"></span>
                    <small>{{ plan.isActive ? 'Đang mở bán' : 'Tạm ẩn' }}</small>
                  </div>
                  <div class="plan-actions">
                    <button class="table-action" @click="togglePlanStatus(plan)">
                      {{ plan.isActive ? 'Tạm ẩn' : 'Mở bán' }}
                    </button>
                    <button class="table-action" @click="openPlanModal(plan)">
                      <Pencil :size="13" /> Sửa
                    </button>
                    <button
                      v-if="!['FREE', 'PREMIUM'].includes(plan.code)"
                      class="table-action danger"
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
                  <p>{{ filteredUserBillings.length }} tài khoản người học · Theo dõi mức chi tiêu và phân quyền gói dịch vụ.</p>
                </div>
                <div class="billing-table-filters">
                  <div class="billing-search-box">
                    <Search :size="15" />
                    <input
                      v-model="userBillingSearch"
                      type="search"
                      placeholder="Tìm theo ID, username..."
                    />
                  </div>
                  <select v-model="userBillingPlanFilter" class="billing-select-filter">
                    <option value="all">Tất cả gói</option>
                    <option value="PREMIUM">Gói Premium</option>
                    <option value="FREE">Gói Miễn Phí</option>
                  </select>
                </div>
              </div>

              <div v-if="loading" class="admin-empty">
                <LoaderCircle class="spin" :size="24" />
                <span>Đang tải dữ liệu học viên...</span>
              </div>
              <div v-else-if="!filteredUserBillings.length" class="admin-empty">
                <Users :size="36" class="admin-empty-icon" />
                <strong>Không tìm thấy tài khoản học viên phù hợp</strong>
              </div>
              <div v-else class="admin-table-wrap">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th>Học viên</th>
                      <th>Gói cước hiện tại</th>
                      <th>Ngày kích hoạt</th>
                      <th>Lượt thi dùng</th>
                      <th>Tổng chi tiêu</th>
                      <th>Số đơn</th>
                      <th>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in filteredUserBillings" :key="user.userId">
                      <td>
                        <span class="account-cell">
                          <i class="account-avatar">
                            {{ user.username.slice(0, 1).toUpperCase() }}
                          </i>
                          <div>
                            <strong>{{ user.username }}</strong>
                            <small class="table-secondary">ID: #{{ user.userId }}</small>
                          </div>
                        </span>
                      </td>
                      <td>
                        <select
                          class="role-select plan-select"
                          :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                          :value="user.plan || 'FREE'"
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
                        <div class="table-row-actions">
                          <button class="table-action" @click="viewUserPayments(user)">
                            <CreditCard :size="13" /> Lịch sử GD
                          </button>
                          <button
                            v-if="user.plan !== 'PREMIUM'"
                            class="table-action"
                            title="Reset lượt thi về 0"
                            @click="resetUserQuota(user)"
                          >
                            <RotateCcw :size="13" /> Reset lượt
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
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
                    />
                  </div>
                  <select v-model="paymentStatusFilter" class="billing-select-filter">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="SUCCEEDED">Thành công</option>
                    <option value="PENDING">Đang chờ</option>
                    <option value="FAILED">Thất bại</option>
                    <option value="REFUNDED">Hoàn tiền</option>
                  </select>
                  <select v-model="paymentProviderFilter" class="billing-select-filter">
                    <option value="all">Tất cả cổng</option>
                    <option value="STRIPE">Stripe</option>
                    <option value="PAYPAL">PayPal</option>
                  </select>
                </div>
              </div>

              <div v-if="loading" class="admin-empty">
                <LoaderCircle class="spin" :size="24" />
                <span>Đang tải lịch sử giao dịch...</span>
              </div>
              <div v-else-if="!filteredPayments.length" class="admin-empty">
                <CreditCard :size="36" class="admin-empty-icon" />
                <strong>Chưa có giao dịch thanh toán nào</strong>
                <p>Học viên có thể nâng cấp gói cước tại trang Bảng giá (/pricing) qua cổng Stripe hoặc PayPal.</p>
              </div>
              <div v-else class="admin-table-wrap">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th>Mã giao dịch</th>
                      <th>Học viên</th>
                      <th>Cổng thanh toán</th>
                      <th>Số tiền</th>
                      <th>Trạng thái</th>
                      <th>Thời gian</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="payment in filteredPayments" :key="payment.paymentId">
                      <td>
                        <span class="payment-id-badge">#PAY-{{ payment.paymentId }}</span>
                        <small v-if="payment.providerPaymentId" class="table-secondary">
                          Ref: {{ payment.providerPaymentId.slice(0, 18) }}...
                        </small>
                      </td>
                      <td>
                        <span class="account-cell">
                          <i class="account-avatar">
                            {{ (payment.username || 'U').slice(0, 1).toUpperCase() }}
                          </i>
                          <strong>{{ payment.username || `User #${payment.userId}` }}</strong>
                        </span>
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
                          {{
                            payment.status === 'SUCCEEDED'
                              ? 'Thành công'
                              : payment.status === 'PENDING'
                              ? 'Đang chờ'
                              : payment.status === 'FAILED'
                              ? 'Thất bại'
                              : payment.status === 'REFUNDED'
                              ? 'Đã hoàn tiền'
                              : payment.status
                          }}
                        </span>
                      </td>
                      <td>
                        <span class="payment-time">{{ formatPaymentDate(payment.createdAt) }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="activeSection === 'results'" class="learner-report">
          <div class="admin-panel learner-report-toolbar">
            <div>
              <h2>Kết quả và tiến độ học</h2>
              <p>Theo dõi lịch sử làm bài và tiến bộ của từng người học.</p>
            </div>
            <div class="learner-report-filters">
              <label>
                <span>Người học</span>
                <select v-model="selectedLearnerId" @change="learnerCertificateId = ''; loadLearnerReport()">
                  <option value="" disabled>Chọn tài khoản</option>
                  <option v-for="student in students" :key="student.userId" :value="String(student.userId)">{{ student.username }}</option>
                </select>
              </label>
              <label v-if="programs.length">
                <span>Kỳ thi</span>
                <select v-model="learnerCertificateId" @change="loadLearnerReport">
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }}</option>
                </select>
              </label>
            </div>
          </div>
          <div v-if="learnerError" class="learner-report-error" role="alert">{{ learnerError }}</div>
          <div v-if="learnerLoading" class="admin-empty">Đang tải báo cáo học tập…</div>
          <template v-else-if="selectedLearner">
            <div class="learner-summary-grid">
              <article class="learner-summary-card blue"><span>Lượt thi đã hoàn thành</span><strong>{{ completedLearnerAttempts.length }}</strong><small>{{ learnerScopedAttempts.length }} lượt thi trong kỳ đã chọn</small></article>
              <article class="learner-summary-card teal"><span>Điểm trung bình</span><strong>{{ learnerAverageScore == null ? '—' : `${Math.round(learnerAverageScore)}%` }}</strong><small>Trung bình tỷ lệ điểm các lượt đã chấm</small></article>
              <article class="learner-summary-card violet"><span>Lần thi gần nhất</span><strong>{{ learnerLatestAttempt ? formatAdminNumber(learnerLatestAttempt.bandScore ?? learnerLatestAttempt.score) : '—' }}</strong><small>{{ learnerLatestAttempt ? formatAttemptDate(learnerLatestAttempt.submittedAt || learnerLatestAttempt.startedAt) : 'Chưa có lượt thi' }}</small></article>
              <article class="learner-summary-card amber"><span>Kỹ năng / môn có dữ liệu</span><strong>{{ learnerSkillCount }}</strong><small>Đo từ kết quả bài thi đã chấm</small></article>
            </div>
            <section class="admin-panel learner-history-panel">
              <div class="admin-panel-heading"><div><h2>Lịch sử bài thi</h2><p>{{ selectedLearner.username }} · {{ learnerScopedAttempts.length }} lượt trong kỳ đã chọn</p></div></div>
              <div v-if="!learnerScopedAttempts.length" class="admin-empty">Tài khoản này chưa có lượt làm bài trong kỳ thi đang chọn.</div>
              <div v-else class="admin-table-wrap">
                <table class="admin-table learner-history-table">
                  <thead><tr><th>Bài thi</th><th>Ngày làm</th><th>Trạng thái</th><th>Điểm</th><th>Band</th><th>Vi phạm</th><th></th></tr></thead>
                  <tbody><tr v-for="attempt in learnerScopedAttempts" :key="attempt.attemptId">
                    <td><strong>{{ examTitle(attempt) }}</strong><small class="table-secondary">Lượt #{{ attempt.attemptId }}</small></td>
                    <td>{{ formatAttemptDate(attempt.submittedAt || attempt.startedAt) }}</td>
                    <td><span class="attempt-state" :class="String(attempt.status || '').toLowerCase()">{{ attempt.status === 'GRADED' ? 'Đã chấm' : attempt.status === 'SUBMITTED' ? 'Đã nộp' : attempt.status === 'IN_PROGRESS' ? 'Đang làm' : attempt.status || '—' }}</span></td>
                    <td>{{ formatAttemptScore(attempt) }}</td><td>{{ attempt.bandScore ?? '—' }}</td><td>{{ attempt.violationCount ?? 0 }}</td><td><button class="table-action" :disabled="!attempt.submittedAt || attempt.score == null" @click="viewAttemptResult(attempt)">Chi tiết</button></td>
                  </tr></tbody>
                </table>
              </div>
            </section>
            <section class="admin-panel learner-skill-panel">
              <div><h2>Tiến độ theo kỹ năng</h2><p>{{ programs.find((program) => String(program.certificateId) === String(learnerCertificateId))?.name || 'Chọn kỳ thi để xem snapshot' }}</p></div>
              <div v-if="learnerProgress?.masteryBySkillJson && Object.keys(parseJson(learnerProgress.masteryBySkillJson)).length" class="skill-progress-list">
                <div v-for="(value, skill) in parseJson(learnerProgress.masteryBySkillJson)" :key="skill" class="skill-progress-row"><span>{{ skill }}</span><div class="skill-progress-track"><i :style="{ width: `${Math.min(100, Math.max(0, Number(value) || 0))}%` }"></i></div><strong>{{ Math.round(Number(value) || 0) }}%</strong></div>
              </div>
              <div v-else class="learner-progress-empty"><BarChart3 :size="20" /><span>Chưa có dữ liệu phân tích kỹ năng cho kỳ thi này.</span></div>
            </section>
            <section class="admin-panel learner-trend-panel">
              <div><h2>Diễn biến điểm thi</h2><p>{{ learnerScoreTrend.length }} lượt đã chấm gần nhất</p></div>
              <div v-if="learnerScoreTrend.length" class="learner-trend-chart" role="img" aria-label="Biểu đồ diễn biến điểm thi">
                <div v-for="item in learnerScoreTrend" :key="item.attemptId" class="learner-trend-point" :title="`Lượt ${item.attemptId}: ${item.scorePercent}%`"><strong>{{ Math.round(Number(item.scorePercent) || 0) }}%</strong><i :style="{ height: `${Math.max(8, Math.min(100, Number(item.scorePercent) || 0))}%` }"></i><small>{{ formatTrendDate(item.submittedAt) }}</small></div>
              </div>
              <div v-else class="learner-progress-empty"><BarChart3 :size="20" /><span>Chưa có điểm đã chấm để so sánh theo thời gian.</span></div>
            </section>
          </template>
          <div v-else class="admin-empty">Chưa có tài khoản người học để hiển thị.</div>
        </section>

        <section
          v-else-if="activeSection === 'programs'"
          class="admin-panel admin-table-panel"
        >
          <div class="admin-panel-heading">
            <div>
              <h2>Chứng chỉ và kỳ thi</h2>
              <p>{{ filteredPrograms.length }} danh mục · IELTS, TOEIC, THPT</p>
            </div>
            <button class="admin-primary-button" @click="openProgram">
              <GraduationCap :size="16" />
              Tạo chứng chỉ / kỳ thi
            </button>
          </div>
          <div v-if="loading" class="admin-empty">Đang tải chương trình...</div>
          <div v-else-if="!filteredPrograms.length" class="admin-empty">
            <div class="catalog-empty-state">
              <strong>Khởi tạo catalog kỳ thi</strong>
              <span>
                Chọn bộ dữ liệu chuẩn theo thang điểm và kỹ năng trong
                BUSINESS.md.
              </span>
              <div class="preset-actions">
                <button
                  v-for="preset in certificatePresets"
                  :key="preset.code"
                  class="preset-button"
                  @click="openPreset(preset)"
                >
                  <GraduationCap :size="15" />
                  <span>{{ preset.name }}</span>
                  <small>{{ preset.totalScore }} điểm</small>
                </button>
              </div>
            </div>
          </div>
          <div v-else class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Chứng chỉ / kỳ thi</th>
                  <th>Mã</th>
                  <th>Mô tả</th>
                  <th>Tổng điểm</th>
                  <th>Điểm mục tiêu</th>
                  <th>Kỹ năng / môn</th>
                  <th>Số đề</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="program in filteredPrograms"
                  :key="program.certificateId"
                >
                  <td>
                    <strong>{{ program.name }}</strong>
                  </td>
                  <td>{{ program.code }}</td>
                  <td class="description-cell">
                    {{ program.description || '—' }}
                  </td>
                  <td>{{ program.totalScore }}</td>
                  <td>{{ program.passingBand || program.passingScore }}</td>
                  <td class="description-cell">
                    {{ skillLabels(program.skills) }}
                  </td>
                  <td>
                    {{
                      exams.filter(
                        (exam) => exam.certificateId === program.certificateId
                      ).length
                    }}
                  </td>
                  <td>
                    <button class="table-action" @click="openProgram(program)">
                      Sửa
                    </button>
                    <button
                      class="table-action danger"
                      @click="deleteProgram(program)"
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section
          v-else-if="activeSection === 'exams'"
          class="admin-panel admin-table-panel"
        >
          <div class="admin-panel-heading">
            <div>
              <h2>Bài kiểm tra</h2>
              <p>{{ filteredExams.length }} đề · nhóm theo kỹ năng / môn thi</p>
            </div>
            <button class="admin-primary-button" :disabled="!programs.length || !topics.length" @click="showExamBuilder = true">
              <Plus :size="16" /> Tạo bài kiểm tra
            </button>
          </div>
          <div v-if="loading" class="admin-empty admin-empty-loading">
            <LoaderCircle class="overlay-spinner" :size="22" />
            <span>Đang tải danh sách bài kiểm tra…</span>
          </div>
          <div v-else-if="!filteredExams.length" class="admin-empty">
            <div class="empty-icon-wrap">
              <ClipboardCheck :size="26" />
            </div>
            <strong>Chưa có bài kiểm tra nào</strong>
            <p>Bắt đầu tạo bài kiểm tra đầu tiên để phục vụ luyện tập và đánh giá năng lực học viên.</p>
            <button class="admin-primary-button" :disabled="!programs.length || !topics.length" @click="showExamBuilder = true">
              <Plus :size="15" /> Tạo bài kiểm tra
            </button>
          </div>
          <div v-else class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Đề kiểm tra</th>
                  <th>Kỳ thi / kỹ năng</th>
                  <th>Mục tiêu</th>
                  <th>Thời lượng</th>
                  <th>Số câu</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="exam in filteredExams" :key="exam.examId">
                  <td>
                    <strong>{{ exam.title }}</strong>
                    <small class="secondary-text">
                      {{ exam.description || 'Bài đánh giá học tập' }}
                    </small>
                  </td>
                  <td>{{ programName(exam.certificateId) }} · {{ topicName(exam.topicId) }}</td>
                  <td>{{ exam.targetScoreMin != null ? `${exam.targetScoreMin}–${exam.targetScoreMax} ${exam.targetScoreUnit || ''}` : '—' }}</td>
                  <td>{{ exam.durationMinutes || 0 }} phút</td>
                  <td>{{ exam.questionCount || exam.totalQuestions || 0 }}</td>
                  <td>
                    <span
                      :class="[
                        'account-state',
                        exam.status === 'PUBLISHED' ? 'enabled' : 'disabled',
                      ]"
                    >
                      {{
                        exam.status === 'PUBLISHED'
                          ? 'Đã phát hành'
                          : exam.status || 'Bản nháp'
                      }}
                    </span>
                  </td>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <template v-else>
          <section class="knowledge-page">
            <header class="knowledge-page-heading">
              <div class="knowledge-breadcrumb">
                <span>Quản trị</span><span aria-hidden="true">/</span>
                <strong>Kho kiến thức</strong>
              </div>
              <h1>Kho kiến thức</h1>
              <p>Tài liệu PDF được phân nhóm theo kỳ thi.</p>
            </header>

            <section class="knowledge-summary" aria-label="Tổng quan tài liệu">
              <article class="knowledge-stat stat-blue">
                <span class="knowledge-stat-icon"><FileText :size="19" /></span>
                <div><span>Tổng tài liệu</span><strong>{{ formatAdminNumber(knowledgeDocuments.length) }}</strong><small>PDF trong kho</small></div>
              </article>
              <article class="knowledge-stat stat-teal">
                <span class="knowledge-stat-icon"><CircleCheck :size="19" /></span>
                <div><span>Đang sử dụng</span><strong>{{ formatAdminNumber(activeKnowledgeCount) }}</strong><small>{{ activeKnowledgePercent }}% tổng tài liệu</small></div>
              </article>
              <article class="knowledge-stat stat-amber">
                <span class="knowledge-stat-icon"><CirclePause :size="19" /></span>
                <div><span>Đã tắt</span><strong>{{ formatAdminNumber(inactiveKnowledgeCount) }}</strong><small>Không dùng khi tạo câu hỏi</small></div>
              </article>
              <article class="knowledge-stat stat-violet">
                <span class="knowledge-stat-icon"><Database :size="19" /></span>
                <div><span>Tổng đoạn kiến thức</span><strong>{{ formatAdminNumber(knowledgeChunkCount) }}</strong><small>Tách từ tất cả tài liệu</small></div>
              </article>
            </section>

            <section class="knowledge-layout">
                <form id="kb-add" class="knowledge-card knowledge-import" @submit.prevent="importKnowledge">
                <div class="knowledge-card-heading">
                  <span class="knowledge-heading-icon"><Plus :size="20" /></span>
                  <div><h2>Thêm tài liệu vào kho</h2><p>File PDF theo kỳ thi và kỹ năng/môn thi.</p></div>
                </div>

                <label
                  class="knowledge-dropzone"
                  :class="{ 'is-dragging': knowledgeDragging }"
                  @dragenter.prevent="knowledgeDragging = true"
                  @dragover.prevent="knowledgeDragging = true"
                  @dragleave="knowledgeDragging = false"
                  @drop="dropKnowledgeFiles"
                >
                  <input type="file" accept=".pdf,application/pdf" multiple @change="readKnowledgeFiles" />
                  <span class="knowledge-upload-icon"><Upload :size="23" /></span>
                  <strong>Kéo thả PDF hoặc chọn từ máy</strong>
                  <small>PDF · tối đa 20 MB mỗi tệp</small>
                </label>
                <small v-if="knowledgeFormErrors.files" class="knowledge-field-error" role="alert">{{ knowledgeFormErrors.files }}</small>

                <label class="knowledge-field">
                  <span>Kỳ thi <b class="required-mark" aria-hidden="true">*</b></span>
                  <select v-model="knowledgeForm.certificateId" required :aria-invalid="!!knowledgeFormErrors.exam" @invalid="knowledgeFormErrors.exam = 'Chọn kỳ thi cho tài liệu.'" @change="knowledgeFormErrors.exam = ''">
                    <option disabled value="">Chọn kỳ thi</option>
                    <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                      {{ program.name }}
                    </option>
                  </select>
                  <small v-if="knowledgeFormErrors.exam" class="knowledge-field-error" role="alert">{{ knowledgeFormErrors.exam }}</small>
                </label>

                <div v-if="knowledgeTopics.length" class="knowledge-skill-field">
                  <div id="knowledge-skill-heading" class="knowledge-skill-heading"><span>Kỹ năng / môn thi</span><small>Tùy chọn</small></div>
                  <div class="knowledge-skill-choice" role="group" aria-labelledby="knowledge-skill-heading">
                    <button type="button" class="knowledge-skill-chip skill-auto" :class="{ selected: !knowledgeForm.topicId }" :aria-pressed="!knowledgeForm.topicId" @click="knowledgeForm.topicId = ''">
                      <Check v-if="!knowledgeForm.topicId" :size="15" />
                      <span v-else class="knowledge-skill-dot"></span>Tất cả
                    </button>
                    <button
                      v-for="topic in knowledgeTopics"
                      :key="topic.topicId"
                      type="button"
                      class="knowledge-skill-chip"
                      :class="[knowledgeSkillTone(topic), { selected: String(topic.topicId) === knowledgeForm.topicId }]"
                      :aria-pressed="String(topic.topicId) === knowledgeForm.topicId"
                      @click="knowledgeForm.topicId = String(topic.topicId)"
                    >
                      <Check v-if="String(topic.topicId) === knowledgeForm.topicId" :size="15" />
                      <span v-else class="knowledge-skill-dot"></span>{{ topic.name }}
                    </button>
                  </div>
                </div>

                <div v-if="knowledgeFiles.length" class="knowledge-upload-list">
                  <div v-for="item in knowledgeFiles" :key="item.id" class="knowledge-upload-item">
                    <span class="queued-file-icon"><FileText :size="17" /></span>
                    <div class="queued-file-info">
                      <strong :title="item.file.name">{{ item.file.name }}</strong>
                      <small>{{ (item.file.size / 1024 / 1024).toFixed(1) }} MB</small>
                    </div>
                    <label class="knowledge-field queued-title">
                      <span>Tiêu đề <b class="required-mark" aria-hidden="true">*</b></span>
                      <input v-model.trim="item.title" required maxlength="200" :aria-invalid="!!knowledgeFormErrors.titles && !item.title.trim()" @invalid="knowledgeFormErrors.titles = 'Nhập tiêu đề cho từng tài liệu.'" @input="knowledgeFormErrors.titles = ''" />
                    </label>
                    <button type="button" class="knowledge-icon-button danger" :disabled="importingKnowledge" :aria-label="`Bỏ ${item.file.name}`" title="Bỏ tệp" @click="removeKnowledgeFile(item.id)">
                      <X :size="17" />
                    </button>
                  </div>
                </div>
                <small v-if="knowledgeFormErrors.titles" class="knowledge-field-error" role="alert">{{ knowledgeFormErrors.titles }}</small>

                <div v-if="knowledgeNotice" class="knowledge-notice" role="status">
                  <CheckCircle2 :size="18" />
                  <span>Đã thêm <strong>“{{ knowledgeNotice }}”</strong> vào kho kiến thức.</span>
                  <button type="button" class="knowledge-notice-close" aria-label="Đóng thông báo" @click="closeKnowledgeNotice"><X :size="17" /></button>
                </div>
                <footer class="knowledge-form-footer">
                  <span class="knowledge-form-hint"><Info :size="16" /> Tài liệu mới được bật ngay sau khi nhập.</span>
                  <div class="knowledge-form-actions">
                    <button type="button" class="knowledge-clear" @click="clearKnowledgeForm">Làm mới</button>
                    <button class="knowledge-submit" type="submit" :disabled="importingKnowledge">
                      <Database :size="17" />
                      {{ importingKnowledge ? 'Đang nhập...' : 'Đưa vào kho kiến thức' }}
                    </button>
                  </div>
                </footer>
              </form>

              <section class="knowledge-library-column">
                <div class="knowledge-card knowledge-list">
                  <div class="knowledge-card-heading library-heading">
                    <div><h2>Tài liệu đã nhập</h2><p>Dùng chung cho mọi lộ trình học.</p></div>
                    <span class="knowledge-total-badge">{{ formatAdminNumber(knowledgeDocuments.length) }}</span>
                  </div>
                  <div v-if="knowledgeDocuments.length" class="knowledge-library-tools">
                    <label class="knowledge-search">
                      <Search :size="17" />
                      <input v-model="knowledgeSearch" type="search" placeholder="Tìm tên tài liệu, file, kỳ thi" aria-label="Tìm trong kho kiến thức" />
                    </label>
                    <div class="knowledge-filter" role="group" aria-label="Lọc tài liệu theo trạng thái">
                      <button v-for="filter in [
                        { id: 'all', label: 'Tất cả', count: knowledgeDocuments.length },
                        { id: 'active', label: 'Đang dùng', count: activeKnowledgeCount },
                        { id: 'inactive', label: 'Đã tắt', count: inactiveKnowledgeCount },
                      ]" :key="filter.id" type="button" :aria-pressed="knowledgeFilter === filter.id" @click="knowledgeFilter = filter.id">
                        {{ filter.label }} <span>{{ formatAdminNumber(filter.count) }}</span>
                      </button>
                    </div>
                  </div>

                  <div v-if="!knowledgeDocuments.length" class="knowledge-empty">
                    <span><Database :size="25" /></span>
                    <strong>Chưa có tài liệu</strong>
                    <small>Các tài liệu bạn thêm sẽ xuất hiện ở đây.</small>
                    <a class="knowledge-empty-link" href="#kb-add">Thêm tài liệu đầu tiên</a>
                  </div>
                  <div v-else-if="!filteredKnowledgeDocuments.length" class="knowledge-empty compact-empty">
                    <strong>Không tìm thấy tài liệu</strong>
                    <small>Thử từ khóa hoặc bộ lọc khác.</small>
                  </div>
                  <ul v-else class="knowledge-document-list">
                    <li v-for="document in filteredKnowledgeDocuments" :key="document.documentId" class="knowledge-document" :class="{ 'is-inactive': !document.active }">
                      <span class="knowledge-document-icon">PDF</span>
                      <div class="knowledge-document-info">
                        <strong :title="document.title">{{ document.title }}</strong>
                        <small class="knowledge-file-name">{{ document.fileName }}</small>
                        <span class="knowledge-document-meta">
                          <span v-if="knowledgeTopicName(document.topicId)" class="knowledge-skill-tag" :class="knowledgeSkillTone(knowledgeTopicName(document.topicId))"><span class="knowledge-skill-dot"></span>{{ knowledgeTopicName(document.topicId) }}</span>
                          <span>{{ programName(document.certificateId) }} · {{ formatAdminNumber(document.chunkCount) }} đoạn · {{ knowledgeDate(document.createdAt) }}</span>
                        </span>
                      </div>
                      <div class="knowledge-document-actions">
                        <Tooltip :label="document.active ? 'Ngừng dùng tài liệu' : 'Kích hoạt tài liệu'">
                          <template #trigger>
                            <button type="button" class="knowledge-switch" role="switch" :aria-checked="document.active" :aria-label="`${document.active ? 'Tắt' : 'Bật'} ${document.title}`" @click="toggleKnowledgeActive(document)">
                              <span></span>
                            </button>
                          </template>
                        </Tooltip>
                        <small class="knowledge-switch-label">{{ document.active ? 'Đang dùng' : 'Đã tắt' }}</small>
                        <DropdownMenu trigger-label="Tùy chọn tài liệu">
                          <template #trigger>
                            <button type="button" class="knowledge-icon-button" :aria-label="`Tùy chọn ${document.title}`">
                              <MoreHorizontal :size="19" aria-hidden="true" />
                            </button>
                          </template>
                          <DropdownMenuLabel>{{ document.title }}</DropdownMenuLabel>
                          <DropdownMenuItem @select="viewKnowledgeDocument(document)">
                            <template #icon><Eye :size="16" /></template>Xem trước đoạn kiến thức
                          </DropdownMenuItem>
                          <DropdownMenuItem @select="editKnowledgeDocument(document)">
                            <template #icon><Pencil :size="16" /></template>Sửa tài liệu
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem destructive @select="removeKnowledge(document)">
                            <template #icon><Trash2 :size="16" /></template>Xóa tài liệu
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </div>
                    </li>
                  </ul>
                </div>
              </section>
            </section>
          </section>
        </template>
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
        <p v-if="adminModalError" class="overlay-form-error" role="alert">{{ adminModalError }}</p>
        <label class="admin-overlay-field">Tên chứng chỉ / kỳ thi <b aria-hidden="true">*</b>
          <input v-model.trim="programForm.name" required maxlength="120" placeholder="vd: IELTS Academic, TOEIC, THPT Quốc Gia" />
        </label>
        <label class="admin-overlay-field">Mã kỳ thi <b aria-hidden="true">*</b>
          <input v-model.trim="programForm.code" required maxlength="50" placeholder="IELTS, TOEIC hoặc THPT" />
        </label>
        <label class="admin-overlay-field">Mô tả
          <textarea v-model.trim="programForm.description" rows="3" placeholder="Thang điểm, mục tiêu và kỹ năng/môn thi"></textarea>
        </label>
        <div class="form-row">
          <label class="admin-overlay-field">Tổng điểm <b aria-hidden="true">*</b><input v-model.number="programForm.totalScore" type="number" min="1" required /></label>
          <label class="admin-overlay-field">Điểm đạt <b aria-hidden="true">*</b><input v-model.number="programForm.passingScore" type="number" min="0" required /></label>
          <label class="admin-overlay-field">Band / điểm mục tiêu<input v-model.number="programForm.passingBand" type="number" min="0" step="0.5" /></label>
        </div>
        <label class="admin-overlay-field">Kỹ năng / môn thi (JSON) <b aria-hidden="true">*</b>
          <textarea v-model.trim="programForm.skills" rows="3" required placeholder='["LISTENING","READING"]'></textarea>
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
        <label class="form-field">
          <span>Mật khẩu hiện tại</span>
          <input v-model="adminPasswordForm.oldPassword" type="password" placeholder="Nhập mật khẩu cũ" required />
        </label>
        <label class="form-field">
          <span>Mật khẩu mới</span>
          <input v-model="adminPasswordForm.newPassword" type="password" placeholder="Tối thiểu 6 ký tự" required />
        </label>
        <label class="form-field">
          <span>Nhập lại mật khẩu mới</span>
          <input v-model="adminPasswordForm.confirmPassword" type="password" placeholder="Xác nhận mật khẩu mới" required />
        </label>
        <div class="form-actions" style="margin-top: 14px; display: flex; justify-content: flex-end; gap: 8px;">
          <button type="button" class="overlay-button overlay-button-secondary" @click="adminPasswordModalOpen = false">Hủy</button>
          <button type="submit" class="overlay-button overlay-button-primary" :disabled="adminChangingPassword">Lưu mật khẩu mới</button>
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
    width: 256px;
    flex-direction: column;
    padding: 18px 12px;
    border-right: 1px solid #E2E8F0;
    background: #FFFFFF;
    color: #475569;
  }
  .admin-brand {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 6px 10px;
    margin-bottom: 6px;
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
    font-size: 14.5px;
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
  .admin-workspace-label {
    padding: 12px 10px 6px;
    color: #94A3B8;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }
  .admin-nav {
    display: flex;
    flex-direction: column;
    gap: 3px;
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
    display: none;
  }
  .admin-sidebar-bottom {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: auto;
    padding: 10px 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #F8FAFC;
    color: #334155;
  }
  .admin-status-dot {
    width: 7px;
    height: 7px;
    flex: 0 0 auto;
    border-radius: 50%;
    background: #10B981;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
  }
  .admin-sidebar-bottom div {
    display: grid;
    min-width: 0;
    gap: 1px;
  }
  .admin-sidebar-bottom strong {
    font-size: 12px;
    font-weight: 600;
    color: #1E293B;
  }
  .admin-sidebar-bottom small {
    color: #64748B;
    font-size: 11px;
  }
  .admin-workspace {
    min-width: 0;
    width: calc(100% - 256px);
    box-sizing: border-box;
    margin-left: 256px;
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
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(12px);
  }
  .admin-search {
    display: flex;
    width: min(100%, 420px);
    min-width: 0;
    height: 38px;
    flex: 1 1 300px;
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
    border-color: #0F766E;
    box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
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
    background: #0F766E;
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
    color: #0F766E;
  }
  .admin-menu-avatar {
    background: linear-gradient(135deg, #0F766E, #0E7490) !important;
  }
  .badge-admin {
    background: #E6F7F3 !important;
    color: #0F766E !important;
    border: 1px solid #C9ECE2 !important;
  }
  .admin-menu-button {
    display: none;
  }
  .admin-content {
    width: 100%;
    max-width: 1440px;
    min-width: 0;
    box-sizing: border-box;
    margin: 0 auto;
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
    color: #0F766E;
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
  .analytics-controls { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
  .analytics-time-filter { display: inline-flex; min-height: 44px; align-items: center; gap: 2px; padding: 3px; border: 1px solid var(--ds-border); border-radius: var(--radius-lg); background: var(--surface); }
  .analytics-time-filter button { min-width: 60px; min-height: 36px; padding: 0 10px; border: 0; border-radius: 8px; color: var(--text-secondary); background: transparent; font: inherit; font-size: 12px; font-weight: 500; cursor: pointer; }
  .analytics-time-filter button:hover { background: var(--surface-hover); }
  .analytics-time-filter button.active { color: var(--primary-ink); background: var(--primary-soft); font-weight: 600; }
  .analytics-exam-filter select { min-width: 156px; max-width: 240px; min-height: 44px; padding: 0 34px 0 12px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text-strong); background: var(--surface); font: inherit; font-size: 13px; }
  .analytics-state { min-height: 240px; border-top: 1px solid var(--ds-border); border-bottom: 1px solid var(--ds-border); }
  .analytics-loading { display: flex; align-items: center; gap: 14px; color: var(--text-muted); font-size: 13px; }
  .analytics-loading-line { width: min(180px, 32%); height: 6px; overflow: hidden; background: var(--surface-muted); }
  .analytics-loading-line::after { display: block; width: 35%; height: 100%; background: var(--primary); content: ''; animation: analytics-loading 1.2s ease-in-out infinite; }
  .analytics-error { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 24px; }
  .analytics-error > div { display: grid; gap: 4px; }
  .analytics-error strong { color: var(--text-strong); font-size: 15px; }
  .analytics-error span { color: var(--text-muted); font-size: 13px; }
  .analytics-retry { min-height: 44px; padding: 0 16px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text-strong); background: var(--surface); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
  .analytics-empty { display: flex; min-height: 280px; flex-direction: column; align-items: flex-start; justify-content: center; padding: 30px 4px; }
  .analytics-overline { margin: 0 0 9px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
  .analytics-empty h2 { margin: 0; color: var(--text-strong); font-size: 20px; line-height: 28px; font-weight: 600; }
  .analytics-empty > p:last-child { max-width: 500px; margin: 8px 0 0; color: var(--text-muted); font-size: 13px; line-height: 1.6; }
  .analytics-sample-button { min-height: 44px; margin-top: 18px; padding: 0 14px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text-strong); background: var(--surface); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
  .analytics-sample-button:hover { border-color: var(--primary-border); color: var(--primary-ink); background: var(--primary-tint); }
  .analytics-demo-notice { margin: 0 0 12px; padding-bottom: 8px; border-bottom: 1px solid var(--border-subtle); color: var(--text-muted); font-size: 11px; font-weight: 600; }
  .analytics-observation { display: grid; grid-template-columns: 132px minmax(0, 1fr); align-items: baseline; gap: 14px; padding: 0 0 16px; border-bottom: 1px solid var(--border-subtle); }
  .analytics-observation > span { color: var(--primary-ink); font-size: 11px; font-weight: 700; }
  .analytics-observation p { margin: 0; color: var(--text-secondary); font-size: 14px; line-height: 1.55; }
  .analytics-lead { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, .8fr); gap: clamp(24px, 5vw, 72px); padding: 30px 0 28px; border-bottom: 1px solid var(--ds-border); }
  .analytics-primary-metric { display: flex; min-width: 0; flex-direction: column; align-items: flex-start; justify-content: center; }
  .analytics-primary-metric > span { color: var(--text-overline); font-size: 11px; font-weight: 700; }
  .analytics-primary-metric > strong { margin: 8px 0 10px; color: var(--text); font-size: 64px; line-height: 1.05; font-weight: 600; font-variant-numeric: tabular-nums; }
  .analytics-primary-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; color: var(--text-secondary); font-size: 13px; }
  .analytics-delta { font-size: 12px; font-weight: 600; font-variant-numeric: tabular-nums; }
  .analytics-delta.positive { color: var(--primary-ink); }
  .analytics-delta.negative { color: var(--text-secondary); }
  .analytics-delta.neutral { color: var(--text-muted); }
  .analytics-primary-metric > p { margin: 7px 0 0; color: var(--text-muted); font-size: 12px; }
  .analytics-supporting-metrics { display: grid; align-content: center; margin: 0; }
  .analytics-supporting-metrics > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: baseline; gap: 5px 16px; padding: 12px 0; border-bottom: 1px solid var(--border-subtle); }
  .analytics-supporting-metrics > div:last-child { border-bottom: 0; }
  .analytics-supporting-metrics dt { color: var(--text-muted); font-size: 12px; }
  .analytics-supporting-metrics dd { grid-column: 2; grid-row: 1 / span 2; margin: 0; color: var(--text-strong); font-size: 23px; line-height: 29px; font-weight: 600; font-variant-numeric: tabular-nums; }
  .analytics-supporting-metrics small { color: var(--text-muted); font-size: 11px; }
  .analytics-detail-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(300px, .85fr); gap: clamp(24px, 4vw, 56px); padding-top: 26px; }
  .analytics-chart-section,
  .analytics-learners-section { min-width: 0; }
  .analytics-section-heading { display: flex; min-height: 54px; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
  .analytics-section-heading h2 { margin: 0; color: var(--text-strong); font-size: 16px; line-height: 23px; font-weight: 600; }
  .analytics-section-heading p { margin: 3px 0 0; color: var(--text-muted); font-size: 12px; line-height: 1.5; }
  .analytics-legend { display: flex; flex-wrap: wrap; gap: 12px; padding-top: 4px; color: var(--text-muted); font-size: 11px; }
  .analytics-legend span { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
  .analytics-legend span::before { display: inline-block; width: 15px; height: 2px; background: var(--primary); content: ''; }
  .analytics-legend .legend-volume::before { width: 8px; height: 8px; background: var(--surface-muted); }
  .analytics-chart { display: block; width: 100%; height: auto; overflow: visible; }
  .analytics-chart-grid line { stroke: var(--border-subtle); stroke-width: 1; }
  .analytics-chart-grid text,
  .analytics-chart-unit { fill: var(--text-muted); font-family: var(--font-sans); font-size: 11px; text-anchor: end; }
  .analytics-chart-unit { text-anchor: start; }
  .analytics-chart-bars rect { fill: var(--surface-muted); }
  .analytics-score-line { fill: none; stroke: var(--primary); stroke-linecap: round; stroke-linejoin: round; stroke-width: 2.5; }
  .analytics-score-point { fill: var(--surface); stroke: var(--primary); stroke-width: 2.5; }
  .analytics-chart-label { fill: var(--text-muted); font-family: var(--font-sans); font-size: 10px; text-anchor: middle; }
  .analytics-annotation line { stroke: var(--primary); stroke-dasharray: 2 3; stroke-width: 1; }
  .analytics-annotation rect { fill: var(--surface); stroke: var(--primary-border); stroke-width: 1; }
  .analytics-annotation text { fill: var(--primary-ink); font-family: var(--font-sans); font-size: 9px; font-weight: 600; text-anchor: middle; }
  .analytics-chart-empty { display: flex; min-height: 208px; align-items: center; justify-content: center; padding: 20px; border-top: 1px solid var(--border-subtle); border-bottom: 1px solid var(--border-subtle); color: var(--text-muted); font-size: 13px; text-align: center; }
  .analytics-table-wrap { width: 100%; overflow-x: auto; }
  .analytics-table { width: 100%; border-collapse: collapse; text-align: left; }
  .analytics-table th { height: 38px; border-bottom: 1px solid var(--ds-border); color: var(--text-muted); font-size: 11px; font-weight: 600; white-space: nowrap; }
  .analytics-table td { height: 58px; border-bottom: 1px solid var(--border-subtle); color: var(--text-secondary); font-size: 12px; font-variant-numeric: tabular-nums; }
  .analytics-table tbody tr:last-child td { border-bottom: 1px solid var(--ds-border); }
  .analytics-table th:not(:first-child),
  .analytics-table td:not(:first-child) { padding-left: 12px; text-align: right; }
  .analytics-table td:first-child { max-width: 170px; }
  .analytics-table td strong { display: block; overflow: hidden; color: var(--text-strong); font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
  .analytics-table td small { display: block; margin-top: 3px; color: var(--text-muted); font-size: 10px; }
  @keyframes analytics-loading { from { transform: translateX(-120%); } to { transform: translateX(320%); } }
  @media (prefers-reduced-motion: reduce) {
    .analytics-loading-line::after { animation: none; }
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
    background: #0F766E;
    color: #FFFFFF;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  .admin-primary-button:hover:not(:disabled) {
    background: #115E59;
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
    color: #0F766E;
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
    color: #0F766E;
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
  .knowledge-layout {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 20px;
  }
  .knowledge-layout > .knowledge-import { flex: 1.5 1 540px; }
  .knowledge-layout > .knowledge-library-column { flex: 1 1 360px; }
  .knowledge-page { display: grid; gap: 20px; }
  .knowledge-page-heading { display: grid; gap: 4px; min-width: 0; }
  .knowledge-breadcrumb { display: flex; align-items: center; gap: 6px; color: #64748B; font-size: 13px; }
  .knowledge-breadcrumb strong { color: #0F766E; font-weight: 600; }
  .knowledge-page-heading h1 { margin: 0; color: #0F172A; font-size: 24px; line-height: 32px; font-weight: 700; letter-spacing: -0.02em; }
  .knowledge-page-heading p { margin: 0; max-width: 680px; color: #64748B; font-size: 13.5px; }
  .knowledge-summary {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
  }
  .knowledge-summary article {
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
  .knowledge-summary article:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
  .knowledge-stat-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    border: 1px solid #E2E8F0;
    background: #F1F5F9;
    color: #0F766E;
  }
  .knowledge-stat > div { display: grid; min-width: 0; gap: 2px; }
  .knowledge-stat > div > span { color: #64748B; font-size: 12.5px; font-weight: 500; }
  .knowledge-stat strong { color: #0F172A; font-size: 24px; line-height: 30px; font-weight: 700; font-variant-numeric: tabular-nums; }
  .knowledge-stat small { color: #94A3B8; font-size: 11.5px; }
  .stat-blue,
  .stat-teal,
  .stat-amber,
  .stat-violet {
    border-color: #E2E8F0 !important;
    background: #FFFFFF !important;
  }
  .stat-blue .knowledge-stat-icon { background: #EFF6FF; color: #2563EB; border-color: #DBEAFE; }
  .stat-teal .knowledge-stat-icon { background: #ECFDF5; color: #0F766E; border-color: #A7F3D0; }
  .stat-amber .knowledge-stat-icon { background: #FFFBEB; color: #D97706; border-color: #FDE68A; }
  .stat-violet .knowledge-stat-icon { background: #F5F3FF; color: #7C3AED; border-color: #DDD6FE; }
  .knowledge-card {
    min-width: 0;
    overflow: hidden;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    background: #FFFFFF;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04);
  }
  .knowledge-card-heading {
    display: flex;
    min-height: 64px;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    border-bottom: 1px solid #F1F5F9;
  }
  .knowledge-heading-icon {
    display: grid;
    width: 36px;
    height: 36px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    color: #0F766E;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
  }
  .knowledge-card-heading h2 { margin: 0; color: #0F172A; font-size: 15px; line-height: 22px; font-weight: 600; }
  .knowledge-card-heading p { margin: 2px 0 0; color: #64748B; font-size: 12.5px; }
  .knowledge-import,
  .knowledge-list { min-width: 0; padding: 0; }
  .knowledge-import { display: flex; flex-direction: column; gap: 16px; }
  .knowledge-import > :not(.knowledge-card-heading):not(.knowledge-form-footer) { margin-right: 20px; margin-left: 20px; }
  .knowledge-import > .knowledge-card-heading { margin: 0; }
  .knowledge-import .knowledge-dropzone {
    position: relative;
    display: flex;
    min-height: 150px;
    align-items: center;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
    padding: 24px 20px;
    border: 1.5px dashed #CBD5E1;
    border-radius: 10px;
    color: #334155;
    background: #F8FAFC;
    text-align: center;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-import .knowledge-dropzone:hover,
  .knowledge-import .knowledge-dropzone.is-dragging {
    border-color: #0F766E;
    background: #F0FDF4;
  }
  .knowledge-dropzone input { position: absolute; width: 1px; height: 1px; overflow: hidden; opacity: 0; }
  .knowledge-upload-icon {
    display: grid;
    width: 44px;
    height: 44px;
    place-items: center;
    border-radius: 10px;
    color: #0F766E;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  .knowledge-dropzone strong { font-size: 13.5px; font-weight: 600; color: #0F172A; }
  .knowledge-dropzone small { color: #64748B; font-size: 12px; }
  .knowledge-field { display: grid; min-width: 0; gap: 6px; color: #334155; font-size: 13px; font-weight: 600; }
  .knowledge-import .knowledge-field input,
  .knowledge-import .knowledge-field select {
    width: 100%;
    min-width: 0;
    height: 38px;
    box-sizing: border-box;
    padding: 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    color: #0F172A;
    background: #FFFFFF;
    font: inherit;
    font-size: 13.5px;
    transition: all 0.15s ease;
  }
  .knowledge-import .knowledge-field input:focus-visible,
  .knowledge-import .knowledge-field select:focus-visible {
    border-color: #0F766E;
    outline: none;
    box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
  }
  .knowledge-field-error { color: #DC2626 !important; font-size: 12px !important; font-weight: 400 !important; }
  .required-mark { color: #DC2626; }
  .knowledge-upload-list { display: grid; gap: 8px; max-height: 320px; overflow-y: auto; }
  .knowledge-upload-item {
    display: grid;
    grid-template-columns: 36px minmax(0, 1fr) minmax(180px, .85fr) 36px;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
  }
  .queued-file-icon {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    border-radius: 8px;
    color: #0F766E;
    background: #ECFDF5;
  }
  .queued-file-info { display: grid; min-width: 0; gap: 2px; }
  .queued-file-info strong { overflow-wrap: anywhere; color: #0F172A; font-size: 13px; font-weight: 600; }
  .queued-file-info small { color: #64748B; font-size: 11.5px; }
  .knowledge-import .queued-title { font-size: 13px; }
  .knowledge-import .queued-title input { height: 36px; font-size: 13px; }
  .knowledge-form-footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 4px;
    padding: 14px 20px;
    border-top: 1px solid #F1F5F9;
    background: #F8FAFC;
  }
  .knowledge-form-hint { display: inline-flex; align-items: center; gap: 8px; color: #64748B; font-size: 12px; }
  .knowledge-form-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-left: auto; }
  .knowledge-clear {
    display: inline-flex;
    height: 36px;
    align-items: center;
    padding: 0 14px;
    border: 1px solid #E2E8F0;
    border-radius: 7px;
    color: #334155;
    background: #FFFFFF;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-clear:hover { background: #F1F5F9; border-color: #CBD5E1; color: #0F172A; }
  .knowledge-submit {
    display: inline-flex;
    height: 36px;
    align-items: center;
    justify-content: center;
    gap: 7px;
    padding: 0 16px;
    border: 0;
    border-radius: 7px;
    color: #FFFFFF;
    background: #0F766E;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    cursor: pointer;
    transition: background 0.15s ease;
  }
  .knowledge-submit:hover:not(:disabled) { background: #115E59; }
  .knowledge-submit:disabled { cursor: not-allowed; opacity: .6; }
  .knowledge-notice {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 10px;
    margin: 0 20px;
    padding: 8px 12px;
    border-radius: 7px;
    color: #065F46;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
    font-size: 13px;
  }
  .knowledge-notice > span { flex: 1; min-width: 0; }
  .knowledge-notice-close {
    display: grid;
    width: 26px;
    height: 26px;
    flex: 0 0 auto;
    place-items: center;
    border: 0;
    border-radius: 6px;
    color: #065F46;
    background: transparent;
    cursor: pointer;
  }
  .knowledge-notice-close:hover { background: rgba(6, 95, 70, 0.1); }
  .knowledge-library-column { display: grid; flex: 1 1 360px; gap: 20px; min-width: 0; }
  .knowledge-library-column .knowledge-card-heading { justify-content: space-between; }
  .library-heading > div { min-width: 0; }
  .knowledge-total-badge {
    display: grid;
    min-width: 26px;
    height: 22px;
    padding: 0 7px;
    place-items: center;
    border-radius: 6px;
    color: #0F766E;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
    font-size: 11.5px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .knowledge-library-tools { display: grid; gap: 10px; padding: 14px 18px; border-bottom: 1px solid #F1F5F9; }
  .knowledge-search {
    display: flex;
    height: 36px;
    align-items: center;
    gap: 9px;
    padding: 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 7px;
    color: #64748B;
    background: #FFFFFF;
    transition: all 0.15s ease;
  }
  .knowledge-search:focus-within {
    border-color: #0F766E;
    box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
  }
  .knowledge-search input { width: 100%; min-width: 0; border: 0; outline: 0; color: #0F172A; background: transparent; font: inherit; font-size: 13px; }
  .knowledge-search input::placeholder { color: #94A3B8; }
  .knowledge-filter { display: flex; gap: 3px; padding: 3px; border-radius: 7px; background: #F1F5F9; }
  .knowledge-filter button {
    display: inline-flex;
    min-width: 0;
    height: 32px;
    flex: 1;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 0 8px;
    border: 0;
    border-radius: 6px;
    color: #64748B;
    background: transparent;
    font: inherit;
    font-size: 12.5px;
    font-weight: 500;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-filter button[aria-pressed='true'] { color: #0F172A; background: #FFFFFF; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
  .knowledge-filter button span { display: grid; min-width: 18px; height: 18px; padding: 0 5px; place-items: center; border-radius: 4px; color: #64748B; background: #E2E8F0; font-size: 10.5px; font-variant-numeric: tabular-nums; }
  .knowledge-filter button[aria-pressed='true'] span { color: #0F766E; background: #ECFDF5; }
  .knowledge-document-list { display: grid; gap: 8px; margin: 0; padding: 14px 18px 18px; list-style: none; }
  .knowledge-document {
    display: grid;
    grid-template-columns: 36px minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    transition: all 0.15s ease;
  }
  .knowledge-document:hover { border-color: #CBD5E1; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
  .knowledge-document:last-child { border-bottom: 1px solid #E2E8F0; }
  .knowledge-document-icon { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 8px; color: #0F766E; background: #ECFDF5; font-size: 11px; font-weight: 700; }
  .knowledge-document.is-inactive { background: #F8FAFC; }
  .knowledge-document.is-inactive .knowledge-document-icon { color: #94A3B8; background: #F1F5F9; }
  .knowledge-document-info { display: grid; min-width: 0; gap: 2px; }
  .knowledge-document-info > strong { min-width: 0; overflow-wrap: anywhere; color: #0F172A; font-size: 13.5px; line-height: 19px; font-weight: 600; }
  .knowledge-document.is-inactive .knowledge-document-info > strong { color: #94A3B8; }
  .knowledge-file-name { overflow-wrap: anywhere; color: #64748B; font-size: 12px; }
  .knowledge-document-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 3px; color: #64748B; font-size: 11.5px; }
  .knowledge-exam-tag { display: inline-flex; align-items: center; gap: 5px; color: #64748B; font-weight: 400; }
  .knowledge-skill-field { display: grid; gap: 6px; }
  .knowledge-skill-heading { display: flex; align-items: center; gap: 7px; color: #334155; font-size: 13px; font-weight: 600; }
  .knowledge-skill-heading small { color: #94A3B8; font-size: 12px; font-weight: 400; }
  .knowledge-skill-choice { display: flex; flex-wrap: wrap; gap: 6px; }
  .knowledge-skill-chip {
    display: inline-flex;
    height: 32px;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    color: #475569;
    background: #FFFFFF;
    font: inherit;
    font-size: 12.5px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .knowledge-skill-chip:hover { background: #F8FAFC; border-color: #CBD5E1; color: #0F172A; }
  .knowledge-skill-chip.selected { color: #0F766E; border-color: #0F766E; background: #ECFDF5; font-weight: 600; }
  .knowledge-skill-dot { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; background: #0F766E; }
  .knowledge-skill-tag { display: inline-flex; align-items: center; gap: 5px; padding: 2px 7px; border-radius: 5px; color: #0F766E; background: #ECFDF5; font-size: 11px; font-weight: 500; }
  .knowledge-document.is-inactive .knowledge-skill-tag { color: #94A3B8; background: #F1F5F9; }
  .knowledge-document-actions { display: flex; align-items: center; gap: 4px; }
  .knowledge-switch { position: relative; display: grid; width: 44px; height: 36px; flex: 0 0 auto; place-items: center; border: 0; border-radius: 999px; background: transparent; cursor: pointer; }
  .knowledge-switch::before { position: absolute; width: 36px; height: 20px; border-radius: 999px; background: #CBD5E1; content: ''; transition: background 0.15s ease; }
  .knowledge-switch[aria-checked='true']::before { background: #0F766E; }
  .knowledge-switch > span { position: absolute; top: 10px; left: 6px; width: 16px; height: 16px; border-radius: 50%; background: #FFFFFF; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2); transition: transform 0.15s ease; }
  .knowledge-switch[aria-checked='true'] > span { transform: translateX(16px); }
  .knowledge-switch-label { color: #64748B; font-size: 11.5px; font-weight: 500; white-space: nowrap; }
  .knowledge-icon-button { display: grid; width: 32px; height: 32px; flex: 0 0 auto; place-items: center; border: 1px solid #E2E8F0; border-radius: 6px; color: #64748B; background: #FFFFFF; cursor: pointer; transition: all 0.15s ease; }
  .knowledge-icon-button:hover { color: #0F766E; background: #F8FAFC; border-color: #CBD5E1; }
  .knowledge-icon-button.danger:hover { color: #DC2626; background: #FEF2F2; border-color: #FEE2E2; }
  .knowledge-empty {
    display: flex;
    min-height: 180px;
    align-items: center;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
    padding: 32px 20px;
    border: 1.5px dashed #E2E8F0;
    border-radius: 10px;
    color: #64748B;
    background: #F8FAFC;
    text-align: center;
  }
  .knowledge-empty > span {
    display: grid;
    width: 44px;
    height: 44px;
    margin-bottom: 4px;
    place-items: center;
    border-radius: 10px;
    color: #0F766E;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
  }
  .knowledge-empty strong { color: #0F172A; font-size: 14px; font-weight: 600; }
  .knowledge-empty small { max-width: 300px; color: #64748B; font-size: 12.5px; }
  .knowledge-empty-link { margin-top: 4px; color: #0F766E; font-size: 13px; font-weight: 500; text-underline-offset: 3px; }
  .compact-empty { min-height: 110px; }
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
  .knowledge-preview-list { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
  .knowledge-preview-chunk { display: grid; grid-template-columns: 36px minmax(0,1fr); align-items: start; gap: 12px; padding: 14px; border: 1px solid var(--border-item); border-radius: var(--radius-xl); background: var(--surface); }
  .knowledge-preview-chunk > span { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 9px; color: var(--blue-ink); background: var(--blue-soft); font-size: 13px; font-weight: 700; }
  .knowledge-preview-chunk p { min-width: 0; margin: 2px 0 0; color: var(--text-secondary); font-size: 14px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
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
    width: 36px;
    height: 36px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    background: var(--blue-soft);
    color: var(--blue-ink);
    font-size: 14px;
    font-style: normal;
    font-weight: 700;
  }
  .account-avatar.administrator {
    background: var(--violet-soft);
    color: var(--violet-ink);
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
    height: 24px;
    padding: 0 8px;
    border-radius: 6px;
    background: #ECFDF5;
    color: #065F46;
    border: 1px solid #A7F3D0;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;
  }
  .account-state.disabled {
    background: #F1F5F9;
    color: #475569;
    border-color: #E2E8F0;
  }
  .account-state.pending {
    background: #FFFBEB;
    color: #92400E;
    border-color: #FDE68A;
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
    color: #0F766E;
    background: #ECFDF5;
    border: 1px solid #A7F3D0;
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
    min-width: 120px;
    height: 32px;
    padding: 0 8px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 12.5px;
  }
  .table-action {
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
  }
  .table-action:hover {
    background: #F1F5F9;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .table-action.danger {
    color: #DC2626;
    border-color: #FEE2E2;
    background: #FEF2F2;
  }
  .table-action.danger:hover {
    background: #FEE2E2;
    border-color: #FCA5A5;
  }
  .account-actions-cell { display: flex; align-items: center; gap: 6px; }
  .learner-report { display: grid; gap: 20px; min-width: 0; }
  .learner-report-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 16px 20px;
  }
  .learner-report-toolbar h2,
  .learner-skill-panel h2 { margin: 0; color: #0F172A; font-size: 15px; line-height: 22px; font-weight: 600; }
  .learner-report-toolbar p,
  .learner-skill-panel > div:first-child p { margin: 2px 0 0; color: #64748B; font-size: 13px; }
  .learner-report-filters { display: flex; align-items: center; gap: 10px; }
  .learner-report-filters label { display: flex; align-items: center; gap: 6px; color: #475569; font-size: 13px; font-weight: 500; }
  .learner-report-filters select { height: 34px; max-width: 220px; padding: 0 28px 0 10px; border: 1px solid #E2E8F0; border-radius: 6px; background: #FFFFFF; color: #0F172A; font: inherit; font-size: 13px; }
  .learner-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
  .learner-summary-card {
    display: grid;
    align-content: start;
    gap: 6px;
    min-width: 0;
    min-height: 104px;
    padding: 16px 18px;
    border: 1px solid #E2E8F0 !important;
    border-radius: 10px;
    background: #FFFFFF !important;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  }
  .learner-summary-card > span { color: #64748B; font-size: 12.5px; font-weight: 500; }
  .learner-summary-card strong { color: #0F172A; font-size: 24px; line-height: 30px; font-weight: 700; font-variant-numeric: tabular-nums; }
  .learner-summary-card small { color: #94A3B8; font-size: 11.5px; line-height: 16px; }
  .learner-summary-card.blue,
  .learner-summary-card.teal,
  .learner-summary-card.violet,
  .learner-summary-card.amber { background: #FFFFFF; border-color: #E2E8F0; }
  .learner-history-panel { min-width: 0; padding: 0; overflow: hidden; }
  .learner-history-panel .admin-panel-heading { padding: 16px 20px; }
  .learner-history-table { min-width: 860px; }
  .learner-history-table td strong { display: block; color: #0F172A; font-size: 13px; font-weight: 600; }
  .table-secondary { display: block; margin-top: 2px; color: #64748B; font-size: 11.5px; }
  .attempt-state { display: inline-flex; align-items: center; height: 24px; padding: 0 8px; border-radius: 6px; background: #F1F5F9; color: #475569; font-size: 12px; font-weight: 500; }
  .attempt-state.submitted { background: #ECFDF5; color: #065F46; border: 1px solid #A7F3D0; }
  .attempt-state.graded { background: #ECFDF5; color: #065F46; border: 1px solid #A7F3D0; }
  .attempt-state.in_progress { background: #FFFBEB; color: #92400E; border: 1px solid #FDE68A; }
  .learner-skill-panel { display: grid; gap: 16px; padding: 18px 20px; }
  .learner-trend-panel { display: grid; gap: 16px; padding: 18px 20px; }
  .learner-trend-panel h2 { margin: 0; color: #0F172A; font-size: 15px; line-height: 22px; font-weight: 600; }
  .learner-trend-panel > div:first-child p { margin: 2px 0 0; color: #64748B; font-size: 13px; }
  .learner-trend-chart { display: flex; min-height: 160px; align-items: flex-end; gap: clamp(10px, 2vw, 24px); overflow-x: auto; padding: 12px 8px 0; border-bottom: 1px solid #E2E8F0; }
  .learner-trend-point { display: grid; min-width: 40px; height: 150px; flex: 1; grid-template-rows: 20px minmax(14px, 1fr) 24px; justify-items: center; align-items: end; gap: 4px; }
  .learner-trend-point strong { color: #64748B; font-size: 11px; font-variant-numeric: tabular-nums; }
  .learner-trend-point i { width: min(32px, 70%); min-height: 8px; align-self: end; border-radius: 4px 4px 2px 2px; background: #0F766E; }
  .learner-trend-point small { align-self: center; color: #94A3B8; font-size: 10.5px; }
  .skill-progress-list { display: grid; gap: 12px; }
  .skill-progress-row { display: grid; grid-template-columns: minmax(100px, 180px) minmax(60px, 1fr) 48px; align-items: center; gap: 14px; color: #475569; font-size: 13px; }
  .skill-progress-row strong { color: #0F172A; font-variant-numeric: tabular-nums; text-align: right; }
  .skill-progress-track { height: 8px; overflow: hidden; border-radius: 999px; background: #F1F5F9; }
  .skill-progress-track i { display: block; height: 100%; border-radius: inherit; background: #0F766E; }
  .learner-progress-empty { display: flex; align-items: center; gap: 10px; min-height: 60px; padding: 12px 14px; border: 1px dashed #E2E8F0; border-radius: 8px; background: #F8FAFC; color: #64748B; font-size: 13px; }
  .learner-progress-empty svg { flex: 0 0 auto; color: #0F766E; }
  .learner-report-error { padding: 10px 14px; border: 1px solid #FECACA; border-radius: 8px; background: #FEF2F2; color: #991B1B; font-size: 13px; }
  .attempt-result-detail { display: grid; gap: 16px; }
  .attempt-result-score { display: grid; gap: 4px; padding: 16px; border: 1px solid #A7F3D0; border-radius: 10px; background: #ECFDF5; }
  .attempt-result-score span { color: #065F46; font-size: 12.5px; font-weight: 500; }
  .attempt-result-score strong { color: #0F172A; font-size: 26px; line-height: 32px; font-variant-numeric: tabular-nums; font-weight: 700; }
  .attempt-result-score small { color: #0F766E; font-size: 12px; font-weight: 600; }
  .attempt-result-detail dl { display: grid; margin: 0; border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; }
  .attempt-result-detail dl > div { display: flex; min-height: 44px; align-items: center; justify-content: space-between; gap: 16px; padding: 6px 14px; border-bottom: 1px solid #F1F5F9; }
  .attempt-result-detail dl > div:last-child { border: 0; }
  .attempt-result-detail dt { color: #64748B; font-size: 13px; }
  .attempt-result-detail dd { margin: 0; color: #0F172A; font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; }
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
    .analytics-detail-grid { grid-template-columns: minmax(0, 1fr); gap: 30px; }
    .analytics-learners-section { max-width: none; }
  }
  @media (max-width: 800px) {
    .admin-page-heading { align-items: flex-start; flex-direction: column; }
    .analytics-controls { width: 100%; justify-content: flex-start; }
    .analytics-detail-grid { gap: 26px; }
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
    .analytics-controls { align-items: stretch; flex-direction: column; gap: 8px; }
    .analytics-time-filter { width: 100%; justify-content: stretch; }
    .analytics-time-filter button { min-width: 0; flex: 1; }
    .analytics-exam-filter select { width: 100%; max-width: none; }
    .analytics-observation { grid-template-columns: minmax(0, 1fr); gap: 4px; }
    .analytics-observation p { font-size: 13px; }
    .analytics-lead { grid-template-columns: minmax(0, 1fr); gap: 18px; padding-top: 22px; }
    .analytics-primary-metric > strong { font-size: 50px; }
    .analytics-supporting-metrics > div { padding: 10px 0; }
    .analytics-section-heading { flex-direction: column; gap: 8px; }
    .analytics-legend { padding: 0; }
    .analytics-chart-label.label-hidden-mobile { display: none; }
    .analytics-table { min-width: 330px; }
    .analytics-error { align-items: flex-start; flex-direction: column; }
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
  @media (max-width: 767px) {
    .admin-sidebar {
      z-index: 45;
      width: min(264px, 86vw);
      transform: translateX(-100%);
      transition: transform 0.2s ease;
    }
    .admin-sidebar.admin-sidebar-open {
      transform: translateX(0);
    }
    .admin-workspace {
      width: 100%;
      margin-left: 0;
    }
    .admin-menu-button {
      display: grid;
      flex: 0 0 auto;
      border: 1px solid #e1e7ec;
      background: #fff;
    }
    .admin-scrim {
      position: fixed;
      z-index: 40;
      inset: 0;
      display: block;
      background: rgba(19, 34, 48, 0.3);
    }
    .admin-topbar {
      gap: 9px;
      padding: 0 clamp(16px, 3vw, 40px);
    }
    .admin-search {
      width: auto;
      flex: 1;
    }
    .admin-top-actions {
      gap: 8px;
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
  @media (max-width: 640px) {
    .admin-profile div,
    .admin-profile > svg { display: none; }
    .admin-topbar { gap: 8px; padding-right: 16px; padding-left: 16px; }
    .admin-top-actions { gap: 2px; }
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
      align-items: flex-start;
    }
    .billing-search-box input {
      width: 100%;
    }
    .billing-tabs-nav {
      width: 100%;
    }
  }
</style>
