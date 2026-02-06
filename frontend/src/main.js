/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { createApp } from "vue";
import "@mdi/font/css/materialdesignicons.css";
import "vuetify/styles";
import { createVuetify } from "vuetify";
import * as components from "vuetify/components";
import * as directives from "vuetify/directives";
import App from "./App.vue";
import router from "./router";

const vuetify = createVuetify({
  components,
  directives
});

createApp(App).use(vuetify).use(router).mount("#app");

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    const baseUrl = import.meta.env.BASE_URL || "/";
    const swUrl = `${baseUrl}sw.js`;
    navigator.serviceWorker.register(swUrl, { scope: baseUrl });
  });
}
