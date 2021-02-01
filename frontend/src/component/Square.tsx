import React from "react";

import { ISquare } from "../interface/ISquare";
import { sendMarkBoard } from "../api";
import { Marking } from "../interface/IPlayerBoard";

type IProps = {
  square: ISquare;
  marking?: Marking;
}

export const Square: React.FunctionComponent<IProps> = (props: IProps) => {
  const square = props.square;

  const onMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();

    const isRightClick = e.button === 2;
    const newMarking = isRightClick ? 0 : ((props.marking || 0) + 1) % Marking.__COUNT;

    sendMarkBoard(props.square.position, newMarking);

    return false;
  }

  const onContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
  }

  return (
    <div className={"bingo-square mark-" + (props.marking || Marking.UNMARKED)}
         onMouseDown={onMouseDown} onContextMenu={onContextMenu}>
      <div className="bingo-text primary-only">  {/* TODO small text for long squares */}
        {square.text}
      </div>
    </div>
  );
}
