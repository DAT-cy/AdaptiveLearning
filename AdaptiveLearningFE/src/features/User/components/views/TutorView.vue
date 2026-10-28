<script setup>
import { computed, inject, nextTick, ref, watch } from 'vue'
import {
  AlertCircle,
  ArrowRight,
  BookOpen,
  Brain,
  Check,
  Compass,
  FileText,
  Lightbulb,
  Loader2,
  MessageSquare,
  Plus,
  Send,
  Sparkles,
  Target,
  UserRound,
  X,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const home = inject(homeContextKey)

const messageListRef = ref(null)
const mobileConversationsOpen = ref(false)

// Tự động cuộn xuống tin nhắn mới nhất
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

watch(
  () => home.messages?.length,
  () => {
    scrollToBottom()
  }
)

watch(
  () => home.askingTutor,
  (val) => {
    if (val) scrollToBottom()
  }
)

// Khởi tạo cuộc trò chuyện mới
function startNewChat() {
  home.currentConversation = null
  home.messages = []
  home.tutorPrompt = ''
  mobileConversationsOpen.value = false
}

function handleSelectConversation(conv) {
  home.selectConversation(conv)
  mobileConversationsOpen.value = false
}

// Gợi ý câu hỏi nhanh
function applySuggestion(promptText) {
  home.tutorPrompt = promptText
}

// Xử lý gửi tin nhắn bằng Enter (Shift + Enter để xuống dòng)
function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    home.askTutor()
  }
}

// Parse danh sách trích dẫn (citations) nếu backend RAG trả về
function parseCitations(citationsJson) {
  if (!citationsJson) return []
  try {
    const parsed = typeof citationsJson === 'string' ? JSON.parse(citationsJson) : citationsJson
    if (Array.isArray(parsed)) return parsed
    if (typeof parsed === 'object') return Object.values(parsed)
  } catch {
    return []
  }
  return []
}
</script>

<template>
  <div class="tutor-page-view" aria-label="Gia sư và Trợ lý học tập AI">
    <!-- 1. HEADER KHÔNG GIAN AI TUTOR -->
    <header class="tutor-header-card">
      <div class="header-main-info">
        <Badge variant="ai" class="header-badge">
          <Sparkles :size="13" class="text-ai-accent" />
          GIA SƯ & TRỢ LÝ HỌC TẬP THÍCH ỨNG
        </Badge>
        <h1 class="header-title">Trợ lý AI 24/7</h1>
        <p class="header-desc">
          Giải đáp thắc mắc chuyên sâu, gợi ý chiến lược làm bài, phân tích lỗi sai và đồng hành cùng tiến độ học tập của bạn.
        </p>
      </div>

      <div class="header-actions">
        <!-- Nút mở danh sách hội thoại trên mobile -->
        <Button
          variant="outline"
          size="sm"
          class="mobile-toggle-conv-btn"
          @click="mobileConversationsOpen = !mobileConversationsOpen"
        >
          <MessageSquare :size="15" />
          <span>Lịch sử chat</span>
          <span v-if="home.conversations?.length" class="conv-count-badge">
            {{ home.conversations.length }}
          </span>
        </Button>

        <!-- Nút tạo hội thoại mới -->
        <Button
          variant="ai"
          size="sm"
          class="new-chat-btn"
          @click="startNewChat"
        >
          <Plus :size="15" /> Cuộc trò chuyện mới
        </Button>
      </div>
    </header>

    <!-- 2. BỐ CỤC KHÔNG GIAN LÀM VIỆC (WORKSPACE) -->
    <div class="tutor-workspace-grid">
      <!-- CỘT 1: DANH SÁCH CUỘC TRÒ CHUYỆN (CONVERSATIONS ASIDE) -->
      <aside
        class="conversations-aside"
        :class="{ 'mobile-drawer-open': mobileConversationsOpen }"
      >
        <div class="aside-header">
          <div class="aside-title-group">
            <MessageSquare :size="16" class="text-ai-accent" />
            <h2 class="aside-title">Lịch sử trò chuyện</h2>
          </div>
          <button
            type="button"
            class="new-chat-icon-btn"
            title="Tạo cuộc trò chuyện mới"
            aria-label="Tạo cuộc trò chuyện mới"
            @click="startNewChat"
          >
            <Plus :size="16" />
          </button>
        </div>

        <div class="conversations-scroll-list">
          <div
            v-if="home.conversations?.length"
            class="conversations-items-stack"
            role="list"
          >
            <button
              v-for="conv in home.conversations"
              :key="conv.id"
              type="button"
              class="conversation-item-btn"
              :class="{ active: home.currentConversation?.id === conv.id }"
              @click="handleSelectConversation(conv)"
            >
              <span class="conv-icon">
                <Brain :size="15" />
              </span>
              <span class="conv-title-text">{{ conv.title || 'Hỗ trợ học tập' }}</span>
            </button>
          </div>

          <div v-else class="conversations-empty-card">
            <span class="empty-icon-box">
              <Brain :size="20" />
            </span>
            <p>Chưa có hội thoại nào. Nhập câu hỏi để bắt đầu cuộc trò chuyện đầu tiên với gia sư AI.</p>
          </div>
        </div>
      </aside>

      <!-- Lớp phủ đóng mobile drawer -->
      <div
        v-if="mobileConversationsOpen"
        class="mobile-backdrop"
        @click="mobileConversationsOpen = false"
      ></div>

      <!-- CỘT 2: KHUNG CHAT CHÍNH (CHAT PANEL) -->
      <main class="chat-main-panel">
        <!-- Thông báo lỗi nếu có -->
        <div v-if="home.pageError" class="chat-alert-banner alert-error" role="alert">
          <AlertCircle :size="17" class="text-rose-600 flex-shrink-0" />
          <span class="alert-msg">{{ home.pageError }}</span>
          <button
            type="button"
            class="alert-close"
            aria-label="Đóng cảnh báo"
            @click="home.pageError = ''"
          >
            <X :size="15" />
          </button>
        </div>

        <!-- DANH SÁCH TIN NHẮN (MESSAGE STREAM) -->
        <div
          v-if="home.messages?.length"
          ref="messageListRef"
          class="messages-scroll-area"
          role="log"
          aria-live="polite"
        >
          <article
            v-for="msg in home.messages"
            :key="msg.id"
            class="message-card"
            :class="msg.role === 'USER' ? 'user-bubble-row' : 'assistant-bubble-row'"
          >
            <div class="message-avatar-wrap">
              <span
                class="avatar-circle"
                :class="msg.role === 'USER' ? 'user-avatar' : 'ai-avatar'"
              >
                <UserRound v-if="msg.role === 'USER'" :size="15" />
                <Brain v-else :size="16" />
              </span>
            </div>

            <div class="message-body-wrap">
              <div class="message-sender-name">
                <strong>{{ msg.role === 'USER' ? 'Bạn' : 'Trợ lý AI' }}</strong>
                <Badge
                  v-if="msg.role !== 'USER'"
                  variant="ai"
                  size="sm"
                  class="ai-mini-tag"
                >
                  AI Tutor
                </Badge>
              </div>

              <!-- Nội dung tin nhắn -->
              <div class="message-content-bubble">
                <p class="message-text">{{ msg.content }}</p>

                <!-- Trích dẫn nguồn / Tài liệu tham khảo nếu có -->
                <div v-if="parseCitations(msg.citationsJson).length" class="citations-container">
                  <span class="citations-title">
                    <BookOpen :size="12" /> Nguồn tài liệu tham khảo:
                  </span>
                  <div class="citations-pills">
                    <span
                      v-for="(cit, citIdx) in parseCitations(msg.citationsJson)"
                      :key="citIdx"
                      class="citation-pill"
                    >
                      {{ cit.title || cit.name || cit }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <!-- BONG BÓNG ĐANG SUY NGHĨ / ĐANG TRẢ LỜI (THINKING STATE) -->
          <article v-if="home.askingTutor" class="message-card assistant-bubble-row">
            <div class="message-avatar-wrap">
              <span class="avatar-circle ai-avatar">
                <Brain :size="16" />
              </span>
            </div>
            <div class="message-body-wrap">
              <div class="message-sender-name">
                <strong>Trợ lý AI</strong>
                <Badge variant="ai" size="sm" class="ai-mini-tag">AI Tutor</Badge>
              </div>
              <div class="message-content-bubble thinking-bubble">
                <Loader2 :size="15" class="animate-spin text-ai-accent" />
                <span class="thinking-text">Trợ lý AI đang suy nghĩ câu trả lời…</span>
              </div>
            </div>
          </article>
        </div>

        <!-- MÀN HÌNH CHÀO MỪNG KHI CHƯA CÓ TIN NHẮN (EMPTY STATE) -->
        <div v-else class="chat-welcome-empty">
          <div class="welcome-icon-circle">
            <Sparkles :size="28" class="text-ai-accent" />
          </div>
          <h2 class="welcome-heading">Gia sư AI có thể giúp gì cho bạn?</h2>
          <p class="welcome-subtext">
            Đặt câu hỏi về bất kỳ chủ đề bài học, yêu cầu giải thích đáp án sai hoặc đề xuất kế hoạch ôn tập được cá nhân hóa.
          </p>

          <!-- Các thẻ câu hỏi gợi ý nhanh -->
          <div class="suggestion-chips-grid">
            <button
              type="button"
              class="suggestion-chip-btn"
              @click="applySuggestion('Giúp mình lập kế hoạch ôn tập hiệu quả cho kỳ thi sắp tới.')"
            >
              <Compass :size="15" class="chip-icon text-brand-primary" />
              <span>Lập kế hoạch ôn tập hiệu quả</span>
            </button>

            <button
              type="button"
              class="suggestion-chip-btn"
              @click="applySuggestion('Giải thích giúp mình khái niệm đang học mà mình chưa nắm vững.')"
            >
              <Lightbulb :size="15" class="chip-icon text-amber-600" />
              <span>Giải thích khái niệm khó</span>
            </button>

            <button
              type="button"
              class="suggestion-chip-btn"
              @click="applySuggestion('Phân tích giúp mình các lỗi sai thường gặp trong bài thi gần nhất.')"
            >
              <Target :size="15" class="chip-icon text-purple-600" />
              <span>Phân tích điểm yếu & lỗi sai</span>
            </button>

            <button
              type="button"
              class="suggestion-chip-btn"
              @click="applySuggestion('Tạo cho mình một bài luyện tập ngắn để củng cố kỹ năng.')"
            >
              <Sparkles :size="15" class="chip-icon text-ai-accent" />
              <span>Tạo bài luyện tập nhanh</span>
            </button>
          </div>
        </div>

        <!-- KHUNG SOẠN THẢO VÀ GỬI CÂU HỎI (CHAT COMPOSER) -->
        <form class="chat-composer-form" @submit.prevent="home.askTutor">
          <div class="composer-inner-wrap">
            <textarea
              v-model="home.tutorPrompt"
              class="composer-textarea"
              rows="2"
              placeholder="Nhập câu hỏi hoặc chủ đề bạn cần trợ lý AI hỗ trợ..."
              aria-label="Nhập câu hỏi cho trợ lý AI"
              :disabled="home.askingTutor"
              @keydown="handleKeydown"
            ></textarea>

            <div class="composer-actions-bar">
              <span class="composer-hint-text">
                Nhấn <strong>Enter</strong> để gửi, <strong>Shift + Enter</strong> để xuống dòng
              </span>

              <Button
                type="submit"
                variant="ai"
                size="sm"
                class="send-message-btn"
                :disabled="home.askingTutor || !home.tutorPrompt?.trim()"
                :aria-label="home.askingTutor ? 'Đang gửi câu hỏi' : 'Gửi câu hỏi'"
              >
                <Loader2 v-if="home.askingTutor" :size="15" class="animate-spin" />
                <Send v-else :size="15" />
                <span class="hidden sm:inline">{{ home.askingTutor ? 'Đang gửi…' : 'Gửi' }}</span>
              </Button>
            </div>
          </div>
        </form>
      </main>

      <!-- CỘT 3: TÍNH NĂNG GỢI Ý & PHÍM TẮT HỌC TẬP (FEATURES ASIDE) -->
      <aside class="features-aside">
        <div class="features-card">
          <h2 class="features-title">Gợi ý học tập cùng AI</h2>

          <div class="features-buttons-list">
            <button
              type="button"
              class="feature-action-btn"
              @click="applySuggestion('Giải thích giúp mình chi tiết bài học mà mình đang gặp khó khăn.')"
            >
              <span class="feature-icon bg-sky-50 text-sky-600 border border-sky-200/60">
                <BookOpen :size="16" />
              </span>
              <div class="feature-text-group">
                <strong>Giải thích bài học</strong>
                <small>Hiểu sâu lý thuyết & ví dụ</small>
              </div>
              <ArrowRight :size="14" class="feature-arrow" />
            </button>

            <button
              type="button"
              class="feature-action-btn"
              @click="applySuggestion('Gợi ý kế hoạch học phù hợp nhất với mục tiêu hiện tại của mình.')"
            >
              <span class="feature-icon bg-blue-50 text-brand-primary border border-blue-200/60">
                <Compass :size="16" />
              </span>
              <div class="feature-text-group">
                <strong>Gợi ý lộ trình</strong>
                <small>Tối ưu theo mô hình thích ứng</small>
              </div>
              <ArrowRight :size="14" class="feature-arrow" />
            </button>

            <button
              type="button"
              class="feature-action-btn"
              @click="applySuggestion('Phân tích giúp mình bài thi vừa làm và hướng dẫn cách khắc phục lỗi sai.')"
            >
              <span class="feature-icon bg-purple-50 text-ai-accent border border-purple-200/60">
                <Target :size="16" />
              </span>
              <div class="feature-text-group">
                <strong>Phân tích lỗi sai</strong>
                <small>Nhận xét chi tiết từng câu</small>
              </div>
              <ArrowRight :size="14" class="feature-arrow" />
            </button>

            <button
              type="button"
              class="feature-action-btn"
              @click="applySuggestion('Tạo cho mình 3 câu hỏi trắc nghiệm ôn tập về chủ đề vừa học.')"
            >
              <span class="feature-icon bg-amber-50 text-amber-600 border border-amber-200/60">
                <Sparkles :size="16" />
              </span>
              <div class="feature-text-group">
                <strong>Tạo bài luyện tập</strong>
                <small>Thực hành ngay tức thì</small>
              </div>
              <ArrowRight :size="14" class="feature-arrow" />
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.tutor-page-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  height: calc(100vh - 120px);
  min-height: 580px;
}

/* ==========================================================================
   HEADER
   ========================================================================== */
.tutor-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px;
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(135deg, #FFFFFF 0%, #FAF5FF 60%, #F0FDFA 100%);
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.02);
  flex-shrink: 0;
}

.header-badge {
  margin-bottom: 8px;
}

.header-title {
  margin: 0 0 4px;
  font-size: clamp(20px, 2.2vw, 24px);
  font-weight: 800;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.4px;
}

.header-desc {
  margin: 0;
  font-size: 13.5px;
  color: var(--text-secondary, #475569);
  max-width: 680px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.mobile-toggle-conv-btn {
  display: none;
}

.conv-count-badge {
  padding: 1px 6px;
  border-radius: 999px;
  background-color: #E2E8F0;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}

/* ==========================================================================
   WORKSPACE GRID (3 COLUMNS)
   ========================================================================== */
.tutor-workspace-grid {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 260px;
  gap: 18px;
  width: 100%;
  flex: 1;
  min-height: 0;
}

/* ==========================================================================
   CỘT 1: DANH SÁCH CUỘC TRÒ CHUYỆN (CONVERSATIONS)
   ========================================================================== */
.conversations-aside {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  overflow: hidden;
}

.aside-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #F1F5F9;
}

.aside-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.aside-title {
  margin: 0;
  font-size: 14px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.new-chat-icon-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #F8FAFC;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.new-chat-icon-btn:hover {
  background-color: #ffffff;
  color: var(--brand-primary, #2563EB);
  border-color: #CBD5E1;
}

.conversations-scroll-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.conversations-items-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conversation-item-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary, #475569);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}

.conversation-item-btn:hover {
  background-color: #F8FAFC;
  color: var(--text-primary, #0F172A);
}

.conversation-item-btn.active {
  background-color: #FAF5FF;
  border-color: #DDD6FE;
  color: #7C3AED;
  font-weight: 600;
}

.conv-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.conv-title-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversations-empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 24px 14px;
  color: var(--text-muted, #64748B);
  font-size: 12.5px;
  line-height: 1.5;
}

.empty-icon-box {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #F5F3FF;
  color: #7C3AED;
  margin-bottom: 10px;
}

/* ==========================================================================
   CỘT 2: KHUNG CHAT CHÍNH (CHAT MAIN PANEL)
   ========================================================================== */
.chat-main-panel {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  overflow: hidden;
  min-height: 0;
}

.chat-alert-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 13px;
  background-color: #FFF1F2;
  border-bottom: 1px solid #FECDD3;
  color: #9F1239;
}

.alert-msg {
  flex: 1;
}

.alert-close {
  display: grid;
  place-items: center;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  opacity: 0.7;
}

.alert-close:hover {
  opacity: 1;
}

/* Messages Scroll Area */
.messages-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-card {
  display: flex;
  gap: 12px;
  max-width: 88%;
}

.user-bubble-row {
  margin-left: auto;
  flex-direction: row-reverse;
}

.assistant-bubble-row {
  margin-right: auto;
}

.message-avatar-wrap {
  flex-shrink: 0;
}

.avatar-circle {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid;
}

.user-avatar {
  background-color: #F1F5F9;
  border-color: #CBD5E1;
  color: #334155;
}

.ai-avatar {
  background: linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%);
  border-color: #DDD6FE;
  color: #7C3AED;
}

.message-body-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-sender-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

.user-bubble-row .message-sender-name {
  justify-content: flex-end;
}

.ai-mini-tag {
  font-size: 10px;
  padding: 1px 6px;
}

.message-content-bubble {
  padding: 14px 16px;
  border-radius: var(--radius-md, 12px);
  font-size: 14px;
  line-height: 1.6;
}

.user-bubble-row .message-content-bubble {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  color: #1E40AF;
  border-top-right-radius: 3px;
}

.assistant-bubble-row .message-content-bubble {
  background-color: #FAF5FF;
  border: 1px solid #E9D5FF;
  color: #1E293B;
  border-top-left-radius: 3px;
}

.message-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Citations container */
.citations-container {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #E9D5FF;
}

.citations-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #7C3AED;
  margin-bottom: 4px;
}

.citations-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.citation-pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #ffffff;
  border: 1px solid #DDD6FE;
  font-size: 11px;
  color: #6B21A8;
}

/* Thinking Bubble */
.thinking-bubble {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
}

.thinking-text {
  font-size: 13px;
  color: #7C3AED;
}

/* ==========================================================================
   WELCOME EMPTY STATE
   ========================================================================== */
.chat-welcome-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 30px 24px;
}

.welcome-icon-circle {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: #F5F3FF;
  border: 1px solid #DDD6FE;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.08);
}

.welcome-heading {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.welcome-subtext {
  margin: 0 0 24px;
  font-size: 13.5px;
  color: var(--text-muted, #64748B);
  max-width: 480px;
  line-height: 1.5;
}

.suggestion-chips-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 560px;
}

.suggestion-chip-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 12.5px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}

.suggestion-chip-btn:hover {
  background-color: #ffffff;
  border-color: #CBD5E1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.chip-icon {
  flex-shrink: 0;
}

/* ==========================================================================
   CHAT COMPOSER FORM
   ========================================================================== */
.chat-composer-form {
  padding: 14px 18px;
  border-top: 1px solid #F1F5F9;
  background-color: #ffffff;
}

.composer-inner-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #F8FAFC;
  padding: 10px 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.composer-inner-wrap:focus-within {
  background-color: #ffffff;
  border-color: #DDD6FE;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.composer-textarea {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 13.5px;
  line-height: 1.5;
  outline: none;
  resize: none;
}

.composer-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.composer-hint-text {
  font-size: 11px;
  color: var(--text-muted, #94A3B8);
}

.composer-hint-text strong {
  color: #64748B;
}

.send-message-btn {
  padding: 6px 14px;
}

/* ==========================================================================
   CỘT 3: TÍNH NĂNG GỢI Ý & PHÍM TẮT HỌC TẬP (FEATURES ASIDE)
   ========================================================================== */
.features-aside {
  display: flex;
  flex-direction: column;
}

.features-card {
  display: flex;
  flex-direction: column;
  padding: 18px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
}

.features-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.features-buttons-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feature-action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  color: var(--text-primary, #0F172A);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}

.feature-action-btn:hover {
  background-color: #ffffff;
  border-color: #CBD5E1;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.feature-action-btn:hover .feature-arrow {
  color: #7C3AED;
  transform: translateX(2px);
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  flex-shrink: 0;
}

.feature-text-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.feature-text-group strong {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.feature-text-group small {
  font-size: 11px;
  color: var(--text-muted, #64748B);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.feature-arrow {
  color: #94A3B8;
  transition: transform 0.15s ease, color 0.15s ease;
  flex-shrink: 0;
}

/* ==========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 1100px) {
  .tutor-workspace-grid {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .features-aside {
    display: none;
  }
}

@media (max-width: 768px) {
  .tutor-page-view {
    height: auto;
    min-height: calc(100vh - 140px);
  }

  .tutor-header-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 16px;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .mobile-toggle-conv-btn {
    display: inline-flex;
  }

  .tutor-workspace-grid {
    grid-template-columns: 1fr;
    height: 600px;
  }

  /* Off-canvas drawer for conversations on mobile */
  .conversations-aside {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    width: 280px;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    border-radius: 0;
    box-shadow: 4px 0 24px rgba(15, 23, 42, 0.15);
  }

  .conversations-aside.mobile-drawer-open {
    transform: translateX(0);
  }

  .mobile-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.4);
    z-index: 40;
    backdrop-filter: blur(2px);
  }

  .suggestion-chips-grid {
    grid-template-columns: 1fr;
  }

  .composer-hint-text {
    display: none;
  }
}
</style>
