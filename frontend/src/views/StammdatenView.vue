<template>
  <div v-if="currentSubView === 'categories'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Strukturiere Kategorien fuer wiederkehrende Posten.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <CategoryManagementCard
          :incomeCategoryOptions="incomeCategoryOptions"
          :expenseCategoryOptions="expenseCategoryOptions"
          :categorySaving="categorySaving"
          :createCategory="createCategory"
          :updateCategory="updateCategory"
          :deleteCategory="deleteCategory"
        />
      </v-col>
    </v-row>
  </div>

  <div v-if="currentSubView === 'persons'">
    <v-row>
      <v-col cols="12">
        <h2 class="text-h5 mb-2">Stammdaten</h2>
        <p class="text-body-2">Verwalte Personen fuer gemeinsame Budgets.</p>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <PersonManagementCard
          :persons="persons"
          :personSaving="personSaving"
          :createPerson="createPerson"
          :updatePerson="updatePerson"
        />
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import CategoryManagementCard from "../components/stammdaten/CategoryManagementCard.vue";
import PersonManagementCard from "../components/stammdaten/PersonManagementCard.vue";

const props = defineProps({
  currentSubView: { type: String, required: true },
  incomeCategoryOptions: { type: Array, required: true },
  expenseCategoryOptions: { type: Array, required: true },
  persons: { type: Array, required: true },
  categorySaving: { type: Boolean, required: true },
  personSaving: { type: Boolean, required: true },
  createCategory: { type: Function, required: true },
  updateCategory: { type: Function, required: true },
  deleteCategory: { type: Function, required: true },
  createPerson: { type: Function, required: true },
  updatePerson: { type: Function, required: true }
});
</script>
