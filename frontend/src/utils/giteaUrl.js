// Gitea 仓库 / Issue 地址规整。
//
// 后端导入仓库时把 source_url 回填成内网地址（如 http://127.0.0.1:3000/owner/repo），
// 浏览器在用户机器上无法访问这个回环地址。这里把回环 host 换成「当前页面的 hostname」，
// 端口与路径保持不变——因为 Gitea 与 Web 前端同机部署（同 IP，Gitea 固定在 :3000）。
// 这样无论部署到哪个 IP/域名都自动适配，无需后端重打包。

const LOOPBACK_HOSTS = new Set(['127.0.0.1', 'localhost', '0.0.0.0', '::1'])

/**
 * 把可能含回环 host 的 Gitea 地址转换成浏览器可点开的地址。
 * 非回环地址、空值或非法 URL 原样返回。
 * @param {string} rawUrl 原始地址
 * @returns {string} 浏览器可访问的地址
 */
export function toBrowsableGiteaUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') return rawUrl
  try {
    const url = new URL(rawUrl)
    const pageHost = typeof window !== 'undefined' ? window.location?.hostname : ''
    if (LOOPBACK_HOSTS.has(url.hostname) && pageHost) {
      url.hostname = pageHost
    }
    return url.toString()
  } catch {
    return rawUrl
  }
}
