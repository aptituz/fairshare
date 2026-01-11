<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-container class="py-12" fluid>
    <v-row justify="center">
      <v-col cols="12" md="5">
        <v-card>
          <v-card-title>{{ setupRequired ? "Ersteinrichtung" : "Anmeldung" }}</v-card-title>
          <v-card-subtitle>
            <span v-if="setupRequired">
              Setze das erste Passwort fuer eine bestehende Person.
            </span>
            <span v-else>Bitte melde dich an.</span>
          </v-card-subtitle>
          <v-card-text>
            <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
              {{ error }}
            </v-alert>
            <v-form @submit.prevent="submit">
              <v-text-field v-model="username" label="Benutzername" autocomplete="username" />
              <v-text-field
                v-model="password"
                label="Passwort"
                type="password"
                autocomplete="current-password"
              />
            </v-form>
          </v-card-text>
          <v-card-actions>
            <v-spacer />
            <v-btn :loading="loading" color="primary" @click="submit">
              {{ setupRequired ? "Passwort setzen" : "Anmelden" }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
  setupRequired: { type: Boolean, required: true },
  loading: { type: Boolean, required: true },
  error: { type: String, required: true },
  onSetup: { type: Function, required: true },
  onLogin: { type: Function, required: true }
});

const emit = defineEmits(["authenticated"]);

const username = ref("");
const password = ref("");

const submit = async () => {
  const trimmedUsername = username.value.trim();
  if (!trimmedUsername || !password.value) {
    return;
  }
  const success = props.setupRequired
    ? await props.onSetup(trimmedUsername, password.value)
    : await props.onLogin(trimmedUsername, password.value);
  if (success) {
    username.value = "";
    password.value = "";
    emit("authenticated");
  }
};
</script>
