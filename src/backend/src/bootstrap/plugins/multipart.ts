import Multipart from "@fastify/multipart";
import {FastifyInstance} from "fastify";

export const registerMultipart = (server: FastifyInstance) => {
  server.register(Multipart, {
    attachFieldsToBody: false,
    limits: { fileSize: 10 * 1024 * 1024 },
  });
};
