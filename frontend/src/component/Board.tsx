import React from "react";

import { IBoard } from "../interface/IBoard";
import { Square } from "./Square";
import { Markings } from "../interface/IPlayerBoard";

type IProps = {
  board: IBoard;
  markings?: Markings;
}

export const Board: React.FunctionComponent<IProps> = (props: IProps) => {
  const NUM_ROWS = 5;
  const NUM_COLS = 5;

  let rows = [];
  for (let row = 0; row < NUM_ROWS; row++) {
    let cells = [];
    for (let col = 0; col < NUM_COLS; col++) {
      const pos = (row * NUM_COLS) + col;
      cells.push(
        <td>
          <Square key={pos} square={props.board.squares[pos]} />
        </td>
      );
    }
    rows.push(<tr>{cells}</tr>);
  }

  return (
    <table className="bingo-table"> {/* TODO Get rid of this className */}
      {rows}
    </table>
  );
}
