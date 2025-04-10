import asyncHandler from "express-async-handler";
import { addTrustedContact, getTrustedContacts, updateTrustedContact, removeTrustedContact } from "../../firebase/firestore/trustedContactsDataService.js";

//@desc Add new Trusted Contact
//@route POST /api/contacts
//@access private
const addTrustedContactController = asyncHandler(async (req, res) => {
    const { userId, contactName, contactNum, relationship } = req.body;
    if (!userId || !contactName || !contactNum || !relationship) {
        return res.status(400).json({ error: "Missing required fields" });
    }
    const contactInfo = { contactName, contactNum, relationship };
    try {
        const response = await addTrustedContact(userId, contactInfo);
        if (response && response.success) {
            return res.status(200).json(response);
        } else {
            return res.status(500).json({ error: response?.error || "Failed to add trusted contact" });
        }
    } catch (error) {
        return res.status(500).json({ error: error.message || "Internal Server Error" });
    }
});

//@desc Get all Trusted Contacts
//@route GET /api/contacts
//@access private
const getTrustedContactsController = asyncHandler(async (req, res) => {
    const { userId } = req.query;
    if (!userId) {
        return res.status(400).json({ error: "Missing required fields" });
    }
    const contactsList = await getTrustedContacts(userId);
    return res.status(200).json(contactsList);
})

//@desc Update Trusted Contact
//@route PUT /api/contacts/:contactId
//@access private
const updateTrustedContactController = asyncHandler(async (req, res) => {
    const { userId, contactName, contactNum, relationship } = req.body;
    if ( !userId || !contactName || !contactNum || !relationship) {
        return res.status(400).json({ error: "Missing required fields" });
    }
    const updatedData = {contactName, contactNum, relationship}
    const response = await updateTrustedContact(userId, req.params.contactId, updatedData);
    if (response.success) {
        return res.status(200).json({ message: response.message, contact: response.contact });
    } else {
        return res.status(500).json(response.error);
    }
});

//@desc Delete Trusted Contact
//@route DELETE /api/contacts/:contactId
//@access private
const deleteTrustedContactController = asyncHandler(async (req, res) => {
    const { userId } = req.body;
    if (!userId) {
        return res.status(400).json({ error: "Missing required fields" });
    }
    const response = await removeTrustedContact(userId, req.params.contactId);
    if (response.success) {
        return res.status(200).json({ message: response.message, contact: response.contact });
    } else {
        return res.status(500).json(response.error);
    }
})

export { addTrustedContactController, getTrustedContactsController, updateTrustedContactController, deleteTrustedContactController };