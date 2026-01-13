<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-card>
    <v-card-title>Import / Export</v-card-title>
    <v-card-subtitle>Sichere oder stelle den kompletten Datenbestand wieder her.</v-card-subtitle>
    <v-card-text>
      <div class="d-flex flex-wrap ga-3">
        <v-btn color="primary" @click="downloadExport">Daten exportieren</v-btn>
        <v-btn color="error" variant="outlined" @click="openImportDialog">
          Daten importieren
        </v-btn>
      </div>
      <div v-if="exportError" class="text-error text-caption mt-3">{{ exportError }}</div>
    </v-card-text>
  </v-card>

  <v-dialog v-model="importDialogOpen" max-width="640">
    <v-card>
      <v-card-title>Daten importieren</v-card-title>
      <v-card-text>
        <div class="d-flex flex-column ga-3">
          <div class="text-body-2">
            Achtung: Der Import ueberschreibt den aktuellen Datenbestand vollstaendig.
          </div>
          <v-file-input
            v-model="importFile"
            label="Exportdatei (JSON)"
            accept="application/json"
            density="compact"
            show-size
          />
          <v-text-field
            v-model="confirmText"
            label='Zur Bestaetigung "IMPORTIEREN" eingeben'
            density="compact"
          />
          <div v-if="importError" class="text-error text-caption">{{ importError }}</div>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="closeImportDialog">Abbrechen</v-btn>
        <v-btn
          color="error"
          :disabled="!canImport"
          :loading="importing"
          @click="submitImport"
        >
          Import starten
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  exportData: { type: Function, required: true },
  importData: { type: Function, required: true }
});

const exportError = ref("");
const importDialogOpen = ref(false);
const importFile = ref(null);
const confirmText = ref("");
const importError = ref("");
const importing = ref(false);

const canImport = computed(() => importFile.value && confirmText.value === "IMPORTIEREN");

const downloadExport = async () => {
  exportError.value = "";
  try {
    const payload = await props.exportData();
    if (!payload) {
      exportError.value = "Export fehlgeschlagen.";
      return;
    }
    const dateTag = new Date().toISOString().slice(0, 10);
    const fileName = `fairshare-export-${dateTag}.json`;
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    exportError.value = err?.message || "Export fehlgeschlagen.";
  }
};

const openImportDialog = () => {
  importDialogOpen.value = true;
  importFile.value = null;
  confirmText.value = "";
  importError.value = "";
};

const closeImportDialog = () => {
  importDialogOpen.value = false;
  importFile.value = null;
  confirmText.value = "";
  importError.value = "";
};

const submitImport = async () => {
  if (!importFile.value) {
    importError.value = "Bitte eine Exportdatei auswaehlen.";
    return;
  }
  importError.value = "";
  importing.value = true;
  try {
    const text = await importFile.value.text();
    const payload = JSON.parse(text);
    const success = await props.importData(payload);
    if (!success) {
      importError.value = "Import fehlgeschlagen.";
      return;
    }
    closeImportDialog();
  } catch (err) {
    importError.value = err?.message || "Import fehlgeschlagen.";
  } finally {
    importing.value = false;
  }
};
</script>
