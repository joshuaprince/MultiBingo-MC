import z, { Infer } from "myzod";

import { TSpace } from "./ISpace";

export enum BoardShape {
  SQUARE = "square",
  HEXAGON = "hexagon",
}

export const TBoard = z.object({
  obscured: z.boolean(),
  shape: z.enum(BoardShape),
  spaces: z.array(TSpace),
});

export type IBoard = Infer<typeof TBoard>;
