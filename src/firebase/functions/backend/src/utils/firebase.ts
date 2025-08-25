import * as admin from 'firebase-admin';

const serviceAccount = require('../bootstrap/wordwar-firebase-adminsdk.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://word-war-4-default-rtdb.asia-southeast1.firebasedatabase.app"
});

export const firebaseAuth = admin.auth();
export const firebaseDatabase = admin.database();

