import z from "myzod"
import React from "react"
import { WebSocketLike } from "react-use-websocket/dist/lib/types"

import { IBingoGameState } from "./components/game/BingoGame"
import { TBoard } from "./interface/IBoard"
import { IPlayerBoardMarking, TPlayerBoard } from "./interface/IPlayerBoard"

type SetState = React.Dispatch<React.SetStateAction<IBingoGameState>>

let socket: WebSocket | null = null

export const updateWebSocket = (ws: WebSocketLike | null) => {
  if (!(ws instanceof WebSocket)) {
    console.error("Got a WebSocketLike that is not an instance of a WebSocket: ")
    console.error(ws)
    return
  }
  socket = ws
}

export const onApiMessage = (setState: SetState, message: any) => {
  console.log(message)

  if (!message) {
    return
  }

  if (message.hasOwnProperty("board")) {
    const board = TBoard.parse(message["board"])
    setState(state => ({...state, board: board}))
  }

  if (message.hasOwnProperty("pboards")) {
    const pbs = z.array(TPlayerBoard).parse(message["pboards"])
    setState(state => ({...state, playerBoards: pbs}))
  }

  if (message.hasOwnProperty("message")) {
    /* TODO
    const msg = TMessage.parse(message["message"])
    setState(state => ({...state, messages: [msg, ...state.messages]}))
     */
  }
}

export const getWebSocketUrl = (gameCode: string, playerName?: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const portStr = (process.env.NODE_ENV === "development" ? ":8000" :
    (window.location.port ? ":" + window.location.port : ""))
  return protocol + '://' + window.location.hostname + portStr
      + '/ws/board/'
      + encodeURI(gameCode) + encodeURI(playerName ? ('/' + playerName) : '')
}

export const sendMarkBoard = (marking: Partial<IPlayerBoardMarking> & Pick<IPlayerBoardMarking, 'space_id'>) => {
  send({
    action: "board_mark",
    space_id: marking.space_id,
    to_state: marking.color,
    covert_marked: marking.covert_marked,
  })
}

export const sendRevealBoard = () => {
  send({
    action: "reveal_board",
  })
}

const send = (obj: object) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(obj))
  } else {
    console.error("Attempted to send data on null websocket")
  }
}
