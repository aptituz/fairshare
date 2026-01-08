<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-card>
    <v-card-title>Kategorien</v-card-title>
    <v-card-subtitle>
      Erstelle Töpfe für wiederkehrende Ausgaben und Einnahmen.
    </v-card-subtitle>
    <v-divider />
    <v-list density="compact">
      <v-list-subheader class="font-weight-bold">Einnahmen</v-list-subheader>
      <v-list-item v-for="category in incomeCategoryOptions" :key="category.id">
        <div v-if="editingCategoryId === category.id" class="w-100">
          <v-text-field
            v-model="editingCategoryName"
            label="Name"
            density="compact"
            hide-details
            @keyup.enter="saveCategoryEdit(category.id)"
          />
          <div class="d-flex ga-2 mt-2">
            <v-btn size="small" color="primary" @click="saveCategoryEdit(category.id)">
              Speichern
            </v-btn>
            <v-btn size="small" variant="text" @click="cancelCategoryEdit">
              Abbrechen
            </v-btn>
          </div>
        </div>
        <template v-else>
          <v-list-item-title>
            <span style="cursor: pointer;" @click="startCategoryEdit(category)">
              {{ category.label }}
            </span>
          </v-list-item-title>
        </template>
        <template #append>
          <div class="d-flex align-center ga-2">
            <v-chip size="small">#{{ category.id }}</v-chip>
            <v-btn
              size="small"
              variant="text"
              icon
              color="error"
              @click="confirmDeleteCategory(category.id)"
            >
              <v-icon icon="mdi-delete" />
            </v-btn>
          </div>
        </template>
      </v-list-item>
      <v-list-item>
        <v-menu>
          <template #activator="{ props }">
            <v-btn variant="outlined" size="small" v-bind="props">Aktionen</v-btn>
          </template>
          <v-list density="compact">
            <v-list-item @click="incomeDialogOpen = true">
              <v-list-item-title>Einnahmen-Kategorie hinzufuegen</v-list-item-title>
            </v-list-item>
            <v-list-item @click="expenseDialogOpen = true">
              <v-list-item-title>Ausgaben-Kategorie hinzufuegen</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-list-item>
      <v-divider class="my-4" />
      <v-list-subheader class="font-weight-bold">Ausgaben</v-list-subheader>
      <v-list-item v-for="category in expenseCategoryOptions" :key="category.id">
        <div v-if="editingCategoryId === category.id" class="w-100">
          <v-text-field
            v-model="editingCategoryName"
            label="Name"
            density="compact"
            hide-details
            @keyup.enter="saveCategoryEdit(category.id)"
          />
          <div class="d-flex ga-2 mt-2">
            <v-btn size="small" color="primary" @click="saveCategoryEdit(category.id)">
              Speichern
            </v-btn>
            <v-btn size="small" variant="text" @click="cancelCategoryEdit">
              Abbrechen
            </v-btn>
          </div>
        </div>
        <template v-else>
          <v-list-item-title>
            <span style="cursor: pointer;" @click="startCategoryEdit(category)">
              {{ category.label }}
            </span>
          </v-list-item-title>
        </template>
        <template #append>
          <div class="d-flex align-center ga-2">
            <v-chip size="small">#{{ category.id }}</v-chip>
            <v-btn
              size="small"
              variant="text"
              icon
              color="error"
              @click="confirmDeleteCategory(category.id)"
            >
              <v-icon icon="mdi-delete" />
            </v-btn>
          </div>
        </template>
      </v-list-item>
    </v-list>
    <v-dialog v-model="incomeDialogOpen" max-width="520">
      <v-card>
        <v-card-title>Neue Einnahmen-Kategorie</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="submitCategory('INCOME', newIncomeCategoryName)">
            <v-text-field
              v-model="newIncomeCategoryName"
              label="Name"
              placeholder="Gehalt"
              required
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="incomeDialogOpen = false">Abbrechen</v-btn>
          <v-btn :loading="categorySaving" color="primary" @click="submitCategory('INCOME', newIncomeCategoryName)">
            Hinzufügen
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="expenseDialogOpen" max-width="520">
      <v-card>
        <v-card-title>Neue Ausgaben-Kategorie</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="submitCategory('EXPENSE', newExpenseCategoryName)">
            <v-text-field
              v-model="newExpenseCategoryName"
              label="Name"
              placeholder="Miete"
              required
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="expenseDialogOpen = false">Abbrechen</v-btn>
          <v-btn :loading="categorySaving" color="primary" @click="submitCategory('EXPENSE', newExpenseCategoryName)">
            Hinzufügen
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
  incomeCategoryOptions: { type: Array, required: true },
  expenseCategoryOptions: { type: Array, required: true },
  categorySaving: { type: Boolean, required: true },
  createCategory: { type: Function, required: true },
  updateCategory: { type: Function, required: true },
  deleteCategory: { type: Function, required: true }
});

const newIncomeCategoryName = ref("");
const newExpenseCategoryName = ref("");
const editingCategoryId = ref(null);
const editingCategoryName = ref("");
const incomeDialogOpen = ref(false);
const expenseDialogOpen = ref(false);

const submitCategory = async (type, name) => {
  const created = await props.createCategory(type, name);
  if (created) {
    if (type === "INCOME") {
      newIncomeCategoryName.value = "";
      incomeDialogOpen.value = false;
    } else {
      newExpenseCategoryName.value = "";
      expenseDialogOpen.value = false;
    }
  }
};

const startCategoryEdit = (category) => {
  editingCategoryId.value = category.id;
  editingCategoryName.value = category.name;
};

const cancelCategoryEdit = () => {
  editingCategoryId.value = null;
  editingCategoryName.value = "";
};

const saveCategoryEdit = async (id) => {
  const success = await props.updateCategory(id, editingCategoryName.value);
  if (success) {
    cancelCategoryEdit();
  }
};

const confirmDeleteCategory = async (id) => {
  if (!window.confirm("Kategorie wirklich loeschen?")) {
    return;
  }
  await props.deleteCategory(id);
};
</script>
