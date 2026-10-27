import type { Component } from 'vue'

export type OverlaySize = 'sm' | 'md' | 'lg' | 'xl'
export type OverlayVariant = 'default' | 'danger' | 'success'

/** Options shared by confirmation requests throughout the app. */
export interface ConfirmOptions {
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  variant?: 'default' | 'danger'
}

export interface ConfirmRequest extends ConfirmOptions {
  id: number
  resolve: (confirmed: boolean) => void
}

/** Action rendered as a link-style control in a toast. */
export interface ToastActionOption {
  label: string
  altText?: string
  onClick: () => void
}

/** Optional description/action options accepted by the toast API. */
export interface OverlayToastOptions {
  description?: string
  action?: ToastActionOption
  duration?: number
}

export interface OverlayToast extends OverlayToastOptions {
  id: number
  type: 'success' | 'error' | 'info'
  title: string
  duration: number
}

export interface ModalProps {
  open: boolean
  title: string
  description?: string
  icon?: Component
  size?: OverlaySize
  variant?: OverlayVariant
  footer?: boolean
  dismissible?: boolean
  dirty?: boolean
  submitting?: boolean
}
