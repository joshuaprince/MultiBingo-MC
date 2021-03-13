import React from "react";

import { IBoard } from "../interface/IBoard";
import { Space } from "./Space";
import { IPlayerBoard, IPlayerBoardMarking } from "../interface/IPlayerBoard";

type IProps = {
  board: IBoard;
  playerBoard?: IPlayerBoard;
  isPrimary: boolean;
}

export const Board: React.FunctionComponent<IProps> = (props: IProps) => {
  return (
    <div className={"bingo-board " + props.board.shape}>
      {props.board.spaces.map(s => {
        const marking: IPlayerBoardMarking | undefined =
          props.playerBoard?.markings.find(pbm => pbm.space_id === s.space_id);

        const win: boolean = !!(props.playerBoard?.win?.find(n => n === s.space_id));

        const editable = !!(props.playerBoard && !props.board.obscured && props.isPrimary);

        return <Space key={s.space_id} space={s} shape={props.board.shape} marking={marking?.color}
                      winning={win} obscured={props.board.obscured} editable={editable}
                      isPrimary={props.isPrimary} />
      })}
    </div>
  );
}
