const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_senior_123";
const BOB_UID = "bob_senior_456";
const TODAY_KEY = "2026-11-16";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read or write daily_logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc(TODAY_KEY).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_logs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc(TODAY_KEY).set({
      userId: ALICE_UID,
      dateKey: TODAY_KEY,
      morningTaken: true,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 1,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid Kotlin create & action-based partial updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc(TODAY_KEY);
  const created = new Date(Date.now() - 5000);

  // Create initial daily log (mirrors Kotlin createPayload, no 'id' field)
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      dateKey: TODAY_KEY,
      morningTaken: true,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Action: toggle lunch medication
  await assertSucceeds(
    docRef.update({
      lunchTaken: true,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Action: toggle dinner medication
  await assertSucceeds(
    docRef.update({
      dinnerTaken: true,
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Action: increment water cups
  await assertSucceeds(
    docRef.update({
      waterCups: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner read & list succeed
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").get()
  );
});

test("Authenticated owner: invalid schemas, extra fields, out-of-bounds waterCups, and immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc(TODAY_KEY);
  const now = new Date(Date.now() - 2000);

  // Reject extra @DocumentId 'id' field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-17").set({
      id: "2026-11-17",
      userId: ALICE_UID,
      dateKey: "2026-11-17",
      morningTaken: false,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid dateKey document ID format
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("invalid_date").set({
      userId: ALICE_UID,
      dateKey: "invalid_date",
      morningTaken: false,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18").set({
      userId: ALICE_UID,
      dateKey: "2026-11-18",
      morningTaken: false,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Create valid doc first
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      dateKey: TODAY_KEY,
      morningTaken: false,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject negative or excessive waterCups
  await assertFails(docRef.update({ waterCups: -1, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(docRef.update({ waterCups: 31, updatedAt: new Date(Date.now() - 500) }));

  // Reject mutating multiple unrelated fields or immutable createdAt/userId
  await assertFails(
    docRef.update({
      morningTaken: true,
      waterCups: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    docRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's daily_logs", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("daily_logs").doc(TODAY_KEY).set({
      userId: BOB_UID,
      dateKey: TODAY_KEY,
      morningTaken: true,
      lunchTaken: false,
      dinnerTaken: false,
      waterCups: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").doc(TODAY_KEY).get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").doc(TODAY_KEY).update({
      morningTaken: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
