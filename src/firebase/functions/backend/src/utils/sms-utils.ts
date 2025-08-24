import axios from "axios";
import {config} from "../config";

export const sendSms = async (phoneNumber: string, otp: string): Promise<boolean> => {
  try {
    const url = `${config.twoFactor.url}/${config.twoFactor.apiKey}/SMS/${phoneNumber}/${otp}`;
    const response = await axios.get(url);
    if (response.data.Status === "Success") {
      return true;
    } else {
      console.error(`Failed to send SMS: ${response.data.Details}`);
      return false;
    }
  } catch (error) {
    console.error("Error sending SMS:", error);
    return false;
  }
};
