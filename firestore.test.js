const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validItemPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    name: "Sage Linen Overshirt",
    imageUrl: "preset:top_sage_linen",
    category: "Top",
    weather: "Mild",
    style: "Casual",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or create wardrobe items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("wardrobe").get());
  await assertFails(
    unauthDb.collection("wardrobe").doc("item_1").set(validItemPayload(ALICE_UID))
  );
});

test("Authenticated user: can create and read their own valid wardrobe item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("wardrobe").doc("alice_item_1").set(validItemPayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("wardrobe").doc("alice_item_1").get());
});

test("Authenticated user: cannot read or delete another user's wardrobe item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("wardrobe")
      .doc("bob_item_1")
      .set(validItemPayload(BOB_UID, { name: "Charcoal Wool Coat" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("wardrobe").doc("bob_item_1").get());
  await assertFails(aliceDb.collection("wardrobe").doc("bob_item_1").delete());
});

test("Authenticated user: can query their own wardrobe items with userId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("wardrobe")
      .doc("alice_item_2")
      .set(validItemPayload(ALICE_UID, { name: "White Leather Sneakers" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("wardrobe").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails list query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("wardrobe").get());
});

test("Authenticated user: rejects invalid enum values or extra ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("wardrobe")
      .doc("bad_cat")
      .set(validItemPayload(ALICE_UID, { category: "Hat" }))
  );
  await assertFails(
    aliceDb
      .collection("wardrobe")
      .doc("bad_weather")
      .set(validItemPayload(ALICE_UID, { weather: "Snowy" }))
  );
  await assertFails(
    aliceDb
      .collection("wardrobe")
      .doc("ghost_field")
      .set(validItemPayload(ALICE_UID, { isVerified: true }))
  );
});
