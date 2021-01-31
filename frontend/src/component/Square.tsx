import React from "react";

import { ISquare } from "../interface/ISquare";
import { sendMarkBoard } from "../api";

type IProps = {
  square: ISquare;
  marking: string;
}

export const Square: React.FunctionComponent<IProps> = (props: IProps) => {
  const square = props.square;

  const onClick = () => {
    sendMarkBoard(props.square.position, 1); // TODO toState
  }

  return (
    <div className={"bingo-square mark-" + props.marking} onClick={onClick}>
      <div className="bingo-text">  {/* TODO small text for long squares */}
        {square.text}
      </div>
    </div>
  );
}
