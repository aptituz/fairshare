/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { createRouter, createWebHistory } from "vue-router";

const EmptyRoute = { template: "<div></div>" };

const routes = [
  { path: "/", name: "overview", component: EmptyRoute },
  { path: "/wealth", name: "wealth", component: EmptyRoute },
  { path: "/cost-split", name: "cost-split", component: EmptyRoute },
  { path: "/income/:scope(.*)?", name: "income", component: EmptyRoute },
  { path: "/expenses/:scope(.*)?", name: "expenses", component: EmptyRoute },
  { path: "/master-data/:section?", name: "master-data", component: EmptyRoute }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
