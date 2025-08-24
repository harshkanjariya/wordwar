import {Request, Response, Router} from "express";
import {createLiveGame} from "../services/game";

export const autoPrefix = "/game";

const gameRoutes = Router();

function validateFirebaseCall(req: Request, res: Response, next: Function) {
  const incomingKey = req.get("x-api-key");

  if (incomingKey != "5cdf2476-491a-4cc5-8ff2-ecd8767a7e23") {
    return res.status(403).send("Forbidden: Invalid API key.");
  }
  next();
}

gameRoutes.post("/start_game",
  validateFirebaseCall,
  async (req: Request, res: Response) => {
    const game = await createLiveGame(req.body);
    // @ts-ignore
    delete game.cellData;
    res.send(game);
  }
);

export default gameRoutes;

