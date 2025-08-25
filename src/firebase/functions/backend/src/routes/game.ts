import {Request, Response, Router} from "express";
import {createLiveGame, endGame, getCurrentGameInfo, getGameInfo, performGameAction, quitGame} from "../services/game";
import {validationAndPermissionMiddleware} from "../middlewares/validation-permission.middleware";
import {GameAction} from "../dtos/game";

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

gameRoutes.post("/end_game",
  validateFirebaseCall,
  async (req: Request, res: Response) => {
    const result = await endGame(req.body.gameId);
    res.send(result);
  }
)

gameRoutes.post("/submit_action",
  validationAndPermissionMiddleware(GameAction),
  async (req: Request, res: Response) => {
    const user = req.user;
    const result = await performGameAction(user, req.body);
    res.send(result);
  }
);

gameRoutes.get("/active",
  async (req: Request, res: Response) => {
    const result = await getCurrentGameInfo(req.user);
    res.send(result);
  }
)

gameRoutes.get("/info/:gameId",
  async (req: Request, res: Response) => {
    const result = await getGameInfo(req.user, req.params.gameId);
    res.send(result);
  }
)

gameRoutes.post("/quit",
  async (req: Request, res: Response) => {
    const result = await quitGame(req.user);
    res.send(result);
  }
)

export default gameRoutes;

