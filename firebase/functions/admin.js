/**
 * Firebase initialization file for functions
 * Immediate initialization without deferral
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin immediately
try {
  // Try to get the default app first - if it exists, we're already initialized
  try {
    admin.app();
    console.log('Using existing Firebase Admin app in functions/admin.js');
  } catch (e) {
    // No default app exists, so initialize one
    admin.initializeApp();
    console.log('Firebase Admin successfully initialized in functions/admin.js');
  }
} catch (error) {
  console.error('Firebase Admin initialization error:', error);
  throw error;
}

// Export the initialized admin instance
module.exports = admin;
