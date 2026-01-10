<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

<template>
  <v-app>
    <v-navigation-drawer permanent width="280">
      <div class="px-4 py-4 d-flex align-center">
        <v-img :src="logo" alt="Fairshare" max-height="80" max-width="248" contain class="w-100" />
      </div>
      <v-divider />
      <v-list nav v-model:opened="openGroups">
        <v-list-item
          title="Overview"
          :active="currentView === 'overview'"
          @click="navigate('overview')"
        />
        <v-list-item
          title="Vermoegen"
          :active="currentView === 'vermoegen'"
          @click="navigate('vermoegen')"
        />
        <v-list-item
          title="Kostenverteilung"
          :active="currentView === 'kostenverteilung'"
          @click="navigate('kostenverteilung')"
        />
        <v-list-group value="datenerfassung">
          <template #activator="{ props }">
            <v-list-item
              v-bind="props"
              title="Datenerfassung"
              :active="currentView === 'datenerfassung'"
              @click="navigate('datenerfassung', currentSubView)"
            />
          </template>
          <v-list-item
            v-for="item in datenerfassungNavItems"
            :key="item.key"
            :title="item.title"
            :active="currentView === 'datenerfassung' && currentSubView === item.key"
            @click="navigate('datenerfassung', item.key)"
          />
        </v-list-group>
        <v-list-group value="stammdaten">
          <template #activator="{ props }">
            <v-list-item
              v-bind="props"
              title="Stammdaten"
              :active="currentView === 'stammdaten'"
              @click="navigate('stammdaten', 'categories')"
            />
          </template>
          <v-list-item
            title="Kategorien"
            :active="currentView === 'stammdaten' && currentSubView === 'categories'"
            @click="navigate('stammdaten', 'categories')"
          />
          <v-list-item
            title="Personen"
            :active="currentView === 'stammdaten' && currentSubView === 'persons'"
            @click="navigate('stammdaten', 'persons')"
          />
          <v-list-item
            title="Sparkonten"
            :active="currentView === 'stammdaten' && currentSubView === 'savings-accounts'"
            @click="navigate('stammdaten', 'savings-accounts')"
          />
        </v-list-group>
      </v-list>
      <v-divider />
      <v-list density="compact">
        <v-list-item title="Letzte Aktualisierung" :subtitle="summary ? 'Aktuell' : 'Laedt...'" />
      </v-list>
      <v-divider class="my-2" />
      <div class="px-4 pb-4">
        <MonthYearPicker :model-value="summaryMonth" @update:modelValue="updateMonth" />
      </div>
    </v-navigation-drawer>

    <v-main>
      <v-app-bar color="surface" flat>
        <v-spacer />
        <v-menu v-if="currentUsername">
          <template #activator="{ props }">
            <v-btn icon v-bind="props">
              <v-icon icon="mdi-account-circle-outline" />
            </v-btn>
          </template>
          <v-list density="compact" style="min-width: 240px;">
            <v-list-item
              :title="currentName || currentUsername"
              :subtitle="currentName ? currentUsername : ''"
            />
            <v-divider />
            <v-list-item @click="passwordDialogOpen = true">
              <v-list-item-title>Passwort aendern</v-list-item-title>
            </v-list-item>
            <v-list-item @click="logout">
              <v-list-item-title>Abmelden</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-app-bar>
      <v-container fluid class="pa-6">
        <slot />
      </v-container>
    </v-main>

    <v-dialog v-model="passwordDialogOpen" max-width="420">
      <v-card>
        <v-card-title>Passwort aendern</v-card-title>
        <v-card-text>
          <div class="d-flex flex-column ga-2">
            <v-text-field
              v-model="currentPassword"
              label="Aktuelles Passwort"
              type="password"
              autocomplete="current-password"
            />
            <v-text-field
              v-model="newPassword"
              label="Neues Passwort"
              type="password"
              autocomplete="new-password"
            />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="passwordDialogOpen = false">Abbrechen</v-btn>
          <v-btn color="primary" @click="submitPasswordChange">Speichern</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup>
import { ref } from "vue";
import MonthYearPicker from "./MonthYearPicker.vue";
import logo from "../assets/logo.png";

const props = defineProps({
  currentView: { type: String, required: true },
  currentSubView: { type: String, required: true },
  datenerfassungNavItems: { type: Array, required: true },
  summary: { type: Object, default: null },
  summaryMonth: { type: String, required: true },
  currentUsername: { type: String, default: "" },
  currentName: { type: String, default: "" },
  onChangePassword: { type: Function, required: true },
  onLogout: { type: Function, required: true }
});

const emit = defineEmits(["navigate", "month-change"]);

const openGroups = ref(["datenerfassung", "stammdaten"]);
const passwordDialogOpen = ref(false);
const currentPassword = ref("");
const newPassword = ref("");

const navigate = (view, subView) => {
  emit("navigate", { view, subView });
};

const updateMonth = (value) => {
  emit("month-change", value);
};

const logout = () => {
  props.onLogout();
};

const submitPasswordChange = async () => {
  if (!currentPassword.value || !newPassword.value) {
    return;
  }
  const success = await props.onChangePassword(currentPassword.value, newPassword.value);
  if (success) {
    passwordDialogOpen.value = false;
    currentPassword.value = "";
    newPassword.value = "";
  }
};
</script>
