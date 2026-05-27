<script setup>
import { computed, ref } from 'vue'

import { adminApi } from '../../api/adminApi'
import { questCategories, questDifficulties, questTags, tagPalette } from '../../data/adminTaxonomy'

const categories = ref(questCategories.map((item) => ({ ...item })))
const tags = ref(questTags.map((item) => ({ ...item })))
const difficulties = questDifficulties

const newCategory = ref({ name: '', description: '' })
const newTag = ref({ name: '', color: tagPalette[0] })
// 自定义取色：独立保存最近一次自定义值，不被预设点击覆盖。
const customColor = ref('#c84b8f')
const categoryNotice = ref(null)
const tagNotice = ref(null)

// 当前选色不在预设色板中时，视为使用了自定义颜色。
const usingCustomColor = computed(() => !tagPalette.includes(newTag.value.color))

function pickCustomColor(event) {
  customColor.value = event.target.value
  newTag.value.color = event.target.value
}

let categorySeq = Math.max(0, ...categories.value.map((c) => c.categoryId))
let tagSeq = Math.max(0, ...tags.value.map((t) => t.tagId))

const enabledCategoryCount = computed(() => categories.value.filter((c) => c.enabled).length)
const enabledTagCount = computed(() => tags.value.filter((t) => t.enabled).length)

function notice(target, tone, text) {
  const value = { tone, text }
  if (target === 'category') categoryNotice.value = value
  else tagNotice.value = value
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
  const payload = { name, description: newCategory.value.description.trim(), enabled: true }
  await adminApi.createCategory(payload)
  categorySeq += 1
  categories.value = [...categories.value, { categoryId: categorySeq, questCount: 0, ...payload }]
  newCategory.value = { name: '', description: '' }
  notice('category', 'approved', `任务分类已创建：${name}`)
}

async function toggleCategory(category) {
  // 业务规则：被任务引用的分类不能禁用。
  if (category.enabled && category.questCount > 0) {
    notice('category', 'danger', `「${category.name}」被 ${category.questCount} 个任务引用，无法禁用。`)
    return
  }
  await adminApi.updateCategory(category.categoryId, { enabled: !category.enabled })
  category.enabled = !category.enabled
  notice('category', category.enabled ? 'approved' : 'return', `「${category.name}」已${category.enabled ? '启用' : '停用'}。`)
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
  const payload = { name, color: newTag.value.color, enabled: true }
  await adminApi.createTag(payload)
  tagSeq += 1
  tags.value = [...tags.value, { tagId: tagSeq, questCount: 0, ...payload }]
  newTag.value = { name: '', color: tagPalette[0] }
  notice('tag', 'approved', `任务标签已创建：${name}`)
}

async function toggleTag(tag) {
  if (tag.enabled && tag.questCount > 0) {
    notice('tag', 'danger', `「${tag.name}」被 ${tag.questCount} 个任务引用，无法禁用。`)
    return
  }
  await adminApi.updateTag(tag.tagId, { enabled: !tag.enabled })
  tag.enabled = !tag.enabled
  notice('tag', tag.enabled ? 'approved' : 'return', `「${tag.name}」已${tag.enabled ? '启用' : '停用'}。`)
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
        <span>{{ enabledCategoryCount }} / {{ categories.length }} 启用</span>
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
        <span>{{ enabledTagCount }} / {{ tags.length }} 启用</span>
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
