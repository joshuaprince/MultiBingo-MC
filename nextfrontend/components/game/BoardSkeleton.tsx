import React from "react"
import { Spinner } from "@chakra-ui/react"

import styles from "styles/Game.module.scss"

export const BoardSkeleton: React.FunctionComponent = () => {
  return (
    <Spinner size="xl" className={styles.boardSkeleton}/>
  )
}
