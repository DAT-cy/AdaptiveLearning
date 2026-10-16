<script setup>
  import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import {
    ArrowRight,
    Bell,
    BookOpen,
    Check,
    CheckCircle2,
    ClipboardCheck,
    Database,
    FileQuestion,
    GraduationCap,
    LayoutDashboard,
    Layers3,
    LogOut,
    Menu,
    Plus,
    Search,
    Shield,
    Trash2,
    Users,
    X,
  } from 'lucide-vue-next'
  import { useUserStore } from '@/stores/user'
  import { adminService } from '@/services/adminService'

  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const sectionIds = [
    'overview',
    'accounts',
    'programs',
    'exams',
    'templates',
    'questions',
    'knowledge',
  ]
  const initialSection = String(route.query.section || 'overview')
  const activeSection = ref(
    sectionIds.includes(initialSection) ? initialSection : 'overview'
  )
  const users = ref([])
  const programs = ref([])
  const exams = ref([])
  const examTemplates = ref([])
  const questions = ref([])
  const knowledgeDocuments = ref([])
  const knowledgeDetail = ref(null)
  const loadingKnowledgeDetail = ref(false)
  const topics = ref([])
  const query = ref('')
  const loading = ref(false)
  const pageError = ref('')
  const mobileMenuOpen = ref(false)
  const modal = ref('')
  const questionMode = ref('manual')
  const generating = ref(false)
  const generatedDraft = ref(null)
  const saving = ref(false)
  const editingCertificateId = ref(null)
  const editingQuestionId = ref(null)
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
  const templateForm = reactive({
    certificateId: '',
    topicId: '',
    title: '',
    description: '',
    durationMinutes: 60,
    targetScoreMin: 0,
    targetScoreMax: 0,
    passingScore: 0,
    sections: [
      {
        topicId: '',
        name: 'Part 1',
        questionType: 'MULTIPLE_CHOICE',
        questionCount: 10,
        pointsPerQuestion: 1,
      },
    ],
  })
  const questionForm = reactive({
    certificateId: '',
    topicId: '',
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    content: '',
    contentJson: '',
    optionsJson:
      '[{"label":"A","text":""},{"label":"B","text":""},{"label":"C","text":""},{"label":"D","text":""}]',
    correctAnswer: '"A"',
    explanation: '',
    points: 1,
    estimatedTimeSeconds: 60,
    tags: '',
  })
  const generationForm = reactive({
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    count: 5,
    instructions: '',
  })
  const knowledgeForm = reactive({
    title: '',
    fileName: '',
    certificateId: '',
    topicId: '',
    content: '',
  })

  const navigation = [
    { id: 'overview', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'accounts', label: 'Tài khoản', icon: Users },
    { id: 'programs', label: 'Kỳ thi & chứng chỉ', icon: GraduationCap },
    { id: 'exams', label: 'Bài kiểm tra', icon: ClipboardCheck },
    { id: 'templates', label: 'Mẫu đề', icon: Layers3 },
    { id: 'questions', label: 'Ngân hàng câu hỏi', icon: FileQuestion },
    { id: 'knowledge', label: 'Kho kiến thức', icon: Database },
  ]

  const currentTitle = computed(
    () =>
      navigation.find((item) => item.id === activeSection.value)?.label ||
      'Tổng quan'
  )
  const adminName = computed(() => userStore.state.username || 'Quản trị viên')
  const activeUsers = computed(
    () => users.value.filter((item) => item.enabled).length
  )
  const activeKnowledgeCount = computed(
    () => knowledgeDocuments.value.filter((document) => document.active).length
  )
  const inactiveKnowledgeCount = computed(
    () => knowledgeDocuments.value.length - activeKnowledgeCount.value
  )
  const knowledgeChunkCount = computed(() =>
    knowledgeDocuments.value.reduce(
      (total, document) => total + (document.chunkCount || 0),
      0
    )
  )
  const students = computed(() =>
    users.value.filter((item) => item.role === 'ROLE_USER')
  )
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
  const filteredExamTemplates = computed(() =>
    examTemplates.value.filter((template) =>
      `${template.title || ''} ${template.description || ''} ${programName(template.certificateId)}`
        .toLowerCase()
        .includes(query.value.toLowerCase())
    )
  )
  const selectedTemplateTopic = computed(() =>
    topics.value.find(
      (topic) => String(topic.topicId) === String(templateForm.sections[0]?.topicId)
    )
  )
  const templateMaxPoints = computed(() =>
    templateForm.sections.reduce(
      (total, section) => total + Number(section.questionCount || 0) * Number(section.pointsPerQuestion || 0),
      0
    )
  )
  const templateTopicHasScale = computed(() =>
    selectedTemplateTopic.value?.scoreMin != null &&
    selectedTemplateTopic.value?.scoreMax != null &&
    selectedTemplateTopic.value?.scoreStep > 0 &&
    Boolean(selectedTemplateTopic.value?.scoreUnit)
  )
  const templateBandOptions = computed(() => {
    const topic = selectedTemplateTopic.value
    if (!topic || topic.scoreUnit !== 'band' || topic.scoreStep <= 0) return []
    const count = Math.floor((topic.scoreMax - topic.scoreMin) / topic.scoreStep + 1e-8)
    return Array.from({ length: count + 1 }, (_, index) =>
      Number((topic.scoreMin + index * topic.scoreStep).toFixed(2))
    )
  })
  const allowedTemplateBandMins = templateBandOptions
  const allowedTemplateBandMaxes = templateBandOptions
  const templateScoreLabel = computed(() =>
    selectedTemplateTopic.value?.scoreUnit === 'band' ? 'Band' : 'Điểm mục tiêu'
  )
  const templateTopics = computed(() =>
    topics.value.filter(
      (topic) => String(topic.certificateId) === String(templateForm.certificateId)
    )
  )
  const filteredQuestions = computed(() =>
    questions.value.filter((item) =>
      `${item.content || item.questionText || ''} ${item.skill || item.topicName || ''}`
        .toLowerCase()
        .includes(query.value.toLowerCase())
    )
  )
  const generatedQuestions = computed(() => {
    try {
      const items = JSON.parse(generatedDraft.value?.resultJson || '[]')
      return Array.isArray(items) ? items : []
    } catch {
      return []
    }
  })

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
      adminService.questions(),
      adminService.knowledgeDocuments(),
      adminService.examTemplates(),
    ])
    if (results[0].status === 'fulfilled')
      users.value = asList(results[0].value)
    if (results[1].status === 'fulfilled')
      programs.value = asList(results[1].value)
    if (results[2].status === 'fulfilled')
      exams.value = asList(results[2].value)
    if (results[3].status === 'fulfilled')
      questions.value = asList(results[3].value)
    if (results[4].status === 'fulfilled')
      knowledgeDocuments.value = asList(results[4].value)
    if (results[5].status === 'fulfilled')
      examTemplates.value = asList(results[5].value)
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
  }

  function openSection(section) {
    activeSection.value = section
    query.value = ''
    mobileMenuOpen.value = false
    router.replace({
      path: '/admin',
      query: section === 'overview' ? {} : { section },
    })
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

  function openAccount() {
    Object.assign(accountForm, {
      username: '',
      password: '',
      role: 'ROLE_USER',
    })
    modal.value = 'account'
  }

  function openProgram(program = null) {
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
  }

  function openPreset(preset) {
    openProgram()
    Object.assign(programForm, preset)
  }

  function defaultTargetRange(topic) {
    if (!topic || topic.scoreMin == null || topic.scoreMax == null || topic.scoreStep <= 0) {
      return { min: 0, max: 0 }
    }
    const steps = Math.floor((topic.scoreMax - topic.scoreMin) / topic.scoreStep + 1e-8)
    return {
      min: Number((topic.scoreMin + Math.floor(steps * 0.5) * topic.scoreStep).toFixed(2)),
      max: Number((topic.scoreMin + Math.floor(steps * 0.75) * topic.scoreStep).toFixed(2)),
    }
  }

  function openTemplate() {
    const certificateId = programs.value[0]?.certificateId
    const topic = topics.value.find(
      (item) => String(item.certificateId) === String(certificateId)
    )
    const targetRange = defaultTargetRange(topic)
    Object.assign(templateForm, {
      certificateId: certificateId ? String(certificateId) : '',
      topicId: String(topic?.topicId || ''),
      title: '',
      description: '',
      durationMinutes: 60,
      targetScoreMin: targetRange.min,
      targetScoreMax: targetRange.max,
      passingScore: 5,
      sections: [
        {
          topicId: String(topic?.topicId || ''),
          name: 'Part 1',
          questionType: 'MULTIPLE_CHOICE',
          questionCount: 10,
          pointsPerQuestion: 1,
        },
      ],
    })
    modal.value = 'template'
  }

  function addTemplateSection() {
    templateForm.sections.push({
      topicId: templateForm.sections[0]?.topicId || '',
      name: `Part ${templateForm.sections.length + 1}`,
      questionType: 'MULTIPLE_CHOICE',
      questionCount: 10,
      pointsPerQuestion: 1,
    })
  }

  function removeTemplateSection(index) {
    if (templateForm.sections.length <= 1) return
    templateForm.sections.splice(index, 1)
    templateForm.sections.forEach((section, sectionIndex) => {
      if (/^Part \d+$/.test(section.name)) section.name = `Part ${sectionIndex + 1}`
    })
  }

  function topicName(topicId) {
    if (topicId == null) return '—'
    return topics.value.find((topic) => String(topic.topicId) === String(topicId))?.name || 'Kỹ năng'
  }

  function templateSkillsLabel(template) {
    const topicIds = [...new Set((template.sections || []).map((section) => String(section.topicId)))]
    return topicIds.map(topicName).join(', ')
  }

  function templateQuestionTotal(template) {
    return (template.sections || []).reduce(
      (total, section) => total + Number(section.questionCount || 0),
      0
    )
  }

  watch(
    () => templateForm.certificateId,
    (certificateId, previousCertificateId) => {
      if (previousCertificateId && String(certificateId) !== String(previousCertificateId)) {
        const firstTopicId = String(
          topics.value.find((topic) => String(topic.certificateId) === String(certificateId))?.topicId || ''
        )
        templateForm.topicId = firstTopicId
        templateForm.sections.forEach((section, index) => {
          if (index === 0 || !templateTopics.value.some((topic) => String(topic.topicId) === String(section.topicId))) {
            section.topicId = firstTopicId
          }
        })
        templateForm.passingScore = Math.floor(templateMaxPoints.value * 0.5 * 2) / 2
      }
    }
  )

  watch(
    () => templateForm.sections[0]?.topicId,
    (topicId) => {
      templateForm.topicId = topicId || ''
      const topic = topics.value.find((item) => String(item.topicId) === String(topicId))
      if (topic?.scoreMin == null || topic?.scoreMax == null || topic?.scoreStep <= 0) {
        templateForm.targetScoreMin = 0
        templateForm.targetScoreMax = 0
        return
      }
      const range = defaultTargetRange(topic)
      templateForm.targetScoreMin = range.min
      templateForm.targetScoreMax = range.max
    }
  )

  watch(
    () => templateMaxPoints.value,
    (maxPoints) => {
      if (templateForm.passingScore > maxPoints) templateForm.passingScore = maxPoints
    }
  )

  watch(
    () => templateForm.targetScoreMin,
    (minimum) => {
      if (minimum > templateForm.targetScoreMax) {
        templateForm.targetScoreMax = minimum
      }
    }
  )

  watch(
    () => templateForm.targetScoreMax,
    (maximum) => {
      if (maximum < templateForm.targetScoreMin) {
        templateForm.targetScoreMin = maximum
      }
    }
  )

  async function openQuestion(question = null) {
    editingQuestionId.value = question?.questionId ?? null
    questionForm.certificateId = programs.value[0]?.certificateId
      ? String(programs.value[0].certificateId)
      : ''
    questionForm.topicId = ''
    questionForm.content = ''
    questionForm.contentJson = ''
    questionForm.explanation = ''
    questionForm.tags = ''
    questionMode.value = 'manual'
    generatedDraft.value = null
    Object.assign(generationForm, {
      questionType: 'MULTIPLE_CHOICE',
      difficulty: 'MEDIUM',
      count: 5,
      instructions: '',
    })
    modal.value = 'question'
    if (question) {
      await nextTick()
      Object.assign(questionForm, {
        certificateId: String(question.certificateId),
        topicId: String(question.topicId),
        questionType: question.questionType || 'MULTIPLE_CHOICE',
        difficulty: question.difficulty || 'MEDIUM',
        content: question.content || '',
        contentJson: question.contentJson || '',
        optionsJson: question.optionsJson || '[]',
        correctAnswer: question.correctAnswer || '""',
        explanation: question.explanation || '',
        points: question.points || 1,
        estimatedTimeSeconds: question.estimatedTimeSeconds || 60,
        tags: question.tags || '',
      })
    }
  }

  const selectedTopics = computed(() =>
    topics.value.filter(
      (topic) =>
        String(topic.certificateId) === String(questionForm.certificateId)
    )
  )
  const knowledgeTopics = computed(() =>
    topics.value.filter(
      (topic) =>
        String(topic.certificateId) === String(knowledgeForm.certificateId)
    )
  )

  watch(
    () => questionForm.certificateId,
    (certificateId) => {
      questionForm.topicId = String(
        topics.value.find(
          (topic) => String(topic.certificateId) === String(certificateId)
        )?.topicId || ''
      )
    }
  )

  watch(
    () => knowledgeForm.certificateId,
    (certificateId) => {
      if (
        !knowledgeTopics.value.some(
          (topic) => String(topic.topicId) === String(knowledgeForm.topicId)
        )
      ) {
        knowledgeForm.topicId = String(
          topics.value.find(
            (topic) => String(topic.certificateId) === String(certificateId)
          )?.topicId || ''
        )
      }
    }
  )

  function programName(certificateId) {
    return (
      programs.value.find(
        (item) => String(item.certificateId) === String(certificateId)
      )?.name || 'Chưa gắn kỳ thi'
    )
  }

  function sourceChunkLabels(question) {
    try {
      const ids = JSON.parse(question.contentJson || '[]')
      return Array.isArray(ids) ? ids.join(', ') : ''
    } catch {
      return ''
    }
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
    pageError.value = ''
    try {
      await adminService.createUser({
        username: accountForm.username.trim(),
        password: accountForm.password,
        role: accountForm.role,
      })
      modal.value = ''
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể tạo tài khoản.'
    } finally {
      saving.value = false
    }
  }

  async function saveProgram() {
    saving.value = true
    pageError.value = ''
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
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể lưu chứng chỉ / kỳ thi.'
    } finally {
      saving.value = false
    }
  }

  async function saveTemplate() {
    saving.value = true
    pageError.value = ''
    try {
      await adminService.createExamTemplate({
        certificateId: Number(templateForm.certificateId),
        topicId: Number(templateForm.sections[0]?.topicId),
        title: templateForm.title.trim(),
        description: templateForm.description.trim(),
        durationMinutes: Number(templateForm.durationMinutes),
        targetScoreMin: Number(templateForm.targetScoreMin),
        targetScoreMax: Number(templateForm.targetScoreMax),
        passingScore: Number(templateForm.passingScore),
        sections: templateForm.sections.map((section) => ({
          topicId: Number(section.topicId),
          name: section.name.trim(),
          questionType: section.questionType,
          questionCount: Number(section.questionCount),
          pointsPerQuestion: Number(section.pointsPerQuestion),
        })),
      })
      modal.value = ''
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.message ||
        error?.response?.data?.responseMessage ||
        'Không thể lưu mẫu đề.'
    } finally {
      saving.value = false
    }
  }

  async function deactivateTemplate(template) {
    if (!window.confirm(`Ngừng sử dụng mẫu đề “${template.title}”?`)) return
    try {
      await adminService.deactivateExamTemplate(template.templateId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể ngừng mẫu đề.'
    }
  }

  function previewTemplate() {
    pageError.value = ''
    modal.value = 'template-preview'
  }

  function previewQuestionNumbers(count) {
    return Array.from({ length: Math.min(Number(count || 0), 12) }, (_, index) => index + 1)
  }

  async function saveQuestion() {
    saving.value = true
    pageError.value = ''
    try {
      const payload = {
        ...questionForm,
        certificateId: Number(questionForm.certificateId),
        topicId: Number(questionForm.topicId),
        points: Number(questionForm.points),
        estimatedTimeSeconds: Number(questionForm.estimatedTimeSeconds),
      }
      if (editingQuestionId.value) {
        await adminService.updateQuestion(editingQuestionId.value, payload)
      } else {
        await adminService.createQuestion(payload)
      }
      modal.value = ''
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể tạo câu hỏi. Hãy kiểm tra topic và JSON đáp án.'
    } finally {
      saving.value = false
    }
  }

  async function generateQuestionDrafts() {
    generating.value = true
    pageError.value = ''
    generatedDraft.value = null
    try {
      const result = await adminService.generateQuestions({
        certificateId: Number(questionForm.certificateId),
        topicId: Number(questionForm.topicId),
        ...generationForm,
        count: Number(generationForm.count),
        instructions: [
          `Kỳ thi: ${programName(questionForm.certificateId)}`,
          `Kỹ năng hoặc môn: ${selectedTopics.value.find((topic) => String(topic.topicId) === String(questionForm.topicId))?.name || ''}`,
          generationForm.instructions,
        ]
          .filter(Boolean)
          .join('\n'),
      })
      generatedDraft.value = result
      if (result?.status === 'FAILED') {
        pageError.value =
          result.errorMessage ||
          'AI chưa thể tạo câu hỏi. Kiểm tra cấu hình AI rồi thử lại.'
      }
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể kết nối AI để tạo câu hỏi.'
    } finally {
      generating.value = false
    }
  }

  async function applyGeneratedDrafts() {
    if (
      !generatedDraft.value?.generationId ||
      generatedDraft.value.status !== 'READY'
    )
      return
    saving.value = true
    pageError.value = ''
    try {
      await adminService.applyGeneratedQuestions(
        generatedDraft.value.generationId
      )
      modal.value = ''
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể lưu câu hỏi AI vào ngân hàng.'
    } finally {
      saving.value = false
    }
  }

  async function reviewQuestion(question) {
    try {
      await adminService.submitQuestionReview(question.questionId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể gửi câu hỏi đi duyệt.'
    }
  }

  async function approveQuestion(question) {
    try {
      await adminService.approveQuestion(question.questionId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể duyệt câu hỏi.'
    }
  }

  async function rejectQuestion(question) {
    try {
      await adminService.rejectQuestion(question.questionId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể trả câu hỏi về bản nháp.'
    }
  }

  async function readKnowledgeFile(event) {
    const file = event.target.files?.[0]
    if (!file) return
    const extension = file.name.split('.').pop()?.toLowerCase()
    if (!['txt', 'md'].includes(extension)) {
      pageError.value = 'Kho hiện chỉ nhận file .txt và .md.'
      event.target.value = ''
      return
    }
    if (file.size > 500000) {
      pageError.value = 'File vượt quá giới hạn 500 KB.'
      event.target.value = ''
      return
    }
    try {
      knowledgeForm.fileName = file.name
      knowledgeForm.title = file.name.replace(/\.(txt|md)$/i, '')
      knowledgeForm.content = await file.text()
      pageError.value = ''
    } catch {
      pageError.value = 'Không đọc được nội dung file này.'
    }
  }

  async function importKnowledge() {
    importingKnowledge.value = true
    pageError.value = ''
    try {
      await adminService.uploadKnowledgeDocument({
        ...knowledgeForm,
        certificateId: Number(knowledgeForm.certificateId),
        topicId: Number(knowledgeForm.topicId),
      })
      Object.assign(knowledgeForm, {
        title: '',
        fileName: '',
        content: '',
      })
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể thêm tài liệu vào kho kiến thức.'
    } finally {
      importingKnowledge.value = false
    }
  }

  async function removeKnowledge(document) {
    if (!window.confirm(`Xóa tài liệu “${document.title}” khỏi kho kiến thức?`))
      return
    try {
      await adminService.deleteKnowledgeDocument(document.documentId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể xóa tài liệu.'
    }
  }

  async function toggleKnowledgeActive(document) {
    try {
      await adminService.setKnowledgeDocumentActive(
        document.documentId,
        !document.active
      )
      if (knowledgeDetail.value?.documentId === document.documentId) {
        knowledgeDetail.value.active = !document.active
      }
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể cập nhật trạng thái tài liệu.'
    }
  }

  async function viewKnowledgeDocument(document) {
    loadingKnowledgeDetail.value = true
    try {
      knowledgeDetail.value = await adminService.knowledgeDocument(
        document.documentId
      )
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage ||
        'Không thể tải nội dung tài liệu.'
    } finally {
      loadingKnowledgeDetail.value = false
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

  async function deleteProgram(program) {
    if (!window.confirm(`Xóa chứng chỉ ${program.name}?`)) return
    try {
      await adminService.deleteCertificate(program.certificateId)
      await loadData()
    } catch (error) {
      pageError.value =
        error?.response?.data?.responseMessage || 'Không thể xóa chương trình.'
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
        <span><Shield :size="25" /></span>
        <strong>Adaptive Learning</strong>
      </a>
      <div class="admin-workspace-label">QUẢN TRỊ HỌC TẬP CÁ NHÂN</div>
      <nav class="admin-nav" aria-label="Điều hướng quản trị">
        <button
          v-for="item in navigation"
          :key="item.id"
          :class="['admin-nav-link', { active: activeSection === item.id }]"
          @click="openSection(item.id)"
        >
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="admin-sidebar-bottom">
        <span class="admin-status-dot"></span>
        <span>Hệ thống hoạt động</span>
        <small>Quyền quản trị</small>
      </div>
    </aside>

    <section class="admin-workspace">
      <header class="admin-topbar">
        <button
          class="admin-menu-button"
          :aria-label="mobileMenuOpen ? 'Đóng menu' : 'Mở menu'"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <X v-if="mobileMenuOpen" :size="19" />
          <Menu v-else :size="19" />
        </button>
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
          <button class="admin-notification" aria-label="Thông báo">
            <Bell :size="18" />
            <i></i>
          </button>
          <button class="admin-profile" @click="logout">
            <span>{{ adminName.slice(0, 1).toUpperCase() }}</span>
            <div>
              <strong>{{ adminName }}</strong>
              <small>Quản trị viên</small>
            </div>
            <LogOut :size="16" />
          </button>
        </div>
      </header>
      <div
        v-if="mobileMenuOpen"
        class="admin-scrim"
        @click="mobileMenuOpen = false"
      ></div>

      <div class="admin-content">
        <div v-if="pageError" class="admin-alert" role="alert">
          <span>{{ pageError }}</span>
          <button aria-label="Đóng thông báo" @click="pageError = ''">
            <X :size="16" />
          </button>
        </div>
        <div class="admin-page-heading">
          <div>
            <p class="admin-kicker">TRUNG TÂM QUẢN TRỊ</p>
            <h1>{{ currentTitle }}</h1>
            <p>
              Quản lý tài khoản và nội dung phục vụ lộ trình học tập cá nhân.
            </p>
          </div>
          <div class="heading-actions">
            <button
              v-if="
                activeSection === 'overview' || activeSection === 'accounts'
              "
              class="admin-primary-button"
              @click="openAccount"
            >
              <Users :size="16" />
              Tạo tài khoản
            </button>
            <button
              v-if="
                activeSection === 'overview' || activeSection === 'programs'
              "
              class="admin-secondary-button"
              @click="openProgram"
            >
              <GraduationCap :size="16" />
              Tạo chứng chỉ
            </button>
            <button
              v-if="activeSection === 'overview' || activeSection === 'exams'"
              class="admin-secondary-button"
              :disabled="!programs.length || !topics.length"
              @click="openTemplate"
            >
              <Layers3 :size="16" />
              Tạo bài kiểm tra
            </button>
            <button
              v-if="activeSection === 'templates'"
              class="admin-primary-button"
              :disabled="!programs.length || !topics.length"
              @click="openTemplate"
            >
              <Plus :size="16" />
              Tạo bài kiểm tra
            </button>
            <button
              v-if="
                activeSection === 'overview' || activeSection === 'questions'
              "
              class="admin-secondary-button"
              :disabled="!programs.length || !topics.length"
              @click="openQuestion"
            >
              <FileQuestion :size="16" />
              Tạo câu hỏi
            </button>
          </div>
        </div>

        <template v-if="activeSection === 'overview'">
          <section class="admin-stats">
            <article>
              <span class="admin-stat-icon teal"><Users :size="19" /></span>
              <div>
                <small>Tổng tài khoản</small>
                <strong>{{ users.length }}</strong>
              </div>
              <span class="stat-caption">Toàn hệ thống</span>
            </article>
            <article>
              <span class="admin-stat-icon coral">
                <GraduationCap :size="19" />
              </span>
              <div>
                <small>Người học</small>
                <strong>{{ students.length }}</strong>
              </div>
              <button class="stat-link" @click="openSection('accounts')">
                Quản lý
                <ArrowRight :size="13" />
              </button>
            </article>
            <article>
              <span class="admin-stat-icon violet">
                <BookOpen :size="19" />
              </span>
              <div>
                <small>Chứng chỉ / kỳ thi</small>
                <strong>{{ programs.length }}</strong>
              </div>
              <button class="stat-link" @click="openSection('programs')">
                Quản lý
                <ArrowRight :size="13" />
              </button>
            </article>
            <article>
              <span class="admin-stat-icon amber">
                <CheckCircle2 :size="19" />
              </span>
              <div>
                <small>Bài kiểm tra</small>
                <strong>{{ exams.length }}</strong>
              </div>
              <button class="stat-link" @click="openSection('exams')">
                Quản lý
                <ArrowRight :size="13" />
              </button>
            </article>
          </section>
          <div class="admin-overview-grid">
            <section class="admin-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Tài khoản gần đây</h2>
                  <p>{{ activeUsers }} tài khoản đang hoạt động</p>
                </div>
                <button
                  class="admin-text-button"
                  @click="openSection('accounts')"
                >
                  Xem tất cả
                  <ArrowRight :size="14" />
                </button>
              </div>
              <div class="admin-list">
                <article v-for="user in users.slice(0, 6)" :key="user.userId">
                  <span
                    class="account-avatar"
                    :class="user.role === 'ROLE_ADMIN' ? 'administrator' : ''"
                  >
                    {{ user.username.slice(0, 1).toUpperCase() }}
                  </span>
                  <div>
                    <strong>{{ user.username }}</strong>
                    <small>
                      {{
                        user.role === 'ROLE_ADMIN'
                          ? 'Quản trị viên'
                          : 'Người học'
                      }}
                    </small>
                  </div>
                  <span
                    :class="[
                      'account-state',
                      user.enabled ? 'enabled' : 'disabled',
                    ]"
                  >
                    {{ user.enabled ? 'Hoạt động' : 'Đã khóa' }}
                  </span>
                </article>
                <p v-if="!loading && !users.length" class="admin-empty">
                  Chưa có tài khoản.
                </p>
              </div>
            </section>
            <section class="admin-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Nội dung học tập</h2>
                  <p>Nội dung hỗ trợ AI cá nhân hóa</p>
                </div>
              </div>
              <button class="content-link" @click="openSection('programs')">
                <span class="admin-stat-icon teal">
                  <GraduationCap :size="18" />
                </span>
                <span>
                  <strong>Chứng chỉ / kỳ thi</strong>
                  <small>{{ programs.length }} danh mục</small>
                </span>
                <ArrowRight :size="16" />
              </button>
              <button class="content-link" @click="openSection('exams')">
                <span class="admin-stat-icon coral">
                  <ClipboardCheck :size="18" />
                </span>
                <span>
                  <strong>Bài kiểm tra</strong>
                  <small>{{ exams.length }} đề</small>
                </span>
                <ArrowRight :size="16" />
              </button>
              <button class="content-link" @click="openSection('questions')">
                <span class="admin-stat-icon violet">
                  <FileQuestion :size="18" />
                </span>
                <span>
                  <strong>Ngân hàng câu hỏi</strong>
                  <small>{{ questions.length }} câu hỏi</small>
                </span>
                <ArrowRight :size="16" />
              </button>
              <button
                class="content-link"
                :disabled="!programs.length || !topics.length"
                @click="openQuestion"
              >
                <span class="admin-stat-icon amber">
                  <FileQuestion :size="18" />
                </span>
                <span>
                  <strong>Biên soạn câu hỏi</strong>
                  <small>{{ topics.length }} kỹ năng / môn thi</small>
                </span>
                <ArrowRight :size="16" />
              </button>
            </section>
          </div>
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
          <div v-if="loading" class="admin-empty">Đang tải tài khoản...</div>
          <div v-else-if="!filteredUsers.length" class="admin-empty">
            Không có tài khoản phù hợp.
          </div>
          <div v-else class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Tài khoản</th>
                  <th>Loại tài khoản</th>
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
                    <span
                      :class="[
                        'account-state',
                        user.enabled ? 'enabled' : 'disabled',
                      ]"
                    >
                      {{ user.enabled ? 'Hoạt động' : 'Đã khóa' }}
                    </span>
                  </td>
                  <td>
                    <button class="table-action" @click="updateStatus(user)">
                      {{ user.enabled ? 'Khóa' : 'Mở khóa' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
            <div class="heading-actions">
              <button
                class="admin-secondary-button"
                :disabled="!programs.length || !topics.length"
                @click="openTemplate"
              >
                <Plus :size="16" />
                Tạo bài kiểm tra
              </button>
            </div>
          </div>
          <div v-if="loading" class="admin-empty">Đang tải bài kiểm tra...</div>
          <div v-else-if="!filteredExams.length" class="admin-empty">
            Chưa có bài kiểm tra.
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

        <section
          v-else-if="activeSection === 'templates'"
          class="admin-panel admin-table-panel"
        >
          <div class="admin-panel-heading">
            <div>
              <h2>Mẫu đề thi</h2>
              <p>{{ filteredExamTemplates.length }} mẫu · cấu trúc đề dùng lại</p>
            </div>
            <button
              class="admin-primary-button"
              :disabled="!programs.length || !topics.length"
              @click="openTemplate"
            >
              <Plus :size="16" /> Tạo bài kiểm tra
            </button>
          </div>
          <div v-if="loading" class="admin-empty">Đang tải mẫu đề...</div>
          <div v-else-if="!filteredExamTemplates.length" class="admin-empty">
            Chưa có mẫu đề. Tạo mẫu với các phần, dạng câu hỏi và số câu cần dùng.
          </div>
          <div v-else class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr><th>Tên mẫu</th><th>Kỳ thi / kỹ năng</th><th>Thời lượng</th><th>Cấu trúc</th><th></th></tr>
              </thead>
              <tbody>
                <tr v-for="template in filteredExamTemplates" :key="template.templateId">
                  <td><strong>{{ template.title }}</strong><small class="secondary-text">{{ template.description || 'Mẫu đề thi' }}</small></td>
                  <td>{{ programName(template.certificateId) }} · {{ templateSkillsLabel(template) }}</td>
                  <td>{{ template.durationMinutes }} phút</td>
                  <td>{{ template.targetScoreMin }}–{{ template.targetScoreMax }} {{ template.targetScoreUnit }} · {{ (template.sections || []).length }} phần · {{ templateQuestionTotal(template) }} câu</td>
                  <td>
                    <button class="table-action danger" @click="deactivateTemplate(template)">Ngừng dùng</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section
          v-else-if="activeSection === 'questions'"
          class="admin-panel admin-table-panel"
        >
          <div class="admin-panel-heading">
            <div>
              <h2>Ngân hàng câu hỏi</h2>
              <p>
                {{ filteredQuestions.length }} câu hỏi · {{ topics.length }} kỹ
                năng / môn thi
              </p>
            </div>
            <button
              class="admin-primary-button"
              :disabled="!programs.length || !topics.length"
              @click="openQuestion"
            >
              <FileQuestion :size="16" />
              Tạo câu hỏi
            </button>
          </div>
          <div v-if="loading" class="admin-empty">Đang tải câu hỏi...</div>
          <div v-else-if="!filteredQuestions.length" class="admin-empty">
            Chưa có câu hỏi.
          </div>
          <div v-else class="admin-question-list">
            <article
              v-for="(question, index) in filteredQuestions.slice(0, 100)"
              :key="question.questionId || index"
            >
              <span class="admin-stat-icon violet">
                <FileQuestion :size="17" />
              </span>
              <div>
                <strong>
                  {{
                    question.content ||
                    question.questionText ||
                    `Câu hỏi #${question.questionId}`
                  }}
                </strong>
                <small>
                  {{ question.skill || question.topicName || 'Chưa phân loại' }}
                  · {{ programName(question.certificateId) }} ·
                  {{ question.difficulty || 'Mức độ chưa xác định' }}
                </small>
                <small v-if="sourceChunkLabels(question)">
                  Nguồn kiến thức: đoạn {{ sourceChunkLabels(question) }}
                </small>
              </div>
              <span class="account-state enabled">
                {{ question.status || 'Có sẵn' }}
              </span>
              <button
                v-if="['DRAFT', 'IN_REVIEW'].includes(question.status)"
                class="table-action"
                @click="openQuestion(question)"
              >
                Sửa
              </button>
              <button
                v-if="question.status === 'DRAFT'"
                class="table-action"
                @click="reviewQuestion(question)"
              >
                Gửi duyệt
              </button>
              <button
                v-else-if="question.status === 'IN_REVIEW'"
                class="table-action"
                @click="approveQuestion(question)"
              >
                Duyệt
              </button>
              <button
                v-if="question.status === 'IN_REVIEW'"
                class="table-action"
                @click="rejectQuestion(question)"
              >
                Trả nháp
              </button>
            </article>
          </div>
        </section>
        <template v-else>
          <section class="knowledge-summary">
            <article>
              <span>Tổng tài liệu</span>
              <strong>{{ knowledgeDocuments.length }}</strong>
            </article>
            <article>
              <span>Đang sử dụng</span>
              <strong>{{ activeKnowledgeCount }}</strong>
            </article>
            <article>
              <span>Đã tắt</span>
              <strong>{{ inactiveKnowledgeCount }}</strong>
            </article>
            <article>
              <span>Tổng đoạn kiến thức</span>
              <strong>{{ knowledgeChunkCount }}</strong>
            </article>
          </section>
          <section class="knowledge-layout">
            <form
              class="admin-panel knowledge-import"
              @submit.prevent="importKnowledge"
            >
              <div class="admin-panel-heading">
                <div>
                  <h2>Thêm tài liệu vào kho</h2>
                  <p>Nội dung được dùng làm nguồn khi AI tạo câu luyện tập.</p>
                </div>
              </div>
              <label>
                Chọn file .txt hoặc .md
                <input
                  type="file"
                  accept=".txt,.md,text/plain,text/markdown"
                  @change="readKnowledgeFile"
                />
              </label>
              <div class="form-row">
                <label>
                  Tên tài liệu
                  <input
                    v-model.trim="knowledgeForm.title"
                    required
                    maxlength="200"
                    placeholder="Ví dụ: IELTS Reading - Main Idea"
                  />
                </label>
                <label>
                  Tên file nguồn
                  <input
                    v-model.trim="knowledgeForm.fileName"
                    required
                    maxlength="255"
                    placeholder="tai-lieu.md"
                  />
                </label>
              </div>
              <div class="form-row">
                <label>
                  Kỳ thi
                  <select v-model="knowledgeForm.certificateId" required>
                    <option disabled value="">Chọn kỳ thi</option>
                    <option
                      v-for="program in programs"
                      :key="program.certificateId"
                      :value="String(program.certificateId)"
                    >
                      {{ program.name }}
                    </option>
                  </select>
                </label>
                <label>
                  Kỹ năng / môn thi
                  <select v-model="knowledgeForm.topicId" required>
                    <option disabled value="">Chọn kỹ năng hoặc môn</option>
                    <option
                      v-for="topic in knowledgeTopics"
                      :key="topic.topicId"
                      :value="String(topic.topicId)"
                    >
                      {{ topic.name }}
                    </option>
                  </select>
                </label>
              </div>
              <label>
                Nội dung tài liệu
                <textarea
                  v-model.trim="knowledgeForm.content"
                  required
                  rows="12"
                  maxlength="500000"
                  placeholder="Chọn file .txt/.md hoặc dán nội dung kiến thức vào đây"
                ></textarea>
                <small>
                  {{ knowledgeForm.content.length.toLocaleString() }} / 500.000
                  ký tự
                </small>
              </label>
              <footer>
                <button
                  class="admin-primary-button"
                  :disabled="
                    importingKnowledge ||
                    !knowledgeTopics.length ||
                    !knowledgeForm.content.trim()
                  "
                >
                  <Database :size="16" />
                  {{
                    importingKnowledge
                      ? 'Đang nhập...'
                      : 'Đưa vào kho kiến thức'
                  }}
                </button>
              </footer>
            </form>
            <section class="admin-panel knowledge-list">
              <div class="admin-panel-heading">
                <div>
                  <h2>Tài liệu đã nhập</h2>
                  <p>{{ knowledgeDocuments.length }} tài liệu dùng chung</p>
                </div>
              </div>
              <div v-if="!knowledgeDocuments.length" class="admin-empty">
                Chưa có tài liệu kiến thức.
              </div>
              <article
                v-for="document in knowledgeDocuments"
                :key="document.documentId"
                class="knowledge-document"
              >
                <span class="admin-stat-icon blue">
                  <BookOpen :size="17" />
                </span>
                <div>
                  <strong>{{ document.title }}</strong>
                  <small>
                    {{ document.fileName }} ·
                    {{ programName(document.certificateId) }}
                  </small>
                  <small>
                    {{
                      topics.find(
                        (topic) =>
                          String(topic.topicId) === String(document.topicId)
                      )?.name || 'Chưa phân loại'
                    }}
                    · {{ document.chunkCount }} đoạn
                  </small>
                </div>
                <span
                  :class="[
                    'account-state',
                    document.active ? 'enabled' : 'disabled',
                  ]"
                >
                  {{ document.active ? 'Đang dùng' : 'Đã tắt' }}
                </span>
                <button
                  class="table-action"
                  @click="viewKnowledgeDocument(document)"
                >
                  Xem
                </button>
                <button
                  class="table-action"
                  @click="toggleKnowledgeActive(document)"
                >
                  {{ document.active ? 'Ngừng dùng' : 'Kích hoạt' }}
                </button>
                <button
                  class="table-action danger"
                  :aria-label="`Xóa ${document.title}`"
                  @click="removeKnowledge(document)"
                >
                  Xóa
                </button>
              </article>
            </section>
          </section>
        </template>
      </div>
    </section>

    <div v-if="modal" class="modal-backdrop" @click.self="modal = ''">
      <section class="admin-modal" role="dialog" aria-modal="true">
        <header>
          <div>
            <p class="admin-kicker">QUẢN TRỊ HỌC TẬP CÁ NHÂN</p>
            <h2>
              {{
                modal === 'account'
                  ? 'Tạo tài khoản'
                  : modal === 'program'
                    ? editingCertificateId
                      ? 'Cập nhật chứng chỉ / kỳ thi'
                      : 'Tạo chứng chỉ / kỳ thi'
                    : modal === 'template'
                      ? 'Tạo bài kiểm tra'
                      : modal === 'template-preview'
                        ? 'Xem trước bài kiểm tra'
                      : modal === 'question'
                      ? editingQuestionId
                        ? 'Chỉnh sửa câu hỏi'
                        : 'Biên soạn câu hỏi'
                      : 'Tạo đề kiểm tra'
              }}
            </h2>
          </div>
          <button class="modal-close" aria-label="Đóng" @click="modal = ''">
            <X :size="19" />
          </button>
        </header>
        <form v-if="modal === 'account'" @submit.prevent="saveAccount">
          <label>
            Tên đăng nhập
            <input
              v-model.trim="accountForm.username"
              required
              maxlength="50"
              autocomplete="username"
              placeholder="vd: hocvien01"
            />
          </label>
          <label>
            Mật khẩu
            <input
              v-model="accountForm.password"
              type="password"
              required
              minlength="8"
              autocomplete="new-password"
              placeholder="Tối thiểu 8 ký tự"
            />
          </label>
          <label>
            Vai trò
            <select v-model="accountForm.role">
              <option value="ROLE_USER">Người học</option>
              <option value="ROLE_ADMIN">Quản trị viên</option>
            </select>
          </label>
          <footer>
            <button
              type="button"
              class="admin-secondary-button"
              @click="modal = ''"
            >
              Hủy
            </button>
            <button class="admin-primary-button" :disabled="saving">
              <Check :size="16" />
              {{ saving ? 'Đang lưu...' : 'Tạo tài khoản' }}
            </button>
          </footer>
        </form>
        <form v-else-if="modal === 'program'" @submit.prevent="saveProgram">
          <label>
            Tên chứng chỉ / kỳ thi
            <input
              v-model.trim="programForm.name"
              required
              maxlength="120"
              placeholder="vd: IELTS Academic, TOEIC, THPT Quốc Gia"
            />
          </label>
          <label>
            Mã kỳ thi
            <input
              v-model.trim="programForm.code"
              required
              maxlength="50"
              placeholder="IELTS, TOEIC hoặc THPT"
            />
          </label>
          <label>
            Mô tả
            <textarea
              v-model.trim="programForm.description"
              rows="3"
              placeholder="Thang điểm, mục tiêu và kỹ năng/môn thi"
            ></textarea>
          </label>
          <div class="form-row">
            <label>
              Tổng điểm
              <input
                v-model.number="programForm.totalScore"
                type="number"
                min="1"
                required
              />
            </label>
            <label>
              Điểm đạt
              <input
                v-model.number="programForm.passingScore"
                type="number"
                min="0"
                required
              />
            </label>
            <label>
              Band / điểm mục tiêu
              <input
                v-model.number="programForm.passingBand"
                type="number"
                min="0"
                step="0.5"
              />
            </label>
          </div>
          <label>
            Kỹ năng / môn thi (JSON)
            <textarea
              v-model.trim="programForm.skills"
              rows="3"
              required
              placeholder='["LISTENING","READING"]'
            ></textarea>
          </label>
          <footer>
            <button
              type="button"
              class="admin-secondary-button"
              @click="modal = ''"
            >
              Hủy
            </button>
            <button class="admin-primary-button" :disabled="saving">
              <Check :size="16" />
              {{
                saving
                  ? 'Đang lưu...'
                  : editingCertificateId
                    ? 'Cập nhật catalog'
                    : 'Tạo catalog'
              }}
            </button>
          </footer>
        </form>
        <form v-else-if="modal === 'template'" @submit.prevent="previewTemplate">
          <label>Kỳ thi / chứng chỉ
            <select v-model="templateForm.certificateId" required>
              <option disabled value="">Chọn kỳ thi</option>
              <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }} ({{ program.code }})</option>
            </select>
          </label>
          <label>Tên mẫu đề
            <input v-model.trim="templateForm.title" required maxlength="200" placeholder="Ví dụ: IELTS Academic - Writing Practice" />
          </label>
          <label>Mô tả
            <textarea v-model.trim="templateForm.description" rows="2" placeholder="Mục tiêu hoặc ghi chú về đề"></textarea>
          </label>
          <label>Thời lượng (phút)
            <input v-model.number="templateForm.durationMinutes" type="number" min="1" max="600" required />
          </label>
          <fieldset class="template-sections">
            <legend>Cấu trúc đề</legend>
            <div v-for="(section, index) in templateForm.sections" :key="index" class="template-section-row">
              <label>Kỹ năng / môn<select v-model="section.topicId" required><option disabled value="">Chọn kỹ năng / môn</option><option v-for="topic in templateTopics" :key="topic.topicId" :value="String(topic.topicId)">{{ topic.name }}</option></select></label>
              <label>Tên phần<input v-model.trim="section.name" required maxlength="100" /></label>
              <label>Dạng câu hỏi<select v-model="section.questionType"><option value="MULTIPLE_CHOICE">Trắc nghiệm</option><option value="TRUE_FALSE">Đúng / sai</option><option value="ESSAY">Tự luận</option><option value="FILL_BLANK">Điền khuyết</option></select></label>
              <label>Số câu<input v-model.number="section.questionCount" type="number" min="1" max="200" required /></label>
              <label>Điểm / câu<input v-model.number="section.pointsPerQuestion" type="number" min="0.1" step="0.1" required /></label>
              <button type="button" class="icon-button" aria-label="Xóa phần" title="Xóa phần" :disabled="templateForm.sections.length <= 1" @click="removeTemplateSection(index)"><Trash2 :size="15" /></button>
            </div>
            <button type="button" class="admin-text-button" @click="addTemplateSection"><Plus :size="15" /> Thêm phần</button>
          </fieldset>
          <div v-if="selectedTemplateTopic" class="form-row">
            <label>
              {{ templateScoreLabel }} từ (theo phần đầu tiên)
              <select v-if="selectedTemplateTopic.scoreUnit === 'band'" v-model.number="templateForm.targetScoreMin" required>
                <option v-for="score in allowedTemplateBandMins" :key="score" :value="score">{{ score.toFixed(1) }}</option>
              </select>
              <input v-else v-model.number="templateForm.targetScoreMin" type="number" :min="selectedTemplateTopic.scoreMin" :max="selectedTemplateTopic.scoreMax" :step="selectedTemplateTopic.scoreStep" required />
            </label>
            <label>
              {{ templateScoreLabel }} đến
              <select v-if="selectedTemplateTopic.scoreUnit === 'band'" v-model.number="templateForm.targetScoreMax" required>
                <option v-for="score in allowedTemplateBandMaxes" :key="score" :value="score">{{ score.toFixed(1) }}</option>
              </select>
              <input v-else v-model.number="templateForm.targetScoreMax" type="number" :min="selectedTemplateTopic.scoreMin" :max="selectedTemplateTopic.scoreMax" :step="selectedTemplateTopic.scoreStep" required />
            </label>
          </div>
          <p v-if="selectedTemplateTopic" class="template-summary">
            Thang điểm {{ selectedTemplateTopic.scoreMin }}–{{ selectedTemplateTopic.scoreMax }} {{ selectedTemplateTopic.scoreUnit }}, bước {{ selectedTemplateTopic.scoreStep }}
          </p>
          <p v-if="selectedTemplateTopic && !templateTopicHasScale" class="availability-low">
            Kỹ năng / môn đầu tiên chưa được cấu hình thang điểm.
          </p>
          <label>Điểm đạt theo tổng điểm câu hỏi (tối đa {{ templateMaxPoints }})
            <input v-model.number="templateForm.passingScore" type="number" min="0" :max="templateMaxPoints" step="any" required />
          </label>
          <footer>
            <button type="button" class="admin-secondary-button" @click="modal = ''">Hủy</button>
            <button class="admin-primary-button" :disabled="!templateTopicHasScale || templateForm.passingScore > templateMaxPoints"><Check :size="16" />Xem trước</button>
          </footer>
        </form>
        <section v-else-if="modal === 'template-preview'" class="exam-form-preview">
          <div class="preview-heading">
            <p>{{ programName(templateForm.certificateId) }} · {{ templateForm.durationMinutes }} phút · {{ templateQuestionTotal({ sections: templateForm.sections }) }} câu</p>
            <h3>{{ templateForm.title }}</h3>
            <small v-if="selectedTemplateTopic">Mục tiêu {{ templateForm.targetScoreMin }}–{{ templateForm.targetScoreMax }} {{ selectedTemplateTopic.scoreUnit }} · Điểm đạt {{ templateForm.passingScore }} / {{ templateMaxPoints }}</small>
          </div>
          <article v-for="(section, sectionIndex) in templateForm.sections" :key="sectionIndex" class="preview-section">
            <header><div><strong>{{ section.name }}</strong><small>{{ topicName(section.topicId) }} · {{ section.questionType }}</small></div><span>{{ section.questionCount }} câu</span></header>
            <div v-for="questionNumber in previewQuestionNumbers(section.questionCount)" :key="questionNumber" class="preview-question"><span>{{ questionNumber }}</span><div></div></div>
            <small v-if="section.questionCount > 12" class="template-summary">Còn {{ section.questionCount - 12 }} câu trống</small>
          </article>
          <footer>
            <button type="button" class="admin-secondary-button" @click="modal = 'template'">Quay lại chỉnh sửa</button>
            <button type="button" class="admin-primary-button" :disabled="saving" @click="saveTemplate"><Check :size="16" />{{ saving ? 'Đang lưu...' : 'Lưu cấu hình bài kiểm tra' }}</button>
          </footer>
        </section>
        <form v-else-if="false" @submit.prevent="saveExam">
          <label>Mẫu đề
            <select v-model="examForm.templateId" required>
              <option disabled value="">Chọn mẫu đề</option>
              <option v-for="template in examTemplates" :key="template.templateId" :value="String(template.templateId)">{{ template.title }} · {{ programName(template.certificateId) }} / {{ templateSkillsLabel(template) }}</option>
            </select>
          </label>
          <p v-if="selectedExamTemplate" class="template-summary">{{ programName(selectedExamTemplate.certificateId) }} · {{ templateSkillsLabel(selectedExamTemplate) }} · {{ selectedExamTemplate.durationMinutes }} phút · {{ templateQuestionTotal(selectedExamTemplate) }} câu</p>
          <label>
            Tên đề thi
            <input
              v-model.trim="examForm.title"
              required
              maxlength="200"
              placeholder="Tên lần thi này"
            />
          </label>
          <p v-if="selectedExamTemplate" class="template-summary">
            Mục tiêu: {{ selectedExamTemplate.targetScoreMin }}–{{ selectedExamTemplate.targetScoreMax }} {{ selectedExamTemplate.targetScoreUnit }}
          </p>
          <fieldset v-if="selectedExamTemplate" class="template-sections">
            <legend>Chọn độ khó cho từng phần</legend>
            <div v-for="(section, index) in selectedTemplateSections" :key="index" class="exam-section-row">
              <div><strong>{{ section.name }}</strong><small>{{ section.questionCount }} câu · {{ section.questionType }}</small></div>
              <label>Độ khó<select v-model="examForm.sectionDifficulties[index]" required><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label>
              <small :class="sectionAvailableCount(index) < section.questionCount ? 'availability-low' : 'availability-ok'">Có {{ sectionAvailableCount(index) }} / {{ section.questionCount }} câu</small>
            </div>
          </fieldset>
          <p v-else class="picker-empty">Chọn mẫu bài kiểm tra để cấu hình độ khó từng phần.</p>
          <footer>
            <button
              type="button"
              class="admin-secondary-button"
              @click="modal = ''"
            >
              Hủy
            </button>
            <button
              class="admin-primary-button"
              :disabled="saving || !selectedExamTemplate || selectedTemplateSections.some((section, index) => sectionAvailableCount(index) < section.questionCount)"
            >
              <Check :size="16" />
              {{
                saving
                  ? 'Đang lưu...'
                  : 'Sinh đề từ ngân hàng câu hỏi'
              }}
            </button>
          </footer>
        </form>
        <template v-else-if="modal === 'question'">
          <div class="question-mode" role="group" aria-label="Cách biên soạn">
            <button
              type="button"
              :class="{ active: questionMode === 'manual' }"
              @click="questionMode = 'manual'"
            >
              Tự biên soạn
            </button>
            <button
              type="button"
              :class="{ active: questionMode === 'ai' }"
              @click="questionMode = 'ai'"
            >
              AI tạo bản nháp
            </button>
          </div>
          <form v-if="questionMode === 'manual'" @submit.prevent="saveQuestion">
            <label>
              Kỳ thi
              <select v-model="questionForm.certificateId" required>
                <option disabled value="">Chọn kỳ thi</option>
                <option
                  v-for="program in programs"
                  :key="program.certificateId"
                  :value="String(program.certificateId)"
                >
                  {{ program.name }} ({{ program.code }})
                </option>
              </select>
            </label>
            <label>
              Kỹ năng / môn thi
              <select v-model="questionForm.topicId" required>
                <option disabled value="">Chọn kỹ năng hoặc môn thi</option>
                <option
                  v-for="topic in selectedTopics"
                  :key="topic.topicId"
                  :value="String(topic.topicId)"
                >
                  {{ topic.name }}
                </option>
              </select>
            </label>
            <div class="form-row">
              <label>
                Dạng câu hỏi
                <select v-model="questionForm.questionType">
                  <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
                  <option value="TRUE_FALSE">Đúng / sai</option>
                  <option value="ESSAY">Tự luận</option>
                </select>
              </label>
              <label>
                Độ khó
                <select v-model="questionForm.difficulty">
                  <option value="EASY">Cơ bản</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="HARD">Nâng cao</option>
                </select>
              </label>
            </div>
            <label>
              Nội dung câu hỏi
              <textarea
                v-model.trim="questionForm.content"
                rows="4"
                required
                placeholder="Nhập nội dung câu hỏi"
              ></textarea>
            </label>
            <label v-if="questionForm.questionType === 'MULTIPLE_CHOICE'">
              Các phương án (JSON)
              <textarea
                v-model.trim="questionForm.optionsJson"
                rows="4"
                required
              ></textarea>
              <small class="form-hint">Mỗi phương án gồm label và text.</small>
            </label>
            <div class="form-row">
              <label>
                Đáp án đúng (JSON)
                <input
                  v-model.trim="questionForm.correctAnswer"
                  required
                  placeholder='"A"'
                />
              </label>
              <label>
                Điểm
                <input
                  v-model.number="questionForm.points"
                  type="number"
                  min="0.1"
                  step="0.1"
                  required
                />
              </label>
            </div>
            <label>
              Giải thích đáp án
              <textarea
                v-model.trim="questionForm.explanation"
                rows="2"
                placeholder="Giải thích để AI có thể hướng dẫn người học"
              ></textarea>
            </label>
            <footer>
              <button
                type="button"
                class="admin-secondary-button"
                @click="modal = ''"
              >
                Hủy
              </button>
              <button
                class="admin-primary-button"
                :disabled="saving || !selectedTopics.length"
              >
                <Check :size="16" />
                {{
                  saving
                    ? 'Đang lưu...'
                    : editingQuestionId
                      ? 'Lưu thay đổi'
                      : 'Lưu câu hỏi nháp'
                }}
              </button>
            </footer>
          </form>
          <form v-else @submit.prevent="generateQuestionDrafts">
            <label>
              Kỳ thi
              <select v-model="questionForm.certificateId" required>
                <option disabled value="">Chọn kỳ thi</option>
                <option
                  v-for="program in programs"
                  :key="program.certificateId"
                  :value="String(program.certificateId)"
                >
                  {{ program.name }} ({{ program.code }})
                </option>
              </select>
            </label>
            <label>
              Kỹ năng / môn thi
              <select v-model="questionForm.topicId" required>
                <option disabled value="">Chọn kỹ năng hoặc môn thi</option>
                <option
                  v-for="topic in selectedTopics"
                  :key="topic.topicId"
                  :value="String(topic.topicId)"
                >
                  {{ topic.name }}
                </option>
              </select>
            </label>
            <div class="form-row">
              <label>
                Dạng câu hỏi
                <select v-model="generationForm.questionType">
                  <option value="MULTIPLE_CHOICE">Trắc nghiệm</option>
                  <option value="TRUE_FALSE">Đúng / sai</option>
                  <option value="ESSAY">Tự luận</option>
                </select>
              </label>
              <label>
                Độ khó
                <select v-model="generationForm.difficulty">
                  <option value="EASY">Cơ bản</option>
                  <option value="MEDIUM">Trung bình</option>
                  <option value="HARD">Nâng cao</option>
                </select>
              </label>
              <label>
                Số câu
                <input
                  v-model.number="generationForm.count"
                  type="number"
                  min="1"
                  max="20"
                  required
                />
              </label>
            </div>
            <label>
              Yêu cầu nội dung
              <textarea
                v-model.trim="generationForm.instructions"
                rows="3"
                placeholder="Chủ đề, mục tiêu kiến thức hoặc lưu ý cần có"
              ></textarea>
            </label>
            <section
              v-if="generatedDraft"
              class="generated-preview"
              aria-live="polite"
            >
              <strong>
                {{
                  generatedDraft.status === 'READY'
                    ? `AI đã tạo ${generatedQuestions.length} câu nháp`
                    : `Trạng thái: ${generatedDraft.status}`
                }}
              </strong>
              <p v-if="generatedDraft.status === 'FAILED'">
                {{ generatedDraft.errorMessage || 'AI chưa tạo được câu hỏi.' }}
              </p>
              <article v-for="(item, index) in generatedQuestions" :key="index">
                <b>{{ index + 1 }}. {{ item.content }}</b>
                <small v-for="option in item.options" :key="option.label">
                  {{ option.label }}. {{ option.text }}
                </small>
                <small>
                  Đáp án: {{ item.correctAnswer }} · {{ item.explanation }}
                </small>
                <small>
                  Nguồn đoạn kiến thức:
                  {{ item.sourceChunkIds?.join(', ') || 'Không có' }}
                </small>
              </article>
            </section>
            <footer>
              <button
                type="button"
                class="admin-secondary-button"
                @click="modal = ''"
              >
                Hủy
              </button>
              <button
                v-if="generatedDraft?.status === 'READY'"
                type="button"
                class="admin-primary-button"
                :disabled="saving"
                @click="applyGeneratedDrafts"
              >
                <Check :size="16" />
                {{ saving ? 'Đang lưu...' : 'Đưa bản nháp vào ngân hàng' }}
              </button>
              <button
                v-else
                class="admin-primary-button"
                :disabled="generating || !selectedTopics.length"
              >
                <Check :size="16" />
                {{ generating ? 'AI đang tạo...' : 'Tạo câu hỏi bằng AI' }}
              </button>
            </footer>
          </form>
        </template>
      </section>
    </div>
    <div
      v-if="knowledgeDetail || loadingKnowledgeDetail"
      class="modal-backdrop"
      @click.self="knowledgeDetail = null"
    >
      <section
        class="admin-modal knowledge-detail-modal"
        role="dialog"
        aria-modal="true"
      >
        <header>
          <div>
            <p class="admin-kicker">TỔNG HỢP TÀI LIỆU</p>
            <h2>{{ knowledgeDetail?.title || 'Đang tải tài liệu...' }}</h2>
            <small v-if="knowledgeDetail">
              {{ knowledgeDetail.fileName }} ·
              {{ knowledgeDetail.chunkCount }} đoạn ·
              {{
                knowledgeDetail.active ? 'Đang dùng cho AI' : 'Đã tắt với AI'
              }}
            </small>
          </div>
          <button
            class="modal-close"
            aria-label="Đóng nội dung tài liệu"
            @click="knowledgeDetail = null"
          >
            <X :size="19" />
          </button>
        </header>
        <div v-if="loadingKnowledgeDetail" class="admin-empty">
          Đang tải nội dung tài liệu...
        </div>
        <pre v-else class="knowledge-detail-content">{{
          knowledgeDetail?.content
        }}</pre>
      </section>
    </div>
  </main>
</template>

<style scoped>
  .admin-shell {
    min-height: 100vh;
    color: #1a2746;
    background: #f5f7fa;
    font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  }
  .admin-sidebar {
    position: fixed;
    z-index: 30;
    inset: 0 auto 0 0;
    display: flex;
    width: 224px;
    flex-direction: column;
    padding: 18px 13px 15px;
    border-right: 1px solid #e4e9f0;
    background: #182a3a;
    color: #e8eef4;
  }
  .admin-brand {
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 0 8px 20px;
    color: #fff;
    text-decoration: none;
  }
  .admin-brand span {
    display: grid;
    color: #75d0bf;
  }
  .admin-brand strong {
    font-size: 13px;
  }
  .admin-workspace-label {
    padding: 11px 10px 8px;
    color: #8fa4b4;
    font-size: 8px;
    font-weight: 700;
  }
  .admin-nav {
    display: grid;
    gap: 4px;
  }
  .admin-nav-link {
    display: flex;
    height: 39px;
    align-items: center;
    gap: 10px;
    padding: 0 10px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #bbc8d2;
    font: inherit;
    font-size: 10px;
    text-align: left;
    cursor: pointer;
  }
  .admin-nav-link svg {
    color: #9fb2bf;
  }
  .admin-nav-link:hover,
  .admin-nav-link.active {
    background: #294357;
    color: #fff;
  }
  .admin-nav-link.active {
    box-shadow: inset 3px 0 #56bca8;
  }
  .admin-nav-link.active svg {
    color: #74d2bf;
  }
  .admin-sidebar-bottom {
    display: grid;
    grid-template-columns: 8px 1fr;
    align-items: center;
    gap: 8px;
    margin-top: auto;
    padding: 12px 9px;
    border-top: 1px solid #304452;
    color: #c0ced7;
    font-size: 9px;
  }
  .admin-status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #48c296;
  }
  .admin-sidebar-bottom small {
    grid-column: 2;
    color: #8fa3b1;
    font-size: 8px;
  }
  .admin-workspace {
    min-width: 0;
    margin-left: 224px;
  }
  .admin-topbar {
    position: sticky;
    z-index: 20;
    top: 0;
    display: flex;
    height: 59px;
    align-items: center;
    gap: 14px;
    padding: 0 22px;
    border-bottom: 1px solid #e4eaf0;
    background: #fff;
  }
  .admin-search {
    display: flex;
    width: min(440px, 55%);
    height: 35px;
    align-items: center;
    gap: 8px;
    padding: 0 11px;
    border: 1px solid #dce4eb;
    border-radius: 6px;
    color: #8293a2;
  }
  .admin-search input {
    width: 100%;
    border: 0;
    outline: 0;
    color: #263b50;
    font: inherit;
    font-size: 10px;
  }
  .admin-search input::placeholder {
    color: #9aa8b4;
  }
  .admin-top-actions {
    display: flex;
    align-items: center;
    gap: 18px;
    margin-left: auto;
  }
  .admin-notification,
  .admin-menu-button {
    position: relative;
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #73889a;
    cursor: pointer;
  }
  .admin-notification i {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #e78058;
  }
  .admin-profile {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0;
    border: 0;
    background: none;
    color: #2a3a4a;
    text-align: left;
    cursor: pointer;
  }
  .admin-profile > span {
    display: grid;
    width: 31px;
    height: 31px;
    place-items: center;
    border-radius: 50%;
    background: #e6f3ef;
    color: #238b78;
    font-size: 11px;
    font-weight: 700;
  }
  .admin-profile div {
    display: grid;
    gap: 2px;
  }
  .admin-profile strong {
    font-size: 9px;
  }
  .admin-profile small {
    color: #8191a0;
    font-size: 8px;
  }
  .admin-profile > svg {
    margin-left: 5px;
    color: #758796;
  }
  .admin-menu-button {
    display: none;
  }
  .admin-content {
    width: min(100%, 1500px);
    margin: 0 auto;
    padding: 22px 24px 32px;
  }
  .admin-alert {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
    padding: 10px 12px;
    border: 1px solid #efd8cf;
    border-radius: 6px;
    background: #fff8f4;
    color: #a25337;
    font-size: 10px;
  }
  .admin-alert button {
    display: grid;
    place-items: center;
    border: 0;
    background: transparent;
    color: inherit;
    cursor: pointer;
  }
  .admin-page-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
  }
  .admin-kicker {
    margin: 0 0 6px;
    color: #3d9584;
    font-size: 8px;
    font-weight: 750;
  }
  .admin-page-heading h1 {
    margin: 0;
    color: #1c2d40;
    font-size: 24px;
  }
  .admin-page-heading > div > p:last-child {
    margin: 6px 0 0;
    color: #7c8b99;
    font-size: 10px;
  }
  .heading-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .admin-primary-button,
  .admin-secondary-button {
    display: inline-flex;
    min-height: 34px;
    align-items: center;
    justify-content: center;
    gap: 7px;
    padding: 0 11px;
    border: 1px solid transparent;
    border-radius: 6px;
    font: inherit;
    font-size: 10px;
    font-weight: 600;
    cursor: pointer;
  }
  .admin-primary-button {
    background: #178b7b;
    color: white;
  }
  .admin-primary-button:hover {
    background: #117467;
  }
  .admin-primary-button:disabled {
    opacity: 0.55;
    cursor: wait;
  }
  .admin-secondary-button {
    border-color: #dce4eb;
    background: #fff;
    color: #536779;
  }
  .admin-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 11px;
    margin-bottom: 14px;
  }
  .admin-stats article {
    position: relative;
    display: grid;
    min-height: 98px;
    grid-template-columns: 38px 1fr;
    align-content: start;
    column-gap: 10px;
    padding: 13px;
    border: 1px solid #e3e9ef;
    border-radius: 7px;
    background: #fff;
  }
  .admin-stat-icon {
    display: grid;
    width: 36px;
    height: 36px;
    grid-row: span 2;
    place-items: center;
    border-radius: 8px;
    background: #e6f4f1;
    color: #208d7d;
  }
  .admin-stat-icon.coral {
    background: #fff0e8;
    color: #d7794d;
  }
  .admin-stat-icon.violet {
    background: #efedfa;
    color: #7668c6;
  }
  .admin-stat-icon.amber {
    background: #f7f2e2;
    color: #b18433;
  }
  .admin-stats article > div {
    display: grid;
    gap: 5px;
  }
  .admin-stats small {
    color: #718292;
    font-size: 9px;
  }
  .admin-stats strong {
    color: #22384b;
    font-size: 21px;
    line-height: 1;
  }
  .stat-caption,
  .stat-link {
    position: absolute;
    right: 12px;
    bottom: 12px;
    color: #8795a1;
    font-size: 8px;
  }
  .stat-link {
    display: flex;
    align-items: center;
    gap: 4px;
    border: 0;
    background: none;
    color: #278b7c;
    cursor: pointer;
  }
  .admin-overview-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 13px;
  }
  .admin-panel {
    min-width: 0;
    padding: 15px;
    border: 1px solid #e3e9ef;
    border-radius: 7px;
    background: #fff;
  }
  .knowledge-layout {
    display: grid;
    grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.95fr);
    align-items: start;
    gap: 14px;
  }
  .knowledge-summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 13px;
  }
  .knowledge-summary article {
    display: grid;
    min-height: 68px;
    align-content: center;
    gap: 7px;
    padding: 10px 12px;
    border: 1px solid #e3e9ef;
    border-radius: 6px;
    background: #fff;
  }
  .knowledge-summary span {
    color: #7b8d9a;
    font-size: 9px;
  }
  .knowledge-summary strong {
    color: #263a4e;
    font-size: 18px;
    line-height: 1;
  }
  .knowledge-import,
  .knowledge-list {
    min-width: 0;
    padding: 16px;
  }
  .knowledge-import {
    display: grid;
    gap: 12px;
  }
  .knowledge-import label {
    display: grid;
    gap: 6px;
    color: #556a7b;
    font-size: 9px;
    font-weight: 600;
  }
  .knowledge-import input,
  .knowledge-import select,
  .knowledge-import textarea {
    width: 100%;
    min-height: 36px;
    box-sizing: border-box;
    padding: 8px 9px;
    border: 1px solid #dce4eb;
    border-radius: 5px;
    background: #fff;
    color: #33495b;
    font: inherit;
    font-size: 10px;
  }
  .knowledge-import input[type='file'] {
    padding: 6px;
    background: #f8fafb;
  }
  .knowledge-import textarea {
    min-height: 190px;
    resize: vertical;
    line-height: 1.6;
  }
  .knowledge-import small {
    color: #83919d;
    font-size: 8px;
  }
  .knowledge-import footer {
    display: flex;
    justify-content: flex-end;
  }
  .knowledge-document {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 10px;
    padding: 11px 2px;
    border-top: 1px solid #edf1f4;
  }
  .knowledge-document > div {
    display: grid;
    min-width: 0;
    flex: 1;
    gap: 4px;
  }
  .knowledge-document strong {
    overflow-wrap: anywhere;
    color: #35495a;
    font-size: 10px;
  }
  .knowledge-document small {
    color: #8896a2;
    font-size: 8px;
  }
  .knowledge-detail-modal {
    width: min(100%, 760px);
  }
  .knowledge-detail-modal header small {
    display: block;
    margin-top: 6px;
    color: #82909d;
    font-size: 9px;
  }
  .knowledge-detail-content {
    max-height: min(62vh, 620px);
    overflow: auto;
    margin: 0;
    padding: 14px;
    border: 1px solid #e3e9ef;
    border-radius: 5px;
    background: #f8fafb;
    color: #40566a;
    font: inherit;
    font-size: 10px;
    line-height: 1.7;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
  .admin-panel-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }
  .admin-panel-heading h2 {
    margin: 0;
    color: #263a4e;
    font-size: 12px;
  }
  .admin-panel-heading p {
    margin: 4px 0 0;
    color: #82909d;
    font-size: 8px;
  }
  .template-sections {
    display: grid;
    gap: 9px;
    min-width: 0;
    margin: 4px 0;
    padding: 10px;
    border: 1px solid #e5ebef;
    border-radius: 5px;
  }
  .template-sections legend {
    padding: 0 5px;
    color: #526878;
    font-size: 9px;
    font-weight: 700;
  }
  .template-section-row {
    display: grid;
    grid-template-columns: minmax(120px, 1fr) minmax(90px, .8fr) minmax(120px, 1.2fr) 70px 80px 28px;
    align-items: end;
    gap: 7px;
    padding-bottom: 8px;
    border-bottom: 1px solid #edf1f4;
  }
  .template-section-row label,
  .exam-section-row label { min-width: 0; }
  .template-section-row .icon-button {
    width: 28px;
    height: 30px;
    display: grid;
    place-items: center;
    border: 1px solid #e5ebef;
    border-radius: 4px;
    background: #fff;
    color: #a24f4f;
    cursor: pointer;
  }
  .template-section-row .icon-button:disabled { opacity: .4; cursor: not-allowed; }
  .exam-section-row {
    display: grid;
    grid-template-columns: minmax(100px, 1fr) minmax(125px, .8fr) auto;
    align-items: center;
    gap: 10px;
    padding: 7px 0;
    border-bottom: 1px solid #edf1f4;
  }
  .exam-section-row > div { display: grid; gap: 4px; }
  .exam-section-row strong { color: #35495a; font-size: 9px; }
  .exam-section-row small, .template-summary { color: #82909d; font-size: 8px; }
  .availability-low { color: #b14f43 !important; }
  .availability-ok { color: #28806f !important; }
  .template-summary { margin: -3px 0 0; }
  .exam-form-preview {
    display: grid;
    gap: 12px;
  }
  .preview-heading {
    display: grid;
    gap: 5px;
    padding-bottom: 10px;
    border-bottom: 1px solid #e8edf1;
  }
  .preview-heading h3 { margin: 0; color: #263a4e; font-size: 15px; }
  .preview-heading p, .preview-heading small { margin: 0; color: #82909d; font-size: 9px; }
  .preview-section {
    display: grid;
    gap: 8px;
    padding: 10px;
    border: 1px solid #e5ebef;
    border-radius: 5px;
  }
  .preview-section > header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding-bottom: 7px;
    border-bottom: 1px solid #edf1f4;
  }
  .preview-section > header > div { display: grid; gap: 3px; }
  .preview-section strong { color: #35495a; font-size: 10px; }
  .preview-section small, .preview-section > header > span { color: #82909d; font-size: 8px; }
  .preview-question { display: flex; align-items: center; gap: 8px; min-height: 24px; }
  .preview-question > span { display: grid; width: 20px; height: 20px; place-items: center; border: 1px solid #dce5e9; border-radius: 50%; color: #617787; font-size: 8px; }
  .preview-question > div { height: 1px; flex: 1; background: #e8edf1; }
  .admin-text-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px;
    border: 0;
    background: none;
    color: #208b7b;
    font: inherit;
    font-size: 9px;
    cursor: pointer;
  }
  .admin-list {
    display: grid;
  }
  .admin-list article {
    display: flex;
    min-height: 49px;
    align-items: center;
    gap: 9px;
    border-top: 1px solid #edf1f4;
  }
  .account-avatar {
    display: grid;
    width: 29px;
    height: 29px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    background: #e9eff4;
    color: #557185;
    font-size: 9px;
    font-style: normal;
    font-weight: 700;
  }
  .account-avatar.administrator {
    background: #f1ecfa;
    color: #7664ba;
  }
  .admin-list article > div {
    display: grid;
    min-width: 0;
    flex: 1;
    gap: 4px;
  }
  .admin-list strong {
    overflow: hidden;
    color: #35495a;
    font-size: 9px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .admin-list small {
    color: #8896a2;
    font-size: 8px;
  }
  .account-state {
    flex: 0 0 auto;
    padding: 4px 7px;
    border-radius: 12px;
    background: #eaf5f1;
    color: #28806f;
    font-size: 8px;
    white-space: nowrap;
  }
  .account-state.disabled {
    background: #f1f3f5;
    color: #84919b;
  }
  .admin-empty {
    padding: 30px 12px;
    color: #82909d;
    font-size: 10px;
    text-align: center;
  }
  .content-link {
    display: flex;
    width: 100%;
    align-items: center;
    gap: 10px;
    padding: 10px 2px;
    border: 0;
    border-top: 1px solid #edf1f4;
    background: #fff;
    text-align: left;
    cursor: pointer;
  }
  .content-link > span:nth-child(2) {
    display: grid;
    flex: 1;
    gap: 4px;
  }
  .content-link strong {
    color: #35495a;
    font-size: 9px;
  }
  .content-link small {
    color: #8896a2;
    font-size: 8px;
  }
  .content-link > svg {
    color: #73889a;
  }
  .admin-table-panel {
    padding: 15px;
  }
  .admin-table-wrap {
    width: 100%;
    overflow-x: auto;
  }
  .admin-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 9px;
    text-align: left;
  }
  .admin-table th {
    padding: 9px 8px;
    border-top: 1px solid #edf1f4;
    border-bottom: 1px solid #e8edf1;
    color: #768795;
    font-size: 8px;
    font-weight: 600;
    white-space: nowrap;
  }
  .admin-table td {
    height: 48px;
    padding: 7px 8px;
    border-bottom: 1px solid #edf1f4;
    color: #657786;
    white-space: nowrap;
  }
  .admin-table tbody tr:last-child td {
    border-bottom: 0;
  }
  .table-account {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #34495b;
  }
  .table-account strong {
    font-size: 9px;
    font-weight: 600;
  }
  .role-select {
    min-width: 125px;
    padding: 6px 8px;
    border: 1px solid #dfe6eb;
    border-radius: 5px;
    background: #fff;
    color: #526778;
    font: inherit;
    font-size: 8px;
  }
  .table-action {
    padding: 5px 7px;
    border: 0;
    border-radius: 5px;
    background: #edf5f3;
    color: #1f8174;
    font: inherit;
    font-size: 8px;
    cursor: pointer;
  }
  .table-action:hover {
    background: #dff0ec;
  }
  .table-action.danger {
    background: #fff1eb;
    color: #b56545;
  }
  .secondary-text {
    display: block;
    margin-top: 4px;
    color: #8795a1;
    font-size: 8px;
  }
  .description-cell {
    max-width: 330px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .admin-question-list {
    display: grid;
  }
  .admin-question-list article {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 10px;
    padding: 10px 2px;
    border-top: 1px solid #edf1f4;
  }
  .admin-question-list article > div {
    display: grid;
    min-width: 0;
    flex: 1;
    gap: 4px;
  }
  .admin-question-list strong {
    overflow: hidden;
    color: #35495a;
    font-size: 9px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .admin-question-list small {
    color: #8896a2;
    font-size: 8px;
  }
  .question-picker {
    display: grid;
    max-height: 210px;
    gap: 7px;
    overflow-y: auto;
    margin: 0;
    padding: 10px;
    border: 1px solid #e3e9ef;
    border-radius: 6px;
  }
  .question-mode {
    display: flex;
    gap: 5px;
    margin: -5px 0 14px;
    padding: 4px;
    border-radius: 6px;
    background: #f0f4f6;
  }
  .question-mode button {
    min-height: 32px;
    flex: 1;
    border: 0;
    border-radius: 4px;
    background: transparent;
    color: #637787;
    font: inherit;
    font-size: 9px;
    cursor: pointer;
  }
  .question-mode button.active {
    background: #fff;
    color: #167b70;
    box-shadow: 0 1px 3px rgba(20, 40, 55, 0.12);
  }
  .generated-preview {
    display: grid;
    max-height: 250px;
    gap: 8px;
    overflow: auto;
    padding: 10px;
    border: 1px solid #dce8e7;
    border-radius: 6px;
    background: #f8fbfa;
    color: #42605e;
    font-size: 9px;
  }
  .generated-preview article {
    display: grid;
    gap: 4px;
    padding-top: 8px;
    border-top: 1px solid #e4eeec;
  }
  .generated-preview p,
  .generated-preview small {
    margin: 0;
    color: #738582;
    font-size: 8px;
    line-height: 1.5;
  }
  .question-picker legend {
    padding: 0 5px;
    color: #536a7c;
    font-size: 9px;
    font-weight: 650;
  }
  .question-choice {
    display: flex !important;
    grid-template-columns: 15px minmax(0, 1fr);
    align-items: flex-start;
    gap: 8px !important;
    padding: 8px;
    border: 1px solid #edf1f4;
    border-radius: 5px;
    cursor: pointer;
  }
  .question-choice input {
    width: 14px !important;
    min-height: 14px !important;
    margin-top: 2px;
    accent-color: #188b7b;
  }
  .question-choice span {
    display: grid;
    gap: 4px;
  }
  .question-choice strong {
    color: #40576a;
    font-size: 9px;
    font-weight: 550;
  }
  .question-choice small,
  .form-hint,
  .picker-empty {
    color: #83919d;
    font-size: 8px;
  }
  .picker-empty {
    margin: 4px;
    line-height: 1.6;
  }
  .modal-backdrop {
    position: fixed;
    z-index: 80;
    inset: 0;
    display: grid;
    place-items: center;
    padding: 16px;
    background: rgba(17, 31, 45, 0.46);
  }
  .admin-modal {
    width: min(100%, 640px);
    max-height: min(90vh, 760px);
    overflow: auto;
    padding: 19px;
    border: 1px solid #e3e9ef;
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 22px 70px rgba(10, 25, 38, 0.25);
  }
  .admin-modal > header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 17px;
  }
  .admin-modal h2 {
    margin: 0;
    color: #23384b;
    font-size: 17px;
  }
  .modal-close {
    display: grid;
    width: 30px;
    height: 30px;
    place-items: center;
    border: 0;
    border-radius: 5px;
    background: #f2f5f7;
    color: #657888;
    cursor: pointer;
  }
  .admin-modal form {
    display: grid;
    gap: 12px;
  }
  .admin-modal label {
    display: grid;
    gap: 6px;
    color: #556a7b;
    font-size: 9px;
    font-weight: 600;
  }
  .admin-modal input,
  .admin-modal select,
  .admin-modal textarea {
    width: 100%;
    min-height: 35px;
    box-sizing: border-box;
    padding: 8px 9px;
    border: 1px solid #dce4eb;
    border-radius: 5px;
    background: #fff;
    color: #33495b;
    font: inherit;
    font-size: 10px;
  }
  .admin-modal input:focus,
  .admin-modal select:focus,
  .admin-modal textarea:focus {
    border-color: #4eaa9a;
    outline: 3px solid rgba(46, 155, 139, 0.12);
  }
  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .admin-modal form footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 6px;
    padding-top: 13px;
    border-top: 1px solid #edf1f4;
  }
  .admin-scrim,
  .admin-menu-button {
    display: none;
  }
  @media (max-width: 1050px) {
    .admin-sidebar {
      width: 205px;
    }
    .admin-workspace {
      margin-left: 205px;
    }
    .admin-content {
      padding: 18px 14px 28px;
    }
    .admin-stats {
      gap: 8px;
    }
    .admin-stats article {
      padding: 10px;
    }
    .admin-overview-grid {
      gap: 9px;
    }
  }
  @media (max-width: 800px) {
    .knowledge-summary {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .knowledge-layout {
      grid-template-columns: minmax(0, 1fr);
    }
    .admin-sidebar {
      z-index: 45;
      width: 235px;
      transform: translateX(-100%);
      transition: transform 0.2s ease;
    }
    .admin-sidebar.admin-sidebar-open {
      transform: translateX(0);
    }
    .admin-workspace {
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
      padding: 0 11px;
    }
    .admin-search {
      width: auto;
      flex: 1;
    }
    .admin-profile div,
    .admin-profile > svg {
      display: none;
    }
    .admin-top-actions {
      gap: 8px;
    }
    .admin-stats {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
  @media (max-width: 520px) {
    .admin-content {
      padding: 16px 10px 24px;
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
    .admin-table {
      min-width: 620px;
    }
    .admin-table-panel {
      padding: 11px;
    }
    .form-row {
      grid-template-columns: minmax(0, 1fr);
    }
    .admin-modal {
      padding: 15px;
    }
    .template-section-row {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 28px;
    }
    .template-section-row label:nth-child(1) { grid-column: 1; grid-row: 1; }
    .template-section-row label:nth-child(2) { grid-column: 2; grid-row: 1; }
    .template-section-row label:nth-child(3) { grid-column: 1 / -1; grid-row: 2; }
    .template-section-row label:nth-child(4) { grid-column: 1; grid-row: 3; }
    .template-section-row label:nth-child(5) { grid-column: 2; grid-row: 3; }
    .template-section-row .icon-button { grid-column: 3; grid-row: 1; }
    .exam-section-row {
      grid-template-columns: minmax(0, 1fr) minmax(100px, .8fr);
    }
    .exam-section-row > small { grid-column: 1 / -1; }
  }
</style>
