import asyncHandler from "express-async-handler";
import { saveHazardReport, getHazardReports } from "../../firebase/firestore/routeNavigationDataService.js";
import getRoutes from "../services/googleMapsService.js";
import getSafeRoutes from "../services/routeSafetyService.js";

//@desc Save GPS data to database
//@route POST /api/safe_route/hazard
//@access private
const saveHazardReportControllers = async (req, res) => {
    const { userId, latitude, longitude, type, description, risk_level } = req.body;

    if (!userId || !latitude || !longitude || !type || !description || !risk_level) {
        return res.status(400).json({ error: "Missing required fields" });
    }

    const response = await saveHazardReport(userId, latitude, longitude, type, description, risk_level);
    return res.status(response.success ? 200 : 500).json(response);
}


//@desc Get a safest route
//@route GET /api/safe_route
//@access private
const getSafeRoute = async (req, res) => {
    try {
        const { origin, destination, mode = "walking" } = req.query;

        if (!origin || !destination) {
            return res.status(400).json({ error: "Origin and destination are required" });
        }

        // Fetch route options from Google Direction API
        const routes = await getRoutes(origin, destination, mode);

        // Fetch user-reported hazard data from Firestore
        const hazardData = await getHazardReports();

        const safeRoutes = await getSafeRoutes(routes, hazardData);

        if (safeRoutes.length === 0) {
            return res.status(404).json({ message: "No safe routes found." })
        }

        return res.status(200).json({ safestRoute: safeRoutes[0], alternativeRoutes: safeRoutes.slice(1) });
    } catch(error) {
        res.status(500).json({ error: error.message });
    }
}


export { saveHazardReportControllers, getSafeRoute };