import {User} from "../types";

export function checkCurrentEmailVerified(user: User | undefined) {
  if (!user) return false;
  return user.verifiedEmails.includes(user.email);
}