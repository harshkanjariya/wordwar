import {ConfigurationTypeEnum} from "./api";

export interface Configuration {
  key: string;
  value: string;
  type: ConfigurationTypeEnum;
}

export const configurationKeys = {
  defaultUserRole: 'default_user_role',
  defaultAdminRole: 'default_admin_role',
  defaultVendorRole: 'default_vendor_role',
}
