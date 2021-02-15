import z, { Infer } from "myzod";

import { TPosition } from "./IPosition";

export const TSpace = z.object({
  space_id: z.number(),
  position: TPosition,
  text: z.string(),
  tooltip: z.string().optional(),
  auto: z.boolean().default(false),
});

export type ISpace = Infer<typeof TSpace>;
