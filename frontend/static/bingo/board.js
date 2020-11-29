const gameCode = document.querySelector('#game-code').innerHTML;
const boardSocket = new WebSocket('ws://' + window.location.host + '/ws/board/' + gameCode);
boardSocket.onmessage = e => {
  const data = JSON.parse(e.data);
  for (const pboard of data) {
    const squares = pboard['board'];
    for (let i = 0; i < squares.length; i++) {
      const sq = document.querySelector('#square-' + i.toString());
      sq.classList.add('mark-' + squares.charAt(i));
    }
  }
}
boardSocket.onclose = e => {
  console.error("Board socket was closed: " + e);
}

document.querySelectorAll('.bingo-square').forEach(square => square.onclick = e => {
  const rex = /square-(\d+)/;
  const pos = parseInt(rex.exec(e.currentTarget.id)[1]);
  boardSocket.send(JSON.stringify({
    player_name: "HardcodedPlayer",
    action: "board_mark",
    position: pos,
    to_state: 1
  }))
});
