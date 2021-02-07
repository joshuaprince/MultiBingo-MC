import React from "react";
import Tippy from "@tippyjs/react";

import { ISquare } from "../interface/ISquare";
import { sendMarkBoard } from "../api";
import { Marking } from "../interface/IPlayerBoard";

type IProps = {
  obscured: boolean;
  square: ISquare;
  marking?: Marking;
  isPrimary: boolean;
}

export const Square: React.FunctionComponent<IProps> = (props: IProps) => {
  const onMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();

    if (props.obscured) {
      return;
    }

    const isRightClick = e.button === 2;
    const newMarking = isRightClick ? 0 : ((props.marking || 0) + 1) % Marking.__COUNT;

    sendMarkBoard(props.square.position, newMarking);

    return false;
  }

  const onContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
  }

  const isAutoactivated = props.square.auto;
  const smallText = props.square.text.length > 40;
  const wholeSquareTooltip = props.isPrimary ? undefined : props.square.text;
  const innerTooltip = props.isPrimary ? props.square.tooltip : undefined;

  const squareDiv = (
    <div className={"bingo-square mark-" + (props.marking || Marking.UNMARKED)}
         onMouseDown={onMouseDown} onContextMenu={onContextMenu}>
      <div className={"bingo-text primary-only" + (smallText ? " small" : "")}>
        {innerTooltip &&
          <Tippy content={innerTooltip}>
            <div className="bingo-tooltip">?</div>
          </Tippy>}
        {props.square.text}
        {isAutoactivated &&
          <Tippy content={"This square will be activated automatically."}>
            <div className="bingo-auto-tooltip">A</div>
          </Tippy>}
      </div>
    </div>
  );

  if (wholeSquareTooltip) {
    return (<Tippy content={wholeSquareTooltip}>{squareDiv}</Tippy>)
  } else {
    return squareDiv;
  }
}
