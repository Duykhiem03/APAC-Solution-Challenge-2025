/**
 * Root-level Firebase Admin instance 
 * This file acts as a centralized point for all Firebase Admin SDK usage
 * throughout the project. Supports ES modules.
 */

import admin from 'firebase-admin';
import { createRequire } from 'module';

// For importing JSON in ESM
const require = createRequire(import.meta.url);

// Check if we already have initialized the app
let firebaseApp;

try {
  // Try to get the default app - if it exists, we're already initialized
  firebaseApp = admin.app();
  console.log('Using existing Firebase Admin app in root admin.js');
} catch (e) {
  // No default app exists, so initialize one
  try {
    // Look for service account credentials
    let serviceAccount;
    try {
      // Try to import credentials
      serviceAccount = require('../serviceAccountKey.json');
    } catch (credError) {
      console.log('No service account found, using application default credentials');
      // Continue without service account - will use default credentials
    }

    // Initialize with service account if available, otherwise use default credentials
    if (serviceAccount) {
      firebaseApp = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
    } else {
      firebaseApp = admin.initializeApp();
    }
    console.log('Firebase Admin successfully initialized in root admin.js');
  } catch (initError) {
    if (initError.code === 'app/duplicate-app') {
      try {
        firebaseApp = admin.app();
        console.log('Using existing Firebase Admin app after failed initialization');
      } catch (appError) {
        console.error('Fatal: Unable to get Firebase Admin app', appError);
        throw appError;
      }
    } else {
      console.error('Firebase Admin initialization error:', initError.message);
      throw initError;
    }
  }
}

// Export for ES modules
export default admin;
export { admin };
