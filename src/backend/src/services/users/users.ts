import {repositories} from "../../db/repositories";
import * as jwt from "jsonwebtoken";
import {config} from "../../config";
import {ApiError} from "../../bootstrap/errors";
import {
  ChannelIdentifierDto,
  ForgotPasswordDto,
  LoginUserDto,
  RegisterUserDto,
  ResetPasswordDto,
  SocialLoginUserDto,
  VerifyOtpDto,
  VerifyResetPasswordTokenDto
} from "../../dtos/auth";
import {Filter, ObjectId} from "mongodb";
import {getConfigurationValue} from "../configurations";
import {AdminCreateUserDto, AdminUpdateUserDto, SearchUserDto, UpdateProfileDto} from "../../dtos/users";
import {atlasIndex} from "../../utils/constants";
import redisManager from "../../cache/redis";
import {cacheExpiration} from "../../cache/cache.constants";
import ejs from "ejs";
import path from "path";
import crypto from "crypto";
import {sendEmail} from "../../utils/email-utils";
import {firebaseAuth} from "../../utils/firebase";
import {PipelineStage, Projection} from "../../utils/mongo-types";
import {decrypt, encrypt} from "../../utils/encryption";
import {ChannelFactory} from "../notifications/channel";
import bcrypt from 'bcryptjs';
import {ChannelEnum, FullDocument} from "../../types/api";
import {User, UserStatusEnum, UserType} from "../../types";
import {configurationKeys} from "../../types/configuration";

export async function getUserCount() {
  return repositories.users.count();
}

export async function getNewUsersPerDay(startDate: Date, endDate: Date) {
  const users = await repositories.users.aggregate([
    {
      $match: {
        createdAt: {
          $gte: startDate,
          $lte: endDate,
        },
      },
    },
    {
      $group: {
        _id: {$dateToString: {format: "%Y-%m-%d", date: "$createdAt"}},
        count: {$sum: 1},
      },
    },
    {
      $sort: {_id: 1},
    },
  ]) as any;

  return users.map((entry: any) => ({
    date: entry._id,
    count: entry.count,
  }));
}

// Generate OTP and cache it
export async function generateOTP(email: string): Promise<string> {
  const otp = crypto.randomInt(100000, 999999).toString(); // Generate a 6-digit OTP
  const cacheKey = `otp:${email?.toLowerCase()}`;
  await redisManager.set(cacheKey, otp, cacheExpiration.ten_minutes); // Cache OTP for 10 minutes
  return otp;
}

// Verify OTP
export async function verifyOTP(data: VerifyOtpDto): Promise<boolean> {
  const cacheKey = `otp:${data.identifier.toLowerCase()}`;
  const cachedOtp = await redisManager.get<string>(cacheKey);

  if (!cachedOtp) {
    throw new ApiError("OTP has expired. Please request a new one.", 400);
  }

  if (cachedOtp !== data.otp) {
    throw new ApiError("Invalid OTP. Please try again.", 400);
  }

  const user = await repositories.users.findOne({
    filter: data.channel === ChannelEnum.Email ?
      {email: data.identifier?.toLowerCase()} :
      {phoneNumber: data.identifier?.toLowerCase()}
  });

  if (!user) {
    throw new ApiError("User not found", 404);
  }


  const dataToUpdate: Partial<User> = {};

  if (data.channel === ChannelEnum.Email) {
    dataToUpdate.verifiedEmails = user.verifiedEmails?.concat(user.email) || [user.email];
  } else if (data.channel === ChannelEnum.Sms) {
    if (!user.phoneNumber) {
      throw new ApiError("User doesn't have phone number");
    }
    dataToUpdate.verifiedPhones = user.verifiedPhones?.concat(user.phoneNumber) || [user.phoneNumber];
  }

  await repositories.users.update({_id: user._id}, dataToUpdate);

  await redisManager.delete(`users:${user?._id}`);
  await redisManager.delete(cacheKey);

  return true;
}

export async function resendOTP(body: ChannelIdentifierDto) {
  const otp = await generateOTP(body.identifier);

  const channel = ChannelFactory.getChannel(body.channel);

  return channel.sendOtp(body.identifier, otp);
}

export async function registerAdminUser(data: RegisterUserDto) {
  const defaultUserRole = await getConfigurationValue(configurationKeys.defaultVendorRole);
  const hashedPassword = await bcrypt.hash(data.password, 10);
  const user: Omit<User, "_id"> = {
    ...data,
    password: hashedPassword,
    status: UserStatusEnum.ACTIVE,
    email: data?.email?.toLowerCase(),
    verifiedEmails: [],
    verifiedPhones: [],
    type: UserType.ADMIN,
    roleId: defaultUserRole,
  };

  return repositories.users.create(user);
}

export async function registerUser(data: RegisterUserDto) {
  const defaultUserRole = await getConfigurationValue(configurationKeys.defaultUserRole);
  const hashedPassword = await bcrypt.hash(data.password, 10);
  const user: User = {
    ...data,
    password: hashedPassword,
    status: UserStatusEnum.ACTIVE,
    verifiedEmails: [],
    verifiedPhones: [],
    email: data?.email?.toLowerCase(),
    type: UserType.USER,
    roleId: defaultUserRole,
  };

  return repositories.users.create(user);
}

export async function validateUser(data: LoginUserDto) {
  const user = await repositories.users.findOne({
    filter: {
      email: data.email?.toLowerCase(),
    }
  });

  if (!user) {
    throw new ApiError("User not found", 404);
  }

  const isMatch = await bcrypt.compare(data.password, user.password);
  if (!isMatch) {
    throw new ApiError("Passwords do not match", 400);
  }

  return {
    token: generateToken(user),
  };
}

export async function forgotPassword(data: ForgotPasswordDto) {
  const user = await repositories.users.findOne({
    filter: {
      email: data.email.toLowerCase(),
    },
  });

  if (!user) {
    throw new ApiError("User not found", 404);
  }

  const tokenData = {
    email: user.email,
    timestamp: Date.now(), // Include the time of generation
  };

  const resetToken = encrypt(tokenData, config.encryption.resetPassword);

  const emailTemplatePath = path.join(__dirname, "/../../notifications/emails/reset-password.ejs");
  const emailContent = await ejs.renderFile(emailTemplatePath, {
    resetToken,
    frontendBaseUrl: "https://wordwar.com",
  });

  return sendEmail({
    Destination: {ToAddresses: [user.email]},
    Subject: "Password Reset Request",
    BodyHtml: emailContent,
  });
}

export async function verifyResetToken(body: VerifyResetPasswordTokenDto): Promise<{
  email: string;
  timestamp: number
}> {
  try {
    const data = decrypt(body.token, config.encryption.resetPassword);

    // Validate token age (e.g., 1-hour expiration)
    const MAX_AGE = 60 * 60 * 1000; // 1 hour in milliseconds
    if (Date.now() - data.timestamp > MAX_AGE) {
      throw new ApiError("Token has expired.", 400);
    }

    // Optionally, check if the user exists in the database
    const user = await repositories.users.findOne({
      filter: {
        email: data.email,
      },
    });

    if (!user) {
      throw new ApiError("User not found.", 404);
    }

    return data; // Return the email and timestamp
  } catch (err) {
    throw new ApiError("Invalid or corrupted token.", 400);
  }
}

export async function resetPassword(data: ResetPasswordDto) {
  const {email} = await verifyResetToken(data);

  const hashedPassword = await bcrypt.hash(data.password, 10);
  const user = await repositories.users.update(
    {email},
    {password: hashedPassword} // Update the password
  );

  if (!user) {
    throw new ApiError("Failed to reset password.", 500);
  }

  return {message: "Password reset successfully."};
}

export async function socialLogin(data: SocialLoginUserDto) {
  const decodedToken = await firebaseAuth.verifyIdToken(data.accessToken);

  const {email = "", email_verified = false, picture, name, phone_number} = decodedToken;

  const user = await repositories.users.findOne({
    filter: {email}
  });
  if (user) {
    return {
      token: generateToken(user),
    };
  } else {
    const defaultUserRole = await getConfigurationValue(configurationKeys.defaultUserRole);

    const user = await repositories.users.create({
      status: UserStatusEnum.ACTIVE,
      email: email.toLowerCase(),
      verifiedEmails: email_verified ? [email.toLowerCase()] : [],
      verifiedPhones: [],
      phoneNumber: phone_number || undefined,
      name,
      password: "",
      type: UserType.USER,
      roleId: defaultUserRole,
      image: picture,
    });

    return {
      token: generateToken(user),
    }
  }
}

export function generateToken(payload: any) {
  return jwt.sign(payload, config.jwtSecret);
}

export function getUserDetails(id: string) {
  return repositories.users.findOne({
    filter: {
      _id: ObjectId.createFromHexString(id),
    }
  });
}

export function createUser(user: AdminCreateUserDto) {
  return repositories.users.create({
    ...user,
    status: UserStatusEnum.ACTIVE,
    email: user?.email?.toLowerCase(),
    verifiedEmails: [],
    verifiedPhones: [],
  });
}

export async function updateUserDetails(id: string, body: AdminUpdateUserDto) {
  await redisManager.delete(`users:${id}`);
  const {_id, ...updateInfo} = body as any;
  return repositories.users.update(
    {
      _id: ObjectId.createFromHexString(id),
    },
    updateInfo,
  );
}

export function searchUser(body: SearchUserDto, context: "client" | "admin") {
  const filter: Filter<User> = {
    ...(body.userIds?.length ? {_id: {$in: body.userIds}} : {}),
  }
  if (body.userType) filter.type = body.userType;
  if (context === "client") filter.status = {$in: [UserStatusEnum.ACTIVE, UserStatusEnum.COMING_SOON]};
  else if (body.status) filter.status = body.status;

  const project: Projection<User> = {
    _id: 1, name: 1, image: 1, status: 1,
  }
  if (context === "admin") {
    project.email = 1;
    project.phoneNumber = 1;
    project.type = 1;
  }
  if (body.keyword) {
    const pipeline: PipelineStage<FullDocument<User>>[] = [
      {$search: atlasIndex.user.name(body.keyword)},
      {$match: filter},
      {$project: project},
    ];
    return repositories.users.aggregate(pipeline);
  } else {
    return repositories.users.findAll({
      filter,
      skip: body.skip,
      limit: body.limit,
      project,
    });
  }
}

export async function getCachedUser(id: string) {
  const cacheKey = `users:${id}`;
  const user = await redisManager.get<FullDocument<User> | undefined>(
    cacheKey,
    cacheExpiration.one_hour,
    (cachedValue) => !cachedValue,
    async () => {
      const user = await repositories.users.findOne({
        filter: {
          _id: new ObjectId(id),
        }
      });
      if (user) return user;
      else return undefined;
    }
  );
  return user ? {
    ...user,
    _id: new ObjectId(user._id)
  } : user;
}

export async function updateProfile(body: UpdateProfileDto, user: FullDocument<User>) {
  if (!user?._id) {
    throw new ApiError("Unauthorized", 401);
  }

  const cacheKey = `users:${user._id}`;
  const oldUser = await getCachedUser(user._id.toString());
  await redisManager.delete(cacheKey);

  const dataToUpdate: any = {};

  if (body.image != undefined) dataToUpdate.image = body.image;
  if (body.email) {
    dataToUpdate.email = body.email;
    dataToUpdate.emailVerified = oldUser?.verifiedEmails?.includes(body.email);
  }
  if (body.phoneNumber) {
    dataToUpdate.phoneNumber = body.phoneNumber;
    dataToUpdate.phoneVerified = oldUser?.verifiedPhones?.includes(body.phoneNumber);
  }

  return await repositories.users.update({
    _id: user?._id,
  }, dataToUpdate);
}