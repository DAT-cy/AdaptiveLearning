import { cva } from "class-variance-authority";

export { default as Button } from "./Button.vue";

export const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary/30 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]",
  {
    variants: {
      variant: {
        default:
          "bg-brand-primary text-white hover:bg-brand-primary-hover shadow-sm hover:shadow",
        destructive:
          "bg-status-danger text-white hover:bg-red-600 shadow-sm active:scale-[0.98]",
        outline:
          "border border-border-subtle bg-white text-text-primary hover:bg-brand-primary-soft hover:text-brand-primary hover:border-brand-primary/40 transition-colors shadow-xs",
        secondary:
          "bg-surface-muted text-text-primary hover:bg-slate-200 border border-border-subtle shadow-xs",
        ghost:
          "text-text-secondary hover:bg-surface-muted hover:text-text-primary",
        link:
          "text-brand-primary underline-offset-4 hover:underline font-medium p-0 h-auto",
        ai:
          "bg-gradient-to-r from-brand-primary to-ai-accent text-white hover:opacity-95 shadow-sm active:scale-[0.98]",
      },
      size: {
        default: "h-10 px-4 py-2 gap-2",
        sm: "h-8 px-3 rounded-md text-xs gap-1.5",
        lg: "h-11 px-6 rounded-lg text-base font-semibold gap-2.5",
        icon: "h-10 w-10",
        "icon-sm": "h-8 w-8 rounded-md",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);
