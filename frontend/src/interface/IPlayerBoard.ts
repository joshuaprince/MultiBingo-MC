export type PlayerId = number;
export type Markings = string;

export interface IPlayerBoard {
  player_id: PlayerId;
  player_name: string;
  board: Markings;
}
