/**
 * This is a direct port of the Backend's `wd_hex_snake.py` win detector. It is intended to run in
 * the user's browser for example purposes only. In actual games, win detection is performed and
 * communicated by the backend, not in the browser.
 *
 * TODO: This port also does not implement "clumped" (neighborless=False) win detection; it always
 *       disallows wins that include a clump.
 */

type Position = {x: number, y: number}

const WIN_LENGTH = 6
const DEPTH_LIMIT = 6

export const getBoardWinHex = (markings: Position[]) => {
  const longest = longestChain([], markings)
  if (longest.length >= WIN_LENGTH) {
    return longest
  } else {
    return []
  }
}

const longestChain = (current: Position[], candidates: Position[]) => {
  let nextNodeChoices: Position[]
  if (current.length === 0) {
    nextNodeChoices = candidates
  } else {
    if (current.length < 2) {
      nextNodeChoices = neighbors(current[current.length - 1], candidates)
    } else {
      const nbrsTwoAgo = neighbors(current[current.length - 2], candidates)
      nextNodeChoices = neighbors(current[current.length - 1], candidates)
        .filter(n => !nbrsTwoAgo.find(t => t.x === n.x && t.y === n.y))
    }
  }

  if (current.length >= DEPTH_LIMIT) {
    return current
  }

  let longest = current
  for (const neigh of nextNodeChoices) {
    const newChain = [...current, neigh]
    const newCandidates = candidates.filter(c => !eq(c, neigh))
    const longestFrom = longestChain(newChain, newCandidates)
    if (longestFrom.length > longest.length) {
      longest = longestFrom
    }
    if (longest.length >= DEPTH_LIMIT) {
      break
    }
  }

  return longest
}

/**
 * Get a list of neighbors to a specific hexagon that occur in `candidates`.
 */
const neighbors = (of: Position, candidates: Position[]) => {
  return candidates.filter(c => isNeighbor(of, c))
}

const isNeighbor = (left: Position, right: Position) => {
  const leftZ = -(left.x + left.y)
  const rightZ = -(right.x + right.y)
  if (left.x === right.x) {
    return Math.abs(left.y - right.y) === 1
  }
  if (left.y === right.y) {
    return Math.abs(leftZ - rightZ) === 1
  }
  if (leftZ === rightZ) {
    return Math.abs(left.x - right.x) === 1
  }
}

const eq = (left: Position, right: Position) => {
  return left.x === right.x && left.y === right.y
}
