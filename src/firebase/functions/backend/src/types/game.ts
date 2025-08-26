export interface LiveGame {
  players: string[];
  joinedAt: Record<string, number>;
  createdAt: number;
  cellData: string[][];
  currentPlayer: string;
  claimedWords: Record<string, string[]>;
}

export interface GameHistory {
  players: string[];
  joinedAt: Record<string, number>;
  leftAt: Record<string, number>;
  startedAt: number;
  endedAt: number;
  cellData: string[][];
  claimedWords: Record<string, string[]>;
  playerNames?: Record<string, string>;
}
