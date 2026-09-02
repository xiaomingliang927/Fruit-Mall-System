#!/usr/bin/env node
// 轻量压测工具：node stress-test.mjs <url> [并发数] [持续秒数]
// 无第三方依赖，keep-alive 长连接，统计 QPS / 平均 / P50 / P95 / P99
import http from 'node:http'
import { performance } from 'node:perf_hooks'

const [url = 'http://localhost:8080/api/v1/products?page=1&size=10', concArg = '100', durArg = '15'] = process.argv.slice(2)
const CONCURRENCY = Math.max(1, Number(concArg))
const DURATION_MS = Math.max(1, Number(durArg)) * 1000

const target = new URL(url)
let ok = 0, fail = 0
const latencies = []
let running = true

const agent = new http.Agent({ keepAlive: true, maxSockets: CONCURRENCY })

function one() {
  return new Promise((resolve) => {
    const t0 = performance.now()
    const req = http.request(
      { hostname: target.hostname, port: target.port || 80, path: target.pathname + target.search, method: 'GET', agent },
      (res) => {
        res.resume()
        res.on('end', () => {
          const dt = performance.now() - t0
          if (res.statusCode === 200) { ok++; latencies.push(dt) } else fail++
          resolve()
        })
      })
    req.on('error', () => { fail++; resolve() })
    req.end()
  })
}

async function worker() {
  while (running) await one()
}

console.log(`目标: ${url}`)
console.log(`并发: ${CONCURRENCY} | 时长: ${DURATION_MS / 1000}s`)
const workers = Array.from({ length: CONCURRENCY }, worker)
const timer = setTimeout(() => { running = false }, DURATION_MS)
await Promise.all(workers)
clearTimeout(timer)

latencies.sort((a, b) => a - b)
const total = ok + fail
const pct = (p) => latencies.length ? latencies[Math.min(latencies.length - 1, Math.floor(latencies.length * p / 100))].toFixed(1) : '-'
const avg = latencies.length ? (latencies.reduce((a, b) => a + b, 0) / latencies.length).toFixed(1) : '-'

console.log('──────────────────────────────')
console.log(`总请求: ${total}  成功: ${ok}  失败: ${fail}`)
console.log(`QPS: ${(total / (DURATION_MS / 1000)).toFixed(0)}`)
console.log(`平均延迟: ${avg} ms`)
console.log(`P50: ${pct(50)} ms | P95: ${pct(95)} ms | P99: ${pct(99)} ms`)
