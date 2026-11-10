// All calls to the Spring Boot REST API go through this file.
//
// After login the backend returns a JWT. We keep it in sessionStorage so a page reload
// does not log you out, and send it as "Authorization: Bearer <token>" on every request.
// sessionStorage is cleared when the browser tab closes.

const TOKEN_KEY = 'lms_token'

let token = readStoredToken()
let onUnauthorized = () => {}

function readStoredToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(newToken) {
  token = newToken
  try {
    if (newToken) sessionStorage.setItem(TOKEN_KEY, newToken)
    else sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // storage may be unavailable (e.g. private mode); the token still works in memory
  }
}

export function hasToken() {
  return !!token
}

/** Called when the backend says our token is no longer valid (e.g. expired). */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

/** An error response from the API. message is the backend's explanation (ProblemDetail.detail). */
export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

/** Sends a request and returns the raw Response, throwing ApiError for error statuses. */
async function send(method, path, body) {
  const headers = {}
  const isForm = body instanceof FormData
  // For FormData (file uploads) the browser sets the multipart Content-Type itself
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is Spring Boot running?')
  }

  if (response.status === 401 && token) {
    setToken(null)
    onUnauthorized()
  }
  if (!response.ok) {
    let message = response.statusText
    try {
      const problem = await response.json()
      message = problem.detail || problem.title || message
    } catch {
      // response had no JSON body
    }
    throw new ApiError(response.status, message)
  }
  return response
}

async function request(method, path, body) {
  const response = await send(method, path, body)
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

/**
 * Downloads a protected file. A plain <a href> can't send our login token, so we fetch the
 * file with the token and then hand it to the browser as a download.
 */
async function download(path, filename) {
  const response = await send('GET', path)
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const get = (path) => request('GET', path)
const post = (path, body = {}) => request('POST', path, body)
const put = (path, body = {}) => request('PUT', path, body)
const del = (path) => request('DELETE', path)

export const api = {
  // auth & users
  register: (email, name, password) => post('/auth/register', { email, name, password }),
  login: (email, password) => post('/auth/login', { email, password }),
  me: () => get('/users/me'),
  myEnrollments: () => get('/users/me/enrollments'),

  // admin only: who is a teacher (may create courses) or an admin
  adminUsers: () => get('/admin/users'),
  setAccountRole: (userId, role) => put(`/admin/users/${userId}/role`, { role }),

  // courses & participants
  createCourse: (course) => post('/courses', course),
  getCourse: (id) => get(`/courses/${id}`),
  updateCourse: (id, course) => put(`/courses/${id}`, course),
  deleteCourse: (id) => del(`/courses/${id}`),
  participants: (courseId) => get(`/courses/${courseId}/enrollments`),
  enroll: (courseId, email, role) => post(`/courses/${courseId}/enrollments`, { email, role }),
  unenroll: (courseId, userId) => del(`/courses/${courseId}/enrollments/${userId}`),

  // modules
  modules: (courseId) => get(`/courses/${courseId}/modules`),
  createModule: (courseId, title) => post(`/courses/${courseId}/modules`, { title }),
  renameModule: (moduleId, title) => put(`/modules/${moduleId}`, { title }),
  deleteModule: (moduleId) => del(`/modules/${moduleId}`),
  addRequirement: (moduleId, requiredId) => put(`/modules/${moduleId}/requires/${requiredId}`),
  removeRequirement: (moduleId, requiredId) => del(`/modules/${moduleId}/requires/${requiredId}`),

  // contents (type is one of: pages, quizzes, coding-exercises, assignments)
  contents: (moduleId) => get(`/modules/${moduleId}/contents`),
  addContent: (moduleId, type, item) => post(`/modules/${moduleId}/${type}`, item),
  getContent: (id) => get(`/contents/${id}`),
  deleteContent: (id) => del(`/contents/${id}`),

  // submissions & grading
  /** Hand in a text answer and/or a file. With a file it is sent as multipart/form-data. */
  submit: (contentId, answer, file) => {
    if (!file) return post(`/contents/${contentId}/submissions`, { answer })
    const form = new FormData()
    if (answer) form.append('answer', answer)
    form.append('file', file)
    return post(`/contents/${contentId}/submissions`, form)
  },
  downloadSubmissionFile: (submissionId, filename) => download(`/submissions/${submissionId}/file`, filename),
  submissions: (contentId) => get(`/contents/${contentId}/submissions`),
  grade: (submissionId, points, feedback) => put(`/submissions/${submissionId}/grade`, { points, feedback }),
  publishGrades: (contentId, published) => put(`/contents/${contentId}/grades-published`, { published }),
  gradebook: (courseId) => get(`/courses/${courseId}/gradebook`),
  myGrades: (courseId) => get(`/courses/${courseId}/my-grades`),

  // quizzes (kind is one of: multiple-choice, ordering, matching, fill-in-blanks)
  quizQuestions: (quizId) => get(`/contents/${quizId}/questions`),                  // teachers: with solutions
  addQuestion: (quizId, kind, question) => post(`/contents/${quizId}/questions/${kind}`, question),
  deleteQuestion: (questionId) => del(`/questions/${questionId}`),
  quizForStudent: (quizId) => get(`/contents/${quizId}/questions/student-view`),   // no solutions
  attemptQuiz: (quizId, answers) => post(`/contents/${quizId}/attempts`, { answers }),
  myQuizAttempts: (quizId) => get(`/contents/${quizId}/attempts/mine`),

  // notifications (created from RabbitMQ domain events)
  notifications: () => get('/notifications'),
  markNotificationRead: (id) => put(`/notifications/${id}/read`),
  markAllNotificationsRead: () => put('/notifications/read-all'),

  // groups
  groups: (courseId) => get(`/courses/${courseId}/groups`),
  createGroup: (courseId, name) => post(`/courses/${courseId}/groups`, { name }),
  addGroupMember: (groupId, email) => post(`/groups/${groupId}/members`, { email }),
  removeGroupMember: (groupId, userId) => del(`/groups/${groupId}/members/${userId}`),
}
