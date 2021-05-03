import { Board } from "components/board/Board"
import { PlayerNameInput } from "components/game/PlayerNameInput"

import { IBoard } from "interface/IBoard"
import { IPlayerBoard } from "interface/IPlayerBoard"
import React from "react"

import styles from "styles/Game.module.scss"

type IProps = {
  isPrimary: boolean,
  gameCode: string,
  board: IBoard,
  playerBoard?: IPlayerBoard,
}

export const BoardContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  const containerStyle = props.isPrimary ? styles.bcPrimary : styles.bcSecondary

  let nameDisplay
  if (props.playerBoard) {
    nameDisplay = <h2>{props.playerBoard.player_name}</h2>
  } else {
    nameDisplay = <PlayerNameInput gameCode={props.gameCode}/>
  }

  return (
    <div className={containerStyle}>
      <Board
        board={props.board}
        playerBoard={props.playerBoard}
        isPrimary={props.isPrimary}
      />

      <div className={styles.boardName}>
        {nameDisplay}
      </div>
    </div>
  )
}
