import React from "react";

import { Board } from "./Board";
import { IBoard } from "../interface/IBoard";
import { IPlayerBoard } from "../interface/IPlayerBoard";
import { PlayerNameInput } from "./PlayerNameInput";

type IProps = {
  isPrimary: boolean,
  board: IBoard,
  playerBoard?: IPlayerBoard,
}

export const BoardContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  const nameDisplay = (props.playerBoard
    ? <h2 className="player-name">{props.playerBoard.player_name}</h2>
    : <PlayerNameInput/>
  );

  return (
    <div className={"board-container " + (props.isPrimary ? "primary" : "secondary")}>
      <Board board={props.board} markings={props.playerBoard?.board}/>
      {nameDisplay}
    </div>
  );
}
