import {
  IsNumber,
  IsString,
  Min,
  ValidateNested,
  IsArray,
  ArrayMinSize,
} from 'class-validator';
import { Type } from 'class-transformer';


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
  @ArrayMinSize(2)
  @ValidateNested({ each: true })
  @Type(() => CellCoordinate)
  cellCoordinates: CellCoordinate[];
}

export class GameAction {
  @IsString()
  character: string;

  @IsNumber()
  @Min(0)
  row: number;

  @IsNumber()
  @Min(0)
  col: number;

  @IsArray()
  @ArrayMinSize(1)
  @ValidateNested({ each: true })
  @Type(() => ClaimedWord)
  claimedWords: ClaimedWord[];
}

