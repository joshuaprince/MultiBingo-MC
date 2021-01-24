// Get variables from Django json_script tags in template
const game_code = JSON.parse(document.getElementById('game_code').textContent);
const num_mark_colors = JSON.parse(document.getElementById('num_mark_colors').textContent);
const player_id = JSON.parse(document.getElementById('player_id').textContent);
const player_name = JSON.parse(document.getElementById('player_name').textContent);

/* WebSocket stuff */
let boardSocket;
const connectingPopup = document.getElementsByClassName('connecting-popup')[0];
function connect() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const wsUrl = protocol + '://'
      + window.location.host + '/ws/board/'
      + game_code + (player_name ? ('/' + player_name) : '');

  boardSocket = new WebSocket(wsUrl);

  boardSocket.onmessage = e => {
    const data = JSON.parse(e.data);

    const obscured = data['obscured'];
    const pboards = data['pboards'];

    if (!obscured) {
      reveal_boards();
    }
    build_secondary_boards(pboards);
    mark_all_boards(pboards);
  }

  boardSocket.onopen = e => {
    connectingPopup.hidden = true;
  }

  boardSocket.onclose = e => {
    console.log("Board socket was closed, retrying in 1 second. ");
    setTimeout(connect, 1000);
    connectingPopup.hidden = false;
  }

  boardSocket.onerror = e => {
    console.error("Board socket encountered error: " + e.message);
    boardSocket.close();
  }
}

connect();

/* Board marking stuff */
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
    clone.querySelector('.board-secondary').classList.add('board-player-' + pboard['player_id']);
    clone.querySelector('.player-name').innerHTML = pboard['player_name'];
    clone.querySelector('.board-secondary').classList.toggle('disconnected', !!pboard['disconnected_at'])
    anchor.appendChild(clone);
  }

  update_tooltips();
}

function mark_all_boards(data) {
  for (pboard of data) {
    const board_node = document.querySelector('.board-player-' + pboard['player_id']);
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

function reveal_boards() {
  document.querySelector('.reveal-controls')?.remove();
  document.querySelectorAll('.obscured').forEach(e => {
    e.classList.remove('obscured');
    e.classList.add('obscurable');
  });
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

/* Reveal button */
const revealButtonNode = document.querySelector('div.reveal-controls');
if (revealButtonNode) {
  revealButtonNode.onclick = function (e) {
    boardSocket.send(JSON.stringify({
      action: "reveal_board",
    }));
  };
}

/* Tooltips */
function update_tooltips() {
  /* disable if the board is obscured */
  const disable = !!document.querySelector('.board-primary.obscured');
  tippy('.bingo-tooltip', {
    content: reference => reference.getAttribute('data-tooltip'),
    trigger: disable ? 'manual' : undefined,
  });
}
update_tooltips();
