<script setup>
import { computed, onMounted, ref } from 'vue'

import { adminApi } from '../../api/adminApi'
import { questDifficulties, tagPalette } from '../../data/adminTaxonomy'

const categories = ref([])
const tags = ref([])
const difficulties = questDifficulties

const newCategory = ref({ name: '', description: '' })
const newTag = ref({ name: '', color: tagPalette[0] })
// 自定义取色：独立保存最近一次自定义值，不被预设点击覆盖。
const customColor = ref('#c84b8f')
const categoryNotice = ref(null)
const tagNotice = ref(null)
const loading = ref(true)

// 当前选色不在预设色板中时，视为使用了自定义颜色。
const usingCustomColor = computed(() => !tagPalette.includes(newTag.value.color))

function pickCustomColor(event) {
  customColor.value = event.target.value
  newTag.value.color = event.target.value
}

const enabledCategoryCount = computed(() => categories.value.filter((c) => c.enabled).length)
const enabledTagCount = computed(() => tags.value.filter((t) => t.enabled).length)

function notice(target, tone, text) {
  const value = { tone, text }
  if (target === 'category') categoryNotice.value = value
  else tagNotice.value = value
}

// 管理台需要看到「停用」项以便重新启用，故 includeDisabled=true；分类要真实引用数。
onMounted(loadTaxonomy)

async function loadTaxonomy() {
  loading.value = true
  try {
    const [catRes, tagRes] = await Promise.all([
      adminApi.listCategories({ withQuestCount: true, includeDisabled: true, sortBy: 'name' }),
      adminApi.listTags({ includeDisabled: true, size: 100 }),
    ])
    categories.value = (catRes?.data ?? []).map(normalizeCategory)
    tags.value = (tagRes?.data?.items ?? tagRes?.data ?? []).map(normalizeTag)
  } catch (error) {
    const text = readableError(error, '加载分类/标签失败，请确认后端已启动并以管理员登录。')
    notice('category', 'danger', text)
    notice('tag', 'danger', text)
  } finally {
    loading.value = false
  }
}

function normalizeCategory(category) {
  return {
    categoryId: category.categoryId,
    name: category.name ?? '',
    description: category.description ?? '',
    enabled: category.enabled !== false,
    questCount: category.questCount ?? 0,
  }
}

function normalizeTag(tag) {
  return {
    tagId: tag.tagId,
    name: tag.name ?? '',
    color: tag.color ?? tagPalette[0],
    enabled: tag.enabled !== false,
    questCount: tag.questCount ?? 0,
  }
}

function readableError(error, fallback) {
  if (error?.details) return `${fallback} ${error.details}`
  if (error?.message) return `${fallback} ${error.message}`
  return fallback
}

async function addCategory() {
  const name = newCategory.value.name.trim()
  if (name.length < 1 || name.length > 32) {
    notice('category', 'danger', '分类名称长度需为 1-32 个字符。')
    return
  }
  if (categories.value.some((c) => c.name === name)) {
    notice('category', 'danger', `同名分类已存在：${name}`)
    return
  }
  try {
    const payload = { name, description: newCategory.value.description.trim(), enabled: true }
    const res = await adminApi.createCategory(payload)
    const created = res?.data ?? {}
    categories.value = [...categories.value, normalizeCategory({ ...payload, questCount: 0, ...created })]
    newCategory.value = { name: '', description: '' }
    notice('category', 'approved', `任务分类已创建：${created.name ?? name}`)
  } catch (error) {
    notice('category', 'danger', readableError(error, '创建分类失败。'))
  }
}

async function toggleCategory(category) {
  // 被任务引用的分类不能停用，避免已有委托展示成不可解释的配置状态。
  if (category.enabled && category.questCount > 0) {
    notice('category', 'danger', `「${category.name}」被 ${category.questCount} 个任务引用，无法禁用。`)
    return
  }
  const nextEnabled = !category.enabled
  try {
    const payload = {
      name: category.name,
      description: category.description,
      enabled: nextEnabled,
    }
    const res = await adminApi.updateCategory(category.categoryId, payload)
    Object.assign(category, normalizeCategory({ ...category, ...payload, ...(res?.data ?? {}) }))
    notice('category', category.enabled ? 'approved' : 'return', `「${category.name}」已${category.enabled ? '启用' : '停用'}。`)
  } catch (error) {
    notice('category', 'danger', readableError(error, '更新分类失败。'))
  }
}

async function addTag() {
  const name = newTag.value.name.trim()
  if (name.length < 1) {
    notice('tag', 'danger', '标签名称不能为空。')
    return
  }
  if (tags.value.some((t) => t.name === name)) {
    notice('tag', 'danger', `同名标签已存在：${name}`)
    return
  }
  try {
    const payload = { name, color: newTag.value.color, enabled: true }
    const res = await adminApi.createTag(payload)
    const created = res?.data ?? {}
    tags.value = [...tags.value, normalizeTag({ ...payload, questCount: 0, ...created })]
    newTag.value = { name: '', color: tagPalette[0] }
    notice('tag', 'approved', `任务标签已创建：${created.name ?? name}`)
  } catch (error) {
    notice('tag', 'danger', readableError(error, '创建标签失败。'))
  }
}

async function toggleTag(tag) {
  if (tag.enabled && tag.questCount > 0) {
    notice('tag', 'danger', `「${tag.name}」被 ${tag.questCount} 个任务引用，无法禁用。`)
    return
  }
  const nextEnabled = !tag.enabled
  try {
    const payload = {
      name: tag.name,
      color: tag.color,
      enabled: nextEnabled,
    }
    const res = await adminApi.updateTag(tag.tagId, payload)
    Object.assign(tag, normalizeTag({ ...tag, ...payload, ...(res?.data ?? {}) }))
    notice('tag', tag.enabled ? 'approved' : 'return', `「${tag.name}」已${tag.enabled ? '启用' : '停用'}。`)
  } catch (error) {
    notice('tag', 'danger', readableError(error, '更新标签失败。'))
  }
}
</script>

<template>
  <div class="admin-taxonomy-workspace">
    <!-- 分类 -->
    <section class="admin-tax-panel">
      <div class="admin-panel-head">
        <div>
          <p class="kicker">Categories</p>
          <h1>任务分类</h1>
        </div>
        <span>{{ loading ? '加载中…' : `${enabledCategoryCount} / ${categories.length} 启用` }}</span>
      </div>

      <form class="admin-tax-add" @submit.prevent="addCategory">
        <input v-model="newCategory.name" type="text" maxlength="32" placeholder="新分类名称（1-32 字）" />
        <input v-model="newCategory.description" type="text" maxlength="200" placeholder="分类说明（可选）" />
        <button type="submit" class="primary-action">新增分类</button>
      </form>

      <p v-if="categoryNotice" class="admin-tax-notice" :class="categoryNotice.tone">{{ categoryNotice.text }}</p>

      <ul class="admin-tax-list">
        <li v-for="category in categories" :key="category.categoryId" :class="{ disabled: !category.enabled }">
          <div class="admin-tax-row-main">
            <strong>{{ category.name }}</strong>
            <small>{{ category.description || '—' }}</small>
          </div>
          <span class="admin-tax-count">{{ category.questCount }} 引用</span>
          <button
            type="button"
            class="admin-tax-toggle"
            :class="category.enabled ? 'on' : 'off'"
            @click="toggleCategory(category)"
          >
            {{ category.enabled ? '启用中' : '已停用' }}
          </button>
        </li>
      </ul>
    </section>

    <!-- 标签 -->
    <section class="admin-tax-panel">
      <div class="admin-panel-head">
        <div>
          <p class="kicker">Tags</p>
          <h1>任务标签</h1>
        </div>
        <span>{{ loading ? '加载中…' : `${enabledTagCount} / ${tags.length} 启用` }}</span>
      </div>

      <form class="admin-tax-add admin-tax-add-tag" @submit.prevent="addTag">
        <input v-model="newTag.name" type="text" placeholder="新标签名称" />
        <button type="submit" class="primary-action">新增标签</button>
        <div class="admin-tax-palette" role="radiogroup" aria-label="标签颜色">
          <button
            v-for="color in tagPalette"
            :key="color"
            type="button"
            class="admin-tax-swatch"
            :class="{ active: newTag.color === color }"
            :style="{ background: color }"
            :aria-label="`选择颜色 ${color}`"
            @click="newTag.color = color"
          ></button>
          <label
            class="admin-tax-custom"
            :class="{ active: usingCustomColor }"
            title="自定义颜色"
            :style="{ background: customColor }"
          >
            <input
              type="color"
              :value="customColor"
              aria-label="自定义标签颜色"
              @input="pickCustomColor"
            />
            <span aria-hidden="true">＋</span>
          </label>
        </div>
      </form>

      <p v-if="tagNotice" class="admin-tax-notice" :class="tagNotice.tone">{{ tagNotice.text }}</p>

      <ul class="admin-tax-list">
        <li v-for="tag in tags" :key="tag.tagId" :class="{ disabled: !tag.enabled }">
          <span class="admin-tax-dot" :style="{ background: tag.color }" aria-hidden="true"></span>
          <div class="admin-tax-row-main">
            <strong>{{ tag.name }}</strong>
          </div>
          <span class="admin-tax-count">{{ tag.questCount }} 引用</span>
          <button
            type="button"
            class="admin-tax-toggle"
            :class="tag.enabled ? 'on' : 'off'"
            @click="toggleTag(tag)"
          >
            {{ tag.enabled ? '启用中' : '已停用' }}
          </button>
        </li>
      </ul>
    </section>

    <!-- 难度（固定枚举，只读） -->
    <section class="admin-tax-panel admin-tax-difficulty">
      <div class="admin-panel-head">
        <div>
          <p class="kicker">Difficulty</p>
          <h1>难度选项</h1>
        </div>
        <span>固定枚举 · 只读</span>
      </div>
      <div class="admin-difficulty-grid">
        <article v-for="level in difficulties" :key="level.code">
          <span class="admin-difficulty-code">{{ level.code }}</span>
          <div>
            <strong>{{ level.label }}</strong>
            <small>{{ level.hint }}</small>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
