import React from "react";

import { ISquare } from "../interface/ISquare";

type IProps = {
  square: ISquare;
}

export const Square: React.FunctionComponent<IProps> = (props: IProps) => {
  const square = props.square;

  return (
    <div className="bingo-square" data-position={square.position}>
      <div className="bingo-text">  {/* TODO small text for long squares */}
        {square.text}
      </div>
    </div>
  );
}
