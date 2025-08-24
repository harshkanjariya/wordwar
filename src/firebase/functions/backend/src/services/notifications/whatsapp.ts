import { IChannel } from "./channel";

export class WhatsAppChannel implements IChannel {
  async sendOtp(identifier: string, otp: string): Promise<boolean> {
    console.log(`Sending WhatsApp otp to ${identifier}: ${otp}`);
    return true;
  }
}
