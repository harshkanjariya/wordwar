import "reflect-metadata";

const METHOD_PERMISSIONS = Symbol("method_permissions");

export function Permission(permission: string | "" = "") {
  return function (target: any, propertyKey: string) {
    Reflect.defineMetadata(METHOD_PERMISSIONS, permission, target, propertyKey);
  };
}

export function getMethodPermission(target: any, propertyKey: string): string | undefined {
  return Reflect.getMetadata(METHOD_PERMISSIONS, target, propertyKey);
}
