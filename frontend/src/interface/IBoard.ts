import z, { Infer } from "myzod";

import { TSpace } from "./ISpace";

export const TBoard = z.object({
  obscured: z.boolean(),
  shape: z.literals("square").default("square"),
  spaces: z.array(TSpace),
});

export type IBoard = Infer<typeof TBoard>;
