import z, { Infer } from "myzod";

export const TGameMessage = z.object({
  sender: z.string(),
  formatted: z.unknown(), // Needs update when supported messages on web
});

export type IGameMessage = Infer<typeof TGameMessage>;
