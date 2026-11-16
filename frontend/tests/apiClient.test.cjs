const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

const source = fs.readFileSync(path.join(__dirname, "../src/api/client.ts"), "utf8")
  .replace("import.meta.env.VITE_API_URL", '""');

const compiled = ts.transpileModule(source, {
  fileName: "client.ts",
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020,
  },
}).outputText;

const loadClient = (request) => {
  const context = {
    exports: {},
    fetch: request,
    Headers,
    Response,
    AbortController,
  };
  vm.runInNewContext(compiled, context, { filename: "client.js" });
  return context.exports;
};

const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { "Content-Type": "application/json" },
});

test("authenticated GET sends browser cookies and never sends bearer token", async () => {
  const calls = [];
  const { apiRequest } = loadClient(async (url, options) => {
    calls.push({ url, options });
    return json({ username: "student", email: "student@example.org", role: "USER" });
  });

  const result = await apiRequest("/auth/me");
  assert.equal(result.username, "student");
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, "/auth/me");
  assert.equal(calls[0].options.credentials, "include");
  assert.equal(calls[0].options.cache, "no-store");
  assert.equal(calls[0].options.headers.has("Authorization"), false);
});

test("unsafe requests fetch CSRF token and send it with cookies", async () => {
  const calls = [];
  const { apiRequest } = loadClient(async (url, options) => {
    calls.push({ url, options });
    if (url === "/auth/csrf") return json({ token: "masked-token", headerName: "X-XSRF-TOKEN" });
    return json({ username: "student", email: "student@example.org", role: "USER" });
  });

  await apiRequest("/auth/sign-in", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: "student", password: "password123", rememberMe: true }),
  });

  assert.deepEqual(calls.map((entry) => entry.url), ["/auth/csrf", "/auth/sign-in"]);
  assert.equal(calls[1].options.headers.get("X-XSRF-TOKEN"), "masked-token");
  assert.equal(calls[1].options.credentials, "include");
  assert.equal(calls[1].options.headers.get("Content-Type"), "application/json");
  assert.equal(calls[1].options.headers.has("Authorization"), false);
});

test("multipart upload does not force JSON Content-Type", async () => {
  const calls = [];
  const { apiRequest } = loadClient(async (url, options) => {
    calls.push({ url, options });
    if (url === "/auth/csrf") return json({ token: "value", headerName: "X-XSRF-TOKEN" });
    return json({ imageId: "image-1", status: "PENDING" }, 202);
  });

  const result = await apiRequest("/images/upload", { method: "POST", body: "multipart-mock" });
  assert.equal(result.status, "PENDING");
  assert.equal(calls[1].options.headers.has("Content-Type"), false);
  assert.equal(calls[1].options.headers.get("X-XSRF-TOKEN"), "value");
});

test("missing CSRF token prevents unsafe request", async () => {
  const calls = [];
  const { apiRequest } = loadClient(async (url) => {
    calls.push(url);
    return json({ headerName: "X-XSRF-TOKEN" });
  });

  await assert.rejects(apiRequest("/auth/logout", { method: "POST" }), /CSRF token is unavailable/);
  assert.deepEqual(calls, ["/auth/csrf"]);
});

test("204 No Content is handled without JSON parsing", async () => {
  const { apiRequest } = loadClient(async (url) => {
    if (url === "/auth/csrf") return json({ token: "value", headerName: "X-XSRF-TOKEN" });
    return new Response(null, { status: 204 });
  });

  assert.equal(await apiRequest("/users/me/email", { method: "PATCH" }), null);
});

test("HTTP errors are distinguished by status and safe message", async () => {
  const { apiRequest } = loadClient(async () => json({ message: "Authentication required" }, 401));
  await assert.rejects(apiRequest("/auth/me"), (error) => error.status === 401
    && error.message === "Authentication required");
});
