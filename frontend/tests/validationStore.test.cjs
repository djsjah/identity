const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

const compiled = ts.transpileModule(
  fs.readFileSync(path.join(__dirname, "../src/store/validation.ts"), "utf8"),
  {
    fileName: "validation.ts",
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }
).outputText;

const createStore = (api) => {
  const storage = new Map();
  const timers = new Map();
  let timerId = 0;
  const authStore = { isAuthenticated: true, clearSession() { this.isAuthenticated = false; } };
  const context = {
    exports: {},
    require(moduleName) {
      if (moduleName === "mobx") return {
        makeAutoObservable() {},
        runInAction(fn) { fn(); },
      };
      if (moduleName === "../api/client") return { ApiError: class ApiError extends Error {}, apiRequest: api };
      if (moduleName === "./auth") return { authStore };
      throw new Error(`Unexpected import: ${moduleName}`);
    },
    sessionStorage: {
      getItem(key) { return storage.get(key) || null; },
      setItem(key, value) { storage.set(key, value); },
      removeItem(key) { storage.delete(key); },
    },
    setTimeout(fn) { const id = ++timerId; timers.set(id, fn); return id; },
    clearTimeout(id) { timers.delete(id); },
    FormData,
    AbortController,
    console,
  };
  vm.runInNewContext(compiled, context, { filename: "validation.js" });
  return {
    store: context.exports.validationStore,
    authStore,
    storage,
    async runTimer() {
      const [id, callback] = timers.entries().next().value || [];
      if (!callback) return false;
      timers.delete(id);
      callback();
      await new Promise((resolve) => setImmediate(resolve));
      return true;
    },
  };
};

const imageFile = () => new File([new Uint8Array([1, 2, 3])], "sample.jpg", { type: "image/jpeg" });

test("upload and polling transition to a real result without resubmitting", async () => {
  const calls = [];
  let pollCount = 0;
  const harness = createStore(async (url, options) => {
    calls.push({ url, options });
    if (url === "/images/upload") return { imageId: "image-123", status: "PENDING" };
    pollCount++;
    if (pollCount === 1) return { imageId: "image-123", status: "PENDING" };
    if (pollCount === 2) return { imageId: "image-123", status: "PROCESSING" };
    return {
      imageId: "image-123", status: "COMPLETED", predictedClass: 0,
      classDescription: "Real", fakeProbability: 0.2, error: null,
    };
  });

  await harness.store.validate(imageFile());
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(harness.storage.get("identity-image-id"), "image-123");
  assert.equal(harness.store.phase, "pending");
  assert.equal(await harness.runTimer(), true);
  assert.equal(harness.store.phase, "processing");
  assert.equal(await harness.runTimer(), true);
  assert.equal(harness.store.phase, "completed");
  assert.equal(harness.store.result.result, "Real");
  assert.equal(harness.store.result.probability, 0.8);
  assert.equal(calls.filter(({ url }) => url === "/images/upload").length, 1);
});

test("ML no-face terminal result is displayed without retrying the upload", async () => {
  const harness = createStore(async (url) => {
    if (url === "/images/upload") return { imageId: "image-456", status: "PENDING" };
    return {
      imageId: "image-456", status: "FAILED", predictedClass: null,
      classDescription: null, fakeProbability: null, error: "NO_FACE",
    };
  });
  await harness.store.validate(imageFile());
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(harness.store.phase, "failed");
  assert.equal(harness.store.errorStatus, "NoFaceDetected");
});

test("invalid image does not reach the upload endpoint", async () => {
  let calls = 0;
  const harness = createStore(async () => { calls++; });
  await harness.store.validate(new File(["bad"], "bad.txt", { type: "text/plain" }));
  assert.equal(harness.store.phase, "failed");
  assert.equal(harness.store.errorStatus, "InvalidFile");
  assert.equal(calls, 0);
});

test("reset cancels pending request and removes stored imageId", async () => {
  const harness = createStore(async (url) => {
    if (url === "/images/upload") return { imageId: "image-789", status: "PENDING" };
    return { imageId: "image-789", status: "PROCESSING" };
  });
  await harness.store.validate(imageFile());
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(harness.storage.get("identity-image-id"), "image-789");
  harness.store.reset();
  assert.equal(harness.store.imageId, null);
  assert.equal(harness.store.result, null);
  assert.equal(harness.storage.has("identity-image-id"), false);
  assert.equal(await harness.runTimer(), false);
});
