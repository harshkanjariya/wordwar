import { Request, Response, NextFunction } from 'express';
import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';

// Validation and Permission Middleware with optional checks
export function validationAndPermissionMiddleware(dtoClass: any = null, requiredPermission: string | null = null) {
  return async (req: Request, res: Response, next: NextFunction) => {
    // 1. Validation Check (Only if dtoClass is provided)
    if (dtoClass) {
      const instance = plainToInstance(dtoClass, req.body || req.query || {}, { enableImplicitConversion: true });
      const errors = await validate(instance, { whitelist: true });

      if (errors.length > 0) {
        const errorMessage = errors.map((err) => Object.values(err.constraints || {}).join(', ')).join('; ');
        return res.status(400).json({ message: `Validation failed: ${errorMessage}` });
      }

      req.dto = instance;
    }

    // 2. Permission Check (Only if requiredPermission is provided)
    if (requiredPermission) {
      const user = req.user;  // Assuming user info is populated by authentication middleware
      if (!user) {
        return res.status(401).json({ message: 'Unauthorized' });
      }

      if (user.type !== 'SUPER_ADMIN' && !req.permissions?.includes(requiredPermission)) {
        return res.status(403).json({ message: 'Forbidden: Insufficient permissions' });
      }
    }

    // If both checks pass, move to the next middleware/route handler
    next();
  };
}
