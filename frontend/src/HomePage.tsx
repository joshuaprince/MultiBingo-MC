import React from "react";

import logo from './style/logo.png';

export const HomePage: React.FunctionComponent = () => {
  const submit = () => {
    const playerName = document.querySelector<HTMLInputElement>('#player-name-input')?.value;
    let roomName = document.querySelector<HTMLInputElement>('#room-name-input')?.value;
    if (!roomName) {
      roomName = randomGameCode();
    }
    window.location.href = '/game/' + roomName + (playerName ? ('?name=' + playerName) : '');
  }

  return (
    <div className="homepage" onKeyUp={e => e.key === "Enter" && submit}>
      <img className="logo" src={logo} alt={"Minecraft Bingo"}/>
      <div className="player-name">
        <label htmlFor="player-name-input">Your Name:</label><br/>
        <input id="player-name-input" type="text"/><br/>
      </div>
      <div className="room-name">
        <label htmlFor="room-name-input">Room Code:</label><br/>
        <input id="room-name-input" type="text" placeholder="Leave blank for random"/><br/>
      </div>
      <input id="room-name-submit" type="button" value="Enter" onClick={submit}/>
    </div>
  );
}

const randomGameCode = () => {
  let a: string[] = [];
  for (let i = 0; i < 6; i++) {
    a.push(String.fromCharCode(65+Math.floor(Math.random() * 26)));
  }
  return a.join("");
}
