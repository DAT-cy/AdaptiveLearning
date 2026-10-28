<script setup>
import { ChevronRight } from "lucide-vue-next";
import { cn } from "@/lib/utils";

const props = defineProps({
  title: { type: String, required: false },
  description: { type: String, required: false },
  kicker: { type: String, required: false },
  breadcrumbs: { type: Array, required: false, default: () => [] },
  class: { type: null, required: false },
});
</script>

<template>
  <header
    :class="
      cn(
        'flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between pb-6 mb-6 border-b border-border-subtle',
        props.class
      )
    "
  >
    <div class="min-w-0 flex-1">
      <!-- Breadcrumbs -->
      <nav
        v-if="breadcrumbs?.length || $slots.breadcrumbs"
        aria-label="Breadcrumb"
        class="flex items-center flex-wrap gap-1.5 text-xs text-text-secondary mb-2"
      >
        <slot name="breadcrumbs">
          <template v-for="(crumb, index) in breadcrumbs" :key="crumb.label || index">
            <span v-if="index > 0" class="text-text-placeholder">
              <ChevronRight class="w-3.5 h-3.5" />
            </span>
            <RouterLink
              v-if="crumb.to"
              :to="crumb.to"
              class="hover:text-brand-primary transition-colors"
            >
              {{ crumb.label }}
            </RouterLink>
            <a
              v-else-if="crumb.href"
              :href="crumb.href"
              class="hover:text-brand-primary transition-colors"
            >
              {{ crumb.label }}
            </a>
            <span
              v-else
              :class="index === breadcrumbs.length - 1 ? 'font-medium text-text-primary' : ''"
            >
              {{ crumb.label }}
            </span>
          </template>
        </slot>
      </nav>

      <!-- Kicker -->
      <p
        v-if="kicker || $slots.kicker"
        class="text-xs font-bold uppercase tracking-wider text-brand-primary mb-1"
      >
        <slot name="kicker">{{ kicker }}</slot>
      </p>

      <!-- Title -->
      <h1
        v-if="title || $slots.title"
        class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary"
      >
        <slot name="title">{{ title }}</slot>
      </h1>

      <!-- Description -->
      <p
        v-if="description || $slots.description"
        class="text-sm text-text-secondary mt-1.5 leading-relaxed"
      >
        <slot name="description">{{ description }}</slot>
      </p>

      <slot />
    </div>

    <!-- Actions -->
    <div
      v-if="$slots.actions"
      class="flex items-center flex-wrap gap-2.5 sm:self-center flex-shrink-0"
    >
      <slot name="actions" />
    </div>
  </header>
</template>
