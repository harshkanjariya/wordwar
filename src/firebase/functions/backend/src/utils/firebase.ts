import * as admin from 'firebase-admin';

const serviceAccount = require('../bootstrap/wordwar-firebase-adminsdk.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

export const firebaseAuth = admin.auth();
