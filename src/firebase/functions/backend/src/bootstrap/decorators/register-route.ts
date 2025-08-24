import {FastifyInstance, FastifyRequest} from "fastify";
import {decoratorRoutes, ROUTE_ARGS_METADATA} from "./index";
import {getMethodPermission} from "./auth-guard";
import {ApiError} from "../errors";
import {plainToInstance} from "class-transformer";
import {validate} from "class-validator";
import qs from "qs";
import {UserType} from "../../types";

export function registerRoutes(server: FastifyInstance, controllerClass: any) {
  const controllerRoutes = decoratorRoutes.get(controllerClass.name) || [];
  console.log('in registerRoutes', controllerRoutes.map(o=>o.path));

  controllerRoutes.forEach((route) => {
    const {method, path, handler, propertyKey} = route;

    const convertedRoute: any = {
      method,
      url: path,
      handler: async (request: FastifyRequest & {user: any, permissions: any}, reply: any) => {
        console.log('in handler', request.originalUrl, request.method);

        const requiredPermission = getMethodPermission(controllerClass.prototype, propertyKey);
        if (requiredPermission === "" && !request.user) {
          throw new ApiError("Unauthorized", 401);
        }
        if (
          request.user?.type !== UserType.SUPER_ADMIN &&
          requiredPermission && !request.permissions?.includes(requiredPermission)
        ) {
          throw new ApiError("Forbidden: Insufficient permissions", 403);
        }

        const args = await generateArgs(controllerClass, propertyKey, request, reply);
        const result = await handler.apply(controllerClass, args);

        if (!reply.sent) {
          reply.send(result);
        }
      },
    };

    server.route(convertedRoute);
  });
}
async function generateArgs(
  controllerClass: any,
  propertyKey: string,
  request: any,
  reply: any
): Promise<any[]> {
  const metadata = Reflect.getOwnMetadata(ROUTE_ARGS_METADATA, controllerClass.prototype, propertyKey) || [];
  const paramTypes = Reflect.getMetadata("design:paramtypes", controllerClass.prototype, propertyKey);
  const parametersLength = paramTypes ? paramTypes.length : 0;

  const args = [];

  for (let i = 0; i < parametersLength; i++) {
    const paramMeta = metadata[i];

    if (paramMeta) {
      const {type, dtoClass} = paramMeta;

      if (type === "body" && dtoClass) {
        const instance = plainToInstance(dtoClass, request.body || {}, {
          enableImplicitConversion: true,
        });
        const errors = await validate(instance, {whitelist: true});

        if (errors.length > 0) {
          const errorMessage = errors.map((err) => Object.values(err.constraints || {}).join(", ")).join("; ");
          throw new ApiError(`Validation failed: ${errorMessage}`, 400);
        }

        args[i] = instance;
      } else if (type === "query" && dtoClass) {
        const instance = plainToInstance(dtoClass, qs.parse(request.query || {}), {enableImplicitConversion: true}) as any;
        const errors = await validate(instance, {whitelist: true});

        if (errors.length > 0) {
          const errorMessage = errors.map((err) => Object.values(err.constraints || {}).join(", ")).join("; ");
          throw new ApiError(`Validation failed: ${errorMessage}`, 400);
        }

        args[i] = instance;
      } else if (type === "params" && dtoClass) {
        const instance = plainToInstance(dtoClass, request.params || {}, {enableImplicitConversion: true});
        const errors = await validate(instance, {whitelist: true});

        if (errors.length > 0) {
          const errorMessage = errors.map((err) => Object.values(err.constraints || {}).join(", ")).join("; ");
          throw new ApiError(`Validation failed: ${errorMessage}`, 400);
        }

        args[i] = instance;
      } else if (type === "files") {
        args[i] = request.files;
      } else if (type === "auth_user") {
        args[i] = request.user;
      } else if (type === "request") {
        args[i] = request;
      } else if (type === "reply") {
        args[i] = reply;
      } else {
        args[i] = undefined;
      }
    } else {
      args[i] = undefined;
    }
  }

  return args;
}
