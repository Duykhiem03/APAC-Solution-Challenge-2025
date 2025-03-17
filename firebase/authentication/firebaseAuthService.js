const admin = require("../firebaseConfig");
const firebaseAuth = admin.auth();

/**
 * Register a new user with email and password
 * @param {string} email - User's email
 * @param {string} password - User's password
 */
const registerUser = async (email, password) => {
    try {
        const userRecord = await firebaseAuth.createUser({
            email, password
        });

        return { success: true, uid: userRecord.uid, message: "User registered successfully" };
    } catch (error) {
        throw new Error(`Error registing user: ${error.message}`);
    }
}

module.exports = { registerUser }