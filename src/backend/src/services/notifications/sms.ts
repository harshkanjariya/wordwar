import { IChannel } from "./channel";
import {sendSms} from "../../utils/sms-utils";

export class SmsChannel implements IChannel {
  async sendOtp(identifier: string, otp: string): Promise<boolean> {
    return sendSms(identifier, otp);
  }
}
