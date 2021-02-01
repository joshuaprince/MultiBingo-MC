import React from 'react';

export const PlayerNameInput: React.FunctionComponent = () => {
  const onKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      const playerName = e.currentTarget.value;
      window.location.search = new URLSearchParams({"name": playerName}).toString();
    }
  };

  return (
    <input className="player-name-input"
           type="text"
           placeholder="Enter your name to join..."
           onKeyUp={onKeyUp}
    />
  );
}
