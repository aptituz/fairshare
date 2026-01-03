<template>
  <v-card>
    <v-card-title>Personen</v-card-title>
    <v-card-subtitle>Lege Personen fuer die Zuordnung an.</v-card-subtitle>
    <v-card-text>
      <v-form @submit.prevent="submitPerson">
        <v-text-field
          v-model="newPersonName"
          label="Name"
          placeholder="Alex"
          required
        />
        <v-btn :loading="personSaving" type="submit" color="primary">
          Person hinzufügen
        </v-btn>
      </v-form>
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
        </template>
        <template #append>
          <v-chip size="small">#{{ person.id }}</v-chip>
        </template>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
  persons: { type: Array, required: true },
  personSaving: { type: Boolean, required: true },
  createPerson: { type: Function, required: true },
  updatePerson: { type: Function, required: true }
});

const newPersonName = ref("");
const editingPersonId = ref(null);
const editingPersonName = ref("");

const submitPerson = async () => {
  const success = await props.createPerson(newPersonName.value);
  if (success) {
    newPersonName.value = "";
  }
};

const startPersonEdit = (person) => {
  editingPersonId.value = person.id;
  editingPersonName.value = person.name;
};

const cancelPersonEdit = () => {
  editingPersonId.value = null;
  editingPersonName.value = "";
};

const savePersonEdit = async (id) => {
  const success = await props.updatePerson(id, editingPersonName.value);
  if (success) {
    cancelPersonEdit();
  }
};
</script>
