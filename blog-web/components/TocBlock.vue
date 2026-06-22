<template>
  <nav v-if="headings.length > 0" class="toc" ref="tocRef">
    <div class="toc-title">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="18"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
      {{ t('common.toc') }}
    </div>
    <ul class="toc-list">
      <li
        v-for="h in headings"
        :key="h.id"
        class="toc-item"
        :class="{ 'toc-item--h3': h.level === 3, 'toc-item--active': activeId === h.id }"
      >
        <a :href="`#${h.id}`" class="toc-link" @click.prevent="scrollTo(h.id)">
          {{ h.text }}
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
const { t } = useI18n()

interface TocHeading {
  id: string
  text: string
  level: number
}

const props = defineProps<{ html: string }>()

const headings = ref<TocHeading[]>([])
const activeId = ref('')
const tocRef = ref<HTMLElement | null>(null)

let observer: IntersectionObserver | null = null

// Parse headings from HTML
watch(() => props.html, (html) => {
  if (!html || !import.meta.client) {
    headings.value = []
    return
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const result: TocHeading[] = []
  const seen = new Set<string>()

  doc.querySelectorAll('h2, h3').forEach((el) => {
    const text = el.textContent?.trim() || ''
    if (!text) return

    // Use existing id (flexmark generates them) or create a slug
    let id = el.id || text.toLowerCase().replace(/[^\w\u4e00-\u9fff]+/g, '-').replace(/^-|-$/g, '')
    // Deduplicate
    if (seen.has(id)) {
      id = `${id}-${seen.size}`
    }
    seen.add(id)

    result.push({
      id,
      text,
      level: el.tagName === 'H3' ? 3 : 2,
    })
  })

  headings.value = result

  // After DOM renders, attach ids to actual heading elements and set up observer
  nextTick(() => setupObserver())
}, { immediate: true })

function scrollTo(id: string) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeId.value = id
  }
}

function setupObserver() {
  observer?.disconnect()

  // Ensure heading elements have IDs
  headings.value.forEach((h) => {
    const el = document.getElementById(h.id)
    if (!el) {
      // Try to find by text match if flexmark generated a different id
      const allHeadings = document.querySelectorAll('.article-content h2, .article-content h3')
      allHeadings.forEach((heading) => {
        if (heading.textContent?.trim() === h.text && !heading.id) {
          heading.id = h.id
        }
      })
    }
  })

  const elements = headings.value
    .map((h) => document.getElementById(h.id))
    .filter(Boolean) as HTMLElement[]

  if (elements.length === 0) return

  observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          activeId.value = entry.target.id
          break
        }
      }
    },
    { rootMargin: '-80px 0px -70% 0px', threshold: 0 },
  )

  elements.forEach((el) => observer!.observe(el))
}

onBeforeUnmount(() => {
  observer?.disconnect()
})
</script>

<style scoped>
.toc {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  padding: var(--space-4) 0;
}

.toc-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-heading);
  font-size: var(--font-size-xs);
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 var(--space-4);
  margin-bottom: var(--space-3);
}

.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.toc-item {
  position: relative;
}

.toc-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
  transition: background var(--transition-fast);
}

.toc-item--active::before {
  background: var(--primary);
}

.toc-link {
  display: block;
  padding: 0.3rem var(--space-4);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-decoration: none;
  line-height: 1.5;
  border-left: 3px solid transparent;
  transition: all var(--transition-fast);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-link:hover {
  color: var(--primary);
}

.toc-item--active .toc-link {
  color: var(--primary);
  font-weight: 600;
}

.toc-item--h3 .toc-link {
  padding-left: calc(var(--space-4) + 12px);
  font-size: var(--font-size-xs);
}

/* Scrollbar */
.toc::-webkit-scrollbar {
  width: 4px;
}

.toc::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}
</style>
