<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-row>
    <v-col cols="12">
      <h2 class="text-h5 mb-2">Vermoegen</h2>
      <p class="text-body-2">Behalte den Gesamtstand deiner Sparkonten im Blick.</p>
    </v-col>
  </v-row>
  <v-row class="align-center">
    <v-col cols="12" md="4">
      <div class="text-caption">Zeitraum von</div>
      <MonthYearPicker
        :model-value="fromMonth"
        :year-range="20"
        @update:modelValue="(value) => (fromMonth = value)"
      />
    </v-col>
    <v-col cols="12" md="4">
      <div class="text-caption">Zeitraum bis</div>
      <MonthYearPicker
        :model-value="toMonth"
        :year-range="20"
        @update:modelValue="(value) => (toMonth = value)"
      />
    </v-col>
    <v-col cols="12" md="4" class="d-flex flex-column align-end ga-3">
      <div class="text-right">
        <div class="text-caption">Aktueller Gesamtstand</div>
        <div class="text-h6">{{ formatCurrency(currentTotalBalance) }}</div>
      </div>
      <v-menu>
        <template #activator="{ props }">
          <v-btn color="primary" v-bind="props">Aktionen</v-btn>
        </template>
        <v-list density="compact">
          <v-list-item @click="balanceDialogOpen = true">
            <v-list-item-title>Kontostand erfassen</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-col>
  </v-row>
  <v-row>
    <v-col cols="12">
      <v-card>
        <v-card-text>
          <div style="height: 260px;">
            <Line v-if="chartData" :data="chartData" :options="chartOptions" />
          </div>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <v-row>
    <v-col cols="12">
      <v-card>
        <v-card-title>Kontostaende</v-card-title>
        <v-card-text>
          <v-table density="compact">
            <thead>
              <tr>
                <th class="text-left">Datum</th>
                <th class="text-left">Sparkonto</th>
                <th class="text-right">Betrag</th>
                <th class="text-right">Aktion</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="balance in balances" :key="balance.id">
                <td>{{ balance.balanceDate }}</td>
                <td>{{ accountName(balance.savingsAccountId) }}</td>
                <td class="text-right">{{ formatCurrency(balance.balanceAmount) }}</td>
                <td class="text-right">
                  <v-btn size="small" variant="text" icon @click="openEditBalance(balance)">
                    <v-icon icon="mdi-pencil" size="small" />
                  </v-btn>
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <v-dialog v-model="balanceDialogOpen" max-width="520">
    <v-card>
      <v-card-title>{{ balanceDialogTitle }}</v-card-title>
      <v-card-text>
        <div class="d-flex flex-column ga-2">
          <v-select
            v-model="selectedAccountId"
            label="Sparkonto"
            :items="savingsAccounts"
            item-title="name"
            item-value="id"
          />
          <v-text-field v-model="balanceDate" type="date" label="Datum" />
          <v-text-field v-model="balanceAmount" type="number" step="0.01" min="0" label="Kontostand" />
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="balanceDialogOpen = false">Abbrechen</v-btn>
        <v-btn color="primary" @click="submitBalance">Speichern</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { Line } from "vue-chartjs";
import {
  Chart as ChartJS,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Legend
} from "chart.js";
import MonthYearPicker from "../components/MonthYearPicker.vue";

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Legend);

const props = defineProps({
  savingsAccounts: { type: Array, required: true },
  fetchWealthSummary: { type: Function, required: true },
  fetchWealthBalances: { type: Function, required: true },
  createSavingsAccountBalance: { type: Function, required: true },
  formatCurrency: { type: Function, required: true }
});

const formatMonth = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
const now = new Date();
const currentMonth = formatMonth(now);
const defaultStart = new Date(now.getFullYear(), now.getMonth() - 11, 1);
const fromMonth = ref(formatMonth(defaultStart));
const toMonth = ref(currentMonth);
const summaryData = ref([]);
const balances = ref([]);
const balanceDialogOpen = ref(false);
const selectedAccountId = ref(null);
const balanceDate = ref("");
const balanceAmount = ref("");
const editingBalanceId = ref(null);

const loadSummary = async () => {
  summaryData.value = await props.fetchWealthSummary(fromMonth.value, toMonth.value);
};

const loadBalances = async () => {
  balances.value = await props.fetchWealthBalances();
};

watch([fromMonth, toMonth], () => {
  loadSummary();
}, { immediate: true });

onMounted(() => {
  loadBalances();
});

const chartData = computed(() => {
  if (!summaryData.value.length) {
    return null;
  }
  return {
    labels: summaryData.value.map((entry) => entry.month),
    datasets: [
      {
        label: "Gesamtstand",
        data: summaryData.value.map((entry) => Number(entry.totalBalance || 0)),
        borderColor: "#1e88e5",
        backgroundColor: "rgba(30,136,229,0.2)",
        pointRadius: 4,
        tension: 0
      }
    ]
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (context) => props.formatCurrency(context.parsed.y)
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
        autoSkip: true,
        maxRotation: 0
      }
    }
  }
}));

const currentTotalBalance = computed(() => {
  if (!summaryData.value.length) {
    return 0;
  }
  const last = summaryData.value[summaryData.value.length - 1];
  return Number(last.totalBalance || 0);
});

const accountName = (accountId) => {
  const match = props.savingsAccounts.find((account) => account.id === accountId);
  return match?.name || "Unbekannt";
};

const balanceDialogTitle = computed(() =>
  editingBalanceId.value ? "Kontostand bearbeiten" : "Kontostand erfassen"
);

const openEditBalance = (balance) => {
  editingBalanceId.value = balance.id;
  selectedAccountId.value = balance.savingsAccountId;
  balanceDate.value = balance.balanceDate;
  balanceAmount.value = balance.balanceAmount;
  balanceDialogOpen.value = true;
};

const submitBalance = async () => {
  const success = await props.createSavingsAccountBalance(
    selectedAccountId.value,
    balanceDate.value,
    balanceAmount.value
  );
  if (success) {
    balanceDialogOpen.value = false;
    selectedAccountId.value = null;
    balanceDate.value = "";
    balanceAmount.value = "";
    editingBalanceId.value = null;
    await loadSummary();
    await loadBalances();
  }
};
</script>
