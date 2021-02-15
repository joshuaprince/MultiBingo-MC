import z, { Infer } from "myzod";

export const TPosition = z.object({
  x: z.number(),
  y: z.number(),
  z: z.number(),  /* 0 for square boards */
}, {});

export type IPosition = Infer<typeof TPosition>;
