import express from "express";
import { registerUserController, getUser, loginUserController, logoutUserController } from "../controllers/userControllers.js";
import validateToken from "../middleware/validateTokenHandler.js";

const router = express.Router();

router.post("/register", registerUserController);
router.post("/login", loginUserController);
router.post("/logout", logoutUserController);
router.get("/current", validateToken, getUser);

export default router;