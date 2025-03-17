const admin = require("../firebaseConfig");
const db = admin.firestore();


/**
 * Save hazard report data for a user
 * @param {string} userId - User's ID
 * @param {number} latitude - Latitude of the GPS location
 * @param {number} longitude - Longitude of the GPS location
 * @param {string} type - Type of the hazard
 * @param {string} description - Description of the hazard
 * @param {string} risk_level - Risk level of the hazard
 */
const saveHazardReport = async (userId, latitude, longitude, type, description, risk_level) => {
    try {
        const hazardData = { userId, latitude, longitude, type, description, risk_level, createdAt: new Date() };
        await db.collection("hazard_reports").add(hazardData);
        return { success: true, message: "Hazard report stored successfully" };
    } catch(error) {
        console.error("Error saving hazard reports", error);
        return { success: false, error: error.message };
    }
}

/**
 * Fetch all user-reported hazards
 * @returns {Promise<Array>} - A promise that resolves to an array of hazard reports
 */
const getHazardReports = async () => {
    const snapshot = await db.collection("hazard_reports").get();

    return snapshot.docs.map(doc => doc.data());

}


module.exports = { saveHazardReport, getHazardReports }