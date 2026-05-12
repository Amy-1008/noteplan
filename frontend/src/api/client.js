import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.code === 'ECONNABORTED') {
      console.error(err)
      return Promise.reject(new Error('请求超时，请检查服务是否启动'))
    }
    console.error(err)
    return Promise.reject(err)
  }
)

export function fetchHealth() {
  return client.get('/health')
}

export default client
