/**
 * SOS Data Service - Firebase Firestore
 * 
 * This service provides methods to interact with the Firestore database
 * for the SOS functionality in ChildSafe app.
 */

const admin = require('firebase-admin');
const db = admin.firestore();

// Collection references
const sosContactsRef = db.collection('sosContacts');
const sosEventsRef = db.collection('sosEvents');
const geofenceZonesRef = db.collection('geofenceZones');

/**
 * SOS data service methods for backend operations
 */
const sosDataService = {
  /**
   * Configure a user's emergency contacts and SOS settings
   * @param {string} userId - ID of the user
   * @param {Array} contacts - Array of emergency contact objects
   * @param {Object} sosSettings - User's SOS settings
   * @returns {Promise<void>}
   */
  configureSosContacts: async (userId, contacts = [], sosSettings = {}) => {
    await sosContactsRef.doc(userId).set({
      contacts,
      sosSettings
    }, { merge: true });
  },
  
  /**
   * Add a single emergency contact for a user
   * @param {string} userId - ID of the user
   * @param {Object} contact - Emergency contact object
   * @returns {Promise<void>}
   */
  addEmergencyContact: async (userId, contact) => {
    const userDoc = await sosContactsRef.doc(userId).get();
    
    if (userDoc.exists) {
      // Update existing document
      await sosContactsRef.doc(userId).update({
        contacts: admin.firestore.FieldValue.arrayUnion(contact)
      });
    } else {
      // Create new document
      await sosContactsRef.doc(userId).set({
        contacts: [contact],
        sosSettings: {
          automaticSosDetection: true,
          confirmationDelaySeconds: 10,
          includeLocation: true,
          includeCameraSnapshot: false,
          includeAudioRecording: false
        }
      });
    }
  },
  
  /**
   * Remove an emergency contact
   * @param {string} userId - ID of the user
   * @param {string} contactId - ID of the contact to remove
   * @returns {Promise<void>}
   */
  removeEmergencyContact: async (userId, contactId) => {
    const userDoc = await sosContactsRef.doc(userId).get();
    
    if (!userDoc.exists) {
      throw new Error('User SOS contacts not configured');
    }
    
    const userData = userDoc.data();
    const updatedContacts = userData.contacts.filter(
      contact => contact.contactId !== contactId
    );
    
    await sosContactsRef.doc(userId).update({
      contacts: updatedContacts
    });
  },
  
  /**
   * Update SOS settings for a user
   * @param {string} userId - ID of the user
   * @param {Object} sosSettings - Updated SOS settings
   * @returns {Promise<void>}
   */
  updateSosSettings: async (userId, sosSettings) => {
    await sosContactsRef.doc(userId).set({
      sosSettings
    }, { merge: true });
  },
  
  /**
   * Trigger an SOS event
   * @param {string} userId - ID of the user triggering SOS
   * @param {Object} location - User's current location
   * @param {string} triggerMethod - How the SOS was triggered
   * @param {Object} contextData - Additional contextual information
   * @returns {Promise<string>} - ID of the created SOS event
   */
  triggerSosEvent: async (userId, location, triggerMethod = 'manual', contextData = {}) => {
    // Create the SOS event
    const sosEvent = {
      userId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      location,
      triggerMethod,
      status: 'active',
      resolvedAt: null,
      resolvedBy: null,
      notificationsSent: [],
      contextData
    };
    
    const sosEventRef = await sosEventsRef.add(sosEvent);
    
    // Get the user's emergency contacts to notify
    const userContactsDoc = await sosContactsRef.doc(userId).get();
    
    if (!userContactsDoc.exists) {
      // No contacts configured, just return the event ID
      return sosEventRef.id;
    }
    
    const userData = userContactsDoc.data();
    const contacts = userData.contacts || [];
    
    // Track notifications sent in a batch
    const batch = db.batch();
    const notificationsSent = [];
    
    // Record that notifications are being sent to each contact
    for (const contact of contacts) {
      notificationsSent.push({
        contactId: contact.contactId,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        methods: getNotificationMethods(contact.notificationPreferences),
        acknowledgedAt: null
      });
    }
    
    // Update the SOS event with notification records
    batch.update(sosEventRef, {
      notificationsSent
    });
    
    await batch.commit();
    
    // Return the SOS event ID
    return sosEventRef.id;
  },
  
  /**
   * Acknowledge an SOS event notification
   * @param {string} sosEventId - ID of the SOS event
   * @param {string} contactId - ID of the contact acknowledging the notification
   * @returns {Promise<void>}
   */
  acknowledgeNotification: async (sosEventId, contactId) => {
    const sosEventDoc = await sosEventsRef.doc(sosEventId).get();
    
    if (!sosEventDoc.exists) {
      throw new Error('SOS event not found');
    }
    
    const sosEvent = sosEventDoc.data();
    
    if (sosEvent.status !== 'active') {
      // Event is already resolved, no need to acknowledge
      return;
    }
    
    // Update the notification record for this contact
    const updatedNotifications = sosEvent.notificationsSent.map(notification => {
      if (notification.contactId === contactId) {
        return {
          ...notification,
          acknowledgedAt: admin.firestore.FieldValue.serverTimestamp()
        };
      }
      return notification;
    });
    
    // Update SOS event status to responded if this is the first acknowledgment
    const hasAcknowledged = sosEvent.notificationsSent.some(
      notification => notification.acknowledgedAt !== null
    );
    
    if (!hasAcknowledged) {
      await sosEventsRef.doc(sosEventId).update({
        notificationsSent: updatedNotifications,
        status: 'responded'
      });
    } else {
      await sosEventsRef.doc(sosEventId).update({
        notificationsSent: updatedNotifications
      });
    }
  },
  
  /**
   * Resolve an SOS event
   * @param {string} sosEventId - ID of the SOS event
   * @param {string} resolvedBy - ID of the user resolving the event
   * @param {boolean} isFalseAlarm - Whether this was a false alarm
   * @returns {Promise<void>}
   */
  resolveSosEvent: async (sosEventId, resolvedBy, isFalseAlarm = false) => {
    await sosEventsRef.doc(sosEventId).update({
      status: isFalseAlarm ? 'false_alarm' : 'resolved',
      resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
      resolvedBy
    });
  },
  
  /**
   * Create a geofence zone
   * @param {string} userId - ID of the user
   * @param {Object} geofenceData - Geofence zone data
   * @returns {Promise<string>} - ID of the created geofence
   */
  createGeofenceZone: async (userId, geofenceData) => {
    const geofence = {
      userId,
      ...geofenceData
    };
    
    const geofenceRef = await geofenceZonesRef.add(geofence);
    return geofenceRef.id;
  },
  
  /**
   * Update a geofence zone
   * @param {string} geofenceId - ID of the geofence
   * @param {Object} geofenceData - Updated geofence data
   * @returns {Promise<void>}
   */
  updateGeofenceZone: async (geofenceId, geofenceData) => {
    await geofenceZonesRef.doc(geofenceId).update(geofenceData);
  },
  
  /**
   * Delete a geofence zone
   * @param {string} geofenceId - ID of the geofence
   * @returns {Promise<void>}
   */
  deleteGeofenceZone: async (geofenceId) => {
    await geofenceZonesRef.doc(geofenceId).delete();
  }
};

/**
 * Helper function to determine notification methods based on preferences
 * @param {Object} preferences - Notification preferences
 * @returns {string[]} - Array of notification method names
 */
function getNotificationMethods(preferences) {
  const methods = [];
  
  if (preferences.receivePushNotifications) {
    methods.push('push');
  }
  
  if (preferences.receiveSMS) {
    methods.push('sms');
  }
  
  if (preferences.receiveEmails) {
    methods.push('email');
  }
  
  return methods;
}

module.exports = sosDataService;

// IMPLEMENTATION NOTES:
// 1. This service should be integrated with:
//    - FCM for push notifications
//    - A SMS service (Twilio, etc.) for text messages
//    - An email service for email notifications
// 2. For the full implementation, add additional methods for:
//    - Fetching active SOS events for a user or contact
//    - Listing a user's SOS history
//    - Managing geofence zones for a user
// 3. The SOS feature should have high-availability and error handling
//    to ensure critical safety features work reliably
// 4. Consider implementing an escalation system if emergency contacts
//    don't acknowledge within a certain time period