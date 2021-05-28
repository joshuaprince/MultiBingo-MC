import React from "react"
import { TippyProps } from "@tippyjs/react"

import { Board, BoardOrientation } from "components/board/Board"
import { Space } from "components/board/Space"
import { SpaceContents } from "components/board/SpaceContents"

import { IBoard } from "interface/IBoard"
import { Color, IPlayerBoard } from "interface/IPlayerBoard"

import boardStyles from "styles/Board.module.scss"
import gameStyles from "styles/Game.module.scss"

type IProps = {
  board: IBoard
  playerBoard: IPlayerBoard
  orientation: BoardOrientation
}

export const SecondaryBoardContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  const nameDisplay = <h2>{props.playerBoard.player_name}</h2>

  const wholeSpaceTooltipProps: TippyProps = {
    delay: 0,
    interactive: false,
  }

  return (
    <div className={gameStyles.bcSecondary}>
      <Board
        orientation={props.orientation}
        isObscured={props.board.obscured}
      >
        {props.board.spaces.map(space => (
          <Space
            orientation={props.orientation}
            key={space.space_id}
            position={space.position}
            colorClass={getSpaceColorClass(props, space.space_id)}
            borderColor={getSpaceCovertMarked(props, space.space_id) ? "crimson" : undefined}
            wholeSpaceTooltip={!props.board.obscured && space.text}
            wholeSpaceTooltipProps={wholeSpaceTooltipProps}
          >
            <SpaceContents
              // Hide all internal content on secondary boards.
              text={""}
              obscured={props.board.obscured}
            />
          </Space>
        ))}
      </Board>

      <div className={gameStyles.boardName}>
        {nameDisplay}
      </div>
    </div>
  )
}

const getSpaceColorClass = (props: IProps, spaceId: number): string => {
  // TODO: Deduplicate with BoardContainer
  const win = props.playerBoard?.win
  if (win && win.includes(spaceId)) {
    return boardStyles.winning
  }

  const marking = props.playerBoard?.markings?.find(m => m.space_id === spaceId)
  return boardStyles["mark-" + (marking?.color ?? Color.UNMARKED)]
}

const getSpaceCovertMarked = (props: IProps, spaceId: number): boolean => {
  const marking = props.playerBoard?.markings?.find(m => m.space_id === spaceId)
  return !!marking?.covert_marked
}
