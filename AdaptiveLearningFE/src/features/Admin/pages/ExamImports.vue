<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowLeft, Check, FileText, LoaderCircle, Save, Upload } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { adminService } from '@/services/adminService'
import { useUserStore } from '@/stores/user'
import { useConfirm } from '@/components/ui/overlay'

const router = useRouter()
const userStore = useUserStore()
const { confirm } = useConfirm()
const isAdmin = computed(() => userStore.state.role === 'ROLE_ADMIN')
const canReview = computed(() => userStore.state.role === 'ROLE_ADMIN' || userStore.state.canApproveExams)
const certificates = ref([])
const topics = ref([])
const imports = ref([])
const selected = ref(null)
const examFile = ref(null)
const answerFile = ref(null)
const examInput = ref(null)
const answerInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({ title: '', certificateId: '', topicId: '' })
const availableTopics = computed(() => topics.value.filter((topic) => String(topic.certificateId) === String(form.certificateId)))

watch(() => form.certificateId, async (certificateId) => {
  form.topicId = ''
  topics.value = certificateId ? await adminService.topics(certificateId).catch(() => []) : []
})

onMounted(async () => {
  if (isAdmin.value) certificates.value = await adminService.certificates().catch(() => [])
  if (canReview.value) await loadImports()
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
  if (file && (!file.name.toLowerCase().endsWith('.pdf') || file.size > 20 * 1024 * 1024)) {
    error.value = 'Chỉ nhận PDF tối đa 20 MB.'
    event.target.value = ''
    return
  }
  if (kind === 'exam') examFile.value = file
  else answerFile.value = file
  error.value = ''
}

async function uploadExam() {
  if (!examFile.value || !form.title.trim() || !form.certificateId || !form.topicId) {
    error.value = 'Cần nhập tên bộ đề, kỳ thi, môn/kỹ năng và PDF đề.'
    return
  }
  uploading.value = true
  error.value = ''
  notice.value = ''
  try {
    const body = new FormData()
    body.append('title', form.title.trim())
    body.append('certificateId', form.certificateId)
    body.append('topicId', form.topicId)
    body.append('examFile', examFile.value)
    if (answerFile.value) body.append('answerFile', answerFile.value)
    const created = await adminService.createExamImport(body)
    form.title = ''
    examFile.value = null
    answerFile.value = null
    examInput.value.value = ''
    answerInput.value.value = ''
    selected.value = created
    notice.value = created.answerGenerated
      ? 'Đã phân tích đề và tạo đáp án gợi ý bằng AI. Cần giáo viên kiểm tra trước khi xác nhận.'
      : 'Đã ghép đề với file đáp án. Giáo viên cần rà soát trước khi xác nhận.'
    await loadImports()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || exception?.response?.data?.message || 'Không thể nhập bộ đề.'
  } finally {
    uploading.value = false
  }
}

async function openImport(item) {
  error.value = ''
  notice.value = ''
  saving.value = true
  try {
    selected.value = await adminService.examImport(item.importId)
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không thể mở bộ đề.'
  } finally {
    saving.value = false
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
    selected.value = await adminService.updateExamImport(selected.value.importId, {
      questions: selected.value.questions,
    })
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
        <div class="section-heading"><div><h2>Tải đề nguồn</h2><p>File đáp án có thể để trống; khi thiếu, AI sẽ đề xuất đáp án để giáo viên rà soát.</p></div><Upload :size="19" /></div>
        <div class="form-grid">
          <label>Tên bộ đề<input v-model.trim="form.title" maxlength="200" placeholder="Ví dụ: Đề thử học kỳ 1" required /></label>
          <label>Kỳ thi<select v-model="form.certificateId" required><option value="" disabled>Chọn kỳ thi</option><option v-for="certificate in certificates" :key="certificate.certificateId" :value="String(certificate.certificateId)">{{ certificate.name }}</option></select></label>
          <label>Môn / kỹ năng<select v-model="form.topicId" :disabled="!availableTopics.length" required><option value="" disabled>Chọn môn hoặc kỹ năng</option><option v-for="topic in availableTopics" :key="topic.topicId" :value="String(topic.topicId)">{{ topic.name }}</option></select></label>
        </div>
        <div class="file-grid">
          <label class="file-field"><span>PDF đề <b>*</b></span><input ref="examInput" type="file" accept="application/pdf,.pdf" required @change="chooseFile($event, 'exam')" /><small>{{ examFile?.name || 'PDF có lớp văn bản, tối đa 20 MB' }}</small></label>
          <label class="file-field"><span>PDF đáp án <small>Không bắt buộc</small></span><input ref="answerInput" type="file" accept="application/pdf,.pdf" @change="chooseFile($event, 'answer')" /><small>{{ answerFile?.name || 'Đáp án chính thức đi kèm nếu có' }}</small></label>
        </div>
        <div class="form-actions"><button type="submit" :disabled="uploading"><LoaderCircle v-if="uploading" class="spin" :size="16" /><Upload v-else :size="16" />{{ uploading ? 'Đang phân tích PDF…' : 'Nhập và phân tích' }}</button></div>
      </form>
    </section>

    <section v-if="!canReview" class="no-permission"><h2>Chưa được cấp quyền duyệt đề</h2><p>Liên hệ quản trị viên để được gán quyền duyệt cho tài khoản giáo viên này.</p></section>

    <div v-else class="work-area" :class="{ 'review-only': !isAdmin }">
      <aside class="import-list">
        <div class="list-heading"><div><h2>Chờ giáo viên duyệt</h2><p>{{ imports.length }} bộ đề</p></div><button type="button" aria-label="Tải lại" @click="loadImports"><LoaderCircle v-if="loading" class="spin" :size="16" /><span v-else>↻</span></button></div>
        <button v-for="item in imports" :key="item.importId" class="import-row" :class="{ selected: selected?.importId === item.importId }" type="button" @click="openImport(item)">
          <FileText :size="18" /><span><strong>{{ item.title }}</strong><small>{{ item.questionCount }} câu · {{ item.answerGenerated ? 'AI gợi ý đáp án' : 'Có file đáp án' }}</small></span>
        </button>
        <p v-if="!loading && !imports.length" class="empty-list">Chưa có bộ đề cần duyệt.</p>
      </aside>

      <section v-if="selected" class="review-panel">
        <header class="review-heading"><div><p class="eyebrow">{{ selected.answerGenerated ? 'ĐÁP ÁN AI GỢI Ý · CẦN KIỂM TRA' : 'ĐÁP ÁN TỪ FILE ĐÍNH KÈM' }}</p><h2>{{ selected.title }}</h2><p>{{ selected.questionCount }} câu · {{ selected.questions?.length || 0 }} câu đang xem</p></div><span class="status-pill">Chờ xác nhận</span></header>
        <details class="source-details"><summary>Đọc nội dung PDF nguồn</summary><div><h3>Đề</h3><pre>{{ selected.sourceContent }}</pre><template v-if="selected.answerContent"><h3>Đáp án gốc</h3><pre>{{ selected.answerContent }}</pre></template></div></details>
        <article v-for="(question, index) in selected.questions" :key="index" class="question-editor" @vue:mounted="normalizeAnswer(question)">
          <header><span>{{ String(index + 1).padStart(2, '0') }}</span><label>Loại câu<select v-model="question.questionType" @change="changeQuestionType(question)"><option value="MULTIPLE_CHOICE">Trắc nghiệm</option><option value="TRUE_FALSE">Đúng / sai</option><option value="FILL_BLANK">Điền khuyết</option><option value="ESSAY">Tự luận</option></select></label><label>Độ khó<select v-model="question.difficulty"><option value="EASY">Dễ</option><option value="MEDIUM">Trung bình</option><option value="HARD">Khó</option></select></label></header>
          <label class="editor-label">Nội dung<textarea v-model.trim="question.content" rows="3" /></label>
          <div v-if="question.questionType === 'MULTIPLE_CHOICE'" class="options-editor">
            <div class="editor-subheading"><strong>Lựa chọn</strong><button type="button" @click="addOption(question)">Thêm lựa chọn</button></div>
            <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="option-row"><b>{{ option.label }}</b><input v-model.trim="option.text" /><button type="button" :aria-label="`Xóa lựa chọn ${option.label}`" @click="removeOption(question, optionIndex)">×</button></div>
            <label class="editor-label answer-field">Đáp án đúng<select v-model="question.correctAnswer"><option v-for="option in question.options" :key="option.label" :value="option.label">{{ option.label }} · {{ option.text }}</option></select></label>
          </div>
          <label v-else class="editor-label answer-field">Đáp án<textarea v-model.trim="question.correctAnswer" rows="2" /></label>
          <label class="editor-label">Giải thích<textarea v-model.trim="question.explanation" rows="2" /></label>
        </article>
        <footer class="review-actions"><button type="button" class="save-button" :disabled="saving" @click="saveReview"><LoaderCircle v-if="saving" class="spin" :size="16" /><Save v-else :size="16" />Lưu chỉnh sửa</button><button type="button" class="confirm-button" :disabled="saving" @click="confirmImport"><Check :size="16" />Xác nhận chính thức</button></footer>
      </section>
      <section v-else class="review-empty"><FileText :size="26" /><h2>Chọn một bộ đề</h2><p>Mở bộ đề để rà từng câu và xác nhận đáp án.</p></section>
    </div>
  </main>
</template>

<style scoped>
.exam-import-page { max-width: 1440px; margin: 0 auto; padding: 28px 32px 56px; color: #17202d; }
.page-heading, .page-heading > div, .section-heading, .list-heading, .review-heading, .review-actions, .form-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-heading { margin-bottom: 24px; }.page-heading > div { justify-content: flex-start; }.back-button { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid #dce2e9; border-radius: 6px; background: white; cursor: pointer; }
.eyebrow { margin: 0 0 5px; color: #547386; font-size: 11px; font-weight: 700; }.page-heading h1 { margin: 0; font-size: 25px; }.queue-count,.status-pill { color: #435565; font-size: 12px; }
.upload-section { margin-bottom: 26px; border-bottom: 1px solid #dce2e9; }.upload-form { max-width: 980px; padding-bottom: 24px; }.section-heading { margin-bottom: 18px; }.section-heading h2,.list-heading h2,.review-heading h2 { margin: 0; font-size: 17px; }.section-heading p,.list-heading p,.review-heading > div > p:last-child { margin: 5px 0 0; color: #64748b; font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr 1fr; gap: 12px; }.form-grid label,.editor-label,.file-field { display: grid; gap: 7px; color: #3a4757; font-size: 12px; font-weight: 600; }.form-grid input,.form-grid select,.editor-label textarea,.editor-label select,.option-row input,.question-editor select { min-width: 0; min-height: 40px; padding: 8px 10px; border: 1px solid #cbd5df; border-radius: 5px; background: white; color: #17202d; font: inherit; font-weight: 400; }
.file-grid { display: grid; max-width: 700px; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 14px; }.file-field { padding: 12px; border: 1px dashed #b9c6d1; border-radius: 5px; }.file-field b { color: #b42318; }.file-field small { color: #64748b; font-weight: 400; }.file-field input { max-width: 100%; font-size: 12px; }
.form-actions { justify-content: flex-end; margin-top: 14px; }.form-actions button,.review-actions button { display: inline-flex; min-height: 40px; align-items: center; gap: 8px; padding: 0 14px; border: 0; border-radius: 5px; background: #126b68; color: white; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }.form-actions button:disabled,.review-actions button:disabled { opacity: .55; cursor: wait; }
.work-area { display: grid; min-height: 420px; grid-template-columns: 290px minmax(0,1fr); gap: 22px; }.review-only { margin-top: 24px; }.import-list { border-right: 1px solid #dce2e9; padding-right: 16px; }.list-heading { margin-bottom: 12px; }.list-heading button { width: 32px; height: 32px; border: 1px solid #dce2e9; border-radius: 5px; background: white; cursor: pointer; }.import-row { display: flex; width: 100%; align-items: flex-start; gap: 10px; padding: 12px 8px; border: 0; border-bottom: 1px solid #e8edf1; background: transparent; color: #425366; text-align: left; cursor: pointer; }.import-row.selected { background: #edf6f5; color: #075e5c; }.import-row span { display: grid; min-width: 0; gap: 5px; }.import-row strong { overflow-wrap: anywhere; color: #1f2937; font-size: 13px; }.import-row small,.empty-list { color: #64748b; font-size: 11px; }.review-panel { min-width: 0; }.review-heading { align-items: flex-start; margin-bottom: 12px; }.review-heading h2 { font-size: 20px; }.review-heading .eyebrow { color: #a34c13; }.status-pill { padding: 5px 8px; border-radius: 4px; background: #fff4e5; color: #8a4b09; white-space: nowrap; }
.source-details { margin-bottom: 16px; border: 1px solid #dce2e9; border-radius: 5px; }.source-details summary { padding: 10px 12px; color: #435565; font-size: 12px; cursor: pointer; }.source-details > div { max-height: 260px; overflow: auto; padding: 0 12px 12px; }.source-details h3 { font-size: 12px; }.source-details pre { white-space: pre-wrap; overflow-wrap: anywhere; color: #475569; font-family: inherit; font-size: 12px; line-height: 1.5; }
.question-editor { margin: 0 0 12px; padding: 14px; border: 1px solid #dce2e9; border-radius: 5px; }.question-editor > header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }.question-editor > header > span { color: #126b68; font-size: 13px; font-weight: 700; }.question-editor > header label { display: flex; align-items: center; gap: 7px; color: #64748b; font-size: 11px; }.question-editor select { min-height: 34px; }.editor-label { margin-top: 11px; }.editor-label textarea { resize: vertical; line-height: 1.45; }.editor-subheading { display: flex; align-items: center; justify-content: space-between; margin: 12px 0 8px; color: #3a4757; font-size: 12px; }.editor-subheading button { border: 0; background: transparent; color: #126b68; font-size: 12px; cursor: pointer; }.option-row { display: grid; grid-template-columns: 28px minmax(0,1fr) 30px; gap: 8px; align-items: center; margin-bottom: 7px; }.option-row b { color: #126b68; }.option-row button { height: 32px; border: 0; background: transparent; color: #9b3028; font-size: 20px; cursor: pointer; }.answer-field { max-width: 520px; }.review-actions { justify-content: flex-end; margin-top: 18px; }.review-actions .save-button { border: 1px solid #cbd5df; background: white; color: #344256; }.review-actions .confirm-button { background: #126b68; }
.review-empty { display: grid; align-content: center; justify-items: center; min-height: 300px; color: #64748b; text-align: center; }.review-empty h2 { margin: 12px 0 0; color: #344256; font-size: 17px; }.review-empty p { font-size: 13px; }.feedback { padding: 10px 12px; border-radius: 4px; font-size: 13px; }.feedback.error { background: #fff1f0; color: #9f2f27; }.feedback.success { background: #eef8f2; color: #1e6a46; }.spin { animation: spin 1s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }
.no-permission { padding: 28px 0; border-top: 1px solid #dce2e9; color: #64748b; }.no-permission h2 { margin: 0; color: #344256; font-size: 17px; }.no-permission p { font-size: 13px; }
@media (max-width: 800px) { .exam-import-page { padding: 20px 16px 40px; }.form-grid { grid-template-columns: 1fr; }.work-area { grid-template-columns: 1fr; }.import-list { border-right: 0; border-bottom: 1px solid #dce2e9; padding: 0 0 14px; }.file-grid { grid-template-columns: 1fr; }.question-editor > header { flex-wrap: wrap; }.question-editor > header label { flex: 1 1 140px; justify-content: space-between; }.review-actions { flex-wrap: wrap; }.review-actions button { flex: 1; justify-content: center; } }
</style>
