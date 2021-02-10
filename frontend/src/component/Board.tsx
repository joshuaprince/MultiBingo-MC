import React from "react";

import { IBoard } from "../interface/IBoard";
import { Square } from "./Square";
import { Marking } from "../interface/IPlayerBoard";

type IProps = {
  board: IBoard;
  markings?: Marking[];
  isPrimary: boolean;
}

export const Board: React.FunctionComponent<IProps> = (props: IProps) => {
  return (
    <div className="bingo-board">
      {props.board.squares.map((s, pos) => (
        <Square key={pos} square={s} marking={props.markings?.[pos]}
                obscured={props.board.obscured} isPrimary={props.isPrimary}/>
      ))}
    </div>
  );
}
