import {ObjectId} from "mongodb";

export interface ApiResponse<T> {
  status: number;
  error?: any;
  message?: any;
  details?: any;
  data?: T;
}

export interface BaseDocument extends Document{
  _id: ObjectId;
  createdAt: Date;
  updatedAt: Date;
}

export type FullDocument<T> = T & BaseDocument;

export enum ConfigurationTypeEnum {
  STRING = "STRING",
  NUMBER = "NUMBER",
  BOOLEAN = "BOOLEAN",
  JSON = "JSON",
}

export enum StatusEnum {
  ACTIVE = "ACTIVE",
  INACTIVE = "INACTIVE",
  DELETED = "DELETED",
}

export enum SocialAuthProvider {
  GOOGLE = 'google',
  FACEBOOK = 'facebook',
  TWITTER = 'twitter',
}

export enum ChannelEnum {
  Email = "email",
  Sms = "sms",
  WhatsApp = "whatsapp",
}
