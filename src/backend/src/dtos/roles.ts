import {ArrayNotEmpty, IsArray, IsNotEmpty, IsOptional, IsString} from "class-validator";
import {IsObjectId} from "../bootstrap/decorators/object-id.decorator";
import {ObjectId} from "mongodb";

export class CreatePermissionDto {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsString()
  @IsNotEmpty()
  value: string;
}

export class CreateRoleDto {
  @IsString()
  @IsNotEmpty()
  name: string;

  @IsString()
  @IsOptional()
  description: string;

  @IsString({ each: true })
  @ArrayNotEmpty()
  @IsArray()
  permissions: string[];
}

export class UpdateRoleDto {
  @IsString()
  @IsOptional()
  name: string;

  @IsString()
  @IsOptional()
  description: string;

  @IsString({ each: true })
  @IsOptional()
  @IsArray()
  permissions: string[];
}