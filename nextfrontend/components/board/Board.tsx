import React from "react"
import { Button } from "@chakra-ui/react"
import classNames from "classnames"

import styles from "styles/Board.module.scss"

export type BoardOrientation = "square" | "hexagon-horizontal" | "hexagon-vertical"

type IProps = {
  /**
   * Shape and direction to display the board.
   */
  orientation: BoardOrientation

  /**
   * If true, the board will be blurred and display a "Reveal" button if `onRevealButton` is also
   * set.
   */
  isObscured: boolean

  /**
   * If set, displays a "Reveal" button in the center of the board that calls this callback when
   * the button is clicked.
   */
  onRevealButton?: () => void

  /**
   * Extra CSS class to add to the Board div.
   */
  className?: string
}

export const Board: React.FunctionComponent<IProps> = (props) => {
  const obscuredClass = props.isObscured ? styles.obscured : styles.revealed

  const shapeClass = (() => {
    if (props.orientation === "square") {
      return styles.square
    } else if (props.orientation === "hexagon-vertical") {
      return styles.hexagonVert
    } else {
      return styles.hexagonHorz
    }
  })()

  return (
    <div className={classNames(styles.board, props.className, obscuredClass, shapeClass)}>
      {props.isObscured && props.onRevealButton &&
        <Button
          size="lg"
          colorScheme="blue"
          className={styles.revealButton}
          onClick={props.onRevealButton}
        >
          Reveal Board
        </Button>
      }
      {props.children}
    </div>
  )
}
