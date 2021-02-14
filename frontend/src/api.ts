import React from "react";

import { IBingoGameState } from "./BingoGame";
import { SpaceId } from "./interface/ISpace";

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
    const spaces = board["spaces"] as any[];
    setState(state => ({
      ...state, board: {
        ...state.board,
        obscured: obscured,
        spaces: spaces.map(s => ({
          space_id: s["space_id"],
          position: {
            x: s["position"]["x"],
            y: s["position"]["y"],
            z: s["position"]["z"],
          },
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
        markings: [...pb["markings"]].map(m => ({
          space_id: m["space_id"],
          color: m["color"],
        })),
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

export const sendMarkBoard = (spaceId: SpaceId, toState: number) => {
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
