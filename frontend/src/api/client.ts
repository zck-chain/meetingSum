import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const message = error.response.data?.detail || error.response.statusText
      console.error(`API Error [${error.response.status}]:`, message)
    } else if (error.request) {
      console.error('Network error:', error.message)
    }
    return Promise.reject(error)
  },
)
