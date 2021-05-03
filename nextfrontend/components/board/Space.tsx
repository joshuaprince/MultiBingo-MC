import { Spinner } from "@chakra-ui/react"
import Tippy from "@tippyjs/react"

import { sendMarkBoard } from "api"
import classNames from "classnames"
import { ColorPicker } from "components/board/ColorPicker"
import { SpaceContents } from "components/board/SpaceContents"
import { BoardShape } from "interface/IBoard"
import { Color, IPlayerBoardMarking } from "interface/IPlayerBoard"
import { IPosition } from "interface/IPosition"
import { ISpace } from "interface/ISpace"
import React from "react"

import styles from "styles/Board.module.scss"
import "tippy.js/animations/shift-away.css"
import { ResponsiveContext } from "../game/ResponsiveContext"
import { SpaceTouchModal } from "./SpaceTouchModal"

type IProps = {
  obscured: boolean
  editable: boolean
  space: ISpace
  shape: BoardShape
  marking?: IPlayerBoardMarking
  winning: boolean
  isPrimary: boolean
  isVertical: boolean
}

type IState = {
  modalOpen: boolean
  waitingForMark?: Color
  waitingForCovert?: boolean
}

export const Space: React.FunctionComponent<IProps> = (props) => {
  const { isTapOnly, tapToMark } = React.useContext(ResponsiveContext)
  const [ state, setState ] = React.useState<IState>({modalOpen: false})

  React.useEffect(() => {
    if (state.waitingForMark !== undefined && state.waitingForMark === props.marking?.color) {
      setState(s => ({...s, waitingForMark: undefined}))
    }
    if (state.waitingForCovert !== undefined && state.waitingForCovert === props.marking?.covert_marked) {
      setState(s => ({...s, waitingForCovert: undefined}))
    }
  })

  const doMark = () => {
    const colorTo = nextColor(props.marking?.color)
    setState(s => ({...s, waitingForMark: colorTo}))
    sendMarkBoard({
      space_id: props.space.space_id,
      color: colorTo,
    })
  }

  const doCovertMark = () => {
    const covertTo = !props.marking?.covert_marked

    setState(s => ({...s, waitingForCovert: covertTo}))
    sendMarkBoard({
      space_id: props.space.space_id,
      covert_marked: covertTo
    })
  }

  const onClick = () => {
    if (!props.editable) {
      return false
    }

    if (tapToMark) {
      doMark()
    } else {
      doCovertMark()
    }

    return false
  }

  const onContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
    if (!props.editable) {
       return false
    }

    if (isTapOnly) {
      setState(s => ({...s, modalOpen: true}))
      return false
    } else {
      doCovertMark()
    }
  }

  const wholeSpaceTooltip = (!props.isPrimary && !props.obscured) && props.space.text
  let displayedColor: Color
  if (state.waitingForMark !== undefined) {
    displayedColor = state.waitingForMark
  } else if (props.marking?.color !== undefined) {
    displayedColor = props.marking?.color
  } else {
    displayedColor = Color.UNMARKED
  }

  let displayedCovertMark: boolean = false
  if (state.waitingForCovert !== undefined) {
    displayedCovertMark = state.waitingForCovert
  } else if (props.marking?.covert_marked) {
    displayedCovertMark = props.marking.covert_marked
  }

  const markColorStyle = styles["mark-" + displayedColor]
  const covertMarkedStyle = displayedCovertMark && styles.covertMarked
  const pendingChange =
    (state.waitingForMark !== undefined || state.waitingForCovert !== undefined) && styles.pendingChange
  const winningStyle = props.winning && styles.winning
  const editable = props.editable && styles.editable

  const spaceDiv = (
    <div
      className={classNames(styles.space, markColorStyle, covertMarkedStyle, pendingChange, winningStyle, editable)}
      style={calculateGridPosition(props.space.position, props.shape, props.isVertical)}
      onClick={onClick}
      onContextMenu={onContextMenu}
    >
      {isTapOnly &&
        <SpaceTouchModal
          isOpen={state.modalOpen}
          close={() => setState(s => ({...s, modalOpen: false}))}
          space={props.space}
        />
      }
      <div className={styles.spaceInner}>
        <Spinner hidden={!pendingChange} className={styles.pendingChangeSpinner} size="sm"/>
        <SpaceContents
          obscured={props.obscured}
          space={props.space}
          isPrimary={props.isPrimary}
        />
      </div>
    </div>
  )

  const colorPickTooltip = (
    <ColorPicker className={styles.colorPickerTooltip} spaceId={props.space.space_id}/>
  )

  if (wholeSpaceTooltip) {
    return (
      <Tippy delay={0} interactive={false} content={wholeSpaceTooltip}>{spaceDiv}</Tippy>
    )
  } else if (isTapOnly) {
    return spaceDiv
  } else {
    return (
      <Tippy
        disabled={!props.editable}
        interactive
        delay={[500, 300]}
        animation={'shift-away'}
        content={colorPickTooltip}
      >{spaceDiv}</Tippy>
    )
  }
}

const calculateGridPosition = (pos: IPosition, shape: BoardShape, vertical: boolean): React.CSSProperties => {
  switch (shape) {
    case BoardShape.SQUARE:
      return {gridColumn: pos.x + 1, gridRow: pos.y + 1}
    case BoardShape.HEXAGON:
      const colStart = (pos.x * 2) + (pos.y % 2) + (pos.y >> 1) * 2 + 1
      const rowStart = (pos.y * 3) + 1

      let err = false
      if (colStart <= 0 || rowStart <= 0) {
        console.error(`Hex position (${pos.x}, ${pos.y}) calculated invalid grid position (${colStart}, ${rowStart})`)
        err = true
      }

      if (!vertical) {
        return {
          background: err ? "red" : undefined,
          gridColumnStart: colStart,
          gridColumnEnd: "span 2",
          gridRowStart: rowStart,
          gridRowEnd: "span 4"
        }
      } else {
        return {
          background: err ? "red" : undefined,
          gridColumnStart: rowStart,
          gridColumnEnd: "span 4",
          gridRowStart: colStart,
          gridRowEnd: "span 2"
        }
      }
  }
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
