import React from "react";
import { Color } from "../interface/IPlayerBoard";
import { $enum } from "ts-enum-util";
import { sendMarkBoard } from "../api";

type IProps = {
  spaceId: number;
}

export const ColorPickerTooltip: React.FunctionComponent<IProps> = (props: IProps) => {
  const onMouseDown = (e: React.MouseEvent, newMarking: number) => {
    e.preventDefault();
    sendMarkBoard({
      space_id: props.spaceId,
      color: newMarking,
    });
    return false;
  };

  return (
    <div className="color-picker">
      {$enum(Color).map(color => (
        <div key={color}
          onMouseDown={(e) => onMouseDown(e, color)}
          className={"mark-" + color}
        />
      ))}
    </div>
  );
}
