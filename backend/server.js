import express from "express";
import dotenv from 'dotenv';
import cookieParser from 'cookie-parser';
dotenv.config({ path: "../.env" });

const app = express();
const port = process.env.BACKEND_PORT || 3000;

app.use(express.json());
app.use(cookieParser());

app.use('/api/users', (await import('./routes/userRoutes.js')).default);
app.use('/api/gps', (await import("./routes/gpsRoutes.js")).default);
app.use('/api/safe_route', (await import("./routes/routeNavigationRoutes.js")).default);
app.use('/api/contacts', (await import("./routes/trustedContactsRoutes.js")).default);

app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});