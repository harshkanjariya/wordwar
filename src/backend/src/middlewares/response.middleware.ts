import { Request, Response, NextFunction } from "express";

// Express middleware to replace Fastify's onSend hook
export const responseMiddleware = async (req: Request, res: Response, next: NextFunction) => {
  // Store the original `send` method to preserve functionality
  const originalSend = res.json;

  // Override the `json` method
  res.json = function (payload: any) {
    try {
      // If the status code is in the 2xx range, modify the payload
      if (res.statusCode >= 200 && res.statusCode < 300) {
        // Parse the payload, assuming it's a JSON string
        const responseBody = {
          status: res.statusCode,
          data: payload,
        };
        // Call the original send method with the modified payload
        return originalSend.call(this, responseBody);
      }
      // If it's not in the 2xx range, send the payload as-is
      return originalSend.call(this, payload);
    } catch (error) {
      // If an error occurs, just send the payload without modification
      return originalSend.call(this, payload);
    }
  };

  // Call the next middleware or route handler
  next();
};
