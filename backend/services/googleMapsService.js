import { Client } from "@googlemaps/google-maps-services-js";
import dotenv from 'dotenv';
dotenv.config({ path: '../.env' }); 

// The API key for accessing Google Maps services
const GOOGLE_MAPS_API_KEY = process.env.GOOGLE_MAPS_API_KEY;

// Initialize the Google Maps client
const mapsClient = new Client({});

/**
 * Fetches routes from Google Maps Directions API using the client library.
 * 
 * @param {string} origin - The starting point for calculating directions.
 * @param {string} destination - The ending point for calculating directions.
 * @param {string} [mode="walking"] - The mode of transport to use when calculating directions.
 * @returns {Promise<Array>} - A promise that resolves to an array of route objects.
*/
const getRoutes = async (origin, destination, mode = "walking") => {
    try {
        const response = await mapsClient.directions({
            params: {
                origin: origin,
                destination: destination,
                mode: mode,
                alternatives: true,
                key: GOOGLE_MAPS_API_KEY
            }
        });
        
        // The client library returns data in response.data.data
        return response.data.routes || [];
    } catch (error) {
        console.error('Error fetching directions:', error.response?.data?.error_message || error.message);
        return [];
    }
}

export default getRoutes;