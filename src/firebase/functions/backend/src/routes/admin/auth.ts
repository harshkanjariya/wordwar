import { Request, Response, Router } from "express";
import { validationAndPermissionMiddleware } from "../../middlewares/validation-permission.middleware";
import { catchAsync } from "../../bootstrap/errors/error-handler";
import { generateToken, registerAdminUser } from "../../services/users/users";
import { RegisterUserDto } from "../../dtos/auth";

export const autoPrefix = "/admin/auth";

const authRoutes = Router();

// Route for user registration
authRoutes.post(
  "/register",
  validationAndPermissionMiddleware(RegisterUserDto), // Only use validation middleware without permission
  catchAsync(async (req: Request, res: Response) => {
    const data: RegisterUserDto = req.dto; // DTO from request
    const user = await registerAdminUser(data); // Register the user
    const token = generateToken(user); // Generate the token
    res.json({ token }); // Send back the token in the response
  })
);

export default authRoutes;
