import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

const trackedFiles = execFileSync('git', ['ls-files'], { encoding: 'utf8' })
  .split(/\r?\n/)
  .filter(Boolean)

const checkedPrefixes = [
  '.github/workflows/',
  'backend/src/',
  'frontend/src/',
  'scripts/',
]

const checkedFiles = new Set([
  'backend/pom.xml',
  'docker-compose.yml',
  'frontend/package.json',
  'frontend/package-lock.json',
])

const checkedExtensions = new Set([
  '.java',
  '.js',
  '.json',
  '.md',
  '.sql',
  '.vue',
  '.xml',
  '.yaml',
  '.yml',
])

const conflictMarkerPattern = /^(<<<<<<<|=======|>>>>>>>)( .*)?$/
const trailingWhitespacePattern = /[ \t]+$/
const problems = []

function hasCheckedExtension(file) {
  return [...checkedExtensions].some((extension) => file.endsWith(extension))
}

function shouldCheck(file) {
  return checkedFiles.has(file) || checkedPrefixes.some((prefix) => file.startsWith(prefix))
}

for (const file of trackedFiles) {
  if (!shouldCheck(file) || !hasCheckedExtension(file)) {
    continue
  }

  const content = readFileSync(file, 'utf8')
  const lines = content.split(/\n/)

  lines.forEach((line, index) => {
    const normalizedLine = line.replace(/\r$/, '')
    const location = `${file}:${index + 1}`

    if (conflictMarkerPattern.test(normalizedLine)) {
      problems.push(`${location} contains an unresolved Git conflict marker`)
    }

    if (trailingWhitespacePattern.test(normalizedLine)) {
      problems.push(`${location} contains trailing whitespace`)
    }

    if (normalizedLine.includes('\uFFFD')) {
      problems.push(`${location} contains a replacement character`)
    }
  })
}

if (problems.length > 0) {
  console.error('Static/format checks failed:')
  for (const problem of problems) {
    console.error(`- ${problem}`)
  }
  process.exit(1)
}

console.log('Static/format checks passed.')
