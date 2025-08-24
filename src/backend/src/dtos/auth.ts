
import {IsEmail, IsEnum, IsNotEmpty, IsOptional, IsString} from "class-validator";
import {ChannelEnum, SocialAuthProvider} from "../types/api";

export class VerifyResetPasswordTokenDto {
  @IsNotEmpty()
  @IsString()
  token: string;
}

export class ResetPasswordDto extends VerifyResetPasswordTokenDto {
  @IsNotEmpty()
  @IsString()
  password: string;
}

export class ForgotPasswordDto {
  @IsNotEmpty()
  @IsEmail()
  email: string;
}

export class LoginUserDto {
  @IsNotEmpty()
  @IsEmail()
  email: string;

  @IsNotEmpty()
  @IsString()
  password: string;
}

export class SocialLoginUserDto {
  @IsNotEmpty()
  @IsEnum(SocialAuthProvider)
  type: SocialAuthProvider;

  @IsNotEmpty()
  @IsString()
  accessToken: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  photoURL?: string;

  @IsOptional()
  @IsString()
  displayName?: string;
}

export class RegisterUserDto {
  @IsNotEmpty()
  @IsEmail()
  email: string;

  @IsNotEmpty()
  @IsString()
  password: string;

  @IsNotEmpty()
  @IsString()
  name: string;

  @IsNotEmpty()
  @IsString()
  phoneNumber: string;
}

export class VerifyOtpDto {
  @IsString()
  @IsNotEmpty()
  identifier: string;

  @IsString()
  @IsNotEmpty()
  otp: string;

  @IsEnum(ChannelEnum)
  @IsNotEmpty()
  channel: ChannelEnum;
}

export class ChannelIdentifierDto {
  @IsString()
  @IsNotEmpty()
  identifier: string;

  @IsEnum(ChannelEnum)
  @IsNotEmpty()
  channel: ChannelEnum;
}