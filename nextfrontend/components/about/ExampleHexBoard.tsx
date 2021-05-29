import React from "react"
import { useToast } from "@chakra-ui/react"
import { useMediaQuery } from "react-responsive"

import { getBoardWinHex } from "hex_win_detection"

import { Color } from "interface/IPlayerBoard"
import { Board, BoardOrientation } from "components/board/Board"
import { Space } from "components/board/Space"

import boardStyles from "styles/Board.module.scss"

type Position = {x: number, y: number}

type InitialMarking = Position & {
  marked?: boolean  // defaults true
  outlineColor?: string
}

type Marking = InitialMarking & {
  marked: boolean
}

type ExampleBoardState = {
  markings: Marking[]
}

export type ExampleBoardProps = {
  markColor: Color
  orientation: BoardOrientation
  initialMarkings: InitialMarking[]
  className?: string
}

export const ExampleHexBoard: React.FC<ExampleBoardProps> = (props) => {
  const [ state, setState ] = React.useState<ExampleBoardState>(getInitialState(props.initialMarkings))
  const isTapOnly = useMediaQuery({
    query: "(hover: none)"
  })

  const toast = useToast()
  const showResetNote = () => {
    const resetNote = {
      description: (isTapOnly ? "Long-press" : "Right-click") + " any board to restore it back to the example.",
      duration: 5000,
      isClosable: true,
    }
    // @ts-ignore
    if (!window.bingoToasted && props.initialMarkings.length > 0) {
      toast(resetNote)
      // @ts-ignore
      window.bingoToasted = true
    }
  }

  const winners = getBoardWinHex(state.markings.filter(m => m.marked))

  return (
    <Board className={props.className} orientation={props.orientation}>
      {state.markings.map(m =>
        <Space
          key={keyOf(m)}
          position={{x: m.x, y: m.y}}
          orientation={props.orientation}
          onClick={() => {toggleMark(m, setState); showResetNote()}}
          onContext={() => setState(getInitialState(props.initialMarkings))}
          colorClass={getColorClass(m, winners, props.markColor)}
          borderColor={m.outlineColor}
        />
      )}
    </Board>
  )
}

const toggleMark = (
  pos: Position,
  setState: React.Dispatch<React.SetStateAction<ExampleBoardState>>,
) => {
  // Holy moly, I need immutable.js
  setState(s => ({...s, markings: s.markings.map(m => {
    if (pos.x === m.x && pos.y === m.y) {
      return {...m, outlineColor: undefined, marked: !m.marked}
    } else return {...m, outlineColor: undefined}
  })}))
}

const getInitialState = (initialMarkings: InitialMarking[]): ExampleBoardState => {
  return {
    markings: standardHexPositions().map<Marking>(pos => {
      /* If the caller specified an initial marking in props, use that marking. */
      const initialMarking = initialMarkings.find(im => im.x === pos.x && im.y === pos.y)
      if (initialMarking !== undefined) {
        /* If the caller specified an initial marking, but did not specify marked true/false, assume
         * that they want to mark that space as Complete. */
        return {...initialMarking, marked: initialMarking.marked === undefined ? true : initialMarking.marked}
      } else {
        return {...pos, marked: false}
      }
    })
  }
}

const getColorClass = (marking: Marking, winners: Position[], color: Color) => {
  const isWin = !!winners.find(w => w.x === marking.x && w.y === marking.y)
  if (isWin) {
    return boardStyles.winning
  }
  if (marking.marked) {
    return boardStyles["mark-" + color]
  } else {
    return boardStyles["mark-0"]
  }
}

const standardHexPositions = (): Position[] => {
  return [
    {x: 1, y: 0}, {x: 2, y: 0}, {x: 3, y: 0}, {x: 4, y: 0},
    {x: 0, y: 1}, {x: 1, y: 1}, {x: 2, y: 1}, {x: 3, y: 1}, {x: 4, y: 1},
    {x: -1, y: 2}, {x: 0, y: 2}, {x: 1, y: 2}, {x: 2, y: 2}, {x: 3, y: 2}, {x: 4, y: 2},
    {x: -1, y: 3}, {x: 0, y: 3}, {x: 1, y: 3}, {x: 2, y: 3}, {x: 3, y: 3},
    {x: -1, y: 4}, {x: 0, y: 4}, {x: 1, y: 4}, {x: 2, y: 4},
  ]
}

const keyOf = (pos: Position) => {
  return pos.x * 1000 + pos.y
}
