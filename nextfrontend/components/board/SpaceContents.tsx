import React from "react"
import classNames from "classnames"
import Tippy from "@tippyjs/react"

import styles from "styles/Board.module.scss"
import "tippy.js/animations/shift-away.css"

type IProps = {
  /**
   * Contents to display front-and-center in the Space.
   */
  text: string

  /**
   * If set, the Space will contain a "?" icon that indicates that there is extra information about
   * this space. This extra text will be displayed on hover of the "?".
   */
  tooltipText?: string

  /**
   * If true, the Space will contain a small, light-colored "A" that suggests on hover that marking
   * of the Space is being automated.
   */
  showAutomatedA?: boolean

  /**
   * If true, all space contents will be blurred.
   */
  obscured: boolean
}

/**
 * Defines most elements that are displayed within the boundaries of a Space, such as text.
 * This element should be passed as a child to {@link Space}.
 * Aspects such as color, border, and click events are defined in {@link Space}.
 */
export const SpaceContents: React.FunctionComponent<IProps> = (props) => {
  const autoAStyle = props.showAutomatedA && styles.goalAutoA
  const textSizeStyle = (props.text.length > 32) && styles.small
  const tooltipStyle = props.tooltipText && styles.hasTooltip

  return (
    <div className={styles.spaceContents}>
      {/* Goal tooltip */}
      {props.tooltipText &&
        <Tippy content={props.tooltipText}>
          <div className={styles.goalTooltip}>
            ?
          </div>
        </Tippy>
      }

      {/* Goal text */}
      <div className={classNames(styles.goalText, textSizeStyle, tooltipStyle)}>
        {props.text}
      </div>

      {/* Auto-activation indicator "A" */}
      {autoAStyle &&
        <Tippy content={"This space will be marked automatically when you complete the objective in-game."}>
          <div className={autoAStyle}>
            A
          </div>
        </Tippy>
      }
    </div>
  )
}
