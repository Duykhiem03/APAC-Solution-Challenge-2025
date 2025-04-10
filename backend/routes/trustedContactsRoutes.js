import express from "express";
import validateToken from "../middleware/validateTokenHandler.js";
import { addTrustedContactController, getTrustedContactsController, updateTrustedContactController, deleteTrustedContactController } from "../controllers/trustedContactsControllers.js";

const router = express.Router();
router.use(validateToken);
router.post("", addTrustedContactController);
router.get("",getTrustedContactsController);
router.put("/:id", updateTrustedContactController);
router.delete("/:id", deleteTrustedContactController);

export default router;