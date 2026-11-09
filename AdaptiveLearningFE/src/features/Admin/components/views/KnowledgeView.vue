<script setup>
import { inject } from 'vue'
import { useRouter } from 'vue-router'
import {
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CircleCheck,
  CirclePause,
  Database,
  Eye,
  FileText,
  Info,
  LoaderCircle,
  MoreHorizontal,
  Pencil,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  Trash2,
  Upload,
  X
} from 'lucide-vue-next'
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from '@/components/ui/overlay'
import { adminContextKey } from '../../composables/adminContext'

const router = useRouter()
const admin = inject(adminContextKey)

const {
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
  programs,
  formatAdminNumber,
  readKnowledgeFiles,
  dropKnowledgeFiles,
  removeKnowledgeFile,
  clearKnowledgeForm,
  importKnowledge,
  closeKnowledgeNotice,
  resetKnowledgeFilters,
  programName,
  knowledgeTopicName,
  knowledgeDate,
  toggleKnowledgeActive,
  viewKnowledgeDocument,
  editKnowledgeDocument,
  removeKnowledge,
  openProgram,
  knowledgeChunkCount,
  activeKnowledgePercent,
  knowledgeForm,
  knowledgeFormErrors,
  knowledgeTopics,
  importingKnowledge,
  loadData
} = admin
</script>

<template>
<section  class="admin-knowledge-section">
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
                <button type="button" class="knowledge-open-exam-import" @click="router.push('/admin/exam-imports')">
                  <FileText :size="15" /> Nhập bộ đề thi
                </button>
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
</template>

<style scoped>
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
  .knowledge-open-exam-import { display: inline-flex; min-height: 36px; align-items: center; gap: 7px; padding: 0 11px; border: 1px solid #C9D9DD; border-radius: 5px; background: #F4FAFA; color: #176B68; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
  .knowledge-open-exam-import:hover { background: #E9F4F3; }
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
  .teacher-approval-toggle { display: inline-flex; align-items: center; gap: 7px; color: var(--text-secondary, #475569); font-size: 12px; white-space: nowrap; }
  .teacher-approval-toggle input { width: 16px; height: 16px; accent-color: var(--ds-primary, #2563EB); }
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

  @media (max-width: 900px) {
    .knowledge-workspace-grid { grid-template-columns: minmax(0, 1fr); }
    .knowledge-hero-panel { align-items: flex-start; flex-direction: column; padding: 20px; }
    .knowledge-hero-actions { width: 100%; flex-wrap: wrap; }
    .knowledge-panel-header { align-items: flex-start; flex-wrap: wrap; }
    .knowledge-panel-title-group { min-width: 0; }
    .knowledge-panel-title { overflow-wrap: anywhere; }
    .knowledge-panel-subtitle { overflow-wrap: anywhere; }
  }

  @media (max-width: 520px) {
    .knowledge-hero-panel { padding: 16px; }
    .knowledge-hero-title { font-size: 20px; }
    .knowledge-hero-actions > * { flex: 1 1 auto; justify-content: center; }
    .knowledge-panel-header { padding: 14px; }
    .knowledge-open-exam-import { width: 100%; justify-content: center; }
    .knowledge-queue-item { grid-template-columns: 32px minmax(0, 1fr) 28px; }
    .knowledge-queue-title-input-wrap { grid-column: 2 / 3; }
    .knowledge-queue-remove-btn { grid-column: 3; grid-row: 1; }
  }

</style>
