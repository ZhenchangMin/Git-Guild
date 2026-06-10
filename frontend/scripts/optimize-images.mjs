// One-off: convert the shipped background PNGs to WebP (q80) to slash first-load weight.
// PNG is the wrong format for these painted/photographic backgrounds; WebP q80 cuts them
// ~85-90% with no visible loss. Run from frontend/: `node scripts/optimize-images.mjs`.
// Originals (.png) are kept untouched; only referenced assets are converted.
import sharp from 'sharp';
import { readFileSync, statSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const assets = join(here, '..', 'src', 'assets');

// Only the 10 PNGs actually imported by the app (verified via grep).
const targets = [
  'desk.png',
  'door.png',
  'hall.png',
  'leader board wall.png',
  'operation room.png',
  'profile-archive-bg.png',
  'quest board.png',
  'submission-counter-clerk-v0.png',
  'submission-form-parchment-v0-clean.png',
  'workbench.png',
];

const mb = (n) => (n / 1048576).toFixed(2);
let before = 0;
let after = 0;

for (const name of targets) {
  const src = join(assets, name);
  if (!existsSync(src)) {
    console.warn(`SKIP (missing): ${name}`);
    continue;
  }
  const out = src.replace(/\.png$/i, '.webp');
  const srcSize = statSync(src).size;
  // q80 is the sweet spot for painted art; effort 6 = max compression search.
  await sharp(src).webp({ quality: 80, effort: 6 }).toFile(out);
  const outSize = statSync(out).size;
  before += srcSize;
  after += outSize;
  const pct = (100 * (1 - outSize / srcSize)).toFixed(0);
  console.log(`${name.padEnd(42)} ${mb(srcSize).padStart(6)}MB -> ${mb(outSize).padStart(6)}MB  (-${pct}%)`);
}

console.log('-'.repeat(70));
console.log(`TOTAL ${mb(before)}MB -> ${mb(after)}MB  (-${(100 * (1 - after / before)).toFixed(0)}%)`);
