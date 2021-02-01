import React, { useCallback, useEffect } from 'react';
import useWebSocket, { ReadyState } from "react-use-websocket";

import "./style/look.css";
import "./style/structure.css";
import { BoardContainer } from "./component/BoardContainer";
import { IBoard } from "./interface/IBoard";
import { IPlayerBoard } from "./interface/IPlayerBoard";
import { SecondaryBoardsSidebar } from "./component/SecondaryBoardsSidebar";
import { getWebSocketUrl, onApiMessage, updateWebSocket } from "./api";
import { LoadingSpinner } from "./component/LoadingSpinner";

export type IBingoState = {
  gameCode: string;
  board: IBoard;
  playerBoards: IPlayerBoard[];
  playerName?: string;
  connecting: boolean;
}

export const BingoApp: React.FunctionComponent = () => {
  const [state, setState] = React.useState<IBingoState>(getInitialState);

  const socketUrl = useCallback(() => getWebSocketUrl(state.gameCode, state.playerName),
    [state.gameCode, state.playerName]);
  const {
    lastJsonMessage,
    getWebSocket,
    readyState,
  } = useWebSocket(socketUrl, {
    onOpen: () => console.log('Websocket opened'),
    shouldReconnect: () => true,
  });

  /* Update `connecting` state entry and API's websocket */
  useEffect(() => {
    setState({...state, connecting: (readyState !== ReadyState.OPEN)});
    updateWebSocket(getWebSocket());
  }, [getWebSocket(), readyState]);

  /* React to incoming messages */
  useEffect(() => {
    onApiMessage(setState, lastJsonMessage);
  }, [lastJsonMessage])

  const primaryPlayer = state.playerBoards.find(pb => pb.player_name === state.playerName);
  const secondaryPlayers = state.playerBoards.filter(pb => pb.player_name !== state.playerName);

  return (
    <div className="bingo-app">
      <h1 className="room-name">{state.gameCode}</h1>
      <BoardContainer isPrimary={true} board={state.board} playerBoard={primaryPlayer}/>
      <SecondaryBoardsSidebar board={state.board} playerBoards={secondaryPlayers}/>
      {state.connecting && <LoadingSpinner/>}
    </div>
  );
}

const getInitialState: (() => IBingoState) = () => {
  const board: IBoard = {
    squares: Array.from(Array(25), (_, i) => i).map(num => ({
      position: num,
      text: "",
      auto: false,
    })),
  }

  // Maybe someday, I'll use React Router to make this better.
  const re = /game\/(\w+)/;
  const gameCode = re.exec(window.location.pathname)?.[1] || "UnknownGame";

  const params = new URLSearchParams(window.location.search);
  const name = params.get("name") || undefined;
  if (name) {
    params.delete("name");
    // window.history.pushState(null, "", window.location.pathname)
    // window.location.search = params.toString();
  }

  console.log("Name is " + name);

  return {
    gameCode: gameCode,
    board: board,
    playerBoards: [],
    playerName: name,
    connecting: true,
  };
}
