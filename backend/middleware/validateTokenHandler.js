import { admin } from "../../firebase/firebaseConfig.js";

/**
 * Middleware to validate the Firebase ID token from the request cookies.
 * If the token is valid, it attaches the decoded token to the request object.
 * If the token is missing or invalid, it responds with a 403 status code.
 *
 * @async
 * @function validateToken
 * @param {Object} req - Express request object.
 * @param {Object} req.cookies - Cookies from the request.
 * @param {string} req.cookies.access_token - Firebase ID token from the cookies.
 * @param {Object} res - Express response object.
 * @param {Function} next - Express next middleware function.
 * @returns {void} Sends a 403 response if the token is missing or invalid, otherwise calls the next middleware.
 */
const validateToken = async (req, res, next) => {
  const idToken = req.cookies.access_token;
  if (!idToken) {
      return res.status(403).json({ error: 'No token provided' });
  }
  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken); 
      req.user = decodedToken;
      next();
  } catch (error) {
      console.error('Error verifying token:', error);
      return res.status(403).json({ error: 'Unauthorized' });
  }
};

export default validateToken;
