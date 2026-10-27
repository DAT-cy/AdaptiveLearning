import { Axios } from './Axios'

const data = (request) => request.then((response) => response.data)

export const learningService = {
  certificates: () => data(Axios.get('/api/v1/certificates')),
  examTemplates: () => data(Axios.get('/api/v1/exam-templates')),
  entitlements: () => data(Axios.get('/api/v1/me/entitlements')),
  generateExamFromTemplate: (templateId, payload) =>
    data(Axios.post(`/api/v1/exam-templates/${templateId}/generate`, payload)),
  exams: () => data(Axios.get('/api/v1/exams')),
  questions: () => data(Axios.get('/api/v1/questions')),
  exam: (id) => data(Axios.get(`/api/v1/exams/${id}/detail`)),
  question: (id) => data(Axios.get(`/api/v1/questions/${id}`)),
  startAttempt: (examId) => data(Axios.post('/api/v1/attempts/start', { examId })),
  attempt: (id) => data(Axios.get(`/api/v1/attempts/${id}`)),
  saveAnswer: (id, answer) => data(Axios.patch(`/api/v1/attempts/${id}/answers`, answer)),
  submitAttempt: (id, answers) => data(Axios.post(`/api/v1/attempts/${id}/submit`, { answers })),
  history: () => data(Axios.get('/api/v1/attempts/me/history')),
  recommendations: () => data(Axios.get('/api/v1/analytics/me/recommendations')),
  startRecommendation: (id) => data(Axios.post(`/api/v1/analytics/recommendations/${id}/start`)),
  progress: (certificateId) => data(Axios.get('/api/v1/analytics/me/progress', { certificateId })),
  adaptiveRecommendations: (certificateId) => data(Axios.get('/api/v1/adaptive/recommend', { certificateId })),
  plans: () => data(Axios.get('/api/v1/plans')),
  createPlan: (payload) => data(Axios.post('/api/v1/plans', payload)),
  planSummary: (id) => data(Axios.get(`/api/v1/plans/${id}/summary`)),
  practicePackages: (certificateId) => data(Axios.get('/api/v1/practice', certificateId ? { certificateId } : {})),
  conversations: () => data(Axios.get('/api/v1/tutor/conversations')),
  createConversation: (payload) => data(Axios.post('/api/v1/tutor/conversations', payload)),
  messages: (id) => data(Axios.get(`/api/v1/tutor/conversations/${id}/messages`)),
  askTutor: (id, payload) => data(Axios.post(`/api/v1/tutor/conversations/${id}/messages`, payload)),
}
