<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-card>
    <v-card-title>Sparkonten</v-card-title>
    <v-card-subtitle>Verwalte Konten und ordne sie Personen zu.</v-card-subtitle>
    <v-card-text>
      <v-menu>
        <template #activator="{ props }">
          <v-btn color="primary" v-bind="props">Aktionen</v-btn>
        </template>
        <v-list density="compact">
          <v-list-item @click="openAccountDialog">
            <v-list-item-title>Konto hinzufuegen</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-card-text>
    <v-divider />
    <v-list density="compact">
      <v-list-item v-for="account in savingsAccounts" :key="account.id">
        <div v-if="editingAccountId === account.id" class="w-100">
          <v-text-field
            v-model="editingAccountName"
            label="Name"
            density="compact"
            hide-details
          />
            <v-select
              v-model="editingOwnerId"
              label="Inhaber"
              :items="ownerOptions"
              item-title="name"
              item-value="id"
              density="compact"
              hide-details
            />
          <v-text-field
            v-model="editingStartDate"
            label="Startdatum"
            type="date"
            density="compact"
            hide-details
            required
          />
          <v-text-field
            v-model="editingEndDate"
            label="Enddatum"
            type="date"
            density="compact"
            hide-details
          />
          <div class="d-flex ga-2 mt-2">
            <v-btn size="small" color="primary" @click="saveAccountEdit(account.id)">
              Speichern
            </v-btn>
            <v-btn size="small" variant="text" @click="cancelAccountEdit">
              Abbrechen
            </v-btn>
          </div>
        </div>
        <template v-else>
          <v-list-item-title>
            <span style="cursor: pointer;" @click="startAccountEdit(account)">
              {{ account.name }}
            </span>
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ account.owner?.name || "Gemeinsam" }}
          </v-list-item-subtitle>
          <v-list-item-subtitle v-if="account.startDate || account.endDate">
            {{ accountDateLabel(account) }}
          </v-list-item-subtitle>
        </template>
        <template #append>
          <div class="d-flex align-center ga-2">
            <v-chip size="small">#{{ account.id }}</v-chip>
            <v-btn
              size="small"
              variant="text"
              icon
              @click.stop="deleteAccount(account.id)"
            >
              <v-icon icon="mdi-delete" size="small" />
            </v-btn>
          </div>
        </template>
      </v-list-item>
    </v-list>
    <v-dialog v-model="accountDialogOpen" max-width="520">
      <v-card>
        <v-card-title>Neues Sparkonto</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="submitAccount">
            <v-text-field
              v-model="newAccountName"
              label="Name"
              placeholder="Ruecklage"
              required
            />
            <v-select
              v-model="newOwnerId"
              label="Inhaber"
              :items="ownerOptions"
              item-title="name"
              item-value="id"
            />
            <v-text-field
              v-model="newStartDate"
              label="Startdatum"
              type="date"
              required
            />
            <v-text-field
              v-model="newEndDate"
              label="Enddatum"
              type="date"
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="accountDialogOpen = false">Abbrechen</v-btn>
          <v-btn color="primary" @click="submitAccount">Hinzufuegen</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  savingsAccounts: { type: Array, required: true },
  persons: { type: Array, required: true },
  createSavingsAccount: { type: Function, required: true },
  updateSavingsAccount: { type: Function, required: true },
  summaryMonth: { type: String, required: true },
  deleteSavingsAccount: { type: Function, required: true }
});

const ownerOptions = computed(() => [
  { id: null, name: "Gemeinsam" },
  ...props.persons
]);

const newAccountName = ref("");
const newOwnerId = ref(null);
const newStartDate = ref("");
const newEndDate = ref("");
const accountDialogOpen = ref(false);
const editingAccountId = ref(null);
const editingAccountName = ref("");
const editingOwnerId = ref(null);
const editingStartDate = ref("");
const editingEndDate = ref("");

const submitAccount = async () => {
  const success = await props.createSavingsAccount(
    newAccountName.value,
    newOwnerId.value,
    newStartDate.value,
    newEndDate.value
  );
  if (success) {
    newAccountName.value = "";
    newOwnerId.value = null;
    newStartDate.value = "";
    newEndDate.value = "";
    accountDialogOpen.value = false;
  }
};

const startAccountEdit = (account) => {
  editingAccountId.value = account.id;
  editingAccountName.value = account.name;
  editingOwnerId.value = account.owner?.id ?? null;
  editingStartDate.value = formatDate(account.startDate);
  editingEndDate.value = formatDate(account.endDate);
};

const cancelAccountEdit = () => {
  editingAccountId.value = null;
  editingAccountName.value = "";
  editingOwnerId.value = null;
  editingStartDate.value = "";
  editingEndDate.value = "";
};

const saveAccountEdit = async (id) => {
  const success = await props.updateSavingsAccount(
    id,
    editingAccountName.value,
    editingOwnerId.value,
    editingStartDate.value,
    editingEndDate.value
  );
  if (success) {
    cancelAccountEdit();
  }
};

const deleteAccount = async (id) => {
  if (!window.confirm("Sparkonto wirklich loeschen?")) {
    return;
  }
  await props.deleteSavingsAccount(id);
};

const openAccountDialog = () => {
  newStartDate.value = props.summaryMonth ? `${props.summaryMonth}-01` : "";
  accountDialogOpen.value = true;
};

const formatDate = (value) => {
  if (!value) {
    return "";
  }
  return String(value).slice(0, 10);
};

const accountDateLabel = (account) => {
  const start = formatDate(account.startDate) || "offen";
  const end = formatDate(account.endDate) || "offen";
  return `Gueltig: ${start} – ${end}`;
};
</script>
