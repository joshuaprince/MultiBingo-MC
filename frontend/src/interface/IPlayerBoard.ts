export type PlayerId = number;
export type Markings = string;

export interface IPlayerBoard {
  playerId: PlayerId;
  name: string;
  markings: Markings;
}
