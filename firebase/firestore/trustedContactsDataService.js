import { admin } from "../firebaseConfig.js";
const db = admin.firestore();

/**
 * Adds a trusted contact to the database for a specific user.
 * @param {string} userId - The ID of the user to whom the contact belongs.
 * @param {Object} contactInfo - The contact information to be added.
 * @param {string} contactInfo.name - The name of the contact.
 * @param {string} contactInfo.phone - The phone number of the contact.
 * @param {string} contactInfo.relationship - The relationship between user with contact person.
 * @returns {Promise<Object>} - A promise that resolves to an object containing success status, message, and the added contact.
 */
const addTrustedContact = async (userId, contactInfo) => {
    try {
        const userRef = db.collection("trusted_contacts").doc(userId);
        const contactId = db.collection("trusted_contacts").doc().id;
        const contact = {id: contactId, ...contactInfo};
        await userRef.set({ contacts: admin.firestore.FieldValue.arrayUnion(contact) }, { merge: true });
        return { success: true, message: "Added successfully", contact: contact };
    } catch (error) {
        return { success: false, error: `Failed to add trusted contact: ${error.message}` };
    }
}

/**
 * Retrieves all trusted contacts for a specific user.
 * 
 * @param {string} userId - The ID of the user whose contacts are being retrieved.
 * @returns {Promise<Object>} - A promise that resolves to an object containing the list of contacts.
 */
const getTrustedContacts = async (userId) => {
    const doc = await db.collection("trusted_contacts").doc(userId).get();
    if (!doc.exists) {
        return { contacts: [] };
    }
    return { contacts: doc.data().contacts };
}

/**
 * Removes a trusted contact from the database for a specific user.
 * 
 * @param {string} userId - The ID of the user whose contact is being removed.
 * @param {string} contactId - The ID of the contact to remove.
 * @returns {Promise<Object>} - A promise that resolves to an object containing the success status, message, and removed contact.
 */
const removeTrustedContact = async (userId, contactId) => {
    try {
        const userRef = db.collection("trusted_contacts").doc(userId);
        const doc = await userRef.get();
        if (!doc.exists) {
            throw new Error("No contacts found for this user");
        }
        let contacts = doc.data().contacts;
        const removedContact = contacts.find(contact => contact.id === contactId);
        if (!removedContact) {
            throw new Error("No contact found");
        }
        await userRef.update({
            contacts: admin.firestore.FieldValue.arrayRemove(removedContact)
        })
        return { success: true, message: "Contact deleted successfully", contact: removedContact };
    } catch(error) {
        return { success: false, error: error.message };
    }
}

/**
 * Updates a trusted contact with new information.
 * 
 * @param {string} userId - The ID of the user whose contact is being updated.
 * @param {string} contactId - The ID of the contact to update.
 * @param {Object} updatedContact - The updated contact information.
 * @returns {Promise<Object>} - A promise that resolves to an object containing the success status, message, and updated contact.
 */
const updateTrustedContact = async (userId, contactId, updatedContactData) => {
    try {
        const userRef = db.collection("trusted_contacts").doc(userId);
        const doc = await userRef.get();
        if (!doc.exists) {
            throw new Error("No contacts found for this user");
        }
        const contacts = doc.data().contacts || [];
        const contact = contacts.find(contact => contact.id === contactId);
        if (!contact) {
            throw new Error("No contact found");
        }
        const updatedContact = {id: contactId, ...updatedContactData}
        // Remove the old contact and add the updated one
        await userRef.update({
            contacts: admin.firestore.FieldValue.arrayRemove(contact)
        });
        await userRef.update({
            contacts: admin.firestore.FieldValue.arrayUnion(updatedContact)
        });
        
        return { success: true, message: "Updated successfully", contact: updatedContact };
    } catch(error) {
        return { success: false, error: error.message };
    }
}

export { addTrustedContact, getTrustedContacts, updateTrustedContact, removeTrustedContact };