import type { Component } from 'vue'

interface ModalOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'info' | 'warning' | 'error'
}

interface ModalState {
  visible: boolean
  options: ModalOptions
  resolve: ((value: boolean) => void) | null
}

export const useModal = () => {
  // useState 必须在 setup / composable 调用链中执行，不能放模块顶层
  const state = useState<ModalState>('app-modal', () => ({
    visible: false,
    options: { message: '' },
    resolve: null,
  }))

  function open(options: ModalOptions): Promise<boolean> {
    return new Promise((resolve) => {
      state.value = {
        visible: true,
        options,
        resolve,
      }
    })
  }

  function confirm(options: ModalOptions | string): Promise<boolean> {
    const opts = typeof options === 'string' ? { message: options } : options
    return open({ ...opts, type: opts.type || 'warning' })
  }

  function alert(options: ModalOptions | string): Promise<boolean> {
    const opts = typeof options === 'string' ? { message: options } : options
    return open({ ...opts, type: opts.type || 'info' })
  }

  function close(result: boolean) {
    const s = state.value
    s.visible = false
    s.resolve?.(result)
    s.resolve = null
  }

  return { state, confirm, alert, close }
}
