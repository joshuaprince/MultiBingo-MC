import React from 'react'
import { useMediaQuery } from "react-responsive"
import useWebSocket, { ReadyState } from "react-use-websocket"

import { getWebSocketUrl, onApiMessage, updateWebSocket } from "api"

import { BoardShape, IBoard } from "interface/IBoard"
import { IGameMessage } from "interface/IGameMessage"
import { IPlayerBoard } from "interface/IPlayerBoard"

import { BoardContainer } from "./BoardContainer"
import { BoardSkeleton } from "./BoardSkeleton"
import { CornerLoadingSpinner } from "./CornerLoadingSpinner"
import { ResponsiveContext } from './ResponsiveContext'
import { SecondaryBoardsAllContainer } from "./SecondaryBoardsAllContainer"
import { TapModeSelector } from "./TapModeSelector"

import styles from "styles/Game.module.scss"

type IProps = {
  gameCode: string
  playerName?: string
}

export type IBingoGameState = {
  board?: IBoard
  playerBoards: IPlayerBoard[]
  messages: IGameMessage[]
  connecting: boolean
  tapToMark: boolean
}

export const BingoGame: React.FunctionComponent<IProps> = (props: IProps) => {
  const isTapOnly = useMediaQuery({
    query: "(hover: none)"
  })
  const [state, setState] = React.useState<IBingoGameState>(() => getInitialState(isTapOnly))
  const isHexagonVertical = useMediaQuery({
    query: "(max-width: 600px)"
  })
  const orientation = (() => {
    if (state.board?.shape == BoardShape.SQUARE) {
      return "square"
    } else {
      return isHexagonVertical ? "hexagon-vertical" : "hexagon-horizontal"
    }
  })()

  const socketUrl = React.useCallback(() => getWebSocketUrl(props.gameCode, props.playerName),
    [props.gameCode, props.playerName])
  const {
    lastJsonMessage,
    getWebSocket,
    readyState,
  } = useWebSocket(socketUrl, {
    onOpen: () => console.log('Websocket opened: ' + getWebSocket()?.url),
    shouldReconnect: () => true,
  })

  /* Update `connecting` state entry and API's websocket */
  React.useEffect(() => {
    setState(s => ({...s, connecting: (readyState !== ReadyState.OPEN)}))
    updateWebSocket(getWebSocket())
  }, [getWebSocket, readyState])

  /* React to incoming messages */
  React.useEffect(() => {
    onApiMessage(setState, lastJsonMessage)
  }, [lastJsonMessage])

  const primaryPlayer = state.playerBoards.find(pb => pb.player_name === props.playerName)
  const secondaryPlayers = state.playerBoards.filter(pb => pb.player_name !== props.playerName)

  return (
    <ResponsiveContext.Provider
      value={{
        isTapOnly: isTapOnly,
        tapToMark: state.tapToMark,
        setTapToMark: (v: boolean) => setState(s => ({...s, tapToMark: v}))
      }}
    >
      <div className={styles.game}>
        {isTapOnly && primaryPlayer &&
          <TapModeSelector/>
        }
        {state.board && <>
          <BoardContainer
            gameCode={props.gameCode}
            board={state.board}
            playerBoard={primaryPlayer}
            orientation={orientation}
          />
          <SecondaryBoardsAllContainer
            board={state.board}
            playerBoards={secondaryPlayers}
            orientation={orientation}
          />
        </>}
        {!state.board &&
          <BoardSkeleton/>
        }
        {state.connecting && <CornerLoadingSpinner/>}
        {/*<ChatBox messages={state.messages}/>*/}
      </div>
    </ResponsiveContext.Provider>
  )
}

const getInitialState = (isTapOnly: boolean): IBingoGameState => {
  return {
    board: undefined,
    connecting: true,
    playerBoards: [],
    messages: [],
    tapToMark: !isTapOnly,
  }
}
