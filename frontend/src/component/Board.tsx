import React from "react";

import { IBoard } from "../interface/IBoard";
import { Square } from "./Square";
import { Marking } from "../interface/IPlayerBoard";

type IProps = {
  board: IBoard;
  markings?: Marking[];
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
        <td key={pos}>
          <Square square={props.board.squares[pos]} marking={props.markings?.[pos]} />
        </td>
      );
    }
    rows.push(<tr key={row}>{cells}</tr>);
  }

  /* TODO Get rid of this className */
  return (
    <table className="bingo-table">
      <tbody>
        {rows}
      </tbody>
    </table>
  );
}
