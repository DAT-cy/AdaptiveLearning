import { Axios } from './Axios'

const data = (request) =>
  request.then((response) => response.data?.data ?? response.data)

export const adminService = {
  analytics: (filters = {}) => data(Axios.get('/api/v1/admin/users/analytics', filters)),
  users: () => data(Axios.get('/api/v1/admin/users')),
  userAttempts: (userId) => data(Axios.get(`/api/v1/admin/users/${userId}/attempts`)),
  userProgress: (userId, certificateId) => data(Axios.get(`/api/v1/admin/users/${userId}/progress`, { certificateId })),
  attemptResult: (attemptId) => data(Axios.get(`/api/v1/attempts/${attemptId}/result`)),
  createUser: (payload) => data(Axios.post('/api/v1/admin/users', payload)),
  updateUserRole: (userId, role) =>
    data(Axios.put(`/api/v1/admin/users/${userId}/role`, { role })),
  updateUserStatus: (userId, enabled) =>
    data(Axios.patch(`/api/v1/admin/users/${userId}/status`, { enabled })),
  updateUserPlan: (userId, plan) =>
    data(Axios.put(`/api/v1/admin/users/${userId}/plan`, { plan })),
  payments: () => data(Axios.get('/api/v1/admin/users/payments')),
  plans: () => data(Axios.get('/api/v1/admin/plans')),
  createPlan: (payload) => data(Axios.post('/api/v1/admin/plans', payload)),
  updatePlan: (id, payload) => data(Axios.put(`/api/v1/admin/plans/${id}`, payload)),
  togglePlan: (id) => data(Axios.patch(`/api/v1/admin/plans/${id}/toggle`)),
  deletePlan: (id) => data(Axios.delete(`/api/v1/admin/plans/${id}`)),
  userBillings: () => data(Axios.get('/api/v1/admin/billing/users')),
  userPayments: (userId) => data(Axios.get(`/api/v1/admin/billing/users/${userId}/payments`)),
  resetUserQuota: (userId) => data(Axios.post(`/api/v1/admin/billing/users/${userId}/reset-quota`)),
  certificates: () => data(Axios.get('/api/v1/certificates')),
  exams: () => data(Axios.get('/api/v1/exams')),
  examTemplates: () => data(Axios.get('/api/v1/exam-templates')),
  createExamTemplate: (payload) =>
    data(Axios.post('/api/v1/exam-templates', payload)),
  generateExamFromTemplate: (id, payload) =>
    data(Axios.post(`/api/v1/exam-templates/${id}/generate`, payload)),
  deactivateExamTemplate: (id) =>
    data(Axios.delete(`/api/v1/exam-templates/${id}`)),
  questions: (filters = {}) => data(Axios.get('/api/v1/questions', filters)),
  knowledgeDocuments: () => data(Axios.get('/api/v1/admin/knowledge-base')),
  knowledgeDocument: (id) =>
    data(Axios.get(`/api/v1/admin/knowledge-base/${id}`)),
  updateKnowledgeDocument: (id, payload) =>
    data(Axios.put(`/api/v1/admin/knowledge-base/${id}`, payload)),
  setKnowledgeDocumentActive: (id, active) =>
    data(Axios.patch(`/api/v1/admin/knowledge-base/${id}/active`, { active })),
  uploadKnowledgeDocument: (payload) =>
    data(
      Axios.post('/api/v1/admin/knowledge-base', payload, { timeout: 30000 })
    ),
  uploadKnowledgePdf: (formData) =>
    data(
      Axios.post('/api/v1/admin/knowledge-base/pdf', formData, {
        timeout: 120000,
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    ),
  deleteKnowledgeDocument: (id) =>
    data(Axios.delete(`/api/v1/admin/knowledge-base/${id}`)),
  topics: (certificateId) =>
    data(Axios.get(`/api/v1/topics/certificate/${certificateId}`)),
  createQuestion: (payload) => data(Axios.post('/api/v1/questions', payload)),
  updateQuestion: (id, payload) =>
    data(Axios.put(`/api/v1/questions/${id}`, payload)),
  generateQuestions: (payload) =>
    data(
      Axios.post('/api/v1/content-generation', payload, { timeout: 120000 })
    ),
  applyGeneratedQuestions: (id) =>
    data(Axios.post(`/api/v1/content-generation/${id}/apply`)),
  submitQuestionReview: (id) =>
    data(Axios.post(`/api/v1/questions/${id}/submit-review`)),
  approveQuestion: (id) => data(Axios.post(`/api/v1/questions/${id}/approve`)),
  rejectQuestion: (id) => data(Axios.post(`/api/v1/questions/${id}/reject`)),
  createCertificate: (payload) =>
    data(Axios.post('/api/v1/certificates', payload)),
  updateCertificate: (id, payload) =>
    data(Axios.put(`/api/v1/certificates/${id}`, payload)),
  deleteCertificate: (id) => data(Axios.delete(`/api/v1/certificates/${id}`)),
}
