<script setup>
  import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
  import {
    ChevronDown,
    ChevronRight,
    Eye,
    EyeOff,
    Filter,
    Search,
    X,
  } from 'lucide-vue-next'

  const props = defineProps({ detail: { type: Object, required: true }, editable: { type: Boolean, default: false }, saving: { type: Boolean, default: false } })
  const emit = defineEmits(['close', 'save'])
  const mode = ref('preview')
  const query = ref('')
  const section = ref('all')
  const type = ref('all')
  const difficulty = ref('all')
  const media = ref('all')
  const activeId = ref(null)
  const flashId = ref(null)
  let flashTimer = null
  const advanced = ref(false)
  const questions = computed(() =>
    (props.detail.questions || [])
      .slice()
      .map((item, index) => {
        const parsed = parse(item.contentJson, {})
        const sourceNumber = item.sourceQuestionNumber || parsed.sourceQuestionNumber
        const numberMatch = String(sourceNumber || '').match(/\d+/)
        const rawContent = item.content || item.questionText || item.stem || item.prompt || parsed.content || ''
        const contentNumberMatch = String(rawContent).match(/^\s*(\d+)\s*[.)\-:]\s+/)
        const normalizedContent = contentNumberMatch
          ? String(rawContent).replace(/^\s*\d+\s*[.)\-:]\s+/, '')
          : rawContent
        const displayNumber = contentNumberMatch && numberMatch && contentNumberMatch[1] !== numberMatch[0]
          ? Number(contentNumberMatch[1])
          : (numberMatch ? Number(numberMatch[0]) : index + 1)
        return {
          ...parsed,
          ...item,
          questionId: item.questionId ?? `preview-question-${index}`,
          // Normalize OCR metadata to the fields used by the preview UI.
          sectionName: item.sectionName || parsed.sectionName || item.sourceSectionTitle || parsed.sourceSectionTitle || '',
          questionOrder: item.questionOrder && !contentNumberMatch
            ? item.questionOrder
            : displayNumber,
          content: normalizedContent,
        }
      })
      .sort(
        (a, b) => Number(a.questionOrder || 0) - Number(b.questionOrder || 0)
      )
  )
  const sections = computed(() => [
    ...new Set(questions.value.map((item) => item.sectionName).filter(Boolean)),
  ])
  const types = computed(() => [
    ...new Set(
      questions.value.map((item) => item.questionType).filter(Boolean)
    ),
  ])
  const filteredQuestions = computed(() =>
    questions.value.filter((item) => {
      const text = `${item.content || ''} ${item.tags || ''}`.toLowerCase()
      const hasMedia = /image|audio|video/i.test(
        `${item.contentJson || ''} ${item.optionsJson || ''}`
      )
      return (
        (!query.value || text.includes(query.value.toLowerCase())) &&
        (section.value === 'all' || item.sectionName === section.value) &&
        (type.value === 'all' || item.questionType === type.value) &&
        (difficulty.value === 'all' || item.difficulty === difficulty.value) &&
        (media.value === 'all' ||
          (media.value === 'yes' ? hasMedia : !hasMedia))
      )
    })
  )
  const sectionGroups = computed(() => {
    const sections = []
    const sectionMap = new Map()
    filteredQuestions.value.forEach((item) => {
      const sectionKey =
        item.sourceSectionOrder != null
          ? `s-${item.sourceSectionOrder}`
          : `s-${item.sectionName || 'default'}`
      let current = sectionMap.get(sectionKey)
      if (!current) {
        current = {
          key: sectionKey,
          title:
            item.sourceSectionTitle || item.sectionName || 'Câu hỏi độc lập',
          instruction: item.sectionInstructions,
          groups: [],
          groupMap: new Map(),
        }
        sectionMap.set(sectionKey, current)
        sections.push(current)
      }
      const stimulus = item.groupStimulusContent || item.stimulusContent
      const groupKey =
        item.sourceGroupOrder != null
          ? `${sectionKey}-g-${item.sourceGroupOrder}`
          : `${sectionKey}-g-${stimulus || 'independent'}`
      let group = current.groupMap.get(groupKey)
      if (!group) {
        group = {
          key: groupKey,
          title:
            item.sourceGroupTitle ||
            (stimulus ? 'Nội dung nguồn' : 'Câu hỏi độc lập'),
          stimulus,
          instruction: item.groupInstruction,
          questions: [],
        }
        current.groupMap.set(groupKey, group)
        current.groups.push(group)
      }
      group.questions.push(item)
    })
    return sections
  })
  function parse(value, fallback = []) {
    try {
      const result = typeof value === 'string' ? JSON.parse(value) : value
      return result == null ? fallback : result
    } catch {
      return fallback
    }
  }
  function options(item) {
    if (Array.isArray(item.options)) {
      const normalized = item.options.map((option, index) => ({
        key: option.key || option.label || String.fromCharCode(65 + index),
        value: option.value || option.text || option.label || option,
      }))
      const seen = new Set()
      return normalized.filter((option) => {
        const key = String(option.value || '').trim().toLowerCase().replace(/\s+/g, ' ')
        if (!key || seen.has(key)) return false
        seen.add(key)
        return true
      })
    }
    const result = parse(item.optionsJson)
    return Array.isArray(result)
      ? result
      : Object.entries(result || {}).map(([key, value]) => ({ key, value }))
  }
  function mediaItems(item) {
    const result = parse(item.contentJson, {})
    return Array.isArray(result)
      ? result
      : Object.values(result || {}).filter(
          (value) =>
            typeof value === 'string' &&
            /\.(png|jpe?g|gif|mp3|wav|ogg|mp4|webm)/i.test(value)
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
  function difficultyLabel(value) {
    return (
      { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' }[value] || value || '—'
    )
  }
  function statusLabel(value) {
    return (
      {
        APPROVED: 'Đã duyệt',
        PENDING: 'Chờ duyệt',
        REJECTED: 'Từ chối',
        DRAFT: 'Bản nháp',
        PUBLISHED: 'Đã phát hành',
        ARCHIVED: 'Đã lưu trữ',
      }[value] ||
      value ||
      '—'
    )
  }
  const status = statusLabel
  function goTo(item) {
    activeId.value = item.questionId
    flashId.value = item.questionId
    if (flashTimer) window.clearTimeout(flashTimer)
    flashTimer = window.setTimeout(() => {
      flashId.value = null
    }, 900)
    nextTick(() => {
      const target = document.getElementById(`preview-question-${item.questionId}`)
        || document.getElementById(`question-detail-${item.questionId}`)
      target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }
  onUnmounted(() => {
    if (flashTimer) window.clearTimeout(flashTimer)
  })
  watch(
    () => props.detail,
    () => {
      activeId.value = null
    },
    { immediate: true }
  )
</script>

<template>
  <div class="preview-backdrop" role="dialog" aria-modal="true">
    <div class="preview-shell">
      <header class="preview-header">
        <div>
          <span class="eyebrow">XEM TRƯỚC BỘ ĐỀ</span>
          <h2>{{ detail.title || 'Chi tiết câu hỏi' }}</h2>
          <p>
            {{ detail.description || 'Xem cấu trúc đề thi ở chế độ chỉ đọc.' }}
          </p>
        </div>
        <button
          class="icon-button"
          title="Đóng bản xem trước"
          @click="emit('close')"
        >
          <X :size="20" />
        </button>
        <button v-if="editable" class="save-preview-button" type="button" :disabled="saving" @click="emit('save')">
          {{ saving ? 'Đang lưu...' : 'Lưu chỉnh sửa' }}
        </button>
      </header>
      <div class="preview-summary">
        <span>{{ detail.durationMinutes || 0 }} phút</span>
        <span>{{ questions.length }} câu hỏi</span>
        <span>{{ detail.totalPoints || 0 }} điểm</span>
        <span>{{ sections.length }} phần</span>
        <span class="summary-status">
          {{ status(detail.status || 'DRAFT') }}
        </span>
      </div>
      <div class="mode-switch">
        <button
          :class="{ active: mode === 'preview' }"
          @click="mode = 'preview'"
        >
          <Eye :size="15" />
          Xem trước đề thi
        </button>
        <button
          :class="{ active: mode === 'inspection' }"
          @click="mode = 'inspection'"
        >
          <Filter :size="15" />
          Kiểm tra quản trị
        </button>
      </div>
      <div class="preview-layout">
        <aside class="navigator">
          <div class="navigator-title">
            <strong>Điều hướng câu hỏi</strong>
            <span>{{ filteredQuestions.length }}</span>
          </div>
          <details v-for="sectionGroup in sectionGroups" :key="sectionGroup.key" class="navigator-section" open>
            <summary class="navigator-section-title">
              <strong>{{ sectionGroup.title }}</strong>
              <small>{{ sectionGroup.groups.reduce((total, group) => total + group.questions.length, 0) }} câu</small>
            </summary>
            <details v-for="group in sectionGroup.groups" :key="group.key" class="navigator-group" open>
              <summary class="navigator-group-title">
                <span class="tree-branch" aria-hidden="true"></span>
                <span>{{ group.title }}</span>
                <small>{{ group.questions.length }}</small>
              </summary>
              <button
                v-for="item in group.questions"
                :key="item.questionId"
                type="button"
                class="navigator-question"
                :class="{ current: activeId === item.questionId }"
                @click="goTo(item)"
              >
                <span class="tree-branch" aria-hidden="true"></span>
                <b>{{ item.questionOrder || '?' }}</b>
                <small>{{ item.content || 'Câu hỏi' }}</small>
              </button>
            </details>
          </details>
        </aside>
        <main class="preview-main">
          <div class="exam-detail-title">Chi tiết bài thi</div>
          <div v-for="sectionGroup in sectionGroups" :key="sectionGroup.key" class="exam-structure">
            <header class="exam-section-heading"><span>SECTION</span><h3>{{ sectionGroup.title }}</h3><p v-if="sectionGroup.instruction" class="section-instruction">{{ sectionGroup.instruction }}</p></header>
            <section v-for="group in sectionGroup.groups" :key="group.key" class="exam-passage-group">
              <div class="passage-heading"><strong>{{ group.title }}</strong></div><div v-if="group.instruction" class="group-instruction">{{ group.instruction }}</div>
              <div v-if="group.stimulus" class="passage-content">{{ group.stimulus }}</div>
              <div v-for="item in group.questions" :id="`preview-question-${item.questionId}`" :key="item.questionId" class="structured-question" :class="{ highlight: activeId === item.questionId, 'flash-highlight': flashId === item.questionId }" @click="activeId = item.questionId">
                <strong>Câu {{ item.questionOrder || '?' }}</strong>
                <textarea v-if="editable" v-model="item.content" class="editable-question-field" rows="3" />
                <p v-else>{{ item.content }}</p>
                <div v-if="options(item).length" class="options"><div v-for="(option, index) in options(item)" :key="index" class="option"><span>{{ option.key || String.fromCharCode(65 + index) }}</span><span>{{ option.value || option.text || option.label || option }}</span></div></div>
                <div class="inspection"><label v-if="editable">Đáp án <input v-model="item.correctAnswer" class="editable-answer-field" /></label><span v-else>Đáp án: {{ item.correctAnswer || 'Chưa có đáp án' }}</span><span>Điểm: {{ item.points || 0 }}</span></div>
              </div>
            </section>
          </div>
          <div class="filter-bar">
            <label>
              <Search :size="15" />
              <input v-model="query" placeholder="Tìm trong câu hỏi" />
            </label>
            <select v-model="section">
              <option value="all">Tất cả phần</option>
              <option v-for="item in sections" :key="item" :value="item">
                {{ item }}
              </option>
            </select>
            <select v-model="type">
              <option value="all">Tất cả dạng câu hỏi</option>
              <option v-for="item in types" :key="item" :value="item">
                {{ questionType(item) }}
              </option>
            </select>
            <button class="advanced-toggle" @click="advanced = !advanced">
              Nâng cao
              <ChevronDown :size="14" />
            </button>
          </div>
          <div v-if="advanced" class="advanced-filters">
            <select v-model="difficulty">
              <option value="all">Tất cả độ khó</option>
              <option value="EASY">Dễ</option>
              <option value="MEDIUM">Trung bình</option>
              <option value="HARD">Khó</option>
            </select>
            <select v-model="media">
              <option value="all">Tất cả nội dung đa phương tiện</option>
              <option value="yes">Có nội dung đa phương tiện</option>
              <option value="no">Không có nội dung đa phương tiện</option>
            </select>
            <button
              class="clear-button"
              @click="query = ''; section = 'all'; type = 'all'; difficulty = 'all'; media = 'all'"
            >
              Xóa tất cả
            </button>
          </div>
          <div v-if="!filteredQuestions.length" class="empty">
            Không có câu hỏi phù hợp với bộ lọc.
          </div>
          <article
            v-if="false"
            v-for="item in filteredQuestions"
            :id="`question-detail-${item.questionId}`"
            :key="item.questionId"
            class="preview-question"
            :class="{ highlight: activeId === item.questionId }"
            @click="goTo(item)"
          >
            <div class="question-heading">
              <span class="question-number">
                {{ item.questionOrder || '?' }}
              </span>
              <div>
                <span class="question-label">
                  {{ item.sectionName || 'Câu hỏi' }} ·
                  {{ questionType(item.questionType) }}
                </span>
                <h3>{{ item.content || 'Câu hỏi chưa có nội dung' }}</h3>
              </div>
            </div>
            <div v-if="mediaItems(item).length" class="media-list">
              <template v-for="source in mediaItems(item)" :key="source">
                <img
                  v-if="/\.(png|jpe?g|gif)$/i.test(source)"
                  :src="source"
                  alt="Nội dung hình ảnh của câu hỏi"
                />
                <audio
                  v-else-if="/\.(mp3|wav|ogg)$/i.test(source)"
                  :src="source"
                  controls
                />
                <video v-else :src="source" controls />
              </template>
            </div>
            <div v-if="options(item).length" class="options">
              <div
                v-for="(option, index) in options(item)"
                :key="index"
                class="option"
              >
                <span>{{ option.key || String.fromCharCode(65 + index) }}</span>
                <span>
                  {{ option.value || option.text || option.label || option }}
                </span>
              </div>
            </div>
            <div class="inspection">
              <span>Độ khó: {{ difficultyLabel(item.difficulty) }}</span>
              <span>Điểm: {{ item.points || 0 }}</span>
              <span>
                Trạng thái: {{ status(item.questionStatus || item.status) }}
              </span>
              <span>Đáp án: {{ item.correctAnswer || 'Chưa có đáp án' }}</span>
              <p v-if="item.explanation">{{ item.explanation }}</p>
            </div>
          </article>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .preview-backdrop {
    position: fixed;
    z-index: 100;
    inset: 0;
    display: flex;
    background: #0f172acc;
  }
  .preview-shell {
    display: flex;
    width: 100%;
    height: 100%;
    flex-direction: column;
    background: #f8fafc;
  }
  .preview-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    padding: 22px 28px;
    border-bottom: 1px solid #e2e8f0;
    background: #fff;
  }
  .eyebrow {
    color: #2563eb;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
  }
  .preview-header h2 {
    margin: 6px 0;
    color: #0f172a;
    font-size: 22px;
  }
  .preview-header p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
  }
  .icon-button {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: #fff;
    color: #334155;
    cursor: pointer;
  }
  .save-preview-button { min-height: 36px; padding: 0 14px; border: 0; border-radius: 6px; background: #176b5d; color: #fff; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
  .save-preview-button:disabled { opacity: .55; cursor: wait; }
  .preview-summary {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
    padding: 12px 28px;
    border-bottom: 1px solid #e2e8f0;
    color: #64748b;
    background: #fff;
    font-size: 12px;
  }
  .summary-status {
    color: #047857;
    font-weight: 700;
  }
  .mode-switch {
    display: none;
  }
  .mode-switch-removed {
    display: flex;
    gap: 6px;
    padding: 12px 28px 0;
    background: #fff;
  }
  .mode-switch button {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 9px 12px;
    border: 1px solid transparent;
    border-radius: 7px;
    background: transparent;
    color: #64748b;
    cursor: pointer;
  }
  .mode-switch button.active {
    border-color: #bfdbfe;
    color: #1d4ed8;
    background: #eff6ff;
  }
  .preview-layout {
    display: grid;
    min-height: 0;
    flex: 1;
    grid-template-columns: 230px minmax(0, 1fr);
  }
  .navigator {
    overflow: auto;
    padding: 16px;
    border-right: 1px solid #e2e8f0;
    background: #fff;
  }
  .navigator-title {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
    color: #334155;
    font-size: 13px;
  }
  .navigator-title span {
    color: #64748b;
  }
  .navigator-section {
    margin-bottom: 12px;
  }
  .navigator-section > summary,
  .navigator-group > summary {
    cursor: pointer;
    list-style: none;
  }
  .navigator-section > summary::-webkit-details-marker,
  .navigator-group > summary::-webkit-details-marker {
    display: none;
  }
  .navigator-section > summary::before,
  .navigator-group > summary::before {
    content: '›';
    display: inline-block;
    width: 12px;
    color: #64748b;
    font-size: 16px;
    line-height: 1;
    transition: transform .15s ease;
  }
  .navigator-section[open] > summary::before,
  .navigator-group[open] > summary::before {
    transform: rotate(90deg);
  }
  .navigator-section-title,
  .navigator-group-title {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    color: #334155;
  }
  .navigator-section-title {
    padding: 8px 6px;
    border-bottom: 1px solid #e2e8f0;
    font-size: 11px;
  }
  .navigator-section-title strong,
  .navigator-group-title span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .navigator-section-title:focus-visible,
  .navigator-group-title:focus-visible {
    outline: 2px solid #2563eb;
    outline-offset: -2px;
    background: #eff6ff;
  }
  .navigator-section-title small,
  .navigator-group-title small {
    margin-left: auto;
    color: #94a3b8;
    font-size: 10px;
  }
  .navigator-group {
    margin-left: 10px;
    padding-left: 8px;
    border-left: 1px solid #cbd5e1;
  }
  .navigator-group-title {
    padding: 7px 4px 4px;
    color: #64748b;
    font-size: 10px;
    font-weight: 600;
  }
  .navigator-question {
    display: grid;
    width: 100%;
    grid-template-columns: 12px 24px minmax(0, 1fr);
    align-items: center;
    gap: 8px;
    padding: 6px 4px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #64748b;
    text-align: left;
    cursor: pointer;
  }
  .navigator-question.current,
  .navigator-question:hover,
  .navigator-question:focus,
  .navigator-question:focus-visible {
    color: #1d4ed8;
    background: #dbeafe;
    outline: 2px solid #60a5fa;
    outline-offset: -2px;
  }
  .navigator-question b {
    display: grid;
    width: 24px;
    height: 24px;
    place-items: center;
    border: 1px solid #cbd5e1;
    border-radius: 50%;
    font-size: 11px;
    font-weight: 600;
  }
  .tree-branch {
    display: block;
    width: 8px;
    height: 1px;
    background: #cbd5e1;
  }
  .navigator-question small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 10px;
  }
  .preview-main {
    overflow: auto;
    padding: 20px 28px 60px;
  }
  .exam-detail-title { margin-bottom: 12px; color: #0f172a; font-size: 20px; font-weight: 700; }
  .exam-section-heading { margin: 18px 0 10px; padding: 12px 14px; border-left: 4px solid #2563eb; background: #eff6ff; }
  .exam-section-heading span { color: #2563eb; font-size: 10px; font-weight: 700; letter-spacing: .08em; }
  .exam-section-heading h3 { margin: 4px 0 0; color: #0f172a; font-size: 17px; }
  .section-instruction, .group-instruction { margin: 8px 0 0; color: #334155; white-space: pre-wrap; line-height: 1.55; }
  .exam-passage-group { margin: 0 0 14px; padding: 14px; border: 1px solid #dbe4f0; border-radius: 8px; background: #fff; }
  .passage-heading { display: flex; justify-content: space-between; gap: 10px; color: #334155; }
  .passage-heading p { margin: 0; color: #64748b; font-size: 12px; }
  .group-instruction { padding: 9px 0; border-top: 1px solid #e2e8f0; font-size: 13px; }
  .passage-content { margin: 10px 0; padding: 12px; border: 1px solid #e2e8f0; background: #f8fafc; color: #334155; white-space: pre-wrap; line-height: 1.55; }
  .structured-question { margin-top: 10px; padding: 12px; border-top: 1px solid #e2e8f0; color: #334155; }
  .structured-question.flash-highlight {
    animation: question-flash .9s ease-out;
    border-radius: 6px;
  }
  @keyframes question-flash {
    0% { background: #93c5fd; box-shadow: 0 0 0 3px #60a5fa; }
    55% { background: #dbeafe; box-shadow: 0 0 0 2px #93c5fd; }
    100% { background: transparent; box-shadow: 0 0 0 0 transparent; }
  }
  .structured-question p { margin: 6px 0 10px; white-space: pre-wrap; line-height: 1.5; }
  .editable-question-field { width: 100%; min-height: 72px; margin: 8px 0 10px; padding: 8px 10px; border: 1px solid #93c5fd; border-radius: 5px; font: inherit; line-height: 1.5; resize: vertical; }
  .editable-answer-field { min-width: 150px; margin-left: 5px; padding: 5px 8px; border: 1px solid #93c5fd; border-radius: 4px; font: inherit; }
  .preview-main > article { display: none; }
  .filter-bar,
  .advanced-filters {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 14px;
  }
  .filter-bar label {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 220px;
    flex: 1;
    padding: 0 10px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    background: #fff;
    color: #94a3b8;
  }
  .filter-bar input {
    width: 100%;
    padding: 9px 0;
    border: 0;
    outline: 0;
  }
  .filter-bar select,
  .advanced-filters select {
    min-height: 36px;
    padding: 0 9px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    background: #fff;
    color: #334155;
  }
  .advanced-toggle,
  .clear-button {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 8px 10px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    background: #fff;
    color: #334155;
    cursor: pointer;
  }
  .clear-button {
    color: #b91c1c;
  }
  .preview-question {
    margin: 0 auto 14px;
    padding: 20px;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    background: #fff;
    scroll-margin-top: 20px;
  }
  .preview-question.highlight {
    border-color: #60a5fa;
    box-shadow: 0 0 0 3px #dbeafe;
  }
  .question-heading {
    display: flex;
    gap: 12px;
  }
  .question-number {
    display: grid;
    width: 30px;
    height: 30px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 50%;
    color: #1d4ed8;
    background: #eff6ff;
    font-weight: 700;
  }
  .question-label {
    color: #64748b;
    font-size: 11px;
  }
  .question-heading h3 {
    margin: 6px 0 14px;
    color: #0f172a;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.55;
    white-space: pre-wrap;
  }
  .options {
    display: grid;
    gap: 8px;
  }
  .option {
    display: flex;
    gap: 9px;
    padding: 10px 12px;
    border: 1px solid #e2e8f0;
    border-radius: 7px;
    color: #475569;
    font-size: 13px;
  }
  .option span:first-child {
    color: #2563eb;
    font-weight: 700;
  }
  .media-list {
    display: grid;
    gap: 10px;
    margin: 0 0 14px;
  }
  .media-list img,
  .media-list video {
    max-width: 100%;
    max-height: 280px;
    object-fit: contain;
  }
  .media-list audio {
    width: 100%;
  }
  .inspection {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #e2e8f0;
    color: #64748b;
    font-size: 12px;
  }
  .inspection p {
    width: 100%;
    margin: 0;
    color: #334155;
    white-space: pre-wrap;
  }
  .empty {
    padding: 40px;
    text-align: center;
    color: #64748b;
  }
  @media (max-width: 760px) {
    .preview-header,
    .preview-summary,
    .mode-switch {
      padding-right: 16px;
      padding-left: 16px;
    }
    .preview-layout {
      grid-template-columns: 1fr;
    }
    .navigator {
      display: none;
    }
    .preview-main {
      padding: 14px 16px 40px;
    }
  }
</style>
