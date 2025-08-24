import fastifyStatic from "@fastify/static";
import path from "node:path";

export const registerStatic = (server: any) => {
  server.register(fastifyStatic, {
    root: "/tmp",
    prefix: "/static/",
  });
};
