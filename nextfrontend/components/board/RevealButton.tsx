import { Button } from "@chakra-ui/react"

import { sendRevealBoard } from "api"
import React from "react"

import styles from "styles/Board.module.scss"

export const RevealButton: React.FunctionComponent = () => {
  return (
    <Button
      size="lg"
      colorScheme="blue"
      className={styles.revealButton}
      onClick={sendRevealBoard}
    >
      Reveal Board
    </Button>
  )
}
