/**
 * SOS Feature - Firestore Database Schema
 * 
 * This file defines the Firestore collections and document structures 
 * for the emergency SOS functionality in ChildSafe app.
 *
 * Last updated: May 7, 2025
 */

const admin = require('../admin');
const { FieldValue } = admin.firestore;

/**
 * SOS Schema Definitions
 * Use these as templates when creating documents in Firestore
 */
const sosSchema = {
  /**
   * SOS Contacts schema
   * Collection: sosContacts
   * Description: Stores emergency contacts for each user
   */
  sosContacts: {
    contacts: [], // Array of emergency contact objects
    sosSettings: {
      automaticSosDetection: true, // Whether to enable automatic SOS triggering
      confirmationDelaySeconds: 10, // How long to wait for cancellation before sending alerts
      includeLocation: true, // Whether to include location data in SOS alerts
      includeCameraSnapshot: false, // Whether to include a camera snapshot in SOS alerts
      includeAudioRecording: false // Whether to include audio recording in SOS alerts
    }
  },

  /**
   * Emergency contact schema (embedded in sosContacts)
   */
  emergencyContact: {
    contactId: "", // User ID of the emergency contact
    name: "",
    phone: "",
    relationship: "", // Relationship to the child (Parent, Guardian, Sibling, etc.)
    notificationPreferences: {
      receivePushNotifications: true,
      receiveSMS: true,
      receiveEmails: false,
      notificationPriority: NotificationPriority.HIGH // Options: high, medium, low
    }
  },

  /**
   * SOS Event schema
   * Collection: sosEvents
   * Description: Records of SOS events triggered by users
   */
  sosEvent: {
    userId: "", // User who triggered the SOS
    timestamp: FieldValue.serverTimestamp(), // When the SOS was triggered
    location: {
      latitude: 0,
      longitude: 0,
      accuracy: 0, // Accuracy in meters
      address: "" // Reverse geocoded address if available
    },
    triggerMethod: TriggerMethod.MANUAL, // How the SOS was triggered
    status: SosStatus.ACTIVE, // Status of the SOS event
    resolvedAt: null, // Timestamp when the SOS was resolved
    resolvedBy: null, // User ID of who resolved the SOS
    notificationsSent: [], // Array of notification records
    contextData: {
      batteryLevel: 0, // Device battery percentage
      moving: false, // Whether the user was moving when SOS triggered
      speed: 0, // Speed in km/h if available
      recentLocations: [], // Recent location history
      aiDetectedContext: "" // AI-detected context if available
    }
  },

  /**
   * Notification record schema (embedded in sosEvent)
   */
  notificationRecord: {
    contactId: "", // ID of the contact notified
    sentAt: FieldValue.serverTimestamp(),
    methods: [], // How the contact was notified: ["push", "sms", "email"]
    acknowledgedAt: null // When the contact acknowledged the alert
  },

  /**
   * Recent location schema (embedded in sosEvent contextData)
   */
  recentLocation: {
    latitude: 0,
    longitude: 0,
    timestamp: FieldValue.serverTimestamp()
  },

  /**
   * Geofence zone schema
   * Collection: geofenceZones
   * Description: Safe/unsafe zones for automatic SOS triggering
   */
  geofenceZone: {
    userId: "", // User associated with this geofence
    name: "",
    type: GeofenceType.SAFE, // "safe" or "unsafe"
    shape: GeofenceShape.CIRCLE, // "circle" or "polygon"
    center: {
      latitude: 0,
      longitude: 0
    },
    radius: 0, // Radius in meters (for circle)
    points: [], // Array of points (for polygon)
    schedule: {
      enabled: true,
      days: [1, 2, 3, 4, 5], // 0 is Sunday, 1 is Monday, etc.
      startTime: "08:00", // 24-hour format
      endTime: "15:00"
    },
    actions: {
      notifyContacts: true,
      triggerSos: false, // Whether to trigger a full SOS
      sendWarning: true // Send warning before triggering SOS
    }
  },

  /**
   * Geofence point schema (embedded in geofenceZone)
   */
  geoPoint: {
    latitude: 0,
    longitude: 0
  }
};

/**
 * Enum for notification priorities
 */
const NotificationPriority = {
  HIGH: "high",
  MEDIUM: "medium",
  LOW: "low"
};

/**
 * Enum for SOS trigger methods
 */
const TriggerMethod = {
  MANUAL: "manual",
  GEOFENCE: "geofence",
  ACTIVITY: "activity",
  AI_DETECTION: "aiDetection"
};

/**
 * Enum for SOS event statuses
 */
const SosStatus = {
  ACTIVE: "active",
  RESPONDED: "responded",
  RESOLVED: "resolved",
  FALSE_ALARM: "false_alarm"
};

/**
 * Enum for geofence types
 */
const GeofenceType = {
  SAFE: "safe",
  UNSAFE: "unsafe"
};

/**
 * Enum for geofence shapes
 */
const GeofenceShape = {
  CIRCLE: "circle",
  POLYGON: "polygon"
};

/**
 * Collection names
 */
const Collections = {
  SOS_CONTACTS: "sosContacts",
  SOS_EVENTS: "sosEvents",
  GEOFENCE_ZONES: "geofenceZones"
};

/**
 * Creates empty SOS contacts configuration for a user
 * @returns {object} SOS contacts configuration
 */
function createSosContactsConfig() {
  return {
    ...sosSchema.sosContacts
  };
}

/**
 * Creates an emergency contact object
 * @param {string} contactId - User ID of the emergency contact
 * @param {object} contactInfo - Contact information
 * @returns {object} Emergency contact object
 */
function createEmergencyContact(contactId, contactInfo = {}) {
  const {
    name = "",
    phone = "",
    relationship = "",
    notificationPreferences = {}
  } = contactInfo;

  return {
    ...sosSchema.emergencyContact,
    contactId,
    name,
    phone,
    relationship,
    notificationPreferences: {
      ...sosSchema.emergencyContact.notificationPreferences,
      ...notificationPreferences
    }
  };
}

/**
 * Creates an SOS event object
 * @param {string} userId - ID of the user triggering SOS
 * @param {object} location - User's location
 * @param {string} triggerMethod - How SOS was triggered
 * @param {object} contextData - Additional context data
 * @returns {object} SOS event object ready for Firestore
 */
function createSosEvent(userId, location, triggerMethod = TriggerMethod.MANUAL, contextData = {}) {
  return {
    ...sosSchema.sosEvent,
    userId,
    location: {
      ...sosSchema.sosEvent.location,
      ...location
    },
    triggerMethod,
    contextData: {
      ...sosSchema.sosEvent.contextData,
      ...contextData
    }
  };
}

/**
 * Creates a notification record for SOS events
 * @param {string} contactId - ID of the contact being notified
 * @param {string[]} methods - Notification methods used
 * @returns {object} Notification record object
 */
function createNotificationRecord(contactId, methods = ["push"]) {
  return {
    ...sosSchema.notificationRecord,
    contactId,
    methods
  };
}

/**
 * Creates a geofence zone object
 * @param {string} userId - User ID
 * @param {object} zoneInfo - Zone information
 * @returns {object} Geofence zone object ready for Firestore
 */
function createGeofenceZone(userId, zoneInfo = {}) {
  const {
    name = "",
    type = GeofenceType.SAFE,
    shape = GeofenceShape.CIRCLE,
    center = null,
    radius = 0,
    points = [],
    schedule = null,
    actions = null
  } = zoneInfo;

  const zone = {
    ...sosSchema.geofenceZone,
    userId,
    name,
    type,
    shape
  };

  // Set shape-specific properties
  if (shape === GeofenceShape.CIRCLE && center) {
    zone.center = {
      latitude: center.latitude || 0,
      longitude: center.longitude || 0
    };
    zone.radius = radius;
  } else if (shape === GeofenceShape.POLYGON && points.length > 2) {
    zone.points = points.map(point => ({
      latitude: point.latitude || 0,
      longitude: point.longitude || 0
    }));
  }

  // Override schedule if provided
  if (schedule) {
    zone.schedule = {
      ...zone.schedule,
      ...schedule
    };
  }

  // Override actions if provided
  if (actions) {
    zone.actions = {
      ...zone.actions,
      ...actions
    };
  }

  return zone;
}

module.exports = {
  sosSchema,
  NotificationPriority,
  TriggerMethod,
  SosStatus,
  GeofenceType,
  GeofenceShape,
  Collections,
  createSosContactsConfig,
  createEmergencyContact,
  createSosEvent,
  createNotificationRecord,
  createGeofenceZone
};

// IMPLEMENTATION NOTES:
// 1. Create indexes for:
//    - sosEvents: userId + timestamp (for fetching user's SOS history)
//    - sosEvents: status + timestamp (for monitoring active SOS events)
//
// 2. Security Rules:
//    - Users should only read/write their own SOS contacts and events
//    - Emergency contacts should be able to read SOS events they're associated with
//
// 3. Set up Firebase Cloud Functions for:
//    - Sending multi-channel notifications when SOS is triggered
//    - Escalating notifications if contacts don't respond
//    - Updating SOS event status based on contact responses