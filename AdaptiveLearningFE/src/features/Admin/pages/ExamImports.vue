<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { AlertTriangle, ArrowLeft, Check, ChevronDown, ChevronRight, FileText, Image as ImageIcon, LoaderCircle, Save, Search, Trash2, Upload } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { adminService } from '@/services/adminService'
import { useUserStore } from '@/stores/user'
import { confirm } from '@/components/ui/overlay/useConfirm'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => {
  const role = userStore.state.role
  return role === 'ROLE_ADMIN' || role === 'ADMIN'
})
const canReview = computed(() => {
  const role = userStore.state.role
  return role === 'ROLE_ADMIN' || role === 'ADMIN' || Boolean(userStore.state.canApproveExams)
})
const certificates = ref([])
const topics = ref([])
const imports = ref([])
const selected = ref(null)
const sourcePdfUrl = ref('')
const resourceUrls = ref({})
const sectionAudioFiles = reactive({})
const sectionImageFiles = reactive({})
const examFile = ref(null)
const answerFile = ref(null)
const answerInExam = ref(false)
const examInput = ref(null)
const answerInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const importingAnswerKey = ref(false)
const deletingImportId = ref(null)
const itemToDelete = ref(null)
const analysisProgress = ref(null)
const saving = ref(false)
const unansweredOnly = ref(false)
const questionSearch = ref('')
const collapsedSections = ref(new Set())
const liveOcrQuestions = computed(() => analysisProgress.value?.questions || [])
function liveQuestionSkill(question) {
  if (question.skill) return question.skill
  const topic = reviewTopics.value.find((item) => String(item.topicId) === String(question.topicId))
  return topic ? topicPath(topic, reviewTopics.value) : ''
}
const error = ref('')
const notice = ref('')
const form = reactive({ title: '', certificateId: '', topicId: '' })
const availableTopics = computed(() => topics.value.filter((topic) => String(topic.certificateId) === String(form.certificateId)))
const reviewTopics = computed(() => topics.value.filter((topic) => String(topic.certificateId) === String(selected.value?.certificateId)))
const availableTopicGroups = computed(() => groupTopics(availableTopics.value))
const reviewTopicGroups = computed(() => groupTopics(reviewTopics.value))
const missingAnswerCount = computed(() => (selected.value?.questions || []).filter(
  (question) => !String(question.correctAnswer ?? '').trim(),
).length)
const answeredCount = computed(() => Math.max(0, (selected.value?.questions?.length || 0) - missingAnswerCount.value))
const uploadingAudioKey = ref('')
const uploadingImageKey = ref('')
const reviewSections = computed(() => {
  const sections = new Map()
  for (const [index, question] of (selected.value?.questions || []).entries()) {
    const topicId = question.topicId ?? selected.value?.topicId
    const majorSkill = majorSkillFor(topicId, reviewTopics.value)
    const sourceTitle = String(question.sourceSectionTitle || '').trim()
    const skillKey = majorSkill ? String(majorSkill.topicId) : 'unclassified'
    const sourceSlug = sourceTitle.toLowerCase().replace(/[^a-z0-9_-]+/g, '-').replace(/^-|-$/g, '').slice(0, 32) || 'section'
    const key = sourceTitle ? `source:${skillKey}:${sourceSlug}` : skillKey
    if (!sections.has(key)) {
      sections.set(key, {
        key,
        resourceKeys: sourceTitle ? [key, `source:${sourceTitle.toLowerCase()}`] : [key],
        title: sourceTitle || majorSkill?.name || 'Chưa phân loại kỹ năng',
        sourceSectionTitle: sourceTitle,
        sourceSectionOrder: question.sourceSectionOrder,
        sourceQuestionStart: question.sourceSectionQuestionStart,
        sourceQuestionEnd: question.sourceSectionQuestionEnd,
        sectionInstructions: question.sectionInstructions || '',
        stimulusContent: question.stimulusContent || '',
        majorSkill,
        questions: [],
      })
    }
    const section = sections.get(key)
    if (section.sourceSectionOrder == null && question.sourceSectionOrder != null) section.sourceSectionOrder = question.sourceSectionOrder
    if (!section.sourceQuestionStart && question.sourceSectionQuestionStart) section.sourceQuestionStart = question.sourceSectionQuestionStart
    if (!section.sourceQuestionEnd && question.sourceSectionQuestionEnd) section.sourceQuestionEnd = question.sourceSectionQuestionEnd
    if (!section.majorSkill && majorSkill) section.majorSkill = majorSkill
    if (!section.sectionInstructions && question.sectionInstructions) section.sectionInstructions = question.sectionInstructions
    if (!section.stimulusContent && question.stimulusContent) section.stimulusContent = question.stimulusContent
    section.questions.push({ question, index })
  }

  const sectionList = [...sections.values()]
  const namedRanges = sectionList
    .filter((section) => section.sourceSectionTitle && section.majorSkill)
    .map((section) => ({ section, range: declaredSectionRange(section) }))
    .filter((entry) => entry.range)
  const mergedSections = new Set()

  // OCR chunks can lose the heading on continuation pages; attach them only when one skill range fits every source number.
  for (const section of sectionList.filter((item) => !item.sourceSectionTitle && item.majorSkill)) {
    const targets = section.questions.map(({ question }) => {
      const number = parsedQuestionNumber(question.sourceQuestionNumber)
      if (number == null) return null
      const matches = namedRanges
        .filter(({ section: target, range }) => target.majorSkill.topicId === section.majorSkill.topicId
          && number >= range.start && number <= range.end)
        .sort((a, b) => {
          const aIsSection = /^section\b/i.test(a.section.sourceSectionTitle) ? 1 : 0
          const bIsSection = /^section\b/i.test(b.section.sourceSectionTitle) ? 1 : 0
          return bIsSection - aIsSection || (b.range.end - b.range.start) - (a.range.end - a.range.start)
        })
      return matches[0]?.section || null
    })
    const target = targets[0]
    if (!target || targets.some((item) => item !== target)) continue
    target.questions.push(...section.questions)
    target.resourceKeys = [...new Set([...target.resourceKeys, ...section.resourceKeys])]
    mergedSections.add(section)
  }

  let majorIndex = 0
  return sectionList.filter((section) => !mergedSections.has(section)).map((section) => ({
    ...section,
    rangeAudit: sectionQuestionAudit(section),
    roman: section.sourceSectionOrder != null
      ? romanNumeral(section.sourceSectionOrder - 1)
      : section.majorSkill ? romanNumeral(majorIndex++) : null,
  }))
})
const visibleReviewSections = computed(() => {
  const query = questionSearch.value.trim().toLowerCase()
  return reviewSections.value.map((section) => ({
    ...section,
    questions: section.questions.filter(({ question, index }) => {
      const hasAnswer = Boolean(String(question.correctAnswer ?? '').trim())
      const matchesQuery = !query || `${index + 1} ${question.sourceQuestionNumber || ''} ${question.content || ''}`.toLowerCase().includes(query)
      return (!unansweredOnly.value || !hasAnswer) && matchesQuery
    }),
  })).filter((section) => section.questions.length)
})

function clearSourcePdf() {
  if (sourcePdfUrl.value) URL.revokeObjectURL(sourcePdfUrl.value)
  sourcePdfUrl.value = ''
}

async function loadSourcePdf(item) {
  clearSourcePdf()
  if (!item?.sourceFileName || item.sourceFileContentType !== 'application/pdf') return
  try {
    const response = await adminService.examImportSource(item.importId)
    sourcePdfUrl.value = URL.createObjectURL(response.data)
  } catch {
    error.value = 'Không thể tải PDF gốc để đối chiếu.'
  }
}

function isListeningSection(section) {
  return /listening|nghe/i.test(`${section?.title || ''} ${section?.majorSkill?.name || ''}`)
}

function questionDisplayNumber(question, index) {
  const sourceNumber = String(question?.sourceQuestionNumber || '').trim()
  if (!sourceNumber) return String(index + 1).padStart(2, '0')
  const match = sourceNumber.match(/\d+(?:\s*[-–]\s*\d+)?/)
  return match ? match[0].replace(/\s*([-–])\s*/, '$1') : sourceNumber
}

function isSectionCollapsed(key) {
  return collapsedSections.value.has(key)
}

function toggleSection(key) {
  const next = new Set(collapsedSections.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  collapsedSections.value = next
}

function blankContentParts(question) {
  const content = String(question?.content || '')
  const parts = content.split(/(\[\s*\d+\s*\]|_{3,})/g).filter(Boolean)
  if (parts.length === 1) return [{ text: content }]
  return parts.map((part) => {
    const number = part.match(/^\[\s*(\d+)\s*\]$/)?.[1]
    return number || /^_{3,}$/.test(part)
      ? { blankNumber: number || question.sourceQuestionNumber || '', text: '' }
      : { text: part }
  })
}

function parsedQuestionNumber(value) {
  const match = String(value ?? '').match(/\d+/)
  return match ? Number(match[0]) : null
}

function declaredSectionRange(section) {
  const instructionRange = String(section.sectionInstructions || '').match(/\bquestions?\s*(\d+)\s*[-–]\s*(\d+)/i)
  const start = parsedQuestionNumber(section.sourceQuestionStart) ?? (instructionRange ? Number(instructionRange[1]) : null)
  const end = parsedQuestionNumber(section.sourceQuestionEnd) ?? (instructionRange ? Number(instructionRange[2]) : null)
  return start != null && end != null && end >= start ? { start, end } : null
}

function sectionQuestionAudit(section) {
  const actualCount = section.questions.length
  const range = declaredSectionRange(section)
  if (!range) return { expectedCount: null, actualCount, missingNumbers: [], mismatch: false }

  const expectedCount = range.end - range.start + 1
  const questionNumbers = section.questions.map(({ question }) => parsedQuestionNumber(question.sourceQuestionNumber))
  const missingNumbers = questionNumbers.every((number) => number != null) && expectedCount <= 500
    ? Array.from({ length: expectedCount }, (_, index) => range.start + index)
      .filter((number) => !questionNumbers.includes(number))
    : []

  return {
    expectedCount,
    actualCount,
    missingNumbers,
    mismatch: expectedCount !== actualCount || missingNumbers.length > 0,
  }
}

function sectionQuestionRange(section) {
  const questionNumbers = section.questions
    .map(({ question, index }) => question.sourceQuestionNumber || String(index + 1))
    .map((value) => String(value).match(/^\D*(\d+)/)?.[1])
    .filter(Boolean)
    .map(Number)
  const declaredRange = declaredSectionRange(section)
  const start = declaredRange ? String(declaredRange.start) : section.sourceQuestionStart || (questionNumbers.length ? String(Math.min(...questionNumbers)) : '')
  const end = declaredRange ? String(declaredRange.end) : section.sourceQuestionEnd || (questionNumbers.length ? String(Math.max(...questionNumbers)) : '')
  if (!start && !end) return ''
  return start === end ? `Question ${start}` : `Questions ${start || '?'}–${end || '?'}`
}

function updateSectionText(section, field, value) {
  section.questions.forEach(({ question }) => { question[field] = value })
}

function sectionResourcesFor(sectionKey, resourceType) {
  const section = typeof sectionKey === 'object'
    ? sectionKey
    : reviewSections.value.find((item) => item.key === String(sectionKey))
  const key = typeof sectionKey === 'object' ? sectionKey.key : String(sectionKey)
  const sectionKeys = new Set(section?.resourceKeys || [key])
  return (selected.value?.resources || []).filter((resource) =>
    resource.resourceType === resourceType && sectionKeys.has(String(resource.sectionKey)),
  )
}

function clearResourcePreviews() {
  Object.values(resourceUrls.value).forEach((url) => URL.revokeObjectURL(url))
  resourceUrls.value = {}
}

async function loadResourcePreviews(importId, resources = []) {
  clearResourcePreviews()
  const assets = resources.filter((resource) => ['AUDIO', 'IMAGE'].includes(resource.resourceType))
  const loaded = await Promise.all(assets.map(async (resource) => {
    try {
      const response = await adminService.examImportResourceFile(importId, resource.resourceId)
      return [resource.resourceId, URL.createObjectURL(response.data)]
    } catch {
      return null
    }
  }))
  resourceUrls.value = Object.fromEntries(loaded.filter(Boolean))
}

function chooseSectionAudio(event, sectionKey) {
  const file = event.target.files?.[0] || null
  if (!file) return
  const validType = /\.(mp3|wav|m4a|ogg|aac)$/i.test(file.name)
  if (!validType || file.size > 50 * 1024 * 1024) {
    error.value = 'Chọn file MP3, WAV, M4A, OGG hoặc AAC tối đa 50 MB.'
    event.target.value = ''
    return
  }
  sectionAudioFiles[sectionKey] = file
  error.value = ''
}

async function uploadSectionAudio(section) {
  const file = sectionAudioFiles[section.key]
  if (!selected.value || !file || uploadingAudioKey.value) return
  uploadingAudioKey.value = section.key
  error.value = ''
  try {
    const body = new FormData()
    body.append('audioFile', file)
    const resource = await adminService.uploadExamImportAudio(
      selected.value.importId, section.key, section.title, body,
    )
    selected.value.resources = [...(selected.value.resources || []), resource]
    sectionAudioFiles[section.key] = null
    const response = await adminService.examImportResourceFile(selected.value.importId, resource.resourceId)
    resourceUrls.value = { ...resourceUrls.value, [resource.resourceId]: URL.createObjectURL(response.data) }
    notice.value = `Đã gắn file nghe vào mục ${section.roman || ''} ${section.title}.`
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không thể tải file nghe lên.'
  } finally {
    uploadingAudioKey.value = ''
  }
}

function chooseSectionImage(event, sectionKey) {
  const file = event.target.files?.[0] || null
  if (!file) return
  if (!/\.(png|jpe?g|webp)$/i.test(file.name) || file.size > 10 * 1024 * 1024) {
    error.value = 'Chọn ảnh PNG, JPG hoặc WEBP tối đa 10 MB.'
    event.target.value = ''
    return
  }
  sectionImageFiles[sectionKey] = file
  error.value = ''
}

async function uploadSectionImage(section) {
  const file = sectionImageFiles[section.key]
  if (!selected.value || !file || uploadingImageKey.value) return
  uploadingImageKey.value = section.key
  error.value = ''
  try {
    const body = new FormData()
    body.append('imageFile', file)
    const resource = await adminService.uploadExamImportImage(
      selected.value.importId, section.key, section.title, body,
    )
    selected.value.resources = [...(selected.value.resources || []), resource]
    sectionImageFiles[section.key] = null
    const response = await adminService.examImportResourceFile(selected.value.importId, resource.resourceId)
    resourceUrls.value = { ...resourceUrls.value, [resource.resourceId]: URL.createObjectURL(response.data) }
    notice.value = `Đã gắn hình vào mục ${section.roman || ''} ${section.title}.`
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không thể tải hình lên.'
  } finally {
    uploadingImageKey.value = ''
  }
}

function majorSkillFor(topicId, topicList) {
  let current = topicList.find((topic) => String(topic.topicId) === String(topicId))
  if (!current) return null
  const path = []
  const seen = new Set()
  while (current && !seen.has(current.topicId)) {
    seen.add(current.topicId)
    path.unshift(current)
    current = topicList.find((topic) => String(topic.topicId) === String(current.parentId))
  }
  return path.find((topic) => topic.type === 'SKILL') || path[0] || null
}

function romanNumeral(index) {
  return ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'][index] || String(index + 1)
}

function topicPath(topic, topicList) {
  const path = []
  const seen = new Set()
  let current = topic
  while (current && !seen.has(current.topicId)) {
    seen.add(current.topicId)
    path.unshift(current.name)
    current = topicList.find((candidate) => String(candidate.topicId) === String(current.parentId))
  }
  return path.join(' / ')
}

function groupTopics(topicList) {
  const groups = new Map()
  for (const topic of topicList) {
    let major = topic
    const seen = new Set()
    while (major.parentId != null && !seen.has(major.topicId)) {
      seen.add(major.topicId)
      major = topicList.find((candidate) => String(candidate.topicId) === String(major.parentId)) || major
      if (major.parentId == null) break
      if (major === topic) break
    }
    const groupId = major.topicId
    if (!groups.has(groupId)) groups.set(groupId, { id: groupId, name: major.name, topics: [] })
    groups.get(groupId).topics.push(topic)
  }
  return [...groups.values()]
}

watch(() => form.certificateId, async (certificateId) => {
  form.topicId = ''
  topics.value = certificateId ? await adminService.topics(certificateId).catch(() => []) : []
})

async function initPage() {
  if (userStore.state.accessToken && !userStore.state.role) {
    await userStore.getInfo().catch(() => {})
  }
  certificates.value = unwrapList(await adminService.certificates().catch(() => []))
  await loadImports()
}

onMounted(() => {
  initPage()
})

onUnmounted(() => {
  clearSourcePdf()
  clearResourcePreviews()
})

watch(() => userStore.state.role, (newRole, oldRole) => {
  if (newRole && !oldRole) initPage()
})

function unwrapList(value) {
  if (Array.isArray(value)) return value
  return value?.items || value?.content || value?.data || []
}

async function loadImports() {
  loading.value = true
  error.value = ''
  try {
    imports.value = unwrapList(await adminService.examImports())
    if (selected.value) {
      const refreshed = imports.value.find((item) => item.importId === selected.value.importId)
      if (!refreshed) selected.value = null
    }
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không thể tải hàng chờ duyệt.'
  } finally {
    loading.value = false
  }
}

function chooseFile(event, kind) {
  const file = event.target.files?.[0] || null
  if (!file) return
  const lowerName = file.name.toLowerCase()
  const isValidFormat = lowerName.endsWith('.pdf') || lowerName.endsWith('.md') || lowerName.endsWith('.markdown') || lowerName.endsWith('.txt')
  if (!isValidFormat || file.size > 20 * 1024 * 1024) {
    error.value = 'Chỉ nhận file PDF hoặc Markdown (.md, .txt) tối đa 20 MB.'
    event.target.value = ''
    return
  }
  if (kind === 'exam') examFile.value = file
  else answerFile.value = file
  error.value = ''
}

async function uploadExam() {
  if (!examFile.value || !form.title.trim() || !form.certificateId) {
    error.value = 'Cần nhập tên bộ đề, chọn kỳ thi và đính kèm file đề.'
    return
  }
  uploading.value = true
  analysisProgress.value = null
  error.value = ''
  notice.value = ''
  try {
    const body = new FormData()
    body.append('title', form.title.trim())
    body.append('certificateId', form.certificateId)
    body.append('answerInExam', String(answerInExam.value))
    if (form.topicId) {
      body.append('topicId', form.topicId)
    }
    body.append('examFile', examFile.value)
    let progress = await adminService.startExamImportAnalysis(body)
    analysisProgress.value = progress
    while (progress.status === 'PROCESSING') {
      await new Promise((resolve) => window.setTimeout(resolve, 1000))
      progress = await adminService.examImportAnalysisProgress(progress.jobId)
      analysisProgress.value = progress
    }
    if (progress.status !== 'COMPLETED') {
      throw new Error(progress.errorMessage || 'Phân tích bộ đề thất bại.')
    }
    const created = await adminService.examImport(progress.importId)
    form.title = ''
    examFile.value = null
    answerFile.value = null
    answerInExam.value = false
    if (examInput.value) examInput.value.value = ''
    if (answerInput.value) answerInput.value.value = ''
    selected.value = created
    collapsedSections.value = new Set()
    await loadSourcePdf(created)
    await loadResourcePreviews(created.importId, created.resources || [])
    notice.value = created.answerGenerated
      ? 'Đã phân tích đề và tự động phân loại kỹ năng cho từng câu bằng AI. Giáo viên kiểm tra trước khi xác nhận.'
      : 'Đã phân tích đề và ghép đáp án. AI đã tự động phân loại kỹ năng cho từng câu.'
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || exception?.response?.data?.message || exception?.message || 'Không thể nhập bộ đề.'
  } finally {
    uploading.value = false
  }
}

async function openImport(item) {
  error.value = ''
  notice.value = ''
  answerFile.value = null
  if (answerInput.value) answerInput.value.value = ''
  saving.value = true
  try {
    selected.value = await adminService.examImport(item.importId)
    collapsedSections.value = new Set()
    await loadSourcePdf(selected.value)
    await loadResourcePreviews(selected.value.importId, selected.value.resources || [])
    if (selected.value?.certificateId) {
      topics.value = await adminService.topics(selected.value.certificateId).catch(() => [])
    }
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không thể mở bộ đề.'
  } finally {
    saving.value = false
  }
}

function promptDelete(item) {
  if (!item || deletingImportId.value) return
  error.value = ''
  itemToDelete.value = item
}

function cancelDelete() {
  if (deletingImportId.value) return
  itemToDelete.value = null
}

async function confirmDeleteImport(item) {
  if (!item || deletingImportId.value) return
  const id = item.importId
  const title = item.title
  deletingImportId.value = id
  error.value = ''
  notice.value = ''
  try {
    await adminService.deleteExamImport(id)
    if (selected.value?.importId === id) {
      selected.value = null
      clearSourcePdf()
      clearResourcePreviews()
    }
    itemToDelete.value = null
    notice.value = `Đã xóa bộ đề "${title}".`
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage
      || exception?.response?.data?.message || 'Không thể xóa bộ đề.'
  } finally {
    deletingImportId.value = null
  }
}

// Giữ lại alias deleteImport trỏ tới promptDelete để tương thích
function deleteImport(item) {
  promptDelete(item)
}

async function importAnswerKey() {
  if (!selected.value || !answerFile.value || importingAnswerKey.value || saving.value) {
    if (!answerFile.value) error.value = 'Chọn file đáp án trước khi ghép.'
    return
  }
  importingAnswerKey.value = true
  error.value = ''
  notice.value = ''
  try {
    const formData = new FormData()
    formData.append('answerFile', answerFile.value)
    const canDelete = selected.value.canDelete
    const result = await adminService.importExamAnswerKey(selected.value.importId, formData)
    selected.value = { ...result.examImport, canDelete }
    notice.value = `Đã ghép đáp án cho ${result.matchedAnswerCount}/${result.totalQuestionCount} câu.`
    answerFile.value = null
    if (answerInput.value) answerInput.value.value = ''
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || exception?.response?.data?.message || 'Không thể ghép file đáp án.'
  } finally {
    importingAnswerKey.value = false
  }
}

function normalizeAnswer(question) {
  if (question.correctAnswer == null) question.correctAnswer = ''
  if (question.options == null) question.options = []
}

async function saveReview() {
  if (!selected.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    const canDelete = selected.value.canDelete
    const updated = await adminService.updateExamImport(selected.value.importId, {
      questions: selected.value.questions,
    })
    selected.value = { ...updated, canDelete }
    notice.value = 'Đã lưu chỉnh sửa.'
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || exception?.response?.data?.message || 'Không thể lưu thay đổi.'
  } finally {
    saving.value = false
  }
}

async function confirmImport() {
  if (!selected.value || saving.value) return
  if (missingAnswerCount.value) {
    error.value = `Còn ${missingAnswerCount.value} câu chưa có đáp án. Hãy bổ sung đáp án cho các câu được viền đỏ trước khi xác nhận.`
    return
  }
  if (reviewTopics.value.length && selected.value.questions.some((question) => !question.topicId)) {
    error.value = 'Hãy chọn kỹ năng/môn cho từng câu chưa được phân loại trước khi xác nhận.'
    return
  }
  const accepted = await confirm({
    title: 'Xác nhận bộ đề chính thức?',
    description: 'Các câu hỏi và đáp án hiện tại sẽ được đưa vào ngân hàng câu hỏi đã duyệt để dùng khi tạo đề.',
    confirmText: 'Xác nhận bộ đề',
    cancelText: 'Tiếp tục rà soát',
  })
  if (!accepted) return
  saving.value = true
  error.value = ''
  try {
    await adminService.updateExamImport(selected.value.importId, { questions: selected.value.questions })
    await adminService.confirmExamImport(selected.value.importId)
    selected.value = null
    clearSourcePdf()
    clearResourcePreviews()
    notice.value = 'Đã xác nhận. Bộ câu hỏi đã được thêm vào ngân hàng đề.'
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || exception?.response?.data?.message || 'Không thể xác nhận bộ đề.'
  } finally {
    saving.value = false
  }
}

function addOption(question) {
  question.options.push({ label: String.fromCharCode(65 + question.options.length), text: '' })
}

function removeOption(question, index) {
  question.options.splice(index, 1)
  question.options.forEach((option, optionIndex) => { option.label = String.fromCharCode(65 + optionIndex) })
  if (!question.options.some((option) => option.label === question.correctAnswer)) {
    question.correctAnswer = question.options[0]?.label || ''
  }
}

function changeQuestionType(question) {
  if (question.questionType !== 'MULTIPLE_CHOICE') question.options = []
  else if (!question.options.length) question.correctAnswer = ''
}
</script>

<template>
  <main class="exam-import-page">
    <header class="page-heading">
      <div>
      <button v-if="isAdmin" class="back-button" type="button" aria-label="Quay lại" @click="router.push('/admin')"><ArrowLeft :size="17" /></button>
        <div><p class="eyebrow">{{ isAdmin ? 'QUẢN TRỊ / NGÂN HÀNG ĐỀ' : 'GIÁO VIÊN / DUYỆT ĐỀ' }}</p><h1>{{ isAdmin ? 'Nhập bộ đề' : 'Duyệt bộ đề' }}</h1></div>
      </div>
      <span class="queue-count">{{ imports.length }} bộ chờ duyệt</span>
    </header>

    <p v-if="error" class="feedback error" role="alert">{{ error }}</p>
    <p v-if="notice" class="feedback success" role="status">{{ notice }}</p>

    <section v-if="isAdmin" class="upload-section">
      <form class="upload-form" @submit.prevent="uploadExam">
        <div class="section-heading"><div><h2>Tải đề nguồn</h2><p>Tải PDF để OCR và tạo các câu hỏi bên dưới. File đáp án riêng có thể ghép sau khi đề được nhận diện.</p></div><Upload :size="19" /></div>
        <div class="form-grid">
          <label>Tên bộ đề<input v-model.trim="form.title" maxlength="200" placeholder="Ví dụ: Đề tổng hợp 4 kỹ năng" required /></label>
          <label>Kỳ thi<select v-model="form.certificateId" required><option value="" disabled>Chọn kỳ thi</option><option v-for="certificate in certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label>
          <label>Mục lớn / kỹ năng <small class="text-slate-500 font-normal">(Tùy chọn - để trống nếu đề gồm nhiều kỹ năng)</small>
            <select v-model="form.topicId" :disabled="!availableTopics.length">
              <option value="">Tất cả / AI tự phân loại từng câu</option>
              <template v-for="group in availableTopicGroups" :key="group.id">
                <option v-if="group.topics.length === 1 && group.topics[0].topicId === group.id" :value="String(group.id)">
                  {{ group.name }}
                </option>
                <optgroup v-else :label="group.name">
                  <option v-for="topic in group.topics" :key="topic.topicId" :value="String(topic.topicId)">
                    {{ topicPath(topic, availableTopics) }}
                  </option>
                </optgroup>
              </template>
            </select>
          </label>
        </div>
        <label class="answer-in-exam"><input v-model="answerInExam" type="checkbox" @change="answerFile = null; if (answerInput) answerInput.value = ''" /><span>PDF đề đã có đáp án</span></label>
        <div class="file-grid">
          <label class="file-field"><span>File đề (PDF / Markdown) <b>*</b></span><input ref="examInput" type="file" accept="application/pdf,.pdf,.md,.markdown,.txt" required @change="chooseFile($event, 'exam')" /><small>{{ examFile?.name || 'File PDF hoặc .md, tối đa 20 MB' }}</small></label>
        </div>
        <div v-if="analysisProgress" class="analysis-progress" role="status">
          <div><span>{{ analysisProgress.message }}</span><strong>{{ analysisProgress.progress }}%</strong></div>
          <progress :value="analysisProgress.progress" max="100" />
          <small v-if="analysisProgress.totalChunks">Đã xử lý {{ analysisProgress.completedChunks }} / {{ analysisProgress.totalChunks }} phần</small>
        </div>
        <section v-if="liveOcrQuestions.length && analysisProgress?.status !== 'COMPLETED'" class="live-ocr-preview" aria-live="polite" aria-label="Câu hỏi đã nhận diện">
          <header class="live-ocr-heading">
            <div><strong>Đã nhận diện {{ liveOcrQuestions.length }} câu</strong><small>Câu hỏi sẽ xuất hiện khi OCR xử lý xong từng trang hoặc phần.</small></div>
            <span>{{ analysisProgress?.completedChunks || 0 }} / {{ analysisProgress?.totalChunks || 0 }} phần</span>
          </header>
          <p v-if="analysisProgress?.status === 'FAILED'" class="live-ocr-error">{{ analysisProgress.errorMessage || 'Phân tích dừng trước khi hoàn tất. Các câu đã nhận diện bên dưới vẫn được giữ để đối chiếu.' }}</p>
          <article v-for="(question, index) in liveOcrQuestions" :key="`${question.sourcePageNumber || 0}-${question.sourceQuestionNumber || index}-${index}`" class="live-ocr-question">
            <header><strong>{{ questionDisplayNumber(question, index) }}</strong><span v-if="question.sourcePageNumber">Trang PDF {{ question.sourcePageNumber }}</span><span v-if="liveQuestionSkill(question)">{{ liveQuestionSkill(question) }}</span><span>{{ question.questionType?.replaceAll('_', ' ') || 'Câu hỏi' }}</span></header>
            <p>{{ question.content }}</p>
            <ul v-if="question.options?.length"><li v-for="option in question.options" :key="option.label"><strong>{{ option.label }}.</strong> {{ option.text }}</li></ul>
            <small v-if="question.sectionInstructions">{{ question.sectionInstructions }}</small>
          </article>
        </section>
        <div class="form-actions"><button type="submit" :disabled="uploading"><LoaderCircle v-if="uploading" class="spin" :size="16" /><Upload v-else :size="16" />{{ uploading ? (analysisProgress?.message || 'Đang chuẩn bị phân tích…') : 'Nhập và phân tích' }}</button></div>
      </form>
    </section>

    <section v-if="!canReview" class="no-permission"><h2>Chưa được cấp quyền duyệt đề</h2><p>Liên hệ quản trị viên để được gán quyền duyệt cho tài khoản giáo viên này.</p></section>

    <div v-else class="work-area" :class="{ 'review-only': !isAdmin }">
      <aside class="import-list">
        <div class="list-heading"><div><h2>Chờ giáo viên duyệt</h2><p>{{ imports.length }} bộ đề</p></div><button type="button" aria-label="Tải lại" @click="loadImports"><LoaderCircle v-if="loading" class="spin" :size="16" /><span v-else>↻</span></button></div>
        <div v-for="item in imports" :key="item.importId" class="import-row-wrap">
          <button class="import-row" :class="{ selected: selected?.importId === item.importId }" type="button" @click="openImport(item)">
            <FileText :size="18" /><span><strong>{{ item.title }}</strong><small>{{ item.questionCount }} câu · {{ item.answerGenerated ? 'AI gợi ý đáp án' : 'Có đáp án nguồn' }}</small></span>
          </button>
          <button v-if="isAdmin || item.canDelete" class="delete-import-button" type="button" :aria-label="`Xóa bộ đề ${item.title}`" title="Xóa bộ đề" :disabled="deletingImportId === item.importId" @click.stop="deleteImport(item)">
            <LoaderCircle v-if="deletingImportId === item.importId" class="spin" :size="16" />
            <Trash2 v-else :size="16" />
          </button>
        </div>
        <p v-if="!loading && !imports.length" class="empty-list">Chưa có bộ đề cần duyệt.</p>
      </aside>

      <section v-if="selected" class="review-panel" :class="{ 'has-source-pdf': sourcePdfUrl }">
        <header class="review-heading">
          <div>
            <p class="eyebrow">{{ selected.answerGenerated ? 'ĐÁP ÁN AI GỢI Ý · CẦN KIỂM TRA' : 'ĐÁP ÁN TỪ NGUỒN ĐỀ / ĐÁP ÁN' }}</p>
            <h2>{{ selected.title }}</h2>
            <p>{{ selected.questionCount }} câu · {{ selected.questions?.length || 0 }} câu đang xem</p>
          </div>
          <div class="review-heading-actions">
            <button v-if="isAdmin || selected.canDelete" class="delete-header-btn" type="button" title="Xóa bộ đề đang xem" :disabled="deletingImportId === selected.importId" @click="deleteImport(selected)">
              <LoaderCircle v-if="deletingImportId === selected.importId" class="spin" :size="14" />
              <Trash2 v-else :size="14" />
              <span>Xóa bộ đề</span>
            </button>
            <span class="status-pill">Chờ xác nhận</span>
          </div>
        </header>
        <p v-if="missingAnswerCount" class="feedback error" role="status">{{ missingAnswerCount }} câu chưa có đáp án; cần hoàn thiện trước khi xác nhận chính thức.</p>
        <details v-if="sourcePdfUrl" class="source-pdf-details" open>
          <summary>PDF gốc · {{ selected.sourceFileName }}</summary>
          <iframe :src="sourcePdfUrl" :title="`PDF gốc ${selected.title}`" />
        </details>
        <details class="source-details"><summary>Đọc nội dung PDF nguồn</summary><div><h3>Đề</h3><pre>{{ selected.sourceContent }}</pre><template v-if="selected.answerContent"><h3>Đáp án gốc</h3><pre>{{ selected.answerContent }}</pre></template></div></details>
        <div class="review-tools">
          <div class="review-progress">
            <div><strong>{{ answeredCount }} / {{ selected.questions?.length || 0 }}</strong><span>câu đã có đáp án</span><small>{{ missingAnswerCount }} câu cần rà soát</small></div>
            <progress :value="answeredCount" :max="selected.questions?.length || 1" />
          </div>
          <div class="review-filters">
            <label class="question-search"><Search :size="16" /><input v-model.trim="questionSearch" type="search" placeholder="Tìm theo số câu hoặc nội dung" /></label>
            <label class="unanswered-filter"><input v-model="unansweredOnly" type="checkbox" /> Chưa có đáp án</label>
          </div>
        </div>
        <section class="answer-key-upload" aria-label="Ghép file đáp án">
          <label class="file-field"><span>Ghép file đáp án sau OCR</span><input ref="answerInput" type="file" accept="application/pdf,.pdf,.md,.markdown,.txt" @change="chooseFile($event, 'answer')" /><small>{{ answerFile?.name || 'Chọn PDF, Markdown hoặc TXT có đáp án' }}</small></label>
          <button type="button" :disabled="importingAnswerKey || saving || !answerFile" @click="importAnswerKey"><LoaderCircle v-if="importingAnswerKey" class="spin" :size="16" /><Upload v-else :size="16" />{{ importingAnswerKey ? 'Đang ghép đáp án…' : 'Ghép đáp án' }}</button>
        </section>
        <section v-for="section in visibleReviewSections" :key="section.key" class="major-skill-section" :class="{ 'is-collapsed': isSectionCollapsed(section.key) }">
          <header class="major-skill-heading">
            <span v-if="section.majorSkill">{{ section.roman }}.</span>
            <h3>{{ section.title }}</h3>
            <small class="section-question-range">{{ sectionQuestionRange(section) }}</small>
            <small v-if="section.sourceSectionTitle && section.majorSkill" class="section-skill-name">{{ section.majorSkill.name }}</small>
            <small :class="['section-question-count', { 'section-count-mismatch': section.rangeAudit.mismatch }]">
              {{ section.questions.length }}<template v-if="section.questions.length !== section.rangeAudit.actualCount"> / {{ section.rangeAudit.actualCount }} tổng</template><template v-else-if="section.rangeAudit.mismatch"> / {{ section.rangeAudit.expectedCount }} theo dải</template> câu
            </small>
            <button
              type="button"
              class="section-toggle"
              :aria-expanded="!isSectionCollapsed(section.key)"
              :aria-label="isSectionCollapsed(section.key) ? `Mở rộng ${section.title}` : `Thu gọn ${section.title}`"
              :title="isSectionCollapsed(section.key) ? 'Mở rộng section' : 'Thu gọn section'"
              @click.stop="toggleSection(section.key)"
            >
              <ChevronRight v-if="isSectionCollapsed(section.key)" :size="17" aria-hidden="true" />
              <ChevronDown v-else :size="17" aria-hidden="true" />
            </button>
          </header>
          <div v-if="section.rangeAudit.mismatch" class="section-range-warning" role="status">
            <AlertTriangle :size="16" aria-hidden="true" />
            <div>
              <strong>Dải câu và số câu nhận diện không khớp</strong>
              <p>PDF ghi {{ sectionQuestionRange(section) }} ({{ section.rangeAudit.expectedCount }} câu), nhưng hệ thống chỉ nhận diện {{ section.rangeAudit.actualCount }} câu.</p>
              <p v-if="section.rangeAudit.missingNumbers.length">Không thấy số câu {{ section.rangeAudit.missingNumbers.join(', ') }} trong dữ liệu nhận diện.</p>
              <p v-else>Đối chiếu PDF để xác định câu bị bỏ sót hay dải section được đánh chưa đúng.</p>
            </div>
          </div>
          <div v-if="section.sectionInstructions || section.stimulusContent" class="section-guidance">
            <p v-if="section.sectionInstructions">{{ section.sectionInstructions }}</p>
            <div v-if="section.stimulusContent" class="section-stimulus"><strong>Nội dung dùng chung</strong><p>{{ section.stimulusContent }}</p></div>
          </div>
          <details class="section-context-editor">
            <summary>{{ section.sectionInstructions || section.stimulusContent ? 'Chỉnh sửa hướng dẫn và nội dung chung' : 'Thêm hướng dẫn hoặc nội dung chung' }}</summary>
            <div class="section-range-editor">
              <label>Câu bắt đầu<input :value="section.sourceQuestionStart" inputmode="numeric" placeholder="1" @input="updateSectionText(section, 'sourceSectionQuestionStart', $event.target.value)" /></label>
              <label>Câu kết thúc<input :value="section.sourceQuestionEnd" inputmode="numeric" placeholder="10" @input="updateSectionText(section, 'sourceSectionQuestionEnd', $event.target.value)" /></label>
            </div>
            <label>Hướng dẫn của phần<textarea :value="section.sectionInstructions" rows="2" placeholder="Ví dụ: Questions 1-4. Complete the sentences below." @input="updateSectionText(section, 'sectionInstructions', $event.target.value)" /></label>
            <label>Nội dung dùng chung<textarea :value="section.stimulusContent" rows="4" placeholder="Đoạn văn hoặc tình huống được các câu trong phần này sử dụng." @input="updateSectionText(section, 'stimulusContent', $event.target.value)" /></label>
          </details>
          <div class="section-images">
            <figure v-for="resource in sectionResourcesFor(section, 'IMAGE')" :key="resource.resourceId" class="section-image-resource">
              <img v-if="resourceUrls[resource.resourceId]" :src="resourceUrls[resource.resourceId]" :alt="resource.fileName" />
              <figcaption>{{ resource.fileName }}</figcaption>
            </figure>
            <div class="section-image-upload">
              <label class="image-file-pick"><ImageIcon :size="15" /><span>{{ sectionImageFiles[section.key]?.name || 'Thêm hình cho mục này' }}</span><input type="file" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp" @change="chooseSectionImage($event, section.key)" /></label>
              <button type="button" :disabled="!sectionImageFiles[section.key] || uploadingImageKey === section.key" @click="uploadSectionImage(section)">
                <LoaderCircle v-if="uploadingImageKey === section.key" class="spin" :size="15" />
                <Upload v-else :size="15" />
                {{ uploadingImageKey === section.key ? 'Đang tải…' : 'Gắn hình' }}
              </button>
            </div>
          </div>
          <div v-if="isListeningSection(section)" class="section-audio">
            <div class="section-audio-heading">
              <div><strong>File nghe cho mục này</strong><small>Dùng chung cho {{ section.questions.length }} câu Listening</small></div>
              <span v-if="!sectionResourcesFor(section.key, 'AUDIO').length" class="audio-empty-state">Chưa có audio</span>
            </div>
            <div v-for="resource in sectionResourcesFor(section, 'AUDIO')" :key="resource.resourceId" class="section-audio-resource">
              <div class="section-audio-file"><FileText :size="16" /><span>{{ resource.fileName }}</span></div>
              <audio v-if="resourceUrls[resource.resourceId]" controls preload="metadata" :src="resourceUrls[resource.resourceId]" :aria-label="`Audio ${section.title}`" />
              <small v-else class="audio-loading">Đang tải audio…</small>
            </div>
            <div class="section-audio-upload">
              <label class="audio-file-pick"><Upload :size="15" /><span>{{ sectionAudioFiles[section.key]?.name || 'Chọn file nghe' }}</span><input type="file" accept="audio/mpeg,audio/wav,audio/mp4,audio/ogg,audio/aac,.mp3,.wav,.m4a,.ogg,.aac" @change="chooseSectionAudio($event, section.key)" /></label>
              <button type="button" :disabled="!sectionAudioFiles[section.key] || uploadingAudioKey === section.key" @click="uploadSectionAudio(section)">
                <LoaderCircle v-if="uploadingAudioKey === section.key" class="spin" :size="15" />
                <Upload v-else :size="15" />
                {{ uploadingAudioKey === section.key ? 'Đang tải…' : 'Gắn audio vào mục' }}
              </button>
            </div>
          </div>
          <article v-for="entry in section.questions" :key="entry.index" class="question-editor" :class="{ 'needs-answer': !String(entry.question.correctAnswer ?? '').trim() }" @vue:mounted="normalizeAnswer(entry.question)">
            <header>
              <span class="question-index" :title="entry.question.sourceQuestionNumber ? `Số câu trong PDF: ${entry.question.sourceQuestionNumber}` : 'Thứ tự câu được nhận diện'">{{ questionDisplayNumber(entry.question, entry.index) }}</span>
              <span class="answer-state" :class="{ 'is-missing': !String(entry.question.correctAnswer ?? '').trim() }">{{ String(entry.question.correctAnswer ?? '').trim() ? 'Đã có đáp án' : 'Thiếu đáp án' }}</span>
              <label>Kỹ năng / Môn
                <select v-model="entry.question.topicId">
                  <option v-if="!entry.question.topicId && !form.topicId" :value="null" disabled>Chọn kỹ năng</option>
                  <template v-for="group in reviewTopicGroups" :key="group.id">
                    <option v-if="group.topics.length === 1 && group.topics[0].topicId === group.id" :value="group.id">
                      {{ group.name }}
                    </option>
                    <optgroup v-else :label="group.name">
                      <option v-for="topic in group.topics" :key="topic.topicId" :value="topic.topicId">
                        {{ topicPath(topic, reviewTopics) }}
                      </option>
                    </optgroup>
                  </template>
                </select>
              </label>
              <label>Loại câu<select v-model="entry.question.questionType" @change="changeQuestionType(entry.question)"><option value="MULTIPLE_CHOICE">Trắc nghiệm</option><option value="TRUE_FALSE">Đúng / sai</option><option value="FILL_BLANK">Điền khuyết</option><option value="ESSAY">Tự luận</option></select></label>
              <label>Độ khó<select v-model="entry.question.difficulty"><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label>
            </header>
            <div v-if="entry.question.questionType === 'FILL_BLANK'" class="question-form-preview" :aria-label="`Bản dựng câu ${questionDisplayNumber(entry.question, entry.index)}`">
              <template v-for="(part, partIndex) in blankContentParts(entry.question)" :key="partIndex">
                <span v-if="part.blankNumber !== undefined" class="inline-answer-slot"><small>{{ part.blankNumber }}</small><span>................</span></span>
                <span v-else>{{ part.text }}</span>
              </template>
            </div>
            <label class="editor-label">Nội dung<textarea v-model.trim="entry.question.content" rows="3" /></label>
            <div v-if="entry.question.questionType === 'MULTIPLE_CHOICE'" class="options-editor">
              <div class="editor-subheading"><strong>Lựa chọn</strong><button type="button" @click="addOption(entry.question)">Thêm lựa chọn</button></div>
              <div v-for="(option, optionIndex) in entry.question.options" :key="optionIndex" class="option-row"><b>{{ option.label }}</b><input v-model.trim="option.text" /><button type="button" :aria-label="`Xóa lựa chọn ${option.label}`" @click="removeOption(entry.question, optionIndex)">×</button></div>
              <label class="editor-label answer-field">Đáp án đúng<select v-model="entry.question.correctAnswer"><option v-for="option in entry.question.options" :key="option.label" :value="option.label">{{ option.label }} · {{ option.text }}</option></select></label>
            </div>
            <label v-else-if="entry.question.questionType === 'FILL_BLANK'" class="editor-label answer-field">Đáp án<input v-model.trim="entry.question.correctAnswer" type="text" autocomplete="off" /></label>
            <label v-else class="editor-label answer-field">Đáp án<textarea v-model.trim="entry.question.correctAnswer" rows="2" /></label>
            <details class="editor-explanation"><summary>Giải thích</summary><textarea v-model.trim="entry.question.explanation" rows="2" /></details>
          </article>
        </section>
        <p v-if="!visibleReviewSections.length" class="review-no-results">Không có câu hỏi khớp với bộ lọc.</p>
        <footer class="review-actions">
          <button v-if="isAdmin || selected.canDelete" type="button" class="delete-review-btn" :disabled="saving || deletingImportId === selected.importId" @click="deleteImport(selected)">
            <LoaderCircle v-if="deletingImportId === selected.importId" class="spin" :size="16" />
            <Trash2 v-else :size="16" />
            Xóa bộ đề này
          </button>
          <div class="review-primary-actions">
            <button type="button" class="save-button" :disabled="saving" @click="saveReview"><LoaderCircle v-if="saving" class="spin" :size="16" /><Save v-else :size="16" />Lưu chỉnh sửa</button>
            <button type="button" class="confirm-button" :disabled="saving" @click="confirmImport"><Check :size="16" />Xác nhận chính thức</button>
          </div>
        </footer>
      </section>
      <section v-else class="review-empty"><FileText :size="26" /><h2>Chọn một bộ đề</h2><p>Mở bộ đề để rà từng câu và xác nhận đáp án.</p></section>
    </div>

    <!-- Dialog xác nhận xóa bộ đề trực tiếp -->
    <div v-if="itemToDelete" class="delete-confirm-backdrop" @click.self="cancelDelete">
      <div class="delete-confirm-card" role="alertdialog" aria-modal="true">
        <div class="delete-confirm-content">
          <div class="delete-confirm-icon">
            <Trash2 :size="22" />
          </div>
          <div class="delete-confirm-text">
            <h3>Xóa bộ đề?</h3>
            <p>Bộ đề "<strong>{{ itemToDelete.title }}</strong>" sẽ bị xóa vĩnh viễn khỏi hàng chờ duyệt. Thao tác này không thể hoàn tác.</p>
          </div>
        </div>
        <div class="delete-confirm-actions">
          <button type="button" class="btn-cancel-delete" :disabled="deletingImportId === itemToDelete.importId" @click="cancelDelete">
            Hủy
          </button>
          <button type="button" class="btn-confirm-delete" :disabled="deletingImportId === itemToDelete.importId" @click="confirmDeleteImport(itemToDelete)">
            <LoaderCircle v-if="deletingImportId === itemToDelete.importId" class="spin" :size="15" />
            <Trash2 v-else :size="15" />
            <span>{{ deletingImportId === itemToDelete.importId ? 'Đang xóa…' : 'Xóa vĩnh viễn' }}</span>
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.exam-import-page { width: 85%; max-width: 1760px; min-width: 320px; margin: 0 auto; padding: 28px 0 56px; color: #17202d; }
.page-heading, .page-heading > div, .section-heading, .list-heading, .review-heading, .review-actions, .form-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-heading { margin-bottom: 24px; }.page-heading > div { justify-content: flex-start; }.back-button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid #dce2e9; border-radius: 6px; background: white; cursor: pointer; }
.eyebrow { margin: 0 0 5px; color: #547386; font-size: 11px; font-weight: 700; }.page-heading h1 { margin: 0; font-size: 25px; }.queue-count,.status-pill { color: #435565; font-size: 12px; }
.upload-section { margin-bottom: 26px; border-bottom: 1px solid #dce2e9; }.upload-form { width: 100%; max-width: 100%; padding-bottom: 24px; }.section-heading { margin-bottom: 18px; }.section-heading h2,.list-heading h2,.review-heading h2 { margin: 0; font-size: 17px; }.section-heading p,.list-heading p,.review-heading > div > p:last-child { margin: 5px 0 0; color: #64748b; font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr 1fr; gap: 16px; }.form-grid label,.editor-label,.file-field { display: grid; gap: 7px; color: #3a4757; font-size: 12px; font-weight: 600; }.form-grid input,.form-grid select,.editor-label textarea,.editor-label select,.option-row input,.question-editor select { min-width: 0; min-height: 40px; padding: 8px 12px; border: 1px solid #cbd5df; border-radius: 6px; background: white; color: #17202d; font: inherit; font-weight: 400; }
.answer-in-exam { display: flex; width: fit-content; align-items: center; gap: 8px; margin-top: 14px; color: #344256; font-size: 13px; }.answer-in-exam input { width: 16px; height: 16px; accent-color: #126b68; }
.analysis-progress { display: grid; gap: 7px; margin-top: 16px; color: #344256; font-size: 12px; }.analysis-progress > div { display: flex; justify-content: space-between; gap: 12px; }.analysis-progress progress { width: 100%; height: 8px; accent-color: #126b68; }.analysis-progress small { color: #64748b; }
.live-ocr-preview { display: grid; max-height: 520px; gap: 10px; margin-top: 16px; padding: 14px; overflow-y: auto; border: 1px solid #cfe3dc; border-radius: 8px; background: #f7fbf9; }
.live-ocr-heading { position: sticky; top: -14px; z-index: 1; display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: -14px -14px 2px; padding: 12px 14px; border-bottom: 1px solid #dcebe5; background: #f7fbf9; }
.live-ocr-heading > div { display: grid; gap: 3px; }
.live-ocr-heading strong { color: #145d50; font-size: 13px; }
.live-ocr-heading small,.live-ocr-heading > span { color: #64748b; font-size: 11px; }
.live-ocr-error { margin: 0; padding: 9px 11px; border: 1px solid #fecaca; border-radius: 6px; color: #991b1b; background: #fef2f2; font-size: 12px; line-height: 1.5; }
.live-ocr-question { display: grid; min-width: 0; gap: 6px; padding: 10px 12px; border: 1px solid #dce8e3; border-radius: 6px; background: #fff; }
.live-ocr-question > header { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; color: #64748b; font-size: 11px; }
.live-ocr-question > header strong { color: #126b68; font-size: 13px; }
.live-ocr-question > p { margin: 0; color: #1f2937; font-size: 13px; line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.live-ocr-question ul { display: grid; gap: 3px; margin: 0; padding: 0; color: #475569; font-size: 12px; list-style: none; }
.live-ocr-question li { overflow-wrap: anywhere; }
.live-ocr-question li strong { color: #126b68; }
.live-ocr-question > small { color: #64748b; font-size: 11px; white-space: pre-wrap; }
.answer-key-upload { display: flex; align-items: flex-end; gap: 12px; margin-bottom: 16px; }.answer-key-upload .file-field { flex: 1; }.answer-key-upload button { display: inline-flex; min-height: 42px; align-items: center; gap: 8px; padding: 0 16px; border: 0; border-radius: 6px; background: #126b68; color: white; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }.answer-key-upload button:disabled { opacity: .55; cursor: wait; }
.file-grid { display: grid; width: 100%; max-width: 100%; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 14px; }.file-field { padding: 14px 16px; border: 1px dashed #b9c6d1; border-radius: 6px; }.file-field b { color: #b42318; }.file-field small { color: #64748b; font-weight: 400; }.file-field input { max-width: 100%; font-size: 12px; }
.form-actions { justify-content: flex-end; margin-top: 16px; }.form-actions button,.review-actions button { display: inline-flex; min-height: 42px; align-items: center; gap: 8px; padding: 0 18px; border: 0; border-radius: 6px; background: #126b68; color: white; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }.form-actions button:disabled,.review-actions button:disabled { opacity: .55; cursor: wait; }
.work-area { display: grid; min-height: 420px; grid-template-columns: 320px minmax(0,1fr); gap: 28px; }.review-only { margin-top: 24px; }.import-list { border-right: 1px solid #dce2e9; padding-right: 16px; }.list-heading { margin-bottom: 12px; }.list-heading button { width: 32px; height: 32px; border: 1px solid #dce2e9; border-radius: 5px; background: white; cursor: pointer; }.import-row { display: flex; width: 100%; align-items: flex-start; gap: 10px; padding: 12px 10px; border: 0; border-bottom: 1px solid #e8edf1; border-radius: 6px; background: transparent; color: #425366; text-align: left; cursor: pointer; }.import-row.selected { background: #edf6f5; color: #075e5c; }.import-row span { display: grid; min-width: 0; gap: 5px; }.import-row strong { overflow-wrap: anywhere; color: #1f2937; font-size: 13px; }.import-row small,.empty-list { color: #64748b; font-size: 11px; }.review-panel { min-width: 0; }.review-heading { align-items: flex-start; margin-bottom: 12px; }.review-heading h2 { font-size: 20px; }.review-heading .eyebrow { color: #a34c13; }.status-pill { padding: 5px 8px; border-radius: 4px; background: #fff4e5; color: #8a4b09; white-space: nowrap; }
.import-row-wrap { display: flex; align-items: stretch; gap: 4px; }.import-row-wrap .import-row { flex: 1; min-width: 0; }.delete-import-button { display: grid; width: 36px; flex: 0 0 36px; place-items: center; align-self: center; aspect-ratio: 1; border: 1px solid #dce2e9; border-radius: 5px; background: white; color: #9f2f27; cursor: pointer; }.delete-import-button:hover { background: #fff1f0; }.delete-import-button:disabled { opacity: .55; cursor: wait; }
.source-pdf-details { margin-bottom: 16px; border: 1px solid #dce2e9; border-radius: 6px; }.source-pdf-details summary { padding: 10px 12px; color: #435565; font-size: 12px; cursor: pointer; }.source-pdf-details iframe { display: block; width: 100%; height: min(72vh, 850px); border: 0; border-top: 1px solid #dce2e9; background: #f1f3f5; }
.source-details { margin-bottom: 16px; border: 1px solid #dce2e9; border-radius: 6px; }.source-details summary { padding: 10px 12px; color: #435565; font-size: 12px; cursor: pointer; }.source-details > div { max-height: 260px; overflow: auto; padding: 0 12px 12px; }.source-details h3 { font-size: 12px; }.source-details pre { white-space: pre-wrap; overflow-wrap: anywhere; color: #475569; font-family: inherit; font-size: 12px; line-height: 1.5; }
.question-editor { margin: 0 0 14px; padding: 16px; border: 1px solid #dce2e9; border-radius: 6px; }.question-editor > header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }.question-editor > header > span { color: #126b68; font-size: 13px; font-weight: 700; }.question-editor > header label { display: flex; align-items: center; gap: 7px; color: #64748b; font-size: 11px; }.question-editor select { min-height: 36px; }.editor-label { margin-top: 11px; }.editor-label textarea { resize: vertical; line-height: 1.45; }.editor-subheading { display: flex; align-items: center; justify-content: space-between; margin: 12px 0 8px; color: #3a4757; font-size: 12px; }.editor-subheading button { border: 0; background: transparent; color: #126b68; font-size: 12px; cursor: pointer; }.option-row { display: grid; grid-template-columns: 28px minmax(0,1fr) 30px; gap: 8px; align-items: center; margin-bottom: 7px; }.option-row b { color: #126b68; }.option-row button { height: 32px; border: 0; background: transparent; color: #9b3028; font-size: 20px; cursor: pointer; }.answer-field { width: 100%; max-width: 100%; }.review-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 22px; }.review-primary-actions { display: flex; align-items: center; gap: 12px; }.review-actions .save-button { border: 1px solid #cbd5df; background: white; color: #344256; }.review-actions .confirm-button { background: #126b68; }
.review-heading-actions { display: flex; align-items: center; gap: 10px; }
.delete-header-btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border: 1px solid #fecdd3; border-radius: 6px; background: #fff1f2; color: #be123c; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; }
.delete-header-btn:hover { background: #ffe4e6; border-color: #fda4af; }
.delete-header-btn:disabled { opacity: .55; cursor: wait; }
.delete-review-btn { display: inline-flex; min-height: 42px; align-items: center; gap: 8px; padding: 0 16px; border: 1px solid #fecdd3; border-radius: 6px; background: #fff1f2; color: #be123c; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; }
.delete-review-btn:hover { background: #ffe4e6; border-color: #fda4af; }
.delete-review-btn:disabled { opacity: .55; cursor: wait; }
.question-editor.needs-answer { border-color: #d92d20; box-shadow: inset 3px 0 #d92d20; }
.major-skill-section { margin-bottom: 22px; }.major-skill-heading { display: flex; align-items: baseline; gap: 8px; margin: 0 0 10px; padding: 10px 12px; border-bottom: 1px solid #cbd5df; color: #126b68; }.major-skill-heading h3 { margin: 0; color: #17202d; font-size: 16px; }.major-skill-heading small { margin-left: auto; color: #64748b; font-size: 11px; }
.major-skill-section.is-collapsed > :not(.major-skill-heading) { display: none; }
.section-toggle { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; margin-left: 2px; padding: 0; border: 1px solid #cbd5df; border-radius: 6px; background: #fff; color: #475569; cursor: pointer; }
.section-toggle:hover { border-color: #94a3b8; background: #f8fafc; color: #0f172a; }
.section-toggle:focus-visible { outline: 2px solid #2563eb; outline-offset: 2px; }
.review-empty { display: grid; align-content: center; justify-items: center; min-height: 300px; color: #64748b; text-align: center; }.review-empty h2 { margin: 12px 0 0; color: #344256; font-size: 17px; }.review-empty p { font-size: 13px; }.feedback { padding: 12px 14px; border-radius: 6px; font-size: 13px; }.feedback.error { background: #fff1f0; color: #9f2f27; }.feedback.success { background: #eef8f2; color: #1e6a46; }.spin { animation: spin 1s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }
.no-permission { padding: 28px 0; border-top: 1px solid #dce2e9; color: #64748b; }.no-permission h2 { margin: 0; color: #344256; font-size: 17px; }.no-permission p { font-size: 13px; }
.delete-confirm-backdrop { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 16px; background: rgba(15, 23, 42, 0.48); backdrop-filter: blur(2px); }
.delete-confirm-card { width: min(440px, calc(100vw - 32px)); border: 1px solid #fee2e2; border-radius: 12px; background: white; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.12), 0 10px 10px -5px rgba(0, 0, 0, 0.04); overflow: hidden; }
.delete-confirm-content { display: flex; align-items: flex-start; gap: 14px; padding: 22px; }
.delete-confirm-icon { display: grid; width: 42px; height: 42px; flex-shrink: 0; place-items: center; border-radius: 10px; background: #fee2e2; color: #dc2626; }
.delete-confirm-text h3 { margin: 0 0 6px; color: #0f172a; font-size: 17px; font-weight: 600; }
.delete-confirm-text p { margin: 0; color: #475569; font-size: 13.5px; line-height: 1.5; }
.delete-confirm-text strong { color: #0f172a; word-break: break-word; }
.delete-confirm-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 14px 22px; background: #f8fafc; border-top: 1px solid #f1f5f9; }
.btn-cancel-delete { min-height: 38px; padding: 0 16px; border: 1px solid #cbd5e1; border-radius: 6px; background: white; color: #475569; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.15s ease; }
.btn-cancel-delete:hover:not(:disabled) { background: #f1f5f9; }
.btn-confirm-delete { display: inline-flex; min-height: 38px; align-items: center; gap: 8px; padding: 0 16px; border: 0; border-radius: 6px; background: #dc2626; color: white; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.15s ease; }
.btn-confirm-delete:hover:not(:disabled) { background: #b91c1c; }
.btn-cancel-delete:disabled, .btn-confirm-delete:disabled { opacity: 0.6; cursor: wait; }
@media (max-width: 800px) { .exam-import-page { width: 95%; padding: 20px 0 40px; }.form-grid { grid-template-columns: 1fr; }.work-area { grid-template-columns: 1fr; }.import-list { border-right: 0; border-bottom: 1px solid #dce2e9; padding: 0 0 14px; }.file-grid { grid-template-columns: 1fr; }.answer-key-upload { align-items: stretch; flex-direction: column; }.answer-key-upload button { justify-content: center; }.question-editor > header { flex-wrap: wrap; }.question-editor > header label { flex: 1 1 140px; justify-content: space-between; }.review-actions { flex-wrap: wrap; }.review-actions button { flex: 1; justify-content: center; } }
.review-panel.has-source-pdf { display: grid; grid-template-columns: minmax(0, .9fr) minmax(0, 1.1fr); align-items: start; gap: 0 18px; }
.review-heading,.review-panel > .feedback { grid-column: 1 / -1; }
.source-pdf-details,.source-details { grid-column: 1; }
.source-pdf-details { position: sticky; top: 12px; }
.answer-key-upload,.major-skill-section,.review-panel > .review-actions { grid-column: 2; }
@media (max-width: 800px) {
  .review-panel.has-source-pdf { grid-template-columns: minmax(0, 1fr); }
  .review-heading,.review-panel > .feedback,.source-pdf-details,.source-details,.answer-key-upload,.major-skill-section,.review-panel > .review-actions { grid-column: 1; }
  .source-pdf-details { position: static; }
}
.review-tools { grid-column: 2; display: grid; gap: 12px; margin: 0 0 16px; padding: 14px 16px; border: 1px solid #dce2e9; border-radius: 6px; background: #fff; }
.review-progress { display: grid; grid-template-columns: auto minmax(100px,1fr); align-items: center; gap: 12px; }
.review-progress > div { display: grid; grid-template-columns: auto auto; align-items: baseline; column-gap: 7px; color: #435565; font-size: 12px; }
.review-progress strong { color: #176b5d; font-size: 15px; }
.review-progress small { grid-column: 1 / -1; color: #9a6700; }
.review-progress progress { width: 100%; height: 8px; accent-color: #16836f; }
.review-filters { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.question-search { display: flex; min-width: 160px; max-width: 420px; flex: 1; align-items: center; gap: 8px; padding: 0 10px; border: 1px solid #cbd5df; border-radius: 5px; color: #64748b; }
.question-search input { width: 100%; min-width: 0; height: 36px; border: 0; outline: 0; background: transparent; color: #17202d; font: inherit; font-size: 12px; }
.unanswered-filter { display: inline-flex; align-items: center; gap: 7px; color: #435565; font-size: 12px; white-space: nowrap; }
.unanswered-filter input { width: 16px; height: 16px; accent-color: #16836f; }
.question-editor { display: grid; grid-template-columns: minmax(0,1.6fr) minmax(180px,.8fr); align-items: start; gap: 10px 14px; margin-bottom: 9px; padding: 12px 14px; border-radius: 6px; }
.question-editor > header { grid-column: 1 / -1; flex-wrap: wrap; gap: 8px 12px; margin: 0; padding-bottom: 9px; border-bottom: 1px solid #e8edf1; }
.question-editor > header > label { flex: initial; justify-content: flex-start; }
.question-editor > header label { font-size: 10px; }
.question-index { color: #176b5d !important; }
.source-question-number { color: #64748b; font-size: 11px; }
.answer-state { margin-left: auto; padding: 3px 7px; border-radius: 4px; background: #eaf5f0; color: #176b5d; font-size: 10px; font-weight: 600; }
.answer-state.is-missing { background: #fff5dc; color: #946200; }
.question-editor.needs-answer { border-color: #e8c878; background: #fffdf7; box-shadow: inset 3px 0 #e2b94f; }
.question-editor > .editor-label:first-of-type { grid-column: 1; }
.question-editor > .answer-field { grid-column: 2; align-self: start; }
.question-editor > .editor-label textarea { height: 54px; min-height: 42px; padding: 8px 10px; font-size: 12px; }
.question-editor > .editor-label input,.question-editor > .editor-label select { min-width: 0; min-height: 38px; padding: 7px 10px; border: 1px solid #cbd5df; border-radius: 5px; background: #fff; color: #17202d; font: inherit; font-size: 12px; font-weight: 400; }
.question-editor > .editor-label { gap: 5px; margin-top: 0; font-size: 11px; }
.question-editor > .options-editor { grid-column: 1 / -1; display: grid; grid-template-columns: minmax(0,1fr) minmax(180px,.65fr); gap: 6px 14px; }
.review-no-results { grid-column: 2; padding: 18px; border: 1px dashed #cbd5df; border-radius: 6px; color: #64748b; font-size: 12px; text-align: center; }
.question-editor .editor-subheading { grid-column: 1 / -1; margin: 0; }
.question-editor .option-row { grid-column: 1; }
.question-editor .options-editor .answer-field { grid-column: 2; grid-row: 2 / span 8; align-self: start; }
.editor-explanation { grid-column: 1 / -1; border-top: 1px solid #e8edf1; }
.editor-explanation summary { width: fit-content; padding-top: 8px; color: #64748b; font-size: 11px; cursor: pointer; }
.editor-explanation textarea { width: 100%; min-height: 44px; margin-top: 6px; padding: 8px 10px; border: 1px solid #cbd5df; border-radius: 5px; font: inherit; font-size: 12px; resize: vertical; }
.section-audio { grid-column: 2; display: grid; gap: 10px; margin: 0 0 12px; padding: 12px 14px; border-left: 3px solid #16836f; background: #f1f8f5; }
.section-audio-heading,.section-audio-resource,.section-audio-upload { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-audio-heading > div { display: grid; gap: 3px; color: #24483f; font-size: 12px; }
.section-audio-heading small,.audio-loading { color: #64748b; font-size: 11px; }
.audio-empty-state { color: #946200; font-size: 11px; }
.section-audio-file { display: flex; min-width: 0; align-items: center; gap: 7px; color: #435565; font-size: 11px; }
.section-audio-file span { overflow-wrap: anywhere; }
.section-audio-resource audio { width: min(100%, 360px); height: 36px; }
.section-audio-upload { justify-content: flex-start; flex-wrap: wrap; }
.audio-file-pick { display: flex; min-width: 0; min-height: 34px; align-items: center; gap: 7px; padding: 0 9px; border: 1px solid #c8d9d3; border-radius: 5px; background: #fff; color: #315b50; font-size: 11px; cursor: pointer; }
.audio-file-pick span { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.audio-file-pick input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.section-audio-upload button { display: inline-flex; min-height: 34px; align-items: center; gap: 7px; padding: 0 11px; border: 0; border-radius: 5px; background: #176b5d; color: #fff; font: inherit; font-size: 11px; font-weight: 600; cursor: pointer; }
.section-audio-upload button:disabled { opacity: .5; cursor: not-allowed; }
.section-images { grid-column: 2; display: flex; align-items: flex-start; gap: 10px; margin: 0 0 12px; overflow-x: auto; }
.section-image-resource { width: 168px; flex: 0 0 168px; margin: 0; overflow: hidden; border: 1px solid #dce2e9; border-radius: 5px; background: #fff; }
.section-image-resource img { display: block; width: 100%; max-height: 128px; object-fit: contain; background: #f4f6f7; }
.section-image-resource figcaption { overflow: hidden; padding: 5px 7px; color: #64748b; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.section-image-upload { display: grid; min-width: 160px; gap: 6px; }
.image-file-pick { display: flex; min-height: 34px; align-items: center; gap: 7px; padding: 0 9px; border: 1px solid #cbd5df; border-radius: 5px; background: #fff; color: #435565; font-size: 11px; cursor: pointer; }
.image-file-pick span { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.image-file-pick input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.section-image-upload button { display: inline-flex; min-height: 32px; width: fit-content; align-items: center; gap: 6px; padding: 0 9px; border: 1px solid #cbd5df; border-radius: 5px; background: #fff; color: #435565; font: inherit; font-size: 11px; cursor: pointer; }
.section-image-upload button:disabled { opacity: .5; cursor: not-allowed; }
.section-skill-name { color: #64748b !important; font-size: 11px !important; font-weight: 500 !important; }
.section-question-count { margin-left: auto; }
.section-question-count.section-count-mismatch { padding: 2px 7px; border: 1px solid #fcd34d; border-radius: 999px; background: #fffbeb; color: #92400e; font-weight: 700; }
.section-range-warning { display: flex; align-items: flex-start; gap: 9px; margin: 0 0 12px; padding: 10px 12px; border: 1px solid #fcd34d; border-radius: 6px; background: #fffbeb; color: #854d0e; font-size: 12px; line-height: 1.5; }
.section-range-warning > svg { flex: 0 0 auto; margin-top: 2px; }
.section-range-warning p { margin: 3px 0 0; }
.section-range-warning strong { color: #78350f; }
.section-guidance { grid-column: 2; margin: 0 0 12px; padding: 12px 15px; border-left: 3px solid #91b8ad; background: #f7faf9; color: #344256; font-size: 12px; line-height: 1.65; white-space: pre-line; }
.section-guidance > p { margin: 0; }.section-stimulus { margin-top: 9px; padding-top: 9px; border-top: 1px solid #dce8e3; }.section-stimulus p { margin: 4px 0 0; white-space: pre-wrap; }
.section-context-editor { grid-column: 2; margin: 0 0 12px; border-bottom: 1px solid #e3e8ec; }
.section-context-editor summary { width: fit-content; padding: 0 0 7px; color: #64748b; font-size: 11px; cursor: pointer; }
.section-context-editor label { display: grid; gap: 5px; margin: 0 0 9px; color: #435565; font-size: 11px; }
.section-context-editor textarea { width: 100%; min-height: 42px; padding: 8px 10px; border: 1px solid #cbd5df; border-radius: 5px; font: inherit; font-size: 12px; resize: vertical; }
.section-range-editor { display: grid; grid-template-columns: repeat(2,minmax(90px,120px)); gap: 10px; }
.section-range-editor input { min-height: 34px; padding: 6px 9px; border: 1px solid #cbd5df; border-radius: 5px; font: inherit; font-size: 12px; }
.major-skill-heading .section-question-range { margin-left: 0; color: #176b5d; font-size: 11px; font-weight: 600; }
.question-form-preview { grid-column: 1 / -1; padding: 12px 15px; background: #fff; color: #26323c; font-family: Georgia, 'Times New Roman', serif; font-size: 15px; line-height: 2; white-space: pre-wrap; }
.inline-answer-slot { display: inline-flex; min-width: 100px; align-items: baseline; justify-content: center; gap: 7px; margin: 0 4px; border-bottom: 1px dotted #475569; color: #64748b; line-height: 1.2; vertical-align: baseline; }
.inline-answer-slot small { color: #176b5d; font-family: Arial, sans-serif; font-size: 10px; font-weight: 700; }
@media (max-width: 800px) {
  .review-tools { grid-column: 1; }
  .section-range-editor { grid-template-columns: repeat(2,minmax(0,1fr)); }
  .review-progress { grid-template-columns: 1fr; }
  .review-filters { align-items: stretch; flex-direction: column; }
  .question-editor { grid-template-columns: minmax(0,1fr); }
  .question-editor > header,.question-editor > .editor-label:first-of-type,.question-editor > .answer-field,.question-editor > .options-editor,.editor-explanation,.section-audio,.section-images,.section-guidance,.section-context-editor { grid-column: 1; }
  .question-editor > .options-editor { grid-template-columns: minmax(0,1fr); }
  .question-form-preview { grid-column: 1; }
  .review-no-results { grid-column: 1; }
  .question-editor .options-editor .answer-field { grid-column: 1; grid-row: auto; }
  .section-audio-heading,.section-audio-resource { align-items: flex-start; flex-direction: column; }
  .section-audio-resource audio { width: 100%; }
}
</style>
