export type PlayerId = number;

export enum Marking {
  UNMARKED = 0,
  COMPLETE = 1,
  REVERTED = 2,
  INVALIDATED = 3,
  NOT_INVALIDATED = 4,
  __COUNT
}

export interface IPlayerBoard {
  player_id: PlayerId;
  player_name: string;
  board: Marking[];
}
