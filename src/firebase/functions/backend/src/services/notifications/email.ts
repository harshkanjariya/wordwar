import { IChannel } from "./channel";
import {sendEmail} from "../../utils/email-utils";
import path from "node:path";
import ejs from "ejs";

export class EmailChannel implements IChannel {
  async sendOtp(identifier: string, message: string): Promise<any> {
    const emailTemplatePath = path.join(__dirname, "/../../notifications/emails/otp.ejs");
    const emailContent = await ejs.renderFile(emailTemplatePath, {otp: message});
    return sendEmail({
      Destination: {ToAddresses: [identifier]},
      Subject: "Your OTP Code",
      BodyHtml: emailContent,
    });
  }
}
