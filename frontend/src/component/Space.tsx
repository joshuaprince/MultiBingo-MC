import React from "react";
import Tippy from "@tippyjs/react";

import { ISpace } from "../interface/ISpace";
import { sendMarkBoard } from "../api";
import { Color } from "../interface/IPlayerBoard";
import { IPosition } from "../interface/IPosition";
import { BoardShape } from "../interface/IBoard";

type IProps = {
  obscured: boolean;
  space: ISpace;
  shape: BoardShape;
  marking?: Color;
  isPrimary: boolean;
}

export const Space: React.FunctionComponent<IProps> = (props: IProps) => {
  const onMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();

    if (props.obscured) {
      return;
    }

    const isRightClick = e.button === 2;
    const newMarking = isRightClick ? 0 : ((props.marking || 0) + 1) % Color.__COUNT;

    sendMarkBoard(props.space.space_id, newMarking);

    return false;
  }

  const onContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
  }

  const isAutoactivated = props.space.auto;
  const smallText = props.space.text.length > 40;
  const wholeSpaceTooltip = props.isPrimary ? undefined : props.space.text;
  const innerTooltip = props.isPrimary ? props.space.tooltip : undefined;

  const spaceDiv = (
    <div className={"bingo-space mark-" + (props.marking || Color.UNMARKED)}
         style={getPositionStyle(props.space.position, props.shape)}
         onMouseDown={onMouseDown} onContextMenu={onContextMenu}>
      <div className={"bingo-space-content primary-only"}>
        {innerTooltip &&
          <Tippy content={innerTooltip}>
            <div className="bingo-tooltip">?</div>
          </Tippy>}
          <span className={"bingo-text" + (smallText ? " small" : "")}>
            {props.space.text}
          </span>
        {isAutoactivated &&
          <Tippy content={"This space will be activated automatically."}>
            <div className="bingo-auto-tip">A</div>
          </Tippy>}
      </div>
    </div>
  );

  if (wholeSpaceTooltip) {
    return (<Tippy content={wholeSpaceTooltip}>{spaceDiv}</Tippy>)
  } else {
    return spaceDiv;
  }
}

const getPositionStyle = (pos: IPosition, shape: BoardShape): React.CSSProperties => {
  if (shape === BoardShape.SQUARE) {
    return {gridColumn: pos.x + 1, gridRow: pos.y + 1};
  }

  if (shape === BoardShape.HEXAGON) {
    const colStart = (pos.x * 2) + (pos.y % 2) + (pos.y >> 1) * 2 + 1;
    const rowStart = (pos.y * 3) + 1;

    let err = false;
    if (colStart <= 0 || rowStart <= 0) {
      console.error(`Hex position (${pos.x}, ${pos.y}) calculated invalid grid position (${colStart}, ${rowStart})`);
      err = true;
    }

    return {
      background: err ? "red" : undefined,
      gridColumnStart: colStart,
      gridColumnEnd: "span 2",
      gridRowStart: rowStart,
      gridRowEnd: "span 4"
    };
  }

  throw new Error("Unknown board shape: " + shape);
}
