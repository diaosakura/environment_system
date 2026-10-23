<script setup>
import { ref, watch, onMounted } from 'vue'
import { api } from '../api'
const props = defineProps({ modelValue: Object, optional: Boolean })
const emit = defineEmits(['update:modelValue'])
const provinces = ref([]),
  cities = ref([])
const set = (key, value) => emit('update:modelValue', { ...props.modelValue, [key]: value })
async function loadCities(provinceId) {
  cities.value = provinceId ? await api.get('/dictionaries/cities', { provinceId }) : []
}
watch(
  () => props.modelValue.provinceId,
  async (value, old) => {
    if (value !== old) {
      await loadCities(value)
      if (props.modelValue.cityId && !cities.value.some((c) => c.id === props.modelValue.cityId))
        set('cityId', null)
    }
  },
)
onMounted(async () => {
  provinces.value = await api.get('/dictionaries/provinces')
  await loadCities(props.modelValue.provinceId)
})
</script>
<template>
  <el-select
    :model-value="modelValue.provinceId"
    placeholder="选择省份"
    :clearable="optional"
    aria-label="省份"
    @update:model-value="
      (value) =>
        emit('update:modelValue', {
          ...modelValue,
          provinceId: value || null,
          cityId: null,
        })
    "
  >
    <el-option v-for="p in provinces" :key="p.id" :label="p.name" :value="p.id" />
  </el-select>
  <el-select
    :model-value="modelValue.cityId"
    placeholder="选择城市"
    :clearable="optional"
    :disabled="!modelValue.provinceId"
    aria-label="城市"
    @update:model-value="(value) => set('cityId', value || null)"
  >
    <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
  </el-select>
</template>
