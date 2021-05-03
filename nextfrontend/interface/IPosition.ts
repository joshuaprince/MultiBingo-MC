import z, { Infer } from "myzod";

export const TPosition = z.object({
  x: z.number(),
  y: z.number(),
}, {});

export type IPosition = Infer<typeof TPosition>;
