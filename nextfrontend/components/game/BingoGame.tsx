import React, { useCallback, useEffect } from 'react'
import { useMediaQuery } from "react-responsive"
import useWebSocket, { ReadyState } from "react-use-websocket"

import styles from "styles/Game.module.scss"
import { getWebSocketUrl, onApiMessage, updateWebSocket } from "../../api"
import { IBoard } from "../../interface/IBoard"
import { IGameMessage } from "../../interface/IGameMessage"
import { IPlayerBoard } from "../../interface/IPlayerBoard"

import { BoardContainer } from "./BoardContainer"
import { BoardSkeleton } from "./BoardSkeleton"
import { CornerLoadingSpinner } from "./CornerLoadingSpinner"
import { ResponsiveContext } from './ResponsiveContext'
import { SecondaryBoardsContainer } from "./SecondaryBoardsContainer"
import { TapModeSelector } from "./TapModeSelector"

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

  const socketUrl = useCallback(() => getWebSocketUrl(props.gameCode, props.playerName),
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
  useEffect(() => {
    setState(s => ({...s, connecting: (readyState !== ReadyState.OPEN)}))
    updateWebSocket(getWebSocket())
  }, [getWebSocket, readyState])

  /* React to incoming messages */
  useEffect(() => {
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
          <BoardContainer isPrimary={true} gameCode={props.gameCode} board={state.board} playerBoard={primaryPlayer} />
          <SecondaryBoardsContainer gameCode={props.gameCode} board={state.board} playerBoards={secondaryPlayers}/>
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
