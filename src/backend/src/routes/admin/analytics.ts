import {Request, Response, Router} from "express";
import {getAnalytics} from "../../services/analytics";
import {validationAndPermissionMiddleware} from "../../middlewares/validation-permission.middleware";
import {catchAsync} from "../../bootstrap/errors/error-handler";
import {PermissionValue} from "../../types";

export const autoPrefix = "/admin/analytics";

const analyticsRoutes = Router();

// Route for getting analytics
analyticsRoutes.get(
  "",
  validationAndPermissionMiddleware(null, PermissionValue.analytics.get), // Using your existing validationAndPermissionMiddleware
  catchAsync(async (req: Request, res: Response) => {
    const analytics = await getAnalytics();
    res.json(analytics);
  })
);

export default analyticsRoutes;
