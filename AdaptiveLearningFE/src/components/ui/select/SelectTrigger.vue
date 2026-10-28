<script setup>
import { computed } from "vue";
import { SelectIcon, SelectTrigger, useForwardProps } from "reka-ui";
import { ChevronDown } from "lucide-vue-next";
import { cn } from "@/lib/utils";

const props = defineProps({
  disabled: { type: Boolean, required: false },
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
});

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props;

  return delegated;
});

const forwardedProps = useForwardProps(delegatedProps);
</script>

<template>
  <SelectTrigger
    v-bind="forwardedProps"
    :class="
      cn(
        'flex h-10 w-full items-center justify-between rounded-md border border-border-subtle bg-white px-3 py-2 text-sm text-text-primary ring-offset-background placeholder:text-text-placeholder transition-colors hover:border-slate-400 focus:outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 focus:ring-offset-1 disabled:cursor-not-allowed disabled:bg-surface-muted disabled:opacity-60 [&>span]:line-clamp-1',
        props.class
      )
    "
  >
    <slot />
    <SelectIcon as-child>
      <ChevronDown class="w-4 h-4 text-text-secondary opacity-60" />
    </SelectIcon>
  </SelectTrigger>
</template>
