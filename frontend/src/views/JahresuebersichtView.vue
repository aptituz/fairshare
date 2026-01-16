<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-row>
    <v-col cols="12">
      <h2 class="text-h5 mb-2">Jahresuebersicht</h2>
      <p class="text-body-2">Gesamtwerte fuer alle Monate des Jahres.</p>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="12">
      <v-card>
        <v-card-text>
          <div style="height: 260px;">
            <Bar v-if="chartData" :data="chartData" :options="chartOptions" />
          </div>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="12">
      <v-expansion-panels v-model="expandedMonths" variant="accordion" multiple>
        <v-expansion-panel v-for="month in monthCards" :key="month.month" :value="month.month">
          <template #title>
            <div class="d-flex flex-column w-100">
              <div class="d-flex align-center justify-space-between">
                <span class="text-subtitle-1 font-weight-medium">{{ month.label }}</span>
                <span class="text-body-2 font-weight-medium">
                  {{ formatCurrency(month.householdBudgetBalance) }}
                </span>
              </div>
              <div class="d-flex align-center justify-space-between text-caption">
                <span>Gesamthaushaltseinkommen</span>
                <span>{{ formatCurrency(month.totalHouseholdIncome) }}</span>
              </div>
              <div class="d-flex align-center justify-space-between text-caption">
                <span>Gesamthaushaltsausgaben</span>
                <span>{{ formatCurrency(month.totalHouseholdExpenditure) }}</span>
              </div>
            </div>
          </template>
          <template #text>
            <div v-if="detailLoading[month.month]" class="text-caption">
              Lade Details...
            </div>
            <div v-else-if="detailSummaries[month.month]" class="d-flex flex-column ga-6">
              <div class="text-caption">* markiert Einmalposten in diesem Monat.</div>
              <v-sheet class="pa-4" border rounded>
                <div class="text-subtitle-1 font-weight-bold mb-3">Einnahmen</div>
                <IncomeSummaryList
                  :summary="detailSummaries[month.month]"
                  :formatCurrency="formatCurrency"
                />
              </v-sheet>
              <v-sheet class="pa-4" border rounded>
                <div class="text-subtitle-1 font-weight-bold mb-3">Ausgaben</div>
                <ExpenseSummaryList
                  :summary="detailSummaries[month.month]"
                  :categories="categories"
                  :formatCurrency="formatCurrency"
                  :categoryRank="categoryRank"
                />
              </v-sheet>
            </div>
            <div v-else class="text-caption">
              Keine Details verfuegbar.
            </div>
          </template>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-col>
  </v-row>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { Bar } from "vue-chartjs";
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  LinearScale,
  Legend,
  Tooltip
} from "chart.js";
import IncomeSummaryList from "../components/overview/IncomeSummaryList.vue";
import ExpenseSummaryList from "../components/overview/ExpenseSummaryList.vue";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const props = defineProps({
  summaryMonth: { type: String, required: true },
  fetchYearlySummary: { type: Function, required: true },
  fetchMonthlySummaryForMonth: { type: Function, required: true },
  categories: { type: Array, required: true },
  formatCurrency: { type: Function, required: true },
  categoryRank: { type: Function, required: true }
});

const selectedYear = computed(() =>
  Number(String(props.summaryMonth || "").slice(0, 4)) || new Date().getFullYear()
);
const yearlySummary = ref(null);
const expandedMonths = ref([]);
const detailSummaries = ref({});
const detailLoading = ref({});

const monthNames = [
  "Januar",
  "Februar",
  "Maerz",
  "April",
  "Mai",
  "Juni",
  "Juli",
  "August",
  "September",
  "Oktober",
  "November",
  "Dezember"
];

const loadSummary = async () => {
  yearlySummary.value = await props.fetchYearlySummary(selectedYear.value);
};

watch(selectedYear, () => {
  expandedMonths.value = [];
  detailSummaries.value = {};
  detailLoading.value = {};
  loadSummary();
}, { immediate: true });

const loadDetailSummary = async (month) => {
  if (detailLoading.value[month] || detailSummaries.value[month]) {
    return;
  }
  detailLoading.value = { ...detailLoading.value, [month]: true };
  try {
    const summary = await props.fetchMonthlySummaryForMonth(month);
    if (summary) {
      detailSummaries.value = { ...detailSummaries.value, [month]: summary };
    }
  } finally {
    detailLoading.value = { ...detailLoading.value, [month]: false };
  }
};

watch(expandedMonths, (months) => {
  (months || []).forEach((month) => {
    loadDetailSummary(month);
  });
});

const monthCards = computed(() => {
  if (!yearlySummary.value?.months) {
    return [];
  }
  return yearlySummary.value.months.map((entry) => {
    const monthIndex = Number(String(entry.month).slice(5, 7)) - 1;
    const label = `${monthNames[monthIndex]} ${selectedYear.value}`;
    return {
      ...entry,
      label
    };
  });
});

const chartData = computed(() => {
  if (!yearlySummary.value?.months) {
    return null;
  }
  const labels = yearlySummary.value.months.map((entry) => {
    const monthIndex = Number(String(entry.month).slice(5, 7)) - 1;
    return monthNames[monthIndex] || entry.month;
  });
  return {
    labels,
    datasets: [
      {
        label: "Einkommen",
        data: yearlySummary.value.months.map((entry) => Number(entry.totalHouseholdIncome || 0)),
        backgroundColor: "rgba(46, 125, 50, 0.6)"
      },
      {
        label: "Ausgaben",
        data: yearlySummary.value.months.map((entry) => Number(entry.totalHouseholdExpenditure || 0)),
        backgroundColor: "rgba(198, 40, 40, 0.6)"
      }
    ]
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: "top" },
    tooltip: {
      callbacks: {
        label: (context) =>
          `${context.dataset.label}: ${props.formatCurrency(context.parsed.y)}`
      }
    }
  },
  scales: {
    y: {
      ticks: {
        callback: (value) => props.formatCurrency(value)
      }
    },
    x: {
      ticks: {
        maxRotation: 0
      }
    }
  }
}));
</script>
