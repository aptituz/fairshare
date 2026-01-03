<template>
  <v-card>
    <v-card-title>{{ title }}</v-card-title>
    <v-card-subtitle>{{ description }}</v-card-subtitle>
    <v-card-text>
      <v-form @submit.prevent="submit">
        <v-text-field
          v-if="showNameField"
          v-model="name"
          label="Name"
          :placeholder="placeholder"
          required
        />
        <v-text-field
          v-model="amount"
          label="Betrag"
          type="number"
          step="0.01"
          min="0"
          required
        />
        <v-select
          v-if="showFrequencyPicker"
          v-model="frequency"
          label="Frequenz"
          :items="frequencyOptions"
          item-title="title"
          item-value="value"
          required
        />
        <MonthYearPicker
          v-if="frequency === 'ONE_TIME'"
          :model-value="oneTimeMonth"
          label="Monat"
          :year-range="20"
          @update:modelValue="updateOneTimeMonth"
        />
        <div v-if="showStartEndFields" class="text-caption">Beruecksichtigen ab</div>
        <MonthYearPicker
          v-if="showStartEndFields"
          :model-value="startMonth"
          :year-range="20"
          @update:modelValue="updateStartMonth"
        />
        <div v-if="showStartEndFields" class="text-caption">Beruecksichtigen bis</div>
        <MonthYearPicker
          v-if="showStartEndFields"
          :model-value="endMonth"
          :year-range="20"
          :allow-empty="true"
          :clearable="true"
          @update:modelValue="updateEndMonth"
        />
        <v-combobox
          v-if="allowCategoryCreate"
          v-model="categoryId"
          v-model:search="categorySearch"
          label="Kategorie"
          :items="sortedCategories"
          item-title="label"
          item-value="id"
          clearable
          :hide-no-data="false"
          :return-object="false"
          @keydown.enter.prevent="commitCategorySearch"
        >
          <template #no-data>
            <v-list-item @click="commitCategorySearch">
              <v-list-item-title>
                Neue Kategorie erstellen: {{ categorySearch || "…" }}
              </v-list-item-title>
            </v-list-item>
          </template>
        </v-combobox>
        <v-autocomplete
          v-else
          v-model="categoryId"
          label="Kategorie"
          :items="sortedCategories"
          item-title="label"
          item-value="id"
          clearable
        />
        <v-autocomplete
          v-if="showPersonSelector"
          v-model="personId"
          label="Person"
          :items="persons"
          item-title="name"
          item-value="id"
          clearable
        />
        <v-btn :loading="saving" type="submit" color="primary" class="mt-2">
          {{ buttonLabel }}
        </v-btn>
      </v-form>
      <v-divider v-if="showCorrectionForm" class="my-4" />
      <div v-if="showCorrectionForm">
        <div class="text-subtitle-2 font-weight-medium mb-2">
          Korrektur fuer Ist-Betrag
        </div>
        <div class="d-flex flex-column ga-2">
          <v-autocomplete
            v-model="correctionCategoryId"
            label="Kategorie"
            :items="sortedCategories"
            item-title="label"
            item-value="id"
            density="compact"
            hide-details
            clearable
          />
          <v-text-field
            v-model="correctionAmount"
            label="Ist-Betrag (aktueller Monat)"
            type="number"
            step="0.01"
            min="0"
            density="compact"
            hide-details
          />
          <v-btn
            size="small"
            variant="outlined"
            @click="submitCategoryCorrection"
          >
            Korrektur erfassen
          </v-btn>
        </div>
      </div>
    </v-card-text>
    <v-divider />
    <div v-if="showFrequencyTable">
      <div v-for="group in groupedBudgetItems" :key="group.key" class="mb-6">
        <v-table density="compact">
          <thead>
            <tr>
              <th colspan="9" class="text-left text-subtitle-1 font-weight-bold">
                {{ group.label }}
              </th>
            </tr>
            <tr>
              <th class="text-left">Name</th>
              <th class="text-right">Monatlich</th>
              <th class="text-right">Einmalig</th>
              <th class="text-right">Quartalsweise</th>
              <th class="text-right">Halbjährlich</th>
              <th class="text-right">Jährlich</th>
              <th class="text-right">Monatsanteil</th>
              <th class="text-right">Aktiv</th>
              <th class="text-right">Aktion</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="budgetItem in group.items" :key="budgetItem.id">
              <td>
                <div class="d-flex align-center ga-2">
                  <span style="cursor: pointer;" @click="startEdit(budgetItem)">
                    {{ budgetItem.name }}
                  </span>
                  <v-chip v-if="budgetItem.active === false" size="x-small" color="warning">
                    Inaktiv
                  </v-chip>
                </div>
              </td>
              <td class="text-right">{{ frequencyAmount(budgetItem, "MONTHLY") }}</td>
              <td class="text-right">{{ frequencyAmount(budgetItem, "ONE_TIME") }}</td>
              <td class="text-right">{{ frequencyAmount(budgetItem, "QUARTERLY") }}</td>
              <td class="text-right">{{ frequencyAmount(budgetItem, "HALF_YEARLY") }}</td>
              <td class="text-right">{{ frequencyAmount(budgetItem, "YEARLY") }}</td>
              <td class="text-right">{{ formatCurrency(budgetItem.monthlyAmount) }}</td>
              <td class="text-right">
                <v-checkbox
                  :model-value="budgetItem.active !== false"
                  density="compact"
                  hide-details
                  @update:model-value="(value) => toggleActive(budgetItem, value)"
                />
              </td>
              <td class="text-right">
                <v-btn
                  size="small"
                  variant="text"
                  icon
                  color="error"
                  @click="remove(budgetItem.id)"
                >
                  <v-icon icon="mdi-delete" />
                </v-btn>
              </td>
            </tr>
            <tr>
              <td class="font-weight-bold">{{ group.label }} Gesamt</td>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
              <td class="text-right font-weight-bold">{{ formatCurrency(group.total) }}</td>
              <td></td>
              <td></td>
            </tr>
          </tbody>
        </v-table>
      </div>
    </div>
    <v-list v-if="!showFrequencyTable" density="compact">
      <v-list-item v-for="budgetItem in budgetItems" :key="budgetItem.id">
          <v-list-item-title>
            <span style="cursor: pointer;" @click="startEdit(budgetItem)">{{ budgetItem.name }}</span>
          </v-list-item-title>
          <template #append>
            <div class="d-flex align-center ga-3">
              <span style="cursor: pointer;" @click="startEdit(budgetItem)">
                {{ formatCurrency(budgetItem.amount) }}
              </span>
              <v-btn
                size="small"
                variant="text"
                icon
                color="error"
                @click="remove(budgetItem.id)"
              >
                <v-icon icon="mdi-delete" />
              </v-btn>
            </div>
          </template>
      </v-list-item>
    </v-list>
    <v-dialog v-model="editDialogOpen" max-width="560">
      <v-card>
        <v-card-title>Budget-Posten bearbeiten</v-card-title>
        <v-card-text>
          <div class="d-flex flex-column ga-2">
            <v-text-field
              v-if="showNameField"
              v-model="editingName"
              label="Name"
              density="compact"
              hide-details
              @keyup.enter="saveEdit(editingId)"
            />
            <v-text-field
              v-model="editingAmount"
              label="Betrag"
              type="number"
              step="0.01"
              min="0"
              density="compact"
              hide-details
              @keyup.enter="saveEdit(editingId)"
            />
            <v-select
              v-if="showFrequencyPicker"
              v-model="editingFrequency"
              label="Frequenz"
              :items="frequencyOptions"
              item-title="title"
              item-value="value"
              density="compact"
              hide-details
            />
            <MonthYearPicker
              v-if="editingFrequency === 'ONE_TIME'"
              :model-value="editingOneTimeMonth"
              label="Monat"
              :year-range="20"
              @update:modelValue="updateEditingOneTimeMonth"
            />
            <div v-if="showEditingStartEndFields" class="text-caption">Beruecksichtigen ab</div>
            <MonthYearPicker
              v-if="showEditingStartEndFields"
              :model-value="editingStartMonth"
              :year-range="20"
              @update:modelValue="updateEditingStartMonth"
            />
            <div v-if="showEditingStartEndFields" class="text-caption">Beruecksichtigen bis</div>
            <MonthYearPicker
              v-if="showEditingStartEndFields"
              :model-value="editingEndMonth"
              :year-range="20"
              :allow-empty="true"
              :clearable="true"
              @update:modelValue="updateEditingEndMonth"
            />
            <v-combobox
              v-if="allowCategoryCreate"
              v-model="editingCategoryId"
              v-model:search="editingCategorySearch"
              label="Kategorie"
              :items="sortedCategories"
              item-title="label"
              item-value="id"
              density="compact"
              hide-details
              clearable
              :hide-no-data="false"
              :return-object="false"
              @keydown.enter.prevent="commitEditingCategorySearch"
            >
              <template #no-data>
                <v-list-item @click="commitEditingCategorySearch">
                  <v-list-item-title>
                    Neue Kategorie erstellen: {{ editingCategorySearch || "…" }}
                  </v-list-item-title>
                </v-list-item>
              </template>
            </v-combobox>
            <v-autocomplete
              v-else
              v-model="editingCategoryId"
              label="Kategorie"
              :items="sortedCategories"
              item-title="label"
              item-value="id"
              density="compact"
              hide-details
              clearable
            />
            <v-autocomplete
              v-if="showPersonSelector"
              v-model="editingPersonId"
              label="Person"
              :items="persons"
              item-title="name"
              item-value="id"
              density="compact"
              hide-details
              clearable
            />
            <v-divider class="my-2" />
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelEdit">Abbrechen</v-btn>
          <v-btn color="primary" @click="saveEdit(editingId)">Speichern</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="confirmOverrideOpen" max-width="420">
      <v-card>
        <v-card-title>Betrag aendern</v-card-title>
        <v-card-text>
          Soll der geaenderte Betrag nur fuer den aktuellen Monat gelten?
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="cancelOverrideChoice">Abbrechen</v-btn>
          <v-btn variant="outlined" @click="confirmOverride(false)">Dauerhaft</v-btn>
          <v-btn color="primary" @click="confirmOverride(true)">Nur aktueller Monat</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script setup>
import { computed, ref } from "vue";
import MonthYearPicker from "./MonthYearPicker.vue";

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  placeholder: { type: String, required: true },
  type: { type: String, required: true },
  categories: { type: Array, required: true },
  persons: { type: Array, required: true },
  budgetItems: { type: Array, required: true },
  summaryMonth: { type: String, required: true },
  saving: { type: Boolean, required: true },
  formatCurrency: { type: Function, required: true },
  categoryPathLabel: { type: Function, required: true },
  onCreateBudgetItem: { type: Function, required: true },
  onUpdateBudgetItem: { type: Function, required: true },
  onDeleteBudgetItem: { type: Function, required: true },
  onOverrideBudgetItemForMonth: { type: Function, required: true },
  onCreateCategoryCorrection: { type: Function, required: true },
  onCreateCategory: { type: Function, default: null },
  fixedPersonId: { type: [Number, null], default: undefined },
  showPersonSelector: { type: Boolean, default: true },
  allowCategoryCreate: { type: Boolean, default: false }
});

const name = ref("");
const amount = ref("");
const categoryId = ref(null);
const categorySearch = ref("");
const personId = ref(null);
const frequency = ref("MONTHLY");
const oneTimeMonth = ref("");
const startMonth = ref(props.summaryMonth);
const endMonth = ref("");
const editingId = ref(null);
const editingName = ref("");
const editingAmount = ref("");
const editingCategoryId = ref(null);
const editingCategorySearch = ref("");
const editingPersonId = ref(null);
const editingFrequency = ref("MONTHLY");
const editingOneTimeMonth = ref("");
const editingStartMonth = ref("");
const editingEndMonth = ref("");
const editingActive = ref(true);
const editDialogOpen = ref(false);
const confirmOverrideOpen = ref(false);
const originalAmount = ref("");
const pendingSaveId = ref(null);
const correctionCategoryId = ref(null);
const correctionAmount = ref("");

const buttonLabel = computed(() =>
  props.type === "INCOME" ? "Einnahme hinzufügen" : "Ausgabe hinzufügen"
);

const showNameField = computed(() => props.type !== "INCOME");
const showFrequencyPicker = computed(() => true);
const showFrequencyTable = computed(() => props.type === "EXPENSE");
const showStartEndFields = computed(() => frequency.value !== "ONE_TIME");
const showEditingStartEndFields = computed(() => editingFrequency.value !== "ONE_TIME");
const showCorrectionForm = computed(() => props.type === "EXPENSE");

const sortedCategories = computed(() =>
  props.categories.slice().sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0))
);

const frequencyOptions = [
  { title: "Monatlich", value: "MONTHLY" },
  { title: "Einmalig", value: "ONE_TIME" },
  { title: "Quartalsweise", value: "QUARTERLY" },
  { title: "Halbjährlich", value: "HALF_YEARLY" },
  { title: "Jährlich", value: "YEARLY" }
];

const findCategoryIdByLabel = (label) => {
  const trimmed = label.trim();
  if (!trimmed) {
    return null;
  }
  const match = props.categories.find(
    (category) => category.name.toLowerCase() === trimmed.toLowerCase()
  );
  return match?.id ?? null;
};

const categoryNameFor = (value) => {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  if (typeof value === "object") {
    return value.name || value.label || "";
  }
  if (typeof value === "string") {
    return value.trim();
  }
  const match = props.categories.find((category) => category.id === value);
  return match?.name || match?.label || "";
};

const groupedBudgetItems = computed(() => {
  if (!showFrequencyTable.value) {
    return [];
  }
  const rankById = new Map(
    sortedCategories.value.map((category) => [category.id, category.rank ?? Number.MAX_SAFE_INTEGER])
  );
  const groups = new Map();
  props.budgetItems.forEach((item) => {
    const categoryId = item.category?.id ?? null;
    const label = props.categoryPathLabel(item.category?.id) || "Keine Kategorie";
    const key = categoryId ?? `none-${label}`;
    if (!groups.has(key)) {
      groups.set(key, { key, label, categoryId, items: [] });
    }
    groups.get(key).items.push(item);
  });
  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      rank: group.categoryId
        ? rankById.get(group.categoryId) ?? Number.MAX_SAFE_INTEGER
        : Number.MAX_SAFE_INTEGER,
      items: group.items.slice().sort((a, b) => a.name.localeCompare(b.name)),
      total: group.items.reduce((sum, item) => {
        if (item.active === false) {
          return sum;
        }
        return sum + Number(item.monthlyAmount || 0);
      }, 0)
    }))
    .sort((a, b) => {
      if (a.rank !== b.rank) {
        return a.rank - b.rank;
      }
      return a.label.localeCompare(b.label);
    });
});

const commitCategorySearch = async () => {
  if (!props.allowCategoryCreate) {
    return;
  }
  const trimmed = categorySearch.value.trim();
  if (!trimmed) {
    return;
  }
  const existingId = findCategoryIdByLabel(trimmed);
  if (existingId) {
    categoryId.value = existingId;
    return;
  }
  if (!props.onCreateCategory) {
    categoryId.value = trimmed;
    return;
  }
  const created = await props.onCreateCategory(props.type, trimmed);
  categoryId.value = created?.id ?? trimmed;
};

const commitEditingCategorySearch = async () => {
  if (!props.allowCategoryCreate) {
    return;
  }
  const trimmed = editingCategorySearch.value.trim();
  if (!trimmed) {
    return;
  }
  const existingId = findCategoryIdByLabel(trimmed);
  if (existingId) {
    editingCategoryId.value = existingId;
    return;
  }
  if (!props.onCreateCategory) {
    editingCategoryId.value = trimmed;
    return;
  }
  const created = await props.onCreateCategory(props.type, trimmed);
  editingCategoryId.value = created?.id ?? trimmed;
};

const frequencyAmount = (budgetItem, frequency) => {
  if (budgetItem.frequency !== frequency) {
    return "";
  }
  return props.formatCurrency(budgetItem.amount);
};

const monthToDate = (value) => {
  if (!value) {
    return null;
  }
  return `${value}-01`;
};

const monthToEndDate = (value) => {
  if (!value) {
    return null;
  }
  const [year, month] = value.split("-").map((part) => Number(part));
  if (!year || !month) {
    return null;
  }
  const lastDay = new Date(year, month, 0).getDate();
  const paddedMonth = String(month).padStart(2, "0");
  const paddedDay = String(lastDay).padStart(2, "0");
  return `${year}-${paddedMonth}-${paddedDay}`;
};

const dateToMonth = (value) => {
  if (!value) {
    return "";
  }
  return String(value).slice(0, 7);
};

const updateOneTimeMonth = (value) => {
  oneTimeMonth.value = value;
};

const updateStartMonth = (value) => {
  startMonth.value = value;
};

const updateEndMonth = (value) => {
  endMonth.value = value;
};

const updateEditingOneTimeMonth = (value) => {
  editingOneTimeMonth.value = value;
};

const updateEditingStartMonth = (value) => {
  editingStartMonth.value = value;
};

const updateEditingEndMonth = (value) => {
  editingEndMonth.value = value;
};

const submit = async () => {
  const derivedName = showNameField.value ? name.value : categoryNameFor(categoryId.value);
  const effectivePersonId = props.showPersonSelector ? personId.value : props.fixedPersonId ?? null;
  const categoryValue = categoryId.value ?? categorySearch.value;
  const startDate =
    frequency.value === "ONE_TIME"
      ? monthToDate(oneTimeMonth.value)
      : monthToDate(startMonth.value);
  const endDate = frequency.value === "ONE_TIME" ? null : monthToEndDate(endMonth.value);
  const success = await props.onCreateBudgetItem({
    name: derivedName,
    amount: amount.value,
    categoryId: categoryValue,
    personId: effectivePersonId,
    type: props.type,
    frequency: showFrequencyPicker.value ? frequency.value : null,
    startDate,
    endDate,
    active: true
  });
  if (success) {
    name.value = "";
    amount.value = "";
    categoryId.value = null;
    categorySearch.value = "";
    personId.value = null;
    frequency.value = "MONTHLY";
    oneTimeMonth.value = "";
    startMonth.value = props.summaryMonth;
    endMonth.value = "";
  }
};

const submitCategoryCorrection = async () => {
  const monthValue = props.summaryMonth;
  const effectivePersonId = props.showPersonSelector ? personId.value : props.fixedPersonId ?? null;
  const categoryValue = correctionCategoryId.value;
  const success = await props.onCreateCategoryCorrection({
    categoryId: categoryValue,
    personId: effectivePersonId,
    month: monthValue,
    actualAmount: correctionAmount.value
  });
  if (success) {
    correctionCategoryId.value = null;
    correctionAmount.value = "";
  }
};

const startEdit = (budgetItem) => {
  editingId.value = budgetItem.id;
  editingName.value = budgetItem.name;
  editingAmount.value = budgetItem.amount;
  originalAmount.value = budgetItem.amount;
  editingCategoryId.value = budgetItem.category?.id ?? null;
  editingCategorySearch.value = budgetItem.category?.name || "";
  editingPersonId.value = budgetItem.person?.id ?? null;
  editingFrequency.value = budgetItem.frequency || "MONTHLY";
  editingOneTimeMonth.value = dateToMonth(budgetItem.startDate);
  editingStartMonth.value = dateToMonth(budgetItem.startDate);
  editingEndMonth.value = dateToMonth(budgetItem.endDate);
  editingActive.value = budgetItem.active !== false;
  editDialogOpen.value = true;
};

const cancelEdit = () => {
  editingId.value = null;
  editingName.value = "";
  editingAmount.value = "";
  editingCategoryId.value = null;
  editingCategorySearch.value = "";
  editingPersonId.value = null;
  editingFrequency.value = "MONTHLY";
  editingOneTimeMonth.value = "";
  editingStartMonth.value = "";
  editingEndMonth.value = "";
  editingActive.value = true;
  editDialogOpen.value = false;
  confirmOverrideOpen.value = false;
  pendingSaveId.value = null;
  originalAmount.value = "";
};

const saveEdit = async (id) => {
  if (!id) {
    return;
  }
  const amountChanged = Number(editingAmount.value) !== Number(originalAmount.value);
  if (props.type === "EXPENSE" && amountChanged) {
    pendingSaveId.value = id;
    confirmOverrideOpen.value = true;
    return;
  }
  await saveEditWithMode(id, false);
};

const saveEditWithMode = async (id, overrideForMonth) => {
  if (!id) {
    return;
  }
  if (overrideForMonth) {
    const success = await props.onOverrideBudgetItemForMonth(
      id,
      props.summaryMonth,
      editingAmount.value
    );
    if (success) {
      cancelEdit();
    }
    return;
  }
  const nameForSave = showNameField.value
    ? editingName.value
    : categoryNameFor(editingCategoryId.value) || editingName.value;
  const effectivePersonId = props.showPersonSelector ? editingPersonId.value : props.fixedPersonId ?? null;
  const categoryValue = editingCategoryId.value ?? editingCategorySearch.value;
  const startDate =
    editingFrequency.value === "ONE_TIME"
      ? monthToDate(editingOneTimeMonth.value)
      : monthToDate(editingStartMonth.value);
  const endDate = editingFrequency.value === "ONE_TIME" ? null : monthToEndDate(editingEndMonth.value);
  const success = await props.onUpdateBudgetItem(
    id,
    nameForSave,
    editingAmount.value,
    categoryValue,
    effectivePersonId,
    props.type,
    showFrequencyPicker.value ? editingFrequency.value : null,
    editingActive.value,
    startDate,
    endDate
  );
  if (success) {
    cancelEdit();
  }
};

const confirmOverride = async (overrideForMonth) => {
  if (!pendingSaveId.value) {
    confirmOverrideOpen.value = false;
    return;
  }
  confirmOverrideOpen.value = false;
  await saveEditWithMode(pendingSaveId.value, overrideForMonth);
  pendingSaveId.value = null;
};

const cancelOverrideChoice = () => {
  confirmOverrideOpen.value = false;
  pendingSaveId.value = null;
};

const toggleActive = async (budgetItem, nextValue) => {
  const success = await props.onUpdateBudgetItem(
    budgetItem.id,
    budgetItem.name,
    budgetItem.amount,
    budgetItem.category?.id ?? null,
    budgetItem.person?.id ?? null,
    props.type,
    budgetItem.frequency,
    nextValue,
    budgetItem.frequency === "ONE_TIME" ? budgetItem.startDate : budgetItem.startDate,
    budgetItem.frequency === "ONE_TIME" ? null : budgetItem.endDate
  );
  return success;
};

const remove = async (id) => {
  if (!window.confirm("Transaktion wirklich loeschen?")) {
    return;
  }
  await props.onDeleteBudgetItem(id);
};

const personLabel = (budgetItem) => {
  return budgetItem.person?.name || "Gemeinsam";
};
</script>
