import {config as dotenv} from "dotenv";

dotenv();

export const config = {
  environment: process.env.NODE_ENV,
  jwtSecret: process.env.JWT_SECRET || "dba07194-069f-4e6f-aded-0131aaa1babf",
  mongoUri: process.env.MONGO_URI || "mongodb://localhost:27017/wordwar",
  redisUri: process.env.REDIS_URI || "redis://localhost:6379",
  encryption: {
    resetPassword: "21cca18592045cdd97afda1e960d5c6a48204f90fb72cc250fe3f33e29e458c8",
    webhookKey: "21cca18592045cdd97afda1e960d5c6a48204f90fb72cc250fe3f33e29e458c9"
  },
  aws: {
    region: process.env.AWS_REGION || "ap-south-1",
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  },
  sortOptions: ["newlyArrived", "popular", "priceLowToHigh", "priceHighToLow"],
  emailActive: process.env.EMAIL_ACTIVE !== "false",
  paymentActive: process.env.PAYMENT_ACTIVE !== "false",
  email: {
    user: "noreply@xkartindia.com",
    pass: "098@Xkart"
  },
  cashfree: {
    clientId: "TEST104236983321f8909b967b4cbcdb89632401",
    clientSecret: "cfsk_ma_test_b7e80d5e87c105fbdef3421bc4103965_91766925",
    isProduction: false,
    returnUrl: (orderId: string) => `${process.env.FRONTEND_URL}/orders/${orderId}`,
    apiVersion: "2023-08-01"
  },
  twoFactor: {
    url: "https://2factor.in/API/V1",
    apiKey: "YOUR_2FACTOR_API_KEY",
  },
  GCS_PROJECT_ID: process.env.GCS_PROJECT_ID,
  GCS_CLIENT_EMAIL: process.env.GCS_CLIENT_EMAIL,
  GCS_PRIVATE_KEY: process.env.GCS_PRIVATE_KEY || '',
};
