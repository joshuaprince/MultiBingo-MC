import { ISquare } from "./ISquare";

export interface IBoard {
  obscured: boolean;
  squares: ISquare[];
}
