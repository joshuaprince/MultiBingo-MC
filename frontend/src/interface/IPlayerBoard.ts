import { SpaceId } from "./ISpace";

export type PlayerId = number;

export enum Color {
  UNMARKED = 0,
  COMPLETE = 1,
  REVERTED = 2,
  INVALIDATED = 3,
  NOT_INVALIDATED = 4,
  __COUNT
}

export interface IPlayerBoardMarking {
  space_id: SpaceId;
  // position: IPosition;
  color: Color;
}

export interface IPlayerBoard {
  player_id: PlayerId;
  player_name: string;
  markings: IPlayerBoardMarking[];
}
