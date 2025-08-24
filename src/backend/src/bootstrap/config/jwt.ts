import fastifyJwt from "@fastify/jwt";
import {config} from "../../config";

export const registerJwt = (server: any) => {
  server.register(fastifyJwt, { secret: config.jwtSecret });
};
