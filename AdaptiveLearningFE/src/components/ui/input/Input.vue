<script setup>
import { useVModel } from "@vueuse/core";
import { cn } from "@/lib/utils";

defineOptions({
  inheritAttrs: false,
});

const props = defineProps({
  defaultValue: { type: [String, Number], required: false },
  modelValue: { type: [String, Number], required: false },
  class: { type: null, required: false },
  label: { type: String, required: false },
  hint: { type: String, required: false },
  error: { type: [Boolean, String], required: false },
  id: { type: String, required: false },
});

const emits = defineEmits(["update:modelValue"]);

const modelValue = useVModel(props, "modelValue", emits, {
  passive: true,
  defaultValue: props.defaultValue,
});

const inputId = props.id || (props.label ? `input-${Math.random().toString(36).slice(2, 9)}` : undefined);
</script>

<template>
  <div v-if="label || hint || (error && typeof error === 'string')" class="w-full flex flex-col gap-1.5">
    <label
      v-if="label"
      :for="inputId"
      class="text-xs font-semibold text-text-secondary uppercase tracking-wider"
    >
      {{ label }}
    </label>
    <input
      :id="inputId"
      v-model="modelValue"
      v-bind="$attrs"
      :aria-invalid="!!error"
      :class="
        cn(
          'flex h-10 w-full rounded-md border border-border-subtle bg-white px-3 py-2 text-sm text-text-primary ring-offset-background placeholder:text-text-placeholder transition-colors hover:border-slate-400 focus-visible:outline-none focus-visible:border-brand-primary focus-visible:ring-2 focus-visible:ring-brand-primary/20 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-text-muted disabled:opacity-60',
          error && 'border-status-danger text-status-danger focus-visible:border-status-danger focus-visible:ring-status-danger/20',
          props.class
        )
      "
    />
    <p v-if="error && typeof error === 'string'" class="text-xs text-status-danger font-medium m-0">
      {{ error }}
    </p>
    <p v-else-if="hint" class="text-xs text-text-secondary m-0">
      {{ hint }}
    </p>
  </div>
  <input
    v-else
    :id="id"
    v-model="modelValue"
    v-bind="$attrs"
    :aria-invalid="!!error"
    :class="
      cn(
        'flex h-10 w-full rounded-md border border-border-subtle bg-white px-3 py-2 text-sm text-text-primary ring-offset-background placeholder:text-text-placeholder transition-colors hover:border-slate-400 focus-visible:outline-none focus-visible:border-brand-primary focus-visible:ring-2 focus-visible:ring-brand-primary/20 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-text-muted disabled:opacity-60',
        error && 'border-status-danger text-status-danger focus-visible:border-status-danger focus-visible:ring-status-danger/20',
        props.class
      )
    "
  />
</template>
