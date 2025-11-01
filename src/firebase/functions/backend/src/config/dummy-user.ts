import {User, UserStatusEnum, UserType} from "../types/user";
import {ObjectId} from "mongodb";
import bcrypt from "bcryptjs";

/**
 * Dummy User Configuration
 * This user is used for testing and demo purposes.
 * All APIs will return predefined dummy data when this user is authenticated.
 */

export const DUMMY_USER_CONFIG = {
  email: "demo@wordwar.com",
  password: "DemoUser@123",
  name: "Demo Player",
};

// Pre-hash the password (you should run this once and store the hash)
const DUMMY_PASSWORD_HASH = "$2y$10$TCoxKbZkKUk7Z1Fl/B3OS.vxuYWqOvW/v09vUkfnNht7hkRagSTA."; // Will be generated on first use

export const DUMMY_USER_ID = new ObjectId("000000000000000000000001");
export const DUMMY_ROLE_ID = new ObjectId("000000000000000000000002");

export const DUMMY_USER: User & { _id: ObjectId } = {
  _id: DUMMY_USER_ID,
  name: DUMMY_USER_CONFIG.name,
  email: DUMMY_USER_CONFIG.email,
  password: DUMMY_PASSWORD_HASH,
  phoneNumber: "+1234567890",
  type: UserType.USER,
  roleId: DUMMY_ROLE_ID,
  image: "https://ui-avatars.com/api/?name=Demo+Player&background=4F46E5&color=fff&size=200",
  verifiedEmails: [DUMMY_USER_CONFIG.email],
  verifiedPhones: ["+1234567890"],
  tags: ["demo", "test"],
  status: UserStatusEnum.ACTIVE,
};

// Dummy game data for the dummy user
export const DUMMY_GAME_DATA = {
  liveGames: [
    {
      _id: new ObjectId("100000000000000000000001"),
      players: [DUMMY_USER_ID.toString(), "100000000000000000000002"],
      joinedAt: {
        [DUMMY_USER_ID.toString()]: Date.now() - 300000, // 5 minutes ago
        "100000000000000000000002": Date.now() - 280000,
      },
      createdAt: Date.now() - 300000,
      cellData: [
        ["W", "O", "R", "D"],
        ["A", "R", "E", "A"],
        ["V", "I", "S", "T"],
        ["E", "N", "D", "S"],
      ],
      currentPlayer: DUMMY_USER_ID.toString(),
      claimedWords: {
        [DUMMY_USER_ID.toString()]: ["WORD", "WAVE"],
        "100000000000000000000002": ["AREA", "ENDS"],
      },
    },
  ],
  gameHistory: [
    {
      _id: new ObjectId("200000000000000000000001"),
      players: [DUMMY_USER_ID.toString(), "200000000000000000000002"],
      joinedAt: {
        [DUMMY_USER_ID.toString()]: Date.now() - 7200000, // 2 hours ago
        "200000000000000000000002": Date.now() - 7180000,
      },
      leftAt: {
        [DUMMY_USER_ID.toString()]: Date.now() - 3600000, // 1 hour ago
        "200000000000000000000002": Date.now() - 3600000,
      },
      startedAt: Date.now() - 7200000,
      endedAt: Date.now() - 3600000,
      cellData: [
        ["G", "A", "M", "E"],
        ["L", "I", "N", "K"],
        ["O", "V", "E", "R"],
        ["W", "I", "N", "S"],
      ],
      claimedWords: {
        [DUMMY_USER_ID.toString()]: ["GAME", "GLOW", "LINK", "WINS"],
        "200000000000000000000002": ["OVER", "VINE"],
      },
      playerNames: {
        [DUMMY_USER_ID.toString()]: "Demo Player",
        "200000000000000000000002": "AI Player",
      },
    },
    {
      _id: new ObjectId("200000000000000000000002"),
      players: [DUMMY_USER_ID.toString(), "200000000000000000000003"],
      joinedAt: {
        [DUMMY_USER_ID.toString()]: Date.now() - 86400000, // 1 day ago
        "200000000000000000000003": Date.now() - 86380000,
      },
      leftAt: {
        [DUMMY_USER_ID.toString()]: Date.now() - 84600000,
        "200000000000000000000003": Date.now() - 84600000,
      },
      startedAt: Date.now() - 86400000,
      endedAt: Date.now() - 84600000,
      cellData: [
        ["T", "E", "S", "T"],
        ["R", "A", "C", "E"],
        ["I", "D", "E", "A"],
        ["P", "L", "A", "Y"],
      ],
      claimedWords: {
        [DUMMY_USER_ID.toString()]: ["TEST", "RACE", "IDEA", "PLAY", "TRIP"],
        "200000000000000000000003": ["TEAR", "RICE"],
      },
      playerNames: {
        [DUMMY_USER_ID.toString()]: "Demo Player",
        "200000000000000000000003": "Pro Challenger",
      },
    },
  ],
};

export const DUMMY_USER_STATS = {
  totalGames: 15,
  wins: 10,
  losses: 5,
  totalWordsFound: 152,
  longestWord: "QUESTIONING",
  averageWordsPerGame: 10.13,
  winRate: 66.67,
  rank: 42,
};

/**
 * Check if a user ID belongs to the dummy user
 */
export function isDummyUser(userId: string | ObjectId): boolean {
  return userId?.toString() === DUMMY_USER_ID.toString();
}

/**
 * Check if an email belongs to the dummy user
 */
export function isDummyUserEmail(email: string): boolean {
  return email?.toLowerCase() === DUMMY_USER_CONFIG.email.toLowerCase();
}

/**
 * Validate dummy user password
 */
export async function validateDummyUserPassword(password: string): Promise<boolean> {
  return password === DUMMY_USER_CONFIG.password;
}

/**
 * Get hashed password for dummy user (used for initialization)
 */
export async function getDummyUserHashedPassword(): Promise<string> {
  return await bcrypt.hash(DUMMY_USER_CONFIG.password, 10);
}

