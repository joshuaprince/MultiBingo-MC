import React from "react"

import { IBoard } from "interface/IBoard"
import { IPlayerBoard } from "interface/IPlayerBoard"

import { BoardOrientation } from "components/board/Board"

import { SecondaryBoardContainer } from "./SecondaryBoardContainer"

import styles from "styles/Game.module.scss"

type IProps = {
  board: IBoard,
  playerBoards: IPlayerBoard[],
  orientation: BoardOrientation
}

export const SecondaryBoardsAllContainer: React.FunctionComponent<IProps> = (props: IProps) => {
  return (
    <div className={styles.bcAllSecondary}>
      {props.playerBoards.map(pb => (
      <div key={pb.player_id}>
        <SecondaryBoardContainer board={props.board} playerBoard={pb} orientation={props.orientation}/>
      </div>
      ))}
    </div>
  )
}
