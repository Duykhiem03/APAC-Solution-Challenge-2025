import { admin } from "../firebaseConfig.js";
const db = admin.firestore();

class GPSDataService {
    /**
     * Save GPS data for a user
     * @param {string} userId - User's ID
     * @param {number} latitude - Latitude of the GPS location
     * @param {number} longitude - Longitude of the GPS location
     */
    static async saveGpsData(userId, latitude, longitude) {
        try {
            const gpsData = { userId, latitude, longitude, timestamp: new Date() };
            await db.collection("gps_locations").add(gpsData);
            return { success: true, message: "GPS data stored successfully" };
        } catch (error) {
            console.error("Error saving GPS data", error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Get last GPS location for a user
     * @param {string} userId - User's ID
     */
    static async getLastGpsLocation(userId) {
        try {
            const snapshot = await db.collection("gps_locations")
                .where("userId", "==", userId)
                .orderBy("timestamp", "desc")
                .limit(1)
                .get();
            if (snapshot.empty) {
                return { success: false, message: "No GPS data found." };
            }
            return { success: true, data: snapshot.docs[0].data() };
        } catch(error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Get GPS history for a user
     * @param {string} userId - User's ID
     */
    static async getGpsHistory(userId) {
        try {
            const snapshot = await db.collection("gps_locations")
                .where("userId", "==", userId)
                .orderBy("timestamp", "desc")
                .get();
            if (snapshot.empty) {
                return { success: false, message: "No GPS data found." };
            }
            const history = snapshot.docs.map(doc => doc.data());
            return { success: true, data: history };
        } catch(error) {
            return { success: false, error: error.message };
        }
    }
}

export const { saveGpsData, getLastGpsLocation, getGpsHistory } = GPSDataService;