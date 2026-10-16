<script setup>
import { inject } from 'vue'
import { ArrowRight, CheckCircle2 } from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'

const home = inject(homeContextKey)
</script>

<template>
  <section v-if="home.result" class="result-panel"><span class="result-icon"><CheckCircle2 :size="28" /></span><p class="view-kicker">ĐÃ HOÀN THÀNH</p><h1>Kết quả bài thi</h1><strong class="result-score">{{ home.result.score ?? 0 }}<small> / {{ home.result.maxScore ?? 0 }}</small></strong><div class="result-counts"><div><strong>{{ home.result.correctCount }}</strong><small>Đúng</small></div><div><strong>{{ home.result.wrongCount }}</strong><small>Sai</small></div><div><strong>{{ home.result.unansweredCount }}</strong><small>Bỏ trống</small></div></div><button class="primary-button" @click="home.showView('dashboard')">Về trang chủ <ArrowRight :size="16" /></button></section>
</template>
