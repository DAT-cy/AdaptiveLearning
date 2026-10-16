<script setup>
import { inject } from 'vue'
import { Clock3, Send } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section class="page-view test-taking-view"><div class="view-heading"><div><p class="view-kicker">BÀI KIỂM TRA ĐANG DIỄN RA</p><h1>{{ home.exams.find((exam) => exam.examId === home.attempt?.examId)?.title || 'Bài kiểm tra' }}</h1><p>Đáp án được tự động lưu trong quá trình làm bài.</p></div><div class="timer" :class="{ urgent: home.secondsLeft < 300 }"><Clock3 :size="17" />{{ home.formattedTimeLeft }}</div></div><p class="save-state" aria-live="polite">{{ home.saveState }}</p>
    <article v-for="(question, index) in home.attemptQuestions" :key="question.questionId" class="question-panel"><div class="question-number">{{ index + 1 }}</div><div class="question-content"><div class="question-meta"><span>Câu {{ index + 1 }}</span><span v-if="question.sectionName">{{ question.sectionName }}</span><span>{{ question.points }} điểm</span></div><p>{{ question.content }}</p><div v-if="home.parseOptions(question).length" class="answer-options"><label v-for="option in home.parseOptions(question)" :key="option.value" class="answer-option"><input v-model="home.answers[question.questionId]" type="radio" :name="`question-${question.questionId}`" :value="option.value" @change="home.saveAnswer(question)"><span>{{ option.label }}</span></label></div><textarea v-else v-model="home.answers[question.questionId]" class="answer-text" rows="3" placeholder="Nhập câu trả lời" @input="home.saveAnswer(question)"></textarea></div></article>
    <div class="submit-bar"><button class="secondary-button" @click="home.leaveAttempt">Rời bài thi</button><button class="primary-button" :disabled="home.submitting || !home.attemptQuestions.length" @click="home.submitExam"><Send :size="16" />{{ home.submitting ? 'Đang nộp...' : 'Nộp bài' }}</button></div>
  </section>
</template>
