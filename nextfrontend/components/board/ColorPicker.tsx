import classNames from "classnames"
import React from "react"

import styles from "styles/ColorPicker.module.scss"

import { $enum } from "ts-enum-util"
import { sendMarkBoard } from "../../api"
import { Color } from "../../interface/IPlayerBoard"

type IProps = {
  spaceId: number
  onClick?: () => void
  className?: string
}

export const ColorPicker: React.FunctionComponent<IProps> = (props: IProps) => {
  const onClick = (e: React.MouseEvent, newMarking: number) => {
    e.preventDefault()
    e.stopPropagation()
    props.onClick?.()
    sendMarkBoard({
      space_id: props.spaceId,
      color: newMarking,
    })
    return false
  }

  return (
    <>
      <p>Set Space Color:</p>
      <div className={classNames(styles.colorPicker, props.className)}>
        {$enum(Color).map(color => (
          <div
            key={color}
            className={styles["mark-" + color]}
            onClick={(e) => onClick(e, color)}
          />
        ))}
      </div>
    </>
  )
}
