<script setup>
import { gradeColor, gradeName } from '../api'
defineProps({ value: [Number, String] })
</script>
<template>
  <span class="grade-badge" :style="{ '--grade': gradeColor(Number(value)) }">
    <i></i>
    {{ gradeName(Number(value)) }}
  </span>
</template>
