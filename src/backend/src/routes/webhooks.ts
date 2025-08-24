import { Router, Request, Response } from "express";

export const autoPrefix = "/webhooks";

const webhooksRoutes = Router();

webhooksRoutes.post(
  "/test",
  async (req: Request, res: Response) => {
    res.status(200).send(); // Responding with status 200
  }
);


export default webhooksRoutes;
