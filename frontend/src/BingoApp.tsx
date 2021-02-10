import React from "react";

import "tippy.js/dist/tippy.css";
import "./style/look.scss";
import "./style/structure.scss";
import "./style/homepage.scss";

import { BingoGame } from "./BingoGame";
import { HomePage } from "./HomePage";

export type IBingoAppState = {
  gameCode?: string;
  playerName?: string;
}

export const BingoApp: React.FunctionComponent = () => {
  const [state] = React.useState<IBingoAppState>(getInitialState);

  if (state.gameCode) {
    return (<BingoGame gameCode={state.gameCode} playerName={state.playerName}/>)
  } else {
    return <HomePage/>
  }
}

const getInitialState: (() => IBingoAppState) = () => {
  // Maybe someday, I'll use React Router to make this better.
  const re = /game\/(\w+)/;
  const gameCode = re.exec(window.location.pathname)?.[1] || undefined;

  const params = new URLSearchParams(window.location.search);
  const name = params.get("name") || undefined;
  if (name) {
    params.delete("name");
    // window.history.pushState(null, "", window.location.pathname)
    // window.location.search = params.toString();
  }

  return {
    gameCode: gameCode,
    playerName: name,
  };
}
