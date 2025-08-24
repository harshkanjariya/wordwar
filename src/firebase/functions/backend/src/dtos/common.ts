import {IsArray, IsInt, IsNotEmpty, IsOptional, IsPositive, IsString, Min} from "class-validator";
import {Type} from "class-transformer";
import {IsObjectId} from "../bootstrap/decorators/object-id.decorator";
import {ObjectId} from "mongodb";

export class PaginationDto {
  @IsInt()
  @Min(0)
  @IsOptional()
  @Type(() => Number)
  skip?: number;

  @IsInt()
  @IsPositive()
  @IsOptional()
  @Type(() => Number)
  limit?: number;

  @IsString()
  @IsOptional()
  @Type(() => String)
  continuationToken?: string;
}

export class IdParamDto {
  @IsString()
  id: ObjectId;
}

export class ValueParamDto {
  @IsString()
  value: string;
}

export class IdArrayDto {
  @IsString({each: true})
  @IsArray()
  @IsNotEmpty()
  ids: string[];
}

export class ImageUploadDto {
  @IsString()
  @IsNotEmpty()
  bucket: string;

  @IsString()
  @IsNotEmpty()
  path: string;
}

export function PartialType<T extends new (...args: any[]) => any>(BaseClass: T) {
  class PartialClass extends BaseClass {
  }

  // Iterate through the properties of the BaseClass and make them optional
  Object.getOwnPropertyNames(new BaseClass()).forEach((property) => {
    Type(() => (() => {
    }))(PartialClass.prototype, property);
  });

  return PartialClass as new () => Partial<InstanceType<T>>;
}