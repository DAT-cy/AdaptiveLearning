<script setup>
import { badgeVariants } from ".";
import { cn } from "@/lib/utils";

const props = defineProps({
  variant: { type: String, required: false, default: "default" },
  size: { type: String, required: false, default: "default" },
  class: { type: null, required: false },
  dot: { type: Boolean, required: false, default: false },
});

const dotColorMap = {
  default: "bg-brand-primary",
  secondary: "bg-text-secondary",
  success: "bg-emerald-500",
  warning: "bg-amber-500",
  destructive: "bg-red-500",
  info: "bg-sky-500",
  outline: "bg-slate-400",
  ai: "bg-ai-accent",
};
</script>

<template>
  <span :class="cn(badgeVariants({ variant, size }), props.class)">
    <span
      v-if="dot"
      class="w-1.5 h-1.5 rounded-full flex-shrink-0"
      :class="dotColorMap[variant] || 'bg-brand-primary'"
    />
    <slot />
  </span>
</template>
