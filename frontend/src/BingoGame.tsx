import React, { useCallback, useEffect } from 'react';
import useWebSocket, { ReadyState } from "react-use-websocket";

import { BoardContainer } from "./component/BoardContainer";
import { BoardShape, IBoard } from "./interface/IBoard";
import { IPlayerBoard } from "./interface/IPlayerBoard";
import { SecondaryBoardsSidebar } from "./component/SecondaryBoardsSidebar";
import { getWebSocketUrl, onApiMessage, updateWebSocket } from "./api";
import { LoadingSpinner } from "./component/LoadingSpinner";
import { RevealButton } from "./component/RevealButton";
import { IGameMessage } from "./interface/IGameMessage";

type IProps = {
  gameCode: string;
  playerName?: string;
}

export type IBingoGameState = {
  board: IBoard;
  playerBoards: IPlayerBoard[];
  messages: IGameMessage[];
  connecting: boolean;
}

export const BingoGame: React.FunctionComponent<IProps> = (props: IProps) => {
  const [state, setState] = React.useState<IBingoGameState>(getInitialState);

  const socketUrl = useCallback(() => getWebSocketUrl(props.gameCode, props.playerName),
    [props.gameCode, props.playerName]);
  const {
    lastJsonMessage,
    getWebSocket,
    readyState,
  } = useWebSocket(socketUrl, {
    onOpen: () => console.log('Websocket opened: ' + getWebSocket()?.url),
    shouldReconnect: () => true,
  });

  /* Update `connecting` state entry and API's websocket */
  useEffect(() => {
    setState(s => ({...s, connecting: (readyState !== ReadyState.OPEN)}));
    updateWebSocket(getWebSocket());
  }, [getWebSocket, readyState]);

  /* React to incoming messages */
  useEffect(() => {
    onApiMessage(setState, lastJsonMessage);
  }, [lastJsonMessage]);

  const primaryPlayer = state.playerBoards.find(pb => pb.player_name === props.playerName);
  const secondaryPlayers = state.playerBoards.filter(pb => pb.player_name !== props.playerName);

  return (
    <div className={"bingo-app " + (state.board.obscured ? "obscured" : "revealed")}>
      <h1 className="room-name">{props.gameCode}</h1>
      <BoardContainer isPrimary={true} board={state.board} playerBoard={primaryPlayer}/>
      <SecondaryBoardsSidebar board={state.board} playerBoards={secondaryPlayers}/>
      {state.connecting && <LoadingSpinner/>}
      {state.board.obscured && <RevealButton/>}
      {/*<ChatBox messages={state.messages}/>*/}
    </div>
  );
}

const getInitialState: (() => IBingoGameState) = () => {
  const board: IBoard = {
    obscured: true,
    shape: BoardShape.SQUARE,
    spaces: [],
  }

  return {
    board: board,
    connecting: true,
    playerBoards: [],
    messages: [],
  };
}
