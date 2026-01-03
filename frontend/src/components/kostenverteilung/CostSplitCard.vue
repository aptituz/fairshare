<template>
  <v-card>
    <v-card-title>Gemeinsame Kosten</v-card-title>
    <v-card-subtitle>Berechnung auf Basis der persoenlichen Einnahmen.</v-card-subtitle>
    <v-divider class="my-4" />
    <v-list density="compact">
      <v-list-item>
        <v-list-item-title class="font-weight-bold">Gesamteinkommen</v-list-item-title>
        <template #append>
          <span class="font-weight-bold">{{ formatCurrency(summary?.totalIncome) }}</span>
        </template>
      </v-list-item>
      <v-list-item>
        <v-list-item-title>Gesamteinkommen ohne ungeplante einmalige Einkuenfte</v-list-item-title>
        <template #append>
          <span>{{ formatCurrency(summary?.totalIncomeRecurring ?? summary?.totalIncome) }}</span>
        </template>
      </v-list-item>
      <v-list-item>
        <v-list-item-title>Gesamtausgaben</v-list-item-title>
        <template #append>
          <span>{{ formatCurrency(summary?.totalExpenses) }}</span>
        </template>
      </v-list-item>
      <v-list-item>
        <v-list-item-title>Gesamtausgaben gemeinsam</v-list-item-title>
        <template #append>
          <span>{{ formatCurrency(summary?.sharedExpenseTotal) }}</span>
        </template>
      </v-list-item>
      <v-list-item>
        <v-list-item-title>Nettoergebnis (gemeinsam)</v-list-item-title>
        <template #append>
          <span>{{ formatCurrency(summary?.netResultShared) }}</span>
        </template>
      </v-list-item>
      <v-list-item>
        <v-list-item-title class="font-weight-bold">Budget pro Person</v-list-item-title>
        <template #append>
          <span class="font-weight-bold">{{ formatCurrency(summary?.budgetPerPerson) }}</span>
        </template>
      </v-list-item>
      <v-divider class="my-4" />
      <template v-if="summary?.costSplit?.length">
        <div v-for="entry in summary.costSplit" :key="entry.personId" class="mb-4">
          <div class="text-subtitle-2 font-weight-bold mb-2">{{ entry.name }}</div>
          <div class="d-flex flex-column ga-2">
            <div class="d-flex align-center justify-space-between">
              <span>Einnahmen</span>
              <span>{{ formatCurrency(entry.personalIncome) }}</span>
            </div>
            <div class="d-flex align-center justify-space-between">
              <span>Noetiger Selbstbehalt</span>
              <span>{{ formatCurrency(entry.personalExpenses) }}</span>
            </div>
            <div class="d-flex align-center justify-space-between">
              <span>Fuer Haushaltsgeld verwendbares Einkommen</span>
              <span>{{ formatCurrency(entry.personalUsableIncome) }}</span>
            </div>
            <div class="d-flex align-center justify-space-between">
              <span>Kostenanteil</span>
              <span>{{ formatCurrency(summary?.budgetPerPerson) }}</span>
            </div>
            <div class="d-flex align-center justify-space-between font-weight-bold">
              <span>Ueberweisung an Gemeinschaftskonto</span>
              <span>{{ formatCurrency(entry.personalContribution) }}</span>
            </div>
          </div>
        </div>
      </template>
      <v-list-item v-else>
        <v-list-item-title>Bitte Personen anlegen.</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script setup>
const props = defineProps({
  summary: { type: Object, default: null },
  formatCurrency: { type: Function, required: true }
});
</script>
