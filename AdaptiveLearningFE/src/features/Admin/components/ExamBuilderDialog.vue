<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ArrowDown, ArrowUp, Check, Plus, Search, Trash2, X } from 'lucide-vue-next'
import { adminService } from '@/services/adminService'

const props = defineProps({
  open: { type: Boolean, default: false },
  programs: { type: Array, default: () => [] },
  topics: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'saved'])
const stage = ref('builder')

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
}

function addSection() {
  form.sections.push({
    name: `Phần ${form.sections.length + 1}`,
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    questionCount: 10,
    pointsPerQuestion: 1,
  })
}

function removeSection(index) {
  if (form.sections.length <= 1) return
  form.sections.splice(index, 1)
  form.sections.forEach((section, sectionIndex) => {
    if (/^Phần \d+$/.test(section.name)) section.name = `Phần ${sectionIndex + 1}`
  })
}

function moveSection(index, direction) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.sections.length) return
  const [section] = form.sections.splice(index, 1)
  form.sections.splice(targetIndex, 0, section)
}

watch(() => props.open, (open) => {
  if (open) resetForm()
})

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
    for (const section of sections) {
      section.candidates = section.candidates.filter((question) => !selectedIds.has(question.questionId))
      if (section.candidates.length < section.form.questionCount) {
        throw new Error(`Phần ${section.form.name} cần ${section.form.questionCount} câu riêng biệt; hiện chỉ còn ${section.candidates.length} câu đã duyệt phù hợp.`)
      }
      section.selectedQuestionIds = section.candidates
        .slice(0, Number(section.form.questionCount))
        .map((question) => question.questionId)
      section.selectedQuestionIds.forEach((questionId) => selectedIds.add(questionId))
    }
    previewSections.value = sections
    stage.value = 'preview'
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
  await deactivateDraftTemplate()
  previewSections.value = []
  stage.value = 'builder'
  error.value = ''
}

async function closeDialog() {
  if (previewLoading.value || saving.value) return
  await deactivateDraftTemplate()
  emit('close')
}

async function publishExam() {
  if (saving.value || previewSections.value.some((section) => section.selectedQuestionIds.length !== Number(section.form.questionCount))) return
  error.value = ''
  saving.value = true
  try {
    await adminService.generateExamFromTemplate(templateId.value, {
      sectionDifficulties: form.sections.map((section) => section.difficulty),
      questionIdsBySection: previewSections.value.map((section) => section.selectedQuestionIds),
    })
    await deactivateDraftTemplate()
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
  <div v-if="open" class="exam-builder-backdrop" @click.self="closeDialog">
    <section class="exam-builder" role="dialog" aria-modal="true" aria-labelledby="exam-builder-title">
      <header class="exam-builder-header">
        <div><p class="exam-builder-kicker">QUẢN TRỊ ĐÁNH GIÁ</p><h2 id="exam-builder-title">{{ stage === 'preview' ? 'Xem trước bài thi' : 'Tạo bài kiểm tra' }}</h2><p>{{ stage === 'preview' ? 'Chọn chính xác các câu hỏi sẽ xuất hiện trong bài thi.' : 'Cấu hình một kỹ năng/môn thi và sắp xếp các phần của bài kiểm tra.' }}</p></div>
        <button class="exam-builder-icon-button" type="button" aria-label="Đóng" @click="closeDialog"><X :size="20" /></button>
      </header>

      <form class="exam-builder-form" @submit.prevent="stage === 'preview' ? publishExam() : loadPreview()">
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
          <section class="exam-builder-section preview-summary"><div class="exam-builder-section-heading"><span>03</span><div><h3>{{ form.title }}</h3><p>{{ selectedTopic?.name }} · {{ form.durationMinutes }} phút · {{ previewTotalPoints }} điểm · mục tiêu {{ form.targetScoreMin }}–{{ form.targetScoreMax }} {{ selectedTopic?.scoreUnit }}</p></div></div><p>Đã chọn câu hỏi đã duyệt theo từng phần. Có thể thay câu trước khi phát hành.</p></section>
          <section v-for="(section, sectionIndex) in previewSections" :key="sectionIndex" class="exam-builder-section preview-questions-section">
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

        <footer v-if="stage === 'builder'" class="exam-builder-footer"><button type="button" class="admin-secondary-button" @click="closeDialog">Hủy</button><button class="admin-primary-button" type="submit" :disabled="previewLoading || !selectedTopic || !hasTargetScale || form.targetScoreMin > form.targetScoreMax || form.passingScore > maxPoints"><Check :size="16" />{{ previewLoading ? 'Đang tải xem trước...' : 'Xem trước bài thi' }}</button></footer>
        <footer v-else class="exam-builder-footer"><button type="button" class="admin-secondary-button" :disabled="saving" @click="editConfiguration">Quay lại chỉnh sửa</button><button class="admin-primary-button" type="submit" :disabled="saving || previewSections.some((section) => section.selectedQuestionIds.length !== Number(section.form.questionCount))"><Check :size="16" />{{ saving ? 'Đang phát hành...' : 'Phát hành bài thi' }}</button></footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.exam-builder-backdrop { position: fixed; z-index: 100; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(14, 24, 38, .54); }
.exam-builder { display: flex; width: min(1080px, 100%); max-height: min(94vh, 960px); flex-direction: column; overflow: hidden; border: 1px solid #dce4e9; border-radius: 10px; background: #f7f9fb; box-shadow: 0 26px 85px rgba(9, 23, 39, .3); }
.exam-builder-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding: 23px 28px 19px; border-bottom: 1px solid #e0e7ed; background: #fff; }
.exam-builder-kicker { margin: 0 0 6px; color: #5e7180; font-size: 10px; font-weight: 700; }
.exam-builder-header h2 { margin: 0; color: #1e3343; font-size: 21px; }
.exam-builder-header p:last-child { margin: 7px 0 0; color: #7b8994; font-size: 12px; }
.exam-builder-icon-button { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border: 1px solid #e0e7ed; border-radius: 6px; background: #fff; color: #62727f; cursor: pointer; }
.exam-builder-form { display: flex; min-height: 0; flex: 1; flex-direction: column; }
.exam-builder-content { display: grid; gap: 16px; padding: 20px 28px; overflow: auto; }
.exam-builder-section { padding: 17px; border: 1px solid #e2e8ed; border-radius: 8px; background: #fff; }
.exam-builder-section-heading { display: flex; align-items: center; gap: 11px; margin-bottom: 17px; }
.exam-builder-section-heading > span { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border-radius: 6px; background: #eaf1f2; color: #3a6865; font-size: 10px; font-weight: 700; }
.exam-builder-section-heading > div { min-width: 0; flex: 1; }
.exam-builder-section-heading h3 { margin: 0; color: #263c4b; font-size: 14px; }
.exam-builder-section-heading p { margin: 4px 0 0; color: #7e8d98; font-size: 10px; }
.exam-builder-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 14px; }
.exam-builder label { display: grid; min-width: 0; gap: 6px; color: #51616d; font-size: 10px; font-weight: 600; }
.exam-builder input,.exam-builder select,.exam-builder textarea { width: 100%; min-width: 0; box-sizing: border-box; min-height: 38px; padding: 8px 10px; border: 1px solid #dce4e9; border-radius: 5px; background: #fff; color: #263c4b; font: inherit; font-size: 11px; outline: none; }
.exam-builder textarea { resize: vertical; }
.exam-builder input:focus,.exam-builder select:focus,.exam-builder textarea:focus { border-color: #3d8a7e; box-shadow: 0 0 0 3px rgba(61, 138, 126, .12); }
.exam-builder label small { color: #7b8994; font-size: 9px; font-weight: 400; }
.span-two { grid-column: span 2; }
.structure-section .exam-builder-section-heading { margin-bottom: 14px; }
.exam-builder-add-button { display: inline-flex; min-height: 34px; align-items: center; gap: 6px; padding: 0 10px; border: 1px solid #d8e4e0; border-radius: 5px; background: #f7fbf9; color: #28685e; font: inherit; font-size: 10px; font-weight: 650; cursor: pointer; }
.exam-builder-part { margin-top: 10px; border: 1px solid #e2e8ed; border-radius: 7px; background: #fbfcfd; }
.exam-builder-part-header { display: flex; min-height: 43px; align-items: center; gap: 9px; padding: 0 12px; border-bottom: 1px solid #e7ecef; }
.exam-builder-part-number { display: grid; width: 25px; height: 25px; place-items: center; border-radius: 50%; background: #edf2f4; color: #536875; font-size: 9px; font-weight: 700; }
.exam-builder-part-header > strong { flex: 1; color: #334a59; font-size: 11px; }
.exam-builder-part-actions { display: flex; gap: 4px; }
.exam-builder-part-actions button { display: grid; width: 27px; height: 27px; place-items: center; border: 0; border-radius: 4px; background: transparent; color: #6d7e89; cursor: pointer; }
.exam-builder-part-actions button:hover:not(:disabled) { background: #eaf1f2; color: #28685e; }
.exam-builder-part-actions button:disabled { color: #c5cdd2; cursor: not-allowed; }
.exam-builder-part-fields { display: grid; grid-template-columns: minmax(145px, 1.4fr) minmax(145px, 1.1fr) minmax(115px, .8fr) minmax(85px, .55fr) minmax(95px, .7fr); gap: 12px; padding: 13px; }
.preview-summary > p { margin: -7px 0 0 41px; color: #7e8d98; font-size: 10px; }
.preview-questions-section { padding-bottom: 12px; }
.preview-search { position: relative; display: flex !important; width: min(250px, 33%); min-height: 32px; align-items: center; gap: 6px !important; padding: 0 8px; border: 1px solid #dce4e9; border-radius: 5px; background: #fff; color: #788792 !important; }
.preview-search input { min-height: 30px; padding: 4px 0; border: 0; box-shadow: none !important; font-size: 10px !important; }
.preview-question-list { display: grid; gap: 6px; }
.preview-question-choice { display: flex !important; min-width: 0; align-items: flex-start; gap: 10px !important; padding: 10px; border: 1px solid #e2e8ed; border-radius: 6px; background: #fff; cursor: pointer; }
.preview-question-choice.selected { border-color: #70a79e; background: #f5faf8; }
.preview-question-choice > input { width: 15px; min-height: 15px; margin-top: 2px; accent-color: #178b7b; }
.preview-question-order { display: grid; width: 24px; height: 24px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: #eef2f4; color: #61717c; font-size: 9px; }
.preview-question-body { display: grid; min-width: 0; flex: 1; gap: 5px; color: #52616c; font-size: 10px; line-height: 1.5; }
.preview-question-body strong { color: #2c3f4d; font-size: 11px; font-weight: 600; white-space: pre-wrap; }
.preview-question-points { color: #607b75; font-size: 9px; white-space: nowrap; }
.preview-candidate-note { margin: 8px 0 0; color: #7c8a95; font-size: 9px; }
.exam-builder-error { margin: 0; padding: 10px 12px; border: 1px solid #f0d5d1; border-radius: 6px; background: #fff7f6; color: #a44f49; font-size: 11px; }
.exam-builder-scale-error { grid-column: 1 / -1; margin: -2px 0 0; color: #ad6755; font-size: 10px; }
.exam-builder-footer .admin-primary-button,.exam-builder-footer .admin-secondary-button { display: inline-flex; min-height: 38px; align-items: center; justify-content: center; gap: 7px; padding: 0 13px; border: 1px solid transparent; border-radius: 6px; font: inherit; font-size: 11px; font-weight: 650; cursor: pointer; }
.exam-builder-footer .admin-primary-button { background: #178b7b; color: #fff; }
.exam-builder-footer .admin-primary-button:hover:not(:disabled) { background: #117467; }
.exam-builder-footer .admin-primary-button:disabled { opacity: .55; cursor: not-allowed; }
.exam-builder-footer .admin-secondary-button { border-color: #dce4eb; background: #fff; color: #536779; }
.exam-builder-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 14px 28px; border-top: 1px solid #e0e7ed; background: #fff; }
@media (max-width: 760px) { .exam-builder-backdrop { padding: 8px; }.exam-builder { max-height: 96vh; }.exam-builder-header { padding: 17px 16px 14px; }.exam-builder-content { padding: 12px; }.exam-builder-section { padding: 12px; }.exam-builder-grid { grid-template-columns: minmax(0, 1fr); }.span-two { grid-column: auto; }.exam-builder-part-fields { grid-template-columns: repeat(2, minmax(0, 1fr)); padding: 10px; }.exam-builder-footer { padding: 12px 16px; } }
</style>
