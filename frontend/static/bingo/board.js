
const boardSocket = new WebSocket(
  'ws://' + window.location.host + '/ws/board/' + game_code +
  '/' + player_name + (player_name ? '/' : ''));
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

document.querySelectorAll('.board-primary .bingo-square').forEach(square => {
  square.onmousedown = e => {
    e.preventDefault();
    const rex_sq = /square-(\d+)/;
    const pos = parseInt(rex_sq.exec(e.currentTarget.className)[1]);

    const rex_mark = /mark-(\d+)/;
    const rex_mark_result = rex_mark.exec(e.currentTarget.className);
    const curr_mark = rex_mark_result ? parseInt(rex_mark_result[1]) : 0;

    // right click to unmark, left click to cycle colors
    const target_mark = e.which === 3 ? 0 : (curr_mark + 1) % num_mark_colors;

    boardSocket.send(JSON.stringify({
      action: "board_mark",
      position: pos,
      to_state: target_mark,
    }));

    return false;
  };
  square.oncontextmenu = e => {
    e.preventDefault();
    return false;
  };
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

/* Player name input */
const playerNameInputNode = document.querySelector('input#player-name-input');
if (playerNameInputNode) {
playerNameInputNode.onkeyup = function (e) {
    if (e.keyCode === 13) {  // enter, return
      const playerName = e.target.value;
      window.location = window.location + '?name=' + playerName;
    }
  };
}
