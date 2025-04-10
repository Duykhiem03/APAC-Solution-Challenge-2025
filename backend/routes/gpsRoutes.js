import express from "express";
import validateToken from "../middleware/validateTokenHandler.js";
import { saveGpsDataControllers, getLastGpsLocationControllers, getGpsHistoryControllers } from "../controllers/gpsControllers.js";

const router = express.Router();
router.use(validateToken);
router.post("", saveGpsDataControllers);
// Fix route ordering - more specific routes first
router.get("/history/:userId", getGpsHistoryControllers);
router.get("/:userId", getLastGpsLocationControllers);

export default router;