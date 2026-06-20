import { sessionStore } from '../stores/sessionStore'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export class ApiError extends Error {
  constructor({ status, code, message, details }) {
    super(message || code || '请求失败')
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

export async function request(endpoint, options = {}) {
  const headers = new Headers(options.headers ?? {})
  const hasBody = options.body !== undefined && options.body !== null
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData

  if (hasBody && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (sessionStore.token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${sessionStore.token}`)
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
    body: hasBody && !isFormData && typeof options.body !== 'string' ? JSON.stringify(options.body) : options.body,
  })

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json') ? await response.json() : null

  if (!response.ok) {
    throw new ApiError({
      status: response.status,
      code: payload?.code,
      message: payload?.message,
      details: payload?.details,
    })
  }

  return payload
}

export async function requestMock(endpoint, options = {}) {
  return {
    code: 'OK',
    message: 'mock',
    data: {
      endpoint,
      method: options.method ?? 'GET',
    },
  }
}
