import { cva } from "class-variance-authority";

export { default as Badge } from "./Badge.vue";

export const badgeVariants = cva(
  "inline-flex items-center rounded-full border font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-brand-primary/20",
  {
    variants: {
      variant: {
        default:
          "border-brand-primary/20 bg-brand-primary-soft text-brand-primary",
        secondary:
          "border-border-subtle bg-surface-muted text-text-secondary",
        success:
          "border-emerald-200 bg-emerald-50 text-emerald-700",
        warning:
          "border-amber-200 bg-amber-50 text-amber-700",
        destructive:
          "border-red-200 bg-red-50 text-red-700",
        info:
          "border-sky-200 bg-sky-50 text-sky-700",
        outline:
          "border-border-subtle bg-white text-text-secondary",
        ai:
          "border-ai-accent-border bg-ai-accent-soft text-ai-accent font-medium shadow-xs",
      },
      size: {
        default: "px-2.5 py-0.5 text-xs gap-1.5",
        sm: "px-2 py-0.2 text-[11px] gap-1",
        lg: "px-3 py-1 text-xs gap-2 font-semibold",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);
