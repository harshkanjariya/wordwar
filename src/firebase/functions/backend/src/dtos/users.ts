import {IsArray, IsEnum, IsOptional, IsString} from "class-validator";
import {PaginationDto} from "./common";
import {IsObjectId} from "../bootstrap/decorators/object-id.decorator";
import {ObjectId} from "mongodb";
import {Type} from "class-transformer";
import {UserStatusEnum, UserType} from "../types";

export class AdminCreateUserDto {
  @IsString()
  @IsOptional()
  name: string;

  @IsObjectId()
  @IsOptional()
  roleId: ObjectId;

  @IsString()
  @IsOptional()
  phoneNumber: string;

  @IsString()
  @IsOptional()
  email: string;

  @IsString()
  @IsOptional()
  password: string;

  @IsEnum(UserType)
  @IsOptional()
  type: UserType;
}

export class AdminUpdateUserDto {
  @IsString()
  @IsOptional()
  name: string;

  @IsObjectId()
  @IsOptional()
  roleId: ObjectId;

  @IsObjectId()
  @IsOptional()
  vendorId: ObjectId;

  @IsString()
  @IsOptional()
  phoneNumber: string;

  @IsString()
  @IsOptional()
  email: string;

  @IsString()
  @IsOptional()
  image: string;

  @IsEnum(UserType)
  @IsOptional()
  type: UserType;

  @IsString({ each: true })
  @IsOptional()
  tags: string[];

  @IsEnum(UserStatusEnum)
  @IsOptional()
  status: UserStatusEnum;
}

export class SearchUserDto extends PaginationDto {
  @IsOptional()
  @IsObjectId({each: true})
  @IsArray()
  @Type(() => ObjectId)
  userIds?: ObjectId[];

  @IsString()
  @IsOptional()
  keyword: string;

  @IsEnum(UserType)
  @IsOptional()
  userType: UserType;

  @IsEnum(UserStatusEnum)
  @IsOptional()
  status: UserStatusEnum;
}

export class UpdateProfileDto {
  @IsString()
  @IsOptional()
  email?: string;

  @IsString()
  @IsOptional()
  phoneNumber?: string;

  @IsString()
  @IsOptional()
  image?: string;
}