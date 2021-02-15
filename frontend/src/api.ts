import React from "react";
import z from "myzod";

import { IBingoGameState } from "./BingoGame";
import { TBoard } from "./interface/IBoard";
import { TPlayerBoard } from "./interface/IPlayerBoard";

type SetState = React.Dispatch<React.SetStateAction<IBingoGameState>>;

let socket: WebSocket | null = null;

export const updateWebSocket = (ws: WebSocket | null) => {
  socket = ws;
}

export const onApiMessage = (setState: SetState, message: any) => {
  console.log(message);

  if (!message) {
    return;
  }

  if (message.hasOwnProperty("board")) {
    const board = TBoard.parse(message["board"]);
    setState(state => ({...state, board: board}));
  }

  if (message.hasOwnProperty("pboards")) {
    const pbs = z.array(TPlayerBoard).parse(message["pboards"]);
    setState(state => ({...state, playerBoards: pbs}));
  }
}

export const getWebSocketUrl = (gameCode: string, playerName?: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return protocol + '://'
      + (process.env.NODE_ENV === "development" ? "localhost:8000" : window.location.host)
      + '/ws/board/'
      + encodeURI(gameCode) + encodeURI(playerName ? ('/' + playerName) : '');
}

export const sendMarkBoard = (spaceId: number, toState: number) => {
  send({
    action: "board_mark",
    space_id: spaceId,
    to_state: toState,
  });
}

export const sendRevealBoard = () => {
  send({
    action: "reveal_board",
  });
}

const send = (obj: object) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(obj));
  } else {
    console.error("Attempted to send data on null websocket");
  }
}
