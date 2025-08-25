import {CreatePermissionDto, CreateRoleDto, UpdateRoleDto} from "../dtos/roles";
import {repositories} from "../db/repositories";
import {ApiError} from "../bootstrap/errors";
import redisManager from "../cache/redis";
import {cacheExpiration, cacheKeys} from "../cache/cache.constants";
import {ObjectId} from "mongodb";

export function createPermission(data: CreatePermissionDto) {
  return repositories.permissions.create(data);
}

export async function getPermissionsList() {
  return repositories.permissions.findAll({
    limit: 1000,
  });
}

export function getRoleList() {
  return repositories.roles.findAll({});
}

export async function getRoleDetails(id: string) {
  const role = await repositories.roles.findOne({
    filter: { _id: ObjectId.createFromHexString(id) }
  });
  if (!role) {
    throw new ApiError("Role not found", 404);
  }
  return role;
}

export async function getRoleAllowedPermissionsCached(roleId: string) {
  return redisManager.get<string[]>(
    cacheKeys.rolePermissions(roleId),
    cacheExpiration.one_day,
    (value) => !(value && value?.length),
    async () => {
      const role = await repositories.roles.findOne({
        filter: { _id: ObjectId.createFromHexString(roleId) },
      });
      return role?.permissions || [];
    }
  );
}


export async function createRole(data: CreateRoleDto) {
  return await repositories.roles.create(data);
}

export async function updateRole(id: string, data: UpdateRoleDto) {
  const role = await repositories.roles.updateOne({
    _id: ObjectId.createFromHexString(id),
  }, data);

  await redisManager.delete(cacheKeys.rolePermissions(id));

  return role;
}
