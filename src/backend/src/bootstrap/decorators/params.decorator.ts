import 'reflect-metadata';
import { ROUTE_ARGS_METADATA } from './index';

function createParamDecorator(type: string) {
  return (target: Object, propertyKey: string | symbol, parameterIndex: number) => {
    const existingMetadata = Reflect.getOwnMetadata(ROUTE_ARGS_METADATA, target, propertyKey) || {};
    const paramTypes = Reflect.getMetadata('design:paramtypes', target, propertyKey);

    const dtoClass = paramTypes[parameterIndex];

    existingMetadata[parameterIndex] = { index: parameterIndex, type, dtoClass };
    Reflect.defineMetadata(ROUTE_ARGS_METADATA, existingMetadata, target, propertyKey);
  };
}

// Updated decorators
export const Body = createParamDecorator('body');
export const Query = createParamDecorator('query');
export const Params = createParamDecorator('params');
export const Files = createParamDecorator('files');
export const Req = createParamDecorator('request');
export const Res = createParamDecorator('reply');
export const AuthUser = createParamDecorator('auth_user');
