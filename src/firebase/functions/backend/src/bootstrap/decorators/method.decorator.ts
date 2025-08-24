import {decoratorRoutes} from "./index";

export function Route(method: string, path: string, dtoClass?: any) {
  return function (target: any, propertyKey: string) {
    if (!decoratorRoutes.has(target.constructor.name)) {
      decoratorRoutes.set(target.constructor.name, []);
    }

    (decoratorRoutes.get(target.constructor.name) as any).push({
      method,
      path,
      handler: target[propertyKey],
      dtoClass,
      propertyKey,
    });
  };
}

export function Get(path: string, dtoClass?: any) {
  return Route("GET", path, dtoClass);
}

export function Post(path: string, dtoClass?: any) {
  return Route("POST", path, dtoClass);
}

export function Put(path: string, dtoClass?: any) {
  return Route("PUT", path, dtoClass);
}

export function Delete(path: string, dtoClass?: any) {
  return Route("DELETE", path, dtoClass);
}
