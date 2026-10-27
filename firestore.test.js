const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_123";
const BOB_UID = "bob_student_456";

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

// --- SECURITY BOUNDS & ISOLATION TESTS ---

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot write topic progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("topics_progress").doc("topic_1").set({
      userId: ALICE_UID,
      topicId: "topic_1",
      paperId: 1,
      chapterName: "AS 10",
      topicTitle: "PPE",
      status: "DONE"
    })
  );
});

test("Authenticated user: can create and read own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      fullName: "Alice Student",
      attempt: "Jan 2027",
      targetGroup: "Both Groups",
      createdAt: new Date()
    })
  );

  const doc = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  if (!doc.exists) {
    throw new Error("Expected doc to exist");
  }
});

test("Authenticated user: cannot read another user's profile (cross-user isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@test.com",
      fullName: "Bob Student",
      attempt: "Jan 2027",
      targetGroup: "Group 1",
      createdAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can create and read own topic progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("topics_progress").doc("topic_p1_01").set({
      userId: ALICE_UID,
      topicId: "topic_p1_01",
      paperId: 1,
      chapterName: "AS 10 - Property, Plant & Equipment",
      topicTitle: "Recognition and Component Accounting",
      icaiRef: "AS 10",
      isCustom: false,
      status: "DONE",
      notes: "Revised depreciation components"
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("topics_progress").get()
  );
});

test("Authenticated user: cannot read another user's topic progress", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("topics_progress").doc("topic_p1_01").set({
      userId: BOB_UID,
      topicId: "topic_p1_01",
      paperId: 1,
      chapterName: "AS 10",
      topicTitle: "PPE",
      status: "DONE"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("topics_progress").doc("topic_p1_01").get()
  );
});

test("Authenticated user: can add mistake vault item for spaced repetition", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mistake_vault").doc("mistake_001").set({
      userId: ALICE_UID,
      mistakeId: "mistake_001",
      paperId: 2,
      subjectName: "Corporate and Other Laws",
      topicOrStandard: "Sec 135 - CSR",
      question: "Whether CSR Committee can be dispensed with?",
      marksLost: 2.5,
      totalMarks: 5.0,
      missingKeywords: "Sub-section (9), 50 lakh limit",
      toppersSolution: "Under Sec 135(9), where CSR obligation does not exceed Rs 50 lakh...",
      reviewIntervalDays: 1,
      isMastered: false,
      createdAt: new Date()
    })
  );
});

test("Authenticated user: cannot spoof another userId on mistake vault item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("mistake_vault").doc("mistake_spoof").set({
      userId: BOB_UID, // Spoofed!
      mistakeId: "mistake_spoof",
      paperId: 2,
      subjectName: "Corporate and Other Laws",
      topicOrStandard: "Sec 135",
      question: "Sample question",
      marksLost: 2.0,
      totalMarks: 5.0,
      reviewIntervalDays: 1,
      isMastered: false,
      createdAt: new Date()
    })
  );
});

test("Authenticated user: test history attempts are append-only (updates rejected)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("test_history").doc("test_01").set({
      userId: ALICE_UID,
      testId: "test_01",
      paperId: 1,
      paperName: "Advanced Accounting",
      chapterOrTopic: "Consolidation",
      totalMarks: 50,
      marksScored: 38.5,
      mcqScore: 12.0,
      descriptiveScore: 26.5,
      dateTaken: new Date(),
      summaryNotes: "Need more work on minority interest"
    })
  );

  // Updating test attempt must be rejected (immutable history)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("test_history").doc("test_01").update({
      marksScored: 50.0
    })
  );
});
