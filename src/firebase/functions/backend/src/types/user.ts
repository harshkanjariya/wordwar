import {BaseDocument} from "./api";
import {ObjectId} from "mongodb";

export enum UserType {
  USER = "USER",
  ADMIN = "ADMIN",
  SUPER_ADMIN = "SUPER_ADMIN",
}

export interface User {
  name: string;
  email: string;
  password: string;
  phoneNumber?: string;
  type: UserType;
  roleId: ObjectId;
  activeAddressId?: ObjectId;
  image?: string;
  vendorId?: string;
  verifiedEmails: string[];
  verifiedPhones: string[];
  tags?: string[];
  status: UserStatusEnum;
}

export enum UserStatusEnum {
  ACTIVE = "ACTIVE",
  DEACTIVATED = "DEACTIVATED",
  HIDDEN = "HIDDEN",
  COMING_SOON = "COMING_SOON",
}
