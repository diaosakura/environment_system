// 本地预览服务器：静态托管 H5 构建产物 + 把 /api 代理到后端 8080
// 用途：后端未开启 CORS，通过同源代理让 H5 能正常调用接口
// 用法：node preview-server.js  →  浏览器打开 http://localhost:5174
const http = require('http')
const fs = require('fs')
const path = require('path')

const ROOT = path.join(__dirname, 'dist', 'build', 'h5')
const PORT = 5174
const BACKEND = { host: '127.0.0.1', port: 8080 }

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json',
}

function proxyApi(req, res) {
  const options = {
    host: BACKEND.host,
    port: BACKEND.port,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `${BACKEND.host}:${BACKEND.port}` },
  }
  const upstream = http.request(options, (up) => {
    const headers = { ...up.headers }
    // 透传 Set-Cookie，前端才能保存 JSESSIONID
    res.writeHead(up.statusCode || 502, headers)
    up.pipe(res)
  })
  upstream.on('error', (err) => {
    res.writeHead(502, { 'content-type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify({ code: 502, message: '后端不可用：' + err.message, data: null }))
  })
  req.pipe(upstream)
}

http
  .createServer((req, res) => {
    if (req.url.startsWith('/api/')) {
      proxyApi(req, res)
      return
    }
    let filePath = path.join(ROOT, decodeURIComponent(req.url.split('?')[0]))
    if (req.url === '/' || !path.extname(filePath)) {
      filePath = path.join(ROOT, 'index.html')
    }
    fs.readFile(filePath, (err, data) => {
      if (err) {
        fs.readFile(path.join(ROOT, 'index.html'), (e2, html) => {
          if (e2) {
            res.writeHead(404)
            res.end('Not found')
            return
          }
          res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' })
          res.end(html)
        })
        return
      }
      if (filePath.endsWith('index.html')) {
        // 让 H5 默认把服务地址指向本代理，避免首次进入时跨域失败
        const injected = String(data).replace(
          '</head>',
          `<script>try{if(!localStorage.getItem('neps-server'))localStorage.setItem('neps-server','http://localhost:${PORT}')}catch(e){}</script></head>`,
        )
        res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' })
        res.end(injected)
        return
      }
      res.writeHead(200, { 'content-type': MIME[path.extname(filePath)] || 'application/octet-stream' })
      res.end(data)
    })
  })
  .listen(PORT, () => {
    console.log('H5 预览: http://localhost:' + PORT)
    console.log('/api 代理目标: http://' + BACKEND.host + ':' + BACKEND.port)
  })
