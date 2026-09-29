import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

test("HTTP diagnostics preserve current retry behavior and redact credentials", async () => {
  // Supply Vite's build-time environment for this dependency-free Node test.
  const source = (await readFile(new URL("../src/composables/authHttp.js", import.meta.url), "utf8"))
    .replace("import.meta.env.VITE_API_BASE_URL", '"https://example.test"');
  const client = await import(`data:text/javascript;base64,${Buffer.from(source).toString("base64")}`);
  const originalFetch = globalThis.fetch;
  const originalWindow = globalThis.window;
  const originalWarn = console.warn;
  const warnings = [];
  const stored = new Map([[client.TOKEN_KEY, "private-token"]]);
  globalThis.window = { localStorage: {
    getItem: (key) => stored.get(key),
    setItem: (key, value) => stored.set(key, value),
    removeItem: (key) => stored.delete(key)
  } };
  console.warn = (...args) => warnings.push(args[1]);
  let responses = [];
  globalThis.fetch = async () => {
    assert.ok(responses.length, "unexpected retry");
    return responses.shift();
  };
  const rejected = (id, status = 401) => new Response(null, {
    status, headers: { "X-Request-ID": id }
  });
  try {
    responses = [rejected("initial-id"), new Response(JSON.stringify({ token: "new-private-token" })), rejected("retry-id")];
    await assert.rejects(client.requestJson("/api/savings-accounts?secret=private-query"),
      /Request failed with 401 \(GET \/api\/savings-accounts; request ID: retry-id\)/);
    assert.equal(responses.length, 0);
    assert.deepEqual(warnings.map((entry) => entry.phase), ["initial", "retry"]);
    assert.deepEqual(warnings.map((entry) => entry.requestId), ["initial-id", "retry-id"]);
    assert.doesNotMatch(JSON.stringify(warnings), /private-token|private-query/);

    warnings.length = 0;
    responses = [rejected("initial-id"), rejected("refresh-id")];
    await assert.rejects(client.requestJson("/api/savings-accounts"), /request ID: initial-id/);
    assert.deepEqual(warnings.map((entry) => entry.phase), ["initial", "refresh"]);
    assert.equal(warnings[1].requestId, "refresh-id");
    assert.equal(client.hasStoredToken(), false);

    // The current client refreshes on 401 only; diagnostics must not introduce 403 retries.
    responses = [rejected("forbidden-id", 403)];
    await assert.rejects(client.requestJson("/api/savings-accounts"), /Request failed with 403/);
    assert.equal(responses.length, 0);
  } finally {
    globalThis.fetch = originalFetch;
    globalThis.window = originalWindow;
    console.warn = originalWarn;
  }
});
