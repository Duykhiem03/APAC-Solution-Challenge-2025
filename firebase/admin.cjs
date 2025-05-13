/**
 * CommonJS version of Firebase Admin initialization
 * This file is specifically for CommonJS modules (like Firebase Functions)
 */

// In a CommonJS module, we need to use require
let admin;
try {
  // First try to load firebase-admin from functions directory
  admin = require('./functions/node_modules/firebase-admin');
} catch (e) {
  try {
    // Then try to load from root directory
    admin = require('firebase-admin');
  } catch (e2) {
    console.error('Failed to load firebase-admin module:', e2.message);
    throw e2;
  }
}

// Check if we already have initialized the app
let firebaseApp;

try {
  // Try to get the default app - if it exists, we're already initialized
  firebaseApp = admin.app();
  console.log('Using existing Firebase Admin app in admin-cjs.js');
} catch (e) {
  // No default app exists, so initialize one
  try {
    // Look for service account credentials
    let serviceAccount;
    try {
      // Try to import credentials
      serviceAccount = require('../serviceAccountKey.json');
    } catch (credError) {
      console.log('No service account found in admin-cjs, using application default credentials');
    }

    // Initialize with service account if available, otherwise use default credentials
    if (serviceAccount) {
      firebaseApp = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
    } else {
      firebaseApp = admin.initializeApp();
    }
    
    console.log('Firebase Admin successfully initialized in admin-cjs.js');
  } catch (initError) {
    console.error('Firebase Admin initialization error:', initError.message);
    
    // If it failed due to duplicate app, try to get the default app again
    if (initError.code === 'app/duplicate-app') {
      try {
        firebaseApp = admin.app();
        console.log('Using existing Firebase Admin app after failed initialization');
      } catch (appError) {
        console.error('Fatal: Unable to get Firebase Admin app', appError);
        throw appError;
      }
    } else {
      throw initError;
    }
  }
}

// Export for CommonJS
module.exports = admin;
