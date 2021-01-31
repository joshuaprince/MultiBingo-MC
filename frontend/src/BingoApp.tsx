import React from 'react';

import "./style/look.css";
import "./style/structure.css";

import { ISquare } from "./interface/ISquare";
import { BoardContainer } from "./component/BoardContainer";
import { IBoard } from "./interface/IBoard";
import { IPlayerBoard, PlayerId } from "./interface/IPlayerBoard";
import { SecondaryBoardsSidebar } from "./component/SecondaryBoardsSidebar";

type IState = {
  gameCode: string;
  board: IBoard;
  playerBoards: IPlayerBoard[];
  playerId?: PlayerId;
}

export const BingoApp: React.FunctionComponent = () => {
  const [state] = React.useState<IState>(getPlaceholderState());
  // const [state] = React.useState<IState>({
  //   board: getPlaceholderBoard(), // TODO
  //   playerBoards: [],
  //   playerId: undefined,
  // });

  const primaryPlayer = state.playerBoards.find(pb => pb.playerId === state.playerId);
  const secondaryPlayers = state.playerBoards.filter(pb => pb.playerId !== state.playerId);

  return (
    <div className="bingo-app">
      <h1 className="room-name">{state.gameCode}</h1>
      <BoardContainer isPrimary={true} board={state.board} playerBoard={primaryPlayer}/>
      <SecondaryBoardsSidebar board={state.board} playerBoards={secondaryPlayers}/>
    </div>
  );
}

const getPlaceholderState = () => {
  const squares: ISquare[] = Array.from(Array(25), (_, i) => i).map(num => ({
    position: num,
    text: "Square " + num,
    auto: false,
  }));

  const board: IBoard = {
    squares: squares,
  }

  const playerBoard: IPlayerBoard = {
    playerId: 30,
    name: "Someone",
    markings: "1111100000000000000000000",
  };

  return {
    gameCode: "ABCDEF",
    board: board,
    playerBoards: [playerBoard, playerBoard],
    playerId: undefined,
  };
}
