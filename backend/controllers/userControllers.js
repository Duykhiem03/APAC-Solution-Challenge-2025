import asyncHandler from "express-async-handler";
import { registerUser, loginUser, logoutUser } from "../../firebase/authentication/firebaseAuthService.js";

//@desc Register an user
//@route POST /api/users/register
//@access public
const registerUserController = asyncHandler(async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        res.status(400);
        throw new Error("All fields are mandatory!");
    }
    try {
        const response = await registerUser(email, password);
        res.status(201).json(response);
    } catch(error) {
        console.log(error);
        res.status(400).json({ error: error.message });
    }
});

//@desc Login a user
//@route POST /api/users/login
//@access public
const loginUserController = asyncHandler(async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        res.status(400);
        throw new Error("All fields are mandatory!");
    }
    try {
        const response = await loginUser(email, password, res);
        res.status(200).json(response);
    } catch(error) {
        console.log(error);
        res.status(500).json({ error: error.message });
    }
});

//@desc Logout a user
//@route POST /api/users/logout
//@access public
const logoutUserController = asyncHandler(async (req, res) => {
    try {
        const response = await logoutUser(res);
        res.status(200).json(response);
    } catch(error) {
        console.log(error);
        res.status(500).json({ error: error.message });
    }
});

//@desc Get current user info
//@route GET /api/users/current
//@access private
const getUser = asyncHandler(async (req, res) => {
    return res.status(200).json({ message: 'Access granted to profile data', user: req.user });
});

export { registerUserController, loginUserController, logoutUserController, getUser };
