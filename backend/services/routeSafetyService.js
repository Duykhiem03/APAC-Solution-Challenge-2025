/**
 * Calculates and returns the safest routes based on the given routes and hazards.
 * 
 * @param {Array} routes - An array of route objects, each containing legs and steps.
 * @param {Array} hazards - An array of hazard objects, each containing latitude and longitude.
 * @returns {Array} - An array of routes sorted by their safety score in descending order.
 */
const getSafeRoutes = (routes, hazards) => {
    return routes
        .map(route => ({
            route,
            safetyScore: calculateSafetyScore(route, hazards)
        }))
        .sort((a, b) => b.safetyScore - a.safetyScore)
        .map(entry => entry.route)  
}

/**
 * Calculates the safety score of a given route based on nearby hazards.
 * 
 * @param {Object} route - The route object containing legs and steps.
 * @param {Array} hazards - An array of hazard objects with latitude and longitude properties.
 * @returns {number} - The calculated safety score for the route.
 */
const calculateSafetyScore = (route, hazards) => {
    let score = 100;
    route.legs.forEach(leg => {
        leg.steps.forEach(step => {
            if (isNearHazard(step, hazards)) score -= 30;
        });
    });
    return score;
}

/**
 * Determines if a step is near any of the given hazards.
 * 
 * @param {Object} step - A step object containing start_location with latitude and longitude.
 * @param {Array} hazards - An array of hazard objects, each containing latitude and longitude.
 * @returns {boolean} - Returns true if the step is near any hazard, otherwise false.
 */
const isNearHazard = (step, hazards) => {
    // The threshold value represents the distance in degrees of latitude/longitude.
    // A value of 0.001 is approximately equal to 100 meters, which is used to determine
    // if a step is near a hazard.
    const threshold = 0.001;
    return hazards.some(hazard =>
        Math.abs(step.start_location.lat - hazard.latitude) < threshold && Math.abs(step.start_location.lng - hazard.longitude) < threshold
    );
}

export default getSafeRoutes;