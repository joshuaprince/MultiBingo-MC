import React from "react"
import { Container, Flex, Heading, Text } from "@chakra-ui/react"

import { ExampleBoardProps, ExampleHexBoard } from "components/about/ExampleHexBoard"
import { Color } from "interface/IPlayerBoard"

import styles from "../styles/about.module.scss"

export default function About() {
  return (
    <Container maxW={"container.md"} className={styles.container}>
      <Heading size="lg">MultiBingo</Heading>
      <Text>
        MultiBingo is a game where you race to complete goals within your favorite games!
      </Text>

      <Heading size="md">How To Play</Heading>
      <Text>
        The easiest way to start a game is to click the New Game button. There will be more
        information here soon!
      </Text>

      <Heading size="md">Hexagons?</Heading>
      <Text>
        Yes, Hexagons!
      </Text>

      <AboutExampleBoard markColor={Color.COMPLETE} initialMarkings={[]} />

      <Text>
        This isn't an ordinary Bingo board, each space has two extra sides! How are you supposed to
        make a row, column, or diagonal on a board like that?
      </Text>

      <Text>
        <b>You aren't</b> - To win Hexagon Bingo, <b>your markings can curve around the board</b>!
        As long as your markings are in a single chain, they don't have to make a straight
        line. If you're the first to mark <b>6</b> hexes in a chain, you win! <b>These are all
        considered WINS</b>:
      </Text>

      <Flex className={styles.flexy}>
        <AboutExampleBoard markColor={Color.COMPLETE} initialMarkings={[
          {x: 1, y: 1}, {x: 0, y: 2}, {x: 0, y: 3}, {x: 1, y: 3}, {x: 2, y: 3}, {x: 3, y: 2}
        ]} />
        <AboutExampleBoard markColor={Color.COMPLETE} initialMarkings={[
          {x: 1, y: 0}, {x: 2, y: 0}, {x: 3, y: 0}, {x: 4, y: 0}, {x: 4, y: 1}, {x: 4, y: 2}
        ]} />
        <AboutExampleBoard markColor={Color.COMPLETE} initialMarkings={[
          {x: 0, y: 1}, {x: 1, y: 1}, {x: 1, y: 2}, {x: 0, y: 3}, {x: -1, y: 3}, {x: -1, y: 2}
        ]} />
        <AboutExampleBoard markColor={Color.COMPLETE} initialMarkings={[
          {x: 4, y: 1}, {x: 3, y: 2}, {x: 3, y: 3}, {x: 2, y: 4}, {x: 1, y: 4}, {x: 0, y: 4}
        ]} />
      </Flex>

      <Text>
        <b>But there is a catch!</b> Not every chain of 6 connected hexes counts as a valid win.
        In order for your spaces to be considered a win, <b>there cannot be any triangular clumps
        of 3 spaces within the chain</b>. That means that <b>these boards are NOT considered valid
        wins</b> (look closely at the 3 outlined spaces on each - those are clumps!)
      </Text>

      <Flex className={styles.flexy}>
        <AboutExampleBoard markColor={Color.INVALIDATED} initialMarkings={[
          {x: 0, y: 2},
          {x: 1, y: 1, outlineColor: "red"},
          {x: 2, y: 0, outlineColor: "red"},
          {x: 2, y: 1, outlineColor: "red"},
          {x: 2, y: 2},
          {x: 2, y: 3},
        ]} />
        <AboutExampleBoard markColor={Color.INVALIDATED} initialMarkings={[
          {x: 4, y: 0},
          {x: 4, y: 1},
          {x: 4, y: 2},
          {x: 3, y: 3, outlineColor: "red"},
          {x: 2, y: 4, outlineColor: "red"},
          {x: 2, y: 3, outlineColor: "red"},
        ]} />
      </Flex>

      <Text>
        The hexes must also form a single, <b>continuous</b> chain of 6. That means the
        following <b>don't count</b> as wins either:
      </Text>

      <Flex className={styles.flexy}>
        <AboutExampleBoard markColor={Color.INVALIDATED} initialMarkings={[
          {x: 2, y: 0},
          {x: 2, y: 1},
          {x: 2, y: 2},
          {x: 3, y: 2},
          {x: 4, y: 2},
          {x: 1, y: 3},
          {x: 0, y: 4},
        ]} />
        <AboutExampleBoard markColor={Color.INVALIDATED} initialMarkings={[
          {x: 3, y: 0},
          {x: 2, y: 0},
          {x: 1, y: 1},
          {x: 0, y: 1},
          {x: -1, y: 2},
          {x: 1, y: 2},
          {x: 2, y: 2},
          {x: 0, y: 3},
        ]} />
      </Flex>

      <Text>
        It seems complicated, but try it out and you'll get the hang of it. <b>All of the boards
        on this page can be marked and unmarked</b>, so you can try it for yourself and learn what
        counts and doesn't count!
      </Text>

    </Container>
  )
}

const AboutExampleBoard: React.FC<Omit<ExampleBoardProps, 'orientation'>> = (props ) => {
  return (
    <ExampleHexBoard
      className={styles.board}
      orientation={"hexagon-horizontal"}
      {...props}
    />
  )
}
