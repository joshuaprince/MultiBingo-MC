import React from 'react';
import useWebSocket from "react-use-websocket";

import "./style/look.css";
import "./style/structure.css";

import { ISquare } from "./interface/ISquare";
import { BoardContainer } from "./component/BoardContainer";
import { IBoard } from "./interface/IBoard";
import { IPlayerBoard } from "./interface/IPlayerBoard";
import { SecondaryBoardsSidebar } from "./component/SecondaryBoardsSidebar";
import { getWebSocketUrl, onApiMessage, updateWebSocket } from "./api";

export type IBingoState = {
  gameCode: string;
  board: IBoard;
  playerBoards: IPlayerBoard[];
  playerName?: string;
}

export const BingoApp: React.FunctionComponent = () => {
  const [state, setState] = React.useState<IBingoState>({
    gameCode: "ABCD",  // TODO
    board: getEmptyBoard(),
    playerBoards: [],
    playerName: "Chips", // TODO
  });

  const socketUrl = getWebSocketUrl(state.gameCode, state.playerName);
  const {
    getWebSocket
  } = useWebSocket(socketUrl, {
    onOpen: () => console.log('Websocket opened'),
    onMessage: (event) => onApiMessage(setState, JSON.parse(event.data)),
    //Will attempt to reconnect on all close events, such as server shutting down
    shouldReconnect: () => true,
  });

  updateWebSocket(getWebSocket());

  const primaryPlayer = state.playerBoards.find(pb => pb.player_name === state.playerName);
  const secondaryPlayers = state.playerBoards.filter(pb => pb.player_name !== state.playerName);

  return (
    <div className="bingo-app">
      <h1 className="room-name">{state.gameCode}</h1>
      <BoardContainer isPrimary={true} board={state.board} playerBoard={primaryPlayer} />
      <SecondaryBoardsSidebar board={state.board} playerBoards={secondaryPlayers}/>
    </div>
  );
}

const getEmptyBoard = () => {
  const squares: ISquare[] = Array.from(Array(25), (_, i) => i).map(num => ({
    position: num,
    text: "",
    auto: false,
  }));

  const board: IBoard = {
    squares: squares,
  }

  return board;
}
