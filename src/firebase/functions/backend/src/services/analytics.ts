import { getNewUsersPerDay, getUserCount } from "./users/users";
import moment from "moment";

export async function getAnalytics() {
  const endDate = moment().toDate();
  const startDate = moment().subtract(10, "days").toDate();

  return {
    totalUsers: await getUserCount(),
    newUsersPerDay: await getNewUsersPerDay(startDate, endDate),
  };
}
