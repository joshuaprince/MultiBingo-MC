import z, { Infer } from "myzod";

export enum Color {
  UNMARKED = 0,
  COMPLETE = 1,
  REVERTED = 2,
  INVALIDATED = 3,
  NOT_INVALIDATED = 4,
}

export const TPlayerBoardMarking = z.object({
  space_id: z.number(),
  color: z.enum(Color),
  covert_marked: z.boolean().optional(),
});

export type IPlayerBoardMarking = Infer<typeof TPlayerBoardMarking>;

export const TPlayerBoard = z.object({
  player_id: z.number(),
  player_name: z.string(),
  markings: z.array(TPlayerBoardMarking),
  win: z.array(z.number()).nullable(),
  disconnected_at: z.date().nullable(),
})

export type IPlayerBoard = Infer<typeof TPlayerBoard>;
