import asyncHandler from "express-async-handler";
import * as GPSDataService from "../../firebase/firestore/gpsDataService.js";

//@desc Save GPS data to database
//@route POST /api/gps
//@access private
const saveGpsDataControllers = async (req, res) => {
    const { userId, latitude, longitude } = req.body;

    if (!userId || !latitude || !longitude) {
        return res.status(400).json({ error: "Missing required fields" });
    }

    const response = await GPSDataService.saveGpsData(userId, latitude, longitude);
    return res.status(response.success ? 200 : 500).json(response);
}


//@desc Get last GPS location for user
//@route GET /api/gps/:userId
//@access private
const getLastGpsLocationControllers = async (req, res) => {
    const { userId } = req.params;
    const response = await GPSDataService.getLastGpsLocation(userId);
    return res.status(response.success ? 200 : 500).json(response);
}


//@desc Get GPS location history for user
//@route GET /api/gps/history/:userId
//@access private
const getGpsHistoryControllers = async (req, res) => {
    const { userId } = req.params;
    const response = await GPSDataService.getGpsHistory(userId);
    return res.status(response.success ? 200 : 500).json(response);
}

export { saveGpsDataControllers, getLastGpsLocationControllers, getGpsHistoryControllers };