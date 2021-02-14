import { IPosition } from "./IPosition";

export type SpaceId = number;

export interface ISpace {
  space_id: SpaceId;
  position: IPosition;
  text: string;
  tooltip?: string;
  auto: boolean;
}
