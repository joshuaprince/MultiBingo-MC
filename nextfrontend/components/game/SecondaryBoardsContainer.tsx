import { IBoard } from "interface/IBoard"
import { IPlayerBoard } from "interface/IPlayerBoard"
import React from "react"

import styles from "styles/Game.module.scss"
import { BoardContainer } from "./BoardContainer"

type IProps = {
  gameCode: string,
  board: IBoard,
  playerBoards: IPlayerBoard[],
}

export const SecondaryBoardsContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  return (
    <div className={styles.bcAllSecondary}>
      {props.playerBoards.map(pb => (
      <div key={pb.player_id}>
        <BoardContainer isPrimary={false} gameCode={props.gameCode} board={props.board} playerBoard={pb}/>
      </div>
      ))}
    </div>
  )
}
