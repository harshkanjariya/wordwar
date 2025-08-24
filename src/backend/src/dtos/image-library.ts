import {IsNotEmpty, IsOptional, IsString} from "class-validator";
import {PaginationDto} from "./common";

export class GetImageUsageDto {
  @IsString()
  @IsNotEmpty()
  bucket: string;

  @IsString()
  @IsNotEmpty()
  image: string;
}

export class GetImagesDto extends PaginationDto {
  @IsString()
  @IsNotEmpty()
  bucket: string;

  @IsString()
  @IsOptional()
  folder: string;
}