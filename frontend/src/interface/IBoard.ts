import { ISpace } from "./ISpace";

export type BoardShape = "square";

export interface IBoard {
  obscured: boolean;
  shape: BoardShape;
  spaces: ISpace[];
}
