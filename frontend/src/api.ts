import React from "react";

import { IBingoState } from "./BingoApp";
import { ISquare } from "./interface/ISquare";
import { IPlayerBoard } from "./interface/IPlayerBoard";

type SetState = React.Dispatch<React.SetStateAction<IBingoState>>;

let socket: WebSocket | null = null;

export const updateWebSocket = (ws: WebSocket | null) => {
  socket = ws;
}

export const onApiMessage = (setState: SetState, message: any) => {
  console.log(message);

  if (message.hasOwnProperty("squares")) {
    const squares = message["squares"] as ISquare[];
    setState(state => ({...state, board: {...state.board, squares}}));
  }

  if (message.hasOwnProperty("pboards")) {
    const pboards = message["pboards"] as IPlayerBoard[];
    setState(state => ({...state, playerBoards: pboards}));
  }
}

export const getWebSocketUrl = (gameCode: string, playerName?: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return protocol + '://'
      // + window.location.host + '/ws/board/'
      + 'localhost:8000' + '/ws/board/'
      + gameCode + (playerName ? ('/' + playerName) : '');
}

export const sendMarkBoard = (position: number, toState: number) => {
  send({
    action: "board_mark",
    position: position,
    to_state: toState,
  });
}

const send = (obj: object) => {
  if (socket) {
    socket.send(JSON.stringify(obj));
  } else {
    console.error("Attempted to send data on null websocket");
  }
}
