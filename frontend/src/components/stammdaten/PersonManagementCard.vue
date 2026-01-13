<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-card>
    <v-card-title>Personen</v-card-title>
    <v-card-subtitle>Lege Personen fuer die Zuordnung an.</v-card-subtitle>
    <v-card-text>
      <v-menu>
        <template #activator="{ props }">
          <v-btn color="primary" v-bind="props">Aktionen</v-btn>
        </template>
        <v-list density="compact">
          <v-list-item @click="personDialogOpen = true">
            <v-list-item-title>Person hinzufuegen</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-card-text>
    <v-divider />
    <v-list density="compact">
      <v-list-item v-for="person in persons" :key="person.id">
        <div v-if="editingPersonId === person.id" class="w-100">
          <v-text-field
            v-model="editingPersonName"
            label="Name"
            density="compact"
            hide-details
            @keyup.enter="savePersonEdit(person.id)"
          />
          <v-text-field
            v-model="editingPersonUsername"
            label="Benutzername"
            density="compact"
            hide-details
            @keyup.enter="savePersonEdit(person.id)"
          />
          <div class="d-flex ga-2 mt-2">
            <v-btn size="small" color="primary" @click="savePersonEdit(person.id)">
              Speichern
            </v-btn>
            <v-btn size="small" variant="text" @click="cancelPersonEdit">
              Abbrechen
            </v-btn>
          </div>
        </div>
        <template v-else>
          <v-list-item-title>
            <span style="cursor: pointer;" @click="startPersonEdit(person)">
              {{ person.name }}
            </span>
          </v-list-item-title>
          <v-list-item-subtitle>@{{ person.username }}</v-list-item-subtitle>
        </template>
        <template #append>
          <div class="d-flex align-center ga-2">
            <v-menu>
              <template #activator="{ props }">
                <v-btn icon variant="text" v-bind="props">
                  <v-icon icon="mdi-dots-vertical" size="small" />
                </v-btn>
              </template>
              <v-list density="compact">
              <v-list-item @click="openPasswordDialog(person)">
                  <v-list-item-title>
                    {{ person.hasPassword ? "Passwort aendern" : "Passwort setzen" }}
                  </v-list-item-title>
              </v-list-item>
            </v-list>
            </v-menu>
            <v-chip size="small">#{{ person.id }}</v-chip>
          </div>
        </template>
      </v-list-item>
    </v-list>
    <v-dialog v-model="passwordDialogOpen" max-width="520">
      <v-card>
        <v-card-title>Passwort setzen</v-card-title>
        <v-card-text>
          <div class="d-flex flex-column ga-3">
            <div class="text-caption" v-if="passwordPersonName">
              Person: {{ passwordPersonName }}
            </div>
            <v-text-field
              v-model="passwordValue"
              label="Neues Passwort"
              type="password"
            />
            <v-text-field
              v-model="passwordConfirm"
              label="Passwort bestaetigen"
              type="password"
            />
            <div v-if="passwordError" class="text-error text-caption">
              {{ passwordError }}
            </div>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="closePasswordDialog">Abbrechen</v-btn>
          <v-btn :loading="personSaving" color="primary" @click="submitPassword">
            Speichern
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="personDialogOpen" max-width="520">
      <v-card>
        <v-card-title>Neue Person</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="submitPerson">
            <v-text-field
              v-model="newPersonName"
              label="Name"
              placeholder="Alex"
              required
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="personDialogOpen = false">Abbrechen</v-btn>
          <v-btn :loading="personSaving" color="primary" @click="submitPerson">
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
  persons: { type: Array, required: true },
  personSaving: { type: Boolean, required: true },
  createPerson: { type: Function, required: true },
  updatePerson: { type: Function, required: true },
  setPersonPassword: { type: Function, required: true }
});

const newPersonName = ref("");
const editingPersonId = ref(null);
const editingPersonName = ref("");
const editingPersonUsername = ref("");
const personDialogOpen = ref(false);
const passwordDialogOpen = ref(false);
const passwordPersonId = ref(null);
const passwordPersonName = ref("");
const passwordValue = ref("");
const passwordConfirm = ref("");
const passwordError = ref("");

const submitPerson = async () => {
  const success = await props.createPerson(newPersonName.value);
  if (success) {
    newPersonName.value = "";
    personDialogOpen.value = false;
  }
};


const startPersonEdit = (person) => {
  editingPersonId.value = person.id;
  editingPersonName.value = person.name;
  editingPersonUsername.value = person.username;
};

const cancelPersonEdit = () => {
  editingPersonId.value = null;
  editingPersonName.value = "";
  editingPersonUsername.value = "";
};

const savePersonEdit = async (id) => {
  const success = await props.updatePerson(id, editingPersonName.value, editingPersonUsername.value);
  if (success) {
    cancelPersonEdit();
  }
};

const openPasswordDialog = (person) => {
  passwordPersonId.value = person.id;
  passwordPersonName.value = person.name;
  passwordValue.value = "";
  passwordConfirm.value = "";
  passwordError.value = "";
  passwordDialogOpen.value = true;
};

const closePasswordDialog = () => {
  passwordDialogOpen.value = false;
  passwordPersonId.value = null;
  passwordPersonName.value = "";
  passwordValue.value = "";
  passwordConfirm.value = "";
  passwordError.value = "";
};

const submitPassword = async () => {
  if (!passwordPersonId.value) {
    return;
  }
  if (!passwordValue.value.trim()) {
    passwordError.value = "Bitte ein Passwort angeben.";
    return;
  }
  if (passwordValue.value !== passwordConfirm.value) {
    passwordError.value = "Passwoerter stimmen nicht ueberein.";
    return;
  }
  const success = await props.setPersonPassword(passwordPersonId.value, passwordValue.value);
  if (success) {
    closePasswordDialog();
  }
};
</script>
