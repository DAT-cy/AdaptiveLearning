<script setup>
import { computed, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Check, CheckCircle2, Clock3, Flag, GraduationCap, LoaderCircle, ListChecks, Plus, Search, Trash2, X } from 'lucide-vue-next'
import { adminService } from '@/services/adminService'
import { useConfirm } from '@/components/ui/overlay'

const props = defineProps({
  open: { type: Boolean, default: false },
  programs: { type: Array, default: () => [] },
  topics: { type: Array, default: () => [] },
  draftKey: { type: String, default: 'admin' },
})
const emit = defineEmits(['close', 'saved'])
const { confirm } = useConfirm()
const windowRef = ref(null)
let returnFocusElement = null
const stage = ref('builder')
const previewMode = ref('exam')
const currentQuestionNumber = ref(1)
const simulationAnswers = reactive({})
const flaggedQuestionIds = ref(new Set())
const draftQuestionSelections = ref([])
const draftSavedAt = ref(null)
const restoringDraft = ref(false)
const remainingSeconds = ref(0)
let timerInterval

const form = reactive({
  certificateId: '',
  topicId: '',
  title: '',
  description: '',
  durationMinutes: 60,
  targetScoreMin: 0,
  targetScoreMax: 0,
  passingScore: 5,
  sections: [],
})
const saving = ref(false)
const previewLoading = ref(false)
const error = ref('')
const templateId = ref(null)
const previewSections = ref([])
const previewSearch = reactive({})
const previewTotalPoints = computed(() => previewSections.value.reduce(
  (total, section) => total + section.selectedQuestionIds.length * Number(section.form.pointsPerQuestion || 0),
  0
))
const simulationQuestions = computed(() => previewSections.value.flatMap((section, sectionIndex) =>
  section.selectedQuestionIds.map((questionId) => ({
    question: section.candidates.find((candidate) => candidate.questionId === questionId),
    section,
    sectionIndex,
  })).filter((item) => item.question)
).map((item, index) => ({ ...item, number: index + 1 })))
const activeSimulationQuestion = computed(() => simulationQuestions.value.find((item) => item.number === currentQuestionNumber.value) || null)
const answeredQuestionCount = computed(() => simulationQuestions.value.filter((item) => simulationAnswers[item.question.questionId]).length)
const remainingTimeLabel = computed(() => `${String(Math.floor(remainingSeconds.value / 60)).padStart(2, '0')}:${String(remainingSeconds.value % 60).padStart(2, '0')}`)
onUnmounted(() => {
  persistDraft()
  stopSimulationTimer()
})

function startSimulationTimer() {
  clearInterval(timerInterval)
  remainingSeconds.value = Math.max(60, Number(form.durationMinutes) * 60)
  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) remainingSeconds.value -= 1
    else clearInterval(timerInterval)
  }, 1000)
}
function stopSimulationTimer() {
  clearInterval(timerInterval)
  timerInterval = undefined
}
function toggleFlag(questionId) {
  const next = new Set(flaggedQuestionIds.value)
  if (next.has(questionId)) next.delete(questionId)
  else next.add(questionId)
  flaggedQuestionIds.value = next
}
function moveToQuestion(number) {
  if (number < 1 || number > simulationQuestions.value.length) return
  currentQuestionNumber.value = number
}

function storageKey() {
  return `adaptive-learning.exam-builder-draft.v1:${String(props.draftKey || 'admin').toLowerCase()}`
}

function persistDraft() {
  if (!props.open || restoringDraft.value) return
  const selectedQuestionIds = previewSections.value.length
    ? previewSections.value.map((section) => section.selectedQuestionIds)
    : draftQuestionSelections.value
  try {
    const savedAt = new Date()
    localStorage.setItem(storageKey(), JSON.stringify({
      version: 1,
      form: JSON.parse(JSON.stringify(form)),
      selectedQuestionIds,
      savedAt: savedAt.toISOString(),
    }))
    draftSavedAt.value = savedAt
  } catch {
    draftSavedAt.value = null
  }
}

function clearDraft() {
  try { localStorage.removeItem(storageKey()) } catch { /* Storage may be unavailable. */ }
  draftSavedAt.value = null
  draftQuestionSelections.value = []
}

function formatDraftSavedAt() {
  if (!draftSavedAt.value) return ''
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(draftSavedAt.value)
}

const availableTopics = computed(() => props.topics.filter(
  (topic) => String(topic.certificateId) === String(form.certificateId)
))
const selectedTopic = computed(() => availableTopics.value.find(
  (topic) => String(topic.topicId) === String(form.topicId)
))
const hasTargetScale = computed(() => Boolean(
  selectedTopic.value?.scoreUnit && selectedTopic.value.scoreStep > 0
  && selectedTopic.value.scoreMin != null && selectedTopic.value.scoreMax != null
))
const maxPoints = computed(() => form.sections.reduce(
  (total, section) => total + Number(section.questionCount || 0) * Number(section.pointsPerQuestion || 0),
  0
))
const scoreOptions = computed(() => {
  const topic = selectedTopic.value
  if (!topic || topic.scoreUnit !== 'band' || topic.scoreStep <= 0) return []
  const count = Math.floor((topic.scoreMax - topic.scoreMin) / topic.scoreStep + 1e-8)
  return Array.from({ length: count + 1 }, (_, index) =>
    Number((topic.scoreMin + index * topic.scoreStep).toFixed(2))
  )
})

function defaultScoreRange(topic) {
  if (!topic || topic.scoreMin == null || topic.scoreMax == null || topic.scoreStep <= 0) {
    return { min: 0, max: 0 }
  }
  const steps = Math.floor((topic.scoreMax - topic.scoreMin) / topic.scoreStep + 1e-8)
  return {
    min: Number((topic.scoreMin + Math.floor(steps * 0.5) * topic.scoreStep).toFixed(2)),
    max: Number((topic.scoreMin + Math.floor(steps * 0.75) * topic.scoreStep).toFixed(2)),
  }
}

function resetForm() {
  stopSimulationTimer()
  Object.keys(simulationAnswers).forEach((key) => delete simulationAnswers[key])
  flaggedQuestionIds.value = new Set()
  draftQuestionSelections.value = []
  previewMode.value = 'exam'
  const certificateId = String(props.programs[0]?.certificateId || '')
  const topic = props.topics.find((item) => String(item.certificateId) === certificateId)
  const range = defaultScoreRange(topic)
  Object.assign(form, {
    certificateId,
    topicId: String(topic?.topicId || ''),
    title: '',
    description: '',
    durationMinutes: 60,
    targetScoreMin: range.min,
    targetScoreMax: range.max,
    passingScore: 5,
    sections: [{ name: 'Phần 1', questionType: 'MULTIPLE_CHOICE', difficulty: 'MEDIUM', questionCount: 10, pointsPerQuestion: 1 }],
  })
  stage.value = 'builder'
  templateId.value = null
  previewSections.value = []
  Object.keys(previewSearch).forEach((key) => delete previewSearch[key])
  error.value = ''
  draftSavedAt.value = null
}

async function initializeDialog() {
  stopSimulationTimer()
  returnFocusElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  restoringDraft.value = true
  let draft = null
  try {
    const stored = localStorage.getItem(storageKey())
    const parsed = stored ? JSON.parse(stored) : null
    if (parsed?.version === 1 && parsed.form && Array.isArray(parsed.form.sections)) draft = parsed
  } catch {
    draft = null
  }

  if (!draft) {
    resetForm()
    restoringDraft.value = false
    await focusBuilder()
    return
  }

  const restoredForm = JSON.parse(JSON.stringify(draft.form))
  Object.assign(form, restoredForm)
  stage.value = 'builder'
  templateId.value = null
  previewSections.value = []
  draftQuestionSelections.value = Array.isArray(draft.selectedQuestionIds) ? draft.selectedQuestionIds : []
  Object.keys(previewSearch).forEach((key) => delete previewSearch[key])
  Object.keys(simulationAnswers).forEach((key) => delete simulationAnswers[key])
  flaggedQuestionIds.value = new Set()
  previewMode.value = 'exam'
  error.value = ''
  draftSavedAt.value = draft.savedAt ? new Date(draft.savedAt) : null

  await nextTick()
  Object.assign(form, restoredForm)
  await nextTick()
  restoringDraft.value = false
  await focusBuilder()
}

async function focusBuilder() {
  await nextTick()
  windowRef.value?.querySelector('input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])')?.focus()
}

function handleDialogKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault()
    void closeDialog()
    return
  }
  if (event.key !== 'Tab' || !windowRef.value) return
  const focusable = [...windowRef.value.querySelectorAll(
    'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  )]
  if (!focusable.length) {
    event.preventDefault()
    windowRef.value.focus()
    return
  }
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

function addSection() {
  form.sections.push({
    name: `Phần ${form.sections.length + 1}`,
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    questionCount: 10,
    pointsPerQuestion: 1,
  })
  draftQuestionSelections.value.push([])
}

function removeSection(index) {
  if (form.sections.length <= 1) return
  form.sections.splice(index, 1)
  draftQuestionSelections.value.splice(index, 1)
  form.sections.forEach((section, sectionIndex) => {
    if (/^Phần \d+$/.test(section.name)) section.name = `Phần ${sectionIndex + 1}`
  })
}

function moveSection(index, direction) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.sections.length) return
  const [section] = form.sections.splice(index, 1)
  form.sections.splice(targetIndex, 0, section)
  if (draftQuestionSelections.value.length) {
    const [selections] = draftQuestionSelections.value.splice(index, 1)
    draftQuestionSelections.value.splice(targetIndex, 0, selections || [])
  }
}

watch(() => props.open, (open) => {
  if (open) void initializeDialog()
})

watch([form, stage, previewSections, draftQuestionSelections], persistDraft, { deep: true })

watch(() => form.certificateId, (certificateId, previous) => {
  if (certificateId === previous) return
  const topic = props.topics.find((item) => String(item.certificateId) === String(certificateId))
  form.topicId = String(topic?.topicId || '')
})

watch(selectedTopic, (topic) => {
  const range = defaultScoreRange(topic)
  form.targetScoreMin = range.min
  form.targetScoreMax = range.max
})

watch(maxPoints, (value) => {
  if (form.passingScore > value) form.passingScore = value
})

async function createTemplate() {
  const template = await adminService.createExamTemplate({
      certificateId: Number(form.certificateId),
      topicId: Number(form.topicId),
      title: form.title.trim(),
      description: form.description.trim(),
      durationMinutes: Number(form.durationMinutes),
      targetScoreMin: Number(form.targetScoreMin),
      targetScoreMax: Number(form.targetScoreMax),
      passingScore: Number(form.passingScore),
      sections: form.sections.map((section) => ({
        topicId: Number(form.topicId),
        name: section.name.trim(),
        questionType: section.questionType,
        questionCount: Number(section.questionCount),
        pointsPerQuestion: Number(section.pointsPerQuestion),
      })),
    })
  templateId.value = template.templateId
}

function asList(response) {
  if (Array.isArray(response)) return response
  return response?.data || response?.content || response?.items || []
}

async function deactivateDraftTemplate() {
  if (!templateId.value) return
  const id = templateId.value
  templateId.value = null
  try {
    await adminService.deactivateExamTemplate(id)
  } catch {
    // Preserve the current preview or generation error if cleanup fails.
  }
}

async function loadPreview() {
  if (previewLoading.value || saving.value) return
  error.value = ''
  if (!selectedTopic.value) {
    error.value = 'Chọn kỳ thi và kỹ năng hoặc môn thi trước khi xem trước.'
    return
  }
  if (!hasTargetScale.value) {
    error.value = 'Kỹ năng/môn này chưa có thang điểm cấu hình. Hãy cấu hình thang điểm trước.'
    return
  }
  if (Number(form.targetScoreMin) > Number(form.targetScoreMax)) {
    error.value = 'Điểm mục tiêu bắt đầu phải nhỏ hơn hoặc bằng điểm kết thúc.'
    return
  }
  if (Number(form.passingScore) > maxPoints.value) {
    error.value = 'Điểm đạt không thể lớn hơn tổng điểm bài kiểm tra.'
    return
  }
  previewLoading.value = true
  try {
    if (!templateId.value) await createTemplate()
      const sections = await Promise.all(form.sections.map(async (section) => {
      const candidates = asList(await adminService.questions({
        certificateId: Number(form.certificateId),
        topicId: Number(form.topicId),
        questionType: section.questionType,
        difficulty: section.difficulty,
        status: 'APPROVED',
      }))
      const available = candidates.filter((question) =>
        String(question.certificateId) === String(form.certificateId)
        && String(question.topicId) === String(form.topicId)
        && question.questionType?.toUpperCase() === section.questionType
        && question.difficulty === section.difficulty
        && question.status === 'APPROVED'
      )
      return {
        form: section,
        candidates: available,
        selectedQuestionIds: [],
      }
    }))
    const selectedIds = new Set()
    for (const [sectionIndex, section] of sections.entries()) {
      section.candidates = section.candidates.filter((question) => !selectedIds.has(question.questionId))
      if (section.candidates.length < section.form.questionCount) {
        throw new Error(`Phần ${section.form.name} cần ${section.form.questionCount} câu riêng biệt; hiện chỉ còn ${section.candidates.length} câu đã duyệt phù hợp.`)
      }
      const preferredIds = draftQuestionSelections.value[sectionIndex] || []
      const validPreferred = preferredIds.filter((questionId) =>
        section.candidates.some((question) => question.questionId === questionId)
      ).slice(0, Number(section.form.questionCount))
      const fallbackIds = section.candidates
        .filter((question) => !validPreferred.includes(question.questionId))
        .slice(0, Number(section.form.questionCount) - validPreferred.length)
        .map((question) => question.questionId)
      section.selectedQuestionIds = [...validPreferred, ...fallbackIds]
      section.selectedQuestionIds.forEach((questionId) => selectedIds.add(questionId))
    }
    previewSections.value = sections
    draftQuestionSelections.value = sections.map((section) => section.selectedQuestionIds)
    stage.value = 'preview'
    currentQuestionNumber.value = 1
    startSimulationTimer()
  } catch (exception) {
    await deactivateDraftTemplate()
    error.value = exception?.response?.data?.responseMessage
      || exception?.response?.data?.message
      || exception?.message
      || 'Không thể tải trước câu hỏi phù hợp.'
  } finally {
    previewLoading.value = false
  }
}

function togglePreviewQuestion(section, questionId, checked) {
  const alreadyUsed = previewSections.value.some((otherSection) =>
    otherSection !== section && otherSection.selectedQuestionIds.includes(questionId)
  )
  if (checked && alreadyUsed) return
  const selected = new Set(section.selectedQuestionIds)
  if (checked && selected.size < Number(section.form.questionCount)) selected.add(questionId)
  else if (!checked) selected.delete(questionId)
  section.selectedQuestionIds = [...selected]
}

function parseOptions(question) {
  try {
    const options = JSON.parse(question.optionsJson || '[]')
    if (Array.isArray(options)) return options.map((option) =>
      typeof option === 'string' ? option : `${option.label ? `${option.label}. ` : ''}${option.text ?? option.value ?? ''}`
    )
    if (options && typeof options === 'object') return Object.entries(options).map(([key, value]) => `${key}. ${value}`)
  } catch { return [] }
  return []
}

function candidateQuestions(section, sectionIndex) {
  const selected = new Set(section.selectedQuestionIds)
  const query = String(previewSearch[sectionIndex] || '').trim().toLowerCase()
  const matches = section.candidates.filter((question) =>
    !query || String(question.content || '').toLowerCase().includes(query)
  )
  const firstCandidates = matches.slice(0, 40)
  const selectedOutsideList = section.candidates.filter((question) => selected.has(question.questionId)
    && !firstCandidates.some((candidate) => candidate.questionId === question.questionId))
  return [...firstCandidates, ...selectedOutsideList]
}

async function editConfiguration() {
  stopSimulationTimer()
  await deactivateDraftTemplate()
  draftQuestionSelections.value = previewSections.value.map((section) => section.selectedQuestionIds)
  previewSections.value = []
  stage.value = 'builder'
  error.value = ''
}

async function closeDialog() {
  if (previewLoading.value || saving.value) return
  if (previewSections.value.length) {
    draftQuestionSelections.value = previewSections.value.map((section) => section.selectedQuestionIds)
  }
  stopSimulationTimer()
  await deactivateDraftTemplate()
  stage.value = 'builder'
  previewSections.value = []
  persistDraft()
  emit('close')
  await nextTick()
  returnFocusElement?.focus()
  returnFocusElement = null
}

async function resetDraft() {
  const accepted = await confirm({
    title: 'Xóa bản nháp hiện tại?',
    description: 'Thông tin bài thi và cấu trúc các phần sẽ được đặt lại.',
    confirmText: 'Xóa bản nháp',
    cancelText: 'Giữ bản nháp',
  })
  if (!accepted) return
  await deactivateDraftTemplate()
  clearDraft()
  resetForm()
}

async function publishExam() {
  if (saving.value) return
  if (previewSections.value.some((section) => section.selectedQuestionIds.length !== Number(section.form.questionCount))) {
    error.value = 'Hãy chọn đủ số câu cho từng phần trước khi phát hành.'
    return
  }
  error.value = ''
  saving.value = true
  try {
    await adminService.generateExamFromTemplate(templateId.value, {
      sectionDifficulties: form.sections.map((section) => section.difficulty),
      questionIdsBySection: previewSections.value.map((section) => section.selectedQuestionIds),
    })
    await deactivateDraftTemplate()
    stopSimulationTimer()
    clearDraft()
    emit('saved')
    emit('close')
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage
      || exception?.response?.data?.message
      || 'Không thể phát hành bài thi. Hãy kiểm tra lại câu hỏi đã chọn.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-if="open" class="exam-creation-page" @click.self="closeDialog" @keydown="handleDialogKeydown">
    <div ref="windowRef" class="exam-creation-window exam-builder" role="dialog" aria-modal="true" aria-labelledby="exam-creation-title" tabindex="-1">
    <header class="exam-creation-topbar">
      <div class="exam-creation-brand"><span><GraduationCap :size="20" /></span><div><strong id="exam-creation-title">{{ stage === 'preview' ? form.title || 'Bài kiểm tra mới' : 'Tạo bài kiểm tra' }}</strong><small>{{ stage === 'preview' ? `${selectedTopic?.name || 'Bản xem trước'} · ${form.durationMinutes} phút` : 'Thiết lập cấu trúc và câu hỏi' }}</small></div></div>
      <div v-if="stage === 'preview'" class="exam-sim-clock" :class="{ urgent: remainingSeconds <= 300 }"><Clock3 :size="17" /><span>{{ remainingTimeLabel }}</span><small>Thời gian làm bài</small></div>
      <button type="button" class="exam-creation-exit" aria-label="Thoát tạo bài kiểm tra" title="Thoát tạo bài kiểm tra" @click="closeDialog"><X :size="20" /></button>
    </header>
    <div class="exam-creation-body">
      <form id="exam-builder-form" class="exam-builder-form" @submit.prevent="stage === 'preview' ? publishExam() : loadPreview()">
        <div v-if="stage === 'builder'" class="exam-builder-content">
          <section class="exam-builder-section exam-details-section">
            <div class="exam-builder-section-heading"><span>01</span><div><h3>Thông tin bài thi</h3><p>Chọn kỳ thi và kỹ năng/môn áp dụng cho toàn bộ bài.</p></div></div>
            <div class="exam-builder-grid">
              <label>Kỳ thi / chứng chỉ<select v-model="form.certificateId" required><option disabled value="">Chọn kỳ thi</option><option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }} ({{ program.code }})</option></select></label>
              <label>Kỹ năng / môn thi<select v-model="form.topicId" required><option disabled value="">Chọn một kỹ năng hoặc môn thi</option><option v-for="topic in availableTopics" :key="topic.topicId" :value="String(topic.topicId)">{{ topic.name }}</option></select></label>
              <label class="span-two">Tên bài kiểm tra<input v-model.trim="form.title" required maxlength="200" placeholder="Ví dụ: IELTS Academic - Reading Practice" /></label>
              <label class="span-two">Mô tả<textarea v-model.trim="form.description" rows="2" placeholder="Mục tiêu hoặc ghi chú về bài kiểm tra"></textarea></label>
              <label>Thời lượng (phút)<input v-model.number="form.durationMinutes" type="number" min="1" max="600" required /></label>
              <label>Điểm đạt<input v-model.number="form.passingScore" type="number" min="0" :max="maxPoints" step="any" required /><small>Tổng điểm bài thi: {{ maxPoints }}</small></label>
              <label>{{ selectedTopic?.scoreUnit === 'band' ? 'Band mục tiêu từ' : 'Điểm mục tiêu từ' }}
                <select v-if="selectedTopic?.scoreUnit === 'band'" v-model.number="form.targetScoreMin" required><option v-for="score in scoreOptions" :key="score" :value="score">{{ score.toFixed(1) }}</option></select>
                <input v-else v-model.number="form.targetScoreMin" type="number" :min="selectedTopic?.scoreMin ?? 0" :max="selectedTopic?.scoreMax ?? 100" :step="selectedTopic?.scoreStep || 1" required />
              </label>
              <label>{{ selectedTopic?.scoreUnit === 'band' ? 'Band mục tiêu đến' : 'Điểm mục tiêu đến' }}
                <select v-if="selectedTopic?.scoreUnit === 'band'" v-model.number="form.targetScoreMax" required><option v-for="score in scoreOptions" :key="score" :value="score">{{ score.toFixed(1) }}</option></select>
                <input v-else v-model.number="form.targetScoreMax" type="number" :min="selectedTopic?.scoreMin ?? 0" :max="selectedTopic?.scoreMax ?? 100" :step="selectedTopic?.scoreStep || 1" required />
              </label>
              <p v-if="selectedTopic && !hasTargetScale" class="exam-builder-scale-error">Kỹ năng/môn này chưa có thang điểm cấu hình. Hãy cấu hình thang điểm trước khi tạo bài.</p>
            </div>
          </section>

          <section class="exam-builder-section structure-section">
            <div class="exam-builder-section-heading"><span>02</span><div><h3>Cấu trúc bài kiểm tra</h3><p>{{ form.sections.length }} phần · {{ form.sections.reduce((sum, section) => sum + Number(section.questionCount || 0), 0) }} câu · {{ maxPoints }} điểm</p></div><button class="exam-builder-add-button" type="button" @click="addSection"><Plus :size="16" /> Thêm phần</button></div>
            <article v-for="(section, index) in form.sections" :key="section" class="exam-builder-part">
              <header class="exam-builder-part-header"><span class="exam-builder-part-number">{{ String(index + 1).padStart(2, '0') }}</span><strong>Phần {{ index + 1 }}</strong><div class="exam-builder-part-actions"><button type="button" title="Di chuyển lên" aria-label="Di chuyển phần lên" :disabled="index === 0" @click="moveSection(index, -1)"><ArrowUp :size="16" /></button><button type="button" title="Di chuyển xuống" aria-label="Di chuyển phần xuống" :disabled="index === form.sections.length - 1" @click="moveSection(index, 1)"><ArrowDown :size="16" /></button><button type="button" title="Xóa phần" aria-label="Xóa phần" :disabled="form.sections.length <= 1" @click="removeSection(index)"><Trash2 :size="16" /></button></div></header>
              <div class="exam-builder-part-fields"><label>Tên phần<input v-model.trim="section.name" required maxlength="120" :aria-label="`Tên phần ${index + 1}`" /></label><label>Dạng câu hỏi<select v-model="section.questionType"><option value="MULTIPLE_CHOICE">Trắc nghiệm</option><option value="TRUE_FALSE">Đúng / sai</option><option value="ESSAY">Tự luận</option><option value="FILL_BLANK">Điền khuyết</option></select></label><label>Độ khó<select v-model="section.difficulty"><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label><label>Số câu<input v-model.number="section.questionCount" type="number" min="1" max="200" required /></label><label>Điểm / câu<input v-model.number="section.pointsPerQuestion" type="number" min="0.1" step="0.1" required /></label></div>
            </article>
          </section>
          <p v-if="error" class="exam-builder-error" role="alert">{{ error }}</p>
        </div>
        <div v-else class="exam-builder-content preview-content">
          <div class="preview-stage-toolbar">
            <div><span class="preview-stage-kicker">MÔ PHỎNG TRẢI NGHIỆM THÍ SINH</span><h2>{{ form.title }}</h2><p>{{ selectedTopic?.name }} · {{ simulationQuestions.length }} câu · {{ previewTotalPoints }} điểm · mục tiêu {{ form.targetScoreMin }}–{{ form.targetScoreMax }} {{ selectedTopic?.scoreUnit }}</p></div>
            <div class="preview-mode-switch" role="tablist" aria-label="Chế độ xem trước">
              <button type="button" :class="{ active: previewMode === 'exam' }" @click="previewMode = 'exam'"><Clock3 :size="15" /> Mô phỏng làm bài</button>
              <button type="button" :class="{ active: previewMode === 'questions' }" @click="previewMode = 'questions'"><ListChecks :size="15" /> Chọn câu hỏi</button>
            </div>
          </div>
          <div v-if="previewMode === 'exam'" class="exam-simulator">
            <aside class="exam-question-nav">
              <div class="exam-nav-heading"><div><strong>Nội dung bài thi</strong><small>{{ answeredQuestionCount }} / {{ simulationQuestions.length }} câu đã trả lời</small></div><CheckCircle2 :size="19" /></div>
              <div v-for="(section, sectionIndex) in previewSections" :key="sectionIndex" class="exam-nav-section">
                <span>{{ section.form.name }}</span>
                <div>
                  <button v-for="item in simulationQuestions.filter((entry) => entry.section === section)" :key="item.question.questionId" type="button" :class="{ active: currentQuestionNumber === item.number, answered: simulationAnswers[item.question.questionId], flagged: flaggedQuestionIds.has(item.question.questionId) }" :aria-label="`Đi tới câu ${item.number}`" @click="moveToQuestion(item.number)">{{ item.number }}</button>
                </div>
              </div>
              <div class="exam-nav-legend"><span><i class="answered-dot"></i> Đã trả lời</span><span><i class="flagged-dot"></i> Đánh dấu xem lại</span></div>
            </aside>
            <section v-if="activeSimulationQuestion" class="exam-question-stage" aria-live="polite">
              <div class="exam-question-meta"><span>{{ activeSimulationQuestion.section.form.name }}</span><span>Câu {{ activeSimulationQuestion.number }} / {{ simulationQuestions.length }}</span><button type="button" :class="{ flagged: flaggedQuestionIds.has(activeSimulationQuestion.question.questionId) }" @click="toggleFlag(activeSimulationQuestion.question.questionId)"><Flag :size="15" /> {{ flaggedQuestionIds.has(activeSimulationQuestion.question.questionId) ? 'Đã đánh dấu' : 'Đánh dấu xem lại' }}</button></div>
              <div class="exam-question-content"><span class="exam-question-number">{{ String(activeSimulationQuestion.number).padStart(2, '0') }}</span><h3>{{ activeSimulationQuestion.question.content }}</h3></div>
              <div v-if="parseOptions(activeSimulationQuestion.question).length" class="exam-answer-options">
                <button v-for="(option, optionIndex) in parseOptions(activeSimulationQuestion.question)" :key="optionIndex" type="button" :class="{ selected: simulationAnswers[activeSimulationQuestion.question.questionId] === option }" :aria-pressed="simulationAnswers[activeSimulationQuestion.question.questionId] === option" @click="simulationAnswers[activeSimulationQuestion.question.questionId] = option"><span>{{ String.fromCharCode(65 + optionIndex) }}</span>{{ option }}</button>
              </div>
              <label v-else class="exam-essay-answer">Câu trả lời<textarea v-model="simulationAnswers[activeSimulationQuestion.question.questionId]" rows="7" placeholder="Nhập câu trả lời của bạn…"></textarea></label>
              <div class="exam-question-bottom"><span>{{ activeSimulationQuestion.section.form.pointsPerQuestion }} điểm</span><div><button type="button" class="sim-secondary" :disabled="currentQuestionNumber <= 1" @click="moveToQuestion(currentQuestionNumber - 1)"><ArrowLeft :size="16" /> Câu trước</button><button type="button" class="sim-primary" :disabled="currentQuestionNumber >= simulationQuestions.length" @click="moveToQuestion(currentQuestionNumber + 1)">Câu tiếp theo <ArrowRight :size="16" /></button></div></div>
            </section>
          </div>
          <section v-else v-for="(section, sectionIndex) in previewSections" :key="sectionIndex" class="exam-builder-section preview-questions-section">
            <div class="exam-builder-section-heading"><span>{{ String(sectionIndex + 1).padStart(2, '0') }}</span><div><h3>{{ section.form.name }}</h3><p>{{ section.form.questionType }} · {{ section.form.difficulty }} · chọn {{ section.selectedQuestionIds.length }} / {{ section.form.questionCount }}</p></div><label class="preview-search"><Search :size="14" /><input v-model="previewSearch[sectionIndex]" type="search" placeholder="Tìm câu hỏi" :aria-label="`Tìm câu hỏi trong ${section.form.name}`" /></label></div>
            <div class="preview-question-list">
              <label v-for="(question, questionIndex) in candidateQuestions(section, sectionIndex)" :key="question.questionId" class="preview-question-choice" :class="{ selected: section.selectedQuestionIds.includes(question.questionId) }">
                <input type="checkbox" :checked="section.selectedQuestionIds.includes(question.questionId)" :disabled="(!section.selectedQuestionIds.includes(question.questionId) && section.selectedQuestionIds.length >= Number(section.form.questionCount)) || previewSections.some((otherSection) => otherSection !== section && otherSection.selectedQuestionIds.includes(question.questionId))" @change="togglePreviewQuestion(section, question.questionId, $event.target.checked)" />
                <span class="preview-question-order">{{ questionIndex + 1 }}</span>
                <span class="preview-question-body"><strong>{{ question.content }}</strong><small v-for="option in parseOptions(question)" :key="option">{{ option }}</small></span>
                <span class="preview-question-points">{{ section.form.pointsPerQuestion }} đ</span>
              </label>
            </div>
            <p v-if="candidateQuestions(section, sectionIndex).length === 40" class="preview-candidate-note">Đang hiển thị tối đa 40 câu. Dùng ô tìm kiếm để lọc thêm.</p>
          </section>
          <p v-if="error" class="exam-builder-error" role="alert">{{ error }}</p>
        </div>
      </form>
    </div>
    <footer class="exam-creation-footer">
        <div class="exam-creation-footer-left">
          <span v-if="draftSavedAt" class="exam-draft-status">Bản nháp tự lưu lúc {{ formatDraftSavedAt() }}</span>
          <button v-if="stage === 'builder'" type="button" class="exam-reset-draft" :disabled="saving || previewLoading" @click="resetDraft">Xóa bản nháp</button>
        </div>
        <template v-if="stage === 'builder'">
          <button type="button" class="overlay-button overlay-button-secondary" @click="closeDialog">Hủy</button>
          <button form="exam-builder-form" class="overlay-button overlay-button-primary overlay-button-loading" :class="{ 'is-loading': previewLoading }" type="submit" :disabled="previewLoading">
            <LoaderCircle v-if="previewLoading" class="animate-spin" :size="16" /><Check v-else :size="16" />
            {{ previewLoading ? 'Đang tải xem trước…' : 'Xem trước bài thi' }}
          </button>
        </template>
        <template v-else>
          <button type="button" class="overlay-button overlay-button-secondary" :disabled="saving" @click="editConfiguration">Quay lại chỉnh sửa</button>
          <button form="exam-builder-form" class="overlay-button overlay-button-primary overlay-button-loading" :class="{ 'is-loading': saving }" type="submit" :disabled="saving">
            <LoaderCircle v-if="saving" class="animate-spin" :size="16" /><Check v-else :size="16" />
            {{ saving ? 'Đang phát hành…' : 'Phát hành bài thi' }}
          </button>
        </template>
    </footer>
    </div>
  </div>
</template>

<style scoped>
.exam-creation-page { position: fixed; inset: 0; z-index: 150; display: grid; place-items: center; padding: 4vh 4vw; overflow: hidden; background: var(--overlay-scrim, rgba(15, 23, 42, 0.45)); backdrop-filter: blur(4px); color: var(--text, #0F172A); font-family: inherit; }
.exam-creation-window { display: grid; width: 100%; height: 100%; min-width: 0; grid-template-rows: 68px minmax(0, 1fr) auto; overflow: hidden; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-2xl, 16px); background: var(--bg-page, #F8FAFC); box-shadow: 0 24px 48px -12px rgba(15, 23, 42, 0.18); }
.exam-creation-topbar { display: flex; min-width: 0; align-items: center; gap: 18px; padding: 0 clamp(16px, 3vw, 40px); border-bottom: 1px solid var(--ds-border, #E2E8F0); background: var(--surface, #FFFFFF); }
.exam-creation-brand { display: flex; min-width: 0; flex: 1; align-items: center; gap: 12px; }
.exam-creation-brand > span { display: grid; width: 40px; height: 40px; flex: 0 0 auto; place-items: center; border-radius: var(--radius-lg, 10px); color: white; background: var(--brand-gradient, linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%)); box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2); }
.exam-creation-brand > div { display: grid; min-width: 0; gap: 2px; }
.exam-creation-brand strong { overflow: hidden; color: var(--text-strong, #0F172A); font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.exam-creation-brand small { color: var(--text-muted, #64748B); font-size: 12px; }
.exam-sim-clock { display: grid; grid-template-columns: 20px auto; align-items: center; column-gap: 8px; min-width: 126px; padding: 8px 12px; border: 1px solid var(--primary-border, #BFDBFE); border-radius: var(--radius-lg, 8px); color: var(--primary-ink, #1D4ED8); background: var(--primary-tint, #EFF6FF); font-variant-numeric: tabular-nums; }
.exam-sim-clock svg { grid-row: span 2; }
.exam-sim-clock span { font-size: 16px; font-weight: 700; }
.exam-sim-clock small { font-size: 10px; }
.exam-sim-clock.urgent { border-color: #FECACA; color: #DC2626; background: #FEF2F2; }
.exam-creation-exit { display: grid; width: 44px; height: 44px; flex: 0 0 auto; place-items: center; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-md, 8px); color: var(--text-secondary, #64748B); background: var(--surface, #FFFFFF); cursor: pointer; transition: all 0.15s ease; }
.exam-creation-exit:hover { background: var(--surface-hover, #F1F5F9); color: var(--text-strong, #0F172A); border-color: #CBD5E1; }
.exam-creation-body { min-height: 0; overflow: auto; padding: 24px clamp(16px, 3vw, 40px); }
.exam-creation-body > form { width: 100%; margin: 0 auto; }
.exam-creation-footer { display: flex; min-height: 72px; align-items: center; justify-content: flex-end; gap: 10px; padding: 12px clamp(16px, 3vw, 40px); border-top: 1px solid var(--ds-border, #E2E8F0); background: var(--surface, #FFFFFF); }
.exam-creation-footer-left { display: flex; min-width: 0; flex: 1; align-items: center; gap: 16px; }
.exam-draft-status { color: var(--text-muted, #64748B); font-size: 12px; }
.exam-reset-draft { min-height: 40px; padding: 0 8px; border: 0; color: var(--text-muted, #64748B); background: transparent; font: inherit; font-size: 12px; text-decoration: underline; text-underline-offset: 3px; cursor: pointer; }
.exam-reset-draft:hover { color: var(--danger, #DC2626); }
.exam-reset-draft:disabled { opacity: .5; cursor: not-allowed; }
.preview-stage-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 2px 0 6px; }
.preview-stage-toolbar h2 { margin: 5px 0 0; color: var(--text, #0F172A); font-size: 22px; line-height: 30px; font-weight: 700; }
.preview-stage-toolbar p { margin: 4px 0 0; color: var(--text-muted, #64748B); font-size: 13px; }
.preview-stage-kicker { color: var(--ds-primary, #2563EB); font-size: 11px; font-weight: 700; }
.preview-mode-switch { display: inline-flex; flex: 0 0 auto; gap: 4px; padding: 4px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-lg, 8px); background: var(--surface-muted, #F1F5F9); }
.preview-mode-switch button { display: inline-flex; min-height: 40px; align-items: center; gap: 7px; padding: 0 12px; border: 0; border-radius: var(--radius-md, 6px); color: var(--text-secondary, #64748B); background: transparent; font: inherit; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s ease; }
.preview-mode-switch button.active { color: var(--ds-primary, #2563EB); background: var(--surface, #FFFFFF); box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); font-weight: 600; }
.exam-simulator { display: grid; grid-template-columns: minmax(220px, 260px) minmax(0, 1fr); min-height: min(620px, calc(100vh - 250px)); border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-xl, 12px); overflow: hidden; background: var(--surface, #FFFFFF); box-shadow: var(--shadow-card, 0 2px 8px rgba(0,0,0,0.04)); }
.exam-question-nav { display: flex; flex-direction: column; gap: 20px; padding: 18px; border-right: 1px solid var(--ds-border, #E2E8F0); background: var(--surface-subtle, #F8FAFC); }
.exam-nav-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 15px; border-bottom: 1px solid var(--border-subtle, #E2E8F0); color: var(--ds-primary, #2563EB); }
.exam-nav-heading div { display: grid; gap: 4px; }
.exam-nav-heading strong { color: var(--text-strong, #0F172A); font-size: 14px; }
.exam-nav-heading small { color: var(--text-muted, #64748B); font-size: 12px; }
.exam-nav-section { display: grid; gap: 10px; }
.exam-nav-section > span { color: var(--text-muted, #64748B); font-size: 12px; font-weight: 600; }
.exam-nav-section > div { display: grid; grid-template-columns: repeat(5, minmax(34px, 1fr)); gap: 8px; }
.exam-nav-section button { position: relative; display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid var(--border-control, #CBD5E1); border-radius: var(--radius-md, 6px); color: var(--text-secondary, #475569); background: var(--surface, #FFFFFF); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; }
.exam-nav-section button.active { border-color: var(--ds-primary, #2563EB); outline: 2px solid var(--ds-primary, #2563EB); outline-offset: 1px; }
.exam-nav-section button.answered { border-color: var(--ds-primary, #2563EB); color: white; background: var(--ds-primary, #2563EB); }
.exam-nav-section button.flagged::after { position: absolute; top: -4px; right: -4px; width: 9px; height: 9px; border: 1px solid white; border-radius: 50%; background: var(--amber, #D97706); content: ''; }
.exam-nav-legend { display: grid; gap: 8px; margin-top: auto; color: var(--text-muted, #64748B); font-size: 11px; }
.exam-nav-legend span { display: flex; align-items: center; gap: 8px; }
.exam-nav-legend i { width: 10px; height: 10px; border-radius: 3px; background: var(--ds-primary, #2563EB); }
.exam-nav-legend i.flagged-dot { background: var(--amber, #D97706); }
.exam-question-stage { display: flex; min-width: 0; flex-direction: column; padding: clamp(18px, 3vw, 36px); }
.exam-question-meta { display: flex; align-items: center; gap: 10px; padding-bottom: 18px; border-bottom: 1px solid var(--border-subtle, #E2E8F0); color: var(--text-muted, #64748B); font-size: 12px; }
.exam-question-meta > span:first-child { color: var(--primary-ink, #1D4ED8); font-weight: 600; }
.exam-question-meta > span:nth-child(2) { margin-right: auto; }
.exam-question-meta button { display: inline-flex; min-height: 40px; align-items: center; gap: 7px; padding: 0 10px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-md, 6px); color: var(--text-secondary, #475569); background: var(--surface, #FFFFFF); font: inherit; font-size: 12px; cursor: pointer; transition: all 0.15s ease; }
.exam-question-meta button.flagged { border-color: #FDE68A; color: #B45309; background: #FEF3C7; }
.exam-question-content { display: flex; align-items: flex-start; gap: 14px; margin: 28px 0 22px; }
.exam-question-number { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border-radius: var(--radius-md, 8px); color: var(--primary-ink, #1D4ED8); background: var(--primary-soft, #EFF6FF); border: 1px solid var(--primary-border, #BFDBFE); font-size: 13px; font-weight: 700; }
.exam-question-content h3 { max-width: 800px; margin: 3px 0 0; color: var(--text-strong, #0F172A); font-size: 18px; line-height: 1.65; font-weight: 600; white-space: pre-wrap; }
.exam-answer-options { display: grid; gap: 10px; max-width: 840px; }
.exam-answer-options button { display: flex; min-height: 56px; align-items: center; gap: 12px; padding: 10px 14px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-lg, 10px); color: var(--text-secondary, #334155); background: var(--surface, #FFFFFF); text-align: left; font: inherit; font-size: 14px; line-height: 1.5; cursor: pointer; transition: all 0.15s ease; }
.exam-answer-options button:hover { border-color: var(--primary-border, #BFDBFE); background: var(--primary-tint, #EFF6FF); }
.exam-answer-options button.selected { border-color: var(--ds-primary, #2563EB); color: var(--text-strong, #0F172A); background: var(--primary-soft, #EFF6FF); font-weight: 500; }
.exam-answer-options button > span { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border: 1px solid var(--border-control, #CBD5E1); border-radius: 50%; color: var(--text-muted, #64748B); font-size: 12px; font-weight: 600; }
.exam-answer-options button.selected > span { border-color: var(--ds-primary, #2563EB); color: white; background: var(--ds-primary, #2563EB); }
.exam-essay-answer { display: grid; max-width: 840px; gap: 8px; color: var(--text-strong, #0F172A); font-size: 13px; font-weight: 600; }
.exam-essay-answer textarea { min-height: 160px; padding: 12px; border: 1px solid var(--border-control, #CBD5E1); border-radius: var(--radius-md, 8px); color: var(--text, #0F172A); background: var(--surface, #FFFFFF); font: inherit; line-height: 1.6; resize: vertical; }
.exam-question-bottom { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: auto; padding-top: 24px; color: var(--text-muted, #64748B); font-size: 12px; }
.exam-question-bottom > div { display: flex; gap: 8px; }
.exam-question-bottom button { display: inline-flex; min-height: 44px; align-items: center; justify-content: center; gap: 8px; padding: 0 14px; border: 1px solid var(--border-control, #CBD5E1); border-radius: var(--radius-md, 8px); color: var(--text-strong, #0F172A); background: var(--surface, #FFFFFF); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; }
.exam-question-bottom button:hover:not(:disabled) { background: #F1F5F9; }
.exam-question-bottom button.sim-primary { border-color: var(--ds-primary, #2563EB); color: white; background: var(--ds-primary, #2563EB); }
.exam-question-bottom button.sim-primary:hover:not(:disabled) { background: var(--primary-hover, #1D4ED8); }
.exam-question-bottom button:disabled { opacity: .5; cursor: not-allowed; }
.preview-content { gap: 18px; }
.exam-builder-form { min-width: 0; }
.exam-builder-content { display: grid; gap: 20px; min-width: 0; }
.exam-builder-section { min-width: 0; padding: 22px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-2xl, 14px); background: var(--surface, #FFFFFF); box-shadow: var(--shadow-card, 0 1px 3px rgba(0,0,0,0.03)); }
.exam-builder-section-heading { display: flex; align-items: center; gap: 14px; min-width: 0; margin-bottom: 20px; }
.exam-builder-section-heading > span { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 8px; color: var(--primary-ink, #1D4ED8); background: var(--primary-soft, #EFF6FF); border: 1px solid var(--primary-border, #BFDBFE); font-size: 13px; font-weight: 700; }
.exam-builder-section-heading > div { min-width: 0; flex: 1; }
.exam-builder-section-heading h3 { margin: 0; color: var(--text-strong, #0F172A); font-size: 16px; line-height: 24px; font-weight: 600; }
.exam-builder-section-heading p { margin: 4px 0 0; color: var(--text-muted, #64748B); font-size: 13px; line-height: 1.5; }
.exam-builder-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(min(240px,100%),1fr)); gap: 16px; align-items: start; }
.exam-builder label { display: flex; flex-direction: column; justify-content: flex-start; min-width: 0; gap: 6px; color: var(--text-strong, #0F172A); font-size: 13px; font-weight: 600; }
.exam-builder input,.exam-builder select,.exam-builder textarea { width: 100%; min-width: 0; min-height: 44px; padding: 0 14px; border: 1px solid var(--border-control, #CBD5E1); border-radius: var(--radius-md, 8px); color: var(--text, #0F172A); background: var(--surface, #FFFFFF); font: inherit; font-size: 14px; transition: border-color 0.15s ease, box-shadow 0.15s ease; }
.exam-builder textarea { min-height: 96px; padding: 12px 14px; line-height: 1.6; resize: vertical; }
.exam-builder input:focus-visible,.exam-builder select:focus-visible,.exam-builder textarea:focus-visible { border-color: var(--ds-primary, #2563EB); outline: 2px solid var(--ds-primary, #2563EB); outline-offset: 1px; }
.exam-builder label small { color: var(--text-muted, #64748B); font-size: 12px; font-weight: 400; }
.span-two { grid-column: 1 / -1; }
.structure-section .exam-builder-section-heading { margin-bottom: 16px; }
.exam-builder-add-button { display: inline-flex; min-height: 42px; align-items: center; gap: 8px; padding: 0 14px; border: 1px solid var(--primary-border, #BFDBFE); border-radius: var(--radius-lg, 8px); color: var(--primary-ink, #1D4ED8); background: var(--primary-tint, #EFF6FF); font: inherit; font-size: 13.5px; font-weight: 600; cursor: pointer; transition: all 0.15s ease; }
.exam-builder-add-button:hover { background: var(--primary-soft, #EFF6FF); border-color: var(--ds-primary, #2563EB); }
.exam-builder-part { min-width: 0; margin-top: 12px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-lg, 10px); background: var(--surface-subtle, #F8FAFC); }
.exam-builder-part-header { display: flex; min-height: 52px; align-items: center; gap: 12px; padding: 4px 12px; border-bottom: 1px solid var(--border-subtle, #E2E8F0); }
.exam-builder-part-number { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: var(--text-secondary, #475569); background: var(--surface-muted, #E2E8F0); font-size: 12px; font-weight: 700; }
.exam-builder-part-header > strong { min-width: 0; flex: 1; color: var(--text-strong, #0F172A); font-size: 14px; font-weight: 600; }
.exam-builder-part-actions { display: flex; flex: 0 0 auto; gap: 4px; }
.exam-builder-part-actions button { display: grid; width: 38px; height: 38px; place-items: center; border: 0; border-radius: var(--radius-sm, 6px); color: var(--text-muted, #64748B); background: transparent; cursor: pointer; transition: all 0.15s ease; }
.exam-builder-part-actions button:hover:not(:disabled) { color: var(--ds-primary, #2563EB); background: var(--surface-hover, #EDF2F7); }
.exam-builder-part-actions button:disabled { color: #CBD5E1; cursor: not-allowed; }
.exam-builder-part-fields { display: grid; grid-template-columns: repeat(auto-fit,minmax(min(180px,100%),1fr)); gap: 16px; padding: 16px; align-items: start; }
.preview-summary > p { margin: 0; color: var(--text-muted, #64748B); font-size: 13px; }
.preview-questions-section { padding-bottom: 16px; }
.preview-search { position: relative; display: flex !important; width: min(280px,40%); min-height: 42px; align-items: center; gap: 8px !important; padding: 0 12px; border: 1px solid var(--border-control, #CBD5E1); border-radius: var(--radius-lg, 8px); color: var(--text-muted, #64748B) !important; background: var(--surface, #FFFFFF); }
.preview-search input { min-height: 38px; padding: 0; border: 0; font-size: 13px !important; background: transparent; }
.preview-search input:focus-visible { outline: none; }
.preview-question-list { display: grid; gap: 8px; }
.preview-question-choice { display: flex !important; min-width: 0; min-height: 56px; align-items: flex-start; gap: 12px !important; padding: 12px; border: 1px solid var(--ds-border, #E2E8F0); border-radius: var(--radius-lg, 10px); background: var(--surface, #FFFFFF); cursor: pointer; transition: all 0.15s ease; }
.preview-question-choice:hover { border-color: var(--primary-border, #BFDBFE); background: var(--primary-tint, #EFF6FF); }
.preview-question-choice.selected { border-color: var(--ds-primary, #2563EB); background: var(--primary-soft, #EFF6FF); }
.preview-question-choice > input { width: 18px; min-height: 18px; flex: 0 0 auto; margin-top: 2px; accent-color: var(--ds-primary, #2563EB); }
.preview-question-order { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: var(--text-secondary, #475569); background: var(--surface-muted, #E2E8F0); font-size: 12px; font-weight: 600; }
.preview-question-body { display: grid; min-width: 0; flex: 1; gap: 6px; color: var(--text-secondary, #475569); font-size: 13px; line-height: 1.5; }
.preview-question-body strong { color: var(--text-strong, #0F172A); font-size: 14px; font-weight: 600; white-space: pre-wrap; }
.preview-question-points { color: var(--primary-ink, #1D4ED8); font-size: 12px; font-weight: 600; white-space: nowrap; }
.preview-candidate-note { margin: 10px 0 0; color: var(--text-muted, #64748B); font-size: 12px; }
.exam-builder-error { margin: 0; padding: 12px 14px; border: 1px solid #FECACA; border-radius: var(--radius-lg, 8px); color: var(--danger, #DC2626); background: #FEF2F2; font-size: 13px; }
.exam-builder-scale-error { grid-column: 1 / -1; margin: 0; color: var(--danger, #DC2626); font-size: 12.5px; }
.exam-builder-form :focus-visible { outline: 2px solid var(--ds-primary, #2563EB); outline-offset: 2px; }
@media (max-width: 768px) {
  .exam-creation-page { padding: 3vh 3vw; }
  .exam-creation-window { grid-template-rows: 60px minmax(0,1fr) auto; }
  .exam-creation-topbar { padding: 0 14px; }
  .exam-creation-body { padding: 16px 12px; }
  .exam-creation-footer { min-height: 64px; padding: 10px 12px; }
  .preview-stage-toolbar { align-items: flex-start; flex-direction: column; }
  .exam-simulator { grid-template-columns: 1fr; }
  .exam-question-nav { order: 2; border-top: 1px solid var(--ds-border); border-right: 0; }
  .exam-question-stage { min-height: 420px; }
  .exam-nav-section > div { grid-template-columns: repeat(auto-fill, 38px); }
  .exam-builder-content { gap: 12px; }
  .exam-builder-section { padding: 16px; border-radius: var(--radius-lg); }
  .exam-builder-section-heading { align-items: flex-start; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
  .exam-builder-grid { grid-template-columns: minmax(0,1fr); }
  .span-two { grid-column: auto; }
  .exam-builder-part-fields { grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px; padding: 12px; }
  .preview-search { width: 100%; }
}
@media (max-width: 480px) {
  .exam-creation-page { padding: 0; }
  .exam-creation-window { grid-template-rows: 60px minmax(0,1fr) auto; border-radius: 0; }
  .exam-creation-footer { flex-wrap: wrap; }
  .exam-creation-footer-left { flex: 1 0 100%; justify-content: space-between; }
  .exam-creation-brand > span { width: 36px; height: 36px; }
  .exam-creation-brand strong { max-width: 210px; font-size: 13px; }
  .exam-creation-brand small { display: none; }
  .exam-sim-clock { min-width: auto; padding: 6px 8px; }
  .exam-sim-clock small { display: none; }
  .exam-creation-exit { width: 40px; height: 40px; }
  .preview-mode-switch { width: 100%; }
  .preview-mode-switch button { flex: 1; justify-content: center; padding: 0 6px; font-size: 12px; }
  .exam-question-stage { min-height: 460px; padding: 16px; }
  .exam-question-content h3 { font-size: 16px; }
  .exam-question-meta { flex-wrap: wrap; }
  .exam-question-meta > span:nth-child(2) { margin-right: 0; }
  .exam-question-bottom { align-items: stretch; flex-direction: column; }
  .exam-question-bottom > div { display: grid; grid-template-columns: 1fr 1fr; }
  .exam-creation-footer { justify-content: stretch; }
  .exam-creation-footer > button { flex: 1; }
  .exam-builder-section { padding: 12px; }
  .exam-builder-part-fields { grid-template-columns: minmax(0,1fr); }
  .exam-builder-part-header { padding: 4px 8px; }
  .exam-builder-part-actions button { width: 36px; height: 36px; }
}
</style>
