import React from "react";
import Tippy from "@tippyjs/react";

import { ISpace } from "../interface/ISpace";
import { sendMarkBoard } from "../api";
import { Color } from "../interface/IPlayerBoard";

type IProps = {
  obscured: boolean;
  space: ISpace;
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
         style={{gridColumn: props.space.position.x+1, gridRow: props.space.position.y+1}}
         onMouseDown={onMouseDown} onContextMenu={onContextMenu}>
      <div className={"bingo-text primary-only" + (smallText ? " small" : "")}>
        {innerTooltip &&
          <Tippy content={innerTooltip}>
            <div className="bingo-tooltip">?</div>
          </Tippy>}
        {props.space.text}
        {isAutoactivated &&
          <Tippy content={"This space will be activated automatically."}>
            <div className="bingo-auto-tooltip">A</div>
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
