<template>
  <main class="app-shell">
    <section class="app-panel">
      <p class="eyebrow">Phase 1</p>
      <h1>{{ appName }}</h1>
      <p>Project foundation is ready.</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const { appName } = storeToRefs(appStore)
</script>
