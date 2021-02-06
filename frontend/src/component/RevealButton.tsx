import React from 'react';
import { sendRevealBoard } from "../api";

export const RevealButton: React.FunctionComponent = () => {
  return (
    <div className="reveal-button" onClick={sendRevealBoard}>
      Reveal Board
    </div>
  );
}
