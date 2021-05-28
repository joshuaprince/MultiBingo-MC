import React from "react"
import { Spinner } from "@chakra-ui/react"
import classNames from "classnames"
import Tippy, { TippyProps } from "@tippyjs/react"

import { IPosition } from "interface/IPosition"

import { BoardOrientation } from "./Board"

import styles from "styles/Board.module.scss"
import "tippy.js/animations/shift-away.css"


type IProps = {
  /**
   * The position to display this Space on the board. This should be unique on a single Board.
   * @see {@link /backend/websockets_api.md} for information about how Hexagonal positions work.
   */
  position: { x: number, y: number }

  /**
   * Shape and direction to display the board.
   */
  orientation: BoardOrientation

  /**
   * A CSS class name that will be added to this Space div, intended for setting its color only.
   */
  colorClass?: string

  /**
   * If set, this Space will be rendered with a border of the specified color.
   */
  borderColor?: string

  /**
   * Action to take when this Space is clicked. When this is defined, the Space will also be
   * rendered with special "clickable" styles, such as a pointer cursor and fade on mouseover.
   */
  onClick?: () => void

  /**
   * Action to take when this Space is right-clicked, or tap-held on touch screens.
   */
  onContext?: () => void

  /**
   * Defines a node that will be given to {@link Tippy} as a tooltip attached to the entire Space.
   * If undefined, the Space will not have a tooltip or hover action.
   */
  wholeSpaceTooltip?: React.ReactNode

  /**
   * Extra props passed to the Tippy node when {@link wholeSpaceTooltip} is set. Ignored if this
   * space has no tooltip.
   */
  wholeSpaceTooltipProps?: TippyProps

  /**
   * If true, this space will be rendered with a spinner that indicates that a change to its
   * contents may be in flight.
   */
  changePending?: boolean
}

export const Space: React.FunctionComponent<IProps> = (props) => {
  const borderStyle = props.borderColor && styles.bordered
  const clickableStyle = props.onClick && styles.clickable

  const onContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
    props.onContext && props.onContext()
    return false
  }

  const inlineStyles = {
    ...calculateGridPosition(props.position, props.orientation),
    "--border-color": props.borderColor
  }

  const spaceDiv = (
    <div
      className={classNames(styles.space, props.colorClass, borderStyle, clickableStyle)}
      style={inlineStyles}
      onClick={props.onClick}
      onContextMenu={onContextMenu}
    >
      <div className={styles.spaceInner}>
        <Spinner hidden={!props.changePending} className={styles.pendingChangeSpinner} size="sm"/>
        {props.children}
      </div>
    </div>
  )

  if (props.wholeSpaceTooltip) {
    return (
      <Tippy
        content={props.wholeSpaceTooltip}
        // interactiveBorder not working?
        // interactiveBorder={props.shape.includes("hexagon") ? 300 : undefined}
        {...props.wholeSpaceTooltipProps}
      >
        {spaceDiv}
      </Tippy>
    )
  } else return spaceDiv
}

const calculateGridPosition = (pos: IPosition, orientation: BoardOrientation): React.CSSProperties => {
  switch (orientation) {
    case "square":
      return {gridColumn: pos.x + 1, gridRow: pos.y + 1}
    case "hexagon-horizontal":
    case "hexagon-vertical":
      const colStart = (pos.x * 2) + (pos.y % 2) + (pos.y >> 1) * 2 + 1
      const rowStart = (pos.y * 3) + 1

      let err = false
      if (colStart <= 0 || rowStart <= 0) {
        console.error(`Hex position (${pos.x}, ${pos.y}) calculated invalid grid position (${colStart}, ${rowStart})`)
        err = true
      }

      if (orientation == "hexagon-horizontal") {
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
