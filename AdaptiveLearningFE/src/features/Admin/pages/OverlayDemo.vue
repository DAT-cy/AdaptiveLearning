<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { AlertTriangle, Bell, Check, CheckCircle, CircleHelp, Database, Eye, FileText, Info, MoreHorizontal, Pencil, Trash2, Upload, X } from 'lucide-vue-next'
import {
  Drawer,
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  Modal,
  Popover,
  Tooltip,
  toast,
  useConfirm,
} from '@/components/ui/overlay'
import { adminService } from '@/services/adminService'

const { confirm } = useConfirm()
const documents = ref<any[]>([])
const programs = ref<any[]>([])
const topics = ref<any[]>([])
const selectedId = ref('')
const selectedDocument = ref<any>(null)
const loadingDocument = ref(false)
const saving = ref(false)
const previewOpen = ref(false)
const drawerOpen = ref(false)
const dialogOpen = ref(false)
const dialogSize = ref<'sm' | 'md' | 'lg' | 'xl'>('md')
const dialogVariant = ref<'default' | 'danger' | 'success'>('default')
const dirtyBaseline = ref('')
const demoDrawerRef = ref(null)
const editor = reactive({ title: '', certificateId: '', topicId: '', content: '' })
const chunks = computed(() => Array.isArray(selectedDocument.value?.chunks) ? selectedDocument.value.chunks : [])
const availableTopics = computed(() => topics.value.filter(
  (topic) => String(topic.certificateId) === editor.certificateId && topic.active !== false && topic.isActive !== false
))
const dirty = computed(() => Boolean(dirtyBaseline.value) && JSON.stringify(editor) !== dirtyBaseline.value)

async function loadData() {
  const [documentList, certificateList] = await Promise.all([
    adminService.knowledgeDocuments(),
    adminService.certificates(),
  ])
  documents.value = Array.isArray(documentList) ? documentList : []
  programs.value = Array.isArray(certificateList) ? certificateList : []
  const results = await Promise.allSettled(programs.value.map((program) => adminService.topics(program.certificateId)))
  topics.value = results.flatMap((result) => result.status === 'fulfilled' && Array.isArray(result.value) ? result.value : [])
  if (!selectedId.value && documents.value[0]) selectedId.value = String(documents.value[0].documentId)
}

async function loadDocument() {
  const document = documents.value.find((item) => String(item.documentId) === selectedId.value)
  if (!document) {
    selectedDocument.value = null
    return null
  }
  loadingDocument.value = true
  try {
    selectedDocument.value = await adminService.knowledgeDocument(document.documentId)
    return selectedDocument.value
  } finally {
    loadingDocument.value = false
  }
}

function openPreview() {
  if (!selectedId.value) return toast.info('Chọn tài liệu', { description: 'Kho hiện chưa có tài liệu để xem.' })
  previewOpen.value = true
  void loadDocument()
}

async function openEditor() {
  const detail = selectedDocument.value || await loadDocument()
  if (!detail) return
  Object.assign(editor, {
    title: detail.title,
    certificateId: String(detail.certificateId || ''),
    topicId: detail.topicId == null ? '' : String(detail.topicId),
    content: detail.content || '',
  })
  dirtyBaseline.value = JSON.stringify(editor)
  drawerOpen.value = true
}

async function saveEditor() {
  if (!selectedDocument.value || saving.value) return
  saving.value = true
  try {
    await adminService.updateKnowledgeDocument(selectedDocument.value.documentId, {
      title: editor.title.trim(),
      certificateId: Number(editor.certificateId),
      topicId: editor.topicId ? Number(editor.topicId) : null,
      content: editor.content,
    })
    await loadData()
    drawerOpen.value = false
    dirtyBaseline.value = ''
    toast.success('Đã lưu thay đổi', { description: editor.title.trim() })
  } catch (error: any) {
    toast.error('Không thể cập nhật tài liệu', { description: error?.response?.data?.message || error?.response?.data?.responseMessage })
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  const document = selectedDocument.value || await loadDocument()
  if (!document) return
  const ok = await confirm({
    title: 'Xóa tài liệu?',
    description: `“${document.title}” và ${document.chunkCount} đoạn kiến thức sẽ bị xóa khỏi kho.`,
    confirmText: 'Xóa tài liệu',
    cancelText: 'Giữ lại',
    variant: 'danger',
  })
  if (!ok) return
  try {
    await adminService.deleteKnowledgeDocument(document.documentId)
    await loadData()
    selectedDocument.value = null
    toast.success('Đã xóa tài liệu', { description: document.title })
  } catch (error: any) {
    toast.error('Không thể xóa tài liệu', { description: error?.response?.data?.responseMessage })
  }
}

async function showConfirmation(variant: 'default' | 'danger') {
  const confirmed = await confirm({
    title: variant === 'danger' ? 'Xóa tài liệu mẫu?' : 'Xác nhận thao tác?',
    description: 'Nút Hủy được focus mặc định để tránh thao tác nguy hiểm ngoài ý muốn.',
    confirmText: variant === 'danger' ? 'Xóa' : 'Xác nhận',
    cancelText: 'Hủy',
    variant,
  })
  toast.info(confirmed ? 'Đã xác nhận' : 'Đã hủy', {
    description: confirmed ? 'Hộp xác nhận trả về true.' : 'Hộp xác nhận trả về false.',
  })
}

onMounted(() => { void loadData() })
</script>

<template>
  <main class="overlay-demo-page">
    <header class="overlay-demo-heading">
      <p class="overlay-demo-breadcrumb">Quản trị <span>/</span> Thành phần giao diện</p>
      <h1>Thư viện lớp phủ</h1>
      <p>Ví dụ đang chạy với cùng các lớp phủ dùng trong ứng dụng.</p>
    </header>

    <section class="overlay-demo-section">
      <h2>Modal</h2>
      <div class="overlay-demo-controls">
        <label>Kích thước<select v-model="dialogSize"><option value="sm">sm</option><option value="md">md</option><option value="lg">lg</option><option value="xl">xl</option></select></label>
        <label>Biến thể<select v-model="dialogVariant"><option value="default">Mặc định</option><option value="danger">Nguy hiểm</option><option value="success">Thành công</option></select></label>
        <button class="overlay-button overlay-button-primary" @click="dialogOpen = true">Mở modal</button>
      </div>
      <Modal v-model:open="dialogOpen" title="Modal dùng chung" description="Kích thước và biến thể được đổi từ các điều khiển bên cạnh." :size="dialogSize" :variant="dialogVariant" footer>
        <p class="overlay-demo-copy">Modal này dùng Dialog primitive của Reka UI. Có thể đóng bằng Esc, bấm ngoài hoặc nút đóng; focus được giữ trong modal và trả về nút mở khi đóng.</p>
        <template #footer><button class="overlay-button overlay-button-secondary" @click="dialogOpen = false">Hủy</button><button class="overlay-button overlay-button-primary" @click="dialogOpen = false"><Check :size="16" />Xong</button></template>
      </Modal>
    </section>

    <section class="overlay-demo-section">
      <h2>Xác nhận và Toast</h2>
      <div class="overlay-demo-controls">
        <button class="overlay-button overlay-button-secondary" @click="showConfirmation('default')">Xác nhận mặc định</button>
        <button class="overlay-button overlay-button-danger" @click="showConfirmation('danger')"><AlertTriangle :size="16" />Xác nhận nguy hiểm</button>
        <label class="overlay-demo-document">Tài liệu mẫu<select v-model="selectedId"><option disabled value="">Chọn tài liệu</option><option v-for="document in documents" :key="document.documentId" :value="String(document.documentId)">{{ document.title }}</option></select></label>
        <button class="overlay-button overlay-button-danger" @click="confirmDelete"><Trash2 :size="16" />Xác nhận xóa tài liệu</button>
        <button class="overlay-button overlay-button-primary" @click="toast.success('Lưu thành công', { description: 'Thông tin đã được cập nhật.' })">Toast thành công</button>
        <button class="overlay-button overlay-button-secondary" @click="toast.info('Có cập nhật mới', { description: 'Nội dung được đồng bộ từ máy chủ.' })">Toast thông tin</button>
        <button class="overlay-button overlay-button-danger" @click="toast.error('Không thể kết nối', { description: 'Máy chủ chưa phản hồi.' })">Toast lỗi</button>
      </div>
    </section>

    <section class="overlay-demo-section">
      <h2>Kho kiến thức</h2>
      <p class="overlay-demo-copy">Dùng tài liệu thật trong kho hiện tại để xem preview đoạn, xóa có xác nhận và sửa bằng Drawer.</p>
      <div class="overlay-demo-controls">
        <label class="overlay-demo-document">Tài liệu mẫu<select v-model="selectedId"><option disabled value="">Chọn tài liệu</option><option v-for="document in documents" :key="document.documentId" :value="String(document.documentId)">{{ document.title }}</option></select></label>
        <button class="overlay-button overlay-button-secondary" :disabled="!documents.length" @click="openPreview"><Eye :size="16" />Xem preview</button>
        <button class="overlay-button overlay-button-secondary" :disabled="!documents.length" @click="openEditor"><Pencil :size="16" />Sửa bằng Drawer</button>
        <button class="overlay-button overlay-button-danger" :disabled="!documents.length" @click="confirmDelete"><Trash2 :size="16" />Xóa có xác nhận</button>
      </div>
      <p v-if="!documents.length" class="overlay-demo-empty">Kho hiện chưa có tài liệu. Các ví dụ thao tác tài liệu sẽ dùng dữ liệu ngay khi có PDF.</p>
    </section>

    <section class="overlay-demo-section overlay-demo-utilities">
      <h2>Popover, menu và Tooltip</h2>
      <div class="overlay-demo-controls">
        <Popover arrow>
          <template #trigger><button class="overlay-button overlay-button-secondary">Mở Popover</button></template>
          <strong>Tùy chọn nhanh</strong><p class="overlay-demo-copy">Popover tự lật vị trí khi chạm cạnh viewport.</p>
        </Popover>
        <DropdownMenu>
          <template #trigger><button class="overlay-button overlay-button-secondary"><MoreHorizontal :size="17" />Menu thao tác</button></template>
          <DropdownMenuLabel>Tài liệu</DropdownMenuLabel>
          <DropdownMenuItem @select="openPreview"><template #icon><Eye :size="16" /></template>Xem nội dung</DropdownMenuItem>
          <DropdownMenuItem @select="openEditor"><template #icon><Pencil :size="16" /></template>Sửa tài liệu</DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem destructive @select="confirmDelete"><template #icon><Trash2 :size="16" /></template>Xóa tài liệu</DropdownMenuItem>
        </DropdownMenu>
        <Tooltip label="Nút thao tác icon có tooltip và aria-label">
          <template #trigger><button class="overlay-icon-button" aria-label="Thông tin thêm"><Info :size="18" /></button></template>
        </Tooltip>
        <button class="overlay-button overlay-button-secondary" :disabled="!documents.length" @click="openEditor">Drawer sửa có cảnh báo chưa lưu</button>
      </div>
    </section>

    <Modal v-model:open="previewOpen" size="lg" title="Xem trước đoạn kiến thức" :description="selectedDocument ? `${selectedDocument.title} · ${selectedDocument.fileName} · ${chunks.length} đoạn` : 'Nội dung các đoạn đã tách.'" :icon="FileText">
      <div v-if="loadingDocument" class="overlay-demo-loading">Đang tải đoạn kiến thức…</div>
      <ol v-else class="overlay-demo-chunks"><li v-for="(chunk,index) in chunks" :key="index"><span>{{ String(index + 1).padStart(2,'0') }}</span><p>{{ chunk }}</p></li></ol>
      <template #footer><button class="overlay-button overlay-button-secondary" @click="previewOpen = false">Đóng</button><button v-if="selectedDocument" class="overlay-button overlay-button-primary" @click="previewOpen = false; openEditor()"><Pencil :size="16" />Sửa tài liệu</button></template>
    </Modal>

    <Drawer ref="demoDrawerRef" :open="drawerOpen" title="Sửa tài liệu" :description="editor.title || 'Thay đổi thông tin và nội dung tài liệu.'" :dirty="dirty" :submitting="saving" size="lg" @update:open="drawerOpen = $event">
      <form id="overlay-demo-edit-form" class="overlay-demo-form" @submit.prevent="saveEditor">
        <label>Tên tài liệu<input v-model.trim="editor.title" required maxlength="200" /></label>
        <label>Kỳ thi<select v-model="editor.certificateId" required><option disabled value="">Chọn kỳ thi</option><option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">{{ program.name }}</option></select></label>
        <label>Kỹ năng / môn thi<select v-model="editor.topicId"><option value="">Tất cả / tự động</option><option v-for="topic in topics.filter(item => String(item.certificateId) === editor.certificateId)" :key="topic.topicId" :value="String(topic.topicId)">{{ topic.name }}</option></select></label>
        <label>Nội dung<textarea v-model="editor.content" required maxlength="500000" rows="14"></textarea></label>
      </form>
      <template #footer><button type="button" class="overlay-button overlay-button-secondary" :disabled="saving" @click="demoDrawerRef?.requestClose()">Hủy</button><button type="submit" form="overlay-demo-edit-form" class="overlay-button overlay-button-primary" :disabled="saving"><Check :size="16" />{{ saving ? 'Đang lưu…' : 'Lưu thay đổi' }}</button></template>
    </Drawer>
  </main>
</template>

<style scoped>
.overlay-demo-page { display: grid; width: min(100%,1200px); gap: 20px; margin: 0 auto; padding: 28px clamp(16px,3vw,40px) 56px; color: var(--text); font-family: var(--font-sans); }
.overlay-demo-heading { display: grid; gap: 6px; }
.overlay-demo-breadcrumb { margin: 0; color: var(--text-muted); font-size: 13px; }
.overlay-demo-breadcrumb span { padding: 0 6px; }
.overlay-demo-breadcrumb span + * { color: var(--ds-primary); }
.overlay-demo-heading h1 { margin: 0; font-size: 28px; line-height: 36px; }
.overlay-demo-heading > p:last-child,.overlay-demo-copy { margin: 0; color: var(--text-muted); font-size: 14px; }
.overlay-demo-section { display: grid; min-width: 0; gap: 14px; padding: 20px; border: 1px solid var(--ds-border); border-radius: var(--radius-3xl); background: var(--surface); box-shadow: var(--shadow-card); }
.overlay-demo-section h2 { margin: 0; color: var(--text-strong); font-size: 17px; line-height: 24px; }
.overlay-demo-controls { display: flex; min-width: 0; align-items: flex-end; flex-wrap: wrap; gap: 10px; }
.overlay-demo-controls label { display: grid; min-width: 180px; gap: 6px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
.overlay-demo-controls select,.overlay-demo-form input,.overlay-demo-form select,.overlay-demo-form textarea { min-width: 0; min-height: 44px; padding: 0 14px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text); background: var(--surface); font: inherit; font-size: 14px; }
.overlay-demo-document { flex: 1 1 280px; }
.overlay-demo-empty { color: var(--text-muted); font-size: 13px; }
.overlay-demo-loading { display: flex; min-height: 120px; align-items: center; justify-content: center; gap: 10px; color: var(--text-muted); }
.overlay-demo-chunks { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.overlay-demo-chunks li { display: grid; grid-template-columns: 36px minmax(0,1fr); gap: 12px; padding: 12px; border: 1px solid var(--border-item); border-radius: var(--radius-xl); background: var(--surface); }
.overlay-demo-chunks li > span { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 9px; color: var(--blue-ink); background: var(--blue-soft); font-size: 13px; font-weight: 700; }
.overlay-demo-chunks p { margin: 3px 0 0; color: var(--text-secondary); font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.overlay-demo-form { display: grid; gap: 20px; }
.overlay-demo-form label { display: grid; gap: 6px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
.overlay-demo-form textarea { min-height: 220px; padding: 12px 14px; line-height: 1.6; resize: vertical; }
.overlay-icon-button { display: grid; width: 44px; height: 44px; place-items: center; border: 0; border-radius: var(--radius-lg); color: var(--text-secondary); background: var(--surface-hover); }
@media (max-width: 640px) { .overlay-demo-page { gap: 14px; padding-top: 20px; }.overlay-demo-section { padding: 16px; }.overlay-demo-controls { align-items: stretch; }.overlay-demo-controls > * { flex: 1 1 100%; }.overlay-demo-controls label { min-width: 0; }.overlay-demo-controls .overlay-demo-document { flex-basis: 100%; } }
</style>
