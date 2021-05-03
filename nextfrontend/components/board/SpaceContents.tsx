import Tippy from "@tippyjs/react"
import classNames from "classnames"

import { ISpace } from "interface/ISpace"
import React from "react"

import styles from "styles/Board.module.scss"
import "tippy.js/animations/shift-away.css"

type IProps = {
  obscured: boolean
  space: ISpace
  isPrimary: boolean
}

export const SpaceContents: React.FunctionComponent<IProps> = (props) => {
  const autoAStyle = props.space.auto && styles.goalAutoA
  const textSizeStyle = (props.space.text.length > 32) && styles.small

  const goalTooltipText = (props.isPrimary && !props.obscured) && props.space.tooltip
  const hasTooltipStyle = goalTooltipText && styles.hasTooltip

  return (
    <div className={styles.spaceContents}>
      {/* Goal tooltip */}
      {goalTooltipText &&
        <Tippy content={goalTooltipText}>
          <div className={styles.goalTooltip}>
            ?
          </div>
        </Tippy>
      }

      {/* Goal text */}
      <div className={classNames(styles.goalText, textSizeStyle, hasTooltipStyle)}>
        {props.space.text}
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
