import React from "react"
import { TippyProps } from "@tippyjs/react"

import { sendMarkBoard, sendRevealBoard } from "api"
import { Board, BoardOrientation } from "components/board/Board"
import { Space } from "components/board/Space"
import { SpaceContents } from "components/board/SpaceContents"
import { PlayerNameInput } from "components/game/PlayerNameInput"

import { IBoard } from "interface/IBoard"
import { Color, IPlayerBoard, IPlayerBoardMarking } from "interface/IPlayerBoard"
import { ISpace } from "interface/ISpace"

import { ColorPicker } from "./ColorPicker"
import { ResponsiveContext } from "./ResponsiveContext"
import { SpaceTouchModal } from "./SpaceTouchModal"

import boardStyles from "styles/Board.module.scss"
import styles from "styles/Game.module.scss"

type IProps = {
  gameCode: string
  board: IBoard
  playerBoard?: IPlayerBoard
  orientation: BoardOrientation
}

type IState = {
  modalSpace: ISpace | null
  waitingSpaceColors: { spaceId: number, colorWaiting: Color }[]
  waitingSpaceCovert: { spaceId: number, covertWaiting: boolean }[]
}

export const BoardContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  const { isTapOnly, tapToMark } = React.useContext(ResponsiveContext)
  const [ state, setState ] = React.useState<IState>({
    modalSpace: null,
    waitingSpaceColors: [],
    waitingSpaceCovert: [],
  })

  React.useEffect(() => {
    /* Clear spinners from spaces that have loaded */
    for (const space of props.board.spaces) {
      const marking = props.playerBoard?.markings?.find(m => m.space_id === space.space_id)
      if (!marking) continue

      if (state.waitingSpaceColors.find(w => w.spaceId === space.space_id)?.colorWaiting === marking.color) {
        setState(s => {
          return {...s, waitingSpaceColors: s.waitingSpaceColors.filter(w => w.spaceId !== space.space_id)}
        })
      }
      if (state.waitingSpaceCovert.find(w => w.spaceId === space.space_id)?.covertWaiting === marking.covert_marked) {
        setState(s => {
          return {...s, waitingSpaceCovert: s.waitingSpaceCovert.filter(w => w.spaceId !== space.space_id)}
        })
      }
    }
  })

  const nameDisplay = (() => {
    if (props.playerBoard) {
      return <h2>{props.playerBoard.player_name}</h2>
    } else {
      return <PlayerNameInput gameCode={props.gameCode} />
    }
  })()

  const doMark = (spaceId: number) => {
    const colorTo = nextColor(props.playerBoard?.markings?.find(m => m.space_id == spaceId)?.color)
    setState(s => ({...s, waitingSpaceColors: [...s.waitingSpaceColors, {spaceId: spaceId, colorWaiting: colorTo}]}))
    sendMarkBoard({
      space_id: spaceId,
      color: colorTo,
    })
  }

  const doCovertMark = (spaceId: number) => {
    const covertTo = !props.playerBoard?.markings?.find(m => m.space_id == spaceId)?.covert_marked
    setState(s => ({...s, waitingSpaceCovert: [...s.waitingSpaceCovert, {spaceId: spaceId, covertWaiting: covertTo}]}))
    sendMarkBoard({
      space_id: spaceId,
      covert_marked: covertTo,
    })
  }

  const editable = !props.board.obscured && props.playerBoard
  const onClick = (() => {
    if (!editable) {
      return undefined
    } else if (tapToMark) {
      return doMark
    } else {
      return doCovertMark
    }
  })()
  const onContext = (() => {
    if (!editable) {
      return undefined
    } else if (isTapOnly) {
      return (spaceId: number) =>
        setState(s => ({
          ...s, modalSpace: props.board.spaces.find(p => p.space_id === spaceId) ?? null
        }))
    } else {
      return doCovertMark
    }
  })()

  const spacesChangePending = [
    ...state.waitingSpaceColors.map(w => w.spaceId),
    ...state.waitingSpaceCovert.map(w => w.spaceId)
  ]

  const wholeSpaceTooltipProps: TippyProps = {
    delay: [300, 600],
    interactive: true,
    animation: "shift-away",
  }

  return (
    <div className={styles.bcPrimary}>
      <Board
        orientation={props.orientation}
        isObscured={props.board.obscured}
        onRevealButton={sendRevealBoard}
      >
        {props.board.spaces.map(space => {
          const changePending = spacesChangePending.includes(space.space_id)
          return (
            <Space
              orientation={props.orientation}
              key={space.space_id}
              position={space.position}
              colorClass={getSpaceColorClass(props, state, space.space_id)}
              borderColor={getSpaceCovertMarked(props, state, space.space_id) ? "crimson" : undefined}
              onClick={onClick && (() => onClick(space.space_id))}
              onContext={onContext && (() => onContext(space.space_id))}
              wholeSpaceTooltip={editable && !isTapOnly &&
                <ColorPicker className={boardStyles.colorPickerTooltip} spaceId={space.space_id}/>
              }
              wholeSpaceTooltipProps={wholeSpaceTooltipProps}
              changePending={changePending}
            >
              <SpaceContents
                text={space.text}
                tooltipText={space.tooltip}
                showAutomatedA={
                  space.auto && !changePending && !getMarking(space.space_id, props.playerBoard)?.marked_by_player
                }
                obscured={props.board.obscured}
              />
            </Space>
          )
        })}
      </Board>

      {state.modalSpace &&
        <SpaceTouchModal
          isOpen={true}
          close={() => setState(s => ({...s, modalSpace: null}))}
          space={state.modalSpace}
        />
      }

      <div className={styles.boardName}>
        {nameDisplay}
      </div>
    </div>
  )
}

const getSpaceColorClass = (props: IProps, state: IState, spaceId: number): string => {
  const inFlight = state.waitingSpaceColors.find(w => w.spaceId === spaceId)
  if (inFlight) {
    return boardStyles["mark-" + inFlight.colorWaiting]
  }

  const win = props.playerBoard?.win
  if (win && win.includes(spaceId)) {
    return boardStyles.winning
  }

  const marking = props.playerBoard?.markings?.find(m => m.space_id === spaceId)
  return boardStyles["mark-" + (marking?.color ?? Color.UNMARKED)]
}

const getSpaceCovertMarked = (props: IProps, state: IState, spaceId: number): boolean => {
  const inFlight = state.waitingSpaceCovert.find(w => w.spaceId === spaceId)
  if (inFlight) {
    return inFlight.covertWaiting
  }

  const marking = props.playerBoard?.markings?.find(m => m.space_id === spaceId)
  return !!marking?.covert_marked
}

const getMarking = (spaceId: number, playerBoard?: IPlayerBoard): IPlayerBoardMarking | undefined => {
  return playerBoard?.markings.find(m => m.space_id === spaceId)
}

const nextColor = (col?: Color) => {
  switch (col) {
    case Color.UNMARKED:
      return Color.COMPLETE
    case Color.COMPLETE:
      return Color.UNMARKED
    case Color.REVERTED:
      return Color.COMPLETE
    case Color.INVALIDATED:
      return Color.NOT_INVALIDATED
    case Color.NOT_INVALIDATED:
      return Color.INVALIDATED
  }

  return Color.UNMARKED
}
