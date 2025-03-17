const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
const credentials = require("../serviceAccountKey.json");


// Configure Firebase application 
// Securely interact with Firebase service
admin.initializeApp({
    credential: admin.credential.cert(credentials)
});

module.exports = admin;