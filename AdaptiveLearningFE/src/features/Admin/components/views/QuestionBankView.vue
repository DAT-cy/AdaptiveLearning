<script setup>
  import { computed, inject, onMounted, ref, watch } from 'vue'
  import {
    ChevronRight,
    FileText,
    Search,
    Sparkles,
  } from 'lucide-vue-next'
  import { adminService } from '@/services/adminService'
  import { adminContextKey } from '../../composables/adminContext'
  import ExamPreview from './ExamPreview.vue'
  import SkillPreview from './SkillPreview.vue'

  const admin = inject(adminContextKey)
  const questions = ref([])
  const certificates = ref([])
  const exams = ref([])
  const imports = ref([])
  const skills = ref([])
  const mode = ref('sets')
  const selectedCertificateId = ref('all')
  const selectedParent = ref(null)
  const selectedSkill = ref(null)
  const search = ref('')
  const loading = ref(false)
  const detailLoading = ref(false)
  const error = ref('')
  const preview = ref(null)
  const examDetails = ref({})
  const importDetails = ref({})
  const skillTree = ref([])
  const skillTreeLoading = ref(false)
  const skillTreeError = ref('')
  const skillPreview = ref(null)

  const asList = (value) =>
    Array.isArray(value)
      ? value
      : value?.data || value?.content || value?.items || []
  const programs = computed(
    () => admin?.programs?.value || admin?.programs || certificates.value
  )
  const visibleExams = computed(() => [
    ...exams.value
      .filter(
        (exam) =>
          selectedCertificateId.value === 'all' ||
          String(exam.certificateId) === String(selectedCertificateId.value)
      )
      .map((exam) => ({
        ...exam,
        setKind: 'exam',
        setId: `exam-${exam.examId}`,
      })),
    ...imports.value
      .filter(
        (item) =>
          selectedCertificateId.value === 'all' ||
          String(item.certificateId) === String(selectedCertificateId.value)
      )
      .map((item) => ({
        ...item,
        examId: item.importId,
        setKind: 'import',
        setId: `import-${item.importId}`,
      })),
  ])
  const visibleSkills = computed(() => {
    const unique = new Map()
    skills.value.forEach((skill) => {
      if ((selectedCertificateId.value === 'all' || String(skill.certificateId) === String(selectedCertificateId.value)) && skill.topicId != null) {
        unique.set(`${skill.certificateId}-${skill.topicId}`, skill)
      }
    })
    return [...unique.values()].filter((skill) => skillQuestionCount(skill) > 0)
  })
  const activeImportIds = computed(() => new Set(imports.value.map((item) => String(item.importId))))
  function isQuestionFromActiveImport(question) {
    if (!String(question.tags || '').includes('CONFIRMED_EXAM_IMPORT')) return true
    try {
      const metadata = typeof question.contentJson === 'string'
        ? JSON.parse(question.contentJson)
        : question.contentJson
      return metadata?.examImportId == null || activeImportIds.value.has(String(metadata.examImportId))
    } catch {
      return true
    }
  }
  function metadata(question) {
    try {
      return typeof question?.contentJson === 'string' ? JSON.parse(question.contentJson) : question?.contentJson || {}
    } catch {
      return {}
    }
  }
  function skillQuestionCount(skill) {
    return questions.value.filter((question) =>
      String(question.certificateId) === String(skill.certificateId) &&
      String(question.topicId) === String(skill.topicId) &&
      isQuestionFromActiveImport(question)
    ).length
  }
  const selectedQuestions = computed(() => {
    const query = search.value.trim().toLowerCase()
    const importedQuestions =
      selectedParent.value?.setKind === 'import'
        ? importDetails.value[selectedParent.value.importId]?.questions || []
        : null
    const sourceQuestions = importedQuestions || questions.value
    return sourceQuestions.filter((question) => {
      if (!isQuestionFromActiveImport(question)) return false
      const matchesText =
        !query ||
        `${question.content || ''} ${question.tags || ''}`
          .toLowerCase()
          .includes(query)
      const matchesExam =
        selectedParent.value?.setKind === 'exam'
          ? (
              examDetails.value[selectedParent.value.examId]?.questions || []
            ).some(
              (item) => String(item.questionId) === String(question.questionId)
            )
          : true
      return matchesText && matchesExam
    })
  })
  function examName(id) {
    return (
      programs.value.find((item) => String(item.certificateId) === String(id))
        ?.name || `Kỳ thi #${id}`
    )
  }
  function questionType(value) {
    return (
      {
        MULTIPLE_CHOICE: 'Trắc nghiệm',
        TRUE_FALSE: 'Đúng / Sai',
        ESSAY: 'Tự luận',
        FILL_BLANK: 'Điền vào chỗ trống',
        MATCHING: 'Nối đáp án',
        ORDERING: 'Sắp xếp',
      }[value] ||
      value ||
      'Câu hỏi'
    )
  }
  function difficulty(value) {
    return (
      { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' }[value] ||
      value ||
      'Trung bình'
    )
  }
  function status(value) {
    return (
      { APPROVED: 'Đã duyệt', PENDING: 'Chờ duyệt', REJECTED: 'Từ chối' }[
        value
      ] ||
      value ||
      'Chờ duyệt'
    )
  }
  function clearSelection() {
    selectedParent.value = null
    selectedSkill.value = null
    search.value = ''
    skillTree.value = []
    skillTreeError.value = ''
    skillPreview.value = null
  }
  async function loadData() {
    loading.value = true
    error.value = ''
    try {
      const [questionData, certificateData, examData, importData] =
        await Promise.all([
          adminService.questions(),
          adminService.certificates(),
          adminService.exams(),
          adminService.examSets(),
        ])
      questions.value = asList(questionData)
      certificates.value = asList(certificateData)
      exams.value = asList(examData)
      imports.value = asList(importData).filter((item) => item.active)
      await loadSkills()
    } catch (exception) {
      error.value =
        exception?.response?.data?.message || 'Không thể tải ngân hàng câu hỏi.'
    } finally {
      loading.value = false
    }
  }
  async function loadSkills() {
    const ids =
      selectedCertificateId.value === 'all'
        ? certificates.value.map((item) => item.certificateId).filter(Boolean)
        : [selectedCertificateId.value]
    try {
      skills.value = (
        await Promise.all(ids.map((id) => adminService.topics(id)))
      ).flatMap(asList)
    } catch {
      skills.value = []
    }
  }
  async function chooseSkill(skill) {
    selectedSkill.value = skill
    selectedParent.value = null
    skillPreview.value = skill
    skillTreeLoading.value = true
    skillTreeError.value = ''
    try {
      skillTree.value = await loadSkillTree(skill)
    } catch (exception) {
      skillTreeError.value = exception?.response?.data?.message || 'Không thể tải cây câu hỏi theo kỹ năng.'
      skillTree.value = []
    } finally {
      skillTreeLoading.value = false
    }
  }
  async function loadSkillTree(skill) {
    const matchesSkill = (question) => String(question?.topicId) === String(skill.topicId)
      && String(question?.certificateId ?? skill.certificateId) === String(skill.certificateId)
    const matchingQuestions = questions.value.filter((question) => matchesSkill(question) && isQuestionFromActiveImport(question))
    const importCandidates = imports.value.filter((item) => String(item.certificateId) === String(skill.certificateId))
    const examCandidates = exams.value.filter((item) => String(item.certificateId) === String(skill.certificateId))
    const [importResults, examResults] = await Promise.all([
      Promise.all(importCandidates.map(async (item) => ({ item, detail: importDetails.value[item.importId] || await adminService.examImport(item.importId) }))),
      Promise.all(examCandidates.map(async (item) => ({ item, detail: examDetails.value[item.examId] || await adminService.examDetail(item.examId) }))),
    ])
    importDetails.value = { ...importDetails.value, ...Object.fromEntries(importResults.map(({ item, detail }) => [item.importId, detail])) }
    examDetails.value = { ...examDetails.value, ...Object.fromEntries(examResults.map(({ item, detail }) => [item.examId, detail])) }
    const questionsByImportPosition = new Map()
    matchingQuestions.forEach((question) => {
      const source = metadata(question)
      if (source.examImportId != null && source.questionNumber != null) {
        questionsByImportPosition.set(`${source.examImportId}-${source.questionNumber}`, question)
      }
    })
    const sets = []
    importResults.forEach(({ item, detail }) => {
      const resolved = (detail?.questions || []).map((source, index) => questionsByImportPosition.get(`${item.importId}-${index + 1}`))
        .filter((question) => question && matchesSkill(question))
      if (resolved.length) sets.push({ key: `import-${item.importId}`, title: item.title || detail?.title || `Bộ đề #${item.importId}`, order: Number(item.importId), questions: resolved })
    })
    examResults.forEach(({ item, detail }) => {
      const resolved = (detail?.questions || []).filter(matchesSkill)
      if (resolved.length) sets.push({ key: `exam-${item.examId}`, title: item.title || detail?.title || `Bộ đề #${item.examId}`, order: Number(item.examId), questions: resolved })
    })
    return sets.map(buildSkillSetTree).sort((left, right) => left.order - right.order)
  }
  function buildSkillSetTree(set) {
    const sectionMap = new Map()
    set.questions.forEach((question, index) => {
      const source = metadata(question)
      const sectionOrder = question.sourceSectionOrder ?? source.sourceSectionOrder
      const sectionName = question.sectionName || source.sourceSectionTitle || 'Câu hỏi chưa phân section'
      const key = `${set.key}-section-${sectionOrder ?? sectionName}`
      if (!sectionMap.has(key)) sectionMap.set(key, { key, title: sectionName, order: Number(sectionOrder ?? Number.MAX_SAFE_INTEGER), questions: [] })
      sectionMap.get(key).questions.push({ ...question, questionOrder: question.questionOrder ?? source.questionNumber ?? index + 1 })
    })
    return {
      ...set,
      sections: [...sectionMap.values()].map((section) => ({ ...section, questions: section.questions.sort((left, right) => Number(left.questionOrder) - Number(right.questionOrder)) })).sort((left, right) => left.order - right.order),
    }
  }
  function closeSkillPreview() {
    selectedSkill.value = null
    skillPreview.value = null
    skillTree.value = []
    skillTreeError.value = ''
  }
  function openSkillQuestion(question, set) {
    preview.value = {
      title: `Câu hỏi ${question.questionOrder || ''}`.trim(),
      description: `${set.title} · ${selectedSkill.value?.name || 'Kỹ năng'}`,
      status: question.questionStatus || question.status || 'APPROVED',
      totalPoints: Number(question.pointsOverride ?? question.points ?? 0),
      questions: [question],
    }
    skillPreview.value = null
  }
  async function chooseExam(exam) {
    selectedParent.value = exam
    detailLoading.value = true
    try {
      if (exam.setKind === 'import') {
        const detail =
          importDetails.value[exam.importId] ||
          (await adminService.examImport(exam.importId))
        importDetails.value = {
          ...importDetails.value,
          [exam.importId]: detail,
        }
        openPreview(detail, true)
      } else {
        const detail =
          examDetails.value[exam.examId] ||
          (await adminService.examDetail(exam.examId))
        examDetails.value = { ...examDetails.value, [exam.examId]: detail }
        openPreview(detail)
      }
    } catch (exception) {
      error.value = exception?.response?.data?.message || 'Không thể tải bộ đề.'
    } finally {
      detailLoading.value = false
    }
  }
  function openPreview(detail, imported = false) {
    if (!detail) return
    preview.value = imported
      ? {
          ...detail,
          durationMinutes: 0,
          totalPoints: (detail.questions || []).reduce(
            (sum, item) => sum + Number(item.points || 0),
            0
          ),
          questions: (detail.questions || []).map((item, index) => ({
            ...item,
            questionId: item.questionId || `${detail.importId}-${index}`,
            questionOrder: index + 1,
            questionStatus: 'APPROVED',
          })),
        }
      : detail
  }
  watch(selectedCertificateId, async () => {
    clearSelection()
    await loadSkills()
  })
  onMounted(loadData)
</script>

<template>
  <section class="question-bank-page">
    <div class="browse-tabs">
      <button
        :class="{ active: mode === 'sets' }"
        @click="mode = 'sets'; clearSelection()"
      >
        <FileText :size="16" />
        Bộ đề thi
      </button>
      <button
        :class="{ active: mode === 'skills' }"
        @click="mode = 'skills'; clearSelection()"
      >
        <Sparkles :size="16" />
        Kỹ năng
      </button>
    </div>
    <div class="question-bank-toolbar">
      <label class="search-box">
        <Search :size="16" />
        <input
          v-model="search"
          type="search"
          placeholder="Tìm câu hỏi trong mục đang xem"
        />
      </label>
      <select v-model="selectedCertificateId" aria-label="Lọc theo kỳ thi">
        <option value="all">Tất cả kỳ thi</option>
        <option
          v-for="item in programs"
          :key="item.certificateId"
          :value="String(item.certificateId)"
        >
          {{ item.name }}
        </option>
      </select>
      <button
        class="reset-button"
        @click="selectedCertificateId = 'all'; clearSelection()"
      >
        Đặt lại
      </button>
    </div>
    <p v-if="error" class="state error" role="alert">{{ error }}</p>
    <div v-else-if="loading" class="state">Đang tải dữ liệu...</div>
    <template v-else-if="mode === 'sets' && !selectedParent">
      <div class="section-heading">
        <div>
          <h2>Danh sách bộ đề</h2>
          <p>{{ visibleExams.length }} bộ đề thuộc kỳ thi đang chọn</p>
        </div>
      </div>
      <div v-if="!visibleExams.length" class="state">
        <FileText :size="28" />
        <strong>Chưa có bộ đề</strong>
        <span>Không tìm thấy bộ đề trong kỳ thi này.</span>
      </div>
      <div v-else class="parent-grid">
        <button
          v-for="exam in visibleExams"
          :key="exam.setId"
          class="parent-card"
          @click="chooseExam(exam)"
        >
          <span class="parent-icon"><FileText :size="21" /></span>
          <span class="parent-copy">
            <strong>{{ exam.title }}</strong>
            <small>{{ examName(exam.certificateId) }}</small>
            <small>
              {{ exam.questionCount || exam.totalQuestions || 0 }} câu ·
              {{ exam.durationMinutes || 0 }} phút
            </small>
          </span>
          <ChevronRight :size="18" />
        </button>
      </div>
    </template>
    <template v-else-if="mode === 'skills' && !skillPreview">
      <div class="section-heading">
        <div>
          <h2>Danh sách kỹ năng</h2>
          <p>Chọn một kỹ năng để xem các câu hỏi theo đúng nhóm đề bài.</p>
        </div>
      </div>
      <div v-if="!visibleSkills.length" class="state">
        <Sparkles :size="28" />
        <strong>Chưa có kỹ năng</strong>
        <span>Không tìm thấy kỹ năng trong kỳ thi đang chọn.</span>
      </div>
      <div v-else class="parent-grid">
        <button
          v-for="skill in visibleSkills"
          :key="`${skill.certificateId}-${skill.topicId}`"
          class="parent-card"
          @click="chooseSkill(skill)"
        >
          <span class="parent-icon skill-icon"><Sparkles :size="21" /></span>
          <span class="parent-copy">
            <strong>{{ skill.name }}</strong>
            <small>{{ examName(skill.certificateId) }}</small>
            <small>
              {{ skillQuestionCount(skill) }}
              câu hỏi
            </small>
          </span>
          <ChevronRight :size="18" />
        </button>
      </div>
    </template>
    <template v-else>
      <div v-if="detailLoading" class="state">Đang tải chi tiết đề thi...</div>
    </template>
    <ExamPreview v-if="preview" :detail="preview" @close="preview = null" />
    <SkillPreview
      v-if="skillPreview"
      :skill="skillPreview"
      :exam-name="examName(skillPreview.certificateId)"
      :sets="skillTree"
      :loading="skillTreeLoading"
      :error="skillTreeError"
      @close="closeSkillPreview"
      @question="openSkillQuestion"
    />
  </section>
</template>

<style scoped>
  .question-bank-page {
    display: grid;
    gap: 16px;
  }
  .section-heading p,
  .detail-heading p {
    margin: 0;
    color: #64748b;
    font-size: 14px;
  }
  .outline-button,
  .reset-button,
  .back-button,
  .preview-button {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    min-height: 36px;
    padding: 0 13px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: #fff;
    color: #334155;
    cursor: pointer;
  }
  .question-bank-toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    padding: 14px;
    border: 1px solid #dbe4f0;
    border-radius: 10px;
    background: #fff;
  }
  .search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 240px;
    color: #94a3b8;
  }
  .search-box input {
    width: 100%;
    padding: 9px 0;
    border: 0;
    outline: 0;
    font: inherit;
  }
  .question-bank-toolbar select {
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid #dbe4f0;
    border-radius: 7px;
    color: #334155;
    background: #fff;
    font: inherit;
  }
  .browse-tabs {
    display: flex;
    gap: 8px;
    padding: 4px;
    border-bottom: 1px solid #dbe4f0;
  }
  .browse-tabs button {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    padding: 10px 14px;
    border: 0;
    border-bottom: 2px solid transparent;
    background: transparent;
    color: #64748b;
    font: inherit;
    cursor: pointer;
  }
  .browse-tabs button.active {
    border-color: #2563eb;
    color: #2563eb;
    font-weight: 700;
  }
  .section-heading,
  .detail-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }
  .section-heading h2,
  .detail-heading h2 {
    margin: 0 0 4px;
    color: #0f172a;
    font-size: 19px;
  }
  .detail-heading {
    padding: 8px 0;
  }
  .parent-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 12px;
  }
  .parent-card {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 106px;
    padding: 16px;
    border: 1px solid #dbe4f0;
    border-radius: 10px;
    background: #fff;
    color: #334155;
    text-align: left;
    cursor: pointer;
  }
  .parent-card:hover {
    border-color: #93c5fd;
    box-shadow: 0 3px 12px #2563eb12;
  }
  .parent-icon {
    display: grid;
    width: 42px;
    height: 42px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 9px;
    color: #2563eb;
    background: #eff6ff;
  }
  .skill-icon {
    color: #047857;
    background: #ecfdf5;
  }
  .parent-copy {
    display: grid;
    min-width: 0;
    flex: 1;
    gap: 4px;
  }
  .parent-copy strong {
    overflow: hidden;
    color: #0f172a;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .parent-copy small {
    color: #64748b;
    font-size: 12px;
  }
  .question-bank-list {
    display: grid;
    gap: 10px;
  }
  .question-bank-item {
    padding: 17px 20px;
    border: 1px solid #dbe4f0;
    border-radius: 10px;
    background: #fff;
  }
  .item-head,
  .meta {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    color: #334155;
    font-size: 13px;
  }
  .number {
    color: #2563eb;
    font-weight: 700;
  }
  .status {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-left: auto;
    color: #059669;
    font-size: 12px;
  }
  .content {
    margin: 12px 0;
    color: #0f172a;
    font-size: 15px;
    line-height: 1.55;
    white-space: pre-wrap;
  }
  .meta {
    color: #64748b;
    font-size: 12px;
  }
  .answer-box {
    margin-top: 13px;
    padding: 11px 13px;
    border: 1px solid #bbf7d0;
    border-radius: 7px;
    color: #166534;
    background: #f0fdf4;
    font-size: 13px;
    line-height: 1.5;
  }
  .answer-box p {
    margin: 6px 0 0;
  }
  .state {
    display: grid;
    justify-items: center;
    gap: 7px;
    padding: 64px 20px;
    border: 1px dashed #cbd5e1;
    border-radius: 10px;
    color: #64748b;
    background: #fff;
    text-align: center;
  }
  .error {
    color: #b91c1c;
    border-color: #fecaca;
  }
  @media (max-width: 760px) {
    .question-bank-toolbar,
    .detail-heading {
      align-items: stretch;
      flex-direction: column;
    }
    .parent-grid {
      grid-template-columns: 1fr;
    }
    .status {
      margin-left: 0;
    }
  }
</style>
