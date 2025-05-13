const admin = require('../admin');
const db = admin.firestore();

// Collection references
const stepsCollection = db.collection('steps');
const monthlyStatsCollection = db.collection('monthlyStats');
const achievementsCollection = db.collection('achievements');

/**
 * Step Tracking Data Service - Firebase Firestore
 * Provides methods for tracking steps and managing leaderboards
 */
const stepTrackingDataService = {
    /**
     * Update user's step count for today
     * @param {string} userId - User's ID
     * @param {number} steps - Current step count
     * @param {number} duration - Duration in milliseconds
     * @returns {Promise<void>}
     */
    updateDailySteps: async (userId, steps, duration) => {
        const today = new Date();
        const docId = `${userId}_${today.toISOString().split('T')[0]}`;

        await stepsCollection.doc(docId).set({
            userId,
            date: admin.firestore.Timestamp.fromDate(today),
            steps,
            duration,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
            isPublic: true,
            goal: 10000 // Default daily goal
        }, { merge: true });
    },

    /**
     * Get user's step count for today
     * @param {string} userId - User's ID
     * @returns {Promise<{steps: number, duration: number}>}
     */
    getDailySteps: async (userId) => {
        const today = new Date();
        const docId = `${userId}_${today.toISOString().split('T')[0]}`;
        
        const doc = await stepsCollection.doc(docId).get();
        return doc.exists ? doc.data() : { steps: 0, duration: 0 };
    },

    /**
     * Get weekly progress for a user
     * @param {string} userId - User's ID
     * @returns {Promise<Array>} - Last 7 days of step data
     */
    getWeeklyProgress: async (userId) => {
        const now = new Date();
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        
        const snapshot = await stepsCollection
            .where('userId', '==', userId)
            .where('date', '>=', admin.firestore.Timestamp.fromDate(weekAgo))
            .orderBy('date', 'desc')
            .get();

        return snapshot.docs.map(doc => ({
            dayOfWeek: new Date(doc.data().date.toDate()).getDay(),
            ...doc.data()
        }));
    },

    /**
     * Get leaderboard for today
     * @returns {Promise<Array>} - Top users and their step counts
     */
    getLeaderboard: async () => {
        const today = new Date();
        const todayStr = today.toISOString().split('T')[0];
        
        const snapshot = await stepsCollection
            .where('date', '==', admin.firestore.Timestamp.fromDate(today))
            .where('isPublic', '==', true)
            .orderBy('steps', 'desc')
            .limit(100)
            .get();

        return Promise.all(snapshot.docs.map(async doc => {
            const data = doc.data();
            // Get user profile to include name
            const userDoc = await db.collection('users').doc(data.userId).get();
            const user = userDoc.data() || {};
            
            return {
                userId: data.userId,
                username: user.displayName || 'Anonymous',
                steps: data.steps,
                photoUrl: user.photoUrl
            };
        }));
    }
};

module.exports = stepTrackingDataService;
