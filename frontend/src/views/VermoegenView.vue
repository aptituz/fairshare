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
      <v-select
        v-model="selectedYear"
        label="Jahr"
        :items="yearOptions"
        density="compact"
      />
    </v-col>
    <v-col cols="12" md="8" class="d-flex flex-column align-end ga-3">
      <div class="text-right">
        <div class="text-caption">Aktueller Gesamtstand</div>
        <div class="text-h6">{{ formatCurrency(currentTotalBalance) }}</div>
      </div>
      <v-menu>
        <template #activator="{ props }">
          <v-btn color="primary" v-bind="props">Aktionen</v-btn>
        </template>
        <v-list density="compact">
          <v-list-item @click="openCreateBalanceDialog">
            <v-list-item-title>Kontostand erfassen</v-list-item-title>
          </v-list-item>
          <v-list-item @click="openBulkDialog">
            <v-list-item-title>Alle Kontostaende erfassen</v-list-item-title>
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
          <div
            v-for="group in sortedMonthlyBalances"
            :key="group.month"
            class="mb-6"
            :class="{ 'text-medium-emphasis': !group.balances.length }"
          >
            <div class="text-subtitle-1 font-weight-medium mb-2">{{ group.month }}</div>
            <v-table density="compact" class="table-scroll">
              <thead>
                <tr>
                  <th class="text-left">Datum</th>
                  <th class="text-left">Sparkonto</th>
                  <th class="text-right">Betrag</th>
                  <th class="text-right">Aktion</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="balance in group.balances" :key="balance.id || balance.balanceDate">
                  <td>{{ balance.balanceDate }}</td>
                  <td>{{ accountName(balance.savingsAccountId) }}</td>
                  <td class="text-right">{{ formatCurrency(balance.balanceAmount) }}</td>
                  <td class="text-right">
                    <v-btn
                      v-if="balance.id"
                      size="small"
                      variant="text"
                      icon
                      @click="openEditBalance(balance)"
                    >
                      <v-icon icon="mdi-pencil" size="small" />
                    </v-btn>
                    <v-btn
                      v-if="balance.id"
                      size="small"
                      variant="text"
                      icon
                      @click="deleteBalance(balance)"
                    >
                      <v-icon icon="mdi-delete" size="small" />
                    </v-btn>
                  </td>
                </tr>
                <tr
                  class="font-weight-bold"
                  :class="group.balances.length ? 'text-black' : 'text-medium-emphasis'"
                >
                  <td>Monatsende gesamt</td>
                  <td />
                  <td class="text-right">{{ formatCurrency(group.totalBalance) }}</td>
                  <td />
                </tr>
              </tbody>
            </v-table>
          </div>
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
            :items="availableAccounts"
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

  <v-dialog v-model="bulkDialogOpen" max-width="680">
    <v-card>
      <v-card-title>Kontostaende erfassen</v-card-title>
      <v-card-text>
        <div class="d-flex flex-column ga-4">
          <v-text-field v-model="bulkDate" type="date" label="Datum" />
          <v-table density="compact" class="table-scroll">
            <thead>
              <tr>
                <th class="text-left">Sparkonto</th>
                <th class="text-right">Betrag</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in bulkBalances" :key="entry.savingsAccountId">
                <td>{{ accountName(entry.savingsAccountId) }}</td>
                <td class="text-right">
                  <v-text-field
                    v-model="entry.balanceAmount"
                    type="number"
                    step="0.01"
                    min="0"
                    density="compact"
                    hide-details
                  />
                </td>
              </tr>
            </tbody>
          </v-table>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="bulkDialogOpen = false">Abbrechen</v-btn>
        <v-btn color="primary" @click="submitBulkBalances">Speichern</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { computed, ref, watch } from "vue";
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

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Legend);

const props = defineProps({
  savingsAccounts: { type: Array, required: true },
  summaryMonth: { type: String, required: true },
  fetchWealthSummary: { type: Function, required: true },
  fetchWealthBalances: { type: Function, required: true },
  createSavingsAccountBalance: { type: Function, required: true },
  deleteSavingsAccountBalance: { type: Function, required: true },
  createSavingsAccountBalancesBulk: { type: Function, required: true },
  formatCurrency: { type: Function, required: true }
});

const formatMonth = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
const now = new Date();
const defaultYear = Number(String(props.summaryMonth || "").slice(0, 4)) || now.getFullYear();
const selectedYear = ref(defaultYear);
const summaryData = ref([]);
const monthlyBalances = ref([]);
const balanceDialogOpen = ref(false);
const selectedAccountId = ref(null);
const balanceDate = ref("");
const balanceAmount = ref("");
const editingBalanceId = ref(null);
const bulkDialogOpen = ref(false);
const bulkDate = ref("");
const bulkBalances = ref([]);
const defaultBalanceDate = ref(`${props.summaryMonth}-01`);

const loadSummary = async () => {
  summaryData.value = await props.fetchWealthSummary(selectedYear.value);
};

const loadBalances = async () => {
  monthlyBalances.value = await props.fetchWealthBalances(selectedYear.value);
};

const yearRange = 20;
const yearOptions = computed(() => {
  const current = now.getFullYear();
  const years = [];
  for (let offset = -yearRange; offset <= yearRange; offset += 1) {
    years.push(current + offset);
  }
  return years;
});

watch(selectedYear, () => {
  loadSummary();
  loadBalances();
}, { immediate: true });

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
      },
      {
        label: "Erwarteter Verlauf",
        data: summaryData.value.map((entry) => Number(entry.expectedBalance || 0)),
        borderColor: "#43a047",
        backgroundColor: "rgba(67,160,71,0.15)",
        pointRadius: 4,
        borderDash: [6, 6],
        tension: 0
      }
    ]
  };
});

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true },
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

const toDateValue = (value) => {
  if (!value) {
    return null;
  }
  return new Date(`${value}T00:00:00`);
};

const isAccountActiveForDate = (account, dateValue) => {
  if (!account) {
    return false;
  }
  const start = account.startDate ? toDateValue(account.startDate) : null;
  const end = account.endDate ? toDateValue(account.endDate) : null;
  const date = toDateValue(dateValue);
  if (!date) {
    return true;
  }
  if (start && date < start) {
    return false;
  }
  if (end && date > end) {
    return false;
  }
  return true;
};

const availableAccounts = computed(() => {
  const dateValue = balanceDate.value || defaultBalanceDate.value;
  return props.savingsAccounts.filter((account) => isAccountActiveForDate(account, dateValue));
});

const availableBulkAccounts = computed(() => {
  const dateValue = bulkDate.value || defaultBalanceDate.value;
  return props.savingsAccounts.filter((account) => isAccountActiveForDate(account, dateValue));
});

const sortedMonthlyBalances = computed(() =>
  monthlyBalances.value.slice().sort((a, b) => String(b.month).localeCompare(String(a.month)))
);

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

const deleteBalance = async (balance) => {
  if (!balance?.id) {
    return;
  }
  if (!window.confirm("Kontostand wirklich loeschen?")) {
    return;
  }
  const success = await props.deleteSavingsAccountBalance(balance.id);
  if (success) {
    await loadSummary();
    await loadBalances();
  }
};

const openBulkDialog = () => {
  bulkBalances.value = availableBulkAccounts.value.map((account) => ({
    savingsAccountId: account.id,
    balanceAmount: ""
  }));
  bulkDate.value = defaultBalanceDate.value;
  bulkDialogOpen.value = true;
};

const openCreateBalanceDialog = () => {
  balanceDate.value = defaultBalanceDate.value;
  balanceDialogOpen.value = true;
};

const syncBulkBalances = () => {
  const accounts = availableBulkAccounts.value;
  const existing = new Map(bulkBalances.value.map((entry) => [entry.savingsAccountId, entry]));
  bulkBalances.value = accounts.map((account) => {
    const prior = existing.get(account.id);
    return {
      savingsAccountId: account.id,
      balanceAmount: prior?.balanceAmount ?? ""
    };
  });
};

watch([bulkDate, bulkDialogOpen], ([, open]) => {
  if (open) {
    syncBulkBalances();
  }
});

watch(balanceDate, () => {
  if (!selectedAccountId.value) {
    return;
  }
  if (!availableAccounts.value.find((account) => account.id === selectedAccountId.value)) {
    selectedAccountId.value = null;
  }
});

watch(
  () => props.summaryMonth,
  (value, previous) => {
    const nextDefault = value ? `${value}-01` : formatMonth(now) + "-01";
    const previousDefault = previous ? `${previous}-01` : defaultBalanceDate.value;
    defaultBalanceDate.value = nextDefault;
    if (balanceDialogOpen.value && balanceDate.value === previousDefault) {
      balanceDate.value = nextDefault;
    }
    if (bulkDialogOpen.value && bulkDate.value === previousDefault) {
      bulkDate.value = nextDefault;
    }
  },
  { immediate: true }
);

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

const submitBulkBalances = async () => {
  const success = await props.createSavingsAccountBalancesBulk(bulkDate.value, bulkBalances.value);
  if (success) {
    bulkDialogOpen.value = false;
    bulkDate.value = "";
    bulkBalances.value = [];
    await loadSummary();
    await loadBalances();
  }
};
</script>
