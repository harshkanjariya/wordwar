import {Router, Request, Response} from "express";
import {createUser, getUserDetails, searchUser, updateUserDetails} from "../../services/users/users";
import {validationAndPermissionMiddleware} from "../../middlewares/validation-permission.middleware";
import {catchAsync} from "../../bootstrap/errors/error-handler";
import {AdminCreateUserDto, AdminUpdateUserDto, SearchUserDto} from "../../dtos/users";
import {PermissionValue} from "../../types";

// Define the prefix for routes
export const autoPrefix = "/admin/users";

// Initialize Express router
const userRoutes = Router();

// Admin User Routes

// Search Users
userRoutes.get(
    "/",
    validationAndPermissionMiddleware(SearchUserDto, PermissionValue.users.get),
    catchAsync(async (req: Request, res: Response) => {
        const searchParams: SearchUserDto = req.query as any;
        const users = await searchUser(searchParams, "admin");
        res.json(users);
    })
);

// Get User Details
userRoutes.get(
    "/:id",
    validationAndPermissionMiddleware(null, PermissionValue.users.get),
    catchAsync(async (req: Request, res: Response) => {
        const {id} = req.params;
        const userDetails = await getUserDetails(id);
        res.json(userDetails);
    })
);

// Update User Details
userRoutes.put(
    "/:id",
    validationAndPermissionMiddleware(AdminUpdateUserDto, PermissionValue.users.update),
    catchAsync(async (req: Request, res: Response) => {
        const {id} = req.params;
        const userData: AdminUpdateUserDto = req.body;
        const updatedUser = await updateUserDetails(id, userData);
        res.json(updatedUser);
    })
);

// Create User
userRoutes.post(
    "/",
    validationAndPermissionMiddleware(AdminCreateUserDto, PermissionValue.users.create),
    catchAsync(async (req: Request, res: Response) => {
        const userData: AdminCreateUserDto = req.body;
        const newUser = await createUser(userData);
        res.status(201).json(newUser);
    })
);

export default userRoutes;
