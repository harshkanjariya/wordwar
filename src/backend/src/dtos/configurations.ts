import {IsEnum, IsNotEmpty, IsOptional, IsString} from "class-validator";
import {ConfigurationTypeEnum} from "../types/api";

export class CreateConfigurationDto {
  @IsString()
  @IsNotEmpty()
  key: string;

  @IsOptional()
  value: any;

  @IsEnum(ConfigurationTypeEnum)
  @IsNotEmpty()
  type: ConfigurationTypeEnum;
}

export class UpdateConfigurationDto {
  @IsOptional()
  value: any;
}
