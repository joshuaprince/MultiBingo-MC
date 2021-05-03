import classNames from "classnames"
import { Space } from "components/board/Space"

import { BoardShape, IBoard } from "interface/IBoard"
import { IPlayerBoard, IPlayerBoardMarking } from "interface/IPlayerBoard"
import React from "react"
import { useMediaQuery } from "react-responsive"

import styles from "styles/Board.module.scss"
import { RevealButton } from "./RevealButton"

type IProps = {
  board: IBoard
  playerBoard?: IPlayerBoard
  isPrimary: boolean
}

export const Board: React.FunctionComponent<IProps> = (props: IProps) => {
  const isVertical = useMediaQuery({
    query: "(max-width: 600px)"
  })

  const primaryClass = props.isPrimary ? styles.primary : styles.secondary
  const obscuredClass = props.board.obscured ? styles.obscured : styles.revealed

  let shapeClass: string
  switch (props.board.shape) {
    case BoardShape.SQUARE:
      shapeClass = styles.square
      break
    case BoardShape.HEXAGON:
      shapeClass = isVertical ? styles.hexagonVert : styles.hexagonHorz
      break
  }

  return (
    <div className={classNames(styles.board, primaryClass, obscuredClass, shapeClass)}>
      {props.isPrimary && props.board.obscured &&
        <RevealButton/>
      }
      {props.board.spaces.map(spc => {
        const marking: IPlayerBoardMarking | undefined =
          props.playerBoard?.markings.find(pbm => pbm.space_id === spc.space_id)
        const win: boolean = !!(props.playerBoard?.win?.find(n => n === spc.space_id))
        const editable = !!(props.playerBoard && !props.board.obscured && props.isPrimary)

        return (
          <Space
            key={spc.space_id}
            space={spc}
            shape={props.board.shape}
            marking={marking}
            winning={win}
            obscured={props.board.obscured}
            editable={editable}
            isPrimary={props.isPrimary}
            isVertical={isVertical}
          />
        )
      })}
    </div>
  )
}
