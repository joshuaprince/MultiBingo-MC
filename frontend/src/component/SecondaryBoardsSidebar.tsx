import React from "react";

import { IBoard } from "../interface/IBoard";
import { IPlayerBoard } from "../interface/IPlayerBoard";
import { BoardContainer } from "./BoardContainer";

type IProps = {
  board: IBoard,
  playerBoards: IPlayerBoard[],
}

export const SecondaryBoardsSidebar: React.FunctionComponent<IProps> = (props: IProps) => {
  return (
    <div className="boards-secondary-sidebar">
      {props.playerBoards.map(pb => (
      <div key={pb.player_id}>
        <BoardContainer isPrimary={false} board={props.board} playerBoard={pb}/>
      </div>
      ))}
    </div>
  );
}
