import {
  IsNumber,
  IsString,
  Min,
  ValidateNested,
  IsArray,
  ArrayMinSize, IsOptional,
} from 'class-validator';
import { Type } from 'class-transformer';
import {google} from "firebase-functions/protos/compiledFirestore";
import type = google.type;


export class CellCoordinate {
  @IsNumber()
  @Min(0)
  row: number;

  @IsNumber()
  @Min(0)
  col: number;
}

export class ClaimedWord {
  @IsString()
  word: string;

  @IsArray()
  @ArrayMinSize(3)
  @ValidateNested({ each: true })
  @Type(() => CellCoordinate)
  cellCoordinates: CellCoordinate[];
}

export class GameAction {
  @IsString()
  @IsOptional()
  character?: string;

  @IsNumber()
  @Min(0)
  @IsOptional()
  row?: number;

  @IsNumber()
  @Min(0)
  @IsOptional()
  col?: number;

  @IsArray()
  @ArrayMinSize(0)
  @ValidateNested({ each: true })
  @Type(() => ClaimedWord)
  claimedWords: ClaimedWord[];

  isValid() {
    if (
      (this.character && typeof this.row === "number" && typeof this.col === "number")
      || this.claimedWords.length
    ) {
      return true;
    }
    return false;
  }
}

