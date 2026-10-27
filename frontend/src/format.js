// Small display helpers shared by several components.

export const CONTENT_TYPES = {
  PAGE: { icon: '📖', label: 'Page', endpoint: 'pages' },
  QUIZ: { icon: '📝', label: 'Quiz', endpoint: 'quizzes' },
  CODING_EXERCISE: { icon: '💻', label: 'Coding exercise', endpoint: 'coding-exercises' },
  ASSIGNMENT: { icon: '📤', label: 'Assignment', endpoint: 'assignments' },
}

export function isGraded(item) {
  return item.type !== 'PAGE'
}

/** "2026-10-30T23:59:00Z" -> local date and time, e.g. "30.10.2026, 23:59" */
export function formatDateTime(iso) {
  if (!iso) return '–'
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

/** Value of an <input type="datetime-local"> (local time) -> ISO instant for the API, or null. */
export function localInputToIso(value) {
  return value ? new Date(value).toISOString() : null
}

export function isLink(text) {
  return typeof text === 'string' && /^https?:\/\/\S+$/.test(text.trim())
}

export const STATUS_LABELS = {
  NOT_SUBMITTED: 'Not submitted',
  SUBMITTED: 'Submitted',
  GRADED: 'Graded',
}

/** 1536 -> "1.5 KB" */
export function formatSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** Must match lms.storage.allowed-extensions in the backend (the backend is what enforces it). */
export const ALLOWED_FILE_TYPES = '.pdf,.doc,.docx,.odt,.txt,.md,.zip,.png,.jpg,.jpeg'
