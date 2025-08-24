import { EmailChannel } from "./email";
import { SmsChannel } from "./sms";
import { WhatsAppChannel } from "./whatsapp";
import {ChannelEnum} from "../../types/api";

export interface IChannel {
  sendOtp(identifier: string, otp: string): Promise<boolean>;
}

export class ChannelFactory {
  static getChannel(channel: ChannelEnum): IChannel {
    switch (channel) {
      case ChannelEnum.Email:
        return new EmailChannel();
      case ChannelEnum.Sms:
        return new SmsChannel();
      case ChannelEnum.WhatsApp:
        return new WhatsAppChannel();
      default:
        throw new Error(`Unsupported channel: ${channel}`);
    }
  }
}
