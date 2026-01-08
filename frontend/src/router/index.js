/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

import { createRouter, createWebHistory } from "vue-router";

const routes = [
  { path: "/", name: "overview" },
  { path: "/cost-split", name: "cost-split" },
  { path: "/income/:scope(.*)?", name: "income" },
  { path: "/expenses/:scope(.*)?", name: "expenses" },
  { path: "/master-data/:section?", name: "master-data" }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
