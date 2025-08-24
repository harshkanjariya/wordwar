import nodemailer from "nodemailer";
import { config } from "../config";

const transporter = nodemailer.createTransport({
  host: "smtp.hostinger.com",
  port: 465,
  secure: true,
  auth: config.email,
});

export async function sendEmail({
                                  Destination,
                                  Subject,
                                  BodyHtml,
                                }: {
  Destination: { ToAddresses: string[] };
  Subject: string;
  BodyHtml?: string;
}) {
  if (!BodyHtml) {
    throw new Error("At least one of BodyHtml or BodyText must be provided.");
  }

  const mailOptions = {
    from: config.email.user,
    to: Destination.ToAddresses.join(", "),
    subject: Subject,
    ...(BodyHtml && { html: BodyHtml }),
  };

  if (config.emailActive) {
    return transporter.sendMail(mailOptions);
  }
  return {};
}
