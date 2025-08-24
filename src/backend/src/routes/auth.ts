import {
  forgotPassword,
  generateToken,
  registerUser,
  resendOTP,
  resetPassword,
  socialLogin,
  validateUser,
  verifyOTP,
  verifyResetToken
} from "../services/users/users";
import {
  ChannelIdentifierDto,
  ForgotPasswordDto,
  LoginUserDto,
  RegisterUserDto,
  ResetPasswordDto,
  SocialLoginUserDto,
  VerifyOtpDto,
  VerifyResetPasswordTokenDto
} from "../dtos/auth";
import { Request, Response, Router } from 'express';
import { validationAndPermissionMiddleware } from "../middlewares/validation-permission.middleware";

export const autoPrefix = "/auth";

const authRoutes = Router();

authRoutes.post(
  "/login",
  validationAndPermissionMiddleware(LoginUserDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const user = await validateUser(data);
    res.json(user);
  }
);

authRoutes.post(
  "/reset-password/verify-token",
  validationAndPermissionMiddleware(VerifyResetPasswordTokenDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const result = await verifyResetToken(data);
    res.json(result);
  }
);

authRoutes.post(
  "/reset-password",
  validationAndPermissionMiddleware(ResetPasswordDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const result = await resetPassword(data);
    res.json(result);
  }
);

authRoutes.post(
  "/forgot-password",
  validationAndPermissionMiddleware(ForgotPasswordDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const result = await forgotPassword(data);
    res.json(result);
  }
);

authRoutes.post(
  "/social-login",
  validationAndPermissionMiddleware(SocialLoginUserDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const result = await socialLogin(data);
    res.json(result);
  }
);

authRoutes.post(
  "/register",
  validationAndPermissionMiddleware(RegisterUserDto, 'user:register'), // Validation + Permission Check
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const user = await registerUser(data);
    const token = generateToken(user);
    res.json({ token });
  }
);

authRoutes.get(
  "/resend-otp",
  validationAndPermissionMiddleware(ChannelIdentifierDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.query;
    const result = await resendOTP(data as any as ChannelIdentifierDto);
    res.json(result);
  }
);

authRoutes.post(
  "/verify-otp",
  validationAndPermissionMiddleware(VerifyOtpDto), // Validation only
  async (req: Request, res: Response) => {
    const data = req.dto; // Access the validated DTO here
    const result = await verifyOTP(data);
    res.json(result);
  }
);

export default authRoutes;
