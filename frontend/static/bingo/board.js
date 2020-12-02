if (!player_name) {
  const new_player_name = prompt("Enter your name:");
  window.location = window.location + '?name=' + new_player_name;
}

const boardSocket = new WebSocket(
  'ws://' + window.location.host + '/ws/board/' + game_code + '/' + player_name);
boardSocket.onmessage = e => {
  const data = JSON.parse(e.data);
  build_secondary_boards(data);
  mark_all_boards(data);
}

boardSocket.onclose = e => {
  console.error("Board socket was closed: " + e);
  alert("Lost connection to server. Click OK to refresh the page.");
  location.reload();
}

document.querySelectorAll('.board-primary .bingo-square').forEach(square => square.onclick = e => {
  const rex = /square-(\d+)/;
  const pos = parseInt(rex.exec(e.currentTarget.className)[1]);
  boardSocket.send(JSON.stringify({
    action: "board_mark",
    position: pos,
    to_state: 1
  }))
});

function build_secondary_boards(data) {
  const template = document.querySelector("#board-secondary-template");
  const anchor = document.querySelector(".boards-secondary");

  while (anchor.firstChild) {
    anchor.removeChild(anchor.firstChild);
  }

  for (const pboard of data) {
    if (pboard['player_name'] === player_name) {
      continue;
    }

    const clone = template.content.cloneNode(true);
    clone.querySelector('.board-secondary').classList.add('board-name-' + pboard['player_name']);
    clone.querySelector('.player-name').innerHTML = pboard['player_name'];
    clone.querySelector('.board-secondary').classList.toggle('disconnected', !!pboard['disconnected_at'])
    anchor.appendChild(clone);
  }
}

function mark_all_boards(data) {
  for (pboard of data) {
    const board_node = document.querySelector('.board-name-' + pboard['player_name']);
    mark_board(pboard['board'], board_node);
  }
}

function mark_board(squares, node) {
  for (let i = 0; i < 25; i++) {
    const sq = node.querySelector('.bingo-square.square-' + i);
    // clear current classList
    sq.classList.forEach(className => {
      if (className.startsWith('mark-')) {
        sq.classList.remove(className);
      }
    });
    sq.classList.add('mark-' + squares.charAt(i));
  }
}
