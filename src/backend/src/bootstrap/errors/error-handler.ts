import { Request, Response, NextFunction } from "express";
import { ApiError } from "./index";
import { MongoServerError } from "mongodb";  // Optional: MongoDB-specific handling

export function errorHandler(
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Log the error (optional)
  console.error(err);

  // Handle custom ApiError
  if (err instanceof ApiError) {
    return res.status(err.statusCode).json({
      status: err.statusCode,
      message: err.message,
    });
  }

  // MongoDB duplicate key error handling
  if (err instanceof MongoServerError && err.code === 11000) {
    const duplicateKey = Object.keys(err.keyValue).join(", ");
    return res.status(400).json({
      status: 400,
      message: `${duplicateKey} already exists`,
      details: duplicateKey,
    });
  }

  // Default fallback error handler
  return res.status(500).json({
    status: 500,
    message: "An unexpected error occurred.",
    details: err.message || err,
  });
}

export function catchAsync(fn: (req: Request, res: Response, next: NextFunction) => Promise<any>) {
  return (req: Request, res: Response, next: NextFunction) => {
    fn(req, res, next).catch(next); // If an error occurs, it will automatically be passed to the error handler
  };
}
