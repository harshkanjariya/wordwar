import * as admin from 'firebase-admin';

const isEmulator = process.env.FIREBASE_DATABASE_EMULATOR_HOST;

if (isEmulator || process.env.GCLOUD_PROJECT) {
  admin.initializeApp({
    databaseURL: "https://word-war-4-default-rtdb.asia-southeast1.firebasedatabase.app"
  });
} else {
  const serviceAccount = require('../bootstrap/wordwar-firebase-adminsdk.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://word-war-4-default-rtdb.asia-southeast1.firebasedatabase.app"
  });
}

export const firebaseAuth = admin.auth();
export const firebaseDatabase = admin.database();