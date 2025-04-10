import express from "express";
import validateToken from "../middleware/validateTokenHandler.js";
import { saveHazardReportControllers, getSafeRoute } from "../controllers/routeNavigationControllers.js";

const router = express.Router()
router.use(validateToken);
router.get("", getSafeRoute);
router.post("/hazard", saveHazardReportControllers);

export default router;