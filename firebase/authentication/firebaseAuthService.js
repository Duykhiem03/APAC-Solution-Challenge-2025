import {
  getAuth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  sendEmailVerification,
} from "firebase/auth";
import { app } from "../firebaseConfig.js";

// Get auth with the initialized app instance explicitly
const auth = getAuth(app);

/**
 * Register a new user with email and password
 * @param {string} email - User's email
 * @param {string} password - User's password
 */
const registerUser = async (email, password) => {
  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    try {
      await sendEmailVerification(auth.currentUser);
      return { message: "Verification email sent! User created successfully!", user: userCredential.user };
    } catch (error) {
      console.error(error);
      // Still return success for registration even if email verification fails
      return { message: "User created successfully but email verification failed", user: userCredential.user };
    }
  } catch (error) {
    const errorMessage = error.message || "An error occurred while registering user";
    throw new Error(errorMessage);
  }
};

/**
 * Logs in a user using their email and password, and sets an HTTP-only cookie with the access token.
 *
 * @async
 * @function loginUser
 * @param {string} email - The email address of the user.
 * @param {string} password - The password of the user.
 * @param {Object} res - The response object to set the HTTP-only cookie.
 * @returns {Promise<Object>} A promise that resolves to an object containing a success message and user details.
 * @throws {Error} Throws an error if login fails or an internal server error occurs.
 */
const loginUser = async (email, password, res) => {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const idToken = userCredential._tokenResponse.idToken;
    if (idToken) {
      res.cookie('access_token', idToken, {
        httpOnly: true
      });
      return {
        message: "User logged in successfully",
        user: {
          email: userCredential.user.email,
          uid: userCredential.user.uid,
          emailVerified: userCredential.user.emailVerified
        }
      };
    } else {
      throw new Error("Internal Server Error");
    }
  } catch (error) {
    console.error(error);
    const errorMessage = error.message || "An error occurred while logging in";
    throw new Error(errorMessage);
  }
};


/**
 * Logs out the currently authenticated user and clears the HTTP-only cookie.
 *
 * @async
 * @function logoutUser
 * @param {Object} res - The response object to clear the HTTP-only cookie.
 * @returns {Promise<Object>} A promise that resolves to an object containing a success message.
 * @throws {Error} Throws an error if logout fails.
 */
const logoutUser = async (res) => {
  try {
    await signOut(auth);
    res.clearCookie('access_token');
    return { message: "User logged out successfully" };
  } catch (error) {
    console.error(error);
    throw new Error(error.message || "Error during logout");
  }
};

export { registerUser, loginUser, logoutUser };
