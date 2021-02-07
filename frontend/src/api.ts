import React from "react";

import { IBingoGameState } from "./BingoGame";

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
    const board = message["board"] as any;
    const obscured = board["obscured"] as boolean;
    const squares = board["squares"] as any[];
    setState(state => ({
      ...state, board: {
        ...state.board,
        obscured: obscured,
        squares: squares.map(s => ({
          position: s["position"],
          text: s["text"],
          tooltip: s["tooltip"],
          auto: s["auto"],
        }))
      }
    }));
  }

  if (message.hasOwnProperty("pboards")) {
    const pboards = message["pboards"] as any[];
    setState(state => ({
      ...state, playerBoards: pboards.map(pb => ({
        board: [...pb["board"]].map(ch => parseInt(ch)),
        player_id: pb["player_id"],
        player_name: pb["player_name"]
      }))
    }));
  }
}

export const getWebSocketUrl = (gameCode: string, playerName?: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return protocol + '://'
      + (process.env.NODE_ENV === "development" ? "localhost:8000" : window.location.host)
      + '/ws/board/'
      + encodeURI(gameCode) + encodeURI(playerName ? ('/' + playerName) : '');
}

export const sendMarkBoard = (position: number, toState: number) => {
  send({
    action: "board_mark",
    position: position,
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
